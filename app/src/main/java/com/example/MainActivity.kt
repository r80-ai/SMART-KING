package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ActiveModal
import com.example.ui.NavigationTab
import com.example.ui.PdfMasterViewModel
import com.example.ui.dialogs.ToolActionModalsHost
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PhotoToolsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ToolsScreen
import com.example.hisab.ui.HisabMainScreen
import com.example.calendar.ui.BanglaCalendarScreen
import com.example.calendar.widget.CalendarWidgetUpdater
import android.content.Intent
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding

class MainActivity : ComponentActivity() {
    private var mainViewModel: PdfMasterViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Update home screen widgets on start
        CalendarWidgetUpdater.updateAllWidgets(this)

        // Initialize Amar Dokan local backup manager
        com.example.amardokan.data.AmarDokanBackupManager.init(this)


        setContent {
            val vm: PdfMasterViewModel = viewModel()
            mainViewModel = vm

            // Handle widget launch intent
            LaunchedEffect(Unit) {
                handleCalendarIntent(intent, vm)
            }

            val darkThemeMode by vm.darkThemeMode.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val isDark = darkThemeMode ?: systemDark

            MyApplicationTheme(darkTheme = isDark) {
                PdfMasterApp(viewModel = vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        mainViewModel?.let { handleCalendarIntent(intent, it) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleCalendarIntent(intent: Intent?, vm: PdfMasterViewModel) {
        val target = intent?.getStringExtra(CalendarWidgetUpdater.EXTRA_OPEN_CALENDAR)
        if (target == CalendarWidgetUpdater.VALUE_OPEN_BANGLA) {
            vm.setTab(NavigationTab.BANGLA_CALENDAR)
        }
    }
}

@Composable
fun PdfMasterApp(viewModel: PdfMasterViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val activeModal by viewModel.activeModal.collectAsState()
    val notification by viewModel.notification.collectAsState()
    val language by viewModel.appLanguage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(notification) {
        notification?.let { notif ->
            snackbarHostState.showSnackbar(
                message = notif.message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearNotification()
        }
    }

    BackHandler(enabled = activeModal == ActiveModal.NONE && (currentTab != NavigationTab.HOME || viewModel.canPopTab())) {
        if (!viewModel.popTab()) {
            viewModel.setTab(NavigationTab.HOME)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_scaffold"),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },

    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            Crossfade(
                targetState = currentTab,
                label = "tab_crossfade",
                modifier = Modifier.fillMaxSize()
            ) { tab ->
                when (tab) {
                    NavigationTab.AMAR_DOKAN -> com.example.amardokan.ui.AmarDokanMainScreen(
                        onExitApp = {
                            if (!viewModel.popTab()) {
                                viewModel.setTab(NavigationTab.HOME)
                            }
                        },
                        onOpenPdfTools = {
                            viewModel.setTab(NavigationTab.HOME)
                        }
                    )
                    NavigationTab.HOME -> HomeScreen(viewModel = viewModel)
                    NavigationTab.TOOLS -> ToolsScreen(viewModel = viewModel)
                    NavigationTab.PHOTO_TOOLS -> PhotoToolsScreen(viewModel = viewModel)
                    NavigationTab.HISAB_CALCULATOR -> HisabMainScreen(
                        viewModel = viewModel,
                        onBack = {
                            if (!viewModel.popTab()) {
                                viewModel.setTab(NavigationTab.HOME)
                            }
                        }
                    )
                    NavigationTab.BANGLA_CALENDAR -> BanglaCalendarScreen(
                        onBack = {
                            if (!viewModel.popTab()) {
                                viewModel.setTab(NavigationTab.HOME)
                            }
                        }
                    )
                    NavigationTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }

            // Host for all interactive PDF tool modals
            ToolActionModalsHost(viewModel = viewModel)
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
