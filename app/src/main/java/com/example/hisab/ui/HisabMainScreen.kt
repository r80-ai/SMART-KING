package com.example.hisab.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hisab.HisabHistoryManager
import com.example.hisab.HisabSubScreen
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ToolBlue
import com.example.ui.theme.ToolGreen
import com.example.ui.theme.ToolOrange
import com.example.ui.theme.ToolPurple
import com.example.ui.theme.ToolTeal
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HisabMainScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentSubScreen by remember { mutableStateOf(HisabSubScreen.HOME) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    val historyList by HisabHistoryManager.history.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    fun showToast(msg: String) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
        }
    }

    // BACK NAVIGATION LOGIC:
    // Sub-screens navigate back to HISAB HOME
    // HISAB HOME navigates back to PDF MASTER HOME
    BackHandler {
        if (currentSubScreen != HisabSubScreen.HOME) {
            currentSubScreen = HisabSubScreen.HOME
        } else {
            onBack()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("hisab_calculator_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when (currentSubScreen) {
                                    HisabSubScreen.HOME -> "Hisab Calculator"
                                    HisabSubScreen.SHOP_HISAB -> "দোকান হিসাব"
                                    HisabSubScreen.AGE_CALCULATOR -> "বয়স ক্যালকুলেটর"
                                    HisabSubScreen.NORMAL_CALCULATOR -> "সাধারণ ক্যালকুলেটর"
                                },
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (currentSubScreen == HisabSubScreen.HOME) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = ToolGreen.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "PRO",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = ToolGreen,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = when (currentSubScreen) {
                                HisabSubScreen.HOME -> "দোকান ও দৈনন্দিন ব্যবসার সহজ সমাধান"
                                HisabSubScreen.SHOP_HISAB -> "দাম, পরিমাণ, কেজি-গ্রাম, ছাড়, GST ও লাভ-ক্ষতি"
                                HisabSubScreen.AGE_CALCULATOR -> "বয়স গণনা ও পরবর্তী জন্মদিন"
                                HisabSubScreen.NORMAL_CALCULATOR -> "মৌলিক পাটিগণিত ও ফলাফল ইতিহাস"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (currentSubScreen != HisabSubScreen.HOME) {
                                currentSubScreen = HisabSubScreen.HOME
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier.testTag("btn_hisab_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showHistoryDialog = true },
                        modifier = Modifier.testTag("btn_hisab_history")
                    ) {
                        BadgedBox(
                            badge = {
                                if (historyList.isNotEmpty()) {
                                    Badge(containerColor = ToolGreen, contentColor = Color.White) {
                                        Text("${historyList.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Crossfade(
            targetState = currentSubScreen,
            label = "hisab_subscreen_crossfade",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { screen ->
            when (screen) {
                HisabSubScreen.HOME -> {
                    HisabCalculatorHomeScreen(
                        onOpenShopHisab = { currentSubScreen = HisabSubScreen.SHOP_HISAB },
                        onOpenAgeCalculator = { currentSubScreen = HisabSubScreen.AGE_CALCULATOR },
                        onOpenNormalCalculator = { currentSubScreen = HisabSubScreen.NORMAL_CALCULATOR },
                        onOpenHistory = { showHistoryDialog = true }
                    )
                }
                HisabSubScreen.SHOP_HISAB -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 60.dp)
                    ) {
                        HisabShopSection(onSavedToHistory = { showToast(it) })
                    }
                }
                HisabSubScreen.AGE_CALCULATOR -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 60.dp)
                    ) {
                        HisabAgeSection(onSavedToHistory = { showToast(it) })
                    }
                }
                HisabSubScreen.NORMAL_CALCULATOR -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 60.dp)
                    ) {
                        HisabNormalCalculatorSection(onSavedToHistory = { showToast(it) })
                    }
                }
            }
        }
    }

    // CALCULATION HISTORY DIALOG
    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = ToolGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "হিসাবের ইতিহাস",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    if (historyList.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                HisabHistoryManager.clearHistory()
                                showToast("হিস্ট্রি মুছে ফেলা হয়েছে!")
                            }
                        ) {
                            Text("ইতিহাস মুছুন", color = CrimsonPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            text = {
                if (historyList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "কোনো হিসাব সংরক্ষিত নেই।\nযেকোনো হিসাব শেষে 'হিস্ট্রিতে সংরক্ষণ করুন' চাপলে এখানে জমা হবে।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val sdf = remember { SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()) }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(historyList) { item ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = item.titleBn,
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = ToolGreen
                                        )
                                        Text(
                                            text = sdf.format(Date(item.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = item.inputDescriptionBn,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = item.resultBn,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHistoryDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ToolGreen)
                ) {
                    Text("ঠিক আছে")
                }
            }
        )
    }
}

/**
 * HISAB CALCULATOR HOME SCREEN
 * Displays Header:
 *   হিসাব ক্যালকুলেটর
 *   দোকান ও দৈনন্দিন ব্যবসার সহজ সমাধান
 * Shows 3 Large Tappable Cards:
 *   1. Hisab Calculator (দোকান হিসাব)
 *   2. Age Calculator (বয়স ক্যালকুলেটর)
 *   3. Calculator (সাধারণ ক্যালকুলেটর)
 */
@Composable
private fun HisabCalculatorHomeScreen(
    onOpenShopHisab: () -> Unit,
    onOpenAgeCalculator: () -> Unit,
    onOpenNormalCalculator: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val historyList by HisabHistoryManager.history.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // HEADER HERO CARD (PDF MASTER PREMIUM DARK STYLE)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "হিসাব ক্যালকুলেটর",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "দোকান ও দৈনন্দিন ব্যবসার সহজ সমাধান",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ToolGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = ToolGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // 3 LARGE MAIN CATEGORY CARDS
        Text(
            text = "ক্যালকুলেটর বিভাগ",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )

        // 1. HISAB CALCULATOR (দোকান হিসাব)
        HisabCategoryBigCard(
            categoryNumber = "১",
            titleEn = "Hisab Calculator",
            titleBn = "দোকান ও ব্যবসা হিসাব",
            description = "নির্দিষ্ট পরিমাণের দাম থেকে KG দাম, টাকা অনুযায়ী পরিমাণ, ছাড়, বৃদ্ধি, GST ও লাভ-ক্ষতি হিসাব",
            icon = Icons.Default.ShoppingBag,
            accentColor = ToolGreen,
            testTag = "card_nav_shop_hisab",
            onClick = onOpenShopHisab
        )

        // 2. AGE CALCULATOR (বয়স ক্যালকুলেটর)
        HisabCategoryBigCard(
            categoryNumber = "২",
            titleEn = "Age Calculator",
            titleBn = "বয়স ক্যালকুলেটর",
            description = "জন্মতারিখ থেকে বর্তমান বা পছন্দের তারিখের সঠিক বয়স, মোট দিন/সপ্তাহ ও পরবর্তী জন্মদিন",
            icon = Icons.Default.Cake,
            accentColor = ToolBlue,
            testTag = "card_nav_age_calc",
            onClick = onOpenAgeCalculator
        )

        // 3. NORMAL CALCULATOR (সাধারণ ক্যালকুলেটর)
        HisabCategoryBigCard(
            categoryNumber = "৩",
            titleEn = "Calculator",
            titleBn = "সাধারণ ক্যালকুলেটর",
            description = "যোগ, বিয়োগ, গুণ, ভাগ, শতকরা, দশমিক এবং দ্রুত গণনার কিপ্যাড",
            icon = Icons.Default.Dialpad,
            accentColor = ToolOrange,
            testTag = "card_nav_normal_calc",
            onClick = onOpenNormalCalculator
        )

        // RECENT HISTORY QUICK BAR
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenHistory() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History",
                        tint = ToolGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "হিসাবের ইতিহাস (${historyList.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (historyList.isEmpty()) "এখনও কোনো হিসাব সেভ করা হয়নি" else "সর্বশেষ হিসাব ও বিস্তারিত দেখতে ট্যাপ করুন",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // FUTURE CALCULATORS MODULAR READY CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        tint = ToolPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "আরও টুলস (ভবিষ্যত সম্প্রসারণ)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "EMI, লোন, সুদ, বিদ্যুৎ বিল, জমি পরিমাপ ও মুদ্রা কনভার্টারের মতো ভবিষ্যৎ ক্যালকুলেটর সরাসরি এই Hisab Calculator মডিউলে যুক্ত করার জন্য আর্কিটেকচার প্রস্তুত রয়েছে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

/**
 * Large premium rounded card for Hisab Calculator Home
 */
@Composable
private fun HisabCategoryBigCard(
    categoryNumber: String,
    titleEn: String,
    titleBn: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container with accent background
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = titleEn,
                    tint = accentColor,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = titleEn,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = accentColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = categoryNumber,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = titleBn,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = accentColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Open $titleEn",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
