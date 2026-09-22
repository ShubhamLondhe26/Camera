package com.example.camera

enum class CameraMode(
    val title: String,
    val description: String,
    val requiresAudio: Boolean = false
) {
    PHOTO(
        title = "PHOTO",
        description = "Standard high-resolution capture with HDR support"
    ),
    PORTRAIT(
        title = "PORTRAIT",
        description = "Depth of field bokeh effect with adjustable aperture"
    ),
    NIGHT(
        title = "NIGHT",
        description = "Multi-exposure low-light enhancement with noise reduction"
    ),
    VIDEO(
        title = "VIDEO",
        description = "High-definition video recording with synchronized audio",
        requiresAudio = true
    );

    companion object {
        fun fromIndex(index: Int): CameraMode = entries.getOrElse(index) { PHOTO }
    }
}

enum class FlashMode {
    OFF,
    AUTO,
    ON,
    TORCH;

    fun next(): FlashMode = when (this) {
        OFF -> AUTO
        AUTO -> ON
        ON -> TORCH
        TORCH -> OFF
    }
}

enum class AspectRatioMode(val displayName: String, val ratio: Float) {
    RATIO_4_3("4:3", 4f / 3f),
    RATIO_16_9("16:9", 16f / 9f),
    RATIO_1_1("1:1", 1f),
    RATIO_FULL("FULL", 0f); // 0f indicates full screen fill

    fun next(): AspectRatioMode = when (this) {
        RATIO_4_3 -> RATIO_16_9
        RATIO_16_9 -> RATIO_1_1
        RATIO_1_1 -> RATIO_FULL
        RATIO_FULL -> RATIO_4_3
    }
}
