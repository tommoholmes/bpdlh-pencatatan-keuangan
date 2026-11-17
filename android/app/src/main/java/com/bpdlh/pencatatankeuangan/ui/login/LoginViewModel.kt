package com.bpdlh.pencatatankeuangan.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bpdlh.pencatatankeuangan.R
import com.bpdlh.pencatatankeuangan.data.AuthException
import com.bpdlh.pencatatankeuangan.data.AuthGateway
import com.bpdlh.pencatatankeuangan.ui.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginState(
    val isSigningIn: Boolean = false,
    val error: UiText? = null,
)

sealed interface LoginAction {
    data object OnSignInClick : LoginAction
}

class LoginViewModel(private val auth: AuthGateway) : ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun onAction(action: LoginAction) {
        when (action) {
            LoginAction.OnSignInClick -> signIn()
        }
    }

    private fun signIn() {
        if (_state.value.isSigningIn) return
        _state.update { it.copy(isSigningIn = true, error = null) }
        viewModelScope.launch {
            val result = auth.signInWithGoogle()
            if (result.isFailure) {
                _state.update {
                    it.copy(
                        isSigningIn = false,
                        error = (result.exceptionOrNull() as? AuthException)?.failure?.uiText
                            ?: UiText.Res(R.string.login_failed),
                    )
                }
                return@launch
            }
            auth.bootstrap()
            _state.update { it.copy(isSigningIn = false) }
        }
    }
}
