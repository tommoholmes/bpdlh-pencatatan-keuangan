package com.bpdlh.pencatatankeuangan.ui.settings

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.bpdlh.pencatatankeuangan.AppContainer
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute : NavKey

fun EntryProviderScope<NavKey>.settingsGraph(
    container: AppContainer,
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
) {
    entry<SettingsRoute> {
        SettingsRoot(container = container, onBack = onBack, onSignedOut = onSignedOut)
    }
}
