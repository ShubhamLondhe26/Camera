package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CameraAccentYellow
import com.example.ui.theme.TextPrimary
import java.util.Locale

@Composable
fun ZoomControls(
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float,
    onZoomSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Generate candidate zoom stops
    val candidateStops = listOf(0.6f, 1.0f, 2.0f, 5.0f)
    val availableStops = candidateStops.filter { it in minZoom..maxZoom }
        .ifEmpty { listOf(1.0f) }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x66000000))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(24.dp))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        availableStops.forEach { stop ->
            val isSelected = Math.abs(currentZoom - stop) < 0.25f
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) CameraAccentYellow else Color.Transparent)
                    .clickable { onZoomSelected(stop) },
                contentAlignment = Alignment.Center
            ) {
                val label = if (stop < 1.0f) {
                    String.format(Locale.US, ".%01dx", (stop * 10).toInt())
                } else {
                    String.format(Locale.US, "%dx", stop.toInt())
                }
                Text(
                    text = label,
                    color = if (isSelected) Color.Black else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
