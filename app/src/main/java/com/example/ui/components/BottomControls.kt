package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.camera.CameraMode
import com.example.ui.theme.CameraAccentCyan
import com.example.ui.theme.CameraAccentRed
import com.example.ui.theme.CameraAccentRose
import com.example.ui.theme.CameraAccentYellow
import com.example.ui.theme.CameraBlack
import com.example.ui.theme.TextPrimary

@Composable
fun BottomControls(
    selectedMode: CameraMode,
    isCapturing: Boolean,
    isProcessing: Boolean,
    isRecording: Boolean,
    lastThumbnailUri: Uri?,
    onShutterClick: () -> Unit,
    onSwitchCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Gallery Preview Thumbnail Button
        GalleryThumbnailButton(
            uri = lastThumbnailUri,
            onClick = onGalleryClick
        )

        // Center Shutter Button
        ShutterButton(
            mode = selectedMode,
            isCapturing = isCapturing,
            isProcessing = isProcessing,
            isRecording = isRecording,
            onClick = onShutterClick
        )

        // Switch Camera (Front / Back)
        CameraSwitchButton(
            isEnabled = !isRecording && !isCapturing && !isProcessing,
            onClick = onSwitchCameraClick
        )
    }
}

@Composable
fun ShutterButton(
    mode: CameraMode,
    isCapturing: Boolean,
    isProcessing: Boolean,
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "shutter_scale"
    )

    // Morph inner shape if recording video
    val innerCornerRadius by animateDpAsState(
        targetValue = if (isRecording) 8.dp else 36.dp,
        animationSpec = tween(durationMillis = 200),
        label = "shutter_corner"
    )

    val innerSize by animateDpAsState(
        targetValue = if (isRecording) 32.dp else 60.dp,
        animationSpec = tween(durationMillis = 200),
        label = "shutter_inner_size"
    )

    val innerColor = when (mode) {
        CameraMode.VIDEO -> CameraAccentRed
        CameraMode.NIGHT -> CameraAccentCyan
        CameraMode.PORTRAIT -> CameraAccentRose
        CameraMode.PHOTO -> Color.White
    }

    Box(
        modifier = modifier
            .size(80.dp)
            .scale(scale)
            .clip(CircleShape)
            .border(4.dp, Color.White, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !isProcessing,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isProcessing) {
            CircularProgressIndicator(
                modifier = Modifier.size(50.dp),
                color = innerColor,
                strokeWidth = 3.dp
            )
        } else {
            Box(
                modifier = Modifier
                    .size(innerSize)
                    .clip(RoundedCornerShape(innerCornerRadius))
                    .background(innerColor)
            )
        }
    }
}

@Composable
fun GalleryThumbnailButton(
    uri: Uri?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color(0x33FFFFFF))
            .border(2.dp, Color(0x66FFFFFF), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = "Gallery Preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "Open Gallery",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
fun CameraSwitchButton(
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rotation = 0f

    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color(0x33FFFFFF))
            .border(1.dp, Color(0x33FFFFFF), CircleShape)
            .clickable(enabled = isEnabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Cameraswitch,
            contentDescription = "Switch Camera",
            tint = if (isEnabled) TextPrimary else Color.Gray,
            modifier = Modifier.size(28.dp)
        )
    }
}
