package com.example.camera

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.example.gallery.MediaStoreHelper
import com.example.settings.CameraPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(CameraState())
    val state: StateFlow<CameraState> = _state.asStateFlow()

    private val cameraManager = CameraManager(application)
    private val preferences = CameraPreferences(application)

    private var recordingTimerJob: Job? = null
    private var recordingStartTime = 0L
    private var pausedTimeAccumulator = 0L
    private var lastPauseTimestamp = 0L

    private var focusResetJob: Job? = null
    private var timerJob: Job? = null

    init {
        // Load initial preferences
        _state.update {
            it.copy(
                isGridEnabled = preferences.isGridEnabled,
                timerSeconds = preferences.timerSeconds,
                aspectRatio = preferences.aspectRatio
            )
        }

        refreshLatestThumbnail()

        cameraManager.initialize { _, _ ->
            // Initialized
        }
    }

    fun getCameraManager(): CameraManager = cameraManager

    fun refreshLatestThumbnail() {
        viewModelScope.launch {
            val thumb = MediaStoreHelper.fetchLatestMediaThumbnailUri(getApplication())
            _state.update { it.copy(lastMediaThumbnailUri = thumb) }
        }
    }

    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        val current = _state.value
        cameraManager.bindCamera(
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            lensFacing = current.lensFacing,
            mode = current.selectedMode,
            flashMode = current.flashMode,
            onCameraBound = { info ->
                updateCameraInfo(info)
            },
            onError = { err ->
                _state.update { it.copy(errorMessage = err) }
            }
        )
    }

    private fun updateCameraInfo(info: CameraInfo) {
        val hasFlash = info.hasFlashUnit()
        val zoom = info.zoomState.value
        val exposure = info.exposureState

        _state.update {
            it.copy(
                hasFlashUnit = hasFlash,
                zoomRatio = zoom?.zoomRatio ?: 1.0f,
                minZoomRatio = zoom?.minZoomRatio ?: 1.0f,
                maxZoomRatio = zoom?.maxZoomRatio ?: 10.0f,
                linearZoom = zoom?.linearZoom ?: 0.0f,
                exposureIndex = exposure.exposureCompensationIndex,
                minExposureIndex = exposure.exposureCompensationRange.lower,
                maxExposureIndex = exposure.exposureCompensationRange.upper,
                exposureStep = exposure.exposureCompensationStep.toFloat()
            )
        }
    }

    fun onModeSelected(mode: CameraMode, lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        if (_state.value.isRecording) return
        _state.update { it.copy(selectedMode = mode, isExposureControlVisible = false) }
        bindCamera(lifecycleOwner, previewView)
    }

    fun toggleCameraFacing(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        if (_state.value.isRecording) return
        val newFacing = if (_state.value.lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        _state.update { it.copy(lensFacing = newFacing, isExposureControlVisible = false) }
        bindCamera(lifecycleOwner, previewView)
    }

    fun toggleFlashMode(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val nextMode = _state.value.flashMode.next()
        _state.update { it.copy(flashMode = nextMode) }
        // Re-bind to apply flash mode or torch
        bindCamera(lifecycleOwner, previewView)
    }

    fun cycleAspectRatio() {
        val next = _state.value.aspectRatio.next()
        preferences.aspectRatio = next
        _state.update { it.copy(aspectRatio = next) }
    }

    fun cycleTimer() {
        val next = when (_state.value.timerSeconds) {
            0 -> 3
            3 -> 10
            else -> 0
        }
        preferences.timerSeconds = next
        _state.update { it.copy(timerSeconds = next) }
    }

    fun toggleGrid() {
        val next = !_state.value.isGridEnabled
        preferences.isGridEnabled = next
        _state.update { it.copy(isGridEnabled = next) }
    }

    fun onFocusTap(previewView: PreviewView, x: Float, y: Float) {
        cameraManager.tapToFocus(previewView, x, y)
        _state.update {
            it.copy(
                focusPoint = Pair(x, y),
                isExposureControlVisible = true
            )
        }

        focusResetJob?.cancel()
        focusResetJob = viewModelScope.launch {
            delay(3500)
            _state.update { it.copy(focusPoint = null, isExposureControlVisible = false) }
        }
    }

    fun setZoomRatio(ratio: Float) {
        val clamped = ratio.coerceIn(_state.value.minZoomRatio, _state.value.maxZoomRatio)
        cameraManager.setZoomRatio(clamped)
        _state.update { it.copy(zoomRatio = clamped) }
    }

    fun setExposure(index: Int) {
        val clamped = index.coerceIn(_state.value.minExposureIndex, _state.value.maxExposureIndex)
        cameraManager.setExposureCompensation(clamped)
        _state.update { it.copy(exposureIndex = clamped) }
    }

    fun setPortraitAperture(aperture: Float) {
        _state.update { it.copy(portraitAperture = aperture) }
    }

    fun triggerShutter() {
        val currentState = _state.value
        if (currentState.isCapturing || currentState.isProcessing) return

        if (currentState.selectedMode == CameraMode.VIDEO) {
            if (currentState.isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
            return
        }

        // Handle timer countdown
        if (currentState.timerSeconds > 0) {
            timerJob?.cancel()
            timerJob = viewModelScope.launch {
                for (sec in currentState.timerSeconds downTo 1) {
                    _state.update { it.copy(timerCountdown = sec) }
                    delay(1000)
                }
                _state.update { it.copy(timerCountdown = null) }
                executeCapture()
            }
        } else {
            executeCapture()
        }
    }

    private fun executeCapture() {
        val mode = _state.value.selectedMode
        _state.update { it.copy(isCapturing = true) }

        if (mode == CameraMode.NIGHT) {
            // Night mode low-light multi-exposure countdown hold
            viewModelScope.launch {
                val totalSteps = 20
                for (step in 1..totalSteps) {
                    delay(100)
                    _state.update { it.copy(nightProgress = step.toFloat() / totalSteps) }
                }
                _state.update { it.copy(nightProgress = null, isProcessing = true) }

                cameraManager.capturePhotoWithProcessing(
                    mode = CameraMode.NIGHT,
                    portraitAperture = _state.value.portraitAperture,
                    onSuccess = { uri ->
                        onMediaCaptured(uri)
                    },
                    onError = { err ->
                        _state.update { it.copy(isCapturing = false, isProcessing = false, errorMessage = err) }
                    }
                )
            }
        } else if (mode == CameraMode.PORTRAIT) {
            _state.update { it.copy(isProcessing = true) }
            cameraManager.capturePhotoWithProcessing(
                mode = CameraMode.PORTRAIT,
                portraitAperture = _state.value.portraitAperture,
                onSuccess = { uri ->
                    onMediaCaptured(uri)
                },
                onError = { err ->
                    _state.update { it.copy(isCapturing = false, isProcessing = false, errorMessage = err) }
                }
            )
        } else {
            // Standard photo capture directly into MediaStore
            cameraManager.capturePhotoDirect(
                onSuccess = { uri ->
                    onMediaCaptured(uri)
                },
                onError = { err ->
                    _state.update { it.copy(isCapturing = false, errorMessage = err) }
                }
            )
        }
    }

    private fun onMediaCaptured(uri: Uri) {
        _state.update {
            it.copy(
                isCapturing = false,
                isProcessing = false,
                lastMediaThumbnailUri = uri
            )
        }
    }

    private fun startRecording() {
        recordingStartTime = SystemClock.elapsedRealtime()
        pausedTimeAccumulator = 0L
        _state.update { it.copy(isRecording = true, isRecordingPaused = false, recordingDurationSeconds = 0L) }

        startRecordingTimer()

        cameraManager.startVideoRecording(withAudio = true) { event ->
            when (event) {
                is VideoRecordEvent.Finalize -> {
                    stopRecordingTimer()
                    val uri = event.outputResults.outputUri
                    _state.update {
                        it.copy(
                            isRecording = false,
                            isRecordingPaused = false,
                            recordingDurationSeconds = 0L,
                            lastMediaThumbnailUri = if (uri != Uri.EMPTY) uri else it.lastMediaThumbnailUri
                        )
                    }
                }
                is VideoRecordEvent.Pause -> {
                    lastPauseTimestamp = SystemClock.elapsedRealtime()
                    _state.update { it.copy(isRecordingPaused = true) }
                }
                is VideoRecordEvent.Resume -> {
                    if (lastPauseTimestamp > 0L) {
                        pausedTimeAccumulator += (SystemClock.elapsedRealtime() - lastPauseTimestamp)
                    }
                    _state.update { it.copy(isRecordingPaused = false) }
                }
            }
        }
    }

    fun pauseRecording() {
        if (_state.value.isRecording && !_state.value.isRecordingPaused) {
            cameraManager.pauseVideoRecording()
        }
    }

    fun resumeRecording() {
        if (_state.value.isRecording && _state.value.isRecordingPaused) {
            cameraManager.resumeVideoRecording()
        }
    }

    fun stopRecording() {
        cameraManager.stopVideoRecording()
    }

    private fun startRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = viewModelScope.launch {
            while (true) {
                delay(500)
                if (_state.value.isRecording && !_state.value.isRecordingPaused) {
                    val now = SystemClock.elapsedRealtime()
                    val elapsed = (now - recordingStartTime - pausedTimeAccumulator) / 1000L
                    _state.update { it.copy(recordingDurationSeconds = elapsed) }
                }
            }
        }
    }

    private fun stopRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopRecordingTimer()
        cameraManager.shutdown()
    }
}
