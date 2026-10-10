package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_birthdays")
data class UserBirthday(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val day: Int, // 1..31
    val month: Int, // 1..12
    val birthYear: Int? = null,
    val note: String? = null
)
