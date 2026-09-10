package cc.devbangs.morpho.ui.tool.invoice

import cc.devbangs.morpho.data.invoice.InvoiceItemRecord
import cc.devbangs.morpho.data.invoice.InvoiceRecord
import cc.devbangs.morpho.data.invoice.InvoiceWithItems
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import kotlin.math.roundToLong

/** A single billable line. Amounts held as strings for editable fields; math via doubles. */
class LineItem(
    description: String = "",
    qty: String = "1",
    rate: String = "0"
) {
    var description = mutableStateOf(description)
    var qty = mutableStateOf(qty)
    var rate = mutableStateOf(rate)
    val amount: Double
        get() = (qty.value.toDoubleOrNull() ?: 0.0) * (rate.value.toDoubleOrNull() ?: 0.0)
}

enum class Template(val label: String) { MODERN("Modern"), CLASSIC("Classic"), MINIMAL("Minimal") }
enum class DocType(val title: String, val numberLabel: String, val numberPrefix: String) {
    INVOICE("INVOICE", "INVOICE #", "INV"),
    RECEIPT("RECEIPT", "RECEIPT #", "RCT"),
    QUOTE("QUOTATION", "QUOTE #", "QUO")
}

data class AccentOption(val label: String, val argb: Long)

val ACCENTS = listOf(
    AccentOption("Cobalt", 0xFF1A46E5),
    AccentOption("Teal",   0xFF0E7C86),
    AccentOption("Ink",    0xFF0B0D12),
    AccentOption("Amber",  0xFFB45309),
    AccentOption("Violet", 0xFF6A4BD6),
    AccentOption("Magenta",0xFF9333AA),
)

val CURRENCIES = listOf("Le", "$", "€", "£", "₦", "₵", "GH₵", "KSh", "R", "₹")

/** Full invoice state. All fields are Compose-observable. */
class InvoiceState {
    /** 0 until this invoice has been saved once. */
    var recordId: Long = 0L
    var seq: Int = 1
    var createdAt: Long = 0L
    // From (business)
    val bizName = mutableStateOf("")
    val bizDetails = mutableStateOf("")      // address / phone / email (multiline)
    val bizTaxId = mutableStateOf("")
    // Bill to
    val clientName = mutableStateOf("")
    val clientDetails = mutableStateOf("")
    val poNumber = mutableStateOf("")
    // Meta
    val docType = mutableStateOf(DocType.INVOICE)
    // Filled by nextNumber() for a new document, or by loadFrom() for a saved
    // one. A default here would only ever be wrong.
    val invoiceNumber = mutableStateOf("")
    val validUntil = mutableStateOf("")      // quotes only
    val issueDate = mutableStateOf("")
    val dueDate = mutableStateOf("")
    val currency = mutableStateOf("Le")
    // Items
    val items = mutableStateListOf(LineItem("Service or product", "1", "0"))
    // Totals config
    val taxLabel = mutableStateOf("GST")
    val taxRate = mutableStateOf("0")        // percent
    val discountRate = mutableStateOf("0")   // percent
    // Payment / notes
    val payment = mutableStateOf("")
    val notes = mutableStateOf("Thank you for your business.")
    // Style
    val template = mutableStateOf(Template.MODERN)
    val accent = mutableStateOf(ACCENTS[0])

    val subtotal: Double get() = items.sumOf { it.amount }
    val discountAmt: Double get() = subtotal * (discountRate.value.toDoubleOrNull() ?: 0.0) / 100.0
    val taxable: Double get() = subtotal - discountAmt
    val taxAmt: Double get() = taxable * (taxRate.value.toDoubleOrNull() ?: 0.0) / 100.0
    val total: Double get() = taxable + taxAmt

    fun money(v: Double): String {
        val cents = (v * 100).roundToLong()
        val whole = cents / 100; val freq = (cents % 100).toInt()
        val grouped = "%,d".format(whole)
        return "${currency.value} $grouped.${"%02d".format(kotlin.math.abs(freq))}"
    }
}

/** Editor state to stored record. */
fun InvoiceState.toRecord(now: Long = System.currentTimeMillis()): InvoiceRecord = InvoiceRecord(
    id = recordId,
    docType = docType.value.name,
    seq = seq,
    number = invoiceNumber.value,
    issueDate = issueDate.value,
    dueDate = dueDate.value,
    validUntil = validUntil.value,
    currency = currency.value,
    bizName = bizName.value,
    bizDetails = bizDetails.value,
    bizTaxId = bizTaxId.value,
    clientName = clientName.value,
    clientDetails = clientDetails.value,
    poNumber = poNumber.value,
    taxLabel = taxLabel.value,
    taxRate = taxRate.value,
    discountRate = discountRate.value,
    payment = payment.value,
    notes = notes.value,
    template = template.value.name,
    accentIndex = ACCENTS.indexOf(accent.value).coerceAtLeast(0),
    status = if (recordId == 0L) "DRAFT" else "UNPAID",
    total = this.total,
    createdAt = if (createdAt == 0L) now else createdAt,
    updatedAt = now
)

fun InvoiceState.itemRecords(): List<InvoiceItemRecord> =
    items.mapIndexed { i, li ->
        InvoiceItemRecord(
            invoiceId = recordId,
            position = i,
            description = li.description.value,
            qty = li.qty.value,
            rate = li.rate.value
        )
    }

/**
 * Stored record back into editor state.
 *
 * Enum and index lookups are defensive on purpose: a row written by a later
 * version, or one hand-edited, should open with a sane default rather than
 * crash the screen the user keeps their invoices in.
 */
fun InvoiceState.loadFrom(data: InvoiceWithItems) {
    val r = data.invoice
    recordId = r.id
    seq = r.seq
    createdAt = r.createdAt
    docType.value = runCatching { DocType.valueOf(r.docType) }.getOrDefault(DocType.INVOICE)
    invoiceNumber.value = r.number
    issueDate.value = r.issueDate
    dueDate.value = r.dueDate
    validUntil.value = r.validUntil
    currency.value = r.currency
    bizName.value = r.bizName
    bizDetails.value = r.bizDetails
    bizTaxId.value = r.bizTaxId
    clientName.value = r.clientName
    clientDetails.value = r.clientDetails
    poNumber.value = r.poNumber
    taxLabel.value = r.taxLabel
    taxRate.value = r.taxRate
    discountRate.value = r.discountRate
    payment.value = r.payment
    notes.value = r.notes
    template.value = runCatching { Template.valueOf(r.template) }.getOrDefault(Template.MODERN)
    accent.value = ACCENTS.getOrElse(r.accentIndex) { ACCENTS[0] }
    items.clear()
    data.items.forEach { items.add(LineItem(it.description, it.qty, it.rate)) }
    if (items.isEmpty()) items.add(LineItem("Service or product", "1", "0"))
}

