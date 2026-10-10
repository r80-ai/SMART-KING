package com.example.hisab.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hisab.AgeResult
import com.example.hisab.HisabCalculationEngine
import com.example.hisab.HisabHistoryManager
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ToolBlue
import com.example.ui.theme.ToolGreen
import com.example.ui.theme.ToolOrange
import com.example.ui.theme.ToolPurple
import java.util.Calendar
import java.util.Locale

enum class AgeCalcMode(val titleBn: String, val subtitleBn: String) {
    CURRENT_DATE("মোড ১: বর্তমান তারিখ পর্যন্ত", "জন্মতারিখ → আজকের তারিখ"),
    CUSTOM_DATE("মোড ২: নিজের পছন্দের তারিখ", "জন্মতারিখ → হিসাবের তারিখ")
}

@Composable
fun HisabAgeSection(
    onSavedToHistory: (String) -> Unit
) {
    // Mode selection: Mode 1 (Today) vs Mode 2 (Custom Target Date)
    var selectedMode by remember { mutableStateOf(AgeCalcMode.CURRENT_DATE) }

    // DYNAMICALLY DETECT CURRENT DEVICE DATE (NEVER HARDCODED)
    val currentCal = remember { Calendar.getInstance() }
    val currentDeviceDay = currentCal.get(Calendar.DAY_OF_MONTH)
    val currentDeviceMonth = currentCal.get(Calendar.MONTH) + 1
    val currentDeviceYear = currentCal.get(Calendar.YEAR)
    val currentDeviceDateFormatted = String.format(Locale.US, "%02d/%02d/%04d", currentDeviceDay, currentDeviceMonth, currentDeviceYear)

    // Mode 1 & 2 inputs: Birth Date (defaulting to test case 22/03/1984)
    var birthDayStr by remember { mutableStateOf("22") }
    var birthMonthStr by remember { mutableStateOf("03") }
    var birthYearStr by remember { mutableStateOf("1984") }

    // Mode 2 input: Custom Target Date (defaulting to test case 14/09/2026)
    var customTargetDayStr by remember { mutableStateOf("14") }
    var customTargetMonthStr by remember { mutableStateOf("09") }
    var customTargetYearStr by remember { mutableStateOf("2026") }

    // Result and validation state
    var calculatedResult by remember { mutableStateOf<AgeResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasCalculatedOnce by remember { mutableStateOf(false) }

    // Calculation function with full Bengali validation
    fun performCalculation() {
        hasCalculatedOnce = true
        errorMessage = null

        val bDay = birthDayStr.toIntOrNull()
        val bMonth = birthMonthStr.toIntOrNull()
        val bYear = birthYearStr.toIntOrNull()

        if (bDay == null || bMonth == null || bYear == null) {
            errorMessage = "জন্মতারিখের দিন, মাস ও বছর সঠিকভাবে পূরণ করুন"
            calculatedResult = null
            return
        }

        // Validate birth date validity (e.g. 31/02/2024, 32/01/2020)
        val birthDateErr = HisabCalculationEngine.validateDate(bDay, bMonth, bYear)
        if (birthDateErr != null) {
            errorMessage = "জন্মতারিখ ভুল: $birthDateErr"
            calculatedResult = null
            return
        }

        val (tDay, tMonth, tYear) = if (selectedMode == AgeCalcMode.CURRENT_DATE) {
            Triple(currentDeviceDay, currentDeviceMonth, currentDeviceYear)
        } else {
            val cDay = customTargetDayStr.toIntOrNull()
            val cMonth = customTargetMonthStr.toIntOrNull()
            val cYear = customTargetYearStr.toIntOrNull()
            if (cDay == null || cMonth == null || cYear == null) {
                errorMessage = "হিসাবের তারিখের দিন, মাস ও বছর সঠিকভাবে পূরণ করুন"
                calculatedResult = null
                return
            }
            val targetDateErr = HisabCalculationEngine.validateDate(cDay, cMonth, cYear)
            if (targetDateErr != null) {
                errorMessage = "হিসাবের তারিখ ভুল: $targetDateErr"
                calculatedResult = null
                return
            }
            Triple(cDay, cMonth, cYear)
        }

        // Check if birth date is after target date
        if (bYear > tYear || (bYear == tYear && bMonth > tMonth) || (bYear == tYear && bMonth == tMonth && bDay > tDay)) {
            errorMessage = "জন্মতারিখ হিসাবের তারিখের চেয়ে ভবিষ্যৎ হতে পারে না"
            calculatedResult = null
            return
        }

        val res = HisabCalculationEngine.calculateAge(
            birthDay = bDay,
            birthMonth = bMonth,
            birthYear = bYear,
            targetDay = tDay,
            targetMonth = tMonth,
            targetYear = tYear
        )

        if (res != null) {
            calculatedResult = res
            errorMessage = null
        } else {
            errorMessage = "তারিখ গণনায় ত্রুটি হয়েছে, অনুগ্রহ করে সঠিক তারিখ দিন"
            calculatedResult = null
        }
    }

    // Auto-calculate on initial launch with default test case
    LaunchedEffect(Unit) {
        performCalculation()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // MODE SELECTOR TABS (MODE 1 vs MODE 2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AgeCalcMode.values().forEach { mode ->
                val isSelected = selectedMode == mode
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedMode = mode
                        performCalculation()
                    },
                    label = {
                        Text(
                            text = if (mode == AgeCalcMode.CURRENT_DATE) "মোড ১: বর্তমান তারিখ" else "মোড ২: পছন্দের তারিখ",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ToolBlue.copy(alpha = 0.2f),
                        selectedLabelColor = ToolBlue
                    )
                )
            }
        }

        // BIRTH DATE INPUT CARD
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cake, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "জন্মতারিখ [ DD/MM/YYYY ]",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "ম্যানুয়ালি টাইপ করুন: দিন, মাস ও বছর",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            birthDayStr = ""
                            birthMonthStr = ""
                            birthYearStr = ""
                            calculatedResult = null
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(18.dp))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = birthDayStr,
                        onValueChange = { if (it.length <= 2) birthDayStr = it },
                        label = { Text("দিন (DD)") },
                        placeholder = { Text("22") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("input_birth_day")
                    )

                    OutlinedTextField(
                        value = birthMonthStr,
                        onValueChange = { if (it.length <= 2) birthMonthStr = it },
                        label = { Text("মাস (MM)") },
                        placeholder = { Text("03") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("input_birth_month")
                    )

                    OutlinedTextField(
                        value = birthYearStr,
                        onValueChange = { if (it.length <= 4) birthYearStr = it },
                        label = { Text("বছর (YYYY)") },
                        placeholder = { Text("1984") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.4f).testTag("input_birth_year")
                    )
                }

                // TEST CASE QUICK FILL
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            birthDayStr = "22"
                            birthMonthStr = "03"
                            birthYearStr = "1984"
                            selectedMode = AgeCalcMode.CUSTOM_DATE
                            customTargetDayStr = "14"
                            customTargetMonthStr = "09"
                            customTargetYearStr = "2026"
                            performCalculation()
                        }
                    ) {
                        Text("টেস্ট কেস (22/03/1984 → 14/09/2026)", fontSize = 11.sp, color = ToolBlue)
                    }
                }
            }
        }

        // TARGET DATE CARD
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedMode == AgeCalcMode.CURRENT_DATE) {
                    // MODE 1: AUTOMATICALLY DETECTED CURRENT DATE
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Event, contentDescription = null, tint = ToolGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "বর্তমান তারিখ [ স্বয়ংক্রিয় ডিভাইস তারিখ ]",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "ডিভাইসের বর্তমান তারিখ থেকে স্বয়ংক্রিয়ভাবে পাওয়া",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "আজকের তারিখ:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currentDeviceDateFormatted,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = ToolGreen
                            )
                        }
                    }
                } else {
                    // MODE 2: CUSTOM TARGET DATE (হিসাবের তারিখ)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = ToolBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "হিসাবের তারিখ [ DD/MM/YYYY ]",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "নিজের পছন্দের যে তারিখে বয়স দেখতে চান",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customTargetDayStr,
                            onValueChange = { if (it.length <= 2) customTargetDayStr = it },
                            label = { Text("দিন (DD)") },
                            placeholder = { Text("14") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("input_target_day")
                        )

                        OutlinedTextField(
                            value = customTargetMonthStr,
                            onValueChange = { if (it.length <= 2) customTargetMonthStr = it },
                            label = { Text("মাস (MM)") },
                            placeholder = { Text("09") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("input_target_month")
                        )

                        OutlinedTextField(
                            value = customTargetYearStr,
                            onValueChange = { if (it.length <= 4) customTargetYearStr = it },
                            label = { Text("বছর (YYYY)") },
                            placeholder = { Text("2026") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.4f).testTag("input_target_year")
                        )
                    }
                }
            }
        }

        // [ হিসাব করুন ] BUTTON
        Button(
            onClick = { performCalculation() },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ToolBlue),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_calculate_age")
        ) {
            Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "হিসাব করুন",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        // ERROR MESSAGE DISPLAY
        if (errorMessage != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CrimsonPrimary.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CrimsonPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = CrimsonPrimary
                    )
                }
            }
        }

        // AGE RESULT DISPLAY
        val res = calculatedResult
        if (res != null) {
            // Check if today is the birthday
            val isBirthdayToday = (selectedMode == AgeCalcMode.CURRENT_DATE &&
                    birthDayStr.toIntOrNull() == currentDeviceDay &&
                    birthMonthStr.toIntOrNull() == currentDeviceMonth)

            // BIRTHDAY BANNER IF TODAY IS BIRTHDAY
            if (isBirthdayToday) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ToolOrange.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ToolOrange),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "আজ আপনার জন্মদিন 🎉",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ToolOrange
                        )
                    }
                }
            }

            // PRIMARY RESULT CARD
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ToolBlue.copy(alpha = 0.12f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, ToolBlue.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_age_hero_result")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (selectedMode == AgeCalcMode.CURRENT_DATE) "আপনার বয়স" else "এই তারিখে বয়স",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "${res.years} বছর  ${res.months} মাস  ${res.days} দিন",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = ToolBlue,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("text_age_main_result")
                    )

                    Text(
                        text = "${res.years} Years, ${res.months} Months, ${res.days} Days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // DETAILED STATISTICS CARD: মোট দিন, মোট সপ্তাহ, মোট মাস
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "বয়সের বিস্তারিত বিবরণ",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // মোট দিন
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "মোট দিন:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${res.totalDays} দিন",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = ToolGreen
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // মোট সপ্তাহ: পূর্ণ সপ্তাহ + অবশিষ্ট দিন
                    val remainingDaysInWeek = res.totalDays % 7
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "মোট সপ্তাহ:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${res.totalWeeks} সপ্তাহ ${remainingDaysInWeek} দিন",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = ToolBlue
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // মোট মাস: পূর্ণ মাস + অবশিষ্ট দিন
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "মোট মাস:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${res.totalMonths} মাস ${res.days} দিন",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = ToolPurple
                        )
                    }
                }
            }

            // NEXT BIRTHDAY CARD: পরবর্তী জন্মদিন & বার
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ToolOrange.copy(alpha = 0.12f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, ToolOrange.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_next_birthday")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cake, contentDescription = null, tint = ToolOrange, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "পরবর্তী জন্মদিন",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "তারিখ:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = res.nextBirthdayDate,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "বার:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${res.nextBirthdayWeekdayBn} (${res.nextBirthdayWeekdayEn})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ToolOrange
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "বাকি আছে: ${res.nextBirthdayMonthsRemaining} মাস ${res.nextBirthdayDaysRemaining} দিন",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // SAVE TO HISTORY
            Button(
                onClick = {
                    val inputDesc = "জন্ম: $birthDayStr/$birthMonthStr/$birthYearStr, গণনা: ${if (selectedMode == AgeCalcMode.CURRENT_DATE) currentDeviceDateFormatted else "$customTargetDayStr/$customTargetMonthStr/$customTargetYearStr"}"
                    val resultDesc = "${res.years} বছর ${res.months} মাস ${res.days} দিন (মোট ${res.totalDays} দিন)"
                    HisabHistoryManager.addHistory(
                        type = "AGE",
                        titleBn = "বয়স ক্যালকুলেটর",
                        inputDescBn = inputDesc,
                        resultBn = resultDesc
                    )
                    onSavedToHistory("বয়সের হিসাব হিস্ট্রিতে সেভ করা হয়েছে!")
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ToolBlue),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("হিস্ট্রিতে সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
            }
        }
    }
}
