import type { Env } from './env'
import { recordSchema, type RecordInput } from './schema'

const UPSERT_RECORD = `
INSERT INTO records (
  user_id, id, name, price, photo_id, latitude, longitude, accuracy_m,
  device_id, device_model, os_version, app_version, created_at, updated_at, deleted_at, seq
) VALUES (
  ?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15,
  (SELECT seq FROM users WHERE id = ?1)
)
ON CONFLICT (user_id, id) DO UPDATE SET
  name       = CASE WHEN excluded.updated_at > records.updated_at THEN excluded.name       ELSE records.name       END,
  price      = CASE WHEN excluded.updated_at > records.updated_at THEN excluded.price      ELSE records.price      END,
  photo_id   = CASE WHEN excluded.updated_at > records.updated_at THEN excluded.photo_id   ELSE records.photo_id   END,
  deleted_at = CASE WHEN excluded.updated_at > records.updated_at THEN excluded.deleted_at ELSE records.deleted_at END,
  updated_at = MAX(records.updated_at, excluded.updated_at),
  seq        = excluded.seq
`

interface PushResult {
  id: string | null
  status: 'applied' | 'rejected'
  reason?: string
}

const rawId = (raw: unknown): string | null => {
  const id = (raw as { id?: unknown } | null)?.id
  return typeof id === 'string' ? id : null
}

export async function pushRecords(env: Env, uid: string, deviceId: string, rawRecords: unknown[], now: number) {
  const results: (PushResult | undefined)[] = []
  const statements: D1PreparedStatement[] = []
  const applied: number[] = []

  rawRecords.forEach((raw, index) => {
    const parsed = recordSchema.safeParse(raw)
    if (!parsed.success) {
      results[index] = { id: rawId(raw), status: 'rejected', reason: 'invalid_record' }
      return
    }
    const rec: RecordInput = parsed.data
    const updatedAt = Math.min(rec.updatedAt, now)
    const deletedAt = rec.deletedAt === null ? null : Math.min(rec.deletedAt, now)

    statements.push(
      env.DB.prepare('UPDATE users SET seq = seq + 1 WHERE id = ?').bind(uid),
      env.DB
        .prepare(UPSERT_RECORD)
        .bind(
          uid,
          rec.id,
          rec.name,
          rec.price,
          rec.photoId,
          rec.latitude,
          rec.longitude,
          rec.accuracyM,
          deviceId,
          rec.deviceModel,
          rec.osVersion,
          rec.appVersion,
          rec.createdAt,
          updatedAt,
          deletedAt,
        ),
    )
    applied.push(index)
  })

  if (statements.length > 0) await env.DB.batch(statements)
  for (const index of applied) results[index] = { id: rawId(rawRecords[index]), status: 'applied' }

  const serverSeq =
    (await env.DB.prepare('SELECT seq FROM users WHERE id = ?').bind(uid).first<number>('seq')) ?? 0

  return {
    results: results.filter((r): r is PushResult => r !== undefined),
    serverSeq,
  }
}

interface PullRow {
  id: string
  name: string
  price: number
  photo_id: string | null
  latitude: number | null
  longitude: number | null
  accuracy_m: number | null
  device_id: string | null
  device_model: string | null
  os_version: string | null
  app_version: string | null
  created_at: number
  updated_at: number
  deleted_at: number | null
  seq: number
}

export async function pullRecords(env: Env, uid: string, since: number, limit: number) {
  const { results } = await env.DB.prepare(
    `SELECT id, name, price, photo_id, latitude, longitude, accuracy_m,
            device_id, device_model, os_version, app_version,
            created_at, updated_at, deleted_at, seq
       FROM records
      WHERE user_id = ? AND seq > ?
      ORDER BY seq
      LIMIT ?`,
  )
    .bind(uid, since, limit)
    .all<PullRow>()

  const last = results.at(-1)

  return {
    records: results.map((r) => ({
      id: r.id,
      name: r.name,
      price: r.price,
      photoId: r.photo_id,
      latitude: r.latitude,
      longitude: r.longitude,
      accuracyM: r.accuracy_m,
      deviceId: r.device_id,
      deviceModel: r.device_model,
      osVersion: r.os_version,
      appVersion: r.app_version,
      createdAt: r.created_at,
      updatedAt: r.updated_at,
      deletedAt: r.deleted_at,
      seq: r.seq,
    })),
    nextSeq: last?.seq ?? since,
    hasMore: results.length === limit,
  }
}
