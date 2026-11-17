export interface Env {
  DB: D1Database
  PHOTOS: R2Bucket
  /** ID proyek Firebase; dipakai sebagai `aud` dan bagian `iss` dari ID token. */
  FIREBASE_PROJECT_ID: string
  /** Bila kosong, pelaporan ke Sentry dimatikan. */
  SENTRY_DSN?: string
}
