package com.example.model

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_files")
data class PdfFileRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val filePath: String,
    val fileSize: Long,
    val pageCount: Int,
    val category: String, // "recent", "created", "converted", "scanned", "downloads"
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val isProtected: Boolean = false,
    val isFavorite: Boolean = false,
    val tag: String? = null,
    val uriString: String? = null
) {
    fun nameWithoutExtension(): String = name.removeSuffix(".pdf")

    /**
     * Resolves the actual document filename (e.g., "ANHARA.pdf") from the URI's DISPLAY_NAME
     * or by stripping internal timestamp prefixes (such as 1792324996557_).
     * Returns "Unknown PDF" as a safe fallback if no valid filename can be derived.
     */
    fun resolveActualFileName(context: Context? = null): String {
        // 1. Query DISPLAY_NAME from ContentResolver if uriString is available
        if (context != null && !uriString.isNullOrBlank()) {
            try {
                val parsedUri = Uri.parse(uriString)
                context.contentResolver.query(parsedUri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIdx != -1) {
                            val queried = cursor.getString(nameIdx)
                            if (!queried.isNullOrBlank()) {
                                val trimmed = queried.trim()
                                return if (!trimmed.endsWith(".pdf", ignoreCase = true)) "$trimmed.pdf" else trimmed
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. Clean internal timestamp prefix (e.g. "1792324996557_ANHARA.pdf" -> "ANHARA.pdf")
        val rawCandidate = name.ifBlank { filePath.substringAfterLast('/') }
        val stripped = rawCandidate.replace(Regex("^\\d{10,15}_"), "").trim()
        if (stripped.isNotBlank() && stripped != ".pdf") {
            return if (!stripped.endsWith(".pdf", ignoreCase = true)) "$stripped.pdf" else stripped
        }

        if (rawCandidate.isNotBlank() && rawCandidate != ".pdf") {
            return if (!rawCandidate.endsWith(".pdf", ignoreCase = true)) "$rawCandidate.pdf" else rawCandidate
        }

        return "Unknown PDF"
    }
}
