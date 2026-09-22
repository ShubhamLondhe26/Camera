package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.AspectRatioMode
import com.example.camera.FlashMode
import com.example.ui.theme.CameraAccentYellow
import com.example.ui.theme.TextPrimary

@Composable
fun TopBarControls(
    flashMode: FlashMode,
    hasFlash: Boolean,
    timerSeconds: Int,
    aspectRatio: AspectRatioMode,
    isGridEnabled: Boolean,
    onFlashToggle: () -> Unit,
    onTimerToggle: () -> Unit,
    onAspectRatioToggle: () -> Unit,
    onGridToggle: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x66000000))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Flash Control
        IconButton(
            onClick = onFlashToggle,
            enabled = hasFlash,
            modifier = Modifier.size(44.dp)
        ) {
            val (icon, tint) = when (flashMode) {
                FlashMode.OFF -> Pair(Icons.Default.FlashOff, Color.White.copy(alpha = 0.6f))
                FlashMode.AUTO -> Pair(Icons.Default.FlashAuto, CameraAccentYellow)
                FlashMode.ON -> Pair(Icons.Default.FlashOn, CameraAccentYellow)
                FlashMode.TORCH -> Pair(Icons.Default.Highlight, CameraAccentYellow)
            }
            Icon(
                imageVector = icon,
                contentDescription = "Flash: $flashMode",
                tint = if (hasFlash) tint else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }

        // Timer Control
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .clickable { onTimerToggle() }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Timer",
                    tint = if (timerSeconds > 0) CameraAccentYellow else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
                if (timerSeconds > 0) {
                    Text(
                        text = "${timerSeconds}s",
                        color = CameraAccentYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
        }

        // Aspect Ratio Control
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .clickable { onAspectRatioToggle() }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = aspectRatio.displayName,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Grid Lines Toggle
        IconButton(
            onClick = onGridToggle,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = if (isGridEnabled) Icons.Default.Grid3x3 else Icons.Default.GridOff,
                contentDescription = "Grid",
                tint = if (isGridEnabled) CameraAccentYellow else Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp)
            )
        }

        // Settings Button
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = TextPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
