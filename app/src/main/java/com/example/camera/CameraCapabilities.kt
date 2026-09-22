package com.example.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager as AndroidCameraManager
import androidx.camera.core.CameraSelector
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager

data class CameraHardwareInfo(
    val cameraId: String,
    val facing: String,
    val hardwareLevel: String,
    val sensorMegaPixels: Float,
    val hasFlash: Boolean,
    val maxZoom: Float,
    val supportedExtensions: List<String>
)

object CameraCapabilities {

    fun inspectDeviceCameras(
        context: Context,
        extensionsManager: ExtensionsManager? = null
    ): List<CameraHardwareInfo> {
        val list = mutableListOf<CameraHardwareInfo>()
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? AndroidCameraManager
            ?: return list

        try {
            for (id in cameraManager.cameraIdList) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val facingInt = chars.get(CameraCharacteristics.LENS_FACING)
                val facingStr = when (facingInt) {
                    CameraCharacteristics.LENS_FACING_BACK -> "Back"
                    CameraCharacteristics.LENS_FACING_FRONT -> "Front"
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
                    else -> "Unknown"
                }

                val levelInt = chars.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                val levelStr = when (levelInt) {
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "EXTERNAL"
                    else -> "UNKNOWN"
                }

                val flash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

                val activeArray = chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                val mp = if (activeArray != null) {
                    (activeArray.width() * activeArray.height()) / 1_000_000f
                } else {
                    0f
                }

                val maxZoom = chars.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f

                val extensions = mutableListOf<String>()
                if (extensionsManager != null) {
                    val selector = if (facingInt == CameraCharacteristics.LENS_FACING_FRONT) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                    if (extensionsManager.isExtensionAvailable(selector, ExtensionMode.BOKEH)) {
                        extensions.add("Bokeh")
                    }
                    if (extensionsManager.isExtensionAvailable(selector, ExtensionMode.NIGHT)) {
                        extensions.add("Night")
                    }
                    if (extensionsManager.isExtensionAvailable(selector, ExtensionMode.HDR)) {
                        extensions.add("HDR")
                    }
                    if (extensionsManager.isExtensionAvailable(selector, ExtensionMode.FACE_RETOUCH)) {
                        extensions.add("Face Retouch")
                    }
                }

                list.add(
                    CameraHardwareInfo(
                        cameraId = id,
                        facing = facingStr,
                        hardwareLevel = levelStr,
                        sensorMegaPixels = (mp * 10).toInt() / 10f,
                        hasFlash = flash,
                        maxZoom = (maxZoom * 10).toInt() / 10f,
                        supportedExtensions = extensions
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return list
    }
}
