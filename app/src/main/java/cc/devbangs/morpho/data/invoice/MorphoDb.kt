package cc.devbangs.morpho.data.invoice

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    version = 2,
    exportSchema = true
)
abstract class MorphoDb : RoomDatabase() {
    abstract fun invoices(): InvoiceDao

    companion object {

        /**
         * Adds the stored total.
         *
         * The first migration, and the reason destructive fallback is off. It
         * costs ten lines here; the alternative would have dropped every saved
         * invoice to add one column.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE invoices ADD COLUMN total REAL NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var instance: MorphoDb? = null

        fun get(ctx: Context): MorphoDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                ctx.applicationContext, MorphoDb::class.java, "morpho.db"
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}
