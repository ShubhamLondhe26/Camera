package com.example.gallery

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateAdded: Long,
    val size: Long,
    val isVideo: Boolean,
    val durationMs: Long = 0L
)
