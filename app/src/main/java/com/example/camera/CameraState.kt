package com.example.camera

import android.net.Uri
import androidx.camera.core.CameraSelector

data class CameraState(
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val selectedMode: CameraMode = CameraMode.PHOTO,
    val flashMode: FlashMode = FlashMode.AUTO,
    val aspectRatio: AspectRatioMode = AspectRatioMode.RATIO_4_3,
    val hasFlashUnit: Boolean = true,
    val isTorchOn: Boolean = false,
    val zoomRatio: Float = 1.0f,
    val minZoomRatio: Float = 1.0f,
    val maxZoomRatio: Float = 10.0f,
    val linearZoom: Float = 0.0f,
    val exposureIndex: Int = 0,
    val minExposureIndex: Int = -4,
    val maxExposureIndex: Int = 4,
    val exposureStep: Float = 0.5f,
    val isExposureControlVisible: Boolean = false,
    val focusPoint: Pair<Float, Float>? = null,
    val isCapturing: Boolean = false,
    val isProcessing: Boolean = false,
    val isRecording: Boolean = false,
    val isRecordingPaused: Boolean = false,
    val recordingDurationSeconds: Long = 0L,
    val timerSeconds: Int = 0, // 0 = off, 3 = 3s, 10 = 10s
    val timerCountdown: Int? = null,
    val isGridEnabled: Boolean = false,
    val portraitAperture: Float = 2.8f, // simulated f/1.4 - f/16
    val nightHoldSeconds: Int = 2, // simulated night exposure hold
    val nightProgress: Float? = null,
    val lastMediaThumbnailUri: Uri? = null,
    val activeExtensionName: String? = null,
    val errorMessage: String? = null
)
