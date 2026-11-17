package com.bpdlh.pencatatankeuangan.ui.editor

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.bpdlh.pencatatankeuangan.AppContainer
import kotlinx.serialization.Serializable

@Serializable
data class EditorRoute(val id: String? = null) : NavKey

fun EntryProviderScope<NavKey>.editorGraph(
    container: AppContainer,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    entry<EditorRoute> { route ->
        EditorRoot(
            container = container,
            recordId = route.id,
            onBack = onBack,
            onOpenSettings = onOpenSettings,
        )
    }
}
