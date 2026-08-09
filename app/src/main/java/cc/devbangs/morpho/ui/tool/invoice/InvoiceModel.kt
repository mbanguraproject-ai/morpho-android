package cc.devbangs.morpho.ui.tool.invoice

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
    // From (business)
    val bizName = mutableStateOf("")
    val bizDetails = mutableStateOf("")      // address / phone / email (multiline)
    val bizTaxId = mutableStateOf("")
    // Bill to
    val clientName = mutableStateOf("")
    val clientDetails = mutableStateOf("")
    val poNumber = mutableStateOf("")
    // Meta
    val invoiceNumber = mutableStateOf("INV-2026-001")
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
