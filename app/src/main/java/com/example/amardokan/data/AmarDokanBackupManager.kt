package com.example.amardokan.data

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.work.WorkManager
import com.example.storage.StoragePermissionHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages completely offline, local storage backup & restore for Amar Dokan database tables.
 * Saves backups to: /storage/emulated/0/Smart King/Amar Dokan/AmarDokan_Backup_YYYYMMDD_HHMMSS.json
 */
object AmarDokanBackupManager {
    private const val TAG = "AmarDokanBackup"
    const val TARGET_FOLDER_PATH = "/storage/emulated/0/Smart King/Amar Dokan"
    private const val PREFS_NAME = "amar_dokan_local_backup_prefs"
    private const val KEY_LAST_BACKUP_TIME = "last_local_backup_time"
    private const val KEY_LAST_BACKUP_PATH = "last_local_backup_path"
    private const val KEY_LAST_STATUS = "last_local_status"

    private val _lastBackupTime = MutableStateFlow(0L)
    val lastBackupTime: StateFlow<Long> = _lastBackupTime.asStateFlow()

    private val _lastBackupPath = MutableStateFlow<String?>(null)
    val lastBackupPath: StateFlow<String?> = _lastBackupPath.asStateFlow()

    private val _isBackupRunning = MutableStateFlow(false)
    val isBackupRunning: StateFlow<Boolean> = _isBackupRunning.asStateFlow()

    private val _lastStatus = MutableStateFlow<String?>(null)
    val lastStatus: StateFlow<String?> = _lastStatus.asStateFlow()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(DatabaseBackup::class.java).indent("  ")

    fun init(context: Context) {
        val prefs = getPrefs(context)
        _lastBackupTime.value = prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)
        _lastBackupPath.value = prefs.getString(KEY_LAST_BACKUP_PATH, null)
        _lastStatus.value = prefs.getString(KEY_LAST_STATUS, null)

        // Cancel any legacy Google Drive cloud backup workers
        try {
            WorkManager.getInstance(context).cancelUniqueWork("amar_dokan_periodic_backup")
            WorkManager.getInstance(context).cancelUniqueWork("amar_dokan_manual_backup")
        } catch (_: Throwable) {}

        // Ensure directory is initialized
        try {
            getBackupDirectory()
        } catch (e: Exception) {
            Log.w(TAG, "Directory init check: ${e.message}")
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getBackupDirectory(): File {
        val dir = File(Environment.getExternalStorageDirectory(), "Smart King/Amar Dokan")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Lists existing backup files in /storage/emulated/0/Smart King/Amar Dokan
     * sorted newest first.
     */
    fun listLocalBackupFiles(): List<File> {
        return try {
            val dir = getBackupDirectory()
            val files = dir.listFiles { f ->
                f.isFile && (f.extension.equals("json", ignoreCase = true) || f.extension.equals("db", ignoreCase = true))
            } ?: emptyArray()
            files.sortedByDescending { it.lastModified() }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing backup files", e)
            emptyList()
        }
    }

    /**
     * Exports all Amar Dokan SQLite/Room database tables
     * (Customers, Bills, Ledger, Items, Settings/Services, Payments) into a timestamped JSON file
     * saved directly in /storage/emulated/0/Smart King/Amar Dokan/
     */
    suspend fun createLocalBackup(context: Context): Result<File> = withContext(Dispatchers.IO) {
        _isBackupRunning.value = true
        _lastStatus.value = "ব্যাকআপ প্রস্তুত করা হচ্ছে..."

        try {
            // Check storage permission if needed
            if (!StoragePermissionHelper.hasFullStoragePermission(context) && context is Activity) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "স্টোরেজ পারমিশন পরীক্ষা করা হচ্ছে...",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            val repository = AmarDokanRepository.getInstance(context)
            val backup = repository.exportBackup().copy(
                backupTimestamp = System.currentTimeMillis()
            )

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "AmarDokan_Backup_$timestamp.json"

            val dir = getBackupDirectory()
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val backupFile = File(dir, fileName)

            val jsonString = adapter.toJson(backup)
            backupFile.writeText(jsonString)

            // Trigger MediaScanner so the new backup file is immediately visible in Android file managers
            try {
                MediaScannerConnection.scanFile(
                    context.applicationContext,
                    arrayOf(backupFile.absolutePath),
                    arrayOf("application/json"),
                    null
                )
            } catch (e: Exception) {
                Log.w(TAG, "MediaScanner scan failed: ${e.message}")
            }

            val now = System.currentTimeMillis()
            getPrefs(context).edit()
                .putLong(KEY_LAST_BACKUP_TIME, now)
                .putString(KEY_LAST_BACKUP_PATH, backupFile.absolutePath)
                .putString(KEY_LAST_STATUS, "সফল: $fileName")
                .apply()

            _lastBackupTime.value = now
            _lastBackupPath.value = backupFile.absolutePath
            _lastStatus.value = "সফল: $fileName"
            _isBackupRunning.value = false

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "✅ ব্যাকআপ সংরক্ষিত হয়েছে:\n${backupFile.absolutePath}",
                    Toast.LENGTH_LONG
                ).show()
            }

            Result.success(backupFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create local backup", e)
            _isBackupRunning.value = false
            _lastStatus.value = "ব্যর্থ: ${e.message}"
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "❌ ব্যাকআপ সংরক্ষণে ব্যর্থ: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
            Result.failure(e)
        }
    }

    /**
     * Inspects a local backup file and returns the DatabaseBackup object for user confirmation
     */
    suspend fun inspectBackupFile(file: File): Result<DatabaseBackup> = withContext(Dispatchers.IO) {
        try {
            val json = file.readText()
            val backup = adapter.fromJson(json)
                ?: return@withContext Result.failure(IllegalStateException("ফাইলের ডাটা সঠিক নয় (Invalid JSON)"))
            Result.success(backup)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Inspects a backup file selected via Android OpenDocument / Storage Access Framework Uri
     */
    suspend fun inspectBackupUri(context: Context, uri: Uri): Result<Pair<String, DatabaseBackup>> = withContext(Dispatchers.IO) {
        try {
            var fileName = "Selected_Backup.json"
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            fileName = it.getString(nameIndex) ?: fileName
                        }
                    }
                }
            } catch (_: Exception) {}

            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().use { it.readText() }
            } ?: return@withContext Result.failure(IllegalStateException("ফাইল পড়তে পারা যায়নি"))

            val backup = adapter.fromJson(content)
                ?: return@withContext Result.failure(IllegalStateException("ফাইলের ফরম্যাট সঠিক নয়"))

            Result.success(Pair(fileName, backup))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Atomically restores database records from a DatabaseBackup object into Room
     */
    suspend fun restoreDatabase(context: Context, backup: DatabaseBackup): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val repository = AmarDokanRepository.getInstance(context)
            repository.restoreBackup(backup)

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "✅ রিস্টোর সম্পন্ন! (${backup.customers.size} গ্রাহক, ${backup.bills.size} বিল, ${backup.payments.size} পেমেন্ট)",
                    Toast.LENGTH_LONG
                ).show()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore backup", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "❌ রিস্টোর ব্যর্থ হয়েছে: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
            Result.failure(e)
        }
    }
}
