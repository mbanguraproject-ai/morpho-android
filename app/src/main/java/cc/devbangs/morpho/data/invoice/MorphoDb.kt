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
        ClientRecord::class,
        CatalogItemRecord::class,
        PaymentRecord::class
    ],
    version = 8,
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
        /**
         * Per-line tax, and shipping.
         *
         * The UPDATE is the part that matters. Existing invoices carried one
         * rate for the whole document; copying it onto every line keeps their
         * totals identical to the PDFs already sent. Without it, reopening an
         * invoice from last month would show a different total than the
         * customer received, which is the worst thing an invoicing app can do.
         */
        /** Adds the logo to a saved business, so it is picked once. */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE businesses ADD COLUMN logoPath TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * Adds the signature and logo paths.
         *
         * Both columns land together although only the signature is wired in
         * this stage - a second migration to add one more empty text column
         * would be cost with no benefit.
         */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE invoices ADD COLUMN signaturePath TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE invoices ADD COLUMN logoPath TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * Adds payments, and the fields that follow from them.
         *
         * The foreign key and index wording is copied from what Room generated
         * for invoice_items rather than written from memory - a close but
         * inexact CREATE TABLE fails at open, and a missing index is a schema
         * mismatch just as much as a missing column.
         *
         * showPaidStamp defaults to 1 so existing invoices behave as they did.
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `payments` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`invoiceId` INTEGER NOT NULL, " +
                        "`position` INTEGER NOT NULL, " +
                        "`amount` TEXT NOT NULL, " +
                        "`date` TEXT NOT NULL, " +
                        "`note` TEXT NOT NULL, " +
                        "FOREIGN KEY(`invoiceId`) REFERENCES `invoices`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_payments_invoiceId` " +
                        "ON `payments` (`invoiceId`)"
                )
                db.execSQL("ALTER TABLE invoices ADD COLUMN sentAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE invoices ADD COLUMN paid REAL NOT NULL DEFAULT 0")
                db.execSQL(
                    "ALTER TABLE invoices ADD COLUMN showPaidStamp INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        /** Adds the saved items catalogue. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `catalog_items` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`rate` TEXT NOT NULL, " +
                        "`taxRate` TEXT NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE invoice_items ADD COLUMN taxRate TEXT NOT NULL DEFAULT '0'"
                )
                db.execSQL(
                    "UPDATE invoice_items SET taxRate = COALESCE(" +
                        "(SELECT taxRate FROM invoices WHERE invoices.id = invoice_items.invoiceId)," +
                        " '0')"
                )
                db.execSQL(
                    "ALTER TABLE invoices ADD COLUMN shipping TEXT NOT NULL DEFAULT '0'"
                )
            }
        }

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
            ).addMigrations(
                MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
                MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8
            ).build().also { instance = it }
        }
    }
}
