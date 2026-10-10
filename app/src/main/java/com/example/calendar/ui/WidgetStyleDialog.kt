package com.example.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.calendar.widget.CalendarWidgetPreferences
import com.example.calendar.widget.WidgetStyle

@Composable
fun WidgetStyleDialog(
    isBangla: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedStyle by remember {
        mutableStateOf(CalendarWidgetPreferences.getWidgetStyle(context))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dialog_widget_style"),
        title = {
            Text(
                text = if (isBangla) "হোম স্ক্রিন উইজেট স্টাইল" else "Home Screen Widget Style",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isBangla) "আপনার অ্যান্ড্রয়েড হোম স্ক্রিনের উইজেটের জন্য পছন্দের থিম বেছে নিন:"
                    else "Choose visual appearance for your Android home-screen widgets:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                WidgetStyle.entries.forEach { style ->
                    val isSelected = style == selectedStyle
                    val (bgModifier, textColor) = when (style) {
                        WidgetStyle.GRADIENT -> Modifier.background(
                            Brush.linearGradient(listOf(Color(0xFF3B1E78), Color(0xFF1E3A8A)))
                        ) to Color.White
                        WidgetStyle.DARK -> Modifier.background(Color(0xFF0F172A)) to Color.White
                        WidgetStyle.LIGHT -> Modifier.background(Color(0xFFF1F5F9)) to Color(0xFF0F172A)
                        WidgetStyle.SIMPLE -> Modifier.background(Color(0xFF334155).copy(alpha = 0.8f)) to Color.White
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .then(bgModifier)
                            .then(
                                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                else Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            )
                            .clickable {
                                selectedStyle = style
                                CalendarWidgetPreferences.setWidgetStyle(context, style)
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isBangla) style.titleBn else style.titleEn,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = textColor
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = if (style == WidgetStyle.LIGHT) Color(0xFF0F172A) else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_close_widget_style")
            ) {
                Text(if (isBangla) "সম্পন্ন" else "Done")
            }
        }
    )
}
