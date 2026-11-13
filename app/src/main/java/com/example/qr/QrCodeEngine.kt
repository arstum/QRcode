package com.example.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.model.QrErrorCorrection
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.InvertedLuminanceSource
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

data class QrMatrix(
    val size: Int,
    val bits: BooleanArray
) {
    fun get(x: Int, y: Int): Boolean = bits[y * size + x]

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is QrMatrix) return false
        return size == other.size && bits.contentEquals(other.bits)
    }

    override fun hashCode(): Int {
        var result = size
        result = 31 * result + bits.contentHashCode()
        return result
    }
}

object QrCodeEngine {

    private val decodeHints: Map<DecodeHintType, Any> = mapOf(
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.CHARACTER_SET to "UTF-8"
    )

    fun encodeMatrix(
        content: String,
        errorCorrection: QrErrorCorrection = QrErrorCorrection.M,
        quietZoneModules: Int = 2
    ): QrMatrix? {
        if (content.isBlank()) return null
        return runCatching {
            val zxingLevel = when (errorCorrection) {
                QrErrorCorrection.L -> ErrorCorrectionLevel.L
                QrErrorCorrection.M -> ErrorCorrectionLevel.M
                QrErrorCorrection.Q -> ErrorCorrectionLevel.Q
                QrErrorCorrection.H -> ErrorCorrectionLevel.H
            }
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to zxingLevel,
                EncodeHintType.MARGIN to quietZoneModules
            )
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val size = maxOf(width, height)
            val bits = BooleanArray(size * size)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    bits[y * size + x] = bitMatrix.get(x, y)
                }
            }
            QrMatrix(size = size, bits = bits)
        }.getOrNull()
    }

    /**
     * Renders a crisp high-resolution Bitmap suitable for saving, copying, and sharing.
     * Always uses pure white background and pure black foreground for universal scanner compatibility.
     */
    fun renderExportBitmap(
        content: String,
        errorCorrection: QrErrorCorrection = QrErrorCorrection.M,
        targetPixels: Int = 1024
    ): Bitmap? {
        val matrix = encodeMatrix(content, errorCorrection, quietZoneModules = 3) ?: return null
        val moduleSize = maxOf(1, targetPixels / matrix.size)
        val actualSize = matrix.size * moduleSize
        val pixels = IntArray(actualSize * actualSize) { Color.WHITE }

        for (my in 0 until matrix.size) {
            for (mx in 0 until matrix.size) {
                if (matrix.get(mx, my)) {
                    val startY = my * moduleSize
                    val startX = mx * moduleSize
                    for (dy in 0 until moduleSize) {
                        val rowOffset = (startY + dy) * actualSize + startX
                        for (dx in 0 until moduleSize) {
                            pixels[rowOffset + dx] = Color.BLACK
                        }
                    }
                }
            }
        }
        return Bitmap.createBitmap(actualSize, actualSize, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, actualSize, 0, 0, actualSize, actualSize)
        }
    }

    fun decodeBitmap(bitmap: Bitmap): String? {
        val scaled = scaleBitmapForDecode(bitmap, maxDimension = 1400)
        val width = scaled.width
        val height = scaled.height
        val pixels = IntArray(width * height)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)
        if (scaled !== bitmap) {
            scaled.recycle()
        }
        val source = RGBLuminanceSource(width, height, pixels)
        return decodeLuminanceSource(source)
    }

    fun decodeYuvFrame(
        yPlaneBytes: ByteArray,
        width: Int,
        height: Int
    ): String? {
        val source = PlanarYUVLuminanceSource(
            yPlaneBytes,
            width,
            height,
            0,
            0,
            width,
            height,
            false
        )
        return decodeLuminanceSource(source)
    }

    private fun decodeLuminanceSource(source: LuminanceSource): String? {
        val reader = MultiFormatReader().apply { setHints(decodeHints) }
        val candidates = listOf(
            BinaryBitmap(HybridBinarizer(source)),
            BinaryBitmap(GlobalHistogramBinarizer(source)),
            BinaryBitmap(HybridBinarizer(InvertedLuminanceSource(source)))
        )
        for (candidate in candidates) {
            val result = runCatching {
                reader.decodeWithState(candidate).text
            }.getOrNull()
            reader.reset()
            if (!result.isNullOrBlank()) {
                return result
            }
        }
        return null
    }

    private fun scaleBitmapForDecode(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val maxSide = maxOf(bitmap.width, bitmap.height)
        if (maxSide <= maxDimension) return bitmap
        val ratio = maxDimension.toFloat() / maxSide.toFloat()
        val newW = maxOf(1, (bitmap.width * ratio).toInt())
        val newH = maxOf(1, (bitmap.height * ratio).toInt())
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }
}
