package com.bpdlh.pencatatankeuangan.data

import androidx.annotation.StringRes
import com.bpdlh.pencatatankeuangan.R
import com.bpdlh.pencatatankeuangan.ui.UiText

sealed interface AuthFailure {
    data object Cancelled : AuthFailure
    data object NoAccount : AuthFailure
    data class GoogleFailed(val detail: String?) : AuthFailure
    data object SignInFailed : AuthFailure
    data object RecentLoginRequired : AuthFailure
    data object DeleteFailed : AuthFailure

    @get:StringRes
    val messageRes: Int
        get() = when (this) {
            Cancelled -> R.string.login_cancelled
            NoAccount -> R.string.login_no_account
            is GoogleFailed -> if (detail == null) R.string.login_google_failed else R.string.login_google_failed_detail
            SignInFailed -> R.string.login_google_failed
            RecentLoginRequired -> R.string.account_recent_login_required
            DeleteFailed -> R.string.settings_delete_failed
        }

    val uiText: UiText
        get() = when (this) {
            is GoogleFailed -> if (detail == null) {
                UiText.Res(messageRes)
            } else {
                UiText.Res(messageRes, listOf(detail))
            }
            else -> UiText.Res(messageRes)
        }
}

class AuthException(val failure: AuthFailure) : Exception(failure.javaClass.simpleName)
