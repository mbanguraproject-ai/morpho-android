package cc.devbangs.morpho.data.invoice

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** An invoice with its lines, in order. */
data class InvoiceWithItems(
    val invoice: InvoiceRecord,
    val items: List<InvoiceItemRecord>,
    val payments: List<PaymentRecord> = emptyList()
)

@Dao
interface InvoiceDao {

    /** Newest first: the list is a work queue, not an archive. */
    @Query("SELECT * FROM invoices ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<InvoiceRecord>>

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun find(id: Long): InvoiceRecord?

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :id ORDER BY position ASC")
    suspend fun itemsOf(id: Long): List<InvoiceItemRecord>

    @Query("SELECT * FROM payments WHERE invoiceId = :id ORDER BY position ASC")
    suspend fun paymentsOf(id: Long): List<PaymentRecord>

    @Insert
    suspend fun insertPayments(rows: List<PaymentRecord>)

    @Query("DELETE FROM payments WHERE invoiceId = :id")
    suspend fun clearPayments(id: Long)

    /**
     * Highest sequence used for a document type, or null when none exists.
     *
     * Per type, so invoices, receipts and quotes each run their own numbering
     * instead of sharing one counter and skipping numbers.
     */
    @Query("SELECT MAX(seq) FROM invoices WHERE docType = :docType")
    suspend fun maxSeq(docType: String): Int?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: InvoiceRecord): Long

    @Update
    suspend fun update(record: InvoiceRecord)

    @Delete
    suspend fun delete(record: InvoiceRecord)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Insert
    suspend fun insertItems(items: List<InvoiceItemRecord>)

    @Query("DELETE FROM invoice_items WHERE invoiceId = :id")
    suspend fun clearItems(id: Long)

    @Query("SELECT * FROM businesses ORDER BY updatedAt DESC")
    fun observeBusinesses(): Flow<List<BusinessRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBusiness(record: BusinessRecord): Long

    @Query("DELETE FROM businesses WHERE id = :id")
    suspend fun deleteBusiness(id: Long)

    @Query("SELECT * FROM clients ORDER BY updatedAt DESC")
    fun observeClients(): Flow<List<ClientRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClient(record: ClientRecord): Long

    @Query("DELETE FROM clients WHERE id = :id")
    suspend fun deleteClient(id: Long)

    @Query("SELECT * FROM catalog_items ORDER BY updatedAt DESC")
    fun observeCatalog(): Flow<List<CatalogItemRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCatalogItem(record: CatalogItemRecord): Long

    @Query("DELETE FROM catalog_items WHERE id = :id")
    suspend fun deleteCatalogItem(id: Long)

    @Transaction
    suspend fun load(id: Long): InvoiceWithItems? {
        val inv = find(id) ?: return null
        return InvoiceWithItems(inv, itemsOf(id), paymentsOf(id))
    }

    /**
     * Write an invoice and its lines as one unit.
     *
     * Lines are replaced wholesale rather than diffed: an edit can reorder,
     * delete and add in the same pass, and a diff that gets it wrong leaves a
     * document that does not add up. Correctness beats the saved writes at
     * this size.
     */
    @Transaction
    suspend fun save(
        record: InvoiceRecord,
        items: List<InvoiceItemRecord>,
        payments: List<PaymentRecord>
    ): Long {
        val id = if (record.id == 0L) insert(record) else {
            update(record)
            record.id
        }
        clearItems(id)
        if (items.isNotEmpty()) {
            insertItems(items.mapIndexed { i, it -> it.copy(id = 0, invoiceId = id, position = i) })
        }
        clearPayments(id)
        if (payments.isNotEmpty()) {
            insertPayments(payments.mapIndexed { i, it -> it.copy(id = 0, invoiceId = id, position = i) })
        }
        return id
    }
}
