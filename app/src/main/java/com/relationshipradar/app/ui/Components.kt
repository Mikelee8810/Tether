package com.relationshipradar.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Shape

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import com.relationshipradar.app.engine.RadarStatus
import com.relationshipradar.app.ui.theme.Radar
import com.relationshipradar.app.ui.theme.StatusColors
import com.relationshipradar.app.work.Health

// ---- People as Material shapes -------------------------------------------------------------

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush

// ---- Modern Glass & Squircle Component Architecture ----------------------------------------

/**
 * A person's face. Full natural photo or initials inside a modern frosted squircle.
 * Zero circles, crisp specular border, and vivid masculine status indicator.
 */
@Composable
fun Face(
    id: Long,
    name: String,
    status: RadarStatus,
    avatar: String?,
    lookupKey: String?,
    size: Int = 56,
    modifier: Modifier = Modifier,
    hiRes: Boolean = size >= 96,
    showRing: Boolean = true,
    shape: Shape? = null,
) {
    val photo = if (avatar == "photo") ContactPhotos.remember(lookupKey, hiRes) else null
    val resolvedShape = shape ?: RoundedCornerShape(if (size >= 80) 18.dp else if (size >= 50) 12.dp else 8.dp)

    Box(
        modifier = modifier
            .size(size.dp)
            .shadow(elevation = if (size >= 80) 6.dp else 2.dp, shape = resolvedShape, spotColor = Color(0x180F172A))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.9f), Color(0x150F172A))),
                shape = resolvedShape,
            )
            .clip(resolvedShape)
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            androidx.compose.foundation.Image(
                photo,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        } else {
            Avatar(id = id, name = name, status = status, size = size, shape = resolvedShape, avatar = avatar)
        }
    }
}

/** Curated vivid gemstone glass palettes for rich tactile monograms (Zero sterile dark boxes). */
object GemstonePalettes {
    val Sapphire = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8), Color(0xFF1E3A8A))
    val Amethyst = listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED), Color(0xFF4C1D95))
    val Emerald = listOf(Color(0xFF059669), Color(0xFF047857), Color(0xFF064E3B))
    val Amber = listOf(Color(0xFFF59E0B), Color(0xFFD97706), Color(0xFF78350F))
    val Rose = listOf(Color(0xFFF43F5E), Color(0xFFBE123C), Color(0xFF881337))
    val Coral = listOf(Color(0xFFFB923C), Color(0xFFEA580C), Color(0xFF7C2D12))
    val Cyan = listOf(Color(0xFF06B6D4), Color(0xFF0891B2), Color(0xFF164E63))
    val Violet = listOf(Color(0xFFA855F7), Color(0xFF9333EA), Color(0xFF581C87))
    val Midnight = listOf(Color(0xFF334155), Color(0xFF1E293B), Color(0xFF0F172A))
    val Ruby = listOf(Color(0xFFDC2626), Color(0xFFB91C1C), Color(0xFF7F1D1D))

    val named = listOf(
        "sapphire" to ("Sapphire Blue" to Sapphire),
        "amethyst" to ("Amethyst Purple" to Amethyst),
        "emerald" to ("Emerald Green" to Emerald),
        "amber" to ("Amber Gold" to Amber),
        "rose" to ("Rose Pink" to Rose),
        "coral" to ("Coral Sunset" to Coral),
        "cyan" to ("Electric Cyan" to Cyan),
        "violet" to ("Royal Violet" to Violet),
        "midnight" to ("Midnight Slate" to Midnight),
        "ruby" to ("Crimson Ruby" to Ruby),
    )

    val all = listOf(Sapphire, Amethyst, Emerald, Amber, Rose, Coral, Cyan, Violet, Midnight, Ruby)

    fun byName(name: String): List<Color>? {
        return named.firstOrNull { it.first.equals(name, ignoreCase = true) }?.second?.second
    }

    fun forPerson(id: Long, name: String): List<Color> {
        val hash = kotlin.math.abs(name.hashCode() + id.hashCode().times(31))
        return all[hash % all.size]
    }
}

/** Precision machined luminous gemstone squircle monogram. Zero cartoon emojis, zero circles, zero sterile drab boxes. */
@Composable
fun Avatar(
    id: Long = 0L,
    name: String,
    status: RadarStatus,
    size: Int = 56,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    avatar: String? = null,
) {
    val initials = name.split(' ', '-').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercaseChar().toString() }.ifEmpty { "?" }
    val resolvedShape = shape ?: RoundedCornerShape(if (size >= 80) 18.dp else if (size >= 50) 13.dp else 9.dp)
    val gemstoneColors = remember(id, name, avatar) {
        if (avatar != null && avatar.startsWith("color:")) {
            val colorKey = avatar.removePrefix("color:")
            GemstonePalettes.byName(colorKey) ?: GemstonePalettes.forPerson(id, name)
        } else {
            GemstonePalettes.forPerson(id, name)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(resolvedShape)
            .background(
                Brush.verticalGradient(gemstoneColors)
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.18f),
                        Color.Transparent,
                    )
                ),
                shape = resolvedShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Specular 3D glass gloss arc on upper half
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.50f)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.32f),
                            Color.White.copy(alpha = 0.06f),
                            Color.Transparent,
                        )
                    )
                )
        )
        Text(
            initials,
            style = if (size >= 80) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
            letterSpacing = (-0.5).sp,
        )
    }
}

// ---- Cell Phone Signal Strength Indicator (Zero Circles) -----------------------------------

/**
 * Modern cell phone signal bars representing connection strength:
 * 4 bars = Full signal / In touch (Vivid Emerald)
 * 3 bars = Decent signal / Due soon (Kinetic Amber)
 * 2 bars = Weak signal / Overdue (Vibrant Flame)
 * 1 bar  = Critical / Needs you (High-voltage Flame)
 * 0 bars = Inactive / Quiet (Titanium Slate)
 */
@Composable
fun SignalBars(
    status: RadarStatus,
    modifier: Modifier = Modifier,
    barWidth: androidx.compose.ui.unit.Dp = 3.5.dp,
    spacing: androidx.compose.ui.unit.Dp = 2.dp,
    maxHeight: androidx.compose.ui.unit.Dp = 13.dp,
) {
    val activeBars = when (status) {
        RadarStatus.GOOD -> 4
        RadarStatus.DUE_SOON -> 3
        RadarStatus.OVERDUE -> 2
        RadarStatus.VERY_OVERDUE -> 1
        RadarStatus.TRACK_ONLY, RadarStatus.PAUSED, RadarStatus.SNOOZED -> 0
    }

    val isCritical = status == RadarStatus.VERY_OVERDUE
    val isWarning = status == RadarStatus.OVERDUE

    // Jewel tone gradients per status
    val activeGradient = when (status) {
        RadarStatus.GOOD -> listOf(Color(0xFF34D399), Color(0xFF059669))
        RadarStatus.DUE_SOON -> listOf(Color(0xFFFBBF24), Color(0xFFD97706))
        RadarStatus.OVERDUE -> listOf(Color(0xFFFB923C), Color(0xFFEA580C))
        RadarStatus.VERY_OVERDUE -> listOf(Color(0xFFF87171), Color(0xFFDC2626))
        else -> listOf(Color(0xFF94A3B8), Color(0xFF64748B))
    }

    val infiniteTransition = rememberInfiniteTransition(label = "signal_telemetry")

    // Heartbeat pulse for critical / overdue signal bars
    val heartbeatScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isCritical) 1.28f else if (isWarning) 1.18f else 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isCritical) 600 else if (isWarning) 850 else 1400,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartbeatScale"
    )

    val activeGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isCritical) 600 else if (isWarning) 850 else 1400,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "activeGlowAlpha"
    )

    // Radar scanning wave sweep across bars (searching for signal)
    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isCritical) 1400 else if (isWarning) 1800 else 2400,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepProgress"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.Bottom,
    ) {
        val heights = listOf(0.32f, 0.54f, 0.76f, 1.0f)
        val capsuleShape = RoundedCornerShape(percent = 50)

        heights.forEachIndexed { index, frac ->
            val isActive = index < activeBars
            val baseBarHeight = maxHeight * frac
            val barNorm = index / 3f

            // Proximity to scanning wave
            val sweepDiff = kotlin.math.abs(sweepProgress - barNorm)
            val cyclicDiff = kotlin.math.min(sweepDiff, 1f - sweepDiff)
            val sweepIntensity = (1f - (cyclicDiff / 0.22f)).coerceIn(0f, 1f)

            if (isActive) {
                // Heartbeat dynamic height pulse for active warning bar
                val dynamicHeight = if ((isCritical || isWarning) && index == activeBars - 1) {
                    baseBarHeight * heartbeatScale
                } else {
                    baseBarHeight * (1f + 0.08f * sweepIntensity)
                }

                val barAlpha = (0.75f + 0.25f * sweepIntensity) * activeGlowAlpha
                val specularAlpha = 0.40f + 0.60f * sweepIntensity

                Box(
                    modifier = Modifier
                        .size(width = barWidth, height = dynamicHeight)
                        .border(
                            width = 0.8.dp,
                            color = Color.White.copy(alpha = 0.40f + 0.45f * sweepIntensity),
                            shape = capsuleShape
                        )
                        .clip(capsuleShape)
                        .background(
                            Brush.verticalGradient(
                                activeGradient.map { it.copy(alpha = barAlpha.coerceIn(0f, 1f)) }
                            )
                        )
                ) {
                    // Top gloss reflection
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.45f)
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = specularAlpha),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            } else {
                // Inactive / Searching telemetry bar: lights up sequentially with scanning wave!
                val inactiveAlpha = 0.18f + 0.50f * sweepIntensity
                val inactiveBorderAlpha = 0.22f + 0.60f * sweepIntensity

                Box(
                    modifier = Modifier
                        .size(width = barWidth, height = baseBarHeight)
                        .border(
                            width = 0.8.dp,
                            color = Color.White.copy(alpha = inactiveBorderAlpha),
                            shape = capsuleShape
                        )
                        .clip(capsuleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF64748B).copy(alpha = inactiveAlpha),
                                    Color(0xFF475569).copy(alpha = inactiveAlpha * 0.7f)
                                )
                            )
                        )
                ) {
                    // Soft scanning reflection
                    if (sweepIntensity > 0.05f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.50f)
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.65f * sweepIntensity),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SignalDot(color: Color, size: Int = 10) = Box(
    Modifier
        .size(size.dp)
        .shadow(1.dp, RoundedCornerShape(2.dp))
        .background(color, RoundedCornerShape(2.dp))
)

@Composable fun HealthDot(level: Health.Level) = SignalDot(StatusColors.of(level), 8)

@Composable
fun StatusChip(status: RadarStatus) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .background(StatusColors.container(status), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        SignalBars(status, maxHeight = 10.dp, barWidth = 2.8.dp, spacing = 1.6.dp)
        Text(
            StatusColors.label(status),
            style = MaterialTheme.typography.labelSmall,
            color = StatusColors.onContainer(status),
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        )
    }
}

// ---- Apple Liquid Glass Architecture -------------------------------------------------------

/**
 * Apple Liquid Glass Modifier:
 * Combines specular edge illumination, light-refracting gradient backdrop,
 * and optical elevation to create tangible digital material.
 */
fun Modifier.liquidGlass(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(22.dp),
    elevation: androidx.compose.ui.unit.Dp = 10.dp,
    surfaceAlphaTop: Float = 0.72f,
    surfaceAlphaBottom: Float = 0.42f,
    specularAlphaTop: Float = 0.95f,
    specularAlphaBottom: Float = 0.18f,
    tintColor: Color = Color.White,
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        spotColor = Color(0x1F0F172A),
        ambientColor = Color(0x0C0F172A),
    )
    .border(
        width = 1.2.dp,
        brush = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = specularAlphaTop),
                Color.White.copy(alpha = specularAlphaTop * 0.45f),
                Color.White.copy(alpha = specularAlphaBottom),
                Color(0x180F172A),
            )
        ),
        shape = shape,
    )
    .clip(shape)
    .background(
        Brush.verticalGradient(
            listOf(
                tintColor.copy(alpha = surfaceAlphaTop),
                Color.White.copy(alpha = surfaceAlphaBottom * 0.88f),
                Color(0xFFF1F5F9).copy(alpha = surfaceAlphaBottom),
            )
        )
    )

/** Ambient daylight canvas that provides luminous refraction background for liquid glass panels with living orbital breathing drift. */
@Composable
fun AmbientGlassCanvas(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_drift")
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(
            animation = tween(21000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )
    val breatheAlpha by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(7500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFE8F1FA),
                        Color(0xFFDFEAF7),
                        Color(0xFFD4E2F4),
                        Color(0xFFCADBF0),
                    )
                )
            )
    ) {
        // High-energy luminous ambient blooms that slowly drift and breathe through translucent glass
        Canvas(modifier = Modifier.fillMaxSize()) {
            val drift1X = kotlin.math.cos(phase1) * size.width * 0.05f
            val drift1Y = kotlin.math.sin(phase1) * size.height * 0.035f
            val drift2X = kotlin.math.sin(phase2) * size.width * 0.045f
            val drift2Y = kotlin.math.cos(phase2) * size.height * 0.03f

            // Electric cobalt daylight bloom (top right)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x663B82F6).copy(alpha = (0.42f * breatheAlpha).coerceAtMost(0.60f)),
                        Color(0x2560A5FA),
                        Color.Transparent,
                    ),
                    center = androidx.compose.ui.geometry.Offset(
                        size.width * 0.90f + drift1X,
                        size.height * 0.08f + drift1Y
                    ),
                    radius = size.width * 0.85f,
                ),
                radius = size.width * 0.85f,
                center = androidx.compose.ui.geometry.Offset(
                    size.width * 0.90f + drift1X,
                    size.height * 0.08f + drift1Y
                ),
            )
            // Kinetic amber / flame sunrise bloom (mid-left behind HUD & first card)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x40F97316).copy(alpha = (0.28f * breatheAlpha).coerceAtMost(0.48f)),
                        Color(0x18FBBF24),
                        Color.Transparent,
                    ),
                    center = androidx.compose.ui.geometry.Offset(
                        size.width * 0.05f + drift2X,
                        size.height * 0.28f + drift2Y
                    ),
                    radius = size.width * 0.75f,
                ),
                radius = size.width * 0.75f,
                center = androidx.compose.ui.geometry.Offset(
                    size.width * 0.05f + drift2X,
                    size.height * 0.28f + drift2Y
                ),
            )
            // Frosted emerald freshness bloom (bottom right)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x3510B981).copy(alpha = (0.24f * breatheAlpha).coerceAtMost(0.42f)),
                        Color.Transparent,
                    ),
                    center = androidx.compose.ui.geometry.Offset(
                        size.width * 0.85f - drift1X,
                        size.height * 0.78f - drift2Y
                    ),
                    radius = size.width * 0.70f,
                ),
                radius = size.width * 0.70f,
                center = androidx.compose.ui.geometry.Offset(
                    size.width * 0.85f - drift1X,
                    size.height * 0.78f - drift2Y
                ),
            )
        }
        content()
    }
}

/** Apple-style Liquid Glass Card with specular highlight border and optical elevation. */
@Composable
fun Sheet(modifier: Modifier = Modifier, padding: Int = 0, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(Radar.cardRadius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(shape = shape, elevation = 8.dp)
    ) {
        Column(Modifier.padding(padding.dp), content = content)
    }
}

@Composable
fun SquircleChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) StatusColors.Cobalt else Color.White.copy(alpha = 0.50f)
            )
            .border(
                width = 1.dp,
                color = if (selected) StatusColors.Cobalt else Color.White.copy(alpha = 0.80f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Black else androidx.compose.ui.text.font.FontWeight.SemiBold,
            color = if (selected) Color.White else Color(0xFF334155),
        )
    }
}

@Composable
fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = {
            Text(
                title,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        },
        supportingContent = {
            Text(
                subtitle,
                color = Color(0xFF64748B)
            )
        },
        trailingContent = { Switch(checked, onChange) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
fun LinkRow(title: String, sub: String? = null, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(
                title,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        },
        supportingContent = sub?.let {
            {
                Text(
                    it,
                    color = Color(0xFF64748B)
                )
            }
        },
        trailingContent = {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(13.dp)
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(Radar.sp5.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Radar.sp2.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
        color = Color(0xFF64748B),
        letterSpacing = 1.2.sp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
    )
}

@Composable
fun Hint(text: String, modifier: Modifier = Modifier) =
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = Color(0xFF64748B),
        modifier = modifier
    )

@Composable
fun Hairline(modifier: Modifier = Modifier) = HorizontalDivider(
    modifier = modifier,
    thickness = 0.75.dp,
    color = Color(0x1A0F172A)
)

@Composable
fun OptionDialog(title: String, options: List<String>, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                color = Color(0xFF0F172A)
            )
        },
        text = {
            Column {
                options.forEachIndexed { i, o ->
                    Text(
                        o,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(i) }
                            .padding(vertical = 14.dp)
                    )
                    if (i < options.lastIndex) Hairline()
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF64748B), fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            }
        },
    )
}

