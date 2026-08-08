package cc.devbangs.morpho.data

import androidx.compose.ui.graphics.Color
import cc.devbangs.morpho.ui.theme.*

enum class ToolCategory(
    val id: String,
    val label: String,
    val blurb: String,
    val accent: Color
) {
    PDF("pdf", "PDF", "Convert, sign, merge & edit PDFs", CatPdf),
    IMAGE("image", "Image", "Compress, resize & retouch images", CatImage),
    CONVERTER("converter", "Convert", "Swap between file & media formats", CatConverter),
    VIDEO("video", "Video", "Trim, compress & convert video", CatVideo),
    AUDIO("audio", "Audio", "Record, trim & convert audio", CatAudio),
    TEXT("text", "Text", "Extract, count & transform text", CatText),
    GENERATOR("generator", "Generate", "QR, passwords, invoices & more", CatGenerator),
    DEVELOPER("developer", "Developer", "JSON, color, hashing & encoding", CatDeveloper),
    AI("ai", "AI", "Smart writing & image helpers", CatAi);

    companion object {
        fun from(id: String): ToolCategory = entries.first { it.id == id }
    }
}
