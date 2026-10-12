package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.PdfFileRecord
import com.example.model.UserBirthday
import com.example.amardokan.data.Customer
import com.example.amardokan.data.ServiceItem
import com.example.amardokan.data.Bill
import com.example.amardokan.data.BillItem
import com.example.amardokan.data.Payment
import com.example.amardokan.data.SyncMetadata
import com.example.amardokan.data.AmarDokanDao

@Database(
    entities = [
        PdfFileRecord::class,
        UserBirthday::class,
        Customer::class,
        ServiceItem::class,
        Bill::class,
        BillItem::class,
        Payment::class,
        SyncMetadata::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pdfFileDao(): PdfFileDao
    abstract fun userBirthdayDao(): UserBirthdayDao
    abstract fun amarDokanDao(): AmarDokanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE pdf_files ADD COLUMN uriString TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE ad_bills ADD COLUMN language TEXT NOT NULL DEFAULT 'BN'")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `ad_sync_metadata` (`id` TEXT NOT NULL, `entityType` TEXT NOT NULL, `entityId` INTEGER NOT NULL, `lastSyncedAt` INTEGER NOT NULL, `isPending` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pdf_master_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_4_5, MIGRATION_5_6)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
