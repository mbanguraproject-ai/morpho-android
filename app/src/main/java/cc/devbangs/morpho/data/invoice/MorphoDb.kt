package cc.devbangs.morpho.data.invoice

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The app's only database.
 *
 * Everything else in Morpho persists through SharedPreferences, and that stays
 * as it is - this exists because invoices are relational and the rest is not.
 *
 * Destructive migration is deliberately not enabled: this holds documents a
 * business has issued, and silently dropping them on a schema change would be
 * the worst kind of data loss. A schema change without a migration should fail
 * loudly in development instead.
 */
@Database(
    entities = [InvoiceRecord::class, InvoiceItemRecord::class],
    version = 1,
    exportSchema = true
)
abstract class MorphoDb : RoomDatabase() {
    abstract fun invoices(): InvoiceDao

    companion object {
        @Volatile
        private var instance: MorphoDb? = null

        fun get(ctx: Context): MorphoDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                ctx.applicationContext, MorphoDb::class.java, "morpho.db"
            ).build().also { instance = it }
        }
    }
}
