package com.example.storage

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object GlobalSaveManager {
    const val MODE_ROOT_PUBLIC = "ROOT_PUBLIC"
    const val MODE_DOCUMENTS = "DOCUMENTS"
    const val MODE_DOWNLOADS = "DOWNLOADS"
    const val MODE_CUSTOM = "CUSTOM"
    const val MODE_INTERNAL = "INTERNAL"

    private const val TAG = "GlobalSaveManager"
    private const val PREFS_NAME = "storage_prefs"
    private const val KEY_STORAGE_MODE = "storage_mode"
    private const val KEY_CUSTOM_PATH = "custom_path"

    fun getActiveMode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_STORAGE_MODE, MODE_ROOT_PUBLIC) ?: MODE_ROOT_PUBLIC
    }

    fun setActiveMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_STORAGE_MODE, mode)
            .apply()
    }

    fun getCustomPath(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_PATH, null)
    }

    fun setCustomPath(context: Context, path: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_PATH, path)
            .apply()
    }

    fun getTargetDirectory(context: Context): File {
        val mode = getActiveMode(context)
        return when (mode) {
            MODE_ROOT_PUBLIC -> {
                val root = Environment.getExternalStorageDirectory()
                File(root, "PDF_Master").also { if (!it.exists()) it.mkdirs() }
            }
            MODE_DOCUMENTS -> {
                val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                File(docs, "PDF_Master").also { if (!it.exists()) it.mkdirs() }
            }
            MODE_DOWNLOADS -> {
                val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                File(downloads, "PDF_Master").also { if (!it.exists()) it.mkdirs() }
            }
            MODE_CUSTOM -> {
                val path = getCustomPath(context)
                if (path != null) File(path).also { if (!it.exists()) it.mkdirs() }
                else context.getExternalFilesDir(null)!!
            }
            else -> context.getExternalFilesDir(null)!!
        }
    }

    data class SaveResult(
        val success: Boolean,
        val uri: String? = null,
        val filePath: String? = null,
        val displayName: String? = null,
        val error: String? = null
    )

    fun saveFile(context: Context, fileName: String, inputStream: InputStream): SaveResult {
        return try {
            val targetDir = getTargetDirectory(context)
            val file = File(targetDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            SaveResult(
                success = true,
                filePath = file.absolutePath,
                displayName = fileName
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error saving file: ${e.message}")
            SaveResult(success = false, error = e.message)
        }
    }

    fun saveFile(context: Context, fileName: String, byteArray: ByteArray): SaveResult {
        return try {
            val targetDir = getTargetDirectory(context)
            val file = File(targetDir, fileName)
            FileOutputStream(file).use { it.write(byteArray) }
            SaveResult(
                success = true,
                filePath = file.absolutePath,
                displayName = fileName
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error saving file: ${e.message}")
            SaveResult(success = false, error = e.message)
        }
    }
}
