package com.bpdlh.pencatatankeuangan.ui.records

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.bpdlh.pencatatankeuangan.AppContainer
import kotlinx.serialization.Serializable

@Serializable
data object RecordsRoute : NavKey

fun EntryProviderScope<NavKey>.recordsGraph(
    container: AppContainer,
    onOpenRecord: (String) -> Unit,
    onAddRecord: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    entry<RecordsRoute> {
        RecordsRoot(
            container = container,
            onOpenRecord = onOpenRecord,
            onAddRecord = onAddRecord,
            onOpenSettings = onOpenSettings,
        )
    }
}
