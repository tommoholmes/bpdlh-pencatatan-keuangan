package com.bpdlh.pencatatankeuangan.ui.login

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bpdlh.pencatatankeuangan.AppContainer
import com.bpdlh.pencatatankeuangan.R
import com.bpdlh.pencatatankeuangan.ui.UiText
import com.bpdlh.pencatatankeuangan.ui.asString
import com.bpdlh.pencatatankeuangan.ui.components.PrimaryAction
import com.bpdlh.pencatatankeuangan.ui.theme.PencatatanTheme

@Composable
fun LoginRoot(
    container: AppContainer,
    viewModel: LoginViewModel = viewModel { LoginViewModel(container.auth) },
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
    ) {
        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.weight(1f))

        Column(modifier = Modifier.fillMaxWidth()) {
            state.error?.let {
                Text(
                    text = it.asString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(12.dp))
            }
            PrimaryAction(
                text = stringResource(R.string.login_cta),
                onClick = { onAction(LoginAction.OnSignInClick) },
                loading = state.isSigningIn,
            )
        }

        Spacer(Modifier.weight(1f))
    }
}

@Preview
@Composable
private fun LoginScreenPreview() {
    PencatatanTheme {
        LoginScreen(
            state = LoginState(error = UiText.Res(R.string.login_failed)),
            onAction = {},
        )
    }
}
