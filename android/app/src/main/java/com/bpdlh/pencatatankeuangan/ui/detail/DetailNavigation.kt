package com.bpdlh.pencatatankeuangan.ui.detail

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.bpdlh.pencatatankeuangan.AppContainer
import kotlinx.serialization.Serializable

@Serializable
data class DetailRoute(val id: String) : NavKey

fun EntryProviderScope<NavKey>.detailGraph(
    container: AppContainer,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
) {
    entry<DetailRoute> { route ->
        DetailRoot(
            container = container,
            recordId = route.id,
            onBack = onBack,
            onEdit = { onEdit(route.id) },
        )
    }
}
