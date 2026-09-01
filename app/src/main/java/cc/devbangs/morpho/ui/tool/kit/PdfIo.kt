package cc.devbangs.morpho.ui.tool.kit

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/** Render every page of a PDF Uri to bitmaps at the given target width. */
fun renderPdf(ctx: Context, uri: Uri, targetWidth: Int = 1240): List<Bitmap> {
    val out = mutableListOf<Bitmap>()
    try {
        ctx.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                for (i in 0 until renderer.pageCount) {
                    renderer.openPage(i).use { page ->
                        val scale = targetWidth.toFloat() / page.width
                        val w = targetWidth
                        val h = (page.height * scale).toInt().coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(AColor.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        out.add(bmp)
                    }
                }
            }
        }
    } catch (e: Exception) { /* return what we have */ }
    return out
}

/**
 * Blueprint section 26 - explain permissions and corruption when known.
 *
 * renderPdf swallows failures and returns whatever it managed to read, so an
 * encrypted or damaged file comes back as an empty page list and the screen
 * silently shows nothing. Call this only when that happens; the cost is paid
 * on the failure path.
 */
fun pdfFailureReason(ctx: Context, uri: Uri): String = try {
    ctx.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
        PdfRenderer(pfd).use { r ->
            if (r.pageCount == 0) "This PDF has no pages in it."
            else "Morpho could open this PDF but couldn't render its pages."
        }
    } ?: "Morpho couldn't open that file. It may have been moved or deleted."
} catch (e: SecurityException) {
    "This PDF is password-protected. Unlock it first, then try again."
} catch (e: Exception) {
    "This file may be damaged, or it may not really be a PDF."
}

fun pdfPageCount(ctx: Context, uri: Uri): Int = try {
    ctx.contentResolver.openFileDescriptor(uri, "r")?.use {
        PdfRenderer(it).use { r -> r.pageCount }
    } ?: 0
} catch (e: Exception) { 0 }

/** Save raw PDF bytes to Downloads/Morpho. */

/** Save arbitrary bytes to Download/Morpho with a full filename + mime type. */
fun saveBytesToDownloads(ctx: Context, bytes: ByteArray, fileName: String, mime: String): Boolean {
    val ok = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/Morpho")
            }
            val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri == null) false
            else {
                val stream = ctx.contentResolver.openOutputStream(uri)
                if (stream == null) { ctx.contentResolver.delete(uri, null, null); false }
                else { stream.use { it.write(bytes) }; true }
            }
        } else {
            val dir = File(android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS), "Morpho").apply { mkdirs() }
            FileOutputStream(File(dir, fileName)).use { it.write(bytes) }
            true
        }
    } catch (e: Exception) {
        false
    }
    reportSave(ctx, ok, "File ready", "Saved to Downloads/Morpho.", "Saved to Download/Morpho")
    return ok
}

fun savePdfToDownloads(ctx: Context, bytes: ByteArray, name: String): Boolean {
    val ok = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "$name.pdf")
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/Morpho")
            }
            val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri == null) false
            else {
                val stream = ctx.contentResolver.openOutputStream(uri)
                if (stream == null) { ctx.contentResolver.delete(uri, null, null); false }
                else { stream.use { it.write(bytes) }; true }
            }
        } else {
            val dir = File(android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS), "Morpho").apply { mkdirs() }
            FileOutputStream(File(dir, "$name.pdf")).use { it.write(bytes) }
            true
        }
    } catch (e: Exception) {
        false
    }
    reportSave(ctx, ok, "PDF ready", "Your PDF was saved to Downloads/Morpho.",
        "Saved to Download/Morpho")
    return ok
}

/**
 * Blueprint section 26 - report the outcome only once it is known.
 *
 * Every save used to fire markUsed() and the "ready" notification before
 * attempting the write, so a failure left a notification claiming success
 * behind a two-second failure toast.
 */
internal fun reportSave(
    ctx: Context,
    ok: Boolean,
    notifyTitle: String,
    notifyBody: String,
    successToast: String,
    failToast: String = "Couldn't save the file"
) {
    // Section 46: every save in the app funnels through here, so this is the
    // one place completion and failure can be counted without threading a tool
    // id through several dozen call sites.
    cc.devbangs.morpho.data.Stats.recordOutcome(ok)
    if (ok) {
        cc.devbangs.morpho.ads.AdState.markUsed()
        cc.devbangs.morpho.notify.Notifier.notifyDone(ctx, notifyTitle, notifyBody)
        // Blank toast means the screen already shows its own result message.
        if (successToast.isNotEmpty())
            Toast.makeText(ctx, successToast, Toast.LENGTH_SHORT).show()
    } else if (failToast.isNotEmpty()) {
        Toast.makeText(ctx, failToast, Toast.LENGTH_SHORT).show()
    }
}

fun sharePdf(ctx: Context, bytes: ByteArray, name: String) {
    try {
        val dir = File(ctx.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "$name.pdf")
        FileOutputStream(file).use { it.write(bytes) }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(intent, "Share PDF"))
    } catch (e: Exception) {
        Toast.makeText(ctx, "Share failed", Toast.LENGTH_SHORT).show()
    }
}

/** Read raw bytes of a Uri. */
fun readBytes(ctx: Context, uri: Uri): ByteArray? =
    try { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } } catch (e: Exception) { null }

/** Write PDF bytes to a cache file and return a FileProvider Uri for hand-off. */
fun cachePdfForHandoff(ctx: Context, bytes: ByteArray): android.net.Uri? = try {
    val dir = java.io.File(ctx.cacheDir, "chain").apply { mkdirs() }
    val f = java.io.File(dir, "chain_${System.currentTimeMillis()}.pdf")
    java.io.FileOutputStream(f).use { it.write(bytes) }
    androidx.core.content.FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
} catch (e: Exception) { null }

/** Write handed-off bytes to a private cache file, return a file Uri the app can read. */
fun bytesToTempUri(ctx: Context, bytes: ByteArray): android.net.Uri {
    val dir = java.io.File(ctx.cacheDir, "chain").apply { mkdirs() }
    val f = java.io.File(dir, "in_${System.currentTimeMillis()}.pdf")
    java.io.FileOutputStream(f).use { it.write(bytes) }
    return android.net.Uri.fromFile(f)
}
