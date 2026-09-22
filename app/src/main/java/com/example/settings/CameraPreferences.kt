package com.example.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.example.camera.AspectRatioMode

class CameraPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("camera_preferences", Context.MODE_PRIVATE)

    var isGridEnabled: Boolean
        get() = prefs.getBoolean(KEY_GRID_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_GRID_ENABLED, value).apply()

    var timerSeconds: Int
        get() = prefs.getInt(KEY_TIMER_SECONDS, 0)
        set(value) = prefs.edit().putInt(KEY_TIMER_SECONDS, value).apply()

    var aspectRatio: AspectRatioMode
        get() {
            val name = prefs.getString(KEY_ASPECT_RATIO, AspectRatioMode.RATIO_4_3.name)
            return try {
                AspectRatioMode.valueOf(name ?: AspectRatioMode.RATIO_4_3.name)
            } catch (e: Exception) {
                AspectRatioMode.RATIO_4_3
            }
        }
        set(value) = prefs.edit().putString(KEY_ASPECT_RATIO, value.name).apply()

    var isShutterSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHUTTER_SOUND, true)
        set(value) = prefs.edit().putBoolean(KEY_SHUTTER_SOUND, value).apply()

    companion object {
        private const val KEY_GRID_ENABLED = "key_grid_enabled"
        private const val KEY_TIMER_SECONDS = "key_timer_seconds"
        private const val KEY_ASPECT_RATIO = "key_aspect_ratio"
        private const val KEY_SHUTTER_SOUND = "key_shutter_sound"

        fun getDeviceInfo(): String {
            val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
            val model = Build.MODEL
            val androidVersion = Build.VERSION.RELEASE
            val sdk = Build.VERSION.SDK_INT
            return "$manufacturer $model (Android $androidVersion, API $sdk)"
        }
    }
}
