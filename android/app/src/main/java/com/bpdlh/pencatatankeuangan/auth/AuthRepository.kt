package com.bpdlh.pencatatankeuangan.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.bpdlh.pencatatankeuangan.BuildConfig
import com.bpdlh.pencatatankeuangan.data.AuthException
import com.bpdlh.pencatatankeuangan.data.AuthFailure
import com.bpdlh.pencatatankeuangan.data.AuthGateway
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

class AuthRepository(
    private val context: Context,
    private val api: SyncApi,
    private val prefs: Prefs,
) : AuthGateway {
    private val firebase: FirebaseAuth get() = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser? get() = firebase.currentUser

    override val currentUserEmail: String? get() = firebase.currentUser?.email

    val session: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        firebase.addAuthStateListener(listener)
        awaitClose { firebase.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithGoogle(): Result<Unit> = try {
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
        Result.failure(AuthException(AuthFailure.Cancelled))
    } catch (e: NoCredentialException) {
        Result.failure(AuthException(AuthFailure.NoAccount))
    } catch (e: GetCredentialException) {
        Log.w("Auth", "Credential Manager gagal", e)
        Result.failure(AuthException(AuthFailure.GoogleFailed(e.message ?: e.type)))
    } catch (e: Exception) {
        Log.w("Auth", "Login gagal", e)
        Result.failure(AuthException(AuthFailure.SignInFailed))
    }

    override suspend fun bootstrap(): Result<Unit> = try {
        val logId = api.bootstrap()
        prefs.logId = logId
        if (io.sentry.Sentry.isEnabled()) {
            io.sentry.Sentry.setUser(io.sentry.protocol.User().apply { id = logId })
        }
        SyncScheduler.requestNow(context)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun signOut() {
        firebase.signOut()
        prefs.clearSession()
    }

    suspend fun deleteAccount(): Result<Unit> = try {
        api.deleteAccount()
        firebase.currentUser?.delete()?.await()
        prefs.clearSession()
        Result.success(Unit)
    } catch (e: FirebaseAuthRecentLoginRequiredException) {
        Result.failure(AuthException(AuthFailure.RecentLoginRequired))
    } catch (e: Exception) {
        Result.failure(AuthException(AuthFailure.DeleteFailed))
    }
}
