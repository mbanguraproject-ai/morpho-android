package cc.devbangs.morpho.ui.tool.invoice

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.ByteArrayOutputStream

/** A4 at 150 DPI: 1240 x 1754 px. */
private const val PW = 1240
private const val PH = 1754
private const val M = 90f  // margin

fun renderInvoicePdf(s: InvoiceState): ByteArray {
    val doc = PdfDocument()
    val info = PdfDocument.PageInfo.Builder(PW, PH, 1).create()
    val page = doc.startPage(info)
    val c = page.canvas
    c.drawColor(Color.WHITE)
    val accent = s.accent.value.argb.toInt()

    when (s.template.value) {
        Template.MODERN -> drawModern(c, s, accent)
        Template.CLASSIC -> drawClassic(c, s, accent)
        Template.MINIMAL -> drawMinimal(c, s, accent)
    }
    doc.finishPage(page)
    val out = ByteArrayOutputStream(); doc.writeTo(out); doc.close(); return out.toByteArray()
}

// paints
private fun p(color: Int, size: Float, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT) =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color; textSize = size; textAlign = align
        typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

private const val INK = 0xFF0B0D12.toInt()
private const val SOFT = 0xFF5A6472.toInt()
private const val FAINT = 0xFF9AA3B2.toInt()
private const val LINE = 0xFFE7E9EE.toInt()
private const val SUNK = 0xFFF4F6FB.toInt()

private fun multiline(c: Canvas, text: String, x: Float, y: Float, paint: Paint, lh: Float): Float {
    var yy = y
    text.split("\n").forEach { c.drawText(it, x, yy, paint); yy += lh }
    return yy
}

// ---------- MODERN ----------
private fun drawModern(c: Canvas, s: InvoiceState, accent: Int) {
    // header band
    c.drawRect(0f, 0f, PW.toFloat(), 230f, Paint().apply { color = accent })
    c.drawText(s.bizName.value.ifEmpty { "Your Business" }, M, 120f, p(Color.WHITE, 52f, true))
    c.drawText("INVOICE", PW - M, 100f, p(Color.WHITE, 46f, true, Paint.Align.RIGHT))
    c.drawText(s.invoiceNumber.value, PW - M, 150f, p(0xCCFFFFFF.toInt(), 28f, false, Paint.Align.RIGHT))

    var y = 320f
    // from / to
    c.drawText("FROM", M, y, p(FAINT, 22f, true))
    c.drawText("BILL TO", PW/2f, y, p(FAINT, 22f, true))
    y += 34f
    val fromEnd = multiline(c, listOf(s.bizName.value, s.bizDetails.value, "Tax ID: ${s.bizTaxId.value}")
        .filter { it.isNotBlank() && it != "Tax ID: " }.joinToString("\n"), M, y, p(INK, 26f), 34f)
    val toEnd = multiline(c, listOf(s.clientName.value, s.clientDetails.value,
        if (s.poNumber.value.isNotBlank()) "PO: ${s.poNumber.value}" else "")
        .filter { it.isNotBlank() }.joinToString("\n"), PW/2f, y, p(INK, 26f), 34f)
    y = maxOf(fromEnd, toEnd) + 30f

    // meta strip
    c.drawRoundRect(RectF(M, y, PW - M, y + 90f), 14f, 14f, Paint().apply { color = SUNK })
    val third = (PW - 2*M) / 3f
    metaCell(c, "ISSUED", s.issueDate.value, M + 24f, y + 38f)
    metaCell(c, "DUE", s.dueDate.value, M + third + 24f, y + 38f)
    metaCell(c, "CURRENCY", s.currency.value, M + 2*third + 24f, y + 38f)
    y += 140f

    // table header
    y = drawTable(c, s, y, accent)

    // totals
    y += 20f
    y = drawTotals(c, s, y, accent, boxed = true)

    // payment
    if (s.payment.value.isNotBlank() || s.notes.value.isNotBlank()) {
        y += 40f
        c.drawText("PAYMENT", M, y, p(FAINT, 22f, true)); y += 32f
        y = multiline(c, s.payment.value, M, y, p(SOFT, 24f), 32f)
        if (s.notes.value.isNotBlank()) { y += 16f; multiline(c, s.notes.value, M, y, p(SOFT, 24f), 32f) }
    }
}

private fun metaCell(c: Canvas, label: String, value: String, x: Float, y: Float) {
    c.drawText(label, x, y, p(FAINT, 20f, true))
    c.drawText(value.ifEmpty { "—" }, x, y + 34f, p(INK, 26f, true))
}

// ---------- CLASSIC ----------
private fun drawClassic(c: Canvas, s: InvoiceState, accent: Int) {
    c.drawText(s.bizName.value.ifEmpty { "Your Business" }, M, 110f, p(INK, 48f, true))
    c.drawText("INVOICE", PW - M, 100f, p(accent, 50f, true, Paint.Align.RIGHT))
    c.drawText(s.invoiceNumber.value, PW - M, 145f, p(SOFT, 26f, false, Paint.Align.RIGHT))
    c.drawRect(M, 165f, PW - M, 171f, Paint().apply { color = accent })

    var y = 250f
    c.drawText("FROM", M, y, p(FAINT, 22f, true)); c.drawText("BILL TO", PW/2f, y, p(FAINT, 22f, true))
    y += 34f
    val a = multiline(c, listOf(s.bizName.value, s.bizDetails.value, "Tax ID: ${s.bizTaxId.value}")
        .filter { it.isNotBlank() && it != "Tax ID: " }.joinToString("\n"), M, y, p(INK, 26f), 34f)
    val b = multiline(c, listOf(s.clientName.value, s.clientDetails.value,
        if (s.poNumber.value.isNotBlank()) "PO: ${s.poNumber.value}" else "").filter { it.isNotBlank() }
        .joinToString("\n"), PW/2f, y, p(INK, 26f), 34f)
    y = maxOf(a, b) + 20f
    c.drawText("Issued ${s.issueDate.value}   ·   Due ${s.dueDate.value}", M, y, p(SOFT, 24f)); y += 50f
    y = drawTable(c, s, y, accent)
    y += 20f; y = drawTotals(c, s, y, accent, boxed = true)
    if (s.payment.value.isNotBlank()) { y += 40f; c.drawText("PAYMENT", M, y, p(FAINT, 22f, true)); y += 32f
        y = multiline(c, s.payment.value, M, y, p(SOFT, 24f), 32f) }
    if (s.notes.value.isNotBlank()) { y += 16f; multiline(c, s.notes.value, M, y, p(SOFT, 24f), 32f) }
}

// ---------- MINIMAL ----------
private fun drawMinimal(c: Canvas, s: InvoiceState, accent: Int) {
    c.drawText("I N V O I C E", M, 110f, p(INK, 40f, true))
    c.drawText("${s.invoiceNumber.value}  ·  ${s.issueDate.value}", M, 150f, p(FAINT, 24f))
    var y = 240f
    val a = multiline(c, listOf(s.bizName.value, s.bizDetails.value).filter { it.isNotBlank() }.joinToString("\n"),
        M, y, p(INK, 26f), 34f)
    c.drawText("TO", PW/2f, y, p(FAINT, 20f, true))
    val b = multiline(c, listOf(s.clientName.value, s.clientDetails.value).filter { it.isNotBlank() }.joinToString("\n"),
        PW/2f, y + 30f, p(INK, 26f), 34f)
    y = maxOf(a, b) + 40f
    y = drawTable(c, s, y, accent, minimal = true)
    y += 20f; y = drawTotals(c, s, y, accent, boxed = false)
    if (s.notes.value.isNotBlank()) { y += 40f; multiline(c, s.notes.value, M, y, p(SOFT, 24f), 32f) }
}

// ---------- shared table ----------
private fun drawTable(c: Canvas, s: InvoiceState, startY: Float, accent: Int, minimal: Boolean = false): Float {
    var y = startY
    val xDesc = M; val xQty = PW - M - 520f; val xRate = PW - M - 300f; val xAmt = PW - M
    c.drawText("DESCRIPTION", xDesc, y, p(FAINT, 20f, true))
    c.drawText("QTY", xQty, y, p(FAINT, 20f, true, Paint.Align.CENTER))
    c.drawText("RATE", xRate, y, p(FAINT, 20f, true, Paint.Align.RIGHT))
    c.drawText("AMOUNT", xAmt, y, p(FAINT, 20f, true, Paint.Align.RIGHT))
    y += 14f
    c.drawLine(M, y, PW - M, y, Paint().apply { color = LINE; strokeWidth = 2f })
    y += 40f
    s.items.forEach { it ->
        if (it.description.value.isBlank() && it.amount == 0.0) return@forEach
        c.drawText(it.description.value.ifEmpty { "Item" }, xDesc, y, p(INK, 26f))
        c.drawText(it.qty.value, xQty, y, p(SOFT, 26f, false, Paint.Align.CENTER))
        c.drawText(fmtNum(it.rate.value.toDoubleOrNull() ?: 0.0), xRate, y, p(SOFT, 26f, false, Paint.Align.RIGHT))
        c.drawText(fmtNum(it.amount), xAmt, y, p(INK, 26f, true, Paint.Align.RIGHT))
        y += 30f
        c.drawLine(M, y, PW - M, y, Paint().apply { color = 0xFFF4F6FB.toInt(); strokeWidth = 1.5f })
        y += 30f
    }
    return y
}

private fun drawTotals(c: Canvas, s: InvoiceState, startY: Float, accent: Int, boxed: Boolean): Float {
    var y = startY + 10f
    val labelX = PW - M - 340f; val valX = PW - M
    fun row(l: String, v: String, strong: Boolean = false) {
        c.drawText(l, labelX, y, p(if (strong) INK else SOFT, 26f, strong, Paint.Align.LEFT))
        c.drawText(v, valX, y, p(if (strong) INK else SOFT, 26f, strong, Paint.Align.RIGHT))
        y += 40f
    }
    row("Subtotal", s.money(s.subtotal))
    if ((s.discountRate.value.toDoubleOrNull() ?: 0.0) > 0)
        row("Discount (${s.discountRate.value}%)", "− ${s.money(s.discountAmt)}")
    if ((s.taxRate.value.toDoubleOrNull() ?: 0.0) > 0)
        row("${s.taxLabel.value} (${s.taxRate.value}%)", s.money(s.taxAmt))
    y += 6f
    if (boxed) {
        c.drawRoundRect(RectF(labelX - 24f, y - 6f, PW - M, y + 66f), 12f, 12f, Paint().apply { color = accent })
        c.drawText("TOTAL DUE", labelX, y + 44f, p(0xE6FFFFFF.toInt(), 24f, true))
        c.drawText(s.money(s.total), PW - M - 24f, y + 46f, p(Color.WHITE, 34f, true, Paint.Align.RIGHT))
        y += 90f
    } else {
        c.drawLine(labelX, y, PW - M, y, Paint().apply { color = INK; strokeWidth = 3f }); y += 44f
        c.drawText("Total", labelX, y, p(INK, 30f, true))
        c.drawText(s.money(s.total), PW - M, y, p(INK, 30f, true, Paint.Align.RIGHT)); y += 40f
    }
    return y
}

private fun fmtNum(v: Double): String {
    val l = Math.round(v)
    return "%,d".format(l)
}

/** Preview bitmap (first page) for on-screen rendering. */
fun renderInvoiceBitmap(s: InvoiceState): Bitmap {
    val bmp = Bitmap.createBitmap(PW, PH, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp); c.drawColor(Color.WHITE)
    val accent = s.accent.value.argb.toInt()
    when (s.template.value) {
        Template.MODERN -> drawModern(c, s, accent)
        Template.CLASSIC -> drawClassic(c, s, accent)
        Template.MINIMAL -> drawMinimal(c, s, accent)
    }
    return bmp
}
