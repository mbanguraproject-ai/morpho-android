package cc.devbangs.morpho.data.invoice

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * The single way in and out of invoice storage.
 *
 * A thin object rather than injected classes, matching how Stats, Workspace
 * and Prefs are already reached in this app - one pattern is worth more than
 * a better pattern used in one corner.
 */
object InvoiceRepo {

    fun observeAll(ctx: Context): Flow<List<InvoiceRecord>> =
        MorphoDb.get(ctx).invoices().observeAll()

    suspend fun load(ctx: Context, id: Long): InvoiceWithItems? =
        MorphoDb.get(ctx).invoices().load(id)

    suspend fun save(ctx: Context, record: InvoiceRecord, items: List<InvoiceItemRecord>): Long =
        MorphoDb.get(ctx).invoices().save(record, items)

    suspend fun delete(ctx: Context, id: Long) =
        MorphoDb.get(ctx).invoices().deleteById(id)

    fun observeBusinesses(ctx: Context): Flow<List<BusinessRecord>> =
        MorphoDb.get(ctx).invoices().observeBusinesses()

    suspend fun saveBusiness(ctx: Context, record: BusinessRecord): Long =
        MorphoDb.get(ctx).invoices().upsertBusiness(record.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteBusiness(ctx: Context, id: Long) =
        MorphoDb.get(ctx).invoices().deleteBusiness(id)

    fun observeClients(ctx: Context): Flow<List<ClientRecord>> =
        MorphoDb.get(ctx).invoices().observeClients()

    suspend fun saveClient(ctx: Context, record: ClientRecord): Long =
        MorphoDb.get(ctx).invoices().upsertClient(record.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteClient(ctx: Context, id: Long) =
        MorphoDb.get(ctx).invoices().deleteClient(id)

    fun observeCatalog(ctx: Context): Flow<List<CatalogItemRecord>> =
        MorphoDb.get(ctx).invoices().observeCatalog()

    suspend fun saveCatalogItem(ctx: Context, record: CatalogItemRecord): Long =
        MorphoDb.get(ctx).invoices()
            .upsertCatalogItem(record.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteCatalogItem(ctx: Context, id: Long) =
        MorphoDb.get(ctx).invoices().deleteCatalogItem(id)

    /**
     * The next document number for a type, as sequence and formatted string.
     *
     * Zero padded to five digits so a list sorts and reads the way people
     * expect - INV00009 then INV00010, not INV9 then INV10.
     */
    suspend fun nextNumber(ctx: Context, docType: String, prefix: String): Pair<Int, String> {
        val next = (MorphoDb.get(ctx).invoices().maxSeq(docType) ?: 0) + 1
        return next to (prefix + "%05d".format(next))
    }
}
