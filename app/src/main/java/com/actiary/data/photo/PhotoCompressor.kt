package com.actiary.data.photo

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import java.io.File
import kotlin.math.roundToInt

internal const val PHOTO_MAX_EDGE_PX = 1920
internal const val PHOTO_JPEG_QUALITY = 84

/** Computes proportional target dimensions so the longest edge is at most [maxEdge]. */
internal fun targetPhotoSize(width: Int, height: Int, maxEdge: Int = PHOTO_MAX_EDGE_PX): Pair<Int, Int> {
    if (width <= 0 || height <= 0 || maxEdge <= 0) return width to height
    val longest = maxOf(width, height)
    if (longest <= maxEdge) return width to height
    val scale = maxEdge.toDouble() / longest.toDouble()
    return (width * scale).roundToInt().coerceAtLeast(1) to (height * scale).roundToInt().coerceAtLeast(1)
}

/**
 * Decodes [source] via Android 12+ [ImageDecoder] (auto-applying EXIF orientation),
 * downscales if its longest edge exceeds [maxEdge], and writes a JPEG to [target].
 * Returns false if [source] cannot be decoded as an image so callers can fall back safely.
 */
internal fun compressPhotoToJpeg(
    source: File,
    target: File,
    maxEdge: Int = PHOTO_MAX_EDGE_PX,
    quality: Int = PHOTO_JPEG_QUALITY,
): Boolean {
    return try {
        val imageSource = ImageDecoder.createSource(source)
        val bitmap = ImageDecoder.decodeBitmap(imageSource) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val size = info.size
            val (targetW, targetH) = targetPhotoSize(size.width, size.height, maxEdge)
            if (targetW != size.width || targetH != size.height) {
                decoder.setTargetSize(targetW, targetH)
            }
        }
        try {
            target.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
        } finally {
            bitmap.recycle()
        }
    } catch (_: Throwable) {
        false
    }
}
