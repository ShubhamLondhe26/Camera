package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CameraAccentYellow
import kotlin.math.roundToInt

@Composable
fun FocusIndicator(
    focusPoint: Pair<Float, Float>,
    isExposureVisible: Boolean,
    exposureIndex: Int,
    minExposureIndex: Int,
    maxExposureIndex: Int,
    onExposureChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = 1.0f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "focus_scale"
    )

    val boxSize = 72.dp

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    (focusPoint.first - (boxSize.toPx() / 2)).roundToInt(),
                    (focusPoint.second - (boxSize.toPx() / 2)).roundToInt()
                )
            }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Focus Bracket Box
            Canvas(
                modifier = Modifier.size(boxSize)
            ) {
                val stroke = 2.dp.toPx()
                val bracketLen = 14.dp.toPx()
                val w = size.width
                val h = size.height
                val color = CameraAccentYellow

                // Top-Left corner
                drawLine(color, Offset(0f, 0f), Offset(bracketLen, 0f), stroke)
                drawLine(color, Offset(0f, 0f), Offset(0f, bracketLen), stroke)

                // Top-Right corner
                drawLine(color, Offset(w, 0f), Offset(w - bracketLen, 0f), stroke)
                drawLine(color, Offset(w, 0f), Offset(w, bracketLen), stroke)

                // Bottom-Left corner
                drawLine(color, Offset(0f, h), Offset(bracketLen, h), stroke)
                drawLine(color, Offset(0f, h), Offset(0f, h - bracketLen), stroke)

                // Bottom-Right corner
                drawLine(color, Offset(w, h), Offset(w - bracketLen, h), stroke)
                drawLine(color, Offset(w, h), Offset(w, h - bracketLen), stroke)

                // Subtle center dot
                drawCircle(color, radius = 2.dp.toPx(), center = Offset(w / 2f, h / 2f))
            }

            // Exposure adjustment sun icon and index readout
            if (isExposureVisible) {
                Column(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount ->
                                // Drag up = brighter (+), Drag down = darker (-)
                                val delta = if (dragAmount < -5) 1 else if (dragAmount > 5) -1 else 0
                                if (delta != 0) {
                                    val newIdx = (exposureIndex + delta).coerceIn(minExposureIndex, maxExposureIndex)
                                    onExposureChanged(newIdx)
                                }
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = "Exposure",
                        tint = CameraAccentYellow,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (exposureIndex > 0) "+$exposureIndex" else "$exposureIndex",
                        color = CameraAccentYellow,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
