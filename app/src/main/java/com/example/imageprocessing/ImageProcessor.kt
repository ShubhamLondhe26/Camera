package com.example.imageprocessing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import android.media.ExifInterface
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

object ImageProcessor {

    /**
     * Fixes bitmap rotation using EXIF orientation tag.
     */
    fun rotateBitmapIfRequired(bitmap: Bitmap, inputStream: InputStream): Bitmap {
        return try {
            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (rotationDegrees != 0f) {
                val matrix = Matrix().apply { postRotate(rotationDegrees) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    /**
     * Simulates portrait mode depth-of-field (bokeh effect) with aperture control (f/1.4 - f/16).
     * Smaller f-number = wider aperture = stronger background blur.
     */
    fun applyPortraitBokeh(source: Bitmap, aperture: Float): Bitmap {
        val width = source.width
        val height = source.height

        // Calculate blur strength inversely proportional to f-stop (f/1.4 -> strong blur, f/16 -> sharp)
        val normalizedStrength = ((16f - aperture) / (16f - 1.4f)).coerceIn(0f, 1f)
        if (normalizedStrength < 0.05f) {
            return source
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // 1. Create blurred version of source bitmap using fast box-blur downsample technique
        val scale = 0.25f
        val smallW = max(1, (width * scale).toInt())
        val smallH = max(1, (height * scale).toInt())
        val smallBitmap = Bitmap.createScaledBitmap(source, smallW, smallH, true)
        val blurredBackground = Bitmap.createScaledBitmap(smallBitmap, width, height, true)

        // 2. Draw blurred background first
        canvas.drawBitmap(blurredBackground, 0f, 0f, null)

        // 3. Create radial/elliptical mask that keeps central subject sharp
        val maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val maskCanvas = Canvas(maskBitmap)

        val centerX = width / 2f
        val centerY = height * 0.45f
        val radius = min(width, height) * 0.42f

        val gradient = RadialGradient(
            centerX,
            centerY,
            radius,
            intArrayOf(Color.BLACK, Color.BLACK, Color.TRANSPARENT),
            floatArrayOf(0.0f, 0.65f, 1.0f),
            Shader.TileMode.CLAMP
        )

        val maskPaint = Paint().apply {
            isAntiAlias = true
            shader = gradient
        }
        maskCanvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), maskPaint)

        // 4. Composite the sharp subject over the blurred background using DST_IN / SRC_OVER
        val sharpSubject = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val sharpCanvas = Canvas(sharpSubject)
        sharpCanvas.drawBitmap(source, 0f, 0f, null)

        val blendPaint = Paint().apply {
            isAntiAlias = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        sharpCanvas.drawBitmap(maskBitmap, 0f, 0f, blendPaint)

        // Draw composite sharp subject over blurred background
        canvas.drawBitmap(sharpSubject, 0f, 0f, null)

        return result
    }

    /**
     * Simulates Night Mode multi-frame low-light enhancement:
     * Lifts deep shadows, improves local dynamic range, and balances exposure.
     */
    fun applyNightEnhancement(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        // Compute average luminance to determine boost
        var totalLum = 0.0
        val sampleStep = 8
        var sampleCount = 0
        for (i in pixels.indices step sampleStep) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            totalLum += (0.299 * r + 0.587 * g + 0.114 * b)
            sampleCount++
        }
        val avgLum = (totalLum / sampleCount).toFloat()

        // Adaptive gamma boost: darker scenes get stronger shadow lift
        val gamma = if (avgLum < 60f) 0.65f else 0.82f
        val lut = IntArray(256) { i ->
            val normalized = i / 255.0f
            val boosted = Math.pow(normalized.toDouble(), gamma.toDouble()).toFloat()
            // Slight contrast S-curve
            val contrastAdjusted = (boosted * 1.1f - 0.05f).coerceIn(0f, 1f)
            (contrastAdjusted * 255f).toInt()
        }

        for (i in pixels.indices) {
            val color = pixels[i]
            val a = (color shr 24) and 0xFF
            val r = lut[(color shr 16) and 0xFF]
            val g = lut[(color shr 8) and 0xFF]
            val b = lut[color and 0xFF]
            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }
}
