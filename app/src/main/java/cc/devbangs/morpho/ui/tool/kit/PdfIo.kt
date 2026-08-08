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

fun pdfPageCount(ctx: Context, uri: Uri): Int = try {
    ctx.contentResolver.openFileDescriptor(uri, "r")?.use {
        PdfRenderer(it).use { r -> r.pageCount }
    } ?: 0
} catch (e: Exception) { 0 }

/** Save raw PDF bytes to Downloads/Morpho. */
fun savePdfToDownloads(ctx: Context, bytes: ByteArray, name: String): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "$name.pdf")
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/Morpho")
            }
            val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return false
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
        } else {
            val dir = File(android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS), "Morpho").apply { mkdirs() }
            FileOutputStream(File(dir, "$name.pdf")).use { it.write(bytes) }
        }
        Toast.makeText(ctx, "Saved to Download/Morpho", Toast.LENGTH_SHORT).show()
        true
    } catch (e: Exception) {
        Toast.makeText(ctx, "Save failed", Toast.LENGTH_SHORT).show(); false
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
