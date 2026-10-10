package com.example.hisab.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import java.util.Locale
import com.example.hisab.*
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ToolBlue
import com.example.ui.theme.ToolGreen
import com.example.ui.theme.ToolOrange
import com.example.ui.theme.ToolPurple
import com.example.ui.theme.ToolTeal

enum class ShopSubTab(val titleBn: String) {
    MAIN_SHOP("দোকান হিসাব"),
    DISCOUNT("ছাড় (Discount)"),
    INCREASE("দাম বৃদ্ধি"),
    GST("GST ক্যালকুলেটর"),
    PROFIT_LOSS("লাভ ও ক্ষতি")
}

enum class WeightCalculatorMode(val titleBn: String) {
    STANDARD("দর ও পরিমাণ হিসাব"),
    MONEY_TO_QTY("টাকা থেকে পরিমাণ নির্ণয়")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HisabShopSection(
    onSavedToHistory: (String) -> Unit
) {
    var selectedSubTab by remember { mutableStateOf(ShopSubTab.MAIN_SHOP) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP SUB-TABS: MAIN SHOP, DISCOUNT, INCREASE, GST, PROFIT/LOSS
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(ShopSubTab.values()) { tab ->
                val isSelected = selectedSubTab == tab
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedSubTab = tab },
                    label = { Text(tab.titleBn, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ToolGreen.copy(alpha = 0.2f),
                        selectedLabelColor = ToolGreen
                    )
                )
            }
        }

        when (selectedSubTab) {
            ShopSubTab.MAIN_SHOP -> MainShopkeeperSubSection(onSavedToHistory)
            ShopSubTab.DISCOUNT -> ShopDiscountSubSection(onSavedToHistory)
            ShopSubTab.INCREASE -> ShopIncreaseSubSection(onSavedToHistory)
            ShopSubTab.GST -> ShopGstSubSection(onSavedToHistory)
            ShopSubTab.PROFIT_LOSS -> ShopProfitLossSubSection(onSavedToHistory)
        }
    }
}

/**
 * 1. MAIN SHOPKEEPER CALCULATION SYSTEM
 * Implements:
 * - কত পরিমাণ মালের দাম জানেন? [ 100 ] [ Gram ▼ ]
 * - এই পরিমাণের দাম [ ₹15 ]
 * - কত পরিমাণের দাম জানতে চান? [ 500 ] [ Gram ▼ ]
 * - [ হিসাব করুন ]
 * - Result: 1 KG = ₹150.00, 500 Gram = ₹75.00
 * - Quick quantity buttons: 50 Gram, 100 Gram, 200 Gram, 250 Gram, 500 Gram
 * - টাকা অনুযায়ী পরিমাণ (Money to Quantity): 1 KG = ₹100, Customer gives ₹50 -> 500 Gram
 */
@Composable
private fun MainShopkeeperSubSection(onSavedToHistory: (String) -> Unit) {
    // Inputs (defaults to mandatory test case: 100 Gram = ₹15 -> 1 KG = ₹150, 500 Gram = ₹75)
    var givenQuantityStr by remember { mutableStateOf("100") }
    var selectedGivenUnit by remember { mutableStateOf(HisabUnit.GRAM) }
    var givenPriceStr by remember { mutableStateOf("15") }

    var requiredQuantityStr by remember { mutableStateOf("500") }
    var selectedRequiredUnit by remember { mutableStateOf(HisabUnit.GRAM) }

    // Money to quantity input (e.g. ₹50)
    var budgetAmountStr by remember { mutableStateOf("") }

    // Calculated state
    var calculatedShopResult by remember { mutableStateOf<ShopHisabResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun calculate() {
        errorMessage = null
        val gQty = givenQuantityStr.toDoubleOrNull()
        val gPrice = givenPriceStr.toDoubleOrNull()
        val rQty = requiredQuantityStr.toDoubleOrNull() ?: 0.0
        val bAmount = budgetAmountStr.toDoubleOrNull() ?: 0.0

        if (gQty == null || gQty <= 0.0) {
            errorMessage = "জানা মালের সঠিক পরিমাণ লিখুন (০-এর বেশি)"
            calculatedShopResult = null
            return
        }

        if (gPrice == null || gPrice <= 0.0) {
            errorMessage = "মালের সঠিক দাম লিখুন (০-এর বেশি)"
            calculatedShopResult = null
            return
        }

        // Check unit compatibility
        if (selectedGivenUnit.category != selectedRequiredUnit.category) {
            errorMessage = "একই ধরনের ইউনিট বেছে নিন (যেমন ওজন ও তরল মিশ্রিত করা যাবে না)"
            calculatedShopResult = null
            return
        }

        calculatedShopResult = HisabCalculationEngine.calculateShopHisab(
            givenQuantity = gQty,
            givenUnit = selectedGivenUnit,
            givenPrice = gPrice,
            requiredQuantity = rQty,
            requiredUnit = selectedRequiredUnit,
            budgetAmount = bAmount
        )
    }

    // Auto-calculate on initial load
    LaunchedEffect(Unit) {
        calculate()
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        var weightMode by remember { mutableStateOf(WeightCalculatorMode.STANDARD) }

        // UNIT CATEGORY SELECTOR CHIPS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HisabUnitCategory.values().forEach { category ->
                val isSelected = selectedGivenUnit.category == category
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val defaultUnit = when (category) {
                            HisabUnitCategory.WEIGHT -> HisabUnit.GRAM
                            HisabUnitCategory.LIQUID -> HisabUnit.ML
                            HisabUnitCategory.PIECE -> HisabUnit.PIECE
                        }
                        selectedGivenUnit = defaultUnit
                        selectedRequiredUnit = defaultUnit
                        calculate()
                    },
                    label = { Text(category.labelBn, fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ToolGreen.copy(alpha = 0.2f),
                        selectedLabelColor = ToolGreen
                    )
                )
            }
        }

        // Mode Selector specifically for ওজন (কেজি/গ্রাম)
        if (selectedGivenUnit.category == HisabUnitCategory.WEIGHT) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(4.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = weightMode == WeightCalculatorMode.STANDARD,
                        onClick = { weightMode = WeightCalculatorMode.STANDARD },
                        label = {
                            Text(
                                "দর ও পরিমাণ হিসাব",
                                fontSize = 12.sp,
                                fontWeight = if (weightMode == WeightCalculatorMode.STANDARD) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (weightMode == WeightCalculatorMode.STANDARD) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f).testTag("chip_weight_standard")
                    )

                    FilterChip(
                        selected = weightMode == WeightCalculatorMode.MONEY_TO_QTY,
                        onClick = { weightMode = WeightCalculatorMode.MONEY_TO_QTY },
                        label = {
                            Text(
                                "টাকা থেকে পরিমাণ নির্ণয়",
                                fontSize = 12.sp,
                                fontWeight = if (weightMode == WeightCalculatorMode.MONEY_TO_QTY) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (weightMode == WeightCalculatorMode.MONEY_TO_QTY) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ToolOrange.copy(alpha = 0.2f),
                            selectedLabelColor = ToolOrange
                        ),
                        modifier = Modifier.weight(1f).testTag("chip_weight_money_to_qty")
                    )
                }
            }
        }

        // Dedicated Money to Quantity mode for ওজন (কেজি/গ্রাম)
        if (selectedGivenUnit.category == HisabUnitCategory.WEIGHT && weightMode == WeightCalculatorMode.MONEY_TO_QTY) {
            WeightMoneyToQuantityCard(onSavedToHistory = onSavedToHistory)
        } else {
            // STANDARD SHOPKEEPER CALCULATOR (100% Intact)

        // INPUT CARD 1: কত পরিমাণ মালের দাম জানেন? & এই পরিমাণের দাম
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
                        Icon(Icons.Default.Scale, contentDescription = null, tint = ToolGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "জানা পরিমাণ ও দাম",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = {
                            givenQuantityStr = ""
                            givenPriceStr = ""
                            requiredQuantityStr = ""
                            budgetAmountStr = ""
                            calculatedShopResult = null
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(18.dp))
                    }
                }

                Text(
                    text = "কত পরিমাণ মালের দাম জানেন?",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = givenQuantityStr,
                        onValueChange = {
                            givenQuantityStr = it
                            calculate()
                        },
                        label = { Text("পরিমাণ") },
                        placeholder = { Text("100") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1.2f).testTag("input_given_quantity")
                    )

                    var expandedGivenUnit by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                        Button(
                            onClick = { expandedGivenUnit = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Text(
                                text = selectedGivenUnit.symbolBn,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        DropdownMenu(
                            expanded = expandedGivenUnit,
                            onDismissRequest = { expandedGivenUnit = false }
                        ) {
                            HisabUnit.values().filter { it.category == selectedGivenUnit.category }.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit.symbolBn + " (" + unit.symbolEn + ")") },
                                    onClick = {
                                        selectedGivenUnit = unit
                                        expandedGivenUnit = false
                                        calculate()
                                    }
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "এই পরিমাণের দাম",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = givenPriceStr,
                    onValueChange = {
                        givenPriceStr = it
                        calculate()
                    },
                    label = { Text("দাম (টাকা / ₹)") },
                    placeholder = { Text("15") },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = ToolGreen) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("input_given_price")
                )
            }
        }

        // INPUT CARD 2: কত পরিমাণের দাম জানতে চান?
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = ToolBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "কত পরিমাণের দাম জানতে চান?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = requiredQuantityStr,
                        onValueChange = {
                            requiredQuantityStr = it
                            calculate()
                        },
                        label = { Text("প্রয়োজনীয় পরিমাণ") },
                        placeholder = { Text("500") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1.2f).testTag("input_required_quantity")
                    )

                    var expandedReqUnit by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                        Button(
                            onClick = { expandedReqUnit = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Text(
                                text = selectedRequiredUnit.symbolBn,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        DropdownMenu(
                            expanded = expandedReqUnit,
                            onDismissRequest = { expandedReqUnit = false }
                        ) {
                            HisabUnit.values().filter { it.category == selectedRequiredUnit.category }.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit.symbolBn + " (" + unit.symbolEn + ")") },
                                    onClick = {
                                        selectedRequiredUnit = unit
                                        expandedReqUnit = false
                                        calculate()
                                    }
                                )
                            }
                        }
                    }
                }

                // QUICK QUANTITY BUTTONS: 50 Gram, 100 Gram, 200 Gram, 250 Gram, 500 Gram
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "দ্রুত পরিমাণ নির্বাচন:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val quickPresets = when (selectedRequiredUnit.category) {
                            HisabUnitCategory.WEIGHT -> listOf("50", "100", "200", "250", "500", "1000")
                            HisabUnitCategory.LIQUID -> listOf("100", "200", "250", "500", "1000")
                            HisabUnitCategory.PIECE -> listOf("1", "4", "6", "10", "12")
                        }
                        items(quickPresets) { preset ->
                            ElevatedFilterChip(
                                selected = requiredQuantityStr == preset,
                                onClick = {
                                    requiredQuantityStr = preset
                                    calculate()
                                },
                                label = { Text("$preset ${selectedRequiredUnit.symbolBn}", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // [ হিসাব করুন ] BUTTON
        Button(
            onClick = { calculate() },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ToolGreen),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_calculate_shop")
        ) {
            Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "হিসাব করুন",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        // ERROR DISPLAY
        if (errorMessage != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CrimsonPrimary.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CrimsonPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = errorMessage ?: "", color = CrimsonPrimary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // RESULT DISPLAY: 1 KG = ₹150.00 & 500 Gram = ₹75.00
        val res = calculatedShopResult
        if (res != null) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ToolGreen.copy(alpha = 0.14f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, ToolGreen.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().testTag("card_shop_result")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ফলাফল (Result)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Standard Rate (e.g. 1 KG = ₹150.00)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = res.standardLabelBn,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = HisabCalculationEngine.formatCurrency(res.pricePerKgOrStandard),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = ToolGreen
                        )
                    }

                    // Required Quantity Price (e.g. 500 Gram = ₹75.00)
                    if (requiredQuantityStr.isNotBlank() && (requiredQuantityStr.toDoubleOrNull() ?: 0.0) > 0.0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$requiredQuantityStr ${selectedRequiredUnit.symbolBn} = ",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = HisabCalculationEngine.formatCurrency(res.requiredPrice),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = ToolBlue
                            )
                        }
                    }

                    // Rate breakdown
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "প্রতি গ্রাম/মি.লি/পিস হার: ₹${String.format(Locale.US, "%.3f", res.pricePerGramOrBase)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // SECTION B: টাকা অনুযায়ী পরিমাণ (Money to Quantity)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = ToolOrange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "টাকা অনুযায়ী পরিমাণ (Money to Quantity)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "কাস্টমার যত টাকা দেবেন তা লিখুন (যেমন ₹50 দিলে কত মাল পাবে):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = budgetAmountStr,
                    onValueChange = {
                        budgetAmountStr = it
                        calculate()
                    },
                    label = { Text("টাকার পরিমাণ (₹)") },
                    placeholder = { Text("50") },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = ToolOrange) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("input_budget_amount")
                )

                // Quick Rupee Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val rupeePresets = listOf("10", "20", "30", "50", "100", "200", "500")
                    items(rupeePresets) { preset ->
                        ElevatedFilterChip(
                            selected = budgetAmountStr == preset,
                            onClick = {
                                budgetAmountStr = preset
                                calculate()
                            },
                            label = { Text("₹$preset", fontSize = 11.sp) }
                        )
                    }
                }

                val budgetVal = budgetAmountStr.toDoubleOrNull() ?: 0.0
                if (res != null && budgetVal > 0.0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ToolOrange.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ToolOrange.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "টাকা: ₹$budgetAmountStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "পাওয়া যাবে:",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = res.budgetQuantityFormattedBn,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = ToolOrange
                            )
                        }
                    }
                }
            }
        }

        // SAVE TO HISTORY
        if (res != null) {
            Button(
                onClick = {
                    val inputDesc = "$givenQuantityStr ${selectedGivenUnit.symbolBn} = ₹$givenPriceStr"
                    val resultDesc = if (requiredQuantityStr.isNotBlank()) {
                        "$requiredQuantityStr ${selectedRequiredUnit.symbolBn} = ${HisabCalculationEngine.formatCurrency(res.requiredPrice)} (${res.standardLabelBn} = ${HisabCalculationEngine.formatCurrency(res.pricePerKgOrStandard)})"
                    } else {
                        "${res.standardLabelBn} = ${HisabCalculationEngine.formatCurrency(res.pricePerKgOrStandard)}"
                    }
                    HisabHistoryManager.addHistory(
                        type = "SHOP",
                        titleBn = "দোকান হিসাব",
                        inputDescBn = inputDesc,
                        resultBn = resultDesc
                    )
                    onSavedToHistory("দোকান হিসাব সফলভাবে হিস্ট্রিতে সেভ করা হয়েছে!")
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ToolGreen),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("হিস্ট্রিতে সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
            }
        }

            // Also display the dedicated "টাকা থেকে পরিমাণ নির্ণয়" card at bottom of weight section for quick access
            if (selectedGivenUnit.category == HisabUnitCategory.WEIGHT) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                WeightMoneyToQuantityCard(onSavedToHistory = onSavedToHistory)
            }
        }
    }
}

/**
 * Dedicated "টাকা থেকে পরিমাণ নির্ণয়" (Money to Quantity Calculator) Card for "ওজন (কেজি/গ্রাম)"
 * Calculates how much quantity (KG / Gram) a customer gets for a specific amount of money,
 * based on known quantity and price.
 */
@Composable
fun WeightMoneyToQuantityCard(
    onSavedToHistory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Inputs (Defaults: 10 KG = ₹150, customer gives ₹30 -> 2 KG / 2000 GM)
    var knownQuantityStr by remember { mutableStateOf("10") }
    var isKnownUnitKg by remember { mutableStateOf(true) } // true: KG, false: GM
    var knownPriceStr by remember { mutableStateOf("150") }

    var targetMoneyStr by remember { mutableStateOf("30") }

    var calculatedResult by remember { mutableStateOf<MoneyToQuantityResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun calculate() {
        errorMessage = null
        val kQty = knownQuantityStr.toDoubleOrNull()
        val kPrice = knownPriceStr.toDoubleOrNull()
        val tMoney = targetMoneyStr.toDoubleOrNull()

        if (kQty == null || kQty <= 0.0) {
            errorMessage = "জানা পরিমাণ সঠিকভাবে লিখুন (০-এর বেশি)"
            calculatedResult = null
            return
        }

        if (kPrice == null || kPrice <= 0.0) {
            errorMessage = "জানা পরিমাণের মোট দাম লিখুন (০-এর বেশি)"
            calculatedResult = null
            return
        }

        if (tMoney == null || tMoney <= 0.0) {
            errorMessage = "কাস্টমার যত টাকার মাল নিতে চান তা লিখুন (০-এর বেশি)"
            calculatedResult = null
            return
        }

        calculatedResult = HisabCalculationEngine.calculateMoneyToQuantity(
            knownQuantity = kQty,
            isKg = isKnownUnitKg,
            knownPrice = kPrice,
            targetMoney = tMoney
        )
    }

    // Auto-calculate on initial render and input changes
    LaunchedEffect(knownQuantityStr, isKnownUnitKg, knownPriceStr, targetMoneyStr) {
        val kQty = knownQuantityStr.toDoubleOrNull()
        val kPrice = knownPriceStr.toDoubleOrNull()
        val tMoney = targetMoneyStr.toDoubleOrNull()
        if (kQty != null && kQty > 0.0 && kPrice != null && kPrice > 0.0 && tMoney != null && tMoney > 0.0) {
            errorMessage = null
            calculatedResult = HisabCalculationEngine.calculateMoneyToQuantity(
                knownQuantity = kQty,
                isKg = isKnownUnitKg,
                knownPrice = kPrice,
                targetMoney = tMoney
            )
        }
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, ToolOrange.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth().testTag("card_m2q_container")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. CARD HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ToolOrange.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.CurrencyRupee,
                                contentDescription = null,
                                tint = ToolOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "জানা পরিমাণ ও দাম থেকে টাকার পরিমাণ নির্ণয়",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "টাকায় কত মাল পাবেন? (Money to Quantity)",
                            style = MaterialTheme.typography.bodySmall,
                            color = ToolOrange,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(
                    onClick = {
                        knownQuantityStr = ""
                        knownPriceStr = ""
                        targetMoneyStr = ""
                        calculatedResult = null
                        errorMessage = null
                    },
                    modifier = Modifier.size(32.dp).testTag("btn_m2q_reset")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(18.dp))
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

            // 2. STEP 1: জানা পরিমাণ ও দাম (Known Quantity & Price)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ToolGreen,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("১", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ধাপ ১: জানা পরিমাণ ও মোট দাম",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "জানা পরিমাণ ও একক নির্বাচন করুন:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = knownQuantityStr,
                            onValueChange = { knownQuantityStr = it },
                            label = { Text("জানা পরিমাণ") },
                            placeholder = { Text("10") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1.2f).testTag("input_m2q_known_qty")
                        )

                        // KG / Gram Unit Toggle
                        Row(
                            modifier = Modifier
                                .weight(1.2f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isKnownUnitKg) ToolGreen else Color.Transparent)
                                    .clickable { isKnownUnitKg = true }
                                    .testTag("toggle_m2q_unit_kg"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "কেজি (KG)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isKnownUnitKg) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (!isKnownUnitKg) ToolGreen else Color.Transparent)
                                    .clickable { isKnownUnitKg = false }
                                    .testTag("toggle_m2q_unit_gm"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "গ্রাম (GM)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isKnownUnitKg) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Text(
                        text = "এই পরিমাণের মোট দাম:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = knownPriceStr,
                        onValueChange = { knownPriceStr = it },
                        label = { Text("এই পরিমাণের মোট দাম (₹)") },
                        placeholder = { Text("150") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = ToolGreen) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("input_m2q_known_price")
                    )
                }
            }

            // 3. STEP 2: নির্দিষ্ট টাকা (Target Money)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ToolOrange,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("২", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ধাপ ২: কাস্টমারের নির্দিষ্ট টাকা",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "কাস্টমার যত টাকার মাল নিতে চান তা লিখুন বা দ্রুত সিলেক্ট করুন:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = targetMoneyStr,
                        onValueChange = { targetMoneyStr = it },
                        label = { Text("কাস্টমার যত টাকার মাল নিতে চান (₹)") },
                        placeholder = { Text("30") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = ToolOrange) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("input_m2q_target_money")
                    )

                    // Quick Amount Chips: ₹10, ₹20, ₹30, ₹50, ₹100, ₹200, ₹500
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "দ্রুত টাকার পরিমাণ:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val amountChips = listOf("10", "20", "30", "50", "100", "200", "500")
                            items(amountChips) { chipAmt ->
                                val isSelected = targetMoneyStr == chipAmt
                                ElevatedFilterChip(
                                    selected = isSelected,
                                    onClick = { targetMoneyStr = chipAmt },
                                    label = { Text("₹$chipAmt", fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.elevatedFilterChipColors(
                                        selectedContainerColor = ToolOrange.copy(alpha = 0.2f),
                                        selectedLabelColor = ToolOrange
                                    ),
                                    modifier = Modifier.testTag("chip_m2q_$chipAmt")
                                )
                            }
                        }
                    }
                }
            }

            // 4. ACTION BUTTON: "হিসাব করুন"
            Button(
                onClick = { calculate() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ToolOrange),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_m2q_calculate")
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "হিসাব করুন",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Error display if any
            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CrimsonPrimary.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = errorMessage ?: "", color = CrimsonPrimary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // 5. RESULT DISPLAY CARD (ফলাফল)
            val result = calculatedResult
            if (result != null) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = ToolGreen.copy(alpha = 0.12f)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, ToolGreen.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth().testTag("card_m2q_result")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ToolGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ফলাফল: ₹${result.targetMoney.toLong()} টাকায় মাল পাবেন",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ToolGreen
                                )
                            }
                        }

                        // Exact calculated quantity in both KG and Gram
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // KG Box
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, ToolGreen.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "কেজি (KG)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = result.formattedKgBn,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = ToolGreen,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // Gram Box
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, ToolBlue.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "গ্রাম (Gram)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = result.formattedGramsBn,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = ToolBlue,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Rate Breakdown
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "দর বিশ্লেষণ (Breakdown Rates):",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "• ${result.rate1KgBn}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "• ${result.rate100GramBn}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "• ${result.ratePerGramBn}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Save to History button
                        Button(
                            onClick = {
                                val unitStr = if (result.isKg) "কেজি" else "গ্রাম"
                                val inputDesc = "${result.knownQuantity} $unitStr = ₹${result.knownPrice}, কাস্টমার টাকা = ₹${result.targetMoney}"
                                val resultDesc = "মাল পাবেন: ${result.formattedKgBn} / ${result.formattedGramsBn} (${result.rate1KgBn})"
                                HisabHistoryManager.addHistory(
                                    type = "SHOP_WEIGHT_M2Q",
                                    titleBn = "টাকা থেকে পরিমাণ নির্ণয় (ওজন)",
                                    inputDescBn = inputDesc,
                                    resultBn = resultDesc
                                )
                                onSavedToHistory("টাকা থেকে পরিমাণের হিসাব হিস্ট্রিতে সেভ করা হয়েছে!")
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ToolGreen),
                            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("btn_m2q_save_history")
                        ) {
                            Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("হিস্ট্রিতে সংরক্ষণ করুন", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * DISCOUNT SUB-SECTION (ছাড় / ডিসকাউন্ট)
 * Formula: discountAmount = price × discount / 100, finalPrice = price - discountAmount
 * Example: মূল দাম = ₹1000, ছাড় = 12% -> ছাড় = ₹120, চূড়ান্ত দাম = ₹880
 */
@Composable
private fun ShopDiscountSubSection(onSavedToHistory: (String) -> Unit) {
    var originalPriceStr by remember { mutableStateOf("1000") }
    var discountPercentStr by remember { mutableStateOf("12") }
    var flatDiscountStr by remember { mutableStateOf("") }

    val origPrice = originalPriceStr.toDoubleOrNull() ?: 0.0
    val discPct = discountPercentStr.toDoubleOrNull() ?: 0.0
    val flatDisc = flatDiscountStr.toDoubleOrNull() ?: 0.0

    val discountResult = remember(origPrice, discPct, flatDisc) {
        if (origPrice > 0.0) {
            HisabCalculationEngine.calculateDiscount(origPrice, discPct, flatDisc)
        } else null
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "ছাড় বা ডিসকাউন্ট হিসাব (Discount)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = originalPriceStr,
                    onValueChange = { originalPriceStr = it },
                    label = { Text("মূল দাম (₹)") },
                    placeholder = { Text("1000") },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = ToolTeal) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("input_discount_original_price")
                )

                OutlinedTextField(
                    value = discountPercentStr,
                    onValueChange = { discountPercentStr = it },
                    label = { Text("ছাড়ের হার (%)") },
                    placeholder = { Text("12") },
                    trailingIcon = { Text("%", fontWeight = FontWeight.Bold, color = ToolTeal) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("input_discount_percent")
                )

                // Quick Discount chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val presets = listOf("5", "10", "12", "15", "20", "25", "50")
                    items(presets) { preset ->
                        ElevatedFilterChip(
                            selected = discountPercentStr == preset,
                            onClick = { discountPercentStr = preset },
                            label = { Text("$preset%") }
                        )
                    }
                }
            }
        }

        if (discountResult != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ToolTeal.copy(alpha = 0.15f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, ToolTeal.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().testTag("card_discount_result")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("মোট ছাড়:", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            HisabCalculationEngine.formatCurrency(discountResult.discountAmount),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = CrimsonPrimary
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("চূড়ান্ত দাম:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            HisabCalculationEngine.formatCurrency(discountResult.finalPrice),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = ToolTeal
                        )
                    }
                }
            }

            Button(
                onClick = {
                    HisabHistoryManager.addHistory(
                        type = "DISCOUNT",
                        titleBn = "ছাড় (Discount)",
                        inputDescBn = "মূল দাম: ₹$originalPriceStr, ছাড়: $discountPercentStr%",
                        resultBn = "ছাড় = ${HisabCalculationEngine.formatCurrency(discountResult.discountAmount)}, চূড়ান্ত দাম = ${HisabCalculationEngine.formatCurrency(discountResult.finalPrice)}"
                    )
                    onSavedToHistory("ডিসকাউন্ট হিসাব হিস্ট্রিতে সেভ করা হয়েছে!")
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ToolTeal),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("হিস্ট্রিতে সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * INCREASE SUB-SECTION (দাম বৃদ্ধি)
 * Example: মূল দাম = ₹1000, বৃদ্ধি = 12% -> বাড়তি = ₹120, চূড়ান্ত দাম = ₹1120
 */
@Composable
private fun ShopIncreaseSubSection(onSavedToHistory: (String) -> Unit) {
    var originalPriceStr by remember { mutableStateOf("1000") }
    var increasePercentStr by remember { mutableStateOf("12") }

    val origPrice = originalPriceStr.toDoubleOrNull() ?: 0.0
    val incPct = increasePercentStr.toDoubleOrNull() ?: 0.0

    val increaseResult = remember(origPrice, incPct) {
        if (origPrice > 0.0) {
            HisabCalculationEngine.calculateIncrease(origPrice, incPct)
        } else null
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "দাম বৃদ্ধির হিসাব (Price Increase / Markup)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = originalPriceStr,
                    onValueChange = { originalPriceStr = it },
                    label = { Text("মূল দাম (₹)") },
                    placeholder = { Text("1000") },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = ToolOrange) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = increasePercentStr,
                    onValueChange = { increasePercentStr = it },
                    label = { Text("বৃদ্ধির হার (%)") },
                    placeholder = { Text("12") },
                    trailingIcon = { Text("%", fontWeight = FontWeight.Bold, color = ToolOrange) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (increaseResult != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ToolOrange.copy(alpha = 0.15f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, ToolOrange.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("বাড়তি টাকা:", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            HisabCalculationEngine.formatCurrency(increaseResult.increaseAmount),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = ToolOrange
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("চূড়ান্ত দাম:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            HisabCalculationEngine.formatCurrency(increaseResult.finalPrice),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = ToolOrange
                        )
                    }
                }
            }

            Button(
                onClick = {
                    HisabHistoryManager.addHistory(
                        type = "INCREASE",
                        titleBn = "দাম বৃদ্ধি (Increase)",
                        inputDescBn = "মূল দাম: ₹$originalPriceStr, বৃদ্ধি: $increasePercentStr%",
                        resultBn = "বাড়তি = ${HisabCalculationEngine.formatCurrency(increaseResult.increaseAmount)}, চূড়ান্ত দাম = ${HisabCalculationEngine.formatCurrency(increaseResult.finalPrice)}"
                    )
                    onSavedToHistory("দাম বৃদ্ধির হিসাব হিস্ট্রিতে সেভ করা হয়েছে!")
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ToolOrange),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("হিস্ট্রিতে সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * GST SUB-SECTION (জিএসটি)
 * Example: মূল দাম = ₹1000, GST = 18% -> GST = ₹180, মোট দাম = ₹1180
 */
@Composable
private fun ShopGstSubSection(onSavedToHistory: (String) -> Unit) {
    var amountStr by remember { mutableStateOf("1000") }
    var gstRateStr by remember { mutableStateOf("18") }
    var isGstIncluded by remember { mutableStateOf(false) }

    val amount = amountStr.toDoubleOrNull() ?: 0.0
    val gstRate = gstRateStr.toDoubleOrNull() ?: 0.0

    val gstResult = remember(amount, gstRate, isGstIncluded) {
        if (amount > 0.0 && gstRate >= 0.0) {
            HisabCalculationEngine.calculateGst(amount, gstRate, isGstIncluded)
        } else null
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "GST ক্যালকুলেটর (CGST + SGST)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isGstIncluded,
                        onClick = { isGstIncluded = false },
                        label = { Text("GST যোগ করুন (+ GST)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isGstIncluded,
                        onClick = { isGstIncluded = true },
                        label = { Text("GST অন্তর্ভুক্ত (Reverse GST)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(if (isGstIncluded) "মোট মূল্য (GST সহ)" else "মূল দাম (GST ছাড়া)") },
                    placeholder = { Text("1000") },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = ToolBlue) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("input_gst_amount")
                )

                OutlinedTextField(
                    value = gstRateStr,
                    onValueChange = { gstRateStr = it },
                    label = { Text("GST হার (%)") },
                    placeholder = { Text("18") },
                    trailingIcon = { Text("%", fontWeight = FontWeight.Bold, color = ToolBlue) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("input_gst_rate")
                )

                // Quick GST slabs: 5%, 12%, 18%, 28%
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val slabs = listOf("5", "12", "18", "28")
                    items(slabs) { slab ->
                        ElevatedFilterChip(
                            selected = gstRateStr == slab,
                            onClick = { gstRateStr = slab },
                            label = { Text("$slab%") }
                        )
                    }
                }
            }
        }

        if (gstResult != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ToolBlue.copy(alpha = 0.15f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, ToolBlue.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().testTag("card_gst_result")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("মূল দাম (Net Amount):", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(HisabCalculationEngine.formatCurrency(gstResult.baseAmount), fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("CGST (${gstRate / 2}%):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(HisabCalculationEngine.formatCurrency(gstResult.cgstAmount), style = MaterialTheme.typography.bodySmall)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("SGST (${gstRate / 2}%):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(HisabCalculationEngine.formatCurrency(gstResult.sgstAmount), style = MaterialTheme.typography.bodySmall)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("মোট GST (${gstRate}%):", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = ToolBlue)
                        Text(HisabCalculationEngine.formatCurrency(gstResult.totalGstAmount), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = ToolBlue)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("সর্বমোট প্রদেয়:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            HisabCalculationEngine.formatCurrency(gstResult.totalAmount),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = ToolBlue
                        )
                    }
                }
            }

            Button(
                onClick = {
                    HisabHistoryManager.addHistory(
                        type = "GST",
                        titleBn = "GST ক্যালকুলেটর",
                        inputDescBn = "মূল্য: ₹$amountStr (${if (isGstIncluded) "অন্তর্ভুক্ত" else "ছাড়া"}), GST: $gstRateStr%",
                        resultBn = "GST = ${HisabCalculationEngine.formatCurrency(gstResult.totalGstAmount)}, মোট = ${HisabCalculationEngine.formatCurrency(gstResult.totalAmount)}"
                    )
                    onSavedToHistory("GST হিসাব হিস্ট্রিতে সেভ করা হয়েছে!")
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

/**
 * PROFIT & LOSS SUB-SECTION (লাভ ও ক্ষতি)
 * Example: ক্রয়মূল্য = ₹800, বিক্রয়মূল্য = ₹1000 -> লাভ = ₹200 (২৫%)
 */
@Composable
private fun ShopProfitLossSubSection(onSavedToHistory: (String) -> Unit) {
    var costPriceStr by remember { mutableStateOf("800") }
    var sellingPriceStr by remember { mutableStateOf("1000") }

    val cost = costPriceStr.toDoubleOrNull() ?: 0.0
    val sell = sellingPriceStr.toDoubleOrNull() ?: 0.0

    val plResult = remember(cost, sell) {
        if (cost > 0.0 && sell > 0.0) {
            HisabCalculationEngine.calculateProfitLoss(cost, sell)
        } else null
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "লাভ ও ক্ষতি হিসাব (Profit & Loss)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = costPriceStr,
                    onValueChange = { costPriceStr = it },
                    label = { Text("ক্রয়মূল্য / কেনা দাম (₹)") },
                    placeholder = { Text("800") },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = ToolOrange) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("input_cost_price")
                )

                OutlinedTextField(
                    value = sellingPriceStr,
                    onValueChange = { sellingPriceStr = it },
                    label = { Text("বিক্রয়মূল্য / বেচা দাম (₹)") },
                    placeholder = { Text("1000") },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = ToolGreen) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("input_selling_price")
                )
            }
        }

        if (plResult != null) {
            val statusColor = if (plResult.isProfit) ToolGreen else CrimsonPrimary
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.15f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().testTag("card_profit_loss_result")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (plResult.isProfit) "মোট লাভ:" else "মোট ক্ষতি:",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = statusColor
                        )
                        Text(
                            text = HisabCalculationEngine.formatCurrency(plResult.difference),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = statusColor
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (plResult.isProfit) "লাভের হার:" else "ক্ষতির হার:",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.2f", plResult.percentage)}%",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = statusColor
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val statusText = if (plResult.isProfit) "লাভ" else "ক্ষতি"
                    HisabHistoryManager.addHistory(
                        type = "PROFIT_LOSS",
                        titleBn = "লাভ ও ক্ষতি ($statusText)",
                        inputDescBn = "কেনা: ₹$costPriceStr, বেচা: ₹$sellingPriceStr",
                        resultBn = "$statusText = ${HisabCalculationEngine.formatCurrency(plResult.difference)} (${String.format(Locale.US, "%.2f", plResult.percentage)}%)"
                    )
                    onSavedToHistory("লাভ-ক্ষতির হিসাব হিস্ট্রিতে সেভ করা হয়েছে!")
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = statusColor),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("হিস্ট্রিতে সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
            }
        }
    }
}
