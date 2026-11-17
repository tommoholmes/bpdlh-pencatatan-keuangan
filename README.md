# Pencatatan Keuangan

Aplikasi catatan pengeluaran pribadi: klien Android offline-first, backend Cloudflare Worker, data di D1, foto di R2. Setiap pengguna hanya melihat catatannya sendiri, dan Worker mengambil `uid` dari ID token Firebase.

## Isi repo

| Folder | Isi |
| --- | --- |
| `android/` | Aplikasi Kotlin + Jetpack Compose (Navigation 3, MVVM, Room + SQLCipher, WorkManager) |
| `backend/` | API Hono + TypeScript di Cloudflare Workers, D1 untuk catatan, R2 untuk foto |

## Fitur

- Catat pengeluaran dengan field nama, harga, foto, dan lokasi.
- Simpan menggunakan SQLite
- Sinkronisasi otomatis: setelah menyimpan, saat aplikasi dibuka, saat jaringan ada, dan per 15 menit sekali.
- Lokasi dan info perangkat diisi sekali saat pengguna memberi izin, lalu disimpan sekali saat catatan disimpan.
- Layar pengaturan berisi status informasi akun, keluar.

## Setup

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


## Verifikasi

```sh
cd backend && npm test && npm run typecheck
cd android && ./gradlew :app:testDebugUnitTest
cd android && ./gradlew :app:connectedDebugAndroidTest   # butuh perangkat atau emulator
```
