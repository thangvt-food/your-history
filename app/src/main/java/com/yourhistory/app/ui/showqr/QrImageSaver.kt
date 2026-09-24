package com.yourhistory.app.ui.showqr

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

/**
 * Lưu ảnh mã VietQR vào Thư viện ảnh chung để app ngân hàng có thể
 * "quét QR từ ảnh" — luồng chuyển nhanh 1 điện thoại, universal.
 *
 * Android 10+: MediaStore, không cần quyền. Android 8-9: cần
 * WRITE_EXTERNAL_STORAGE (đã khai báo maxSdkVersion 28).
 */
object QrImageSaver {

    fun needsWritePermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    /**
     * Trả về Uri ảnh đã lưu, hoặc null khi thất bại.
     */
    fun saveToGallery(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveViaMediaStore(context, bitmap, fileName)
            } else {
                saveLegacy(context, bitmap, fileName)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun saveViaMediaStore(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/YourHistory")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val collection =
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values) ?: return null
        try {
            resolver.openOutputStream(uri)?.use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    resolver.delete(uri, null, null)
                    return null
                }
            } ?: run {
                resolver.delete(uri, null, null)
                return null
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        } catch (e: Exception) {
            try {
                resolver.delete(uri, null, null)
            } catch (_: Exception) {
            }
            return null
        }
    }

    @Suppress("Deprecation")
    private fun saveLegacy(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "YourHistory"
        )
        if (!dir.exists() && !dir.mkdirs()) return null
        val file = File(dir, fileName)
        try {
            FileOutputStream(file).use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) return null
            }
        } catch (e: Exception) {
            return null
        }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DATA, file.absolutePath)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        }
        return context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
        )
    }
}
