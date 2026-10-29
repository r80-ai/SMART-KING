package com.example.amardokan.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.amardokan.data.Bill
import com.example.amardokan.data.Customer
import com.example.amardokan.ui.dialogs.AddCustomerDialog
import com.example.amardokan.ui.dialogs.BackupRestoreDialog
import com.example.amardokan.ui.dialogs.BillDetailDialog
import com.example.amardokan.ui.dialogs.printReceiptHtml
import com.example.amardokan.ui.screens.*

@Composable
fun AmarDokanMainScreen(
    onExitApp: () -> Unit = {},
    onOpenPdfTools: () -> Unit = {},
    viewModel: AmarDokanViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val selectedCustomerIdForLedger by viewModel.selectedCustomerIdForLedger.collectAsState()

    // Dialog States
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<Customer?>(null) }
    var selectedBillForDetails by remember { mutableStateOf<Bill?>(null) }
    var showBackupRestoreDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    BackHandler {
        if (!viewModel.navigateBack()) {
            onExitApp()
        }
    }

    AmarDokanTheme {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (currentScreen == AmarDokanScreenType.HOME_DASHBOARD) {
                FloatingActionButton(
                    onClick = { viewModel.navigateTo(AmarDokanScreenType.NEW_BILL) },
                    containerColor = AmarDokanColors.NavyDarkHeader,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_new_bill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "নতুন বিল")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("নতুন বিল", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AmarDokanColors.ContentBg)
        ) {
            Crossfade(
                targetState = currentScreen,
                label = "amar_dokan_screens"
            ) { screen ->
                when (screen) {
                    AmarDokanScreenType.HOME_DASHBOARD -> {
                        Screen1HomeDashboard(
                            viewModel = viewModel,
                            onOpenPdfTools = onOpenPdfTools,
                            onOpenBackupRestore = { showBackupRestoreDialog = true },
                            onOpenBillDetails = { bill -> selectedBillForDetails = bill }
                        )
                    }
                    AmarDokanScreenType.NEW_BILL -> {
                        Screen2NewBill(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateBack() },
                            onOpenAddCustomerDialog = {
                                customerToEdit = null
                                showAddCustomerDialog = true
                            },
                            onPrintReceipt = { billNumber, customerName, dateStr, items, subtotal, discount, grandTotal, paymentType ->
                                printReceiptHtml(
                                    context = context,
                                    billNumber = billNumber,
                                    customerName = customerName,
                                    dateStr = dateStr,
                                    items = items,
                                    subtotal = subtotal,
                                    discount = discount,
                                    grandTotal = grandTotal,
                                    paymentType = paymentType
                                )
                            }
                        )
                    }
                    AmarDokanScreenType.DAILY_REPORT -> {
                        Screen3DailyReport(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateBack() },
                            onOpenBillDetails = { bill -> selectedBillForDetails = bill }
                        )
                    }
                    AmarDokanScreenType.CUSTOMER_LIST -> {
                        Screen4CustomerList(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateBack() },
                            onOpenAddCustomerDialog = {
                                customerToEdit = null
                                showAddCustomerDialog = true
                            },
                            onEditCustomer = { customer ->
                                customerToEdit = customer
                                showAddCustomerDialog = true
                            }
                        )
                    }
                    AmarDokanScreenType.CUSTOMER_LEDGER -> {
                        Screen5CustomerLedger(
                            viewModel = viewModel,
                            customerId = selectedCustomerIdForLedger,
                            onBack = { viewModel.navigateBack() }
                        )
                    }
                    AmarDokanScreenType.RECEIVE_PAYMENT -> {
                        Screen6ReceivePayment(
                            viewModel = viewModel,
                            preselectedCustomerId = selectedCustomerIdForLedger,
                            onBack = { viewModel.navigateBack() }
                        )
                    }
                    AmarDokanScreenType.ITEM_SETTINGS -> {
                        Screen7ItemSettings(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateBack() }
                        )
                    }
                    AmarDokanScreenType.MONTHLY_REPORT -> {
                        Screen8MonthlyReport(
                            viewModel = viewModel,
                            onBack = { viewModel.navigateBack() }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddCustomerDialog) {
        AddCustomerDialog(
            customerToEdit = customerToEdit,
            onDismiss = {
                showAddCustomerDialog = false
                customerToEdit = null
            },
            onSave = { name, phone, address, notes ->
                if (customerToEdit == null) {
                    viewModel.addCustomer(name, phone, address, notes)
                } else {
                    viewModel.updateCustomer(
                        customerToEdit!!.copy(
                            name = name,
                            phone = phone,
                            address = address,
                            notes = notes
                        )
                    )
                }
                showAddCustomerDialog = false
                customerToEdit = null
            },
            viewModel = viewModel
        )
    }

    selectedBillForDetails?.let { bill ->
        BillDetailDialog(
            bill = bill,
            viewModel = viewModel,
            onDismiss = { selectedBillForDetails = null }
        )
    }

        if (showBackupRestoreDialog) {
            BackupRestoreDialog(
                viewModel = viewModel,
                onDismiss = { showBackupRestoreDialog = false }
            )
        }
    }
}
