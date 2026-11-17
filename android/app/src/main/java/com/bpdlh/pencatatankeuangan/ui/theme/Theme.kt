package com.bpdlh.pencatatankeuangan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = LedgerGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBE2),
    onPrimaryContainer = Color(0xFF0E3A26),
    secondary = Ink,
    onSecondary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = Sheet,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF2F0EA),
    onSurfaceVariant = Muted,
    outline = Rule,
    outlineVariant = Rule,
    error = DangerBrick,
    onError = Color.White,
)

private val DarkScheme = darkColorScheme(
    primary = LedgerGreenDark,
    onPrimary = Color(0xFF10231A),
    primaryContainer = Color(0xFF1E3A2C),
    onPrimaryContainer = Color(0xFFD6EEDF),
    secondary = InkDark,
    onSecondary = Color(0xFF131518),
    background = PaperDark,
    onBackground = InkDark,
    surface = SheetDark,
    onSurface = InkDark,
    surfaceVariant = Color(0xFF24272C),
    onSurfaceVariant = MutedDark,
    outline = RuleDark,
    outlineVariant = RuleDark,
    error = DangerBrickDark,
    onError = Color(0xFF2A0F0A),
)

/** Warna status tidak ada di ColorScheme Material, jadi diteruskan terpisah. */
data class StatusColors(val pending: Color, val synced: Color, val muted: Color, val rule: Color)

val LocalStatusColors = staticCompositionLocalOf {
    StatusColors(pending = PendingAmber, synced = LedgerGreen, muted = Muted, rule = Rule)
}

@Composable
fun PencatatanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Palet kertas dipakai tetap; dynamic color tidak diaktifkan.
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val status = if (darkTheme) {
        StatusColors(PendingAmberDark, LedgerGreenDark, MutedDark, RuleDark)
    } else {
        StatusColors(PendingAmber, LedgerGreen, Muted, Rule)
    }

    CompositionLocalProvider(LocalStatusColors provides status) {
        MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
    }
}
