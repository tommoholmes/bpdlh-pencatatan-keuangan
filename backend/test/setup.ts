import { applyD1Migrations, env } from 'cloudflare:test'
import { beforeEach } from 'vitest'

// Idempoten: migrasi yang sudah tercatat di `d1_migrations` dilewati.
beforeEach(() => applyD1Migrations(env.DB, env.TEST_MIGRATIONS))
