package com.bpdlh.pencatatankeuangan.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class PhotoStore(private val context: Context) : PhotoLibrary {
    private val dir: File by lazy { File(context.filesDir, "photos").apply { mkdirs() } }

    override suspend fun import(uri: Uri): String = withContext(Dispatchers.IO) {
        val bitmap = readScaled(uri)
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 82, it) }
        bitmap.recycle()
        file.absolutePath
    }

    suspend fun save(bytes: ByteArray, photoId: String): String = withContext(Dispatchers.IO) {
        val file = File(dir, "$photoId.jpg")
        file.writeBytes(bytes)
        file.absolutePath
    }

    suspend fun read(path: String): ByteArray = withContext(Dispatchers.IO) { File(path).readBytes() }

    override fun delete(path: String?) {
        if (path != null) File(path).delete()
    }

    fun clearAll() {
        dir.listFiles()?.forEach { it.delete() }
    }

    private fun readScaled(uri: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_SIDE && bounds.outHeight / (sample * 2) >= MAX_SIDE) {
            sample *= 2
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Foto tidak bisa dibaca")

        val rotation = context.contentResolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        val scale = MAX_SIDE.toFloat() / maxOf(decoded.width, decoded.height)
        val matrix = Matrix().apply {
            if (rotation != 0f) postRotate(rotation)
            if (scale < 1f) postScale(scale, scale)
        }
        if (rotation == 0f && scale >= 1f) return decoded
        return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            .also { if (it != decoded) decoded.recycle() }
    }

    private companion object {
        const val MAX_SIDE = 1600
    }
}
