package nl.ecannemeijer.casacheck.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import nl.ecannemeijer.casacheck.data.settings.ThemeMode

private val Green = Color(0xFF1F5C4B)
private val GreenLight = Color(0xFF9ED3BF)
private val Terracotta = Color(0xFFB5643C)

private val LightColors = lightColorScheme(
    primary = Green, onPrimary = Color.White, primaryContainer = Color(0xFFCDEBDF), onPrimaryContainer = Color(0xFF00201A),
    secondary = Terracotta, onSecondary = Color.White, secondaryContainer = Color(0xFFFFDBCB), onSecondaryContainer = Color(0xFF3A1303),
    tertiary = Color(0xFF4A6274), background = Color(0xFFF8FAF8), surface = Color(0xFFF8FAF8),
    surfaceVariant = Color(0xFFDDE5E0), onSurfaceVariant = Color(0xFF414945), error = Color(0xFFB3261E),
)
private val DarkColors = darkColorScheme(
    primary = GreenLight, onPrimary = Color(0xFF003828), primaryContainer = Color(0xFF00513C), onPrimaryContainer = Color(0xFFCDEBDF),
    secondary = Color(0xFFFFB693), onSecondary = Color(0xFF561F04), secondaryContainer = Color(0xFF723418),
    tertiary = Color(0xFFB2CBDF), background = Color(0xFF101413), surface = Color(0xFF101413),
    surfaceVariant = Color(0xFF404945), onSurfaceVariant = Color(0xFFC0C9C3),
)

/** Status colours (always combined with an icon and label, never colour alone). */
object StatusColors {
    val ok = Color(0xFF2E7D32)
    val minor = Color(0xFFF9A825)
    val major = Color(0xFFEF6C00)
    val serious = Color(0xFFC62828)
    val specialist = Color(0xFF6A1B9A)
    val neutral = Color(0xFF757575)
    val unsure = Color(0xFF455A64)
}

private val AppTypography = Typography().let { t ->
    t.copy(
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
    )
}

@Composable
fun CasaTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.DARK -> true; ThemeMode.LIGHT -> false }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = AppTypography, content = content)
    @Suppress("UNUSED_VARIABLE") val unused = Build.VERSION.SDK_INT
}
