package com.relationshipradar.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relationshipradar.app.R
import com.relationshipradar.app.engine.RadarStatus
import com.relationshipradar.app.work.Health

/*
 * Relationship Radar — visual system v3, "native expressive".
 *
 * The app looks like it shipped with the phone: Material 3 Expressive, colour pulled from the
 * user's wallpaper, one geometric sans at expressive sizes, large collapsing titles, spring
 * motion, and Material's shape language for people. No metaphor, no decoration — the content
 * (people, time) is the design.
 */

object Radar {
    const val sp1 = 4
    const val sp2 = 8
    const val sp3 = 16
    const val sp4 = 24
    const val sp5 = 32
    const val fabClearance = 120
    val cardRadius = 18.dp
    val avatarRadius = 14.dp
}

/** Vibrant, masculine status palette with high energy and contrast. */
object StatusColors {
    val Emerald = Color(0xFF059669)       // Deep precision emerald (In touch)
    val Amber = Color(0xFFD97706)         // Rich kinetic amber (Due soon)
    val Flame = Color(0xFFEA580C)         // High-voltage burnt orange / flame (Overdue)
    val Cobalt = Color(0xFF2563EB)        // Electric royal cobalt (Primary action)
    val Slate = Color(0xFF475569)         // Machined titanium slate (Neutral)

    val EmeraldBg = Color(0x1A059669)
    val AmberBg = Color(0x1AD97706)
    val FlameBg = Color(0x1AEA580C)
    val SlateBg = Color(0x12475569)

    @Composable fun container(status: RadarStatus): Color = when (status) {
        RadarStatus.GOOD -> EmeraldBg
        RadarStatus.DUE_SOON -> AmberBg
        RadarStatus.OVERDUE, RadarStatus.VERY_OVERDUE -> FlameBg
        RadarStatus.TRACK_ONLY, RadarStatus.PAUSED, RadarStatus.SNOOZED -> SlateBg
    }

    @Composable fun onContainer(status: RadarStatus): Color = when (status) {
        RadarStatus.GOOD -> Emerald
        RadarStatus.DUE_SOON -> Amber
        RadarStatus.OVERDUE, RadarStatus.VERY_OVERDUE -> Flame
        RadarStatus.TRACK_ONLY, RadarStatus.PAUSED, RadarStatus.SNOOZED -> Slate
    }

    /** Solid accent for badges and status borders. */
    @Composable fun accent(status: RadarStatus): Color = when (status) {
        RadarStatus.GOOD -> Emerald
        RadarStatus.DUE_SOON -> Amber
        RadarStatus.OVERDUE, RadarStatus.VERY_OVERDUE -> Flame
        RadarStatus.TRACK_ONLY, RadarStatus.PAUSED, RadarStatus.SNOOZED -> Slate
    }

    @Composable fun of(level: Health.Level): Color = when (level) {
        Health.Level.OK -> Emerald
        Health.Level.ATTENTION -> Amber
        Health.Level.OFF -> Flame
    }

    fun label(status: RadarStatus): String = when (status) {
        RadarStatus.GOOD -> "In touch"
        RadarStatus.DUE_SOON -> "Up next"
        RadarStatus.OVERDUE -> "Catch up"
        RadarStatus.VERY_OVERDUE -> "Reconnect"
        RadarStatus.TRACK_ONLY -> "Quiet"
        RadarStatus.PAUSED -> "Paused"
        RadarStatus.SNOOZED -> "Snoozed"
    }
}

// ---- Type: high-contrast modern geometric sans & editorial serif ---------------------------

val EditorialSerif = FontFamily(
    Font(R.font.instrument_serif_regular, FontWeight.Normal),
    Font(R.font.instrument_serif_italic, FontWeight.Normal, FontStyle.Italic),
)

val Sans = FontFamily(
    Font(R.font.manrope, FontWeight.Normal, variationSettings = FontVariation.Settings(FontWeight.Normal, FontStyle.Normal)),
    Font(R.font.manrope, FontWeight.Medium, variationSettings = FontVariation.Settings(FontWeight.Medium, FontStyle.Normal)),
    Font(R.font.manrope, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontWeight.SemiBold, FontStyle.Normal)),
    Font(R.font.manrope, FontWeight.Bold, variationSettings = FontVariation.Settings(FontWeight.Bold, FontStyle.Normal)),
    Font(R.font.manrope, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontWeight.ExtraBold, FontStyle.Normal)),
)

private val Type = Typography(
    displayLarge = TextStyle(fontFamily = EditorialSerif, fontWeight = FontWeight.Normal, fontSize = 58.sp, lineHeight = 62.sp, letterSpacing = (-1.0).sp),
    displayMedium = TextStyle(fontFamily = EditorialSerif, fontWeight = FontWeight.Normal, fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-0.8).sp),
    displaySmall = TextStyle(fontFamily = EditorialSerif, fontWeight = FontWeight.Normal, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontFamily = EditorialSerif, fontWeight = FontWeight.Normal, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontFamily = EditorialSerif, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.2).sp),
    headlineSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = (-0.1).sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 19.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, lineHeight = 17.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 11.5.sp, lineHeight = 15.sp, letterSpacing = 0.5.sp),
)

// Crisp daylight palette with high-contrast Obsidian text & frosted glass surfaces
private val VibrantLightColorScheme = ColorScheme(
    primary = Color(0xFF2563EB),             // Electric Cobalt
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    inversePrimary = Color(0xFF93C5FD),
    secondary = Color(0xFF334155),           // Titanium Slate
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = Color(0xFFEA580C),            // Vibrant Flame
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFEDD5),
    onTertiaryContainer = Color(0xFF7C2D12),
    background = Color(0xFFF1F5F9),          // Ambient Cool-Alabaster Canvas
    onBackground = Color(0xFF0F172A),        // Obsidian Charcoal
    surface = Color(0xFFFFFFFF),             // Frosted Pure White
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    surfaceTint = Color(0xFF2563EB),
    inverseSurface = Color(0xFF1E293B),
    inverseOnSurface = Color(0xFFF8FAFC),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    scrim = Color(0xFF000000),
    surfaceBright = Color.White,
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF8FAFC),
    surfaceContainerHighest = Color(0xFFF1F5F9),
    surfaceContainerLow = Color(0xFFFAFAFA),
    surfaceContainerLowest = Color.White,
    surfaceDim = Color(0xFFE2E8F0),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RadarTheme(content: @Composable () -> Unit) {
    MaterialExpressiveTheme(
        colorScheme = VibrantLightColorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = Type,
        content = content,
    )
}
