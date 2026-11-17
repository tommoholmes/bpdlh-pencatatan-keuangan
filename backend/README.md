# Backend: Pencatatan (Cloudflare Worker)

Hono + TypeScript di Cloudflare Workers, data di D1, foto di R2.

## Setup

Database D1 `pencatatan-keuangan` sudah dibuat dan `database_id`-nya terisi di `wrangler.toml`. `FIREBASE_PROJECT_ID` diisi `bpdlh-pencatatan-keuangan`, secret `SENTRY_DSN` sudah terpasang. Yang belum: R2 harus diaktifkan di dashboard Cloudflare sebelum bucket dibuat.

```sh
npm install

npx wrangler r2 bucket create pencatatan-keuangan-photos
npm run db:migrate:remote   # ulangi setiap kali menambah migrasi
npm run db:migrate:local    # database lokal untuk wrangler dev

npm run dev      # wrangler dev
npm run deploy   # wrangler deploy
npm test         # vitest di runtime workerd (D1 + R2 lokal)
npm run typecheck
```
