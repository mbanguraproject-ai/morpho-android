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

    /**
     * DRAFT, SENT, PARTIAL or PAID.
     *
     * Derived from the payments and the sent time rather than set by hand, and
     * written here so the list can show it without loading every payment row.
     */
    val status: String = "DRAFT",

    /** When it was marked as sent. 0 means it has not been. */
    val sentAt: Long = 0L,

    /** Total received against this document, stored for the same reason as [total]. */
    val paid: Double = 0.0,

    /** Whether a settled document prints the PAID mark. */
    val showPaidStamp: Boolean = true,

    /**
     * Files under filesDir, not blobs in the table.
     *
     * A logo is tens of kilobytes; a hundred invoices holding one each would
     * bloat the database and slow every query that touches this table, for
     * data no query ever looks inside. The path is copied onto the invoice
     * rather than pointing at the business, so replacing a logo next year
     * leaves last year's documents as they were sent.
     *
     * Empty means none.
     */
    val signaturePath: String = "",
    val logoPath: String = "",

    /**
     * The document total, stored rather than derived.
     *
     * The list has to show an amount per row. Deriving it would mean either a
     * query per row, or summing text columns in SQL and then applying the
     * discount and tax percentages - which are also text - in the same
     * statement. The arithmetic already exists in InvoiceState and is written
     * here at save time instead.
     */
    /** Flat amount added after tax. Text, like the other money fields. */
    val shipping: String = "0",

    val total: Double = 0.0,

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
    val rate: String = "0",
    /**
     * Per line, because one rate for the whole document cannot express a
     * zero-rated item next to a standard-rated one - which is ordinary on a
     * real invoice, and the reason the invoice-level rate is now only a
     * default for new lines.
     */
    val taxRate: String = "0"
)

/**
 * A business you invoice as.
 *
 * Nothing links an invoice to this row. The invoice keeps its own copy of the
 * name, address and tax id, and this only fills those fields. That is
 * deliberate: an invoice is a record of what was sent, and if the address is
 * referenced rather than copied, moving premises next year silently rewrites
 * every invoice already issued.
 */
@Entity(tableName = "businesses")
data class BusinessRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val details: String = "",
    val taxId: String = "",
    /**
     * Lives on the business, not the invoice, because it is picked once and
     * reused - the same argument as the address. The invoice still keeps its
     * own copy of the path, so replacing a logo leaves documents already sent
     * with the letterhead they were sent with.
     */
    val logoPath: String = "",
    val updatedAt: Long = 0L
)

/**
 * Someone you bill. Copied onto the invoice for the same reason as
 * [BusinessRecord] - a client changing address must not alter last year's
 * documents.
 */
@Entity(tableName = "clients")
data class ClientRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val details: String = "",
    val reference: String = "",
    val updatedAt: Long = 0L
)

/**
 * Something you bill for often.
 *
 * Named catalog_items rather than items so it cannot be confused with
 * invoice_items, which is a different thing entirely: these are the templates,
 * those are the lines on a document.
 *
 * Copied onto the line when picked, never referenced. Raising a price next
 * year must not change what an invoice from last year says you charged.
 */
@Entity(tableName = "catalog_items")
data class CatalogItemRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val rate: String = "0",
    val taxRate: String = "0",
    val updatedAt: Long = 0L
)

/**
 * Money received against an invoice.
 *
 * Rows rather than a single "amount paid" field, because part payments are
 * normal and a business needs to see when each one arrived, not just the
 * remaining balance. CASCADE for the same reason as the line items: a payment
 * against a deleted invoice means nothing.
 */
@Entity(
    tableName = "payments",
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
data class PaymentRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val position: Int,
    val amount: String = "0",
    val date: String = "",
    val note: String = ""
)
