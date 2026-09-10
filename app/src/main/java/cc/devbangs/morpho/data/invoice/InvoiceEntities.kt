package cc.devbangs.morpho.data.invoice

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A saved invoice, receipt or quote.
 *
 * The editor's InvoiceState holds Compose observables and is rebuilt on every
 * open; this is the flat record behind it. They are mapped rather than merged
 * so the storage shape can change without the editor caring, and so Room is
 * not handed mutable state it cannot reason about.
 *
 * [seq] is the numeric part of the document number, kept separately from the
 * formatted [number] so the next one can be derived with a MAX() rather than
 * by parsing strings back out.
 *
 * [status] is stored now although paid/unpaid tracking lands in a later stage:
 * adding a column later means a migration, and adding it now costs nothing.
 */
@Entity(tableName = "invoices")
data class InvoiceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val docType: String = "INVOICE",
    val seq: Int = 1,
    val number: String = "",
    val issueDate: String = "",
    val dueDate: String = "",
    val validUntil: String = "",
    val currency: String = "Le",

    val bizName: String = "",
    val bizDetails: String = "",
    val bizTaxId: String = "",

    val clientName: String = "",
    val clientDetails: String = "",
    val poNumber: String = "",

    val taxLabel: String = "GST",
    val taxRate: String = "0",
    val discountRate: String = "0",

    val payment: String = "",
    val notes: String = "",

    val template: String = "MODERN",
    val accentIndex: Int = 0,

    /** DRAFT, UNPAID or PAID. Only DRAFT and UNPAID are set in this stage. */
    val status: String = "DRAFT",

    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

/**
 * One billable line.
 *
 * A separate table rather than a JSON blob on the invoice, because the items
 * catalogue in a later stage needs to look across lines, and unpicking that
 * from serialised text afterwards is worse than modelling it now.
 *
 * CASCADE on delete: an orphaned line has no meaning, and leaving cleanup to
 * calling code is how orphans accumulate.
 */
@Entity(
    tableName = "invoice_items",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceRecord::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("invoiceId")]
)
data class InvoiceItemRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    /** Explicit, because row order in SQL is not a promise. */
    val position: Int,
    val description: String = "",
    val qty: String = "1",
    val rate: String = "0"
)
