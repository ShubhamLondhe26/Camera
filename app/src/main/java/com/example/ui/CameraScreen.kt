package com.example.ui

import android.view.ScaleGestureDetector
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.camera.AspectRatioMode
import com.example.camera.CameraMode
import com.example.camera.CameraViewModel
import com.example.ui.components.BottomControls
import com.example.ui.components.CameraModeSelector
import com.example.ui.components.FocusIndicator
import com.example.ui.components.GalleryViewerDialog
import com.example.ui.components.GridOverlay
import com.example.ui.components.NightCountdownOverlay
import com.example.ui.components.PermissionHandler
import com.example.ui.components.PortraitControlBar
import com.example.ui.components.SettingsBottomSheet
import com.example.ui.components.TimerCountdownOverlay
import com.example.ui.components.TopBarControls
import com.example.ui.components.VideoRecordingOverlay
import com.example.ui.components.ZoomControls
import com.example.ui.theme.CameraAccentYellow
import com.example.ui.theme.CameraBlack

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = viewModel()
) {
    PermissionHandler {
        CameraContent(viewModel = viewModel)
    }
}

@Composable
private fun CameraContent(
    viewModel: CameraViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsState()

    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isGalleryOpen by remember { mutableStateOf(false) }

    // Flash screen effect on capture
    var isShutterFlashVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.isCapturing) {
        if (state.isCapturing && state.selectedMode != CameraMode.NIGHT) {
            isShutterFlashVisible = true
            kotlinx.coroutines.delay(80)
            isShutterFlashVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CameraBlack)
    ) {
        // Main Viewfinder container
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            val containerModifier = when (state.aspectRatio) {
                AspectRatioMode.RATIO_4_3 -> Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                AspectRatioMode.RATIO_16_9 -> Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
                AspectRatioMode.RATIO_1_1 -> Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                AspectRatioMode.RATIO_FULL -> Modifier.fillMaxSize()
            }

            Box(
                modifier = containerModifier
                    .pointerInput(previewViewRef) {
                        detectTapGestures { offset ->
                            previewViewRef?.let { pv ->
                                viewModel.onFocusTap(pv, offset.x, offset.y)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // CameraX PreviewView
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.PERFORMANCE

                            val scaleGestureDetector = ScaleGestureDetector(
                                ctx,
                                object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                                        val currentZoom = state.zoomRatio
                                        val delta = detector.scaleFactor
                                        viewModel.setZoomRatio(currentZoom * delta)
                                        return true
                                    }
                                }
                            )

                            setOnTouchListener { _, event ->
                                scaleGestureDetector.onTouchEvent(event)
                                false
                            }

                            previewViewRef = this
                            viewModel.bindCamera(lifecycleOwner, this)
                        }
                    },
                    update = { view ->
                        previewViewRef = view
                    }
                )

                // 3x3 Rule of Thirds Grid Overlay
                if (state.isGridEnabled) {
                    GridOverlay()
                }

                // Tap-to-Focus Indicator & Exposure Slider
                state.focusPoint?.let { point ->
                    FocusIndicator(
                        focusPoint = point,
                        isExposureVisible = state.isExposureControlVisible,
                        exposureIndex = state.exposureIndex,
                        minExposureIndex = state.minExposureIndex,
                        maxExposureIndex = state.maxExposureIndex,
                        onExposureChanged = { idx -> viewModel.setExposure(idx) }
                    )
                }

                // Screen Flash feedback during capture
                AnimatedVisibility(
                    visible = isShutterFlashVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.85f))
                    )
                }
            }
        }

        // Top Controls: Flash, Timer, Aspect Ratio, Grid, Settings
        TopBarControls(
            flashMode = state.flashMode,
            hasFlash = state.hasFlashUnit,
            timerSeconds = state.timerSeconds,
            aspectRatio = state.aspectRatio,
            isGridEnabled = state.isGridEnabled,
            onFlashToggle = { previewViewRef?.let { viewModel.toggleFlashMode(lifecycleOwner, it) } },
            onTimerToggle = { viewModel.cycleTimer() },
            onAspectRatioToggle = { viewModel.cycleAspectRatio() },
            onGridToggle = { viewModel.toggleGrid() },
            onSettingsClick = { isSettingsOpen = true },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Video Recording Active Bar
        if (state.selectedMode == CameraMode.VIDEO && state.isRecording) {
            VideoRecordingOverlay(
                isRecording = state.isRecording,
                isPaused = state.isRecordingPaused,
                durationSeconds = state.recordingDurationSeconds,
                onPauseToggle = {
                    if (state.isRecordingPaused) viewModel.resumeRecording()
                    else viewModel.pauseRecording()
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp)
            )
        }

        // Bottom Overlays & Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Zoom Controls Pill (0.6x, 1x, 2x, 5x)
            ZoomControls(
                currentZoom = state.zoomRatio,
                minZoom = state.minZoomRatio,
                maxZoom = state.maxZoomRatio,
                onZoomSelected = { stop -> viewModel.setZoomRatio(stop) },
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Portrait Mode Aperture Selector
            if (state.selectedMode == CameraMode.PORTRAIT && !state.isRecording) {
                PortraitControlBar(
                    aperture = state.portraitAperture,
                    onApertureChanged = { viewModel.setPortraitAperture(it) }
                )
            }

            // Mode Selector: PHOTO, PORTRAIT, NIGHT, VIDEO
            CameraModeSelector(
                selectedMode = state.selectedMode,
                onModeSelected = { mode ->
                    previewViewRef?.let { viewModel.onModeSelected(mode, lifecycleOwner, it) }
                }
            )

            // Bottom Controls: Gallery Thumbnail, Shutter Button, Camera Switch
            BottomControls(
                selectedMode = state.selectedMode,
                isCapturing = state.isCapturing,
                isProcessing = state.isProcessing,
                isRecording = state.isRecording,
                lastThumbnailUri = state.lastMediaThumbnailUri,
                onShutterClick = { viewModel.triggerShutter() },
                onSwitchCameraClick = {
                    previewViewRef?.let { viewModel.toggleCameraFacing(lifecycleOwner, it) }
                },
                onGalleryClick = { isGalleryOpen = true }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Timer Countdown Overlay (3... 2... 1...)
        TimerCountdownOverlay(countdown = state.timerCountdown)

        // Night Mode Exposure Hold Overlay
        NightCountdownOverlay(
            progress = state.nightProgress,
            isProcessing = state.isProcessing
        )

        // Error message banner
        state.errorMessage?.let { msg ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp, start = 16.dp, end = 16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss", color = CameraAccentYellow)
                    }
                }
            ) {
                Text(msg, fontSize = 13.sp)
            }
        }

        // Settings Bottom Sheet
        if (isSettingsOpen) {
            SettingsBottomSheet(
                cameraManager = viewModel.getCameraManager(),
                isGridEnabled = state.isGridEnabled,
                onToggleGrid = { viewModel.toggleGrid() },
                onDismiss = { isSettingsOpen = false }
            )
        }

        // Gallery Fullscreen Viewer
        if (isGalleryOpen) {
            GalleryViewerDialog(
                initialUri = state.lastMediaThumbnailUri,
                onDismiss = { isGalleryOpen = false },
                onMediaDeleted = { viewModel.refreshLatestThumbnail() }
            )
        }
    }
}
