package com.bpdlh.pencatatankeuangan.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Teks yang belum menjadi String. ViewModel tidak punya `Context`, jadi ia hanya menyimpan
 * id resource (atau teks mentah dari exception). Layar yang menerjemahkan lewat [asString].
 */
sealed interface UiText {
    data class Plain(val value: String) : UiText

    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    companion object {
        fun of(@StringRes id: Int, vararg args: Any): UiText = Res(id, args.toList())

        fun of(value: String?): UiText? = value?.let { Plain(it) }
    }
}

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Plain -> value
    is UiText.Res -> if (args.isEmpty()) stringResource(id) else stringResource(id, *args.toTypedArray())
}
