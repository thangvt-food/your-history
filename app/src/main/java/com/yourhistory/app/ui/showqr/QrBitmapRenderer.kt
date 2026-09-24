package com.yourhistory.app.ui.showqr

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Render chuỗi VietQR EMVCo thành Bitmap để hiện lên màn hình cho app
 * ngân hàng quét. Dùng ZXing core (thuần Java, offline).
 */
object QrBitmapRenderer {

    fun render(payload: String, sizePx: Int = 768): Bitmap? {
        if (payload.isBlank()) return null
        return try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "ISO-8859-1",
                EncodeHintType.MARGIN to 1,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
            )
            val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val pixels = IntArray(sizePx * sizePx)
            for (y in 0 until sizePx) {
                for (x in 0 until sizePx) {
                    pixels[y * sizePx + x] =
                        if (matrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                }
            }
            Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565).also {
                it.setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
            }
        } catch (e: Exception) {
            null
        }
    }
}
