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
    entities = [
        InvoiceRecord::class,
        InvoiceItemRecord::class,
        BusinessRecord::class,
        ClientRecord::class
    ],
    version = 3,
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
        /**
         * Adds saved businesses and clients.
         *
         * The SQL has to match what Room generates for these entities exactly
         * - column order, types, the NOT NULL and AUTOINCREMENT wording. Room
         * hashes the schema and refuses to open a database that does not
         * match, which is a loud failure rather than a quiet corruption, and
         * the only real test is opening an existing database.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `businesses` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`details` TEXT NOT NULL, " +
                        "`taxId` TEXT NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `clients` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`details` TEXT NOT NULL, " +
                        "`reference` TEXT NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL)"
                )
            }
        }

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
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
        }
    }
}
