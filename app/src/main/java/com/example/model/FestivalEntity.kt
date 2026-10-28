package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Database entity for festivals, pujas, Islamic events, and national holidays.
 * Supports distinct category tags: "HINDU", "ISLAMIC", "NATIONAL".
 */
@Entity(tableName = "calendar_festivals")
data class FestivalEntity(
    @PrimaryKey
    val id: String,
    val titleBn: String,
    val titleEn: String,
    val year: Int,
    val month: Int,
    val day: Int,
    val type: String, // "HINDU" | "ISLAMIC" | "NATIONAL"
    val isHoliday: Boolean = true,
    val descriptionBn: String? = null,
    val descriptionEn: String? = null,
    val visualType: String? = null,
    val moonSightingAdjustable: Boolean = false
)
