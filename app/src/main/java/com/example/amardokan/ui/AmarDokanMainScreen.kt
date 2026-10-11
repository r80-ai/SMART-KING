package com.example.amardokan.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    onExitApp: () -> Unit,
    onOpenPdfTools: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: AmarDokanViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    val ledgerCustomerId by viewModel.selectedCustomerIdForLedger.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog states
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<Customer?>(null) }
    var billToViewDetails by remember { mutableStateOf<Bill?>(null) }
    var showBackupDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Short)
            viewModel.clearUserMessage()
        }
    }

    // Intercept Back button
    BackHandler {
        if (!viewModel.navigateBack()) {
            onExitApp()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(targetState = currentScreen, label = "amar_dokan_screen_fade") { screen ->
            when (screen) {
                AmarDokanScreenType.HOME_DASHBOARD -> {
                    Screen1HomeDashboard(
                        viewModel = viewModel,
                        onOpenBackupDialog = { showBackupDialog = true },
                        onOpenPdfTools = onOpenPdfTools
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
                        onPrintReceipt = { billNumber, custName, dateStr, items, sub, disc, total, payType ->
                            printReceiptHtml(
                                context = context,
                                billNumber = billNumber,
                                customerName = custName,
                                dateStr = dateStr,
                                items = items,
                                subtotal = sub,
                                discount = disc,
                                grandTotal = total,
                                paymentType = payType
                            )
                        }
                    )
                }
                AmarDokanScreenType.DAILY_REPORT -> {
                    Screen3DailyReport(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() },
                        onOpenBillDetails = { billToViewDetails = it }
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
                        onEditCustomer = { cust ->
                            customerToEdit = cust
                            showAddCustomerDialog = true
                        }
                    )
                }
                AmarDokanScreenType.CUSTOMER_LEDGER -> {
                    Screen5CustomerLedger(
                        viewModel = viewModel,
                        customerId = ledgerCustomerId,
                        onBack = { viewModel.navigateBack() }
                    )
                }
                AmarDokanScreenType.RECEIVE_PAYMENT -> {
                    Screen6ReceivePayment(
                        viewModel = viewModel,
                        preselectedCustomerId = ledgerCustomerId,
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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // ---------------- MODAL DIALOGS ----------------
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
            }
        )
    }

    if (billToViewDetails != null) {
        BillDetailDialog(
            bill = billToViewDetails!!,
            viewModel = viewModel,
            onDismiss = { billToViewDetails = null }
        )
    }

    if (showBackupDialog) {
        BackupRestoreDialog(
            viewModel = viewModel,
            onDismiss = { showBackupDialog = false }
        )
    }
}
