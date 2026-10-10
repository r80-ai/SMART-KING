package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.UserBirthday
import kotlinx.coroutines.flow.Flow

@Dao
interface UserBirthdayDao {
    @Query("SELECT * FROM user_birthdays ORDER BY month ASC, day ASC")
    fun getAllBirthdays(): Flow<List<UserBirthday>>

    @Query("SELECT * FROM user_birthdays WHERE month = :month AND day = :day")
    suspend fun getBirthdaysForDate(month: Int, day: Int): List<UserBirthday>

    @Query("SELECT * FROM user_birthdays WHERE month = :month")
    suspend fun getBirthdaysForMonth(month: Int): List<UserBirthday>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBirthday(birthday: UserBirthday): Long

    @Delete
    suspend fun deleteBirthday(birthday: UserBirthday)
}
