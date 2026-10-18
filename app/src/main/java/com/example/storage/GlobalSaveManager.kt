package com.example.storage

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.example.model.PdfFileRecord
import com.example.pdf.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class SaveResult(
    val fileRecord: PdfFileRecord,
    val displayLocation: String,
    val savedFile: File?,
    val savedUri: Uri,
    val fileName: String,
    val fileSize: Long,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)

object GlobalSaveManager {
    private const val PREFS_NAME = "pdf_master_storage_prefs"
    private const val KEY_MODE = "storage_mode"
    private const val KEY_CUSTOM_NAME = "custom_folder_name"
    private const val KEY_TREE_URI = "custom_tree_uri"
    private const val TAG = "GlobalSaveManager"

    // Storage Modes: "DOCUMENTS", "DOWNLOADS", "CUSTOM", "INTERNAL"
    const val MODE_DOCUMENTS = "DOCUMENTS"
    const val MODE_DOWNLOADS = "DOWNLOADS"
    const val MODE_CUSTOM = "CUSTOM"
    const val MODE_INTERNAL = "INTERNAL"

    private val _storageMode = MutableStateFlow(MODE_DOCUMENTS)
    val storageMode: StateFlow<String> = _storageMode.asStateFlow()

    private val _customFolderName = MutableStateFlow("PDF_Master")
    val customFolderName: StateFlow<String> = _customFolderName.asStateFlow()

    private val _customTreeUri = MutableStateFlow<String?>(null)
    val customTreeUri: StateFlow<String?> = _customTreeUri.asStateFlow()

    private val _displayPath = MutableStateFlow("Storage / PDF_Master")
    val displayPath: StateFlow<String> = _displayPath.asStateFlow()

    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val mode = prefs.getString(KEY_MODE, MODE_DOCUMENTS) ?: MODE_DOCUMENTS
        val folder = prefs.getString(KEY_CUSTOM_NAME, "PDF_Master") ?: "PDF_Master"
        val treeUriStr = prefs.getString(KEY_TREE_URI, null)

        _storageMode.value = mode
        _customFolderName.value = folder
        _customTreeUri.value = treeUriStr
        _displayPath.value = calculateDisplayPath(context, mode, folder, treeUriStr)
        isInitialized = true
    }

    fun setStorageLocation(
        context: Context,
        mode: String,
        folderName: String,
        treeUri: Uri? = null
    ) {
        val cleanFolder = if (folderName.isBlank()) "PDF_Master" else folderName.trim()
        val treeUriStr = treeUri?.toString() ?: if (mode == MODE_CUSTOM) _customTreeUri.value else null

        // If a new treeUri is provided, ensure persistable permission
        if (treeUri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to take persistable URI permission: ${e.message}")
            }
        }

        _storageMode.value = mode
        _customFolderName.value = cleanFolder
        _customTreeUri.value = treeUriStr
        _displayPath.value = calculateDisplayPath(context, mode, cleanFolder, treeUriStr)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_MODE, mode)
            putString(KEY_CUSTOM_NAME, cleanFolder)
            if (treeUriStr != null) {
                putString(KEY_TREE_URI, treeUriStr)
            } else if (mode != MODE_CUSTOM) {
                remove(KEY_TREE_URI)
            }
            apply()
        }

        PdfEngine.setFolderConfig(mode, cleanFolder)
    }

    fun isCurrentLocationAvailable(context: Context): Boolean {
        initialize(context)
        return when (_storageMode.value) {
            MODE_CUSTOM -> {
                val uriStr = _customTreeUri.value ?: return false
                try {
                    val uri = Uri.parse(uriStr)
                    val doc = DocumentFile.fromTreeUri(context, uri)
                    doc != null && doc.exists() && doc.canWrite()
                } catch (e: Exception) {
                    false
                }
            }
            MODE_DOCUMENTS, MODE_DOWNLOADS, MODE_INTERNAL -> true
            else -> true
        }
    }

    fun calculateDisplayPath(
        context: Context,
        mode: String,
        folderName: String,
        treeUriStr: String?
    ): String {
        return when (mode) {
            MODE_CUSTOM -> {
                if (treeUriStr.isNullOrBlank()) {
                    "Custom Folder (Not Selected)"
                } else {
                    try {
                        val treeUri = Uri.parse(treeUriStr)
                        val docId = DocumentsContract.getTreeDocumentId(treeUri)
                        if (docId.startsWith("primary:", ignoreCase = true)) {
                            val rel = docId.substringAfter("primary:")
                            if (rel.isBlank()) "Internal Storage" else rel.replace("/", " / ")
                        } else if (docId.contains(":")) {
                            val parts = docId.split(":", limit = 2)
                            val name = if (parts[0].matches(Regex("[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}"))) "SD Card" else parts[0]
                            val sub = parts.getOrNull(1)?.replace("/", " / ") ?: ""
                            if (sub.isNotBlank()) "$name / $sub" else name
                        } else {
                            val doc = DocumentFile.fromTreeUri(context, treeUri)
                            doc?.name ?: docId.replace("/", " / ")
                        }
                    } catch (e: Exception) {
                        val doc = DocumentFile.fromTreeUri(context, Uri.parse(treeUriStr))
                        doc?.name ?: "Custom Folder"
                    }
                }
            }
            MODE_DOWNLOADS -> "Downloads / $folderName"
            MODE_INTERNAL -> "App Private Storage / $folderName"
            else -> "Documents / $folderName"
        }
    }

    /**
     * Resolves the primary output directory for file-based operations.
     */
    fun getLocalActiveDirectory(context: Context): File {
        initialize(context)
        val folder = _customFolderName.value.ifBlank { "Smart_King" }
        val dir = when (_storageMode.value) {
            MODE_INTERNAL -> File(context.filesDir, folder)
            MODE_CUSTOM -> {
                // For custom SAF, we also maintain a local mirror/cache directory for fast local rendering
                val cache = context.getExternalFilesDir("custom_folder_mirror") ?: context.filesDir
                File(cache, folder)
            }
            MODE_DOWNLOADS -> {
                val appExtDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: context.getExternalFilesDir(null)
                    ?: context.filesDir
                File(appExtDir, folder)
            }
            MODE_DOCUMENTS -> {
                val appExtDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                    ?: context.getExternalFilesDir(null)
                    ?: context.filesDir
                File(appExtDir, folder)
            }
            else -> {
                val appExtDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                    ?: context.getExternalFilesDir(null)
                    ?: context.filesDir
                File(appExtDir, folder)
            }
        }
        if (!dir.exists()) {
            try { dir.mkdirs() } catch (_: Exception) {}
        }
        return if (dir.exists() && dir.canWrite()) dir else File(context.filesDir, folder).apply { if (!exists()) mkdirs() }
    }

    /**
     * Generate non-conflicting filename:
     * Never overwrites original!
     * filename.pdf -> filename_(1).pdf -> filename_(2).pdf
     */
    fun generateUniqueName(
        baseName: String,
        extension: String,
        existsCheck: (String) -> Boolean
    ): String {
        val cleanExt = extension.removePrefix(".")
        var cleanBase = baseName.trim()
        if (cleanBase.endsWith(".$cleanExt", ignoreCase = true)) {
            cleanBase = cleanBase.substring(0, cleanBase.length - (cleanExt.length + 1))
        }
        // Clean characters for file safety
        cleanBase = cleanBase.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
        if (cleanBase.isBlank()) cleanBase = "Document"

        var candidate = "$cleanBase.$cleanExt"
        var counter = 1
        while (existsCheck(candidate)) {
            candidate = "${cleanBase}_($counter).$cleanExt"
            counter++
        }
        return candidate
    }

    /**
     * Unified save method called by EVERY PDF and Photo tool!
     */
    suspend fun saveProcessedFile(
        context: Context,
        tempSourceFile: File,
        desiredFileName: String,
        mimeType: String = "application/pdf",
        category: String = "created",
        tag: String? = null
    ): SaveResult = withContext(Dispatchers.IO) {
        initialize(context)

        val ext = when {
            mimeType.equals("application/pdf", ignoreCase = true) || desiredFileName.endsWith(".pdf", ignoreCase = true) -> "pdf"
            mimeType.equals("image/jpeg", ignoreCase = true) || desiredFileName.endsWith(".jpg", ignoreCase = true) || desiredFileName.endsWith(".jpeg", ignoreCase = true) -> "jpg"
            mimeType.equals("image/png", ignoreCase = true) || desiredFileName.endsWith(".png", ignoreCase = true) -> "png"
            mimeType.equals("image/webp", ignoreCase = true) || desiredFileName.endsWith(".webp", ignoreCase = true) -> "webp"
            else -> desiredFileName.substringAfterLast(".", "pdf")
        }

        val baseName = desiredFileName.substringBeforeLast(".")
        val currentMode = _storageMode.value

        if (currentMode == MODE_CUSTOM) {
            val treeUriStr = _customTreeUri.value
            if (treeUriStr.isNullOrBlank()) {
                throw IllegalStateException("Custom folder location is not configured. Please select a folder in Settings.")
            }
            val treeUri = Uri.parse(treeUriStr)
            val treeDoc = DocumentFile.fromTreeUri(context, treeUri)
            if (treeDoc == null || !treeDoc.exists() || !treeDoc.canWrite()) {
                throw IllegalStateException("Save location is no longer available. Please choose a new folder.")
            }

            // Generate unique filename within the SAF directory
            val finalFileName = generateUniqueName(baseName, ext) { testName ->
                treeDoc.findFile(testName) != null
            }

            val newDocFile = treeDoc.createFile(mimeType, finalFileName)
                ?: throw IllegalStateException("Could not create output file in custom folder.")

            // Write bytes into the SAF document
            context.contentResolver.openOutputStream(newDocFile.uri)?.use { out ->
                FileInputStream(tempSourceFile).use { inStream ->
                    inStream.copyTo(out)
                }
            } ?: throw IllegalStateException("Failed to open output stream to custom storage.")

            // Also keep local cached file for thumbnail generation & page counting
            val localDir = getLocalActiveDirectory(context)
            val localCopy = File(localDir, finalFileName)
            try {
                tempSourceFile.copyTo(localCopy, overwrite = true)
            } catch (_: Exception) {}

            val pageCount = if (ext == "pdf") PdfEngine.getPageCount(localCopy) else 1
            val fileSize = tempSourceFile.length()

            val record = PdfFileRecord(
                name = finalFileName,
                filePath = localCopy.absolutePath,
                fileSize = fileSize,
                pageCount = pageCount,
                category = category,
                tag = tag ?: "Custom Folder",
                uriString = newDocFile.uri.toString()
            )

            SaveResult(
                fileRecord = record,
                displayLocation = _displayPath.value,
                savedFile = localCopy,
                savedUri = newDocFile.uri,
                fileName = finalFileName,
                fileSize = fileSize
            )
        } else {
            // File-based directory (DOCUMENTS, DOWNLOADS, INTERNAL)
            val targetDir = getLocalActiveDirectory(context)
            val destFile = if (tempSourceFile.parentFile?.canonicalPath == targetDir.canonicalPath) {
                tempSourceFile
            } else {
                val finalFileName = generateUniqueName(baseName, ext) { testName ->
                    File(targetDir, testName).exists()
                }
                val dest = File(targetDir, finalFileName)
                try {
                    tempSourceFile.copyTo(dest, overwrite = false)
                    dest
                } catch (e: Exception) {
                    val internalDir = File(context.filesDir, "PDF_Master").apply { if (!exists()) mkdirs() }
                    val fallbackDest = File(internalDir, finalFileName)
                    tempSourceFile.copyTo(fallbackDest, overwrite = false)
                    fallbackDest
                }
            }
            val finalFileName = destFile.name

            // Scan with media scanner so it's indexed in Android files app
            try {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(destFile.absolutePath),
                    arrayOf(mimeType),
                    null
                )
            } catch (e: Exception) {
                Log.w(TAG, "MediaScanner error: ${e.message}")
            }

            val contentUri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destFile)
            } catch (e: Exception) {
                Uri.fromFile(destFile)
            }

            val pageCount = if (ext == "pdf") PdfEngine.getPageCount(destFile) else 1
            val fileSize = destFile.length()

            val record = PdfFileRecord(
                name = finalFileName,
                filePath = destFile.absolutePath,
                fileSize = fileSize,
                pageCount = pageCount,
                category = category,
                tag = tag
            )

            SaveResult(
                fileRecord = record,
                displayLocation = _displayPath.value,
                savedFile = destFile,
                savedUri = contentUri,
                fileName = finalFileName,
                fileSize = fileSize
            )
        }
    }

    /**
     * Unified method for saving in-memory bytes (e.g. converted docs, scans, images)
     */
    suspend fun saveProcessedBytes(
        context: Context,
        bytes: ByteArray,
        desiredFileName: String,
        mimeType: String = "application/pdf",
        category: String = "created",
        tag: String? = null
    ): SaveResult = withContext(Dispatchers.IO) {
        val temp = File(context.cacheDir, "temp_save_${System.currentTimeMillis()}_${desiredFileName}")
        temp.outputStream().use { it.write(bytes) }
        try {
            saveProcessedFile(context, temp, desiredFileName, mimeType, category, tag)
        } finally {
            temp.delete()
        }
    }

    /**
     * Requirement 9: Open the newly created file using the appropriate Android viewer
     */
    fun openFile(context: Context, uri: Uri?, file: File?, mimeType: String = "application/pdf") {
        try {
            val targetUri = if (uri != null && uri.scheme == "content" && !uri.toString().contains(context.packageName)) {
                uri
            } else if (file != null && file.exists()) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                uri
            } ?: run {
                Toast.makeText(context, "File location is not accessible", Toast.LENGTH_SHORT).show()
                return
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(targetUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Open file")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "No app available to open this file (${e.message})", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Requirement 8: Open the REAL Android File Manager/system file browser at the folder
     */
    fun openFolder(
        context: Context,
        treeUriStr: String? = null,
        targetFile: File? = null
    ) {
        var opened = false
        val uriStr = treeUriStr ?: _customTreeUri.value

        // Attempt 1: If SAF treeUri is present, buildDocumentUriUsingTree and open with system Files app
        if (!uriStr.isNullOrBlank()) {
            try {
                val treeUri = Uri.parse(uriStr)
                val docId = DocumentsContract.getTreeDocumentId(treeUri)
                val folderUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(folderUri, DocumentsContract.Document.MIME_TYPE_DIR)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                opened = true
            } catch (_: Exception) {}
        }

        // Attempt 2: If DOWNLOADS mode, launch standard DownloadManager viewer
        if (!opened && _storageMode.value == MODE_DOWNLOADS) {
            try {
                val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                opened = true
            } catch (_: Exception) {}
        }

        // Attempt 3: View parent folder via FileProvider directory
        if (!opened && targetFile != null && targetFile.parentFile != null) {
            val parent = targetFile.parentFile!!
            try {
                val dirUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", parent)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(dirUri, DocumentsContract.Document.MIME_TYPE_DIR)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                opened = true
            } catch (_: Exception) {}
        }

        // Attempt 4: Launch system storage / files browser
        if (!opened) {
            try {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                opened = true
            } catch (e: Exception) {
                Toast.makeText(context, "Saved location: ${_displayPath.value}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
