export interface Env {
  DB: D1Database
  PHOTOS: R2Bucket

  FIREBASE_PROJECT_ID: string

  SENTRY_DSN?: string
}
