package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.ai.GeminiAiHelper
import com.example.ui.dialogs.FolderCustomizerDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PdfMasterViewModel
import com.example.ui.NavigationTab
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ToolBlue
import com.example.ui.theme.ToolGreen
import com.example.ui.theme.ToolIndigo
import com.example.ui.theme.ToolOrange
import com.example.ui.theme.ToolPurple
import com.example.ui.AppScreen

@Composable
fun SettingsScreen(
    viewModel: PdfMasterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val language by viewModel.appLanguage.collectAsState()
    val darkThemeMode by viewModel.darkThemeMode.collectAsState()
    val allFiles by viewModel.allFiles.collectAsState()

    val totalSizeKb = allFiles.sumOf { it.fileSize } / 1024
    val saveFolderType by viewModel.saveFolderType.collectAsState()
    val customFolderName by viewModel.customFolderName.collectAsState()
    val currentSavePath by viewModel.saveLocationDisplayPath.collectAsState()
    val geminiApiKey by viewModel.geminiApiKey.collectAsState()
    var apiKeyInput by remember(geminiApiKey) { mutableStateOf(geminiApiKey) }
    var isKeyVisible by remember { mutableStateOf(false) }
    var showFolderCustomizer by remember { mutableStateOf(false) }
    var autoDeleteTemp by remember { mutableStateOf(true) }

AppScreen(modifier = modifier, isScrollable = false) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen_content"),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.setTab(NavigationTab.HOME) },
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Home",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (language == "bn") "অ্যাপ সেটিংস" else "Settings",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (language == "bn") "ভাষা, থিম ও স্টোরেজ কনফিগারেশন" else "Preferences, Theme & Storage",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Section: Language Selection
        item {
            SettingsSectionHeader(
                title = if (language == "bn") "ভাষা (Language)" else "Language",
                icon = Icons.Default.Language,
                color = ToolIndigo
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    LanguageOptionRow("English (US)", "en", language) { viewModel.setLanguage("en") }
                    LanguageOptionRow("বাংলা (Bengali)", "bn", language) { viewModel.setLanguage("bn") }
                    LanguageOptionRow("हिन्दी (Hindi)", "hi", language) { viewModel.setLanguage("hi") }
                }
            }
        }

        // Section: Dark Mode / Theme
        item {
            Spacer(modifier = Modifier.height(14.dp))
            SettingsSectionHeader(
                title = if (language == "bn") "ডিসপ্লে থিম (Dark Mode)" else "Theme & Display",
                icon = Icons.Default.DarkMode,
                color = ToolPurple
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    ThemeOptionRow(
                        title = if (language == "bn") "সিস্টেম ডিফল্ট" else "System Default",
                        isSelected = darkThemeMode == null,
                        onClick = { viewModel.setDarkThemeMode(null) }
                    )
                    ThemeOptionRow(
                        title = if (language == "bn") "লাইট মোড" else "Light Mode",
                        isSelected = darkThemeMode == false,
                        onClick = { viewModel.setDarkThemeMode(false) }
                    )
                    ThemeOptionRow(
                        title = if (language == "bn") "ডার্ক মোড" else "Dark Mode",
                        isSelected = darkThemeMode == true,
                        onClick = { viewModel.setDarkThemeMode(true) }
                    )
                }
            }
        }

        // Section: Storage & Save Folder
        item {
            Spacer(modifier = Modifier.height(14.dp))
            SettingsSectionHeader(
                title = if (language == "bn") "স্টোরেজ ও সেভ ফোল্ডার" else "Storage & Save Folder",
                icon = Icons.Default.Folder,
                color = ToolOrange
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFolderCustomizer = true }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = ToolOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == "bn") "সেভ লোকেশন" else "Save Location",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = currentSavePath,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Section: Google Backup
        item {
            Spacer(modifier = Modifier.height(14.dp))
            SettingsSectionHeader(
                title = if (language == "bn") "গুগল ব্যাকআপ" else "Google Backup",
                icon = Icons.Default.Security,
                color = ToolGreen
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val isSignedIn = viewModel.isUserSignedIn(context)
                    Button(
                        onClick = {
                            if (isSignedIn) {
                                viewModel.signOut(context)
                                viewModel.setNotification("Signed out of Google")
                            } else {
                                // For now, simple sign-in trigger.
                                // In a real app, this would trigger the actual GoogleSignIn flow
                                viewModel.setNotification("Sign-in logic needs to be triggered via Activity result launcher")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isSignedIn) "Sign Out" else "Sign In with Google")
                    }
                }
            }
        }

        // Section: About App
        item {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CrimsonPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "PDF Master",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "PDF Master – All PDF Tools",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Version 1.0.0 • Offline First & AI Powered",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    }

    if (showFolderCustomizer) {
        FolderCustomizerDialog(
            viewModel = viewModel,
            currentType = saveFolderType,
            currentName = customFolderName,
            onDismiss = { showFolderCustomizer = false }
        )
    }
}

@Composable
fun SettingsSectionHeader(
    title: String,
    icon: ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun LanguageOptionRow(
    title: String,
    code: String,
    currentCode: String,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
        RadioButton(
            selected = code == currentCode,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = CrimsonPrimary)
        )
    }
}

@Composable
fun ThemeOptionRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = CrimsonPrimary)
        )
    }
}

@Composable
fun QualityOptionRow(
    title: String,
    desc: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(text = desc, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = CrimsonPrimary)
        )
    }
}
