package com.example.calendar.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val baseMonthDate = remember { CalendarDate(today.year, today.month, 1) }

    // Centered virtual pagination for smooth horizontal month swipe (100 years forward/backward)
    val initialPage = 1200
    val pageCount = 2400
    val pagerState = rememberPagerState(initialPage = initialPage) { pageCount }

    var selectedDate by remember { mutableStateOf(today) }

    // Top Header visible month derived dynamically from horizontal pager state
    val displayedMonth = remember(pagerState.currentPage) {
        baseMonthDate.plusMonths(pagerState.currentPage - initialPage)
    }

    // Synchronous instant events for instantaneous zero-latency display upon tapping any date cell
    val instantEvents = remember(selectedDate) {
        CalendarEventsRepository.getAllEventsForDateSync(selectedDate)
    }

    // Events for selected date (enriched with Room DB custom festivals and user birthdays)
    var selectedDateEvents by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    // Events for the current visible month (for holidays dialog)
    var monthEvents by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }

    var showHolidaysDialog by remember { mutableStateOf(false) }
    var showAddBirthdayDialog by remember { mutableStateOf(false) }

    // Synchronize selectedDate when user swipes to a new month
    LaunchedEffect(pagerState.currentPage) {
        val newMonth = baseMonthDate.plusMonths(pagerState.currentPage - initialPage)
        if (selectedDate.year != newMonth.year || selectedDate.month != newMonth.month) {
            val newDay = selectedDate.day.coerceAtMost(newMonth.daysInMonth)
            selectedDate = CalendarDate(newMonth.year, newMonth.month, newDay)
        }
    }

    // Asynchronously fetch enriched events for selected date
    LaunchedEffect(selectedDate) {
        selectedDateEvents = repo.getEventsForDate(selectedDate)
    }

    // Fetch month events whenever the displayed month changes
    LaunchedEffect(displayedMonth.year, displayedMonth.month) {
        monthEvents = repo.getEventsForMonth(displayedMonth.year, displayedMonth.month)
    }

    val selectedBanglaDate = remember(selectedDate) {
        BanglaCalendarConverter.toBanglaDate(selectedDate)
    }

    // Bengali month span for the currently displayed Gregorian month (e.g. "আশ্বিন ১৪৩৩ - কার্তিক ১৪৩৩")
    val monthRangeSubtitle = remember(displayedMonth.year, displayedMonth.month) {
        val firstDayOfMonth = CalendarDate(displayedMonth.year, displayedMonth.month, 1)
        val lastDayOfMonth = CalendarDate(displayedMonth.year, displayedMonth.month, firstDayOfMonth.daysInMonth)
        val startBangla = BanglaCalendarConverter.toBanglaDate(firstDayOfMonth)
        val endBangla = BanglaCalendarConverter.toBanglaDate(lastDayOfMonth)
        if (startBangla.monthNameBn == endBangla.monthNameBn) {
            "${startBangla.monthNameBn} ${startBangla.yearBn}"
        } else {
            "${startBangla.monthNameBn} ${startBangla.yearBn} - ${endBangla.monthNameBn} ${endBangla.yearBn}"
        }
    }

    val displayEvents = if (selectedDateEvents.isNotEmpty()) selectedDateEvents else instantEvents

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
                            text = "${BanglaDate.getGregorianMonthNameBn(displayedMonth.month)} ${BanglaDate.toBanglaDigits(displayedMonth.year)}",
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
                                    val newDate = CalendarDate(y, m + 1, d)
                                    selectedDate = newDate
                                    val targetPage = initialPage + ((newDate.year - baseMonthDate.year) * 12 + (newDate.month - baseMonthDate.month))
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(targetPage)
                                    }
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

                    // Previous Month Arrow (<)
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        modifier = Modifier.testTag("btn_prev_month_bangla")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous Month",
                            tint = Color.White
                        )
                    }

                    // Next Month Arrow (>)
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier.testTag("btn_next_month_bangla")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Month",
                            tint = Color.White
                        )
                    }

                    // Holiday List Dialog Button
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
            // 1. HORIZONTAL PAGER FOR SWIPE MONTH NAVIGATION (Right to Left: Next, Left to Right: Previous)
            item {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calendar_month_pager"),
                    verticalAlignment = Alignment.Top,
                    beyondViewportPageCount = 1
                ) { page ->
                    val pageMonth = remember(page) {
                        baseMonthDate.plusMonths(page - initialPage)
                    }

                    BanglaCalendarMonthGrid(
                        pageMonth = pageMonth,
                        selectedDate = selectedDate,
                        today = today,
                        onDateClick = { clickedDate ->
                            selectedDate = clickedDate
                            val targetPage = initialPage + ((clickedDate.year - baseMonthDate.year) * 12 + (clickedDate.month - baseMonthDate.month))
                            if (targetPage != pagerState.currentPage) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(targetPage)
                                }
                            }
                        }
                    )
                }
            }

            // 2. SELECTED DATE MULTI-CALENDAR HEADER
            item {
                val selectedHijriDate = remember(selectedDate) {
                    HijriCalendarEngine.toHijriDate(selectedDate)
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121927)),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_selected_date_header")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val englishMonthName = when (selectedDate.month) {
                            1 -> "January"; 2 -> "February"; 3 -> "March"; 4 -> "April"
                            5 -> "May"; 6 -> "June"; 7 -> "July"; 8 -> "August"
                            9 -> "September"; 10 -> "October"; 11 -> "November"; 12 -> "December"
                            else -> ""
                        }

                        // 1. English Gregorian Date
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedDate.day} $englishMonthName ${selectedDate.year}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = Color.White
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E293B)
                            ) {
                                Text(
                                    text = "${selectedDate.day} ${BanglaDate.getGregorianMonthNameBn(selectedDate.month)} ${BanglaDate.toBanglaDigits(selectedDate.year)} খ্রিষ্টাব্দ",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = Color(0xFFE2E8F0),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // 2. Bengali Date (Bangabda)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Color(0xFFFFB74D),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "${selectedBanglaDate.dayBn} ${selectedBanglaDate.monthNameBn} ${selectedBanglaDate.yearBn} বঙ্গাব্দ (${selectedBanglaDate.dayNameBn})",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                ),
                                color = Color(0xFFFFB74D)
                            )
                        }

                        // 3. Hijri Date (Lunar / Islamic)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NightsStay,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "${selectedHijriDate.dayBn} ${selectedHijriDate.monthNameBn} ${selectedHijriDate.yearBn} হিজরি (${selectedHijriDate.monthNameAr})",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp
                                ),
                                color = Color(0xFF34D399)
                            )
                        }
                    }
                }
            }

            // 3. ENHANCED FESTIVAL DETAILS LIST WITH PROMINENT PREVIEWS & REASON FOR CELEBRATION
            if (displayEvents.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "আজকের উৎসব ও বিশেষ বিবরণী (${BanglaDate.toBanglaDigits(displayEvents.size)})",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp
                                ),
                                color = Color(0xFFE2E8F0)
                            )
                        }

                        displayEvents.forEach { ev ->
                            FestivalEnhancedDetailCard(
                                event = ev,
                                isToday = (selectedDate == today)
                            )
                        }
                    }
                }
            } else {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1522)),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventAvailable,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "${selectedBanglaDate.dayNameBn} — কোনো বিশেষ পূজা বা ছুটির উল্লেখ নেই",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    ),
                                    color = Color(0xFFCBD5E1)
                                )
                                Text(
                                    text = "নিয়মিত দিনপঞ্জিকা ও সাধারণ কর্মদিবস",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }

            // 4. DAY STEPPER CONTROLS & ADD BIRTHDAY BUTTON
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val newDate = selectedDate.minusDays(1)
                            selectedDate = newDate
                            val targetPage = initialPage + ((newDate.year - baseMonthDate.year) * 12 + (newDate.month - baseMonthDate.month))
                            if (targetPage != pagerState.currentPage) {
                                coroutineScope.launch { pagerState.animateScrollToPage(targetPage) }
                            }
                        },
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
                        onClick = {
                            selectedDate = today
                            val targetPage = initialPage + ((today.year - baseMonthDate.year) * 12 + (today.month - baseMonthDate.month))
                            coroutineScope.launch { pagerState.animateScrollToPage(targetPage) }
                        },
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
                        onClick = {
                            val newDate = selectedDate.plusDays(1)
                            selectedDate = newDate
                            val targetPage = initialPage + ((newDate.year - baseMonthDate.year) * 12 + (newDate.month - baseMonthDate.month))
                            if (targetPage != pagerState.currentPage) {
                                coroutineScope.launch { pagerState.animateScrollToPage(targetPage) }
                            }
                        },
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
        val displayedBanglaDate = remember(displayedMonth) {
            BanglaCalendarConverter.toBanglaDate(displayedMonth)
        }
        MonthHolidaysDialog(
            isBangla = true,
            monthName = displayedBanglaDate.monthNameBn,
            yearString = "${displayedBanglaDate.yearBn} বঙ্গাব্দ",
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
                    monthEvents = repo.getEventsForMonth(displayedMonth.year, displayedMonth.month)
                }
            }
        )
    }
}

/**
 * 7-Column Month Grid for a single page in the HorizontalPager.
 * Renders instantaneously using pre-calculated synchronous events map.
 */
@Composable
fun BanglaCalendarMonthGrid(
    pageMonth: CalendarDate,
    selectedDate: CalendarDate,
    today: CalendarDate,
    onDateClick: (CalendarDate) -> Unit
) {
    val firstDayOfMonth = remember(pageMonth.year, pageMonth.month) {
        CalendarDate(pageMonth.year, pageMonth.month, 1)
    }
    val startDayOfWeek = firstDayOfMonth.dayOfWeek // 1 = Sunday
    val daysInMonth = firstDayOfMonth.daysInMonth
    val leadingEmptyCells = startDayOfWeek - 1

    val prevMonthDate = remember(pageMonth.year, pageMonth.month) { firstDayOfMonth.minusMonths(1) }
    val prevDaysInMonth = remember(prevMonthDate.year, prevMonthDate.month) {
        CalendarDate(prevMonthDate.year, prevMonthDate.month, 1).daysInMonth
    }
    val nextMonthDate = remember(pageMonth.year, pageMonth.month) { firstDayOfMonth.plusMonths(1) }

    val totalCells = leadingEmptyCells + daysInMonth
    val rows = (totalCells + 6) / 7

    // Synchronously compute event maps for all cells in the visible grid for 60fps zero-lag swiping
    val monthEventsMap = remember(pageMonth.year, pageMonth.month) {
        val map = mutableMapOf<CalendarDate, List<CalendarEvent>>()
        for (r in 0 until rows) {
            for (c in 0..6) {
                val cellIndex = r * 7 + c
                val cellDate = when {
                    cellIndex < leadingEmptyCells -> {
                        val day = prevDaysInMonth - leadingEmptyCells + 1 + cellIndex
                        CalendarDate(prevMonthDate.year, prevMonthDate.month, day)
                    }
                    cellIndex < leadingEmptyCells + daysInMonth -> {
                        val day = cellIndex - leadingEmptyCells + 1
                        CalendarDate(pageMonth.year, pageMonth.month, day)
                    }
                    else -> {
                        val day = cellIndex - (leadingEmptyCells + daysInMonth) + 1
                        CalendarDate(nextMonthDate.year, nextMonthDate.month, day)
                    }
                }
                map[cellDate] = CalendarEventsRepository.getAllEventsForDateSync(cellDate)
            }
        }
        map
    }

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
                                CalendarDate(pageMonth.year, pageMonth.month, day) to true
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
                                .clickable { onDateClick(cellDate) }
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

/**
 * Enhanced Festival Detail Card featuring a large visual preview image/banner,
 * category badge, title, subtitle, and an explicit "কেন ও কী কারণে পালিত হয়"
 * (Why it is celebrated / significance) section.
 */
@Composable
fun FestivalEnhancedDetailCard(
    event: CalendarEvent,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    val (badgeText, badgeBg, badgeFg) = when (event.type) {
        "ISLAMIC" -> Triple("ইসলামিক উৎসব", Color(0x2A10B981), Color(0xFF34D399))
        "HINDU" -> Triple("সনাতন / পূজা", Color(0x2AF59E0B), Color(0xFFFFD54F))
        "BIRTHDAY" -> Triple("জন্মদিন", Color(0x2AEC4899), Color(0xFFF472B6))
        else -> when (event.category) {
            EventCategory.GOVERNMENT_HOLIDAY -> Triple("সরকারি ছুটি", Color(0x2AEF4444), Color(0xFFF87171))
            EventCategory.NATIONAL_DAY -> Triple("জাতীয় দিবস", Color(0x2A38BDF8), Color(0xFF38BDF8))
            EventCategory.SCHOOL_HOLIDAY -> Triple("বিদ্যালয় ছুটি", Color(0x2AA855F7), Color(0xFFC084FC))
            else -> Triple("বিশেষ দিবস", Color(0x2A64748B), Color(0xFF94A3B8))
        }
    }

    val description = event.descriptionBn?.takeIf { it.isNotBlank() }
        ?: event.descriptionEn?.takeIf { it.isNotBlank() }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111723)),
        border = BorderStroke(1.dp, Color(0xFF1E2B3E)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("festival_card_${event.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Prominent Visual Preview + Category Chips + Title & Subtitle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prominent large visual preview image or high-resolution festival badge (70dp)
                CalendarCellVisual(
                    event = event,
                    isToday = isToday,
                    modifier = Modifier.size(70.dp),
                    size = 70.dp
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Category & Status Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = badgeBg
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = badgeFg,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                            )
                        }

                        if (event.isHoliday && event.category != EventCategory.GOVERNMENT_HOLIDAY) {
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = Color(0x2AEF4444)
                            ) {
                                Text(
                                    text = "ছুটির দিন",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFFF87171),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                )
                            }
                        }

                        if (event.moonSightingAdjustable) {
                            Text(
                                text = "±১ দিন চাঁদ সাপেক্ষ",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Main Title (Bengali)
                    Text(
                        text = event.titleBn,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            lineHeight = 22.sp
                        ),
                        color = Color.White
                    )

                    // Subtitle / English Name
                    if (!event.titleEn.isNullOrBlank() && event.titleEn != event.titleBn) {
                        Text(
                            text = event.titleEn,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // Explicit "কেন ও কী কারণে পালিত হয়" / Significance Section
            if (!description.isNullOrBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0B1019), RoundedCornerShape(10.dp))
                        .border(0.6.dp, Color(0xFF1E2838), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "কেন ও কী কারণে পালিত হয়:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = Color(0xFFFFD54F)
                        )
                    }

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.5.sp,
                            lineHeight = 20.sp
                        ),
                        color = Color(0xFFE2E8F0)
                    )
                }
            }
        }
    }
}

private fun formatShortEnglishDate(date: CalendarDate): String {
    val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val mName = if (date.month in 1..12) monthNames[date.month - 1] else ""
    return "%02d %s".format(date.day, mName)
}
