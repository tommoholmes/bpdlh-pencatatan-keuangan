# Backend: Pencatatan (Cloudflare Worker)

Hono + TypeScript di Cloudflare Workers, data di D1, foto di R2.

## Endpoint

| Endpoint | Fungsi |
| --- | --- |
| `POST /v1/me/bootstrap` | Membuat baris `users` bila belum ada, mengembalikan `logId` |
| `POST /v1/sync/push` | Batch catatan (maks 20), satu transaksi D1 |
| `GET /v1/sync/pull?since=&limit=` | Perubahan sejak `seq`, termasuk tombstone |
| `PUT /v1/photos/{photoId}` | Simpan JPEG (maks 5 MB) |
| `GET /v1/photos/{photoId}` | Ambil foto milik pemanggil |
| `DELETE /v1/me` | Hapus semua baris D1 dan foto R2 pengguna |

Semua endpoint `/v1/*` wajib `Authorization: Bearer <ID token Firebase>`. Worker mengambil `uid` dari `sub` token, tidak pernah dari body.

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

Bucket R2 tidak boleh punya akses publik. Foto hanya keluar lewat Worker, dengan kunci `{uid}/{photoId}.jpg`.

## Catatan implementasi

- Server menyelesaikan konflik per catatan dengan last-write-wins dan membatasi `updated_at` kiriman ke jam server. `seq` selalu naik, jadi versi pemenang tetap ikut terkirim lewat pull.
- Workers Logs menulis satu baris ringkasan per request berisi `logId` dan `X-Sync-Id`. Baris itu tidak pernah memuat isi catatan, koordinat, token, atau body.
- `nodejs_compat` dinyalakan hanya karena SDK Sentry butuh AsyncLocalStorage.
- `test/fixtures/key.pem` adalah kunci uji untuk menandatangani token di test. Jangan pakai di produksi.
