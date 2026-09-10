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
