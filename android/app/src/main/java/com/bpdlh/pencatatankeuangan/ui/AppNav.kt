package com.bpdlh.pencatatankeuangan.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.bpdlh.pencatatankeuangan.AppContainer
import com.bpdlh.pencatatankeuangan.ui.detail.DetailRoute
import com.bpdlh.pencatatankeuangan.ui.detail.detailGraph
import com.bpdlh.pencatatankeuangan.ui.editor.EditorRoute
import com.bpdlh.pencatatankeuangan.ui.editor.editorGraph
import com.bpdlh.pencatatankeuangan.ui.login.LoginRoute
import com.bpdlh.pencatatankeuangan.ui.login.loginGraph
import com.bpdlh.pencatatankeuangan.ui.records.RecordsRoute
import com.bpdlh.pencatatankeuangan.ui.records.recordsGraph
import com.bpdlh.pencatatankeuangan.ui.settings.SettingsRoute
import com.bpdlh.pencatatankeuangan.ui.settings.settingsGraph
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryLevel

@Composable
fun PencatatanNav(container: AppContainer) {
    val start = remember { if (container.auth.currentUser != null) RecordsRoute else LoginRoute }
    val backStack = rememberNavBackStack(start)
    val session by container.auth.session.collectAsStateWithLifecycle(initialValue = container.auth.currentUser)

    LaunchedEffect(session) {
        if (session == null) {
            backStack.clear()
            backStack.add(LoginRoute)
        } else if (backStack.all { it == LoginRoute }) {
            backStack.clear()
            backStack.add(RecordsRoute)
        }
    }

    LaunchedEffect(backStack.lastOrNull()) {
        if (Sentry.isEnabled()) {
            Sentry.addBreadcrumb(
                Breadcrumb().apply {
                    category = "navigation"
                    message = "screen: ${backStack.lastOrNull()?.let { it::class.simpleName }}"
                    level = SentryLevel.INFO
                },
            )
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            loginGraph(container)
            recordsGraph(
                container = container,
                onOpenRecord = { backStack.add(DetailRoute(it)) },
                onAddRecord = { backStack.add(EditorRoute()) },
                onOpenSettings = { backStack.add(SettingsRoute) },
            )
            detailGraph(
                container = container,
                onBack = { backStack.removeLastOrNull() },
                onEdit = { backStack.add(EditorRoute(it)) },
            )
            editorGraph(
                container = container,
                onBack = { backStack.removeLastOrNull() },
                onOpenSettings = { backStack.add(SettingsRoute) },
            )
            settingsGraph(
                container = container,
                onBack = { backStack.removeLastOrNull() },
                onSignedOut = {
                    backStack.clear()
                    backStack.add(LoginRoute)
                },
            )
        },
    )
}
