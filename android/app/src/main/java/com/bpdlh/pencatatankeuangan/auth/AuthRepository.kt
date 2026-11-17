package com.bpdlh.pencatatankeuangan.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.bpdlh.pencatatankeuangan.BuildConfig
import com.bpdlh.pencatatankeuangan.data.Prefs
import com.bpdlh.pencatatankeuangan.data.SyncScheduler
import com.bpdlh.pencatatankeuangan.data.remote.SyncApi
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Login hanya lewat Google. Tidak ada metode lain. */
class AuthRepository(
    private val context: Context,
    private val api: SyncApi,
    private val prefs: Prefs,
) {
    private val firebase: FirebaseAuth get() = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser? get() = firebase.currentUser

    val session: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        firebase.addAuthStateListener(listener)
        awaitClose { firebase.removeAuthStateListener(listener) }
    }

    suspend fun signInWithGoogle(): Result<Unit> = try {
        val manager = CredentialManager.create(context)
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = manager.getCredential(context, request).credential
        val googleId = GoogleIdTokenCredential.createFrom(credential.data)
        val firebaseCredential = GoogleAuthProvider.getCredential(googleId.idToken, null)
        firebase.signInWithCredential(firebaseCredential).await()
        Result.success(Unit)
    } catch (e: GetCredentialCancellationException) {
        Result.failure(IllegalStateException("Login dibatalkan."))
    } catch (e: NoCredentialException) {
        Result.failure(
            IllegalStateException(
                "Tidak ada akun Google di perangkat ini. Tambahkan akun lewat Pengaturan Android, lalu coba lagi.",
            ),
        )
    } catch (e: GetCredentialException) {
        Log.w("Auth", "Credential Manager gagal", e)
        Result.failure(IllegalStateException("Login Google gagal: ${e.message ?: e.type}"))
    } catch (e: Exception) {
        Log.w("Auth", "Login gagal", e)
        Result.failure(IllegalStateException("Login Google gagal. Periksa koneksi lalu coba lagi."))
    }

    /** Membuat baris `users` di server bila belum ada dan menyimpan `log_id` untuk dukungan. */
    suspend fun bootstrap(): Result<Unit> = try {
        val logId = api.bootstrap()
        prefs.logId = logId
        if (io.sentry.Sentry.isEnabled()) {
            io.sentry.Sentry.setUser(io.sentry.protocol.User().apply { id = logId })
        }
        // Login pertama perlu menarik data yang sudah ada di server.
        SyncScheduler.requestNow(context)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun signOut() {
        firebase.signOut()
        prefs.clearSession()
    }

    /** Urutan wajib: hapus data server lebih dulu, baru akun Auth (token tidak valid setelahnya). */
    suspend fun deleteAccount(): Result<Unit> = try {
        api.deleteAccount()
        firebase.currentUser?.delete()?.await()
        prefs.clearSession()
        Result.success(Unit)
    } catch (e: FirebaseAuthRecentLoginRequiredException) {
        Result.failure(IllegalStateException("Sesi sudah lama. Masuk ulang dengan Google, lalu hapus akun lagi."))
    } catch (e: Exception) {
        Result.failure(IllegalStateException(e.message ?: "Hapus akun gagal."))
    }
}
