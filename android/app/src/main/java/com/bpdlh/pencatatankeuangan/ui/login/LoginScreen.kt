package com.bpdlh.pencatatankeuangan.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bpdlh.pencatatankeuangan.AppContainer
import com.bpdlh.pencatatankeuangan.ui.components.PrimaryAction
import com.bpdlh.pencatatankeuangan.ui.theme.PencatatanTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginState(
    val isSigningIn: Boolean = false,
    val error: String? = null,
)

sealed interface LoginAction {
    data object OnSignInClick : LoginAction
}

class LoginViewModel(private val container: AppContainer) : ViewModel() {
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
            val result = container.auth.signInWithGoogle()
            if (result.isFailure) {
                _state.update {
                    it.copy(
                        isSigningIn = false,
                        error = result.exceptionOrNull()?.message ?: "Login gagal.",
                    )
                }
                return@launch
            }
            container.auth.bootstrap()
            _state.update { it.copy(isSigningIn = false) }
        }
    }
}

@Composable
fun LoginRoot(
    container: AppContainer,
    viewModel: LoginViewModel = viewModel { LoginViewModel(container) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LoginScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun LoginScreen(state: LoginState, onAction: (LoginAction) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = "Pencatatan",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Catat pengeluaran harian: nama, harga, satu foto. Tersimpan di perangkat lebih dulu, terkirim saat ada internet.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            state.error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(12.dp))
            }
            PrimaryAction(
                text = "Masuk dengan Google",
                onClick = { onAction(LoginAction.OnSignInClick) },
                loading = state.isSigningIn,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Login butuh internet sekali. Lokasi dan info perangkat hanya dicatat bila diizinkan.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun LoginScreenPreview() {
    PencatatanTheme {
        LoginScreen(
            state = LoginState(error = "Tidak ada akun Google di perangkat ini."),
            onAction = {},
        )
    }
}
