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
    val cardRadius = 28.dp
}

/** Warm, alive status palette that feels encouraging rather than depressing. */
object StatusColors {
    // Curated warm accents so it never looks drab or clinical
    val Emerald = Color(0xFF10B981)       // Healthy, connected (Warm Emerald)
    val Amber = Color(0xFFF59E0B)         // Due soon, gentle nudge (Warm Amber)
    val Coral = Color(0xFFF43F5E)         // Needs love / overdue (Soft Vibrant Coral)
    val Slate = Color(0xFF64748B)         // Calm neutral (Gentle Slate)

    val EmeraldBg = Color(0x1F10B981)
    val AmberBg = Color(0x1FF59E0B)
    val CoralBg = Color(0x1FF43F5E)
    val SlateBg = Color(0x1464748B)

    @Composable fun container(status: RadarStatus): Color = when (status) {
        RadarStatus.GOOD -> EmeraldBg
        RadarStatus.DUE_SOON -> AmberBg
        RadarStatus.OVERDUE, RadarStatus.VERY_OVERDUE -> CoralBg
        RadarStatus.TRACK_ONLY, RadarStatus.PAUSED, RadarStatus.SNOOZED -> SlateBg
    }

    @Composable fun onContainer(status: RadarStatus): Color = when (status) {
        RadarStatus.GOOD -> Emerald
        RadarStatus.DUE_SOON -> Amber
        RadarStatus.OVERDUE, RadarStatus.VERY_OVERDUE -> Coral
        RadarStatus.TRACK_ONLY, RadarStatus.PAUSED, RadarStatus.SNOOZED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    /** Solid accent for dots, rings, and badges. */
    @Composable fun accent(status: RadarStatus): Color = when (status) {
        RadarStatus.GOOD -> Emerald
        RadarStatus.DUE_SOON -> Amber
        RadarStatus.OVERDUE, RadarStatus.VERY_OVERDUE -> Coral
        RadarStatus.TRACK_ONLY, RadarStatus.PAUSED, RadarStatus.SNOOZED -> Slate
    }

    @Composable fun of(level: Health.Level): Color = when (level) {
        Health.Level.OK -> Emerald
        Health.Level.ATTENTION -> Amber
        Health.Level.OFF -> Coral
    }

    fun label(status: RadarStatus): String = when (status) {
        RadarStatus.GOOD -> "In touch"
        RadarStatus.DUE_SOON -> "Nudge soon"
        RadarStatus.OVERDUE -> "Catch up"
        RadarStatus.VERY_OVERDUE -> "Reach out"
        RadarStatus.TRACK_ONLY -> "Quiet radar"
        RadarStatus.PAUSED -> "Paused"
        RadarStatus.SNOOZED -> "Snoozed"
    }
}

// ---- Type: one geometric sans, expressive sizes, tight display tracking ---------------------

val Sans = FontFamily(
    Font(R.font.manrope, FontWeight.Normal, variationSettings = FontVariation.Settings(FontWeight.Normal, FontStyle.Normal)),
    Font(R.font.manrope, FontWeight.Medium, variationSettings = FontVariation.Settings(FontWeight.Medium, FontStyle.Normal)),
    Font(R.font.manrope, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontWeight.SemiBold, FontStyle.Normal)),
    Font(R.font.manrope, FontWeight.Bold, variationSettings = FontVariation.Settings(FontWeight.Bold, FontStyle.Normal)),
    Font(R.font.manrope, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontWeight.ExtraBold, FontStyle.Normal)),
)

private val Type = Typography(
    displayLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 64.sp, lineHeight = 64.sp, letterSpacing = (-2.5).sp),
    displayMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 48.sp, lineHeight = 52.sp, letterSpacing = (-1.5).sp),
    displaySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 42.sp, letterSpacing = (-1).sp),
    headlineLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.4.sp),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RadarTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val scheme: ColorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme()
        else -> expressiveLightColorScheme()
    }
    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        typography = Type,
        content = content,
    )
}
