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
import com.example.model.FestivalEntity
import com.example.data.FestivalDao
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
        SyncMetadata::class,
        FestivalEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pdfFileDao(): PdfFileDao
    abstract fun userBirthdayDao(): UserBirthdayDao
    abstract fun amarDokanDao(): AmarDokanDao
    abstract fun festivalDao(): FestivalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private val userInstances = java.util.concurrent.ConcurrentHashMap<String, AppDatabase>()

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

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `calendar_festivals` (`id` TEXT NOT NULL, `titleBn` TEXT NOT NULL, `titleEn` TEXT NOT NULL, `year` INTEGER NOT NULL, `month` INTEGER NOT NULL, `day` INTEGER NOT NULL, `type` TEXT NOT NULL, `isHoliday` INTEGER NOT NULL, `descriptionBn` TEXT, `descriptionEn` TEXT, `visualType` TEXT, `moonSightingAdjustable` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        fun getDatabase(
            context: Context,
            userKey: String = com.example.util.UserManager.getCurrentUserKey(context)
        ): AppDatabase {
            return userInstances.computeIfAbsent(userKey) { key ->
                val appContext = context.applicationContext
                val dbName = com.example.util.UserManager.getUserDatabaseName(key)

                // If this is a specific user database and doesn't exist yet, check if legacy data should be migrated
                if (key.startsWith("user_")) {
                    maybeMigrateLegacyDatabase(appContext, key, dbName)
                }

                Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    dbName
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .fallbackToDestructiveMigration()
                .build()
            }
        }

        private fun maybeMigrateLegacyDatabase(context: Context, userKey: String, targetDbName: String) {
            try {
                val targetFile = context.getDatabasePath(targetDbName)
                if (targetFile.exists()) return

                val legacyFile = context.getDatabasePath("pdf_master_database")
                if (!legacyFile.exists()) return

                val legacyPrefs = context.getSharedPreferences("legacy_db_migration_prefs", Context.MODE_PRIVATE)
                val alreadyMigrated = legacyPrefs.getString("migrated_to_user", null)
                // Only copy legacy database to the FIRST user who signs in on this device
                if (alreadyMigrated == null) {
                    legacyPrefs.edit().putString("migrated_to_user", userKey).apply()
                    legacyFile.copyTo(targetFile, overwrite = false)
                    val legacyWal = java.io.File(legacyFile.parentFile, "pdf_master_database-wal")
                    val legacyShm = java.io.File(legacyFile.parentFile, "pdf_master_database-shm")
                    if (legacyWal.exists()) {
                        legacyWal.copyTo(java.io.File(targetFile.parentFile, "$targetDbName-wal"), overwrite = false)
                    }
                    if (legacyShm.exists()) {
                        legacyShm.copyTo(java.io.File(targetFile.parentFile, "$targetDbName-shm"), overwrite = false)
                    }
                    android.util.Log.i("AppDatabase", "Migrated legacy database to $targetDbName for $userKey")
                }
            } catch (e: Exception) {
                android.util.Log.e("AppDatabase", "Error migrating legacy database to $targetDbName", e)
            }
        }
    }
}
