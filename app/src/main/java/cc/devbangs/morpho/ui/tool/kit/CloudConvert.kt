package cc.devbangs.morpho.ui.tool.kit

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

/** Base URL of the Morpho conversion Worker. */
private const val CONVERT_BASE = "https://morpho-convert.secretsafe-cc.workers.dev"
private const val SHARED_SECRET = "ZQP2uWmHhajJzjUKm358h-ot1D4_INu9bHU7Q-kPKbI"

/**
 * Outcome of a Worker conversion.
 *
 * Blueprint sections 26 and 28: the caller used to get null for everything -
 * offline, timeout, file too large, auth rejected, server error - and told
 * every user to check their connection, including the ones whose connection
 * was fine.
 */
sealed interface ConvertOutcome {
    data class Success(val file: File) : ConvertOutcome
    /** [reason] is written for the user, not for a log. */
    data class Failure(val reason: String) : ConvertOutcome
}

/**
 * Send a file to the conversion Worker and get the converted bytes back.
 * Runs a blocking network call — call from Dispatchers.IO.
 */
fun cloudConvert(
    ctx: Context,
    uri: Uri,
    from: String,
    to: String,
    outName: String,
    extraParams: String = ""
): ConvertOutcome {
    return try {
        val input = ctx.contentResolver.openInputStream(uri)?.readBytes()
            ?: return ConvertOutcome.Failure(
                "Morpho couldn't read that file. It may have been moved or deleted."
            )
        val ext = from.ifBlank { "bin" }
        val fromParam = if (from.isBlank()) "" else "from=$from&"
        val url = URL("$CONVERT_BASE/?${fromParam}to=$to&name=input.$ext$extraParams")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 30000
            readTimeout = 120000   // conversions can take a bit
            setRequestProperty("Content-Type", "application/octet-stream")
            setRequestProperty("X-Morpho-Key", SHARED_SECRET)
            cc.devbangs.morpho.billing.BillingManager.activeToken?.let {
                setRequestProperty("X-Morpho-Token", it)
            }
            cc.devbangs.morpho.billing.BillingManager.activeProductId?.let {
                setRequestProperty("X-Morpho-Product", it)
            }
        }
        conn.outputStream.use { it.write(input) }

        val code = conn.responseCode
        if (code != 200) {
            conn.disconnect()
            return ConvertOutcome.Failure(reasonForCode(code))
        }
        val outBytes = conn.inputStream.use { it.readBytes() }
        conn.disconnect()
        if (outBytes.isEmpty()) return ConvertOutcome.Failure(
            "The conversion came back empty. This file may not be supported."
        )

        val out = File(ctx.cacheDir, outName)
        FileOutputStream(out).use { it.write(outBytes) }
        ConvertOutcome.Success(out)
    } catch (e: UnknownHostException) {
        ConvertOutcome.Failure("You appear to be offline. This tool needs an internet connection.")
    } catch (e: ConnectException) {
        ConvertOutcome.Failure("Morpho couldn't reach the conversion service. Try again in a moment.")
    } catch (e: SocketTimeoutException) {
        ConvertOutcome.Failure("This took too long and timed out. Try again, or try a smaller file.")
    } catch (e: Exception) {
        ConvertOutcome.Failure("Something went wrong during the conversion. Try again.")
    }
}

/** Section 28: name the case when the server tells us what it was. */
private fun reasonForCode(code: Int): String = when (code) {
    413 -> "That file is too large to convert. Try a smaller one."
    415 -> "That file type isn't supported for this conversion."
    401, 403 -> "Morpho couldn't authorise this conversion. Try again later."
    429 -> "Too many conversions right now. Wait a moment and try again."
    in 500..599 -> "The conversion service is having trouble. Try again shortly."
    else -> "The service couldn't convert this file (error $code)."
}
