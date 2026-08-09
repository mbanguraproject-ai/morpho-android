package cc.devbangs.morpho.ui.tool.resume

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.ByteArrayOutputStream

// US Letter at 150 DPI
private const val PW = 1275
private const val PH = 1650
private const val M = 90f

private const val INK = 0xFF1A1A1A.toInt()
private const val SOFT = 0xFF4A4A4A.toInt()
private const val LINE = 0xFFCCCCCC.toInt()

private fun p(color: Int, size: Float, bold: Boolean = false) =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color; textSize = size
        typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

/** Wrap text to a width, return the lines. */
private fun wrap(text: String, paint: Paint, maxW: Float): List<String> {
    val out = mutableListOf<String>()
    text.split("\n").forEach { para ->
        if (para.isBlank()) { out.add(""); return@forEach }
        var line = StringBuilder()
        para.split(" ").forEach { word ->
            val test = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(test) > maxW && line.isNotEmpty()) {
                out.add(line.toString()); line = StringBuilder(word)
            } else line = StringBuilder(test)
        }
        if (line.isNotEmpty()) out.add(line.toString())
    }
    return out
}

private class Cursor(val c: Canvas, var y: Float, val accent: Int) {
    val contentW = PW - 2 * M
    fun text(s: String, paint: Paint, x: Float = M, lh: Float = 0f) {
        c.drawText(s, x, y, paint); y += if (lh > 0) lh else paint.textSize * 1.35f
    }
    fun para(s: String, paint: Paint, x: Float = M, indent: Float = 0f) {
        wrap(s, paint, contentW - indent).forEach { c.drawText(it, x + indent, y, paint); y += paint.textSize * 1.35f }
    }
    fun gap(h: Float) { y += h }
    fun rule() { c.drawLine(M, y, PW - M, y, Paint().apply { color = LINE; strokeWidth = 2f }); y += 4f }
    fun accentRule() { c.drawLine(M, y, PW - M, y, Paint().apply { color = accent; strokeWidth = 3f }); y += 4f }
}

private fun draw(c: Canvas, s: ResumeState) {
    val accent = s.accent.value.argb.toInt()
    val cur = Cursor(c, M + 20f, accent)
    val nameColor = if (s.layout.value == ResumeLayout.CLEAN || s.layout.value == ResumeLayout.CONTEMPORARY) accent else INK

    // ---- Contact header ----
    cur.text(s.name.value.ifEmpty { "Your Name" }, p(nameColor, 48f, true))
    if (s.title.value.isNotBlank()) cur.text(s.title.value, p(SOFT, 26f))
    if (s.contact.value.isNotBlank()) { cur.gap(4f); cur.para(s.contact.value, p(SOFT, 22f)) }
    cur.gap(14f)
    if (s.layout.value == ResumeLayout.CONTEMPORARY) cur.accentRule() else cur.rule()
    cur.gap(20f)

    fun heading(t: String) {
        val hc = if (s.layout.value == ResumeLayout.CLEAN) INK else accent
        cur.text(t.uppercase(), p(hc, 24f, true))
        if (s.layout.value == ResumeLayout.PROFESSIONAL) { cur.accentRule() }
        else { cur.rule() }
        cur.gap(12f)
    }

    // ---- Summary ----
    if (s.summary.value.isNotBlank()) {
        heading("Professional Summary")
        cur.para(s.summary.value, p(INK, 23f)); cur.gap(22f)
    }

    // ---- Experience ----
    val exp = s.experience.filter { it.role.value.isNotBlank() || it.company.value.isNotBlank() }
    if (exp.isNotEmpty()) {
        heading("Work Experience")
        exp.forEach { e ->
            // role + dates on one line (role left, dates right)
            c.drawText(e.role.value.ifEmpty { "Role" }, M, cur.y, p(INK, 24f, true))
            if (e.dates.value.isNotBlank()) {
                val dp = p(SOFT, 22f); dp.textAlign = Paint.Align.RIGHT
                c.drawText(e.dates.value, (PW - M), cur.y, dp)
            }
            cur.y += 30f
            if (e.company.value.isNotBlank()) cur.text(e.company.value, p(SOFT, 22f))
            cur.gap(4f)
            e.bullets.value.split("\n").filter { it.isNotBlank() }.forEach { b ->
                val bp = p(INK, 22f)
                wrap("•  ${b.trim()}", bp, cur.contentW - 20f).forEachIndexed { i, ln ->
                    c.drawText(if (i == 0) ln else "    ${ln}", M + 10f, cur.y, bp); cur.y += bp.textSize * 1.35f
                }
            }
            cur.gap(16f)
        }
        cur.gap(6f)
    }

    // ---- Education ----
    val edu = s.education.filter { it.school.value.isNotBlank() }
    if (edu.isNotEmpty()) {
        heading("Education")
        edu.forEach { e ->
            c.drawText(e.school.value, M, cur.y, p(INK, 24f, true))
            if (e.dates.value.isNotBlank()) {
                val dp = p(SOFT, 22f); dp.textAlign = Paint.Align.RIGHT
                c.drawText(e.dates.value, PW - M, cur.y, dp)
            }
            cur.y += 30f
            if (e.detail.value.isNotBlank()) cur.text(e.detail.value, p(SOFT, 22f))
            cur.gap(14f)
        }
        cur.gap(6f)
    }

    // ---- Skills ----
    if (s.skills.value.isNotBlank()) {
        heading("Skills")
        val skills = s.skills.value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        cur.para(skills.joinToString("  •  "), p(INK, 23f))
    }
}

fun renderResumePdf(s: ResumeState): ByteArray {
    val doc = PdfDocument()
    val info = PdfDocument.PageInfo.Builder(PW, PH, 1).create()
    val page = doc.startPage(info)
    page.canvas.drawColor(Color.WHITE)
    draw(page.canvas, s)
    doc.finishPage(page)
    val out = ByteArrayOutputStream(); doc.writeTo(out); doc.close(); return out.toByteArray()
}

fun renderResumeBitmap(s: ResumeState): Bitmap {
    val bmp = Bitmap.createBitmap(PW, PH, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp); c.drawColor(Color.WHITE)
    draw(c, s)
    return bmp
}
