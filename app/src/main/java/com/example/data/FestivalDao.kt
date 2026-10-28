package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.FestivalEntity

@Dao
interface FestivalDao {
    @Query("SELECT * FROM calendar_festivals WHERE year = :year AND month = :month AND day = :day")
    suspend fun getFestivalsForDate(year: Int, month: Int, day: Int): List<FestivalEntity>

    @Query("SELECT * FROM calendar_festivals WHERE year = :year AND month = :month")
    suspend fun getFestivalsForMonth(year: Int, month: Int): List<FestivalEntity>

    @Query("SELECT * FROM calendar_festivals WHERE type = :type")
    suspend fun getFestivalsByType(type: String): List<FestivalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFestivals(festivals: List<FestivalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFestival(festival: FestivalEntity)
}
