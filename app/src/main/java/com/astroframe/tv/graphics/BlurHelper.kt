package com.astroframe.tv.graphics

import android.graphics.Bitmap

/**
 * Fast in-place box blur helper for creating smooth ambient blurred backgrounds
 * behind portrait or non-16:9 artworks.
 */
object BlurHelper {

    fun applyBoxBlur(bitmap: Bitmap, radius: Int) {
        if (radius < 1) return
        val width = bitmap.width
        val height = bitmap.height
        if (width < 2 || height < 2) return

        val safeRadius = minOf(radius, (width - 1) / 2, (height - 1) / 2)
        if (safeRadius < 1) return

        val pixels = IntArray(width * height)
        val scratch = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Two passes of box blur approximate a smooth Gaussian blur
        repeat(2) {
            horizontalBoxBlur(pixels, scratch, width, height, safeRadius)
            verticalBoxBlur(scratch, pixels, width, height, safeRadius)
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    private fun horizontalBoxBlur(
        src: IntArray,
        dst: IntArray,
        width: Int,
        height: Int,
        radius: Int
    ) {
        val kernelSize = 2 * radius + 1
        for (y in 0 until height) {
            val rowOffset = y * width
            var rSum = 0
            var gSum = 0
            var bSum = 0

            for (i in -radius..radius) {
                val x = i.coerceIn(0, width - 1)
                val c = src[rowOffset + x]
                rSum += (c shr 16) and 0xFF
                gSum += (c shr 8) and 0xFF
                bSum += c and 0xFF
            }

            for (x in 0 until width) {
                dst[rowOffset + x] = (0xFF shl 24) or
                    ((rSum / kernelSize) shl 16) or
                    ((gSum / kernelSize) shl 8) or
                    (bSum / kernelSize)

                val outX = (x - radius).coerceIn(0, width - 1)
                val inX = (x + radius + 1).coerceIn(0, width - 1)
                val outC = src[rowOffset + outX]
                val inC = src[rowOffset + inX]

                rSum += ((inC shr 16) and 0xFF) - ((outC shr 16) and 0xFF)
                gSum += ((inC shr 8) and 0xFF) - ((outC shr 8) and 0xFF)
                bSum += (inC and 0xFF) - (outC and 0xFF)
            }
        }
    }

    private fun verticalBoxBlur(
        src: IntArray,
        dst: IntArray,
        width: Int,
        height: Int,
        radius: Int
    ) {
        val kernelSize = 2 * radius + 1
        for (x in 0 until width) {
            var rSum = 0
            var gSum = 0
            var bSum = 0

            for (i in -radius..radius) {
                val y = i.coerceIn(0, height - 1)
                val c = src[y * width + x]
                rSum += (c shr 16) and 0xFF
                gSum += (c shr 8) and 0xFF
                bSum += c and 0xFF
            }

            for (y in 0 until height) {
                dst[y * width + x] = (0xFF shl 24) or
                    ((rSum / kernelSize) shl 16) or
                    ((gSum / kernelSize) shl 8) or
                    (bSum / kernelSize)

                val outY = (y - radius).coerceIn(0, height - 1)
                val inY = (y + radius + 1).coerceIn(0, height - 1)
                val outC = src[outY * width + x]
                val inC = src[inY * width + x]

                rSum += ((inC shr 16) and 0xFF) - ((outC shr 16) and 0xFF)
                gSum += ((inC shr 8) and 0xFF) - ((outC shr 8) and 0xFF)
                bSum += (inC and 0xFF) - (outC and 0xFF)
            }
        }
    }
}
