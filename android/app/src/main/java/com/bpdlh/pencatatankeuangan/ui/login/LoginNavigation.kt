package com.bpdlh.pencatatankeuangan.ui.login

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.bpdlh.pencatatankeuangan.AppContainer
import kotlinx.serialization.Serializable

@Serializable
data object LoginRoute : NavKey

fun EntryProviderScope<NavKey>.loginGraph(container: AppContainer) {
    entry<LoginRoute> { LoginRoot(container) }
}
