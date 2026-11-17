import * as Sentry from '@sentry/cloudflare'
import { Hono } from 'hono'
import { HTTPException } from 'hono/http-exception'
import { firebaseAuth, getFirebaseToken } from './auth'
import type { Env } from './env'
import { deleteUserData, getPhoto, putPhoto } from './photos'
import { photoIdSchema, pullQuerySchema, pushBodySchema } from './schema'
import { pullRecords, pushRecords } from './sync'

type Ctx = { Bindings: Env; Variables: { uid: string; logId: string } }

const app = new Hono<Ctx>()

app.use('*', (c, next) =>
  Sentry.continueTrace(
    { sentryTrace: c.req.header('sentry-trace'), baggage: c.req.header('baggage') },
    () => next(),
  ),
)

// Satu log ringkasan per request. Tidak pernah memuat nama, harga, koordinat, foto, token, atau body.
app.use('*', async (c, next) => {
  const started = Date.now()
  const syncId = c.req.header('x-sync-id')
  if (syncId) Sentry.setTag('syncId', syncId)

  await next()

  console.log(
    JSON.stringify({
      method: c.req.method,
      path: c.req.path,
      status: c.res.status,
      ms: Date.now() - started,
      syncId: syncId ?? null,
      logId: c.get('logId') ?? null,
    }),
  )
})

// Verifikasi ID token (401 bila tidak sah), lalu pastikan baris `users` ada
// (idempoten; sekaligus bootstrap). Hanya akun Google yang diterima.
app.use('/v1/*', firebaseAuth)
app.use('/v1/*', async (c, next) => {
  const token = getFirebaseToken(c)
  if (!token || token.firebase?.sign_in_provider !== 'google.com') {
    return c.json({ error: 'unauthorized' }, 401)
  }

  const uid = token.uid
  const selectLogId = c.env.DB.prepare('SELECT log_id FROM users WHERE id = ?').bind(uid)
  let logId = (await selectLogId.first<{ log_id: string }>())?.log_id

  if (!logId) {
    const created = crypto.randomUUID()
    await c.env.DB.prepare('INSERT OR IGNORE INTO users (id, created_at, seq, log_id) VALUES (?, ?, 0, ?)')
      .bind(uid, Date.now(), created)
      .run()
    logId = (await selectLogId.first<{ log_id: string }>())?.log_id ?? created
  }

  c.set('uid', uid)
  c.set('logId', logId)
  Sentry.setUser({ id: logId })
  await next()
})

app.post('/v1/me/bootstrap', (c) => c.json({ logId: c.get('logId') }))

app.post('/v1/sync/push', async (c) => {
  const body = pushBodySchema.safeParse(await c.req.json().catch(() => null))
  if (!body.success) return c.json({ error: 'invalid_body', issues: body.error.issues }, 400)

  return c.json(await pushRecords(c.env, c.get('uid'), body.data.deviceId, body.data.records, Date.now()))
})

app.get('/v1/sync/pull', async (c) => {
  const query = pullQuerySchema.safeParse({ since: c.req.query('since'), limit: c.req.query('limit') })
  if (!query.success) return c.json({ error: 'invalid_query', issues: query.error.issues }, 400)

  return c.json(await pullRecords(c.env, c.get('uid'), query.data.since, query.data.limit))
})

app.put('/v1/photos/:photoId', (c) => {
  const photoId = photoIdSchema.safeParse(c.req.param('photoId'))
  if (!photoId.success) return c.json({ error: 'invalid_photo_id' }, 400)

  return putPhoto(c.env, c.get('uid'), photoId.data, c.req.raw)
})

app.get('/v1/photos/:photoId', (c) => {
  const photoId = photoIdSchema.safeParse(c.req.param('photoId'))
  if (!photoId.success) return c.json({ error: 'invalid_photo_id' }, 400)

  return getPhoto(c.env, c.get('uid'), photoId.data)
})

app.delete('/v1/me', async (c) => {
  await deleteUserData(c.env, c.get('uid'))
  return new Response(null, { status: 204 })
})

app.onError((err, c) => {
  if (err instanceof HTTPException) {
    // Gagal mengambil kunci publik Firebase = gangguan upstream, bukan token tidak sah.
    if (err.cause instanceof Error && err.cause.message.includes('public keys')) {
      Sentry.captureException(err.cause)
      return c.json({ error: 'internal' }, 500)
    }
    // Middleware auth memakai 400 untuk header kosong; kontrak API tetap 401.
    return c.json({ error: 'unauthorized' }, 401)
  }

  Sentry.captureException(err)
  console.error(JSON.stringify({ error: err instanceof Error ? err.message : String(err), path: c.req.path }))
  return c.json({ error: 'internal' }, 500)
})

export default Sentry.withSentry(
  (env: Env) => ({
    dsn: env.SENTRY_DSN,
    tracesSampleRate: 0.1,

    beforeSend(event) {
      if (event.request) {
        delete event.request.data
        delete event.request.headers?.authorization
        delete event.request.headers?.Authorization
        delete event.request.headers?.cookie
      }
      return event
    },
    beforeBreadcrumb(breadcrumb) {
      return breadcrumb.category === 'console' ? null : breadcrumb
    },
  }),
  app,
)
