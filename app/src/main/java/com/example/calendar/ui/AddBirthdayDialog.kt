package com.example.calendar.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.calendar.model.BanglaDate
import com.example.calendar.model.CalendarDate
import com.example.model.UserBirthday

@Composable
fun AddBirthdayDialog(
    initialDate: CalendarDate,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onSave: (UserBirthday) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var day by remember { mutableStateOf(initialDate.day.toString()) }
    var month by remember { mutableStateOf(initialDate.month.toString()) }
    var birthYear by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dialog_add_birthday"),
        title = {
            Text(
                text = if (isBangla) "জন্মদিন যোগ করুন" else "Add Birthday",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorText = null
                    },
                    label = { Text(if (isBangla) "নাম *" else "Name *") },
                    placeholder = { Text(if (isBangla) "যেমন: রহিম বা প্রিয়া" else "e.g. John Doe") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_birthday_name")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = day,
                        onValueChange = { if (it.length <= 2) day = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (isBangla) "দিন (১-৩১)" else "Day (1-31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_birthday_day")
                    )

                    OutlinedTextField(
                        value = month,
                        onValueChange = { if (it.length <= 2) month = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (isBangla) "মাস (১-১২)" else "Month (1-12)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_birthday_month")
                    )
                }

                OutlinedTextField(
                    value = birthYear,
                    onValueChange = { if (it.length <= 4) birthYear = it.filter { ch -> ch.isDigit() } },
                    label = { Text(if (isBangla) "জন্মসাল (ঐচ্ছিক)" else "Birth Year (Optional)") },
                    placeholder = { Text("e.g. 1995") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_birthday_year")
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (isBangla) "নোট / বিবরণ (ঐচ্ছিক)" else "Note / Description (Optional)") },
                    placeholder = { Text(if (isBangla) "যেমন: বন্ধুর শুভ জন্মদিন" else "e.g. Best friend's birthday") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_birthday_note")
                )

                errorText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = name.trim()
                    if (trimmedName.isEmpty()) {
                        errorText = if (isBangla) "অনুগ্রহ করে একটি নাম লিখুন" else "Please enter a name"
                        return@Button
                    }
                    val d = day.toIntOrNull() ?: 1
                    val m = month.toIntOrNull() ?: 1
                    if (d !in 1..31 || m !in 1..12) {
                        errorText = if (isBangla) "দিন বা মাস সঠিক নয়" else "Invalid day or month"
                        return@Button
                    }
                    val y = birthYear.toIntOrNull()

                    onSave(
                        UserBirthday(
                            name = trimmedName,
                            day = d,
                            month = m,
                            birthYear = y,
                            note = note.trim().ifEmpty { null }
                        )
                    )
                },
                modifier = Modifier.testTag("btn_save_birthday")
            ) {
                Text(if (isBangla) "সংরক্ষণ করুন" else "Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancel_birthday")
            ) {
                Text(if (isBangla) "বাতিল" else "Cancel")
            }
        }
    )
}
