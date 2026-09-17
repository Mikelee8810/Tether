package com.relationshipradar.app.ui.gamification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.relationshipradar.app.ui.liquidGlass
import com.relationshipradar.app.ui.theme.StatusColors
import kotlinx.coroutines.delay
import kotlin.random.Random

data class Particle(
    val id: Int,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val color: Color
)

@Composable
fun CelebrationParticleOverlay(
    triggerKey: Any?,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (triggerKey == null) return

    val particles = remember(triggerKey) {
        val list = mutableStateListOf<Particle>()
        val colors = listOf(
            Color(0xFFF59E0B), Color(0xFFEA580C), Color(0xFF10B981),
            Color(0xFF6366F1), Color(0xFFEC4899), Color(0xFF38BDF8)
        )
        for (i in 0 until 40) {
            val angle = Random.nextDouble(0.0, Math.PI * 2)
            val speed = Random.nextDouble(180.0, 600.0).toFloat()
            list.add(
                Particle(
                    id = i,
                    x = 500f,
                    y = 900f,
                    vx = (Math.cos(angle) * speed).toFloat(),
                    vy = (Math.sin(angle) * speed).toFloat() - 250f,
                    size = Random.nextFloat() * 14f + 8f,
                    color = colors.random()
                )
            )
        }
        list
    }

    val progress = remember(triggerKey) { Animatable(0f) }

    LaunchedEffect(triggerKey) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
        )
        onFinished()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val p = progress.value
        val alpha = (1f - p).coerceIn(0f, 1f)
        particles.forEach { part ->
            val currX = size.width / 2f + part.vx * p
            val currY = size.height * 0.5f + part.vy * p + (500f * p * p) // gravity
            drawCircle(
                color = part.color.copy(alpha = alpha),
                radius = part.size * (1f - p * 0.5f),
                center = Offset(currX, currY)
            )
        }
    }
}

@Composable
fun OrbitStarlightBanner(
    sparks: Int,
    weeklyCount: Int,
    onOpenGamification: () -> Unit,
    modifier: Modifier = Modifier
) {
    val level = GamificationSystem.getLevelForSparks(sparks)
    val nextLevel = GamificationSystem.LEVELS.getOrNull(level.level)
    val progressInLevel = if (nextLevel != null) {
        val range = (nextLevel.minSparks - level.minSparks).coerceAtLeast(1)
        ((sparks - level.minSparks).toFloat() / range.toFloat()).coerceIn(0f, 1f)
    } else 1f

    val infiniteTransition = rememberInfiniteTransition(label = "starlight_shimmer")
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(18.dp),
                elevation = 4.dp,
                surfaceAlphaTop = 0.88f,
                surfaceAlphaBottom = 0.62f,
                tintColor = Color(0xFFFFFBEB)
            )
            .clickable { onOpenGamification() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Animated Glowing Level Badge
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFF59E0B), Color(0xFFEA580C))
                            )
                        )
                        .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(level.badgeIcon, fontSize = 18.sp)
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = level.title.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFEA580C),
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = "· $sparks Sparks ✨",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    // Progress Bar to Next Level
                    Box(
                        modifier = Modifier
                            .size(width = 120.dp, height = 5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFFE2E8F0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(fraction = progressInLevel)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFF59E0B), Color(0xFFEA580C))
                                    )
                                )
                        )
                    }
                }
            }

            // Weekly Warmth Counter Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFECFDF5))
                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "🔥 $weeklyCount this week",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF059669)
                )
            }
        }
    }
}
