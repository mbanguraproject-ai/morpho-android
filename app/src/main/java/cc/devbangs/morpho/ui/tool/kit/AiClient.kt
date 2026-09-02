package cc.devbangs.morpho.ui.tool.kit

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

/**
 * Client for the AI routes on the Morpho Worker.
 *
 * The Groq key lives as a Worker secret and never reaches the app - the same
 * arrangement as the conversion key. The Worker's subscription gate runs before
 * these routes, so they are Plus-only, which is as much a capacity decision as
 * a pricing one: Groq's free tier allows 30 requests a minute for the whole
 * organisation rather than per user, so demand has to be bounded by something.
 */
sealed interface AiOutcome {
    data class Success(val text: String) : AiOutcome
    /** Written for the person reading it, not for a log. */
    data class Failure(val reason: String) : AiOutcome
}

private const val AI_BASE = "https://morpho-convert.secretsafe-cc.workers.dev"

private fun HttpURLConnection.applyMorphoAuth() {
    setRequestProperty("X-Morpho-Key", SHARED_SECRET)
    cc.devbangs.morpho.billing.BillingManager.activeToken?.let {
        setRequestProperty("X-Morpho-Token", it)
    }
    cc.devbangs.morpho.billing.BillingManager.activeProductId?.let {
        setRequestProperty("X-Morpho-Product", it)
    }
}

/**
 * Send text to the Worker for one of its defined tasks.
 * Blocking; call from Dispatchers.IO.
 */
fun aiText(
    task: String,
    input: String,
    options: Map<String, String> = emptyMap()
): AiOutcome = try {
    val payload = JSONObject().apply {
        put("task", task)
        put("input", input)
        if (options.isNotEmpty()) {
            put("options", JSONObject().apply { options.forEach { (k, v) -> put(k, v) } })
        }
    }.toString()

    val conn = (URL("$AI_BASE/ai").openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        doOutput = true
        connectTimeout = 20000
        readTimeout = 90000
        setRequestProperty("Content-Type", "application/json")
        applyMorphoAuth()
    }
    conn.outputStream.use { it.write(payload.toByteArray()) }
    readAiResponse(conn)
} catch (e: UnknownHostException) {
    AiOutcome.Failure("No internet connection. This tool needs one.")
} catch (e: ConnectException) {
    AiOutcome.Failure("Morpho couldn't reach the server. Try again shortly.")
} catch (e: SocketTimeoutException) {
    AiOutcome.Failure("That took too long. Try again, or shorten the text.")
} catch (e: Exception) {
    AiOutcome.Failure("Something went wrong. Try again.")
}

/**
 * Transcribe audio. Blocking; call from Dispatchers.IO.
 * The Worker caps uploads at 24MB, which is Whisper's own limit.
 */
fun aiTranscribe(ctx: Context, uri: Uri, language: String? = null): AiOutcome = try {
    val bytes = ctx.contentResolver.openInputStream(uri)?.readBytes()
    when {
        bytes == null -> AiOutcome.Failure("Morpho couldn't read that file.")
        bytes.isEmpty() -> AiOutcome.Failure("That file is empty.")
        bytes.size > 24 * 1024 * 1024 ->
            AiOutcome.Failure(
                "That recording is over 24MB. Trim it first, or use a smaller file."
            )
        else -> {
            val lang = language?.let { "&lang=$it" }.orEmpty()
            val conn = (URL("$AI_BASE/transcribe?name=audio.m4a$lang").openConnection()
                as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 20000
                readTimeout = 180000   // a long recording takes a while
                setRequestProperty("Content-Type", "application/octet-stream")
                applyMorphoAuth()
            }
            conn.outputStream.use { it.write(bytes) }
            readAiResponse(conn)
        }
    }
} catch (e: UnknownHostException) {
    AiOutcome.Failure("No internet connection. This tool needs one.")
} catch (e: SocketTimeoutException) {
    AiOutcome.Failure("That took too long. Try a shorter recording.")
} catch (e: Exception) {
    AiOutcome.Failure("Something went wrong. Try again.")
}

private fun readAiResponse(conn: HttpURLConnection): AiOutcome {
    val code = conn.responseCode
    if (code != 200) {
        val detail = runCatching { conn.errorStream?.bufferedReader()?.readText() }
            .getOrNull().orEmpty()
        conn.disconnect()
        return AiOutcome.Failure(reasonForAiCode(code, detail))
    }
    val body = conn.inputStream.bufferedReader().use { it.readText() }
    conn.disconnect()
    val text = runCatching { JSONObject(body).optString("text") }.getOrDefault("")
    // Whisper answers silence with punctuation - a lone " ." - and a result card
    // holding a full stop is worse than being told nothing was heard.
    return if (looksEmpty(text))
        AiOutcome.Failure(
            "Morpho couldn't hear any speech in that. Check the recording has " +
                "someone talking, and that it isn't too quiet."
        )
    else AiOutcome.Success(text.trim())
}


/** Nothing a person would call a result: blank, or only punctuation. */
private fun looksEmpty(text: String): Boolean =
    text.none { it.isLetterOrDigit() }

/**
 * Rate limits, not cost, are what actually stops a Groq request, so a 429 gets
 * its own wording instead of being folded into a generic failure.
 */
private fun reasonForAiCode(code: Int, detail: String): String = when (code) {
    401 -> "Morpho couldn't authenticate with the server."
    403 -> "This tool is part of Morpho Plus."
    413 -> "That input is too large. Try a shorter piece."
    429 -> "Morpho's AI tools are busy right now. Wait a minute and try again."
    in 500..599 -> "The server had a problem. Try again shortly."
    else -> "That didn't work (error $code)."
}
