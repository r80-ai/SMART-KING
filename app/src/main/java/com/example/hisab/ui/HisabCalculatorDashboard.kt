package com.example.hisab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hisab.HisabHistoryManager
import com.example.ui.theme.ToolBlue
import com.example.ui.theme.ToolGreen
import com.example.ui.theme.ToolOrange
import com.example.ui.theme.ToolPurple

/**
 * Updated Hisab Calculator Dashboard Composable.
 * Features:
 *   - Header Hero Card (হিসাব ক্যালকুলেটর)
 *   - 3 Main Categories:
 *       1. Hisab Calculator (দোকান হিসাব)
 *       2. Age Calculator (বয়স ক্যালকুলেটর)
 *       3. Calculator (সাধারণ ক্যালকুলেটর)
 *   - Recent Calculation History quick bar
 *   - Future expansion ready card
 *
 * The following cards have been removed per specification:
 *   - "আমার দোকান - Xerox & Service Hisab"
 *   - "Billing Memo / জেরক্স ও প্রিন্ট বিল"
 *   - "Quick QR / দ্রুত পেমেন্ট QR"
 */
@Composable
fun HisabCalculatorDashboard(
    onOpenShopHisab: () -> Unit,
    onOpenAgeCalculator: () -> Unit,
    onOpenNormalCalculator: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val historyList by HisabHistoryManager.history.collectAsState()

    Column(
        modifier = modifier
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
