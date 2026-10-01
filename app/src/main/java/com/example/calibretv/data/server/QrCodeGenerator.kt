package com.example.calibretv.data.server

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

/**
 * Generador estándar de Códigos QR basado en ZXing (ISO/IEC 18004).
 * Genera Bitmaps nítidos de alto contraste y corrección de error M/Q
 * fácilmente legibles por cámaras de teléfonos a distancia en Android TV.
 */
object QrCodeGenerator {

    fun generateQrBitmap(
        content: String,
        width: Int = 400,
        height: Int = 400,
        margin: Int = 2
    ): Bitmap {
        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
                put(EncodeHintType.MARGIN, margin)
            }

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints)

            val bmpWidth = bitMatrix.width
            val bmpHeight = bitMatrix.height
            val bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)

            for (x in 0 until bmpWidth) {
                for (y in 0 until bmpHeight) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (_: Exception) {
            // Fallback en caso de error extremo
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.WHITE)
            }
        }
    }
}
