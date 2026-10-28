package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrimsonPrimary

enum class EmojiCategory(val label: String, val icon: String) {
    SMILEYS("Smileys", "😀"),
    GESTURES("Gestures", "👍"),
    SYMBOLS("Symbols", "✅"),
    OBJECTS("Tools", "📌"),
    FLAGS("Flags", "🚩")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfEditorEmojiPickerSheet(
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(EmojiCategory.SMILEYS) }

    val smileysList = remember {
        listOf(
            "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇",
            "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚",
            "😋", "😛", "😜", "🤪", "😝", "🤑", "🤗", "🤭", "🤫", "🤔",
            "🤐", "🤨", "😐", "😑", "😶", "😏", "😒", "🙄", "😬", "🤥",
            "😌", "😔", "😪", "🤤", "😴", "😷", "🤒", "🤕", "🤢", "🤮",
            "🤧", "🥵", "🥶", "🥴", "😵", "🤯", "🤠", "🥳", "🥸", "😎",
            "🤓", "🧐", "😕", "😟", "🙁", "😮", "😯", "😲", "😳", "🥺",
            "😦", "😧", "😨", "😰", "😥", "😢", "😭", "😱", "😖", "😣",
            "😞", "😓", "😩", "😫", "🥱", "😤", "😡", "😠", "🤬", "😈",
            "👿", "💀", "☠️", "💩", "🤡", "👹", "👺", "👻", "👽", "🤖"
        )
    }

    val gesturesList = remember {
        listOf(
            "👍", "👎", "👏", "🙌", "👐", "🤲", "🤝", "🙏", "✍️", "💪",
            "👋", "🤚", "🖐️", "✋", "🖖", "👌", "🤌", "🤏", "✌️", "🤞",
            "🤟", "🤘", "🤙", "👈", "👉", "👆", "👇", "☝️", "✊", "👊",
            "🤛", "🤜", "💅", "🤳", "👂", "👃", "🧠", "👀", "👁️", "👅",
            "👄", "👶", "👧", "🧒", "👦", "👩", "🧑", "👨", "👵", "🧓",
            "👴", "👮", "🕵️", "💂", "👷", "🤴", "👸", "🦸", "🦹", "🧙"
        )
    }

    val symbolsList = remember {
        listOf(
            "✅", "❌", "⭕", "🛑", "⛔", "⚠️", "❗", "❓", "‼️", "⁉️",
            "💯", "✨", "⭐", "🌟", "💫", "🔥", "💥", "❤️", "🧡", "💛",
            "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔", "❣️", "💕", "💞",
            "💓", "💗", "💖", "💘", "💝", "🎯", "🏆", "🥇", "🥈", "🥉",
            "🆗", "🆙", "🆒", "🆕", "🆓", "🔟", "🔢", "➡️", "⬅️", "⬆️",
            "⬇️", "↗️", "↘️", "↙️", "↖️", "🔄", "🔁", "🔂", "▶️", "⏸️",
            "0️⃣", "1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣"
        )
    }

    val objectsList = remember {
        listOf(
            "📌", "📍", "📎", "🖇️", "📏", "📐", "✂️", "🖊️", "🖋️", "✒️",
            "🖌️", "🖍️", "📝", "✏️", "📁", "📂", "🗂️", "📅", "📆", "🗒️",
            "🗓️", "📇", "📈", "📉", "📊", "📋", "📌", "📍", "🔒", "🔓",
            "🔏", "🔐", "🔑", "🗝️", "🔨", "🪓", "🛠️", "⚙️", "💡", "🔦",
            "💼", "📦", "🏷️", "✉️", "📧", "📨", "📩", "📤", "📥", "📱",
            "💻", "🖥️", "🖨️", "📷", "📸", "📹", "🔍", "🔎", "🔔", "🔕"
        )
    }

    val flagsList = remember {
        listOf(
            "🚩", "🏁", "🎌", "🏴", "🏳️", "🏳️‍🌈", "🏳️‍⚧️", "🇧🇩", "🇮🇳", "🇺🇸",
            "🇬🇧", "🇨🇦", "🇦🇺", "🇩🇪", "🇫🇷", "🇯🇵", "🇨🇳", "🇧🇷", "🇸🇦", "🇦🇪",
            "🇵🇰", "🇳🇵", "🇧🇹", "🇱🇰", "🇲🇻", "🇲🇾", "🇸🇬", "🇮🇩", "🇹🇭", "🇻🇳",
            "🇰🇷", "🇷🇺", "🇮🇹", "🇪🇸", "🇹🇷", "🇪🇬", "🇿🇦", "🇳🇬", "🇰🇪", "🇲🇽"
        )
    }

    val currentList = when (selectedCategory) {
        EmojiCategory.SMILEYS -> smileysList
        EmojiCategory.GESTURES -> gesturesList
        EmojiCategory.SYMBOLS -> symbolsList
        EmojiCategory.OBJECTS -> objectsList
        EmojiCategory.FLAGS -> flagsList
    }

    val filteredList = if (searchQuery.isBlank()) {
        currentList
    } else {
        // Simple search filter across all categories
        (smileysList + gesturesList + symbolsList + objectsList + flagsList).distinct()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        contentColor = Color.White,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color(0xFF64748B))
        },
        modifier = Modifier.testTag("pdf_editor_emoji_picker_modal")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header: Title and Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Insert Emoji Sticker",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp).testTag("btn_close_emoji_picker")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search emojis...", color = Color(0xFF64748B), fontSize = 13.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CrimsonPrimary,
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedContainerColor = Color(0xFF0F172A),
                    unfocusedContainerColor = Color(0xFF0F172A),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("emoji_search_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category Tabs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EmojiCategory.values().forEach { cat ->
                    val isSelected = selectedCategory == cat && searchQuery.isBlank()
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedCategory = cat
                            searchQuery = ""
                        },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(cat.icon, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(cat.label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CrimsonPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF334155),
                            labelColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.height(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Emoji Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 44.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredList) { emoji ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A))
                            .clickable {
                                onEmojiSelected(emoji)
                                onDismiss()
                            }
                            .testTag("emoji_item_$emoji")
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 24.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
