package cc.devbangs.morpho.ui.tool.kit

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/**
 * Longest edge a decoded image is held at. Bounds working memory: a square at
 * this cap is 4096*4096*4 = 67 MB of ARGB_8888.
 */
const val IMAGE_MAX_DIM = 4096

/** Largest intermediate the legacy decoder may materialise. ~16 MP, ~64 MB. */
private const val LEGACY_BUDGET_PX = 16_000_000

/**
 * Decode a picked Uri, correctly oriented and scaled to an exact cap.
 *
 * Two things were wrong with the old decoder and both showed up as "low
 * quality output" in every image tool, because they all intake through here.
 *
 * 1. It ignored the EXIF orientation tag, so every portrait phone photo came
 *    out lying on its side.
 * 2. inSampleSize only halves. A 5000px photo is 22% over the 4096 cap, so
 *    sample stepped to 2 and the output was 2500px - half the resolution
 *    thrown away to shed 22%.
 *
 * On API 28+ ImageDecoder fixes both for free: it applies orientation itself
 * and decodes straight to an arbitrary target size, so there is no oversized
 * intermediate and no halving. It also reads HEIC/HEIF and AVIF, which is what
 * current iPhones and Samsungs actually write. Below 28, and whenever
 * ImageDecoder refuses a malformed file, the BitmapFactory path picks the
 * finest sample level that stays at or above the cap and within the memory
 * budget, then lands the exact size with a filtered scale.
 */
fun decodeBitmap(ctx: Context, uri: Uri, maxDim: Int = IMAGE_MAX_DIM): Bitmap? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        try {
            return decodeExact(ImageDecoder.createSource(ctx.contentResolver, uri), maxDim)
        } catch (e: Exception) {
            // fall through to the legacy path
        } catch (e: OutOfMemoryError) {
            return null
        }
    }
    return decodeLegacy(
        open = { ctx.contentResolver.openInputStream(uri) },
        exif = { ctx.contentResolver.openInputStream(uri) },
        maxDim = maxDim
    )
}

/**
 * Decode bytes handed over by another tool. Same orientation and scaling rules
 * as [decodeBitmap], so a workflow hand-off cannot come out rotated or soft
 * where the same file picked directly would not.
 */
fun decodeBitmapBytes(bytes: ByteArray, maxDim: Int = IMAGE_MAX_DIM): Bitmap? {
    if (bytes.isEmpty()) return null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        try {
            return decodeExact(ImageDecoder.createSource(ByteBuffer.wrap(bytes)), maxDim)
        } catch (e: Exception) {
            // fall through to the legacy path
        } catch (e: OutOfMemoryError) {
            return null
        }
    }
    return decodeLegacy(
        open = { ByteArrayInputStream(bytes) },
        exif = { ByteArrayInputStream(bytes) },
        maxDim = maxDim
    )
}

/**
 * ImageDecoder path. Orientation is applied by the decoder.
 *
 * ALLOCATOR_SOFTWARE is not optional: the default prefers a hardware bitmap,
 * whose pixels cannot be read or drawn into, which every transform downstream
 * needs to do.
 */
@RequiresApi(Build.VERSION_CODES.P)
private fun decodeExact(source: ImageDecoder.Source, maxDim: Int): Bitmap =
    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        decoder.isMutableRequired = false
        val w = info.size.width
        val h = info.size.height
        val largest = maxOf(w, h)
        if (largest > maxDim) {
            val f = maxDim.toFloat() / largest
            decoder.setTargetSize(
                (w * f).roundToInt().coerceAtLeast(1),
                (h * f).roundToInt().coerceAtLeast(1)
            )
        }
    }

/** BitmapFactory path for API 24-27 and for files ImageDecoder rejects. */
private fun decodeLegacy(
    open: () -> InputStream?,
    exif: () -> InputStream?,
    maxDim: Int
): Bitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    open()?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val ow = bounds.outWidth
    val oh = bounds.outHeight
    if (ow <= 0 || oh <= 0) null else {
        val largest = maxOf(ow, oh)
        // Finest level that still leaves us at or above the cap, so the exact
        // scale below is always a downscale and never an upscale.
        var sample = 1
        while (largest / (sample * 2) >= maxDim) sample *= 2
        // Then respect the memory budget, even if that lands under the cap.
        while (ow.toLong() / sample * (oh.toLong() / sample) > LEGACY_BUDGET_PX) sample *= 2

        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = open()?.use { BitmapFactory.decodeStream(it, null, opts) }
        if (decoded == null) null else {
            val big = maxOf(decoded.width, decoded.height)
            val sized = if (big > maxDim) {
                val f = maxDim.toFloat() / big
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * f).roundToInt().coerceAtLeast(1),
                    (decoded.height * f).roundToInt().coerceAtLeast(1),
                    true
                )
            } else decoded
            val tag = try {
                exif()?.use {
                    ExifInterface(it).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                    )
                } ?: ExifInterface.ORIENTATION_NORMAL
            } catch (e: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }
            orientToUpright(sized, tag)
        }
    }
} catch (e: Exception) {
    null
} catch (e: OutOfMemoryError) {
    null
}

/**
 * Apply an EXIF orientation tag. All eight cases, not just the three rotations:
 * the mirrored ones come off front cameras and off anything that has been
 * through a "flip" edit, and treating them as normal leaves the image
 * reversed.
 *
 * The source is deliberately not recycled. Freeing it here would save a GC
 * cycle and risk a use-after-recycle if any caller ever holds the input.
 */
private fun orientToUpright(b: Bitmap, orientation: Int): Bitmap {
    val m = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> m.setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.setScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { m.setRotate(90f); m.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_ROTATE_90 -> m.setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> { m.setRotate(270f); m.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_ROTATE_270 -> m.setRotate(270f)
        else -> return b   // NORMAL, UNDEFINED, or anything unrecognised
    }
    return try {
        Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true)
    } catch (e: OutOfMemoryError) {
        b
    }
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
/**
 * Extension and MIME for a compress format.
 *
 * These were a binary PNG-or-JPEG check inlined in both save and share, which
 * meant anything that was not PNG got written as .jpg with image/jpeg - fine
 * while those were the only two formats, wrong the moment WebP exists. The
 * WebP branch is `else` on purpose: WEBP_LOSSY and WEBP_LOSSLESS are API 30
 * constants, and naming them in a `when` would fault on older devices.
 */
fun imageExt(format: Bitmap.CompressFormat): String = when (format) {
    Bitmap.CompressFormat.PNG -> "png"
    Bitmap.CompressFormat.JPEG -> "jpg"
    else -> "webp"
}

fun imageMime(format: Bitmap.CompressFormat): String = when (format) {
    Bitmap.CompressFormat.PNG -> "image/png"
    Bitmap.CompressFormat.JPEG -> "image/jpeg"
    else -> "image/webp"
}

fun saveToGallery(
    ctx: Context,
    bmp: Bitmap,
    name: String,
    format: Bitmap.CompressFormat,
    quality: Int,
    /** False when a batch caller reports the whole run itself. */
    report: Boolean = true
): Boolean {
    val ok = try {
        val ext = imageExt(format)
        val mime = imageMime(format)
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

    if (report) reportSave(
        ctx, ok, "Image ready", "Your image was saved to your gallery.",
        "Saved to Pictures/Morpho", "Couldn't save the image"
    )
    return ok
}

/** Share a bitmap via the system share sheet (through FileProvider cache). */
fun shareBitmap(ctx: Context, bmp: Bitmap, name: String, format: Bitmap.CompressFormat, quality: Int) {
    try {
        val ext = imageExt(format)
        val mime = imageMime(format)
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
