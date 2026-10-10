package com.example.data

import com.example.model.PdfFileRecord
import kotlinx.coroutines.flow.Flow

class PdfRepository(private val pdfFileDao: PdfFileDao) {
    val allFiles: Flow<List<PdfFileRecord>> = pdfFileDao.getAllFiles()
    val recentFiles: Flow<List<PdfFileRecord>> = pdfFileDao.getRecentFiles()

    fun getFilesByCategory(category: String): Flow<List<PdfFileRecord>> {
        return pdfFileDao.getFilesByCategory(category)
    }

    fun searchFiles(query: String): Flow<List<PdfFileRecord>> {
        return pdfFileDao.searchFiles(query)
    }

    suspend fun getFileById(id: Long): PdfFileRecord? {
        return pdfFileDao.getFileById(id)
    }

    suspend fun insertFile(file: PdfFileRecord): Long {
        return pdfFileDao.insertFile(file)
    }

    suspend fun updateFile(file: PdfFileRecord) {
        pdfFileDao.updateFile(file)
    }

    suspend fun deleteFileById(id: Long) {
        pdfFileDao.deleteFileById(id)
    }

    suspend fun renameFile(id: Long, newName: String) {
        pdfFileDao.renameFile(id, newName)
    }

    suspend fun getFileCount(): Int {
        return pdfFileDao.getFileCount()
    }
}
