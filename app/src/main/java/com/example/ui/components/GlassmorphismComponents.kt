package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderActive
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GlassHighlight
import com.example.ui.theme.TextPrimary

@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = GlassCard,
    borderColor: Color = GlassBorder,
    borderWidth: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = CyberCyan.copy(alpha = 0.2f)
            )
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = 0.75f),
                        backgroundColor.copy(alpha = 0.50f)
                    )
                )
            )
            .border(
                width = borderWidth,
                brush = Brush.linearGradient(
                    colors = listOf(
                        borderColor.copy(alpha = 0.6f),
                        borderColor.copy(alpha = 0.15f),
                        borderColor.copy(alpha = 0.4f)
                    )
                ),
                shape = shape
            )
    ) {
        // Specular highlight line along top edge
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                GlassHighlight.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        ),
                        start = Offset(20f, 1f),
                        end = Offset(size.width - 20f, 1f),
                        strokeWidth = 1.5f
                    )
                }
        )
        content()
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "glass_icon_button",
    isActive: Boolean = false,
    activeColor: Color = CyberCyan,
    size: Dp = 48.dp,
    shape: Shape = CircleShape
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .testTag(testTag)
            .clip(shape)
            .background(
                brush = if (isActive) {
                    Brush.radialGradient(
                        colors = listOf(
                            activeColor.copy(alpha = 0.35f),
                            GlassCard.copy(alpha = 0.8f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x331E293B),
                            Color(0x220F172A)
                        )
                    )
                }
            )
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) activeColor else GlassBorder,
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = activeColor),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) activeColor else TextPrimary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun GlassButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "glass_button",
    isActive: Boolean = false,
    activeColor: Color = CyberCyan
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .testTag(testTag)
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = if (isActive) {
                    Brush.linearGradient(
                        colors = listOf(
                            activeColor.copy(alpha = 0.3f),
                            activeColor.copy(alpha = 0.15f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x401E293B),
                            Color(0x2A0F172A)
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                color = if (isActive) activeColor.copy(alpha = 0.8f) else GlassBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = activeColor),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) activeColor else TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = if (isActive) activeColor else TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun LivePulseIndicator(
    isLive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier.size(14.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLive) {
            Box(
                modifier = Modifier
                    .size((14 * pulseScale).dp)
                    .clip(CircleShape)
                    .background(CyberEmerald.copy(alpha = pulseAlpha))
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(CyberEmerald)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF64748B))
            )
        }
    }
}

@Composable
fun ViewfinderOverlay(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val cornerLength = 32.dp.toPx()
                val strokeWidth = 2.5.dp.toPx()
                val padding = 28.dp.toPx()
                val color = CyberCyan.copy(alpha = 0.45f)

                // Top Left
                drawLine(color, Offset(padding, padding), Offset(padding + cornerLength, padding), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(padding, padding), Offset(padding, padding + cornerLength), strokeWidth, StrokeCap.Round)

                // Top Right
                drawLine(color, Offset(size.width - padding, padding), Offset(size.width - padding - cornerLength, padding), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(size.width - padding, padding), Offset(size.width - padding, padding + cornerLength), strokeWidth, StrokeCap.Round)

                // Bottom Left
                drawLine(color, Offset(padding, size.height - padding), Offset(padding + cornerLength, size.height - padding), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(padding, size.height - padding), Offset(padding, size.height - padding - cornerLength), strokeWidth, StrokeCap.Round)

                // Bottom Right
                drawLine(color, Offset(size.width - padding, size.height - padding), Offset(size.width - padding - cornerLength, size.height - padding), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(size.width - padding, size.height - padding), Offset(size.width - padding, size.height - padding - cornerLength), strokeWidth, StrokeCap.Round)

                // Center crosshairs
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val crossLength = 8.dp.toPx()
                val crossGap = 6.dp.toPx()
                val crossColor = Color.White.copy(alpha = 0.3f)

                drawLine(crossColor, Offset(centerX - crossGap - crossLength, centerY), Offset(centerX - crossGap, centerY), 1.5f)
                drawLine(crossColor, Offset(centerX + crossGap, centerY), Offset(centerX + crossGap + crossLength, centerY), 1.5f)
                drawLine(crossColor, Offset(centerX, centerY - crossGap - crossLength), Offset(centerX, centerY - crossGap), 1.5f)
                drawLine(crossColor, Offset(centerX, centerY + crossGap), Offset(centerX, centerY + crossGap + crossLength), 1.5f)
            }
    )
}
