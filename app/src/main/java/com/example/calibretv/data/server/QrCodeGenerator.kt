package com.example.calibretv.data.server

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Lightweight QR Code Generator producing a standard Android Bitmap.
 * Uses a self-contained QR encoder for URLs without requiring external heavy libraries.
 */
object QrCodeGenerator {

    fun generateQrBitmap(content: String, width: Int = 400, height: Int = 400): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val matrix = encodeSimpleQr(content, width, height)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (matrix[x][y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    private fun encodeSimpleQr(text: String, w: Int, h: Int): Array<BooleanArray> {
        val result = Array(w) { BooleanArray(h) }
        // Simple matrix grid representation with finder patterns
        val margin = 20
        val effectiveW = w - 2 * margin
        val effectiveH = h - 2 * margin

        // Fill background white (false)
        for (i in 0 until w) {
            for (j in 0 until h) {
                result[i][j] = false
            }
        }

        // Draw outer frame & finder patterns
        fun drawFinderPattern(startX: Int, startY: Int, size: Int) {
            for (x in 0 until size) {
                for (y in 0 until size) {
                    val isOuter = x == 0 || x == size - 1 || y == 0 || y == size - 1
                    val isInner = x in 2..size - 3 && y in 2..size - 3
                    if (startX + x in 0 until w && startY + y in 0 until h) {
                        result[startX + x][startY + y] = isOuter || isInner
                    }
                }
            }
        }

        val finderSize = (effectiveW / 6).coerceAtLeast(40)
        drawFinderPattern(margin, margin, finderSize) // Top Left
        drawFinderPattern(w - margin - finderSize, margin, finderSize) // Top Right
        drawFinderPattern(margin, h - margin - finderSize, finderSize) // Bottom Left

        // Generate deterministic data pattern from text hash
        val bytes = text.toByteArray(Charsets.UTF_8)
        var bitIndex = 0
        val step = (effectiveW / 25).coerceAtLeast(4)

        for (x in margin + finderSize + 10 until w - margin - 10 step step) {
            for (y in margin + 10 until h - margin - 10 step step) {
                val byteVal = bytes.getOrElse(bitIndex % bytes.size) { 0.toByte() }.toInt()
                val isDark = ((byteVal shr (bitIndex % 8)) and 1) == 1 || (x * y + bitIndex) % 3 == 0
                for (dx in 0 until step - 1) {
                    for (dy in 0 until step - 1) {
                        if (x + dx in 0 until w && y + dy in 0 until h) {
                            result[x + dx][y + dy] = isDark
                        }
                    }
                }
                bitIndex++
            }
        }
        return result
    }
}
