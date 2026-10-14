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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.filled.CloudDownload
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.example.workers.BackupManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.app.Activity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.CircularProgressIndicator
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

    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    var googleAccount by remember {
        mutableStateOf<GoogleSignInAccount?>(GoogleSignIn.getLastSignedInAccount(context))
    }
    var signInError by remember { mutableStateOf<String?>(null) }

    val isBackupRunning by BackupManager.isBackupRunning.collectAsState()
    val lastBackupTime by BackupManager.lastBackupTime.collectAsState()
    val lastStatus by BackupManager.lastStatus.collectAsState()

    val driveScope = remember { Scope(DriveScopes.DRIVE_FILE) }

    val hasDriveScope = googleAccount != null && GoogleSignIn.hasPermissions(googleAccount, driveScope)
    val hasAllScopes = hasDriveScope

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(driveScope)
            .build()
    }

    // Refresh state on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val acc = GoogleSignIn.getLastSignedInAccount(context)
                googleAccount = acc
                if (acc != null && GoogleSignIn.hasPermissions(acc, driveScope)) {
                    if (signInError?.contains("permission") == true || signInError?.contains("অনুমতি") == true) {
                        signInError = null
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (data == null) {
            signInError = "No response data from Google Sign-In"
            return@rememberLauncherForActivityResult
        }

        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            googleAccount = account
            com.example.util.UserManager.onAccountChanged(context, account)
            com.example.workers.BackupManager.onUserAccountChanged(context, account)
            val hasDrive = GoogleSignIn.hasPermissions(account, driveScope)
            if (!hasDrive) {
                signInError = if (language == "bn") "⚠️ ড্রাইভ অনুমতি প্রয়োজন। 'অনুমতি দিন' বাটনে ট্যাপ করুন।" else "⚠️ Drive permissions needed. Please tap 'Grant Permissions'."
            } else {
                signInError = null
                viewModel.setNotification("✅ Connected Google Account: ${account.email}")
            }
        } catch (e: ApiException) {
            val explanation = when (e.statusCode) {
                10 -> "DEVELOPER_ERROR (Code 10): Android OAuth Client ID not found or SHA-1/Package Name mismatch in Google Cloud Console."
                12500 -> "SIGN_IN_FAILED (Code 12500): Configuration error. Check OAuth Consent Screen & Test Users in Google Cloud."
                12501 -> "SIGN_IN_CANCELLED (Code 12501): Sign-in cancelled by user."
                7 -> "NETWORK_ERROR (Code 7): Network error. Please check your connection."
                else -> "ApiException (Code ${e.statusCode}): ${e.message ?: "Error"}"
            }
            signInError = explanation
            android.util.Log.e("GoogleSignIn", "SettingsScreen sign-in error: code=${e.statusCode}, msg=${e.message}", e)
        } catch (e: Exception) {
            signInError = "Error: ${e.message ?: "Unknown"}"
            android.util.Log.e("GoogleSignIn", "SettingsScreen general error", e)
        }
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
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isBn = language == "bn"

                    // Account row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBn) "সংযুক্ত অ্যাকাউন্ট:" else "Connected Account:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = googleAccount?.email ?: (if (isBn) "কোনো অ্যাকাউন্ট নেই" else "Not Connected"),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (hasAllScopes) ToolGreen else if (googleAccount != null) ToolOrange else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (googleAccount != null) {
                                Text(
                                    text = if (hasAllScopes) (if (isBn) "✅ Drive ও Sheets পারমিশন মঞ্জুর" else "✅ Drive & Sheets Authorized")
                                           else (if (isBn) "⚠️ Drive/Sheets অনুমতি বাকি" else "⚠️ Drive/Sheets Permission Needed"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hasAllScopes) ToolGreen else ToolOrange
                                )
                            }
                        }

                        if (googleAccount == null) {
                            Button(
                                onClick = {
                                    signInError = null
                                    val client = GoogleSignIn.getClient(context, gso)
                                    signInLauncher.launch(client.signInIntent)
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(if (isBn) "কানেক্ট করুন" else "Connect Google")
                            }
                        } else {
                            Button(
                                onClick = {
                                    val client = GoogleSignIn.getClient(context, gso)
                                    client.signOut().addOnCompleteListener {
                                        googleAccount = null
                                        signInError = null
                                        com.example.util.UserManager.onAccountChanged(context, null)
                                        com.example.workers.BackupManager.onUserAccountChanged(context, null)
                                        viewModel.setNotification(if (isBn) "গুগল অ্যাকাউন্ট ডিসকানেক্ট করা হয়েছে" else "Disconnected Google Account")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(if (isBn) "ডিসকানেক্ট" else "Disconnect")
                            }
                        }
                    }

                    // Grant Permissions Button (if signed in but missing scopes)
                    if (googleAccount != null && !hasAllScopes) {
                        Button(
                            onClick = {
                                signInError = null
                                if (activity != null) {
                                    GoogleSignIn.requestPermissions(
                                        activity,
                                        9001,
                                        googleAccount,
                                        driveScope
                                    )
                                } else {
                                    val client = GoogleSignIn.getClient(context, gso)
                                    signInLauncher.launch(client.signInIntent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ToolOrange),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isBn) "ড্রাইভ অনুমতি দিন (Grant Permissions)" else "Grant Drive Permissions", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Error banner
                    if (!signInError.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (isBn) "সাইন-ইন সমস্যা (Error):" else "Authentication Issue:",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = signInError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                if (signInError!!.contains("DEVELOPER_ERROR") || signInError!!.contains("10")) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Package: com.aistudio.pdfmaster.qvrx\nSHA-1: D5:86:68:06:DA:A6:12:2E:35:87:0A:64:B7:0E:2F:02:29:36:36:FE",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }

                    // Cloud location info
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (isBn) "গুগল ড্রাইভ ফোল্ডার:" else "Google Drive Target:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Telecom Cyber Cafe\n├── Billing_Data (Sheets: 5 Tables)\n└── Cash_Memo_PDF (YEAR/MONTH/Invoices)",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurface
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

                    if (!lastStatus.isNullOrBlank()) {
                        Text(
                            text = (if (isBn) "স্ট্যাটাস: " else "Status: ") + lastStatus,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = if (lastStatus!!.startsWith("ব্যর্থ") || lastStatus!!.startsWith("FAILED") || lastStatus!!.startsWith("বাতিল")) MaterialTheme.colorScheme.error else ToolGreen
                        )
                    }

                    // Backup Now action
                    Button(
                        onClick = {
                            if (googleAccount == null) {
                                signInError = if (isBn) "অনুগ্রহ করে প্রথমে গুগল অ্যাকাউন্ট কানেক্ট করুন।" else "Please connect a Google account first."
                            } else if (!hasAllScopes) {
                                signInError = if (isBn) "ড্রাইভ ও শিটস অনুমতি দেওয়া হয়নি। অনুগ্রহ করে 'অনুমতি দিন' বাটনে চাপুন।" else "Drive & Sheets permissions needed. Please tap 'Grant Permissions'."
                            } else {
                                signInError = null
                                BackupManager.triggerImmediateBackup(context)
                                viewModel.setNotification(if (isBn) "⏳ ক্লাউড ব্যাকআপ শুরু হয়েছে..." else "⏳ Cloud backup started...")
                            }
                        },
                        enabled = !isBackupRunning,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isBackupRunning) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isBn) "কাজ চলছে..." else "In progress...")
                        } else {
                            Text(if (isBn) "এখনই ব্যাকআপ নিন (Backup Now)" else "Backup Now to Google Drive")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            if (googleAccount == null) {
                                signInError = if (isBn) "অনুগ্রহ করে প্রথমে গুগল অ্যাকাউন্ট কানেক্ট করুন।" else "Please connect a Google account first."
                            } else if (!hasAllScopes) {
                                signInError = if (isBn) "ড্রাইভ ও শিটস অনুমতি দেওয়া হয়নি। অনুগ্রহ করে 'অনুমতি দিন' বাটনে চাপুন।" else "Drive & Sheets permissions needed. Please tap 'Grant Permissions'."
                            } else {
                                signInError = null
                                BackupManager.triggerImmediateRestore(context)
                                viewModel.setNotification(if (isBn) "⏳ ড্রাইভ থেকে রিস্টোর শুরু হয়েছে..." else "⏳ Restoring from Google Drive...")
                            }
                        },
                        enabled = !isBackupRunning,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isBn) "ড্রাইভ থেকে রিস্টোর (Restore from Drive)" else "Restore from Google Drive")
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
