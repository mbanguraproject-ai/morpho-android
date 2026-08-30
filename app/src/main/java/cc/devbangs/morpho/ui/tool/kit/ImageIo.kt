package cc.devbangs.morpho.ui.tool.kit

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/** Decode a picked Uri into a Bitmap (downsampled to a safe max to avoid OOM). */
fun decodeBitmap(ctx: Context, uri: Uri, maxDim: Int = 4096): Bitmap? {
    return try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        var sample = 1
        val largest = maxOf(opts.outWidth, opts.outHeight)
        while (largest / sample > maxDim) sample *= 2
        val real = BitmapFactory.Options().apply { inSampleSize = sample }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, real) }
    } catch (e: Exception) { null }
}

/**
 * Save a bitmap to the gallery (Pictures/Morpho). Returns true on success.
 *
 * Success is only reported once bytes are actually on disk. Previously the
 * "saved to your gallery" notification and the ad-gating markUsed() both fired
 * before the attempt, so a failed save still told the user it had worked - and
 * a null output stream or a failed compress returned true, leaving a zero-byte
 * entry in the user's gallery.
 */
fun saveToGallery(ctx: Context, bmp: Bitmap, name: String, format: Bitmap.CompressFormat, quality: Int): Boolean {
    val ok = try {
        val ext = if (format == Bitmap.CompressFormat.PNG) "png" else "jpg"
        val mime = if (format == Bitmap.CompressFormat.PNG) "image/png" else "image/jpeg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.$ext")
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Morpho")
        }
        val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri == null) false
        else {
            val written = ctx.contentResolver.openOutputStream(uri)
                ?.use { bmp.compress(format, quality, it) } ?: false
            // Never leave an empty row behind in the user's gallery.
            if (!written) ctx.contentResolver.delete(uri, null, null)
            written
        }
    } catch (e: Exception) {
        false
    }

    if (ok) {
        cc.devbangs.morpho.ads.AdState.markUsed()
        cc.devbangs.morpho.notify.Notifier.notifyDone(
            ctx, "Image ready", "Your image was saved to your gallery."
        )
        Toast.makeText(ctx, "Saved to Pictures/Morpho", Toast.LENGTH_SHORT).show()
    } else {
        Toast.makeText(ctx, "Couldn't save the image", Toast.LENGTH_SHORT).show()
    }
    return ok
}

/** Share a bitmap via the system share sheet (through FileProvider cache). */
fun shareBitmap(ctx: Context, bmp: Bitmap, name: String, format: Bitmap.CompressFormat, quality: Int) {
    try {
        val ext = if (format == Bitmap.CompressFormat.PNG) "png" else "jpg"
        val mime = if (format == Bitmap.CompressFormat.PNG) "image/png" else "image/jpeg"
        val dir = File(ctx.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "$name.$ext")
        FileOutputStream(file).use { bmp.compress(format, quality, it) }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(intent, "Share image"))
    } catch (e: Exception) {
        Toast.makeText(ctx, "Share failed", Toast.LENGTH_SHORT).show()
    }
}

fun bytesHuman(n: Long): String = when {
    n >= 1_000_000 -> "%.1f MB".format(n / 1_000_000.0)
    n >= 1_000 -> "%.0f KB".format(n / 1_000.0)
    else -> "$n B"
}

fun bitmapBytes(bmp: Bitmap, format: Bitmap.CompressFormat, quality: Int): Long {
    val s = java.io.ByteArrayOutputStream()
    bmp.compress(format, quality, s)
    return s.size().toLong()
}
