package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ResolutionPreset
import com.example.model.StreamQualitySettings
import com.example.model.StreamServerState
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberIndigo
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderActive
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GlassDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StreamSidebarDrawerContent(
    settings: StreamQualitySettings,
    serverState: StreamServerState,
    onResolutionChange: (ResolutionPreset) -> Unit,
    onFpsChange: (Int) -> Unit,
    onQualityChange: (Int) -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var copiedRecently by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .widthIn(max = 380.dp)
            .fillMaxWidth(0.88f)
            .shadow(24.dp, shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
            .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xF00D1322),
                        Color(0xF5090D16)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GlassBorderActive.copy(alpha = 0.5f),
                        GlassBorder.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(CyberCyan.copy(alpha = 0.25f), CyberIndigo.copy(alpha = 0.25f))
                                )
                            )
                            .border(1.dp, GlassBorderActive.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Stream Settings",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Quality & Frame Rate",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }

                GlassIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = "Close Settings",
                    onClick = onCloseDrawer,
                    size = 38.dp,
                    testTag = "close_sidebar_button"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 1: Resolution Presets
            SectionHeader(
                icon = Icons.Default.HighQuality,
                title = "Resolution Quality",
                badge = settings.resolution.title
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ResolutionPreset.values().forEach { preset ->
                    val isSelected = settings.resolution == preset
                    ResolutionCard(
                        preset = preset,
                        isSelected = isSelected,
                        onClick = { onResolutionChange(preset) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2: FPS Frame Rate
            SectionHeader(
                icon = Icons.Default.Speed,
                title = "Frame Rate (FPS)",
                badge = "${settings.targetFps} FPS"
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(15, 24, 30, 60).forEach { fps ->
                    val isSelected = settings.targetFps == fps
                    val animBg by animateColorAsState(
                        targetValue = if (isSelected) CyberCyan.copy(alpha = 0.25f) else GlassCard,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "fps_bg"
                    )
                    val animBorder by animateColorAsState(
                        targetValue = if (isSelected) CyberCyan else GlassBorder,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "fps_border"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(animBg)
                            .border(1.dp, animBorder, RoundedCornerShape(12.dp))
                            .clickable { onFpsChange(fps) }
                            .testTag("fps_option_$fps"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$fps",
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) CyberCyan else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 3: Compression Quality
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "JPEG Compression",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "${settings.jpegQuality}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CyberCyan
                )
            }

            Slider(
                value = settings.jpegQuality.toFloat(),
                onValueChange = { onQualityChange(it.toInt()) },
                valueRange = 30f..95f,
                steps = 12,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("compression_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = CyberCyan,
                    activeTrackColor = CyberCyan,
                    inactiveTrackColor = Color(0x3338BDF8)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Lower Bandwidth", fontSize = 11.sp, color = TextMuted)
                Text(text = "Maximum Clarity", fontSize = 11.sp, color = TextMuted)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 4: Network & Connection Guide
            SectionHeader(
                icon = Icons.Default.Wifi,
                title = "Laptop Browser Link",
                badge = if (serverState.isStreaming) "ONLINE" else "OFFLINE"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // IP URL Glass Card
            GlassBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                backgroundColor = Color(0x40111827),
                borderColor = if (serverState.isStreaming) CyberCyan.copy(alpha = 0.5f) else GlassBorder
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "URL FOR BROWSER",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            LivePulseIndicator(isLive = serverState.isStreaming)
                            Text(
                                text = "${serverState.connectedClients} active ${if (serverState.connectedClients == 1) "viewer" else "viewers"}",
                                fontSize = 11.sp,
                                color = if (serverState.connectedClients > 0) CyberEmerald else TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = serverState.streamUrl,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (serverState.isStreaming) CyberCyan else TextSecondary
                        )

                        GlassIconButton(
                            icon = if (copiedRecently) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy URL",
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = ClipData.newPlainText("CamStream URL", serverState.streamUrl)
                                clipboard?.setPrimaryClip(clip)
                                copiedRecently = true
                                coroutineScope.launch {
                                    delay(2000)
                                    copiedRecently = false
                                }
                            },
                            isActive = copiedRecently,
                            activeColor = CyberEmerald,
                            size = 36.dp,
                            testTag = "copy_url_button"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Instructions Guide Box
            GlassBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                backgroundColor = Color(0x221E293B)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = CyberIndigo,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "How to view on your laptop:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    InstructionStep(number = "1", text = "Connect laptop to the same Wi-Fi or phone hotspot")
                    InstructionStep(number = "2", text = "Open Chrome, Edge, or Firefox on your laptop")
                    InstructionStep(number = "3", text = "Type ${serverState.streamUrl} in the address bar")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    badge: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CyberCyan,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(CyberCyan.copy(alpha = 0.15f))
                .border(1.dp, CyberCyan.copy(alpha = 0.3f), CircleShape)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = badge,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyberCyan
            )
        }
    }
}

@Composable
private fun ResolutionCard(
    preset: ResolutionPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val animBg by animateColorAsState(
        targetValue = if (isSelected) CyberCyan.copy(alpha = 0.15f) else Color(0x331E293B),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "res_bg"
    )
    val animBorder by animateColorAsState(
        targetValue = if (isSelected) CyberCyan else GlassBorder,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "res_border"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(animBg)
            .border(width = if (isSelected) 1.5.dp else 1.dp, color = animBorder, shape = RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("res_preset_${preset.name}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = preset.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) CyberCyan else TextPrimary
                    )
                    Text(
                        text = "${preset.width}×${preset.height}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                }
                Text(
                    text = preset.subtitle,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(CyberCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = DarkCanvas,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InstructionStep(
    number: String,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(CyberIndigo.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = CyberCyan
            )
        }
        Text(
            text = text,
            fontSize = 12.sp,
            color = TextSecondary,
            lineHeight = 16.sp
        )
    }
}
