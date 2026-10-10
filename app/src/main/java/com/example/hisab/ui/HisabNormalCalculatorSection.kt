package com.example.hisab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hisab.HisabCalculationEngine
import com.example.hisab.HisabHistoryManager
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ToolBlue
import com.example.ui.theme.ToolGreen
import com.example.ui.theme.ToolOrange

@Composable
fun HisabNormalCalculatorSection(
    onSavedToHistory: (String) -> Unit
) {
    var expression by remember { mutableStateOf("") }
    var evaluatedResult by remember { mutableStateOf<Double?>(null) }
    var hasCalculated by remember { mutableStateOf(false) }

    fun onDigit(d: String) {
        if (hasCalculated) {
            expression = d
            hasCalculated = false
        } else {
            expression += d
        }
        evaluatedResult = HisabCalculationEngine.evaluateExpression(expression)
    }

    fun onOperator(op: String) {
        hasCalculated = false
        if (expression.isNotEmpty()) {
            val lastChar = expression.last()
            if (lastChar in listOf('+', '−', '×', '÷', '%')) {
                expression = expression.dropLast(1) + op
            } else {
                expression += op
            }
        } else if (op == "−") {
            expression = "−"
        }
    }

    fun onEquals() {
        val res = HisabCalculationEngine.evaluateExpression(expression)
        if (res != null && !res.isNaN()) {
            evaluatedResult = res
            val formatted = HisabCalculationEngine.formatNumber(res)
            HisabHistoryManager.addHistory(
                type = "CALC",
                titleBn = "সাধারণ ক্যালকুলেটর",
                inputDescBn = expression,
                resultBn = "= $formatted"
            )
            expression = formatted
            hasCalculated = true
        }
    }

    fun onClear() {
        expression = ""
        evaluatedResult = null
        hasCalculated = false
    }

    fun onDelete() {
        if (expression.isNotEmpty()) {
            expression = expression.dropLast(1)
            evaluatedResult = HisabCalculationEngine.evaluateExpression(expression)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // DISPLAY SCREEN
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("calculator_display")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (expression.isEmpty()) "0" else expression,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    modifier = Modifier.testTag("text_calc_expression")
                )

                if (!hasCalculated && evaluatedResult != null && !evaluatedResult!!.isNaN()) {
                    Text(
                        text = "= ${HisabCalculationEngine.formatNumber(evaluatedResult!!)}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = ToolGreen,
                        textAlign = TextAlign.End,
                        modifier = Modifier.testTag("text_calc_preview_result")
                    )
                } else if (hasCalculated && evaluatedResult != null) {
                    Text(
                        text = "ফলাফল সংরক্ষিত",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // QUICK PRESET TEST CASES (100+200, 100*5, 1000/4, 500-125)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val quickTests = listOf("100+200", "100×5", "1000÷4", "500−125")
            quickTests.forEach { testExpr ->
                OutlinedButton(
                    onClick = {
                        expression = testExpr
                        hasCalculated = false
                        onEquals()
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text(testExpr, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // KEYPAD GRID
        val buttonModifier = Modifier
            .weight(1f)
            .height(58.dp)

        // Row 1: AC, DEL, %, ÷
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { onClear() },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary.copy(alpha = 0.2f), contentColor = CrimsonPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = buttonModifier.testTag("btn_calc_ac")
            ) {
                Text("AC", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }

            Button(
                onClick = { onDelete() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = buttonModifier.testTag("btn_calc_del")
            ) {
                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Delete", modifier = Modifier.size(20.dp))
            }

            Button(
                onClick = { onOperator("%") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = ToolBlue),
                shape = RoundedCornerShape(14.dp),
                modifier = buttonModifier.testTag("btn_calc_percent")
            ) {
                Text("%", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }

            Button(
                onClick = { onOperator("÷") },
                colors = ButtonDefaults.buttonColors(containerColor = ToolBlue.copy(alpha = 0.2f), contentColor = ToolBlue),
                shape = RoundedCornerShape(14.dp),
                modifier = buttonModifier.testTag("btn_calc_divide")
            ) {
                Text("÷", fontWeight = FontWeight.Bold, fontSize = 24.sp)
            }
        }

        // Row 2: 7, 8, 9, ×
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf("7", "8", "9").forEach { digit ->
                FilledTonalButton(
                    onClick = { onDigit(digit) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = buttonModifier.testTag("btn_calc_$digit")
                ) {
                    Text(digit, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }

            Button(
                onClick = { onOperator("×") },
                colors = ButtonDefaults.buttonColors(containerColor = ToolBlue.copy(alpha = 0.2f), contentColor = ToolBlue),
                shape = RoundedCornerShape(14.dp),
                modifier = buttonModifier.testTag("btn_calc_multiply")
            ) {
                Text("×", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
        }

        // Row 3: 4, 5, 6, −
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf("4", "5", "6").forEach { digit ->
                FilledTonalButton(
                    onClick = { onDigit(digit) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = buttonModifier.testTag("btn_calc_$digit")
                ) {
                    Text(digit, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }

            Button(
                onClick = { onOperator("−") },
                colors = ButtonDefaults.buttonColors(containerColor = ToolBlue.copy(alpha = 0.2f), contentColor = ToolBlue),
                shape = RoundedCornerShape(14.dp),
                modifier = buttonModifier.testTag("btn_calc_minus")
            ) {
                Text("−", fontWeight = FontWeight.Bold, fontSize = 24.sp)
            }
        }

        // Row 4: 1, 2, 3, +
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf("1", "2", "3").forEach { digit ->
                FilledTonalButton(
                    onClick = { onDigit(digit) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = buttonModifier.testTag("btn_calc_$digit")
                ) {
                    Text(digit, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }

            Button(
                onClick = { onOperator("+") },
                colors = ButtonDefaults.buttonColors(containerColor = ToolBlue.copy(alpha = 0.2f), contentColor = ToolBlue),
                shape = RoundedCornerShape(14.dp),
                modifier = buttonModifier.testTag("btn_calc_plus")
            ) {
                Text("+", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
        }

        // Row 5: 0, 00, ., =
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                onClick = { onDigit("0") },
                shape = RoundedCornerShape(14.dp),
                modifier = buttonModifier.testTag("btn_calc_0")
            ) {
                Text("0", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }

            FilledTonalButton(
                onClick = { onDigit("00") },
                shape = RoundedCornerShape(14.dp),
                modifier = buttonModifier.testTag("btn_calc_00")
            ) {
                Text("00", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            FilledTonalButton(
                onClick = {
                    if (!expression.endsWith(".")) {
                        onDigit(".")
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = buttonModifier.testTag("btn_calc_dot")
            ) {
                Text(".", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
            }

            Button(
                onClick = { onEquals() },
                colors = ButtonDefaults.buttonColors(containerColor = ToolGreen),
                shape = RoundedCornerShape(14.dp),
                modifier = buttonModifier.testTag("btn_calc_equals")
            ) {
                Text("=", fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = Color.White)
            }
        }
    }
}
