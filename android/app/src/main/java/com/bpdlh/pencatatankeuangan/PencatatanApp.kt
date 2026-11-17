package com.bpdlh.pencatatankeuangan

import android.app.Application
import android.content.Context
import com.bpdlh.pencatatankeuangan.auth.AuthRepository
import com.bpdlh.pencatatankeuangan.data.AccountManager
import com.bpdlh.pencatatankeuangan.data.AppDatabase
import com.bpdlh.pencatatankeuangan.data.Connectivity
import com.bpdlh.pencatatankeuangan.data.DatabaseFactory
import com.bpdlh.pencatatankeuangan.data.DatabaseKey
import com.bpdlh.pencatatankeuangan.data.PhotoStore
import com.bpdlh.pencatatankeuangan.data.Prefs
import com.bpdlh.pencatatankeuangan.data.RecordsRepository
import com.bpdlh.pencatatankeuangan.data.SyncCoordinator
import com.bpdlh.pencatatankeuangan.data.SyncEngine
import com.bpdlh.pencatatankeuangan.data.SyncScheduler
import com.bpdlh.pencatatankeuangan.data.remote.SyncApi
import com.bpdlh.pencatatankeuangan.location.LocationClient
import com.google.firebase.auth.FirebaseAuth
import io.sentry.Sentry
import io.sentry.SentryLevel
import io.sentry.android.core.SentryAndroid
import kotlinx.coroutines.tasks.await

class AppContainer(private val context: Context) {
    val prefs = Prefs(context)
    val photos = PhotoStore(context)
    val connectivity = Connectivity(context)

    private val database: AppDatabase by lazy {
        DatabaseFactory.open(context, DatabaseKey(context).passphrase())
    }

    val api = SyncApi(
        baseUrl = BuildConfig.API_BASE_URL,
        tokenProvider = { FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token },
    )

    private val syncCoordinator = SyncCoordinator(
        SyncEngine(database.records(), database.syncState(), api, photos, prefs),
        prefs,
    )

    val records = RecordsRepository(
        dao = database.records(),
        syncStateDao = database.syncState(),
        photos = photos,
        prefs = prefs,
        api = api,
        scheduleSync = { SyncScheduler.requestNow(context) },
        syncCoordinator = syncCoordinator,
    )
    val auth = AuthRepository(context, api, prefs)
    val location = LocationClient(context)
    val account = AccountManager(auth, records, syncCoordinator)

    /** Laporan masalah aktif secara default; mematikannya menutup SDK sepenuhnya. */
    fun setCrashReporting(enabled: Boolean) {
        prefs.crashReportsEnabled = enabled
        val dsn = BuildConfig.SENTRY_DSN
        // Sentry menolak DSN kosong, jadi tanpa DSN SDK tidak pernah dinyalakan.
        if (!enabled || dsn.isBlank()) {
            if (Sentry.isEnabled()) Sentry.close()
            return
        }
        if (Sentry.isEnabled()) return
        SentryAndroid.init(context) { options ->
            options.dsn = dsn
            options.isSendDefaultPii = false
            options.tracesSampleRate = 0.1
            options.environment = if (BuildConfig.DEBUG) "debug" else "release"
            // Yang tidak boleh tercatat: nama, harga, foto, koordinat, token, isi body.
            options.beforeBreadcrumb = { breadcrumb, _ ->
                if (breadcrumb.category == "http") null else breadcrumb
            }
            options.setBeforeSend { event, _ ->
                event.request?.data = null
                event.request?.headers?.remove("Authorization")
                event
            }
            options.setDiagnosticLevel(SentryLevel.WARNING)
        }
        prefs.logId?.let { Sentry.setUser(io.sentry.protocol.User().apply { id = it }) }
    }
}

class PencatatanApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.setCrashReporting(container.prefs.crashReportsEnabled)
        SyncScheduler.ensurePeriodic(this)
        if (container.auth.currentUser != null) SyncScheduler.requestNow(this)
    }
}
