package com.example.model

import android.graphics.Bitmap
import java.util.UUID

data class ResumeData(
    val personalDetails: PersonalDetails = PersonalDetails(),
    val education: List<EducationItem> = listOf(EducationItem()),
    val experience: List<ExperienceItem> = listOf(ExperienceItem()),
    val skills: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val objective: String = "",
    val additionalInfo: String = "",
    val date: String = "",
    val place: String = "",
    val photo: Bitmap? = null,
    val signature: Bitmap? = null,
    val showEducation: Boolean = true,
    val educationColor: String = "#6366F1",
    val showExperience: Boolean = true,
    val experienceColor: String = "#F59E0B",
    val showSkills: Boolean = true,
    val showObjective: Boolean = true
)

data class PersonalDetails(
    val title: String = "Mr.",
    val fullName: String = "",
    val fatherName: String = "",
    val dob: String = "",
    val gender: String = "Select",
    val maritalStatus: String = "Select",
    val professionalTitle: String = "",
    val phone: String = "",
    val email: String = "",
    val houseNo: String = "",
    val landmark: String = "",
    val area: String = "",
    val stateCity: String = "West Bengal",
    val pincode: String = "",
    val nationality: String = "Indian"
)

data class EducationItem(
    val id: String = UUID.randomUUID().toString(),
    val degree: String = "",
    val school: String = "",
    val year: String = "",
    val result: String = ""
)

data class ExperienceItem(
    val id: String = UUID.randomUUID().toString(),
    val jobTitle: String = "",
    val company: String = "",
    val period: String = "",
    val details: String = ""
)

enum class ResumeTemplate(val displayName: String) {
    FORMAL_CURVE("Formal Curve"),
    PROFESSIONAL_SIDEBAR("Professional Sidebar"),
    ACCENT_EXECUTIVE("Accent Executive"),
    PROFILE_HEADER("Profile Header"),
    BIO_DATA_CLASSIC("Bio-Data Classic"),
    FRESH_PROFESSIONAL("Fresh Professional")
}
