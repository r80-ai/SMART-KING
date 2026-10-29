package com.example.calendar.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.model.BanglaDate
import com.example.calendar.model.CalendarDate
import com.example.calendar.model.CalendarEvent
import com.example.calendar.model.EventCategory
import com.example.calendar.util.BanglaCalendarConverter
import com.example.calendar.util.CalendarEventsRepository
import com.example.calendar.util.HijriCalendarEngine
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BanglaCalendarScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repo = remember { CalendarEventsRepository(context) }

    val today = remember { CalendarDate.today() }
    var selectedDate by remember { mutableStateOf(today) }

    // Events for selected date
    var selectedDateEvents by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    // Events for the current month
    var monthEvents by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    // Cached map of events for all cells in the visible grid
    var monthEventsMap by remember { mutableStateOf<Map<CalendarDate, List<CalendarEvent>>>(emptyMap()) }

    var showHolidaysDialog by remember { mutableStateOf(false) }
    var showAddBirthdayDialog by remember { mutableStateOf(false) }

    // Fetch events when selected date changes
    LaunchedEffect(selectedDate) {
        selectedDateEvents = repo.getEventsForDate(selectedDate)
    }

    // Fetch month events when month/year changes
    LaunchedEffect(selectedDate.year, selectedDate.month) {
        val events = repo.getEventsForMonth(selectedDate.year, selectedDate.month)
        monthEvents = events

        // Pre-cache events for all cells in the visible grid (including adjacent month margins)
        val firstDay = CalendarDate(selectedDate.year, selectedDate.month, 1)
        val startDay = firstDay.dayOfWeek
        val leading = startDay - 1
        val prevMonth = firstDay.minusMonths(1)
        val prevDays = CalendarDate(prevMonth.year, prevMonth.month, 1).daysInMonth
        val nextMonth = firstDay.plusMonths(1)

        val map = mutableMapOf<CalendarDate, List<CalendarEvent>>()
        val total = leading + firstDay.daysInMonth
        val rowCount = (total + 6) / 7
        for (i in 0 until (rowCount * 7)) {
            val d = when {
                i < leading -> CalendarDate(prevMonth.year, prevMonth.month, prevDays - leading + 1 + i)
                i < leading + firstDay.daysInMonth -> CalendarDate(selectedDate.year, selectedDate.month, i - leading + 1)
                else -> CalendarDate(nextMonth.year, nextMonth.month, i - (leading + firstDay.daysInMonth) + 1)
            }
            map[d] = repo.getEventsForDate(d)
        }
        monthEventsMap = map
    }

    val selectedBanglaDate = remember(selectedDate) {
        BanglaCalendarConverter.toBanglaDate(selectedDate)
    }

    // Bengali month span for the current Gregorian month view (e.g. "ভাদ্র ১৪৩২ - আশ্বিন ১৪৩২")
    val monthRangeSubtitle = remember(selectedDate.year, selectedDate.month) {
        val firstDayOfMonth = CalendarDate(selectedDate.year, selectedDate.month, 1)
        val lastDayOfMonth = CalendarDate(selectedDate.year, selectedDate.month, firstDayOfMonth.daysInMonth)
        val startBangla = BanglaCalendarConverter.toBanglaDate(firstDayOfMonth)
        val endBangla = BanglaCalendarConverter.toBanglaDate(lastDayOfMonth)
        if (startBangla.monthNameBn == endBangla.monthNameBn) {
            "${startBangla.monthNameBn} ${startBangla.yearBn}"
        } else {
            "${startBangla.monthNameBn} ${startBangla.yearBn} - ${endBangla.monthNameBn} ${endBangla.yearBn}"
        }
    }

    Scaffold(
        containerColor = Color(0xFF0B0F17),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "বাংলা ক্যালেন্ডার",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "${BanglaDate.getGregorianMonthNameBn(selectedDate.month)} ${BanglaDate.toBanglaDigits(selectedDate.year)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = Color(0xFFE2E8F0)
                        )
                        Text(
                            text = monthRangeSubtitle,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_back_bangla_calendar")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Date Picker Dialog
                    IconButton(
                        onClick = {
                            val dp = DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    selectedDate = CalendarDate(y, m + 1, d)
                                },
                                selectedDate.year,
                                selectedDate.month - 1,
                                selectedDate.day
                            )
                            dp.show()
                        },
                        modifier = Modifier.testTag("btn_date_picker_bangla")
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = "Pick Date", tint = Color.White)
                    }

                    // Previous Month Arrow
                    IconButton(
                        onClick = { selectedDate = selectedDate.minusMonths(1) },
                        modifier = Modifier.testTag("btn_prev_month_bangla")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous Month",
                            tint = Color.White
                        )
                    }

                    // Next Month Arrow
                    IconButton(
                        onClick = { selectedDate = selectedDate.plusMonths(1) },
                        modifier = Modifier.testTag("btn_next_month_bangla")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Month",
                            tint = Color.White
                        )
                    }

                    // Holiday List
                    IconButton(
                        onClick = { showHolidaysDialog = true },
                        modifier = Modifier.testTag("btn_holidays_list_bangla")
                    ) {
                        Icon(Icons.Default.FormatListBulleted, contentDescription = "Holiday List", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0B0F17)
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("screen_bangla_calendar"),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 7-COLUMN CALENDAR MONTH GRID
            item {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111722)),
                    border = BorderStroke(1.dp, Color(0xFF2B3342)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Weekday header bar in warm cream
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFFDF0))
                                .padding(vertical = 7.dp)
                        ) {
                            val daysOfWeek = listOf("রবি", "সোম", "মঙ্গল", "বুধ", "বৃহ", "শুক্র", "শনি")
                            daysOfWeek.forEachIndexed { index, name ->
                                val isSunday = index == 0
                                Text(
                                    text = name,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = if (isSunday) Color(0xFFD32F2F) else Color(0xFF111827)
                                )
                            }
                        }

                        HorizontalDivider(thickness = 1.dp, color = Color(0xFFE2D8C3))

                        // Grid of days
                        val firstDayOfMonth = CalendarDate(selectedDate.year, selectedDate.month, 1)
                        val startDayOfWeek = firstDayOfMonth.dayOfWeek // 1 = Sunday
                        val daysInMonth = firstDayOfMonth.daysInMonth
                        val leadingEmptyCells = startDayOfWeek - 1

                        val prevMonthDate = firstDayOfMonth.minusMonths(1)
                        val prevDaysInMonth = CalendarDate(prevMonthDate.year, prevMonthDate.month, 1).daysInMonth
                        val nextMonthDate = firstDayOfMonth.plusMonths(1)

                        val totalCells = leadingEmptyCells + daysInMonth
                        val rows = (totalCells + 6) / 7

                        for (r in 0 until rows) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                for (c in 0..6) {
                                    val cellIndex = r * 7 + c
                                    val (cellDate, isCurrentMonth) = when {
                                        cellIndex < leadingEmptyCells -> {
                                            val day = prevDaysInMonth - leadingEmptyCells + 1 + cellIndex
                                            CalendarDate(prevMonthDate.year, prevMonthDate.month, day) to false
                                        }
                                        cellIndex < leadingEmptyCells + daysInMonth -> {
                                            val day = cellIndex - leadingEmptyCells + 1
                                            CalendarDate(selectedDate.year, selectedDate.month, day) to true
                                        }
                                        else -> {
                                            val day = cellIndex - (leadingEmptyCells + daysInMonth) + 1
                                            CalendarDate(nextMonthDate.year, nextMonthDate.month, day) to false
                                        }
                                    }

                                    val cellBangla = BanglaCalendarConverter.toBanglaDate(cellDate)
                                    val isSelected = cellDate == selectedDate
                                    val isToday = cellDate == today
                                    val isSunday = c == 0

                                    val cellEvents = monthEventsMap[cellDate] ?: emptyList()
                                    val isHoliday = isSunday || cellEvents.any { it.isHoliday || it.category == EventCategory.GOVERNMENT_HOLIDAY }
                                    val visualEvent = cellEvents.firstOrNull { it.resolveDrawableRes() != null || !it.photoUrl.isNullOrEmpty() }
                                        ?: cellEvents.firstOrNull { it.visualType != null || it.type == "ISLAMIC" || it.type == "HINDU" }
                                    val textEvent = cellEvents.firstOrNull { it != visualEvent } ?: cellEvents.firstOrNull()

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(82.dp)
                                            .background(
                                                when {
                                                    isToday -> Color(0xFFFFE38C) // Today: warm vibrant golden yellow
                                                    !isCurrentMonth -> Color(0xFF0B0F17) // Out of month: deep slate
                                                    else -> Color(0xFF111722) // Current month: clean dark slate
                                                }
                                            )
                                            .border(
                                                width = 0.5.dp,
                                                color = Color(0xFF252C3A)
                                            )
                                            .then(
                                                when {
                                                    isToday -> Modifier.border(2.dp, Color(0xFFEAB308))
                                                    isSelected -> Modifier.border(1.5.dp, Color(0xFF38BDF8))
                                                    else -> Modifier
                                                }
                                            )
                                            .clickable { selectedDate = cellDate }
                                            .testTag("calendar_cell_${cellDate.year}_${cellDate.month}_${cellDate.day}")
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 2.dp, vertical = 2.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // 1. English date at the top
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 2.dp, top = 1.dp),
                                                horizontalArrangement = Arrangement.Start
                                            ) {
                                                Text(
                                                    text = formatShortEnglishDate(cellDate),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                                    color = when {
                                                        isToday -> Color(0xFF111827)
                                                        !isCurrentMonth -> Color(0xFF4B5563)
                                                        else -> Color(0xFF94A3B8)
                                                    },
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }

                                            // 2. Bengali date prominent in center
                                            Text(
                                                text = cellBangla.dayBn,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontSize = if (cellBangla.dayBn.length > 1) 17.sp else 19.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = when {
                                                    isToday -> Color(0xFF111827)
                                                    isSunday || isHoliday -> Color(0xFFEF5350)
                                                    !isCurrentMonth -> Color(0xFF4B5563)
                                                    else -> Color.White
                                                },
                                                textAlign = TextAlign.Center
                                            )

                                            // 3. Festival photo/badge or clean Bengali label
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(36.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (visualEvent != null) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        CalendarCellVisual(
                                                            event = visualEvent,
                                                            isToday = isToday
                                                        )
                                                        Text(
                                                            text = visualEvent.titleBn,
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontSize = 7.sp,
                                                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                                            ),
                                                            color = when {
                                                                isToday -> Color(0xFF111827)
                                                                visualEvent.type == "ISLAMIC" -> Color(0xFF34D399)
                                                                visualEvent.type == "HINDU" -> Color(0xFFFFD54F)
                                                                visualEvent.isHoliday -> Color(0xFFEF5350)
                                                                else -> Color(0xFFF1F5F9)
                                                            },
                                                            maxLines = 1,
                                                            softWrap = false,
                                                            overflow = TextOverflow.Ellipsis,
                                                            textAlign = TextAlign.Center,
                                                            modifier = Modifier.padding(horizontal = 1.dp)
                                                        )
                                                    }
                                                } else if (textEvent != null) {
                                                    Text(
                                                        text = textEvent.titleBn,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 7.5.sp,
                                                            fontWeight = FontWeight.Normal
                                                        ),
                                                        color = when {
                                                            isToday -> Color(0xFF111827)
                                                            textEvent.type == "ISLAMIC" -> Color(0xFF34D399)
                                                            textEvent.type == "HINDU" -> Color(0xFFFFD54F)
                                                            isHoliday -> Color(0xFFEF5350)
                                                            else -> Color(0xFFE2E8F0)
                                                        },
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.padding(horizontal = 1.dp)
                                                    )
                                                } else {
                                                    Spacer(modifier = Modifier.height(1.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SELECTED DATE INFO CARD (Gregorian + Bengali Bangabda + Islamic Hijri + Active Events)
            item {
                val selectedHijriDate = remember(selectedDate) {
                    HijriCalendarEngine.toHijriDate(selectedDate)
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121927)),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Multi-calendar Date Headers
                        val englishMonthName = when (selectedDate.month) {
                            1 -> "January"; 2 -> "February"; 3 -> "March"; 4 -> "April"
                            5 -> "May"; 6 -> "June"; 7 -> "July"; 8 -> "August"
                            9 -> "September"; 10 -> "October"; 11 -> "November"; 12 -> "December"
                            else -> ""
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            // 1. English Gregorian Date
                            Text(
                                text = "${selectedDate.day} $englishMonthName ${selectedDate.year} | ${selectedDate.day} ${BanglaDate.getGregorianMonthNameBn(selectedDate.month)} ${BanglaDate.toBanglaDigits(selectedDate.year)} খ্রিষ্টাব্দ",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                ),
                                color = Color.White
                            )

                            // 2. Bengali Date (Bangabda)
                            Text(
                                text = "${selectedBanglaDate.dayBn} ${selectedBanglaDate.monthNameBn} ${selectedBanglaDate.yearBn} বঙ্গাব্দ (${selectedBanglaDate.dayNameBn})",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.5.sp
                                ),
                                color = Color(0xFFFFB74D) // Warm Bengali Orange
                            )

                            // 3. Hijri Date (Lunar / Islamic)
                            Text(
                                text = "${selectedHijriDate.dayBn} ${selectedHijriDate.monthNameBn} ${selectedHijriDate.yearBn} হিজরি (${selectedHijriDate.monthNameAr})",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.5.sp
                                ),
                                color = Color(0xFF34D399) // Emerald Mint Green
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 0.8.dp,
                            color = Color(0xFF1E293B)
                        )

                        // Active Events List for this date
                        if (selectedDateEvents.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                selectedDateEvents.forEach { ev ->
                                    val (badgeText, badgeBg, badgeFg) = when (ev.type) {
                                        "ISLAMIC" -> Triple("ইসলামিক উৎসব", Color(0x2210B981), Color(0xFF34D399))
                                        "HINDU" -> Triple("সনাতন / পূজা", Color(0x22F59E0B), Color(0xFFFFD54F))
                                        "BIRTHDAY" -> Triple("জন্মদিন", Color(0x22EC4899), Color(0xFFF472B6))
                                        else -> when (ev.category) {
                                            EventCategory.GOVERNMENT_HOLIDAY -> Triple("সরকারি ছুটি", Color(0x22EF4444), Color(0xFFF87171))
                                            EventCategory.NATIONAL_DAY -> Triple("জাতীয় দিবস", Color(0x2238BDF8), Color(0xFF38BDF8))
                                            else -> Triple("বিশেষ দিবস", Color(0x2264748B), Color(0xFF94A3B8))
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF0F1522), RoundedCornerShape(10.dp))
                                            .border(0.5.dp, Color(0xFF222F43), RoundedCornerShape(10.dp))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Circular / rounded avatar showcasing the corresponding drawable image or styled vector fallback
                                        CalendarCellVisual(
                                            event = ev,
                                            isToday = false,
                                            modifier = Modifier.size(52.dp),
                                            size = 52.dp
                                        )

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = badgeBg
                                                ) {
                                                    Text(
                                                        text = badgeText,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        color = badgeFg,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }

                                                if (ev.moonSightingAdjustable) {
                                                    Text(
                                                        text = "(±১ দিন চাঁদ দেখার ওপর নির্ভরশীল)",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = Color(0xFF94A3B8)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = ev.titleBn,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                ),
                                                color = badgeFg
                                            )

                                            if (!ev.titleEn.isNullOrBlank() && ev.titleEn != ev.titleBn) {
                                                Text(
                                                    text = ev.titleEn,
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                                    color = Color(0xFF94A3B8)
                                                )
                                            }

                                            if (!ev.descriptionBn.isNullOrBlank()) {
                                                Text(
                                                    text = ev.descriptionBn,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                                                    color = Color(0xFFCBD5E1)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "${selectedBanglaDate.dayNameBn} — কোনো বিশেষ পূজা বা ছুটির উল্লেখ নেই",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            // DAY STEPPER CONTROLS & ADD BIRTHDAY BUTTON
            // Guaranteed singleLine / softWrap=false with contentPadding to prevent any vertical wrapping
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { selectedDate = selectedDate.minusDays(1) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_prev_day_bangla"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE2E8F0)),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Text(
                            text = "পূর্বের দিন",
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Button(
                        onClick = { selectedDate = today },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_quick_today_bangla"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD97706),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "আজকের দিন",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    OutlinedButton(
                        onClick = { selectedDate = selectedDate.plusDays(1) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_next_day_bangla"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE2E8F0)),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Text(
                            text = "পরের দিন",
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Birthday button
                    FilledTonalButton(
                        onClick = { showAddBirthdayDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_add_bday_from_bangla"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = Color(0xFFE2E8F0)
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "জন্মদিন",
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }

    // MONTH HOLIDAYS DIALOG
    if (showHolidaysDialog) {
        MonthHolidaysDialog(
            isBangla = true,
            monthName = selectedBanglaDate.monthNameBn,
            yearString = "${selectedBanglaDate.yearBn} বঙ্গাব্দ",
            events = monthEvents,
            onDismiss = { showHolidaysDialog = false }
        )
    }

    // ADD BIRTHDAY DIALOG
    if (showAddBirthdayDialog) {
        AddBirthdayDialog(
            initialDate = selectedDate,
            isBangla = true,
            onDismiss = { showAddBirthdayDialog = false },
            onSave = { bday ->
                coroutineScope.launch {
                    repo.addUserBirthday(bday)
                    showAddBirthdayDialog = false
                    selectedDateEvents = repo.getEventsForDate(selectedDate)
                    monthEvents = repo.getEventsForMonth(selectedDate.year, selectedDate.month)
                }
            }
        )
    }
}

private fun formatShortEnglishDate(date: CalendarDate): String {
    val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val mName = if (date.month in 1..12) monthNames[date.month - 1] else ""
    return "%02d %s".format(date.day, mName)
}
