package com.bpdlh.pencatatankeuangan.ui.login

import com.bpdlh.pencatatankeuangan.R
import com.bpdlh.pencatatankeuangan.data.AuthException
import com.bpdlh.pencatatankeuangan.data.AuthFailure
import com.bpdlh.pencatatankeuangan.ui.FakeAuth
import com.bpdlh.pencatatankeuangan.ui.UiText
import com.bpdlh.pencatatankeuangan.ui.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun signInSuccessBootstrapsAndClearsLoading() = runTest(main.dispatcher) {
        val auth = FakeAuth()
        val viewModel = LoginViewModel(auth)

        viewModel.onAction(LoginAction.OnSignInClick)
        advanceUntilIdle()

        assertEquals(1, auth.signInCalls)
        assertEquals(1, auth.bootstrapCalls)
        assertFalse(viewModel.state.value.isSigningIn)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun signInFailureShowsMessageAndSkipsBootstrap() = runTest(main.dispatcher) {
        val auth = FakeAuth(signInResult = Result.failure(AuthException(AuthFailure.Cancelled)))
        val viewModel = LoginViewModel(auth)

        viewModel.onAction(LoginAction.OnSignInClick)
        advanceUntilIdle()

        assertEquals(UiText.Res(R.string.login_cancelled), viewModel.state.value.error)
        assertFalse(viewModel.state.value.isSigningIn)
        assertEquals(0, auth.bootstrapCalls)
    }

    @Test
    fun secondTapWhileSigningInIsIgnored() = runTest(main.dispatcher) {
        val auth = FakeAuth()
        val viewModel = LoginViewModel(auth)

        viewModel.onAction(LoginAction.OnSignInClick)
        assertTrue(viewModel.state.value.isSigningIn)
        viewModel.onAction(LoginAction.OnSignInClick)
        advanceUntilIdle()

        assertEquals(1, auth.signInCalls)
    }
}
