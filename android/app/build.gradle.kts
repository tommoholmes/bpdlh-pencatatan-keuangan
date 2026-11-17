import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

// Nilai build yang dipakai bersama. Sentry: `sentry.dsn` (rilis) dan `sentry.dsn.debug` (debug)
// diisi di local.properties. Kosong berarti SDK tidak mengirim apa pun.
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val prodApiBaseUrl = "https://pencatatan-keuangan.adityabudiutomo24.workers.dev"

android {
    namespace = "com.bpdlh.pencatatankeuangan"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.bpdlh.pencatatankeuangan"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Web client ID dari google-services.json; GetGoogleIdOption menolak client tipe Android.
        buildConfigField(
            "String",
            "WEB_CLIENT_ID",
            "\"315183333969-mj8v84030890bcd0c7o5omh1m9hl8ae9.apps.googleusercontent.com\"",
        )
    }

    buildTypes {
        debug {
            // Penanda di luar layar aplikasi: nama peluncur dan versi.
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "Pencatatan Debug")
            // Base URL debug boleh diarahkan ke Worker lain lewat `api.baseUrl.debug`; bawaan tetap produksi.
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${localProperties.getProperty("api.baseUrl.debug", prodApiBaseUrl)}\"",
            )
            // Crash build debug tidak masuk ke proyek Sentry rilis.
            buildConfigField("String", "SENTRY_DSN", "\"${localProperties.getProperty("sentry.dsn.debug", "")}\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"$prodApiBaseUrl\"")
            buildConfigField("String", "SENTRY_DSN", "\"${localProperties.getProperty("sentry.dsn", "")}\"")
            // R8 dimatikan sampai unggahan mapping R8 tersedia, supaya stack trace Sentry tetap terbaca.
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime)
    implementation(libs.sqlcipher.android)
    implementation(libs.sentry.android)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.play.services.location)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.exifinterface)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
