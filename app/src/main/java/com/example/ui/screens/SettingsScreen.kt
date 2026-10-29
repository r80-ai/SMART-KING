package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.RestorePage
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import android.app.Activity
import com.example.updater.AppUpdateManager
import com.example.updater.UpdateConfig
import com.example.updater.UpdateState
import com.example.updater.ui.OptionalUpdateDialog
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.amardokan.data.AmarDokanBackupManager
import com.example.amardokan.data.DatabaseBackup
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.CircularProgressIndicator
import com.example.ui.AppScreen

@Composable
fun SettingsScreen(
    viewModel: PdfMasterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val language by viewModel.appLanguage.collectAsState()
    val isBn = language == "bn"
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

    val isBackupRunning by AmarDokanBackupManager.isBackupRunning.collectAsState()
    val lastBackupTime by AmarDokanBackupManager.lastBackupTime.collectAsState()
    val lastBackupPath by AmarDokanBackupManager.lastBackupPath.collectAsState()

    var showRestoreDialog by remember { mutableStateOf(false) }
    var localBackupFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var pendingRestoreData by remember { mutableStateOf<Pair<String, DatabaseBackup>?>(null) }
    var isRestoring by remember { mutableStateOf(false) }

    val updateManager = remember { AppUpdateManager.getInstance(context) }
    val updateState by updateManager.updateState.collectAsState()
    val currentManifest by updateManager.currentManifest.collectAsState()
    var showConfigDialog by remember { mutableStateOf(false) }
    var configUrlInput by remember(updateManager.config.manifestUrl) { mutableStateOf(updateManager.config.manifestUrl) }
    var configOwnerInput by remember(updateManager.config.githubOwner) { mutableStateOf(updateManager.config.githubOwner) }
    var configRepoInput by remember(updateManager.config.githubRepo) { mutableStateOf(updateManager.config.githubRepo) }
    var configAssetInput by remember(updateManager.config.preferredApkAssetName) { mutableStateOf(updateManager.config.preferredApkAssetName) }
    var showOptionalUpdateModal by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val inspectResult = AmarDokanBackupManager.inspectBackupUri(context, uri)
                inspectResult.onSuccess { pair ->
                    pendingRestoreData = pair
                }.onFailure { err ->
                    viewModel.setNotification("❌ ফাইল পড়তে সমস্যা: ${err.message}")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        AmarDokanBackupManager.init(context)
    }


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

        // Section: Amar Dokan Local Storage Backup
        item {
            Spacer(modifier = Modifier.height(14.dp))
            SettingsSectionHeader(
                title = if (language == "bn") "লোকাল স্টোরেজ ব্যাকআপ (আমার দোকান)" else "Amar Dokan Local Backup",
                icon = Icons.Default.Storage,
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
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isBn = language == "bn"

                    Text(
                        text = if (isBn) "লোকাল স্টোরেজ ব্যাকআপ (Local Storage Backup)" else "Local Storage Backup",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = if (isBn)
                            "আমার দোকান-এর সমস্ত খদ্দের, বিল, রেট ও লেনদেনের ডাটা ফোনের মেমরিতে অফলাইনে সুরক্ষিত রাখুন।"
                        else
                            "Safely back up all Amar Dokan records to your local phone storage without internet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Target directory info
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (isBn) "সংরক্ষণ লোকেশন (Location):" else "Backup Directory:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = AmarDokanBackupManager.TARGET_FOLDER_PATH,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Last backup & status
                    val lastBackupFormatted = if (lastBackupTime > 0) {
                        SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(lastBackupTime))
                    } else {
                        if (isBn) "কখনো ব্যাকআপ নেওয়া হয়নি" else "Never"
                    }
                    Text(
                        text = (if (isBn) "সর্বশেষ ব্যাকআপ: " else "Last Backup: ") + lastBackupFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!lastBackupPath.isNullOrBlank()) {
                        Text(
                            text = (if (isBn) "ফাইল: " else "File: ") + File(lastBackupPath!!).name,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            ),
                            color = ToolGreen
                        )
                    }

                    // Button 1: Backup Now
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val res = AmarDokanBackupManager.createLocalBackup(context)
                                if (res.isSuccess) {
                                    viewModel.setNotification(
                                        if (isBn) "✅ ব্যাকআপ তৈরি সম্পন্ন হয়েছে" else "✅ Local backup created successfully"
                                    )
                                }
                            }
                        },
                        enabled = !isBackupRunning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_settings_amar_dokan_backup"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isBackupRunning) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isBn) "ব্যাকআপ হচ্ছে..." else "Backing up...")
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isBn) "এখনই ব্যাকআপ নিন (Backup Now)" else "Backup Now")
                        }
                    }

                    // Button 2: Restore from Storage
                    OutlinedButton(
                        onClick = {
                            localBackupFiles = AmarDokanBackupManager.listLocalBackupFiles()
                            showRestoreDialog = true
                        },
                        enabled = !isBackupRunning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_settings_amar_dokan_restore"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.RestorePage, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isBn) "ব্যাকআপ থেকে রিস্টোর (Restore from Storage)" else "Restore from Storage")
                    }
                }
            }
        }


        // Section: About App & Remote Update System
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
                            contentDescription = "SMART KING",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SMART KING – All PDF Tools",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Version ${updateManager.installedVersionName} (Build ${updateManager.installedVersionCode}) • Offline First",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Update Status Section
                    when (val state = updateState) {
                        is UpdateState.Checking -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = CrimsonPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBn) "রিমোট আপডেট চেক করা হচ্ছে..." else "Checking GitHub Releases...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        is UpdateState.UpToDate -> {
                            Surface(
                                color = ToolGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = ToolGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isBn) "অ্যাপটি সর্বশেষ সংস্করণে আপডেট আছে" else "Application is up to date",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = ToolGreen
                                    )
                                }
                            }
                        }
                        is UpdateState.Available -> {
                            Surface(
                                color = ToolOrange.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = ToolOrange, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = (if (isBn) "নতুন সংস্করণ উপলভ্য: v" else "Update Available: v") + state.manifest.latestVersionName,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = ToolOrange
                                    )
                                }
                            }
                        }
                        is UpdateState.Error -> {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.error,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                        else -> {}
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                updateManager.checkForUpdates(isManual = true)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_check_for_updates"),
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBn) "আপডেট চেক" else "Check Updates",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { showConfigDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = "Config", modifier = Modifier.size(16.dp))
                        }

                        OutlinedButton(
                            onClick = {
                                (context as? Activity)?.let { act ->
                                    updateManager.fetchAndDownloadLatestReleaseApk(act)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = "Direct Download APK", modifier = Modifier.size(16.dp))
                        }
                    }
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

    // Confirmation dialog before restoring database
    if (pendingRestoreData != null) {
        val (fileName, backup) = pendingRestoreData!!
        val isBn = language == "bn"
        AlertDialog(
            onDismissRequest = { if (!isRestoring) pendingRestoreData = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = ToolOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isBn) "ব্যাকআপ রিস্টোর নিশ্চিত করুন" else "Confirm Restore", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = (if (isBn) "ফাইল: " else "File: ") + fileName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    HorizontalDivider()
                    Text(if (isBn) "রেকর্ড সংখ্যা:" else "Included Records:", fontWeight = FontWeight.Medium)
                    Text("• " + (if (isBn) "খদ্দের তালিকা: ${backup.customers.size} জন" else "Customers: ${backup.customers.size}"))
                    Text("• " + (if (isBn) "তৈরি বিল: ${backup.bills.size} টি" else "Bills: ${backup.bills.size}"))
                    Text("• " + (if (isBn) "গৃহীত পেমেন্ট: ${backup.payments.size} টি" else "Payments: ${backup.payments.size}"))
                    Text("• " + (if (isBn) "আইটেম ও রেট: ${backup.services.size} টি" else "Items/Services: ${backup.services.size}"))
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isBn)
                                "⚠️ রিস্টোর করলে বর্তমান ডাটাবেজ এই ব্যাকআপ ফাইলের তথ্য দিয়ে প্রতিস্থাপিত হবে।"
                            else
                                "⚠️ Restoring will overwrite existing Amar Dokan database records.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isRestoring = true
                        coroutineScope.launch {
                            val res = AmarDokanBackupManager.restoreDatabase(context, backup)
                            isRestoring = false
                            pendingRestoreData = null
                            showRestoreDialog = false
                            if (res.isSuccess) {
                                viewModel.setNotification(
                                    if (isBn) "✅ রিস্টোর সফলভাবে সম্পন্ন হয়েছে" else "✅ Data restored successfully"
                                )
                            }
                        }
                    },
                    enabled = !isRestoring,
                    colors = ButtonDefaults.buttonColors(containerColor = ToolGreen)
                ) {
                    if (isRestoring) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isBn) "রিস্টোর হচ্ছে..." else "Restoring...")
                    } else {
                        Text(if (isBn) "হ্যাঁ, রিস্টোর করুন" else "Yes, Restore")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { pendingRestoreData = null },
                    enabled = !isRestoring
                ) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Modal dialog to select local backup file
    if (showRestoreDialog) {
        val isBn = language == "bn"
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RestorePage, contentDescription = null, tint = ToolGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isBn) "ব্যাকআপ ফাইল থেকে রিস্টোর" else "Restore from Local Backup", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isBn)
                            "📁 সংরক্ষিত ফাইল (${localBackupFiles.size}টি):"
                        else
                            "📁 Local Backups (${localBackupFiles.size}):",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )

                    if (localBackupFiles.isEmpty()) {
                        Text(
                            text = if (isBn)
                                "লোকাল ফোল্ডারে কোনো ব্যাকআপ পাওয়া যায়নি। আপনি 'এখনই ব্যাকআপ নিন' বাটনে চাপ দিয়ে ব্যাকআপ ফাইল তৈরি করতে পারেন।"
                            else
                                "No backup files found in /storage/emulated/0/Smart King/Amar Dokan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        localBackupFiles.forEach { file ->
                            val dateStr = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(file.lastModified()))
                            val sizeKb = (file.length() / 1024).coerceAtLeast(1)
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(file.name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
                                        Text("$dateStr • ${sizeKb} KB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                val inspectRes = AmarDokanBackupManager.inspectBackupFile(file)
                                                inspectRes.onSuccess { backup ->
                                                    pendingRestoreData = Pair(file.name, backup)
                                                }.onFailure { err ->
                                                    viewModel.setNotification("❌ ত্রুটি: ${err.message}")
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ToolGreen),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(if (isBn) "রিস্টোর" else "Restore", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // Pick file via system file picker
                    OutlinedButton(
                        onClick = {
                            filePickerLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isBn) "অন্য ফোল্ডার থেকে ফাইল বাছুন..." else "Choose file from device...", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text(if (isBn) "বন্ধ করুন" else "Close")
                }
            }
        )
    }

    // Modal dialog to view and configure Remote Update Manifest Endpoint & GitHub Repository
    if (showConfigDialog) {
        val isBn = language == "bn"
        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = CrimsonPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isBn) "রিমোট আপডেট ও GitHub কনফিগ" else "Remote Update & GitHub Config", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isBn)
                            "আপডেট ম্যানিফেস্ট JSON URL (রিমোট সার্ভার বা GitHub raw URL):"
                        else
                            "Update manifest JSON URL (Remote server or GitHub raw URL):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = configUrlInput,
                        onValueChange = { configUrlInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        label = { Text("Manifest Endpoint URL") }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        text = if (isBn)
                            "GitHub Releases API সেটিংস (সরাসরি APK ডাউনলোডের জন্য):"
                        else
                            "GitHub Releases API settings (for direct in-app APK download):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = configOwnerInput,
                            onValueChange = { configOwnerInput = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            label = { Text("Owner (e.g. r80-ai)") }
                        )

                        OutlinedTextField(
                            value = configRepoInput,
                            onValueChange = { configRepoInput = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            label = { Text("Repo (SMART-KING)") }
                        )
                    }

                    OutlinedTextField(
                        value = configAssetInput,
                        onValueChange = { configAssetInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        label = { Text("Preferred APK Asset Name (e.g. app-debug.apk)") }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                updateManager.config.resetToDefault()
                                configUrlInput = updateManager.config.manifestUrl
                                configOwnerInput = updateManager.config.githubOwner
                                configRepoInput = updateManager.config.githubRepo
                                configAssetInput = updateManager.config.preferredApkAssetName
                                viewModel.setNotification(if (isBn) "সব ডিফল্ট মান রিসেট হয়েছে" else "Reset to canonical defaults")
                            }
                        ) {
                            Text(if (isBn) "ডিফল্ট রিসেট" else "Reset Defaults", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedUrl = configUrlInput.trim()
                        val trimmedOwner = configOwnerInput.trim()
                        val trimmedRepo = configRepoInput.trim()
                        val trimmedAsset = configAssetInput.trim()

                        if (trimmedUrl.isNotBlank()) updateManager.config.manifestUrl = trimmedUrl
                        if (trimmedOwner.isNotBlank()) updateManager.config.githubOwner = trimmedOwner
                        if (trimmedRepo.isNotBlank()) updateManager.config.githubRepo = trimmedRepo
                        if (trimmedAsset.isNotBlank()) updateManager.config.preferredApkAssetName = trimmedAsset

                        viewModel.setNotification(if (isBn) "✅ আপডেট কনফিগারেশন সংরক্ষিত হয়েছে" else "✅ Update configuration saved")
                        showConfigDialog = false
                        updateManager.checkForUpdates(isManual = true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) {
                    Text(if (isBn) "সংরক্ষণ ও চেক" else "Save & Check")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfigDialog = false }) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Modal dialog when an optional update is available
    if (showOptionalUpdateModal && currentManifest != null) {
        OptionalUpdateDialog(
            updateManager = updateManager,
            manifest = currentManifest!!,
            onDismiss = { showOptionalUpdateModal = false }
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
