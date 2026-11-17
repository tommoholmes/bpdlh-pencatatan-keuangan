import { SELF, env } from 'cloudflare:test'
import { SignJWT, importPKCS8 } from 'jose'
import { beforeAll, describe, expect, it, vi } from 'vitest'
import certPem from './fixtures/cert.pem?raw'
import keyPem from './fixtures/key.pem?raw'

const KID = 'test-kid'
const PROJECT_ID = env.FIREBASE_PROJECT_ID
const privateKey = await importPKCS8(keyPem, 'RS256')

beforeAll(() => {
  vi.stubGlobal('fetch', async (input: unknown) => {
    const url =
      typeof input === 'string' ? input : input instanceof URL ? input.href : (input as Request).url
    if (!url.startsWith('https://www.googleapis.com/robot/v1/metadata/jwk/')) {
      throw new Error(`fetch tidak diharapkan: ${url}`)
    }
    return new Response(JSON.stringify({ [KID]: certPem }), {
      headers: { 'cache-control': 'max-age=0' },
    })
  })
})

interface TokenOptions {
  sub?: string
  provider?: string
  audience?: string
  issuer?: string
}

const signToken = ({ sub = 'uid-a', provider = 'google.com', audience = PROJECT_ID, issuer }: TokenOptions = {}) => {
  const now = Math.floor(Date.now() / 1000)
  return new SignJWT({ firebase: { sign_in_provider: provider }, auth_time: now })
    .setProtectedHeader({ alg: 'RS256', kid: KID })
    .setSubject(sub)
    .setAudience(audience)
    .setIssuer(issuer ?? `https://securetoken.google.com/${PROJECT_ID}`)
    .setIssuedAt(now)
    .setExpirationTime('1h')
    .sign(privateKey)
}

const call = (path: string, token: string, init: RequestInit = {}) =>
  SELF.fetch(`https://api.test${path}`, {
    ...init,
    headers: { ...init.headers, authorization: `Bearer ${token}` },
  })

const json = (body: unknown, method = 'POST') =>
  ({ method, headers: { 'content-type': 'application/json' }, body: JSON.stringify(body) }) satisfies RequestInit

const record = (over: Record<string, unknown> = {}) => ({
  id: 'rec-1',
  name: 'Kopi susu',
  price: 18000,
  photoId: null,
  latitude: null,
  longitude: null,
  accuracyM: null,
  deviceModel: 'Pixel 8',
  osVersion: 'Android 15',
  appVersion: '1.0.0',
  createdAt: 1_794_800_000_000,
  updatedAt: 1_794_800_000_000,
  deletedAt: null,
  ...over,
})

const push = async (token: string, records: unknown[], deviceId = 'dev-1') =>
  (await call('/v1/sync/push', token, json({ deviceId, records }))).json<{
    results: { id: string | null; status: string; reason?: string }[]
    serverSeq: number
  }>()

const pull = async (token: string, since = 0, limit = 200) =>
  (await call(`/v1/sync/pull?since=${since}&limit=${limit}`, token)).json<{
    records: { id: string; name: string; seq: number; deletedAt: number | null; updatedAt: number }[]
    nextSeq: number
    hasMore: boolean
  }>()

describe('verifikasi ID token', () => {
  it('menolak request tanpa token', async () => {
    expect((await SELF.fetch('https://api.test/v1/sync/pull?since=0')).status).toBe(401)
  })

  it('menolak token dari provider selain Google', async () => {
    const token = await signToken({ provider: 'password' })
    expect((await call('/v1/sync/pull?since=0', token)).status).toBe(401)
  })

  it('menolak audience dan issuer yang salah', async () => {
    const wrongAud = await signToken({ audience: 'proyek-lain' })
    const wrongIss = await signToken({ issuer: 'https://securetoken.google.com/proyek-lain' })
    expect((await call('/v1/sync/pull?since=0', wrongAud)).status).toBe(401)
    expect((await call('/v1/sync/pull?since=0', wrongIss)).status).toBe(401)
  })

  it('menolak tanda tangan yang tidak cocok', async () => {
    const [header, payload] = (await signToken()).split('.')
    const forged = `${header}.${payload}.${Buffer.from('palsu').toString('base64url')}`
    expect((await call('/v1/sync/pull?since=0', forged)).status).toBe(401)
  })
})

describe('bootstrap', () => {
  it('mengembalikan log_id yang sama pada panggilan berikutnya, dan log_id bukan turunan uid', async () => {
    const token = await signToken({ sub: 'uid-bootstrap' })
    const first = await (await call('/v1/me/bootstrap', token, { method: 'POST' })).json<{ logId: string }>()
    const second = await (await call('/v1/me/bootstrap', token, { method: 'POST' })).json<{ logId: string }>()

    expect(first.logId).toBe(second.logId)
    expect(first.logId).not.toContain('uid-bootstrap')
  })
})

describe('push dan pull', () => {
  it('menerapkan catatan baru dan menariknya kembali', async () => {
    const token = await signToken({ sub: 'uid-push' })
    const pushed = await push(token, [record()])

    expect(pushed).toEqual({ results: [{ id: 'rec-1', status: 'applied' }], serverSeq: 1 })

    const pulled = await pull(token)
    expect(pulled.records).toHaveLength(1)
    expect(pulled.records[0]).toMatchObject({ id: 'rec-1', name: 'Kopi susu', seq: 1 })
    expect(pulled.nextSeq).toBe(1)
    expect(pulled.hasMore).toBe(false)
  })

  it('memakai last-write-wins berdasarkan updated_at, sementara seq selalu naik', async () => {
    const token = await signToken({ sub: 'uid-lww' })
    await push(token, [record({ name: 'Baru', updatedAt: 1_794_800_000_000 })])

    const stale = await push(token, [record({ name: 'Lama', updatedAt: 1_794_700_000_000 })])
    expect(stale.serverSeq).toBe(2)

    let pulled = await pull(token)
    expect(pulled.records[0]).toMatchObject({ name: 'Baru', seq: 2 })

    await push(token, [record({ name: 'Lebih baru', updatedAt: 1_794_900_000_000 })])
    pulled = await pull(token)
    expect(pulled.records[0]).toMatchObject({ name: 'Lebih baru', seq: 3 })
  })

  it('mempertahankan versi server bila updated_at sama', async () => {
    const token = await signToken({ sub: 'uid-tie' })
    await push(token, [record({ name: 'Server' })])
    await push(token, [record({ name: 'Klien', updatedAt: 1_794_800_000_000 })])

    const pulled = await pull(token)
    expect(pulled.records[0]?.name).toBe('Server')
  })

  it('mengirim tombstone lewat pull', async () => {
    const token = await signToken({ sub: 'uid-tombstone' })
    const deletedAt = Date.now() - 60_000
    await push(token, [record({ deletedAt, updatedAt: deletedAt })])

    const pulled = await pull(token)
    expect(pulled.records[0]?.deletedAt).toBe(deletedAt)
  })

  it('membatasi updated_at ke jam server', async () => {
    const token = await signToken({ sub: 'uid-clock' })
    const future = Date.now() + 10 * 24 * 60 * 60 * 1000
    await push(token, [record({ updatedAt: future })])

    const pulled = await pull(token)
    expect(pulled.records[0]?.updatedAt).toBeLessThanOrEqual(Date.now())
  })

  it('menolak hanya catatan yang tidak valid', async () => {
    const token = await signToken({ sub: 'uid-reject' })
    const result = await push(token, [
      record({ id: 'rec-baik' }),
      record({ id: 'rec-lat-saja', latitude: -7.79, longitude: null }),
      record({ id: 'rec-harga', price: -1 }),
    ])

    expect(result.results).toEqual([
      { id: 'rec-baik', status: 'applied' },
      { id: 'rec-lat-saja', status: 'rejected', reason: 'invalid_record' },
      { id: 'rec-harga', status: 'rejected', reason: 'invalid_record' },
    ])
    expect((await pull(token)).records.map((r) => r.id)).toEqual(['rec-baik'])
  })

  it('menolak lebih dari 20 catatan per push', async () => {
    const token = await signToken({ sub: 'uid-batch' })
    const records = Array.from({ length: 21 }, (_, i) => record({ id: `rec-${i}` }))
    expect((await call('/v1/sync/push', token, json({ deviceId: 'dev-1', records }))).status).toBe(400)
  })

  it('memberi tahu hasMore saat halaman pull penuh', async () => {
    const token = await signToken({ sub: 'uid-page' })
    await push(
      token,
      Array.from({ length: 3 }, (_, i) => record({ id: `rec-${i}`, updatedAt: 1_794_800_000_000 + i })),
    )

    const first = await pull(token, 0, 2)
    expect(first.records).toHaveLength(2)
    expect(first.hasMore).toBe(true)
    expect(first.nextSeq).toBe(2)

    const second = await pull(token, first.nextSeq, 2)
    expect(second.records.map((r) => r.id)).toEqual(['rec-2'])
    expect(second.hasMore).toBe(false)
  })
})

describe('foto', () => {
  const jpeg = (size = 32) => {
    const bytes = new Uint8Array(size).fill(0x20)
    bytes.set([0xff, 0xd8, 0xff, 0xe0])
    return bytes
  }

  it('menyimpan JPEG dan mengirimkannya kembali hanya ke pemiliknya', async () => {
    const owner = await signToken({ sub: 'uid-foto' })
    const other = await signToken({ sub: 'uid-lain' })

    const put = await call('/v1/photos/ph-1', owner, {
      method: 'PUT',
      headers: { 'content-type': 'image/jpeg' },
      body: jpeg(),
    })
    expect(put.status).toBe(204)

    const got = await call('/v1/photos/ph-1', owner)
    expect(got.status).toBe(200)
    expect(got.headers.get('content-type')).toBe('image/jpeg')
    expect(new Uint8Array(await got.arrayBuffer())).toEqual(jpeg())

    expect((await call('/v1/photos/ph-1', other)).status).toBe(404)
  })

  it('menolak bukan JPEG dan foto lebih dari 5 MB', async () => {
    const token = await signToken({ sub: 'uid-foto-invalid' })

    const notJpeg = await call('/v1/photos/ph-2', token, {
      method: 'PUT',
      headers: { 'content-type': 'image/png' },
      body: jpeg(),
    })
    expect(notJpeg.status).toBe(415)

    const tooBig = await call('/v1/photos/ph-3', token, {
      method: 'PUT',
      headers: { 'content-type': 'image/jpeg' },
      body: jpeg(5 * 1024 * 1024 + 1),
    })
    expect(tooBig.status).toBe(413)
  })
})

describe('hapus akun', () => {
  it('menghapus baris D1 dan foto R2 milik pengguna', async () => {
    const token = await signToken({ sub: 'uid-hapus' })
    const before = await (await call('/v1/me/bootstrap', token, { method: 'POST' })).json<{ logId: string }>()

    await push(token, [record({ photoId: 'ph-hapus' })])
    await call('/v1/photos/ph-hapus', token, {
      method: 'PUT',
      headers: { 'content-type': 'image/jpeg' },
      body: new Uint8Array([0xff, 0xd8, 0xff, 0xe0]),
    })

    expect((await call('/v1/me', token, { method: 'DELETE' })).status).toBe(204)
    expect((await pull(token)).records).toEqual([])
    expect((await call('/v1/photos/ph-hapus', token)).status).toBe(404)

    const after = await (await call('/v1/me/bootstrap', token, { method: 'POST' })).json<{ logId: string }>()
    expect(after.logId).not.toBe(before.logId)
  })
})
