# Pencatatan

Aplikasi Android untuk mencatat pengeluaran harian.

## Spesifikasi

- Bahasa: Kotlin
- UI: Jetpack Compose, Material 3, Navigation 3
- Database: Room + SQLCipher
- Sinkronisasi: WorkManager
- minSdk 24, targetSdk 37

## Build

```sh
./gradlew :app:assembleDebug
```

Hasil APK ada di `app/build/outputs/apk/debug/app-debug.apk`.

## Test

```sh
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

Test instrumentasi butuh perangkat atau emulator yang terhubung.

## Konfigurasi

Nilai berikut dibaca dari `local.properties` dan boleh dikosongkan:

```properties
api.baseUrl.debug=
sentry.dsn=
sentry.dsn.debug=
```

`google-services.json` sudah ada di `app/`. Login Google memakai Firebase Auth.
