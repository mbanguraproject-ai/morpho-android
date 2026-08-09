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
        // --- Document BUILDERS: you just created/assembled a PDF, natural to finish it ---
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
            NextStep("pdf-watermark", "Add Watermark", "Mark your document"),
        ),

        // --- Document PREPARERS: you marked/organized a doc, natural to finalize it ---
        "pdf-watermark" to listOf(
            NextStep("pdf-compressor", "Compress PDF", "Shrink after watermarking"),
            NextStep("pdf-password-protector", "Password Protect", "Lock the final file"),
        ),
        "pdf-page-numbering" to listOf(
            NextStep("pdf-watermark", "Add Watermark", "Brand the numbered pages"),
            NextStep("pdf-password-protector", "Password Protect", "Secure the final file"),
        ),
        "pdf-splitter" to listOf(
            NextStep("pdf-compressor", "Compress PDF", "Shrink the split output"),
            NextStep("pdf-password-protector", "Password Protect", "Secure the extract"),
        ),
        "pdf-page-extractor" to listOf(
            NextStep("pdf-compressor", "Compress PDF", "Shrink the extracted pages"),
            NextStep("pdf-password-protector", "Password Protect", "Secure the extract"),
        ),

        // --- ENDPOINTS (deliberately no suggestions — chaining would be pointless or harmful) ---
        // pdf-compressor: final optimization. Watermarking would re-inflate; only a light "protect" makes sense.
        "pdf-compressor" to listOf(
            NextStep("pdf-password-protector", "Password Protect", "Secure the smaller file"),
        ),
        // pdf-password-protector: ALWAYS the last step. No entry.
        // pdf-unlocker: one-time utility, unlock to use elsewhere. No entry.
        // pdf-to-jpg: leaves the PDF family (outputs images). No entry.
        // pdf-page-rotator: natural endpoint. No entry.
    )

    fun nextSteps(toolId: String): List<NextStep> = graph[toolId].orEmpty()
}
