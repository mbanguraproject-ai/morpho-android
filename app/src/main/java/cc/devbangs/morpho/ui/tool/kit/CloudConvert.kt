package cc.devbangs.morpho.ui.tool.kit

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** Base URL of the Morpho conversion Worker. */
private const val CONVERT_BASE = "https://morpho-convert.secretsafe-cc.workers.dev"

/**
 * Send a file to the conversion Worker and get the converted bytes back.
 * Runs a blocking network call — call from Dispatchers.IO.
 * Returns the converted File in cache, or null on failure.
 */
fun cloudConvert(ctx: Context, uri: Uri, from: String, to: String, outName: String, extraParams: String = ""): File? {
    return try {
        val input = ctx.contentResolver.openInputStream(uri)?.readBytes() ?: return null
        val ext = from.ifBlank { "bin" }
        val fromParam = if (from.isBlank()) "" else "from=$from&"
        val url = URL("$CONVERT_BASE/?${fromParam}to=$to&name=input.$ext$extraParams")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 30000
            readTimeout = 120000   // conversions can take a bit
            setRequestProperty("Content-Type", "application/octet-stream")
        }
        conn.outputStream.use { it.write(input) }

        val code = conn.responseCode
        if (code != 200) {
            conn.disconnect()
            return null
        }
        val outBytes = conn.inputStream.use { it.readBytes() }
        conn.disconnect()
        if (outBytes.isEmpty()) return null

        val out = File(ctx.cacheDir, outName)
        FileOutputStream(out).use { it.write(outBytes) }
        out
    } catch (e: Exception) { null }
}
