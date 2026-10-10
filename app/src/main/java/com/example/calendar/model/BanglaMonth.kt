package com.example.calendar.model

enum class BanglaMonth(
    val monthIndex: Int, // 1 to 12
    val nameBn: String,
    val nameEn: String,
    val standardDays: Int
) {
    BOISHAKH(1, "বৈশাখ", "Boishakh", 31),
    JYESHTHA(2, "জ্যৈষ্ঠ", "Jyeshtha", 31),
    ASHHADH(3, "আষাঢ়", "Ashhadh", 31),
    SRABON(4, "শ্রাবণ", "Srabon", 31),
    BHADRO(5, "ভাদ্র", "Bhadro", 31),
    ASHWIN(6, "আশ্বিন", "Ashwin", 30),
    KARTIK(7, "কার্তিক", "Kartik", 30),
    OGROHAYON(8, "অগ্রহায়ণ", "Ogrohayon", 30),
    POUSH(9, "পৌষ", "Poush", 30),
    MAGH(10, "মাঘ", "Magh", 30),
    FALGUN(11, "ফাল্গুন", "Falgun", 29),
    CHOITRO(12, "চৈত্র", "Choitro", 30);

    fun getDays(isLeapYear: Boolean): Int {
        return if (this == FALGUN && isLeapYear) 30 else standardDays
    }

    companion object {
        fun fromIndex(index: Int): BanglaMonth {
            val clamped = ((index - 1) % 12 + 12) % 12 + 1
            return entries.first { it.monthIndex == clamped }
        }
    }
}
