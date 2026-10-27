package com.example.ui.dialogs

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import java.io.File
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.DynamicForm
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import android.widget.Toast
import androidx.compose.material.icons.filled.LowPriority
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.model.PdfFileRecord
import com.example.ui.ActiveModal
import com.example.ui.PdfMasterViewModel
import com.example.ui.screens.RemovePagesScreen
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ToolBlue
import com.example.ui.theme.ToolGreen
import com.example.ui.theme.ToolIndigo
import com.example.ui.theme.ToolOrange
import com.example.ui.theme.ToolPink
import com.example.ui.theme.ToolPurple
import com.example.ui.theme.ToolTeal

@Composable
fun ToolActionModalsHost(viewModel: PdfMasterViewModel) {
    val activeModal by viewModel.activeModal.collectAsState()
    val selectedFile by viewModel.selectedFile.collectAsState()
    val allFiles by viewModel.allFiles.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val processingMessage by viewModel.processingMessage.collectAsState()
    val toolResult by viewModel.toolResult.collectAsState()

    // Back handler for Tool Result preview: Intercepts back press to prevent accidental exit
    BackHandler(enabled = toolResult != null) {
        // Intercept back button to prevent unexpected dismissal or interruption of state
    }

    // Default back handler for active modals (child screens with sub-flows provide their own inner BackHandler)
    BackHandler(enabled = activeModal != ActiveModal.NONE && toolResult == null) {
        viewModel.closeModal()
    }

    // Unified Result Preview Dialog (Step 8 & 9)
    toolResult?.let { resultInfo ->
        ToolResultPreviewDialog(
            info = resultInfo,
            onOpenFile = { record ->
                viewModel.openSavedFile(
                    resultInfo.savedUri,
                    File(record.filePath),
                    if (resultInfo.isPhotoOutput) "image/jpeg" else "application/pdf"
                )
            },
            onViewFolder = { _, uri ->
                viewModel.openSavedFolder(
                    uri,
                    File(resultInfo.resultFile.filePath)
                )
            },
            onShareFile = { record, uri ->
                viewModel.shareSavedFile(
                    uri,
                    File(record.filePath),
                    if (resultInfo.isPhotoOutput) "image/jpeg" else "application/pdf"
                )
            },
            onSaveAndOpenInFiles = {
                viewModel.dismissToolResult(goToFiles = true)
            },
            onDismiss = {
                viewModel.dismissToolResult(goToFiles = false)
            }
        )
    }

    // Loading Overlay
    if (isProcessing) {
        Dialog(onDismissRequest = {}) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = CrimsonPrimary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = processingMessage, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    when (activeModal) {
        ActiveModal.MERGE -> {
            StandaloneMergeScreen(
                initialFiles = emptyList(),
                viewModel = viewModel,
                onBack = { viewModel.closeModal() }
            )
        }
        ActiveModal.SPLIT -> {
            StandaloneSplitScreen(
                initialFile = selectedFile,
                viewModel = viewModel,
                toolTitle = "Split PDF",
                onBack = { viewModel.closeModal() }
            )
        }
        ActiveModal.EXTRACT_PAGES -> {
            StandaloneExtractPagesScreen(
                initialFile = selectedFile,
                viewModel = viewModel,
                onBack = { viewModel.closeModal() }
            )
        }
        ActiveModal.REMOVE_PAGES -> {
            RemovePagesScreen(
                initialFile = selectedFile,
                viewModel = viewModel,
                onBack = { viewModel.closeModal() }
            )
        }
        ActiveModal.CROP -> {
            StandaloneCropScreen(
                initialFile = selectedFile,
                viewModel = viewModel,
                onBack = { viewModel.closeModal() }
            )
        }
        ActiveModal.COMPRESS -> {
            StandaloneCompressScreen(
                initialFile = selectedFile,
                viewModel = viewModel,
                onBack = { viewModel.closeModal() }
            )
        }
        ActiveModal.COMPRESS_PHOTO -> {
            com.example.ui.screens.CompressPhotoScreen(
                viewModel = viewModel,
                onBack = { viewModel.closeModal() }
            )
        }
        ActiveModal.PASSPORT_PHOTO -> {
            com.example.passport.MultiplePassportScreen(
                onBack = { viewModel.closeModal() }
            )
        }
        ActiveModal.PAN_CARD -> {
            com.example.ui.screens.PanCardScreen(
                viewModel = viewModel,
                onBack = { viewModel.closeModal() }
            )
        }
        ActiveModal.PHOTO_EDITING -> {
            com.example.ui.screens.PhotoEditingScreen(
                viewModel = viewModel,
                onBack = { viewModel.closeModal() }
            )
        }
        ActiveModal.SPLIT_CAMERA -> {
            com.example.ui.screens.SplitCameraScreen(
                viewModel = viewModel,
                onBack = { viewModel.closeModal() }
            )
        }
        ActiveModal.ROTATE -> {
            StandaloneRotateScreen(
                initialFile = selectedFile,
                viewModel = viewModel,
                onBack = { viewModel.closeModal() }
            )
        }
        ActiveModal.WATERMARK -> {
            StandaloneWatermarkScreen(
                initialFile = selectedFile,
                viewModel = viewModel,
                onBack = { viewModel.closeModal() }
            )
        }
        ActiveModal.PAGE_NUMBERS -> {
            StandalonePageNumbersScreen(
                initialFile = selectedFile,
                viewModel = viewModel,
                onBack = { viewModel.closeModal() }
            )
        }
        ActiveModal.EDGE_PERSPECTIVE -> {
            com.example.ui.screens.EdgePerspectiveScreen(
                viewModel = viewModel,
                onBack = { viewModel.closeModal() }
            )
        }
        ActiveModal.SCANNER -> {
            com.example.ui.dialogs.ScannerModalDialog(
                isEdgePerspectiveOnly = false,
                onDismiss = { viewModel.closeModal() },
                onSavePdf = { bitmaps, name ->
                    viewModel.scanOrImagesToPdf(bitmaps, name, isScan = true)
                    viewModel.closeModal()
                }
            )
        }
        ActiveModal.VIEWER -> {
            val fileToOpen = selectedFile ?: allFiles.firstOrNull()
            if (fileToOpen != null) {
                PdfWorkspaceDialog(
                    initialFile = fileToOpen,
                    viewModel = viewModel,
                    onDismiss = { viewModel.closeModal() }
                )
            }
        }
        ActiveModal.SIGNATURE_VALIDATION -> {
            com.example.ui.screens.DigitalSignatureValidationScreen(
                initialFile = selectedFile,
                viewModel = viewModel,
                onBack = { viewModel.closeModal() }
            )
        }
        ActiveModal.PDF_TO_JPG -> {
            com.example.ui.screens.StandalonePdfToJpgScreen(
                initialFile = selectedFile,
                viewModel = viewModel,
                onBack = { viewModel.closeModal() }
            )
        }
        ActiveModal.PVC_CARD_MAKER -> {
            com.example.pvccard.ui.PvcCardScreen(
                onBack = { viewModel.closeModal() }
            )
        }
        ActiveModal.RESUME_BUILDER -> {
            com.example.ui.screens.ResumeBuilderScreen(
                onBack = { viewModel.closeModal() }
            )
        }
        ActiveModal.APPLICATION_MAKER -> {
            com.example.ui.screens.ApplicationMakerScreen(
                onBack = { viewModel.closeModal() }
            )
        }
        ActiveModal.ANNAPURNA_DECLARATION -> {
            com.example.ui.screens.AnnapurnaDeclarationScreen(
                onBack = { viewModel.closeModal() }
            )
        }
        ActiveModal.DOC_ENHANCER -> {
            com.example.ui.screens.DocEnhancerScreen(
                viewModel = viewModel,
                onBack = { viewModel.closeModal() }
            )
        }
        ActiveModal.NAME_DATE_PHOTO -> {
            com.example.ui.screens.NameDatePhotoScreen(
                viewModel = viewModel,
                onBack = { viewModel.closeModal() }
            )
        }
        ActiveModal.EDIT -> {
            com.example.ui.screens.EditPdfScreen(
                initialFile = selectedFile,
                viewModel = viewModel,
                onBack = { viewModel.closeModal() }
            )
        }
        ActiveModal.RENAME -> {
            selectedFile?.let { file ->
                var newName by remember(file) { mutableStateOf(file.name) }
                AlertDialog(
                    onDismissRequest = { viewModel.closeModal() },
                    title = { Text("Rename PDF") },
                    text = {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("File Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("rename_input")
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newName.isNotBlank()) {
                                    viewModel.renameFile(file, newName.trim())
                                }
                                viewModel.closeModal()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                            modifier = Modifier.testTag("rename_confirm_button")
                        ) {
                            Text("Rename")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.closeModal() }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
        ActiveModal.DELETE_CONFIRM -> {
            selectedFile?.let { file ->
                AlertDialog(
                    onDismissRequest = { viewModel.closeModal() },
                    title = { Text("Delete PDF?") },
                    text = { Text("Are you sure you want to delete \"${file.name}\"? This action cannot be undone.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteFile(file)
                                viewModel.closeModal()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("delete_confirm_button")
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.closeModal() }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
        ActiveModal.PROTECT -> {
            selectedFile?.let { file ->
                var passwordInput by remember { mutableStateOf("") }
                var passwordVisible by remember { mutableStateOf(false) }
                var outputFileName by remember(file) { mutableStateOf("${file.nameWithoutExtension()}_protected.pdf") }
                val context = LocalContext.current

                AlertDialog(
                    onDismissRequest = { viewModel.closeModal() },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = ToolPurple, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Protect PDF (Add Password)", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "Enter a password to encrypt \"${file.name}\". All pages will be secured with standard encryption.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Set Password *") },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle password visibility"
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("input_modal_protect_password")
                            )
                            OutlinedTextField(
                                value = outputFileName,
                                onValueChange = { outputFileName = it },
                                label = { Text("Output File Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("input_modal_protect_output_name")
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val pass = passwordInput.trim()
                                if (pass.isEmpty()) {
                                    Toast.makeText(context, "Please enter a password", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.closeModal()
                                viewModel.protectPdf(
                                    sourceRecord = file,
                                    password = pass,
                                    desiredFileName = if (outputFileName.isBlank()) "${file.nameWithoutExtension()}_protected.pdf" else outputFileName.trim()
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ToolPurple),
                            modifier = Modifier.testTag("btn_modal_confirm_protect")
                        ) {
                            Text("Protect & Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.closeModal() }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
        ActiveModal.UNLOCK -> {
            selectedFile?.let { file ->
                var passwordInput by remember { mutableStateOf("") }
                var passwordVisible by remember { mutableStateOf(false) }
                var outputFileName by remember(file) { mutableStateOf("${file.nameWithoutExtension()}_unlocked.pdf") }

                AlertDialog(
                    onDismissRequest = { viewModel.closeModal() },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, tint = ToolBlue, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unlock PDF (Remove Password)", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "Remove encryption and save a fully unlocked version of \"${file.name}\". If it requires a password to open, please enter it below:",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Current Password (if required)") },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle password visibility"
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("input_modal_unlock_password")
                            )
                            OutlinedTextField(
                                value = outputFileName,
                                onValueChange = { outputFileName = it },
                                label = { Text("Output File Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("input_modal_unlock_output_name")
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.closeModal()
                                viewModel.unlockPdf(
                                    sourceRecord = file,
                                    password = passwordInput.trim(),
                                    desiredFileName = if (outputFileName.isBlank()) "${file.nameWithoutExtension()}_unlocked.pdf" else outputFileName.trim()
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ToolBlue),
                            modifier = Modifier.testTag("btn_modal_confirm_unlock")
                        ) {
                            Text("Unlock & Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.closeModal() }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
        else -> {}
    }
}
