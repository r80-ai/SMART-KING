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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hisab.HisabCalculationEngine
import com.example.hisab.HisabExtraTool
import com.example.hisab.HisabHistoryManager
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ToolBlue
import com.example.ui.theme.ToolGreen
import com.example.ui.theme.ToolOrange
import com.example.ui.theme.ToolPurple
import com.example.ui.theme.ToolTeal

@Composable
fun HisabToolsSection(
    onSavedToHistory: (String) -> Unit
) {
    var selectedTool by remember { mutableStateOf(HisabExtraTool.DISCOUNT) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOOL SELECTOR CHIPS
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(HisabExtraTool.values()) { tool ->
                val isSelected = selectedTool == tool
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedTool = tool },
                    label = { Text(tool.titleBn, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ToolTeal.copy(alpha = 0.2f),
                        selectedLabelColor = ToolTeal
                    )
                )
            }
        }

        when (selectedTool) {
            HisabExtraTool.DISCOUNT -> DiscountSubSection(onSavedToHistory)
            HisabExtraTool.INCREASE -> IncreaseSubSection(onSavedToHistory)
            HisabExtraTool.GST -> GstSubSection(onSavedToHistory)
            HisabExtraTool.PROFIT_LOSS -> ProfitLossSubSection(onSavedToHistory)
        }
    }
}

@Composable
private fun DiscountSubSection(onSavedToHistory: (String) -> Unit) {
    var originalPriceStr by remember { mutableStateOf("1000") }
    var discountPercentStr by remember { mutableStateOf("10") }
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
                    label = { Text("আসল মূল্য (₹)") },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = ToolTeal) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = discountPercentStr,
                        onValueChange = { discountPercentStr = it },
                        label = { Text("ছাড় শতাংশ (%)") },
                        trailingIcon = { Text("%", fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = flatDiscountStr,
                        onValueChange = { flatDiscountStr = it },
                        label = { Text("সরাসরি ছাড় (₹)") },
                        placeholder = { Text("ঐচ্ছিক") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Quick chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("5", "10", "15", "20", "25", "30", "50").forEach { pct ->
                        item {
                            AssistChip(
                                onClick = { discountPercentStr = pct },
                                label = { Text("$pct%") }
                            )
                        }
                    }
                }
            }
        }

        if (discountResult != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ToolTeal.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("মোট ছাড় (Savings):", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = HisabCalculationEngine.formatCurrency(discountResult.savings),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = ToolOrange
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("পরিশোধযোগ্য মূল্য:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = HisabCalculationEngine.formatCurrency(discountResult.finalPrice),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = ToolTeal
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val inputDesc = "মূল্য: ₹$origPrice, ছাড়: $discPct%"
                    val resultDesc = "ছাড়: ₹${discountResult.savings} | শেষ মূল্য: ₹${discountResult.finalPrice}"
                    HisabHistoryManager.addHistory("DISCOUNT", "ছাড় হিসাব", inputDesc, resultDesc)
                    onSavedToHistory("ছাড়ের হিসাব সংরক্ষিত হয়েছে!")
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ToolTeal),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("হিস্ট্রিতে সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun IncreaseSubSection(onSavedToHistory: (String) -> Unit) {
    var originalPriceStr by remember { mutableStateOf("1000") }
    var increasePercentStr by remember { mutableStateOf("15") }
    var flatIncreaseStr by remember { mutableStateOf("") }

    val origPrice = originalPriceStr.toDoubleOrNull() ?: 0.0
    val incPct = increasePercentStr.toDoubleOrNull() ?: 0.0
    val flatInc = flatIncreaseStr.toDoubleOrNull() ?: 0.0

    val increaseResult = remember(origPrice, incPct, flatInc) {
        if (origPrice > 0.0) {
            HisabCalculationEngine.calculateIncrease(origPrice, incPct, flatInc)
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
                    text = "দাম বৃদ্ধি হিসাব (Price Increase / Markup)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = originalPriceStr,
                    onValueChange = { originalPriceStr = it },
                    label = { Text("বর্তমান মূল্য (₹)") },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = ToolOrange) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = increasePercentStr,
                        onValueChange = { increasePercentStr = it },
                        label = { Text("বৃদ্ধি শতাংশ (%)") },
                        trailingIcon = { Text("%", fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = flatIncreaseStr,
                        onValueChange = { flatIncreaseStr = it },
                        label = { Text("সরাসরি বৃদ্ধি (₹)") },
                        placeholder = { Text("ঐচ্ছিক") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (increaseResult != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ToolOrange.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("মোট বৃদ্ধি:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "+ " + HisabCalculationEngine.formatCurrency(increaseResult.increaseAmount),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = CrimsonPrimary
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("বৃদ্ধির পর নতুন মূল্য:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = HisabCalculationEngine.formatCurrency(increaseResult.finalPrice),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = ToolOrange
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val inputDesc = "মূল্য: ₹$origPrice, বৃদ্ধি: $incPct%"
                    val resultDesc = "বৃদ্ধি: ₹${increaseResult.increaseAmount} | নতুন মূল্য: ₹${increaseResult.finalPrice}"
                    HisabHistoryManager.addHistory("INCREASE", "দাম বৃদ্ধি", inputDesc, resultDesc)
                    onSavedToHistory("দাম বৃদ্ধির হিসাব সংরক্ষিত হয়েছে!")
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ToolOrange),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("হিস্ট্রিতে সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GstSubSection(onSavedToHistory: (String) -> Unit) {
    var amountStr by remember { mutableStateOf("1000") }
    var selectedRate by remember { mutableStateOf(18.0) }
    var isIncluded by remember { mutableStateOf(false) }

    val amount = amountStr.toDoubleOrNull() ?: 0.0

    val gstResult = remember(amount, selectedRate, isIncluded) {
        if (amount > 0.0) {
            HisabCalculationEngine.calculateGst(amount, selectedRate, isIncluded)
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
                    text = "GST ক্যালকুলেটর (জিএসটি)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("টাকার পরিমাণ (₹)") },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = ToolPurple) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // GST Mode Switch (Included / Excluded)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isIncluded) "GST মূল্যের সাথে যুক্ত (Included)" else "GST ছাড়া মূল্য (Excluded)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Switch(checked = isIncluded, onCheckedChange = { isIncluded = it })
                }

                // Standard Rates (3%, 5%, 12%, 18%, 28%)
                Text("GST রেট নির্বাচন করুন:", style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(3.0, 5.0, 12.0, 18.0, 28.0).forEach { rate ->
                        val isSel = selectedRate == rate
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedRate = rate },
                            label = { Text("${rate.toInt()}%", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (gstResult != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ToolPurple.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("মূল পরিমাণ (Base):", style = MaterialTheme.typography.bodyMedium)
                        Text(HisabCalculationEngine.formatCurrency(gstResult.baseAmount), fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("CGST (${selectedRate / 2}%):", style = MaterialTheme.typography.bodyMedium)
                        Text(HisabCalculationEngine.formatCurrency(gstResult.cgstAmount), fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SGST (${selectedRate / 2}%):", style = MaterialTheme.typography.bodyMedium)
                        Text(HisabCalculationEngine.formatCurrency(gstResult.sgstAmount), fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("মোট GST (${selectedRate}%):", style = MaterialTheme.typography.bodyMedium)
                        Text(HisabCalculationEngine.formatCurrency(gstResult.totalGstAmount), fontWeight = FontWeight.Bold, color = ToolPurple)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("সর্বমোট মূল্য (Total):", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(
                            HisabCalculationEngine.formatCurrency(gstResult.totalAmount),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = ToolPurple
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val inputDesc = "টাকা: ₹$amount (${if (isIncluded) "Included" else "Excluded"}), রেট: ${selectedRate}%"
                    val resultDesc = "GST: ₹${HisabCalculationEngine.formatNumber(gstResult.totalGstAmount)} | সর্বমোট: ₹${HisabCalculationEngine.formatNumber(gstResult.totalAmount)}"
                    HisabHistoryManager.addHistory("GST", "GST হিসাব", inputDesc, resultDesc)
                    onSavedToHistory("GST হিসাব সংরক্ষিত হয়েছে!")
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ToolPurple),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("হিস্ট্রিতে সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProfitLossSubSection(onSavedToHistory: (String) -> Unit) {
    var costPriceStr by remember { mutableStateOf("800") }
    var sellingPriceStr by remember { mutableStateOf("1000") }

    val cp = costPriceStr.toDoubleOrNull() ?: 0.0
    val sp = sellingPriceStr.toDoubleOrNull() ?: 0.0

    val plResult = remember(cp, sp) {
        if (cp > 0.0 && sp > 0.0) {
            HisabCalculationEngine.calculateProfitLoss(cp, sp)
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
                    label = { Text("ক্রয়মূল্য (Cost Price ₹)") },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sellingPriceStr,
                    onValueChange = { sellingPriceStr = it },
                    label = { Text("বিক্রয়মূল্য (Selling Price ₹)") },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (plResult != null) {
            val cardColor = if (plResult.isProfit) ToolGreen.copy(alpha = 0.15f) else CrimsonPrimary.copy(alpha = 0.15f)
            val accentColor = if (plResult.isProfit) ToolGreen else CrimsonPrimary
            val title = if (plResult.isProfit) "লাভ (Profit)" else "ক্ষতি (Loss)"

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = HisabCalculationEngine.formatCurrency(plResult.difference),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = accentColor
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("শতাংশ (%):", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = String.format(java.util.Locale.US, "%.2f%%", plResult.percentage),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = accentColor
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val inputDesc = "ক্রয়: ₹$cp | বিক্রয়: ₹$sp"
                    val resultDesc = "$title: ₹${HisabCalculationEngine.formatNumber(plResult.difference)} (${String.format(java.util.Locale.US, "%.1f%%", plResult.percentage)})"
                    HisabHistoryManager.addHistory("PROFIT_LOSS", "লাভ-ক্ষতি হিসাব", inputDesc, resultDesc)
                    onSavedToHistory("লাভ-ক্ষতির হিসাব সংরক্ষিত হয়েছে!")
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("হিস্ট্রিতে সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
            }
        }
    }
}
