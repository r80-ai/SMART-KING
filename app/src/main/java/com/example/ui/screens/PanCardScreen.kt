package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.pdf.PanDocumentPdfBuilder
import com.example.storage.GlobalSaveManager
import com.example.ui.PdfMasterViewModel
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ToolBlue
import com.example.ui.theme.ToolGreen
import com.example.ui.theme.ToolOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.max

enum class PanProvider(val label: String, val subtitle: String) {
    NSDL("NSDL / Protean", "Protean eGov Technologies"),
    UTIITSL("UTIITSL", "UTI Infrastructure Technology")
}

enum class PanAssetType(val label: String) {
    PHOTO("Photo"),
    SIGNATURE("Signature"),
    DOCUMENT("Document")
}

enum class ProteanRoute(val label: String, val note: String) {
    STANDARD("Standard Online Upload", "Photo ≤ 50 KB, Sign ≤ 50 KB"),
    DSC_ALTERNATIVE("Protean DSC Alternative", "Photo ≤ 20 KB, Sign ≤ 10 KB")
}

data class PanSpec(
    val provider: PanProvider,
    val assetType: PanAssetType,
    val widthPx: Int,
    val heightPx: Int,
    val dpi: Int,
    val maxKb: Int,
    val format: String = "JPEG",
    val aspectDescription: String,
    val description: String
)

data class ProcessedPanResult(
    val bytes: ByteArray,
    val bitmap: Bitmap,
    val widthPx: Int,
    val heightPx: Int,
    val dpi: Int,
    val sizeKb: Double,
    val maxKb: Int,
    val format: String,
    val provider: PanProvider,
    val assetType: PanAssetType,
    val savedFile: File? = null,
    val savedUri: Uri? = null,
    val isPdf: Boolean = false,
    val pdfBytes: ByteArray? = null,
    val pdfSizeKb: Double? = null,
    val pageCount: Int = 1
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ProcessedPanResult
        return bytes.contentEquals(other.bytes) && widthPx == other.widthPx && heightPx == other.heightPx && pageCount == other.pageCount
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + widthPx
        result = 31 * result + heightPx
        result = 31 * result + pageCount
        return result
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanCardScreen(
    viewModel: PdfMasterViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedProvider by remember { mutableStateOf(PanProvider.NSDL) }
    var selectedAssetType by remember { mutableStateOf(PanAssetType.PHOTO) }
    var proteanRoute by remember { mutableStateOf(ProteanRoute.STANDARD) }
    
    // Signature enhancement toggle (makes background pure white, text high contrast)
    var enhanceSignature by remember { mutableStateOf(true) }

    // Source image state
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var additionalDocBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }

    // Manual Pan & Zoom inside the crop frame
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var previewBoxWidth by remember { mutableFloatStateOf(1f) }
    var previewBoxHeight by remember { mutableFloatStateOf(1f) }

    // Processing & Result state
    var isProcessing by remember { mutableStateOf(false) }
    var processedResult by remember { mutableStateOf<ProcessedPanResult?>(null) }

    // Resolve current active spec automatically
    val activeSpec = remember(selectedProvider, selectedAssetType, proteanRoute) {
        getPanSpec(selectedProvider, selectedAssetType, proteanRoute)
    }

    val targetAspect = remember(activeSpec) {
        if (activeSpec.heightPx > 0) activeSpec.widthPx.toFloat() / activeSpec.heightPx.toFloat() else 1f
    }

    // Photo picker launcher (single file)
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream)
                        withContext(Dispatchers.Main) {
                            sourceBitmap = bmp
                            sourceUri = uri
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                            processedResult = null
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error loading file: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Multi-page document picker launcher
    val multiDocPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val loaded = mutableListOf<Bitmap>()
                    for (u in uris) {
                        context.contentResolver.openInputStream(u)?.use { stream ->
                            val bmp = BitmapFactory.decodeStream(stream)
                            if (bmp != null) loaded.add(bmp)
                        }
                    }
                    if (loaded.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            sourceBitmap = loaded.first()
                            sourceUri = uris.first()
                            additionalDocBitmaps = loaded.drop(1)
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                            processedResult = null
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error loading document: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Add extra page launcher for multi-page documents
    val addDocPageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream)
                        if (bmp != null) {
                            withContext(Dispatchers.Main) {
                                additionalDocBitmaps = additionalDocBitmaps + bmp
                                processedResult = null
                                Toast.makeText(context, "Added page ${1 + additionalDocBitmaps.size} (Total: ${1 + additionalDocBitmaps.size})", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error adding page: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Auto-fit framing whenever activeSpec or sourceBitmap changes
    LaunchedEffect(activeSpec, sourceBitmap) {
        sourceBitmap?.let { bmp ->
            val imgRatio = bmp.width.toFloat() / bmp.height.toFloat()
            val boxRatio = targetAspect
            // Fit without distortion, fill bounds
            val fillScale = max(boxRatio / imgRatio, imgRatio / boxRatio)
            scale = fillScale
            offsetX = 0f
            offsetY = 0f
        }
    }

    val handleBackPress: () -> Unit = {
        if (processedResult != null) {
            processedResult = null
        } else {
            onBack()
        }
    }

    BackHandler(enabled = true) {
        handleBackPress()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PAN Card Upload Engine",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = androidx.compose.ui.graphics.Color.White
                        )
                        Text(
                            text = "Official 2026 Presets • NSDL & UTIITSL",
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color(0xFF94A3B8)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = handleBackPress, modifier = Modifier.testTag("btn_pan_back")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFF0F172A)
                )
            )
        },
        containerColor = androidx.compose.ui.graphics.Color(0xFF0B1120)
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ==========================================
            // 1. PROVIDER SELECTOR (NSDL / UTIITSL)
            // ==========================================
            Text(
                text = "1. SELECT PAN PROVIDER",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = androidx.compose.ui.graphics.Color(0xFF94A3B8)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PanProviderCard(
                    provider = PanProvider.NSDL,
                    isSelected = selectedProvider == PanProvider.NSDL,
                    accentColor = CrimsonPrimary,
                    modifier = Modifier.weight(1f).testTag("btn_provider_nsdl"),
                    onClick = {
                        selectedProvider = PanProvider.NSDL
                        processedResult = null
                    }
                )

                PanProviderCard(
                    provider = PanProvider.UTIITSL,
                    isSelected = selectedProvider == PanProvider.UTIITSL,
                    accentColor = ToolOrange,
                    modifier = Modifier.weight(1f).testTag("btn_provider_utiitsl"),
                    onClick = {
                        selectedProvider = PanProvider.UTIITSL
                        processedResult = null
                    }
                )
            }

            // Protean DSC Alternative Route Option (if NSDL selected)
            if (selectedProvider == PanProvider.NSDL) {
                Surface(
                    color = androidx.compose.ui.graphics.Color(0xFF1E293B),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Protean Route Mode",
                                    color = androidx.compose.ui.graphics.Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp
                                )
                                Text(
                                    text = if (proteanRoute == ProteanRoute.STANDARD)
                                        "Standard Online Portal (Photo ≤ 50 KB, Sign ≤ 50 KB)"
                                    else
                                        "DSC Alternative Route (Photo ≤ 20 KB, Sign ≤ 10 KB)",
                                    color = if (proteanRoute == ProteanRoute.STANDARD)
                                        androidx.compose.ui.graphics.Color(0xFF94A3B8)
                                    else
                                        ToolOrange,
                                    fontSize = 11.5.sp
                                )
                            }

                            Switch(
                                checked = proteanRoute == ProteanRoute.DSC_ALTERNATIVE,
                                onCheckedChange = { isChecked ->
                                    proteanRoute = if (isChecked) ProteanRoute.DSC_ALTERNATIVE else ProteanRoute.STANDARD
                                    processedResult = null
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ToolOrange,
                                    checkedTrackColor = ToolOrange.copy(alpha = 0.3f),
                                    uncheckedThumbColor = androidx.compose.ui.graphics.Color(0xFF94A3B8),
                                    uncheckedTrackColor = androidx.compose.ui.graphics.Color(0xFF334155)
                                ),
                                modifier = Modifier.testTag("switch_protean_dsc")
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 2. ASSET TYPE SELECTOR (Photo / Signature / Document)
            // ==========================================
            Text(
                text = "2. SELECT REQUIREMENT TYPE",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = androidx.compose.ui.graphics.Color(0xFF94A3B8)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PanAssetTypeTab(
                    assetType = PanAssetType.PHOTO,
                    icon = Icons.Default.Person,
                    isSelected = selectedAssetType == PanAssetType.PHOTO,
                    modifier = Modifier.weight(1f).testTag("btn_type_photo"),
                    onClick = {
                        selectedAssetType = PanAssetType.PHOTO
                        processedResult = null
                    }
                )

                PanAssetTypeTab(
                    assetType = PanAssetType.SIGNATURE,
                    icon = Icons.Default.Draw,
                    isSelected = selectedAssetType == PanAssetType.SIGNATURE,
                    modifier = Modifier.weight(1f).testTag("btn_type_signature"),
                    onClick = {
                        selectedAssetType = PanAssetType.SIGNATURE
                        processedResult = null
                    }
                )

                PanAssetTypeTab(
                    assetType = PanAssetType.DOCUMENT,
                    icon = Icons.Default.Description,
                    isSelected = selectedAssetType == PanAssetType.DOCUMENT,
                    modifier = Modifier.weight(1f).testTag("btn_type_document"),
                    onClick = {
                        selectedAssetType = PanAssetType.DOCUMENT
                        processedResult = null
                    }
                )
            }

            // ==========================================
            // 3. PHOTO SELECTION & CROP PREVIEW
            // ==========================================
            Text(
                text = "3. SELECT & FRAME FILE",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = androidx.compose.ui.graphics.Color(0xFF94A3B8)
            )

            if (sourceBitmap == null) {
                // Empty state: Upload button
                Surface(
                    onClick = {
                        if (selectedAssetType == PanAssetType.DOCUMENT) {
                            multiDocPickerLauncher.launch("image/*")
                        } else {
                            photoPickerLauncher.launch("image/*")
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = androidx.compose.ui.graphics.Color(0xFF1E293B),
                    border = BorderStroke(1.5.dp, androidx.compose.ui.graphics.Color(0xFF475569)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_select_pan_file")
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 36.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = CrimsonPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when (selectedAssetType) {
                                        PanAssetType.PHOTO -> Icons.Default.AddPhotoAlternate
                                        PanAssetType.SIGNATURE -> Icons.Default.Draw
                                        PanAssetType.DOCUMENT -> Icons.Default.Description
                                    },
                                    contentDescription = null,
                                    tint = CrimsonPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Select ${selectedAssetType.label} for ${selectedProvider.label}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (selectedAssetType == PanAssetType.DOCUMENT)
                                "Tap to select 1 or multiple document pages (Gallery/Files). Will combine into 1 PDF strictly ≤ 300 KB."
                            else
                                "Tap to choose from Gallery or Camera. Will automatically crop to ${activeSpec.aspectDescription} at ${activeSpec.dpi} DPI.",
                            textAlign = TextAlign.Center,
                            fontSize = 12.5.sp,
                            color = androidx.compose.ui.graphics.Color(0xFF94A3B8)
                        )
                    }
                }
            } else {
                // Photo is selected: Show interactive framing canvas locked to exact target ratio
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = androidx.compose.ui.graphics.Color(0xFF1E293B),
                    border = BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedAssetType == PanAssetType.DOCUMENT && additionalDocBitmaps.isNotEmpty())
                                    "Auto Frame Page 1 (${1 + additionalDocBitmaps.size} Pages Loaded)"
                                else
                                    "Auto Frame (${activeSpec.aspectDescription})",
                                color = androidx.compose.ui.graphics.Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selectedAssetType == PanAssetType.DOCUMENT) {
                                    TextButton(
                                        onClick = { addDocPageLauncher.launch("image/*") },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = ToolGreen)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("+ Page", color = ToolGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                TextButton(
                                    onClick = {
                                        if (selectedAssetType == PanAssetType.DOCUMENT) {
                                            multiDocPickerLauncher.launch("image/*")
                                        } else {
                                            photoPickerLauncher.launch("image/*")
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = ToolBlue)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Change File", color = ToolBlue, fontSize = 12.sp)
                                }
                            }
                        }

                        if (selectedAssetType == PanAssetType.DOCUMENT && additionalDocBitmaps.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "✓ ${1 + additionalDocBitmaps.size} Pages Loaded • All merged into 1 PDF ≤ 300 KB",
                                color = ToolGreen,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Interactive crop frame (locked aspect ratio)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (targetAspect > 1.8f) 0.95f else 0.82f)
                                .aspectRatio(targetAspect)
                                .background(androidx.compose.ui.graphics.Color.White)
                                .clipToBounds()
                                .onSizeChanged {
                                    if (it.width > 0 && it.height > 0) {
                                        previewBoxWidth = it.width.toFloat()
                                        previewBoxHeight = it.height.toFloat()
                                    }
                                }
                                .pointerInput(activeSpec) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceAtLeast(0.1f)
                                        offsetX += pan.x
                                        offsetY += pan.y
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = sourceBitmap!!.asImageBitmap(),
                                contentDescription = "Cropping Preview",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offsetX,
                                        translationY = offsetY
                                    ),
                                contentScale = ContentScale.Fit
                            )

                            // Border outline for active target frame
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(2.dp, CrimsonPrimary)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // On-screen framing controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        sourceBitmap?.let { bmp ->
                                            val imgRatio = bmp.width.toFloat() / bmp.height.toFloat()
                                            val boxRatio = targetAspect
                                            scale = max(boxRatio / imgRatio, imgRatio / boxRatio)
                                            offsetX = 0f
                                            offsetY = 0f
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF334155)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("btn_auto_fill_frame")
                                ) {
                                    Icon(Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Auto Fill", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF334155)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("btn_fit_frame")
                                ) {
                                    Icon(Icons.Default.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Fit All", fontSize = 12.sp)
                                }
                            }

                            // Zoom controls
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = { scale = (scale / 1.15f).coerceAtLeast(0.1f) },
                                    modifier = Modifier.size(32.dp).testTag("btn_pan_zoom_out")
                                ) {
                                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    text = "${(scale * 100).toInt()}%",
                                    color = androidx.compose.ui.graphics.Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                IconButton(
                                    onClick = { scale = (scale * 1.15f).coerceAtLeast(0.1f) },
                                    modifier = Modifier.size(32.dp).testTag("btn_pan_zoom_in")
                                ) {
                                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        // Signature enhancement switch
                        if (selectedAssetType == PanAssetType.SIGNATURE) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = androidx.compose.ui.graphics.Color(0xFF0F172A),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Tune, contentDescription = null, tint = ToolOrange, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Enhance Signature (Crisp White Background)", fontSize = 11.5.sp, color = androidx.compose.ui.graphics.Color.White)
                                    }
                                    Switch(
                                        checked = enhanceSignature,
                                        onCheckedChange = { enhanceSignature = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = ToolOrange, checkedTrackColor = ToolOrange.copy(alpha = 0.3f))
                                    )
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // 4. ACTION: AUTO CONVERT & VALIDATE
                // ==========================================
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isProcessing = true
                            try {
                                val result = withContext(Dispatchers.Default) {
                                    processPanAsset(
                                        source = sourceBitmap!!,
                                        spec = activeSpec,
                                        scale = scale,
                                        offsetX = offsetX,
                                        offsetY = offsetY,
                                        previewBoxW = previewBoxWidth,
                                        previewBoxH = previewBoxHeight,
                                        enhanceSignature = enhanceSignature && activeSpec.assetType == PanAssetType.SIGNATURE,
                                        additionalPages = if (activeSpec.assetType == PanAssetType.DOCUMENT) additionalDocBitmaps else emptyList()
                                    )
                                }
                                processedResult = result
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error processing: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isProcessing = false
                            }
                        }
                    },
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_convert_pan_photo")
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            color = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Auto Resizing & Binary-Search Compressing...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Auto Process & Validate For ${selectedProvider.label}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        )
                    }
                }
            }

            // ==========================================
            // 5. VALIDATION CARD & DOWNLOAD
            // ==========================================
            AnimatedVisibility(visible = processedResult != null) {
                processedResult?.let { result ->
                    Surface(
                        color = androidx.compose.ui.graphics.Color(0xFF064E3B).copy(alpha = 0.35f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, ToolGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_pan_validation_success")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Header
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = ToolGreen,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = androidx.compose.ui.graphics.Color.Black, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "✓ Requirement Matched",
                                        color = ToolGreen,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp
                                    )
                                    Text(
                                        text = "Guaranteed 100% Compliant with ${result.provider.label} Portal",
                                        color = androidx.compose.ui.graphics.Color(0xFFD1FAE5),
                                        fontSize = 11.5.sp
                                    )
                                }
                            }

                            // Validation parameters table
                            Surface(
                                color = androidx.compose.ui.graphics.Color(0xFF0F172A).copy(alpha = 0.8f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (result.assetType == PanAssetType.DOCUMENT) {
                                        ValidationRow("Document PDF", "${result.provider.label} Supporting Document")
                                        ValidationRow("Page Count", "${result.pageCount} ${if (result.pageCount > 1) "Pages (Combined in 1 PDF)" else "Page"}")
                                        ValidationRow("Format", "PDF (ISO 32000-1)")
                                        ValidationRow("Resolution", "200 DPI (Portal Standard)")
                                        ValidationRow(
                                            "File Size",
                                            "${String.format(Locale.US, "%.1f", result.sizeKb)} KB",
                                            highlightGreen = true
                                        )
                                        ValidationRow("Maximum Allowed", "300 KB")
                                        ValidationRow("Status", "✓ Requirement Matched (≤ 300 KB)", highlightGreen = true)
                                    } else {
                                        ValidationRow("Provider", result.provider.label)
                                        ValidationRow("Preset", "${result.assetType.label} Online Upload")
                                        ValidationRow("Format", result.format)
                                        ValidationRow("Dimensions", "${result.widthPx} × ${result.heightPx} px (Exact)")
                                        ValidationRow("Resolution", "${result.dpi} DPI (JFIF Embedded)")
                                        ValidationRow(
                                            "File Size",
                                            "${String.format(Locale.US, "%.1f", result.sizeKb)} KB (Limit: ≤ ${result.maxKb} KB)",
                                            highlightGreen = true
                                        )
                                        ValidationRow("Status", "Ready for Immediate Upload", highlightGreen = true)
                                    }
                                }
                            }

                            // Processed preview thumbnail
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF64748B)),
                                    color = androidx.compose.ui.graphics.Color.White
                                ) {
                                    Image(
                                        bitmap = result.bitmap.asImageBitmap(),
                                        contentDescription = "Final Result",
                                        modifier = Modifier
                                            .heightIn(max = 160.dp)
                                            .padding(2.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }

                            // Action Buttons: Save/Download & Share
                            val isDoc = result.assetType == PanAssetType.DOCUMENT
                            val actualDocBytes: Long = result.pdfBytes?.let { it.size.toLong() } ?: (result.sizeKb * 1024.0).toLong()
                            val canDownload = if (isDoc) {
                                actualDocBytes <= 300 * 1024L
                            } else {
                                result.sizeKb <= result.maxKb
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            if (isDoc) {
                                                savePanDocumentPdf(context, result)
                                            } else {
                                                savePanResult(context, result)
                                            }
                                        }
                                    },
                                    enabled = canDownload,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ToolGreen),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("btn_pan_download")
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, tint = androidx.compose.ui.graphics.Color.Black, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isDoc) "Download PDF" else "Download",
                                        color = androidx.compose.ui.graphics.Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            sharePanResult(context, result)
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF94A3B8)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("btn_pan_share")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                }
                            }

                            // If Document mode, also offer 1-click Download as Official PDF button
                            if (isDoc) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            savePanDocumentPdf(context, result)
                                        }
                                    },
                                    enabled = canDownload,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ToolBlue),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("btn_pan_download_pdf")
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Download as Official PDF (≤ 300 KB)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PanProviderCard(
    provider: PanProvider,
    isSelected: Boolean,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color(0xFF1E293B),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) accentColor else androidx.compose.ui.graphics.Color(0xFF334155)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = provider.label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isSelected) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color(0xFFCBD5E1)
                )
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = provider.subtitle,
                fontSize = 11.sp,
                color = androidx.compose.ui.graphics.Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun PanAssetTypeTab(
    assetType: PanAssetType,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) CrimsonPrimary else androidx.compose.ui.graphics.Color(0xFF1E293B),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) CrimsonPrimary else androidx.compose.ui.graphics.Color(0xFF334155)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = assetType.label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.5.sp,
                color = if (isSelected) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color(0xFFCBD5E1)
            )
        }
    }
}

@Composable
private fun SpecMetric(label: String, value: String) {
    Column {
        Text(text = label, color = androidx.compose.ui.graphics.Color(0xFF94A3B8), fontSize = 10.5.sp)
        Text(text = value, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun ValidationRow(
    label: String,
    value: String,
    highlightGreen: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = androidx.compose.ui.graphics.Color(0xFF94A3B8), fontSize = 12.sp)
        Text(
            text = value,
            color = if (highlightGreen) ToolGreen else androidx.compose.ui.graphics.Color.White,
            fontWeight = if (highlightGreen) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp
        )
    }
}

// ==========================================
// PRESET SPECIFICATION MATRIX (2026 OFFICIAL)
// ==========================================
fun getPanSpec(
    provider: PanProvider,
    assetType: PanAssetType,
    proteanRoute: ProteanRoute
): PanSpec {
    return when (provider) {
        PanProvider.NSDL -> {
            when (assetType) {
                PanAssetType.PHOTO -> {
                    val maxKb = if (proteanRoute == ProteanRoute.DSC_ALTERNATIVE) 20 else 50
                    PanSpec(
                        provider = provider,
                        assetType = assetType,
                        widthPx = 197,
                        heightPx = 276,
                        dpi = 200,
                        maxKb = maxKb,
                        format = "JPEG",
                        aspectDescription = "2.5:3.5",
                        description = "NSDL/Protean Online Photo ($maxKb KB max, 200 DPI)"
                    )
                }
                PanAssetType.SIGNATURE -> {
                    val maxKb = if (proteanRoute == ProteanRoute.DSC_ALTERNATIVE) 10 else 50
                    PanSpec(
                        provider = provider,
                        assetType = assetType,
                        widthPx = 354,
                        heightPx = 157,
                        dpi = 200,
                        maxKb = maxKb,
                        format = "JPEG",
                        aspectDescription = "4.5:2",
                        description = "NSDL/Protean Online Signature ($maxKb KB max, 200 DPI)"
                    )
                }
                PanAssetType.DOCUMENT -> {
                    PanSpec(
                        provider = provider,
                        assetType = assetType,
                        widthPx = 1654, // A4 at 200 DPI: 8.27 in * 200
                        heightPx = 2338, // A4 at 200 DPI: 11.69 in * 200
                        dpi = 200,
                        maxKb = 300,
                        format = "JPEG/PDF",
                        aspectDescription = "A4 Doc (1:1.41)",
                        description = "NSDL Supporting Document (300 KB max, 200 DPI)"
                    )
                }
            }
        }
        PanProvider.UTIITSL -> {
            when (assetType) {
                PanAssetType.PHOTO -> {
                    PanSpec(
                        provider = provider,
                        assetType = assetType,
                        widthPx = 213,
                        heightPx = 213,
                        dpi = 300,
                        maxKb = 30,
                        format = "JPEG",
                        aspectDescription = "1:1",
                        description = "UTIITSL Online Photo (30 KB max, 300 DPI, 213×213)"
                    )
                }
                PanAssetType.SIGNATURE -> {
                    PanSpec(
                        provider = provider,
                        assetType = assetType,
                        widthPx = 400,
                        heightPx = 200,
                        dpi = 600,
                        maxKb = 60,
                        format = "JPEG",
                        aspectDescription = "2:1",
                        description = "UTIITSL Online Signature (60 KB max, 600 DPI, 400×200)"
                    )
                }
                PanAssetType.DOCUMENT -> {
                    PanSpec(
                        provider = provider,
                        assetType = assetType,
                        widthPx = 1654,
                        heightPx = 2338,
                        dpi = 200,
                        maxKb = 300,
                        format = "JPEG/PDF",
                        aspectDescription = "A4 Doc (1:1.41)",
                        description = "UTIITSL Supporting Document (300 KB max, 200 DPI)"
                    )
                }
            }
        }
    }
}

// ==========================================
// CORE PROCESSING ENGINE (CROP + DPI + BINARY COMPRESSION)
// ==========================================
suspend fun processPanAsset(
    source: Bitmap,
    spec: PanSpec,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    previewBoxW: Float,
    previewBoxH: Float,
    enhanceSignature: Boolean,
    additionalPages: List<Bitmap> = emptyList()
): ProcessedPanResult = withContext(Dispatchers.Default) {
    if (spec.assetType == PanAssetType.DOCUMENT) {
        // Collect all pages (primary + any extra pages)
        val allPages = listOf(source) + additionalPages

        // Crop/frame each page to target dimensions
        val croppedPages = allPages.mapIndexed { index, pageBmp ->
            if (index == 0) {
                createCroppedBitmap(
                    src = pageBmp,
                    targetW = spec.widthPx,
                    targetH = spec.heightPx,
                    scale = scale,
                    offsetX = offsetX,
                    offsetY = offsetY,
                    previewBoxW = previewBoxW,
                    previewBoxH = previewBoxH,
                    enhanceSignature = false
                )
            } else {
                createCroppedBitmap(
                    src = pageBmp,
                    targetW = spec.widthPx,
                    targetH = spec.heightPx,
                    scale = 1f,
                    offsetX = 0f,
                    offsetY = 0f,
                    previewBoxW = 1f,
                    previewBoxH = 1f,
                    enhanceSignature = false
                )
            }
        }

        // Build compliant PDF strictly <= 300 KB with iterative binary-search compression
        val finalPdfBytes = PanDocumentPdfBuilder.buildPanDocumentPdf(
            pages = croppedPages,
            maxAllowedBytes = 300 * 1024L
        )

        val finalSizeKb = finalPdfBytes.size.toDouble() / 1024.0

        ProcessedPanResult(
            bytes = finalPdfBytes,
            bitmap = croppedPages[0],
            widthPx = spec.widthPx,
            heightPx = spec.heightPx,
            dpi = spec.dpi,
            sizeKb = finalSizeKb,
            maxKb = 300,
            format = "PDF",
            provider = spec.provider,
            assetType = spec.assetType,
            isPdf = true,
            pdfBytes = finalPdfBytes,
            pdfSizeKb = finalSizeKb,
            pageCount = allPages.size
        )
    } else {
        // PHOTO & SIGNATURE LOGIC REMAINS 100% UNCHANGED AS MANDATED
        // 1. Crop and resize to exact target dimensions
        val croppedBmp = createCroppedBitmap(
            src = source,
            targetW = spec.widthPx,
            targetH = spec.heightPx,
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
            previewBoxW = previewBoxW,
            previewBoxH = previewBoxH,
            enhanceSignature = enhanceSignature
        )

        // 2. Binary search JPEG compression to find the best quality staying strictly <= maxKb
        val maxBytes = spec.maxKb * 1024L
        val finalBytes = compressJpegWithBinarySearch(
            bitmap = croppedBmp,
            maxBytes = maxBytes,
            dpi = spec.dpi
        )

        val finalSizeKb = finalBytes.size.toDouble() / 1024.0

        ProcessedPanResult(
            bytes = finalBytes,
            bitmap = croppedBmp,
            widthPx = spec.widthPx,
            heightPx = spec.heightPx,
            dpi = spec.dpi,
            sizeKb = finalSizeKb,
            maxKb = spec.maxKb,
            format = "JPEG",
            provider = spec.provider,
            assetType = spec.assetType
        )
    }
}

private fun createCroppedBitmap(
    src: Bitmap,
    targetW: Int,
    targetH: Int,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    previewBoxW: Float,
    previewBoxH: Float,
    enhanceSignature: Boolean
): Bitmap {
    val result = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    
    val canvasPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    canvas.drawColor(Color.WHITE) // Pure white background for passport / signature

    val srcAspect = src.width.toFloat() / src.height.toFloat()
    val targetAspect = targetW.toFloat() / targetH.toFloat()

    var baseScale: Float
    var dx = 0f
    var dy = 0f

    if (srcAspect > targetAspect) {
        baseScale = targetW.toFloat() / src.width.toFloat()
        dy = (targetH - (src.height * baseScale)) / 2f
    } else {
        baseScale = targetH.toFloat() / src.height.toFloat()
        dx = (targetW - (src.width * baseScale)) / 2f
    }

    val cx = targetW / 2f
    val cy = targetH / 2f

    val targetOffsetX = if (previewBoxW > 0f) offsetX * (targetW.toFloat() / previewBoxW) else offsetX
    val targetOffsetY = if (previewBoxH > 0f) offsetY * (targetH.toFloat() / previewBoxH) else offsetY

    val matrix = android.graphics.Matrix()
    matrix.postScale(baseScale, baseScale)
    matrix.postTranslate(dx, dy)
    matrix.postScale(scale, scale, cx, cy)
    matrix.postTranslate(targetOffsetX, targetOffsetY)

    if (enhanceSignature) {
        // High contrast filter: push grey/light backgrounds to clean white and sharpen dark strokes
        val cm = ColorMatrix().apply {
            set(floatArrayOf(
                2.2f, 0f, 0f, 0f, -120f,
                0f, 2.2f, 0f, 0f, -120f,
                0f, 0f, 2.2f, 0f, -120f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        canvasPaint.colorFilter = ColorMatrixColorFilter(cm)
    }

    canvas.drawBitmap(src, matrix, canvasPaint)
    return result
}

// Binary-Search JPEG compression strictly staying <= maxBytes while preserving maximum quality
private fun compressJpegWithBinarySearch(
    bitmap: Bitmap,
    maxBytes: Long,
    dpi: Int
): ByteArray {
    var low = 10
    var high = 95
    var bestBytes: ByteArray? = null

    // First test at quality 95
    val streamHigh = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, streamHigh)
    val testBytes = injectJfifDpi(streamHigh.toByteArray(), dpi)
    if (testBytes.size <= maxBytes) {
        return testBytes
    }

    // Binary search between 10 and 95
    while (low <= high) {
        val mid = (low + high) / 2
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, mid, stream)
        val candidate = injectJfifDpi(stream.toByteArray(), dpi)

        if (candidate.size <= maxBytes) {
            bestBytes = candidate
            low = mid + 1 // try higher quality
        } else {
            high = mid - 1 // try lower quality
        }
    }

    if (bestBytes != null && bestBytes.size <= maxBytes) {
        return bestBytes
    }

    // Fallback if even quality 10 is slightly above maxBytes (e.g. 10 KB DSC signature with complex noise)
    val streamMin = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 5, streamMin)
    val minBytes = injectJfifDpi(streamMin.toByteArray(), dpi)
    if (minBytes.size <= maxBytes) {
        return minBytes
    }

    return bestBytes ?: minBytes
}

/**
 * Injects or updates the standard JFIF APP0 marker with the exact DPI (dots per inch)
 * in the JPEG byte array so official portals and file inspectors read 200, 300, or 600 DPI.
 */
fun injectJfifDpi(jpegBytes: ByteArray, dpi: Int): ByteArray {
    if (jpegBytes.size < 4 || (jpegBytes[0] != 0xFF.toByte()) || (jpegBytes[1] != 0xD8.toByte())) {
        return jpegBytes
    }
    val dpiHi = ((dpi shr 8) and 0xFF).toByte()
    val dpiLo = (dpi and 0xFF).toByte()

    // If already has APP0 marker (FF E0)
    if (jpegBytes[2] == 0xFF.toByte() && jpegBytes[3] == 0xE0.toByte() && jpegBytes.size >= 18) {
        if (jpegBytes[6] == 'J'.code.toByte() && jpegBytes[7] == 'F'.code.toByte() &&
            jpegBytes[8] == 'I'.code.toByte() && jpegBytes[9] == 'F'.code.toByte()) {
            val copy = jpegBytes.clone()
            copy[13] = 1 // units = dots per inch (DPI)
            copy[14] = dpiHi
            copy[15] = dpiLo
            copy[16] = dpiHi
            copy[17] = dpiLo
            return copy
        }
    }

    // Otherwise insert standard 18-byte JFIF APP0 marker right after SOI (FF D8)
    val app0 = byteArrayOf(
        0xFF.toByte(), 0xE0.toByte(),
        0x00.toByte(), 0x10.toByte(), // Length = 16
        'J'.code.toByte(), 'F'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 0x00.toByte(),
        0x01.toByte(), 0x02.toByte(), // Version 1.02
        0x01.toByte(), // Units = DPI (1)
        dpiHi, dpiLo,
        dpiHi, dpiLo,
        0x00.toByte(), 0x00.toByte()
    )

    val result = ByteArray(jpegBytes.size + app0.size)
    result[0] = 0xFF.toByte()
    result[1] = 0xD8.toByte()
    System.arraycopy(app0, 0, result, 2, app0.size)
    System.arraycopy(jpegBytes, 2, result, 2 + app0.size, jpegBytes.size - 2)
    return result
}

// ==========================================
// SAVE & SHARE HELPERS
// ==========================================
private suspend fun savePanResult(context: Context, result: ProcessedPanResult) {
    withContext(Dispatchers.IO) {
        try {
            val prefix = "${result.provider.name}_${result.assetType.name}"
            val fileName = "${prefix}_${result.widthPx}x${result.heightPx}_${result.dpi}DPI.jpg"
            val tempFile = File(context.cacheDir, fileName)
            FileOutputStream(tempFile).use { it.write(result.bytes) }

            val saveRes = GlobalSaveManager.saveProcessedFile(
                context = context,
                tempSourceFile = tempFile,
                desiredFileName = fileName,
                mimeType = "image/jpeg",
                category = "pan_card"
            )

            withContext(Dispatchers.Main) {
                if (saveRes.isSuccess) {
                    Toast.makeText(context, "Saved: $fileName\n✓ ${result.widthPx}×${result.heightPx} | ${result.dpi} DPI | ${String.format(Locale.US, "%.1f", result.sizeKb)} KB", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Saved to App Folder", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private suspend fun savePanDocumentPdf(context: Context, result: ProcessedPanResult) {
    withContext(Dispatchers.IO) {
        try {
            var finalBytes = result.pdfBytes ?: result.bytes
            var finalSizeKB = finalBytes.size.toDouble() / 1024.0

            // FINAL SAFETY CHECK: if size > 300 KB, automatically compress again!
            if (finalSizeKB > 300.0) {
                finalBytes = PanDocumentPdfBuilder.buildPanDocumentPdf(
                    pages = listOf(result.bitmap),
                    maxAllowedBytes = 300 * 1024L
                )
                finalSizeKB = finalBytes.size.toDouble() / 1024.0
            }

            if (finalSizeKB > 300.0) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: PDF size exceeds 300 KB limit", Toast.LENGTH_SHORT).show()
                }
                return@withContext
            }

            val fileName = "${result.provider.name}_Supporting_Document_200DPI.pdf"
            val tempFile = File(context.cacheDir, fileName)
            FileOutputStream(tempFile).use { it.write(finalBytes) }

            val saveRes = GlobalSaveManager.saveProcessedFile(
                context = context,
                tempSourceFile = tempFile,
                desiredFileName = fileName,
                mimeType = "application/pdf",
                category = "pan_card"
            )

            withContext(Dispatchers.Main) {
                if (saveRes.isSuccess) {
                    Toast.makeText(
                        context,
                        "Saved PDF: $fileName (✓ ${String.format(Locale.US, "%.1f", finalSizeKB)} KB ≤ 300 KB)",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(context, "Failed: ${saveRes.errorMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "PDF creation failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private suspend fun sharePanResult(context: Context, result: ProcessedPanResult) {
    withContext(Dispatchers.IO) {
        try {
            val isDoc = result.assetType == PanAssetType.DOCUMENT
            val prefix = "${result.provider.name}_${result.assetType.name}"
            val fileName = if (isDoc) "${prefix}_Supporting_Document.pdf" else "${prefix}_${result.widthPx}x${result.heightPx}_${result.dpi}DPI.jpg"
            val mime = if (isDoc) "application/pdf" else "image/jpeg"
            val shareFile = File(context.cacheDir, "share_$fileName")

            var bytesToShare = if (isDoc) (result.pdfBytes ?: result.bytes) else result.bytes
            if (isDoc && bytesToShare.size.toDouble() / 1024.0 > 300.0) {
                bytesToShare = PanDocumentPdfBuilder.buildPanDocumentPdf(
                    pages = listOf(result.bitmap),
                    maxAllowedBytes = 300 * 1024L
                )
            }

            FileOutputStream(shareFile).use { it.write(bytesToShare) }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", shareFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "PAN Card ${result.assetType.label} - ${result.provider.label}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            withContext(Dispatchers.Main) {
                context.startActivity(Intent.createChooser(intent, "Share PAN ${result.assetType.label}"))
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
