package com.example.qr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object QrMediaHelper {

    fun saveQrBitmapToGallery(context: Context, bitmap: Bitmap): Result<String> = runCatching {
        val filename = "QRcode_${System.currentTimeMillis()}.png"
        val resolver = context.contentResolver

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/QRcode")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: error("无法创建相册文件")

            try {
                resolver.openOutputStream(uri)?.use { stream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                        error("图片压缩写入失败")
                    }
                } ?: error("无法写入输出流")

                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            } catch (t: Throwable) {
                resolver.delete(uri, null, null)
                throw t
            }
        } else {
            val appPictures = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: context.filesDir
            val outDir = File(appPictures, "QRcode").apply { mkdirs() }
            val outFile = File(outDir, filename)
            FileOutputStream(outFile).use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    error("图片压缩写入失败")
                }
            }
        }
        filename
    }

    fun copyQrBitmapToClipboard(context: Context, bitmap: Bitmap): Result<Unit> = runCatching {
        val uri = writeTempQrPngForProvider(context, bitmap)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newUri(context.contentResolver, "QRcode", uri)
        clipboard.setPrimaryClip(clip)
    }

    fun copyTextToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun shareQrBitmap(context: Context, bitmap: Bitmap, content: String): Result<Unit> = runCatching {
        val uri = writeTempQrPngForProvider(context, bitmap)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, content)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享二维码"))
    }

    fun shareText(context: Context, text: String): Result<Unit> = runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "分享内容"))
    }

    fun openExternalUrl(context: Context, url: String): Result<Unit> = runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val maxSide = maxOf(info.size.width, info.size.height)
                if (maxSide > 1600) {
                    val scale = 1600f / maxSide
                    decoder.setTargetSize(
                        maxOf(1, (info.size.width * scale).toInt()),
                        maxOf(1, (info.size.height * scale).toInt())
                    )
                }
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }
    }.getOrNull()

    fun readClipboardImageUri(context: Context): Uri? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return null
        val clip = clipboard.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        for (i in 0 until clip.itemCount) {
            val uri = clip.getItemAt(i).uri
            if (uri != null) {
                val mime = context.contentResolver.getType(uri)
                if (mime == null || mime.startsWith("image/")) {
                    return uri
                }
            }
        }
        return null
    }

    fun readClipboardText(context: Context): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return null
        val clip = clipboard.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        return clip.getItemAt(0).coerceToText(context)?.toString()?.takeIf { it.isNotBlank() }
    }

    private fun writeTempQrPngForProvider(context: Context, bitmap: Bitmap): Uri {
        val cacheDir = File(context.cacheDir, "qr_images").apply { mkdirs() }
        // Clean old temp files to avoid unbounded cache growth
        cacheDir.listFiles()?.forEach { file ->
            if (System.currentTimeMillis() - file.lastModified() > 60_000L) {
                file.delete()
            }
        }
        val outFile = File(cacheDir, "qr_${System.currentTimeMillis()}.png")
        FileOutputStream(outFile).use { stream ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                error("无法写入缓存图片")
            }
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )
    }
}
