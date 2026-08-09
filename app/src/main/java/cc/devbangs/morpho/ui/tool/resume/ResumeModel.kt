package cc.devbangs.morpho.ui.tool.resume

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf

class ExperienceEntry(
    role: String = "", company: String = "", dates: String = "", bullets: String = ""
) {
    var role = mutableStateOf(role)
    var company = mutableStateOf(company)
    var dates = mutableStateOf(dates)          // "Jan 2023 – Present"
    var bullets = mutableStateOf(bullets)      // newline-separated achievements
}

class EducationEntry(
    school: String = "", detail: String = "", dates: String = ""
) {
    var school = mutableStateOf(school)
    var detail = mutableStateOf(detail)        // degree / field
    var dates = mutableStateOf(dates)
}

enum class ResumeLayout(val label: String) {
    CLEAN("ATS Clean"),         // accent on name only
    PROFESSIONAL("Professional"), // accent header underlines
    CONTEMPORARY("Contemporary")  // accent name + header rules
}

// Restrained, ATS-safe accent colors (name/headings only) per 2026 guidance
data class ResumeAccent(val label: String, val argb: Long)
val RESUME_ACCENTS = listOf(
    ResumeAccent("Navy", 0xFF1E3A8A),
    ResumeAccent("Slate", 0xFF334155),
    ResumeAccent("Green", 0xFF166534),
    ResumeAccent("Cobalt", 0xFF1A46E5),
    ResumeAccent("Ink", 0xFF0B0D12),
    ResumeAccent("Maroon", 0xFF7F1D1D),
)

class ResumeState {
    // Contact
    val name = mutableStateOf("")
    val title = mutableStateOf("")             // e.g. "Android Developer"
    val contact = mutableStateOf("")           // "email · phone · city · linkedin" (one line)
    // Summary
    val summary = mutableStateOf("")
    // Sections
    val experience = mutableStateListOf(ExperienceEntry())
    val education = mutableStateListOf(EducationEntry())
    val skills = mutableStateOf("")            // comma-separated
    // Style
    val layout = mutableStateOf(ResumeLayout.CLEAN)
    val accent = mutableStateOf(RESUME_ACCENTS[0])
}
