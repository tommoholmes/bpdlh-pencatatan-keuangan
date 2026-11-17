import { getFirebaseToken, verifyFirebaseAuth } from '@hono/firebase-auth'
import type { KeyStorer } from 'firebase-auth-cloudflare-workers'
import type { MiddlewareHandler } from 'hono'
import type { Env } from './env'

// Cache kunci publik di memori isolate (tanpa binding KV). TTL mengikuti `max-age`
// yang dikirim library; sertifikat Firebase dirotasi berkala.
let cached: { json: string; expiresAt: number } | null = null

const keyStore: KeyStorer = {
  async get<ExpectedValue = unknown>(): Promise<ExpectedValue | null> {
    if (!cached || cached.expiresAt <= Date.now()) return null
    return JSON.parse(cached.json) as ExpectedValue
  },
  async put(value, expirationTtl) {
    cached = { json: value, expiresAt: Date.now() + expirationTtl * 1000 }
  },
}

/**
 * Verifikasi ID token Firebase. Hasilnya diambil dengan `getFirebaseToken(c)`;
 * token tidak sah dilempar sebagai HTTPException 401.
 */
export const firebaseAuth: MiddlewareHandler<{ Bindings: Env }> = (c, next) =>
  verifyFirebaseAuth({
    projectId: c.env.FIREBASE_PROJECT_ID,
    keyStore,
    disableErrorLog: true,
  })(c, next)

export { getFirebaseToken }
