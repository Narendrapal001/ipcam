package com.example.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.camera.CameraStreamManager
import com.example.ui.components.GlassBox
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassIconButton
import com.example.ui.components.LivePulseIndicator
import com.example.ui.components.StreamSidebarDrawerContent
import com.example.ui.components.ViewfinderOverlay
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberIndigo
import com.example.ui.theme.CyberRose
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderActive
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GlassDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.StreamViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CameraStreamScreen(
    viewModel: StreamViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val settings by viewModel.settings.collectAsState()
    val serverState by viewModel.serverState.collectAsState()
    val isDrawerOpen by viewModel.isDrawerOpen.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var tapFocusPoint by remember { mutableStateOf<Offset?>(null) }
    var copiedRecently by remember { mutableStateOf(false) }

    // Camera Stream Manager
    val cameraManager = remember {
        CameraStreamManager(
            context = context,
            lifecycleOwner = lifecycleOwner,
            onFrameAvailable = { frameBytes ->
                viewModel.broadcastFrame(frameBytes)
            }
        )
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            cameraManager.release()
        }
    }

    // Sync ViewModel callbacks with CameraStreamManager
    LaunchedEffect(cameraManager) {
        viewModel.onCameraTorchToggled = { enabled ->
            cameraManager.setTorch(enabled)
        }
        viewModel.onCameraFlipped = {
            cameraManager.switchCamera()
        }
        viewModel.onCameraSettingsChanged = { preset, fps, quality, isBack ->
            cameraManager.updateSettings(preset, fps, quality, isBack)
        }
    }

    LaunchedEffect(serverState.isStreaming) {
        cameraManager.setStreamingActive(serverState.isStreaming)
    }

    // Camera Flip Rotation Animation
    val cameraFlipRotation by animateFloatAsState(
        targetValue = if (settings.isBackCamera) 0f else 180f,
        animationSpec = spring(stiffness = 300f),
        label = "camera_flip_rotation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCanvas)
    ) {
        if (!hasCameraPermission) {
            // Permission Request View
            PermissionRequiredCard(
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            )
        } else {
            // Live Camera Preview Viewport
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            tapFocusPoint = offset
                            previewViewRef?.let { pv ->
                                cameraManager.focusOnPoint(
                                    offset.x,
                                    offset.y,
                                    pv.width.toFloat(),
                                    pv.height.toFloat()
                                )
                            }
                        }
                    }
            ) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            previewViewRef = this
                            cameraManager.bindCameraUseCases(this)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Tech Viewfinder Overlay
                ViewfinderOverlay()

                // Focus tap ring animation
                tapFocusPoint?.let { point ->
                    FocusRingIndicator(
                        point = point,
                        onDismiss = { tapFocusPoint = null }
                    )
                }
            }

            // Top Glass Header Bar
            TopGlassAppBar(
                isStreaming = serverState.isStreaming,
                connectedClients = serverState.connectedClients,
                currentFps = serverState.currentFps,
                bitrateKbps = serverState.bitrateKbps,
                onOpenSidebar = { viewModel.setDrawerOpen(true) },
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .align(Alignment.TopCenter)
            )

            // Floating Central URL Badge (Fast access for laptop)
            FloatingUrlCard(
                streamUrl = serverState.streamUrl,
                isStreaming = serverState.isStreaming,
                copiedRecently = copiedRecently,
                onCopy = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = ClipData.newPlainText("CamStream URL", serverState.streamUrl)
                    clipboard?.setPrimaryClip(clip)
                    copiedRecently = true
                    coroutineScope.launch {
                        delay(2000)
                        copiedRecently = false
                    }
                },
                onShare = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Connect to my phone camera stream: ${serverState.streamUrl}"
                        )
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share CamStream URL"))
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 76.dp, start = 16.dp, end = 16.dp)
            )

            // Bottom Floating Glass Control Dock
            BottomGlassControlDock(
                isStreaming = serverState.isStreaming,
                isTorchOn = settings.isTorchOn,
                isBackCamera = settings.isBackCamera,
                flipRotation = cameraFlipRotation,
                onToggleStreaming = { viewModel.toggleStreaming() },
                onToggleTorch = { viewModel.toggleTorch() },
                onFlipCamera = { viewModel.switchCamera() },
                onOpenSidebar = { viewModel.setDrawerOpen(true) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            )
        }

        // Animated Glass Scrim when sidebar is open
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { viewModel.setDrawerOpen(false) }
                    )
            )
        }

        // Sliding Glassmorphic Settings Sidebar
        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = spring(stiffness = 400f)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = spring(stiffness = 400f)
            ),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            StreamSidebarDrawerContent(
                settings = settings,
                serverState = serverState,
                onResolutionChange = { viewModel.setResolution(it) },
                onFpsChange = { viewModel.setFps(it) },
                onQualityChange = { viewModel.setJpegQuality(it) },
                onCloseDrawer = { viewModel.setDrawerOpen(false) }
            )
        }
    }
}

@Composable
private fun TopGlassAppBar(
    isStreaming: Boolean,
    connectedClients: Int,
    currentFps: Double,
    bitrateKbps: Double,
    onOpenSidebar: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassBox(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        backgroundColor = GlassDark
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Sidebar menu trigger
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassIconButton(
                    icon = Icons.Default.Menu,
                    contentDescription = "Open Quality & FPS Sidebar Menu",
                    onClick = onOpenSidebar,
                    size = 40.dp,
                    testTag = "open_sidebar_button"
                )

                // Live Stream status indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (isStreaming) CyberEmerald.copy(alpha = 0.15f)
                            else Color(0x331E293B)
                        )
                        .border(
                            1.dp,
                            if (isStreaming) CyberEmerald.copy(alpha = 0.4f)
                            else GlassBorder,
                            CircleShape
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    LivePulseIndicator(isLive = isStreaming)
                    Text(
                        text = if (isStreaming) "LIVE" else "STANDBY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = if (isStreaming) CyberEmerald else TextMuted
                    )
                }
            }

            // Real-time Stats chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Viewers pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x331E293B))
                        .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "👁 $connectedClients",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = if (connectedClients > 0) CyberCyan else TextMuted
                    )
                }

                // FPS & Bitrate pill
                if (isStreaming) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x331E293B))
                            .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${currentFps.toInt()} FPS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberCyan
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingUrlCard(
    streamUrl: String,
    isStreaming: Boolean,
    copiedRecently: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassBox(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color(0xCC0D1322),
        borderColor = if (isStreaming) CyberCyan.copy(alpha = 0.6f) else GlassBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "OPEN IN LAPTOP BROWSER",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = TextMuted
                )
                Text(
                    text = streamUrl,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isStreaming) CyberCyan else TextSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GlassIconButton(
                    icon = if (copiedRecently) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = "Copy Stream URL",
                    onClick = onCopy,
                    isActive = copiedRecently,
                    activeColor = CyberEmerald,
                    size = 38.dp,
                    testTag = "floating_copy_button"
                )

                GlassIconButton(
                    icon = Icons.Default.Share,
                    contentDescription = "Share Stream URL",
                    onClick = onShare,
                    size = 38.dp,
                    testTag = "floating_share_button"
                )
            }
        }
    }
}

@Composable
private fun BottomGlassControlDock(
    isStreaming: Boolean,
    isTorchOn: Boolean,
    isBackCamera: Boolean,
    flipRotation: Float,
    onToggleStreaming: () -> Unit,
    onToggleTorch: () -> Unit,
    onFlipCamera: () -> Unit,
    onOpenSidebar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playStopBg by animateColorAsState(
        targetValue = if (isStreaming) CyberRose else CyberCyan,
        animationSpec = spring(stiffness = 300f),
        label = "play_stop_bg"
    )

    GlassBox(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        backgroundColor = GlassDark,
        borderColor = GlassBorder
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sidebar trigger
            GlassIconButton(
                icon = Icons.Default.Tune,
                contentDescription = "Open Quality & FPS Settings",
                onClick = onOpenSidebar,
                size = 52.dp,
                testTag = "bottom_dock_settings_button"
            )

            // Torch Toggle (flashlight)
            GlassIconButton(
                icon = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                contentDescription = "Toggle Torch",
                onClick = onToggleTorch,
                isActive = isTorchOn,
                activeColor = CyberAmber,
                size = 52.dp,
                testTag = "bottom_dock_torch_button"
            )

            // Big Streaming Action Button
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .shadow(16.dp, CircleShape, spotColor = playStopBg)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                playStopBg,
                                playStopBg.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                    .clickable(onClick = onToggleStreaming)
                    .testTag("stream_toggle_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isStreaming) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isStreaming) "Stop Streaming" else "Start Streaming",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            // Flip Camera Toggle
            GlassIconButton(
                icon = Icons.Default.FlipCameraAndroid,
                contentDescription = "Flip Camera",
                onClick = onFlipCamera,
                size = 52.dp,
                modifier = Modifier.rotate(flipRotation),
                testTag = "bottom_dock_flip_camera_button"
            )
        }
    }
}

@Composable
private fun FocusRingIndicator(
    point: Offset,
    onDismiss: () -> Unit
) {
    LaunchedEffect(point) {
        delay(1200)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {}
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.TopStart)
                .padding(start = (point.x - 32).coerceAtLeast(0f).dp, top = (point.y - 32).coerceAtLeast(0f).dp)
                .border(2.dp, CyberCyan, CircleShape)
        )
    }
}

@Composable
private fun PermissionRequiredCard(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCanvas)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassBox(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = GlassDark
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(CyberCyan.copy(alpha = 0.2f))
                        .border(1.5.dp, CyberCyan.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Camera Access Required",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "CamStream needs camera permission to capture and broadcast live video feed directly to your laptop browser.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                GlassButton(
                    text = "Enable Camera Access",
                    icon = Icons.Default.CameraAlt,
                    onClick = onRequestPermission,
                    isActive = true,
                    activeColor = CyberCyan,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "grant_camera_permission_button"
                )
            }
        }
    }
}
