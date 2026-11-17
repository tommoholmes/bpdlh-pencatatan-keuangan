# Pencatatan Keuangan

Aplikasi catatan pengeluaran pribadi: klien Android offline-first, backend Cloudflare Worker, data di D1, foto di R2. Setiap pengguna hanya melihat catatannya sendiri, dan Worker mengambil `uid` dari ID token Firebase, tidak pernah dari body permintaan.

## Isi repo

| Folder | Isi |
| --- | --- |
| `android/` | Aplikasi Kotlin + Jetpack Compose (Navigation 3, MVVM, Room + SQLCipher, WorkManager) |
| `backend/` | API Hono + TypeScript di Cloudflare Workers, D1 untuk catatan, R2 untuk foto |

## Fitur

- Catat pengeluaran dengan nama, harga, foto, dan lokasi.
- Tulis selalu masuk Room dulu, jadi aplikasi tetap jalan tanpa internet.
- Sinkronisasi otomatis: setelah menyimpan (jeda 3 detik), saat aplikasi dibuka, saat jaringan kembali, dan periodik 15 menit.
- Server menyelesaikan konflik per catatan dengan last-write-wins; penghapusan memakai tombstone supaya ikut tersinkron ke perangkat lain.
- Lokasi dan info perangkat diisi sekali saat catatan dibuat. Foto ditulis ulang di klien sehingga EXIF, termasuk GPS, hilang.
- Layar pengaturan berisi status sinkronisasi, ID dukungan, sakelar simpan lokasi otomatis, keluar, dan hapus akun.

## Menjalankan

Backend:

```sh
cd backend
npm install
npm run db:migrate:local
npm run dev
```

Android:

```sh
cd android
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Build debug memakai nama peluncur `Pencatatan Debug`, sedangkan rilis `Pencatatan`. Base URL debug bisa diarahkan ke Worker lain lewat `api.baseUrl.debug` di `android/local.properties`; bawaannya URL produksi. DSN Sentry juga dibaca dari `local.properties` dan kosong berarti tidak ada laporan.

## Verifikasi

```sh
cd backend && npm test && npm run typecheck
cd android && ./gradlew :app:testDebugUnitTest
cd android && ./gradlew :app:connectedDebugAndroidTest   # butuh perangkat atau emulator
```

## Data dan privasi

- Tabel `records` menyimpan nama, harga, foto, koordinat, akurasi, info perangkat, dan waktu. Foto disimpan di R2 dengan kunci `{uid}/{photoId}.jpg`; bucket tidak boleh punya akses publik.
- Log ringkasan satu baris per request hanya memuat `logId` dan `X-Sync-Id`, tidak pernah isi catatan, koordinat, token, atau body.
- Sentry tidak menerima nama, harga, foto, koordinat, token, atau isi body.
- Saat pengguna menghapus akun, server membuang semua baris D1 dan foto R2 miliknya, lalu aplikasi membuang data lokal.

## Dokumen lain

- `android/README.md`: spesifikasi, cara build, test, dan konfigurasi lokal.
- `backend/README.md`: daftar endpoint, setup D1 dan R2, catatan implementasi.
