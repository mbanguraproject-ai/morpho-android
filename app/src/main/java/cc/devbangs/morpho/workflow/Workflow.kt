package cc.devbangs.morpho.workflow

import android.net.Uri
import androidx.compose.runtime.mutableStateOf

/**
 * In-memory hand-off bus. When a tool finishes and the user taps a suggested
 * next tool, the output file is parked here; the receiving tool consumes it on open.
 */
object WorkflowBus {
    // Bytes passed from a previous tool (consumed once). Carrying bytes directly
    // avoids FileProvider path config entirely — robust for internal hand-off.
    private val pending = mutableStateOf<PendingFile?>(null)

    fun handOff(bytes: ByteArray, mime: String) { pending.value = PendingFile(bytes, mime) }

    /** Consume the pending file if present (returns once, then clears). */
    fun consume(): PendingFile? {
        val p = pending.value
        pending.value = null
        return p
    }

    fun peek(): PendingFile? = pending.value
    fun clear() { pending.value = null }
}

data class PendingFile(val bytes: ByteArray, val mime: String)

/** A suggested next step after a tool produces output. */
data class NextStep(val toolId: String, val label: String, val reason: String)

/**
 * The workflow graph: which tool suggests which next tools.
 * Only chains where passing the output forward genuinely makes sense.
 */
object WorkflowGraph {
    private val graph: Map<String, List<NextStep>> = mapOf(
        // PDF chain
        "merge-pdf" to listOf(
            NextStep("pdf-compressor", "Compress PDF", "Merged PDFs are often large"),
            NextStep("pdf-watermark", "Add Watermark", "Brand or mark the document"),
            NextStep("pdf-password-protector", "Password Protect", "Secure the final file"),
        ),
        "jpg-to-pdf" to listOf(
            NextStep("pdf-compressor", "Compress PDF", "Shrink the new PDF"),
            NextStep("pdf-watermark", "Add Watermark", "Mark your document"),
        ),
        "image-to-pdf" to listOf(
            NextStep("pdf-compressor", "Compress PDF", "Shrink the new PDF"),
        ),
        "pdf-compressor" to listOf(
            NextStep("pdf-password-protector", "Password Protect", "Secure the smaller file"),
            NextStep("pdf-watermark", "Add Watermark", "Mark the document"),
        ),
        "pdf-watermark" to listOf(
            NextStep("pdf-compressor", "Compress PDF", "Shrink after watermarking"),
            NextStep("pdf-password-protector", "Password Protect", "Lock the final file"),
        ),
        "pdf-page-numbering" to listOf(
            NextStep("pdf-compressor", "Compress PDF", "Shrink the file"),
        ),
        "pdf-splitter" to listOf(
            NextStep("pdf-compressor", "Compress PDF", "Shrink the split output"),
        ),
    )

    fun nextSteps(toolId: String): List<NextStep> = graph[toolId].orEmpty()
}
