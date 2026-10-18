package com.example.model

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
}
