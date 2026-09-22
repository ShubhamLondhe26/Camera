package com.example.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.view.ScaleGestureDetector
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.PendingRecording
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.gallery.MediaStoreHelper
import com.example.imageprocessing.ImageProcessor
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CameraManager(private val context: Context) {

    private val TAG = "CameraManager"

    private var cameraProvider: ProcessCameraProvider? = null
    private var extensionsManager: ExtensionsManager? = null
    private var camera: Camera? = null
    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null

    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    fun getExtensionsManager(): ExtensionsManager? = extensionsManager

    fun initialize(onInitialized: (ProcessCameraProvider, ExtensionsManager?) -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            cameraProvider = provider

            val extensionsManagerFuture = ExtensionsManager.getInstanceAsync(context, provider)
            extensionsManagerFuture.addListener({
                try {
                    val extManager = extensionsManagerFuture.get()
                    extensionsManager = extManager
                    onInitialized(provider, extManager)
                } catch (e: Exception) {
                    Log.w(TAG, "ExtensionsManager initialization failed", e)
                    onInitialized(provider, null)
                }
            }, ContextCompat.getMainExecutor(context))

        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Lifecycle-aware camera binding supporting Photo, Portrait, Night, and Video modes.
     */
    @SuppressLint("MissingPermission")
    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        lensFacing: Int,
        mode: CameraMode,
        flashMode: FlashMode,
        onCameraBound: (CameraInfo) -> Unit,
        onError: (String) -> Unit
    ) {
        val provider = cameraProvider ?: run {
            onError("Camera provider is not initialized yet")
            return
        }

        try {
            provider.unbindAll()

            var baseSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            // Check if extension (Bokeh / Night) is natively supported by the device
            var activeSelector = baseSelector
            val extManager = extensionsManager
            if (extManager != null) {
                when (mode) {
                    CameraMode.PORTRAIT -> {
                        if (extManager.isExtensionAvailable(baseSelector, ExtensionMode.BOKEH)) {
                            activeSelector = extManager.getExtensionEnabledCameraSelector(
                                baseSelector,
                                ExtensionMode.BOKEH
                            )
                            Log.d(TAG, "Native BOKEH extension enabled")
                        }
                    }
                    CameraMode.NIGHT -> {
                        if (extManager.isExtensionAvailable(baseSelector, ExtensionMode.NIGHT)) {
                            activeSelector = extManager.getExtensionEnabledCameraSelector(
                                baseSelector,
                                ExtensionMode.NIGHT
                            )
                            Log.d(TAG, "Native NIGHT extension enabled")
                        }
                    }
                    else -> Unit
                }
            }

            // Preview setup
            preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            // ImageCapture setup
            val imageFlashMode = when (flashMode) {
                FlashMode.ON -> ImageCapture.FLASH_MODE_ON
                FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
                else -> ImageCapture.FLASH_MODE_OFF
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setFlashMode(imageFlashMode)
                .build()

            // VideoCapture setup
            val qualitySelector = QualitySelector.from(
                Quality.FHD,
                FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
            )
            val recorder = Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .setExecutor(cameraExecutor)
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            // Bind appropriate use cases
            camera = if (mode == CameraMode.VIDEO) {
                provider.bindToLifecycle(
                    lifecycleOwner,
                    activeSelector,
                    preview,
                    videoCapture
                )
            } else {
                provider.bindToLifecycle(
                    lifecycleOwner,
                    activeSelector,
                    preview,
                    imageCapture
                )
            }

            cameraControl = camera?.cameraControl
            cameraInfo = camera?.cameraInfo

            // Apply torch if selected
            if (flashMode == FlashMode.TORCH && cameraInfo?.hasFlashUnit() == true) {
                cameraControl?.enableTorch(true)
            }

            cameraInfo?.let { onCameraBound(it) }

        } catch (e: Exception) {
            Log.e(TAG, "Camera binding error", e)
            onError("Failed to bind camera: ${e.localizedMessage}")
        }
    }

    /**
     * Tap to focus and metering with visual target.
     */
    fun tapToFocus(previewView: PreviewView, x: Float, y: Float) {
        val factory = previewView.meteringPointFactory
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()
        cameraControl?.startFocusAndMetering(action)
    }

    fun setZoomRatio(ratio: Float) {
        cameraControl?.setZoomRatio(ratio)
    }

    fun setLinearZoom(linear: Float) {
        cameraControl?.setLinearZoom(linear.coerceIn(0f, 1f))
    }

    fun setExposureCompensation(index: Int) {
        cameraControl?.setExposureCompensationIndex(index)
    }

    fun setTorchEnabled(enabled: Boolean) {
        if (cameraInfo?.hasFlashUnit() == true) {
            cameraControl?.enableTorch(enabled)
        }
    }

    /**
     * Captures a standard photo directly to MediaStore.
     */
    fun capturePhotoDirect(
        onSuccess: (Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        val capture = imageCapture ?: run {
            onError("Image capture is not ready")
            return
        }

        val outputOptions = MediaStoreHelper.createImageCaptureOutputOptions(context, "IMG_")
        val mainExecutor = ContextCompat.getMainExecutor(context)

        capture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val uri = outputFileResults.savedUri
                    if (uri != null) {
                        mainExecutor.execute { onSuccess(uri) }
                    } else {
                        // Fallback to querying latest media
                        val latest = MediaStoreHelper.fetchLatestMediaThumbnailUri(context)
                        mainExecutor.execute {
                            if (latest != null) onSuccess(latest)
                            else onError("Saved image URI not returned")
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo direct capture error", exception)
                    mainExecutor.execute {
                        onError("Capture failed: ${exception.localizedMessage}")
                    }
                }
            }
        )
    }

    /**
     * Captures an in-memory frame for software pipeline post-processing (Portrait bokeh or Night enhancement).
     */
    fun capturePhotoWithProcessing(
        mode: CameraMode,
        portraitAperture: Float,
        onSuccess: (Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        val capture = imageCapture ?: run {
            onError("Image capture is not ready")
            return
        }

        val mainExecutor = ContextCompat.getMainExecutor(context)

        capture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val buffer: ByteBuffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)

                        var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bitmap != null) {
                            // Rotate if required via EXIF
                            bitmap = ImageProcessor.rotateBitmapIfRequired(
                                bitmap,
                                ByteArrayInputStream(bytes)
                            )

                            // Apply processing according to camera mode
                            val processed = when (mode) {
                                CameraMode.PORTRAIT -> {
                                    ImageProcessor.applyPortraitBokeh(bitmap, portraitAperture)
                                }
                                CameraMode.NIGHT -> {
                                    ImageProcessor.applyNightEnhancement(bitmap)
                                }
                                else -> bitmap
                            }

                            val prefix = if (mode == CameraMode.PORTRAIT) "PORTRAIT_" else "NIGHT_"
                            val savedUri = MediaStoreHelper.saveBitmapToMediaStore(context, processed, prefix)

                            mainExecutor.execute {
                                if (savedUri != null) {
                                    onSuccess(savedUri)
                                } else {
                                    onError("Failed to save processed image to MediaStore")
                                }
                            }
                        } else {
                            mainExecutor.execute { onError("Failed to decode captured image") }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Image processing error", e)
                        mainExecutor.execute { onError("Processing error: ${e.localizedMessage}") }
                    } finally {
                        image.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "In-memory capture error", exception)
                    mainExecutor.execute {
                        onError("Capture failed: ${exception.localizedMessage}")
                    }
                }
            }
        )
    }

    /**
     * Starts video recording with audio to MediaStore.
     */
    @SuppressLint("MissingPermission")
    fun startVideoRecording(
        withAudio: Boolean,
        onEvent: (VideoRecordEvent) -> Unit
    ) {
        val capture = videoCapture ?: return
        val outputOptions = MediaStoreHelper.createVideoCaptureOutputOptions(context, "VID_")

        var pending: PendingRecording = capture.output.prepareRecording(context, outputOptions)
        if (withAudio) {
            try {
                pending = pending.withAudioEnabled()
            } catch (e: SecurityException) {
                Log.w(TAG, "Audio permission not granted for recording", e)
            }
        }

        activeRecording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
            onEvent(event)
        }
    }

    fun pauseVideoRecording() {
        activeRecording?.pause()
    }

    fun resumeVideoRecording() {
        activeRecording?.resume()
    }

    fun stopVideoRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    fun shutdown() {
        activeRecording?.stop()
        activeRecording = null
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
    }
}
