package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.PdfFileRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfFileDao {
    @Query("SELECT * FROM pdf_files ORDER BY lastModified DESC")
    fun getAllFiles(): Flow<List<PdfFileRecord>>

    @Query("SELECT * FROM pdf_files WHERE category = :category ORDER BY lastModified DESC")
    fun getFilesByCategory(category: String): Flow<List<PdfFileRecord>>

    @Query("SELECT * FROM pdf_files WHERE name LIKE '%' || :query || '%' ORDER BY lastModified DESC")
    fun searchFiles(query: String): Flow<List<PdfFileRecord>>

    @Query("SELECT * FROM pdf_files ORDER BY lastModified DESC LIMIT 10")
    fun getRecentFiles(): Flow<List<PdfFileRecord>>

    @Query("SELECT * FROM pdf_files WHERE id = :id LIMIT 1")
    suspend fun getFileById(id: Long): PdfFileRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: PdfFileRecord): Long

    @Update
    suspend fun updateFile(file: PdfFileRecord)

    @Query("DELETE FROM pdf_files WHERE id = :id")
    suspend fun deleteFileById(id: Long)

    @Query("UPDATE pdf_files SET name = :newName, lastModified = :time WHERE id = :id")
    suspend fun renameFile(id: Long, newName: String, time: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM pdf_files")
    suspend fun getFileCount(): Int
}
