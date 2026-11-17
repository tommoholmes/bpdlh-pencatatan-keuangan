import type { Env } from './env'

export const MAX_PHOTO_BYTES = 5 * 1024 * 1024

const isJpeg = (bytes: Uint8Array) => bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff

/** Kunci selalu berawalan uid dari token, jadi pengguna tidak bisa menyentuh foto pengguna lain. */
const photoKey = (uid: string, photoId: string) => `${uid}/${photoId}.jpg`

export async function putPhoto(env: Env, uid: string, photoId: string, req: Request): Promise<Response> {
  if (!(req.headers.get('content-type') ?? '').startsWith('image/jpeg')) {
    return Response.json({ error: 'hanya JPEG' }, { status: 415 })
  }
  const declared = Number(req.headers.get('content-length') ?? 0)
  if (declared > MAX_PHOTO_BYTES) return Response.json({ error: 'foto terlalu besar' }, { status: 413 })

  const body = await req.arrayBuffer()
  if (body.byteLength === 0 || body.byteLength > MAX_PHOTO_BYTES) {
    return Response.json({ error: 'foto terlalu besar' }, { status: 413 })
  }
  if (!isJpeg(new Uint8Array(body.slice(0, 3)))) {
    return Response.json({ error: 'hanya JPEG' }, { status: 415 })
  }

  await env.PHOTOS.put(photoKey(uid, photoId), body, {
    httpMetadata: { contentType: 'image/jpeg' },
  })
  return new Response(null, { status: 204 })
}

export async function getPhoto(env: Env, uid: string, photoId: string): Promise<Response> {
  const object = await env.PHOTOS.get(photoKey(uid, photoId))
  if (!object) return Response.json({ error: 'tidak ditemukan' }, { status: 404 })

  return new Response(object.body, {
    headers: {
      'content-type': 'image/jpeg',
      'cache-control': 'private, max-age=3600',
      etag: object.httpEtag,
    },
  })
}

/** Hapus semua data pengguna: seluruh objek R2 di bawah `{uid}/` lalu baris D1. */
export async function deleteUserData(env: Env, uid: string): Promise<void> {
  let cursor: string | undefined
  do {
    const listed = await env.PHOTOS.list({ prefix: `${uid}/`, cursor })
    if (listed.objects.length > 0) await env.PHOTOS.delete(listed.objects.map((o) => o.key))
    cursor = listed.truncated ? listed.cursor : undefined
  } while (cursor)

  await env.DB.batch([
    env.DB.prepare('DELETE FROM records WHERE user_id = ?').bind(uid),
    env.DB.prepare('DELETE FROM users WHERE id = ?').bind(uid),
  ])
}
