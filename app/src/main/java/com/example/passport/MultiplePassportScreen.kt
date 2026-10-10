package com.example.passport

import android.graphics.Bitmap
import android.graphics.ColorMatrix as AndroidColorMatrix
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix as ComposeColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ToolBlue
import com.example.ui.theme.ToolGreen
import com.example.ui.theme.ToolOrange
import kotlin.math.max

/**
 * Multiple Passport Photo Maker Screen.
 *
 * Provides:
 * - Responsive selected photos preview grid/row with index badges (#1, #2...) and remove (✕) buttons.
 * - Clean friendly placeholder when no photos are added.
 * - Studio controls for Crop & Size, Background Settings, and Print Sheet Layout.
 * - Safe 300 DPI preview and export without OutOfMemoryError or crashes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplePassportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MultiplePassportViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Show toast for status messages
    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearStatusMessage()
        }
    }

    // Photo picker for adding photos
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addPhotos(context, uris)
        }
    }

    // Back navigation handling
    BackHandler {
        if (uiState.photos.isNotEmpty() && uiState.activeStudioTab != PassportStudioTab.CROP_SIZE) {
            viewModel.setStudioTab(PassportStudioTab.CROP_SIZE)
        } else {
            viewModel.cleanupSession()
            onBack()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Multiple Passport Photo Maker",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.photos.isNotEmpty() && uiState.activeStudioTab != PassportStudioTab.CROP_SIZE) {
                                viewModel.setStudioTab(PassportStudioTab.CROP_SIZE)
                            } else {
                                viewModel.cleanupSession()
                                onBack()
                            }
                        },
                        modifier = Modifier.testTag("passport_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.photos.isEmpty()) {
                // Friendly placeholder when no photos are added yet
                EmptyPhotosPlaceholder(
                    onAddPhotosClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                // Top selected photo thumbnails ONLY shown on the "Crop & Size" tab
                if (uiState.activeStudioTab == PassportStudioTab.CROP_SIZE) {
                    SelectedPhotosThumbnailRow(
                        photos = uiState.photos,
                        selectedIndex = uiState.selectedPhotoIndex,
                        onSelectPhoto = { idx -> viewModel.selectPhotoIndex(idx) },
                        onDeletePhoto = { photoId -> viewModel.removePhoto(photoId) },
                        onAddMoreClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }

                // Active Studio Tab Content
                Box(modifier = Modifier.weight(1f)) {
                    when (uiState.activeStudioTab) {
                        PassportStudioTab.CROP_SIZE -> {
                            Step2CropAdjustContent(
                                uiState = uiState,
                                onSelectPreset = { preset -> viewModel.setSizePreset(preset) },
                                onCustomSizeChanged = { w, h, u -> viewModel.setCustomSize(w, h, u) },
                                onToggleCustom = { enabled -> viewModel.toggleCustomSize(enabled) },
                                onCropChanged = { id, zoom, px, py -> viewModel.updateCrop(id, zoom, px, py) },
                                onResizeOriginal = { id -> viewModel.resizeOriginal(id) },
                                onAdjustmentChanged = { id, b, c, s -> viewModel.updateAdjustments(id, b, c, s) },
                                onResetAdjustments = { id -> viewModel.resetAdjustments(id) }
                            )
                        }

                        PassportStudioTab.BACKGROUND -> {
                            Step3BackgroundContent(
                                uiState = uiState,
                                onSelectBackground = { option ->
                                    uiState.currentPhoto?.let { photo ->
                                        viewModel.updateBackground(context, photo.id, option)
                                    }
                                },
                                onApplyToAll = { option -> viewModel.applyBackgroundToAll(context, option) }
                            )
                        }

                        PassportStudioTab.PRINT_SHEET -> {
                            Step4LayoutExportContent(
                                uiState = uiState,
                                onPaperChanged = { paper ->
                                    viewModel.setPaperFormat(paper)
                                    viewModel.generateSheets(context)
                                },
                                onCopyDelta = { id, delta -> viewModel.updateCopies(id, delta) },
                                onFillEvenly = {
                                    viewModel.fillEvenly()
                                    viewModel.generateSheets(context)
                                },
                                onBorderToggled = { enabled ->
                                    viewModel.setPhotoBorderEnabled(enabled)
                                    viewModel.generateSheets(context)
                                },
                                onGenerateSheet = { viewModel.generateSheets(context) },
                                onPageChanged = { page -> viewModel.setSheetPage(page) }
                            )
                        }
                    }
                }

                // PINNED BOTTOM NAVIGATION: 3 Category Tabs directly above CTA Action Button
                Surface(
                    shadowElevation = 8.dp,
                    tonalElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Category Tabs: "Crop & Size", "Background", "Print Sheet"
                        PrimaryTabRow(
                            selectedTabIndex = uiState.activeStudioTab.ordinal,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = CrimsonPrimary
                        ) {
                            PassportStudioTab.values().forEach { tab ->
                                Tab(
                                    selected = uiState.activeStudioTab == tab,
                                    onClick = {
                                        viewModel.setStudioTab(tab)
                                        if (tab == PassportStudioTab.PRINT_SHEET && uiState.generatedSheets.isEmpty()) {
                                            viewModel.generateSheets(context)
                                        }
                                    },
                                    text = {
                                        Text(
                                            text = tab.shortLabel,
                                            fontWeight = if (uiState.activeStudioTab == tab) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = when (tab) {
                                                PassportStudioTab.CROP_SIZE -> Icons.Default.Crop
                                                PassportStudioTab.BACKGROUND -> Icons.Default.Palette
                                                PassportStudioTab.PRINT_SHEET -> Icons.Default.Print
                                            },
                                            contentDescription = tab.title,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                                )
                            }
                        }

                        // CTA Action Buttons directly below the 3 tabs
                        when (uiState.activeStudioTab) {
                            PassportStudioTab.CROP_SIZE -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.setStudioTab(PassportStudioTab.BACKGROUND) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("btn_crop_next"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                                    ) {
                                        Text("Next →", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                }
                            }

                            PassportStudioTab.BACKGROUND -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.setStudioTab(PassportStudioTab.CROP_SIZE) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp)
                                            .testTag("btn_bg_back"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Back", fontSize = 14.sp)
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.setStudioTab(PassportStudioTab.PRINT_SHEET)
                                            if (uiState.generatedSheets.isEmpty()) {
                                                viewModel.generateSheets(context)
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1.5f)
                                            .height(50.dp)
                                            .testTag("btn_bg_next"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                                    ) {
                                        Text("Next →", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                }
                            }

                            PassportStudioTab.PRINT_SHEET -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.setStudioTab(PassportStudioTab.BACKGROUND) },
                                        modifier = Modifier.weight(0.9f).height(48.dp).testTag("btn_step4_back"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Back", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = { viewModel.saveAsImage(context) { _, msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() } },
                                        modifier = Modifier.weight(1.1f).height(48.dp).testTag("btn_save_jpg"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ToolBlue)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Save JPG", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { viewModel.saveAsPdf(context) { _, msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() } },
                                        modifier = Modifier.weight(1.1f).height(48.dp).testTag("btn_save_pdf"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Save PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    IconButton(
                                        onClick = { viewModel.printSheet(context) },
                                        modifier = Modifier.size(48.dp).testTag("btn_print_sheet")
                                    ) {
                                        Icon(Icons.Default.Print, contentDescription = "Print", tint = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Processing overlay dialog
            if (uiState.isProcessing) {
                Dialog(onDismissRequest = {}) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = CrimsonPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = uiState.processingMessage.ifBlank { "Processing..." },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// ISSUE 1: CLEAN EMPTY PLACEHOLDER
// -------------------------------------------------------------------------------------------------

@Composable
private fun EmptyPhotosPlaceholder(
    onAddPhotosClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("placeholder_empty_photos"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = CrimsonPrimary.copy(alpha = 0.12f),
                modifier = Modifier.size(96.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "No photos added yet. Tap '+ Add Photos' below to start.",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Select single or multiple photos to crop to standard passport size, adjust backgrounds, and generate A4 or 4×6 print sheets.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onAddPhotosClick,
                modifier = Modifier
                    .height(52.dp)
                    .testTag("btn_empty_add_photos"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("+ Add Photos", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// ISSUE 1: RESPONSIVE SELECTED PHOTOS THUMBNAIL ROW (HEADER REMOVED)
// -------------------------------------------------------------------------------------------------

@Composable
private fun SelectedPhotosThumbnailRow(
    photos: List<PassportPhotoItem>,
    selectedIndex: Int,
    onSelectPhoto: (Int) -> Unit,
    onDeletePhoto: (String) -> Unit,
    onAddMoreClick: () -> Unit
) {
    // ONLY display the horizontal thumbnail selector row right below the top app bar
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .testTag("preview_photos_row")
    ) {
        itemsIndexed(photos, key = { _, item -> item.id }) { index, photo ->
            val isSelected = index == selectedIndex
            SelectedPhotoThumbnailCard(
                photo = photo,
                badgeText = "#${index + 1}",
                isSelected = isSelected,
                onClick = { onSelectPhoto(index) },
                onDelete = { onDeletePhoto(photo.id) }
            )
        }

        // Quick "+ Add" tile at the end
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .size(width = 64.dp, height = 80.dp)
                    .clickable { onAddMoreClick() }
                    .testTag("btn_tile_add_more")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add More", tint = CrimsonPrimary, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("+ Add", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CrimsonPrimary)
                }
            }
        }
    }
}

/**
 * Calculates a reactive ColorFilter based on Brightness, Contrast, and Saturation.
 * Matches PassportSheetRenderer.kt for identical WYSIWYG preview to final print sheet.
 */
private fun createLiveAdjustmentColorFilter(
    brightness: Float,
    contrast: Float,
    saturation: Float
): ColorFilter {
    // If values are around 50 (0..100 scale where 50 is neutral), convert to -100..100
    val (b, c, s) = if (brightness > 20f && contrast > 20f && saturation > 20f &&
        brightness <= 100f && contrast <= 100f && saturation <= 100f
    ) {
        Triple((brightness - 50f) * 2f, (contrast - 50f) * 2f, (saturation - 50f) * 2f)
    } else {
        Triple(brightness, contrast, saturation)
    }

    // 1. Saturation (0 = Normal 1.0f, -100 = B&W 0.0f, +100 = 2.0f)
    val satScale = (1.0f + (s / 100f)).coerceIn(0f, 2.5f)
    val satMatrix = AndroidColorMatrix().apply {
        setSaturation(satScale)
    }

    // 2. Contrast (-100 = 0.3f, 0 = 1.0f, +100 = 2.5f)
    val contrastScale = if (c >= 0f) {
        (1.0f + (c / 100f) * 1.5f).coerceIn(1.0f, 3.0f)
    } else {
        (1.0f + (c / 100f) * 0.7f).coerceIn(0.2f, 1.0f)
    }

    // 3. Brightness shift (-100 = -128, 0 = 0, +100 = +128)
    val brightnessTranslate = (b / 100f) * 128f

    val bcMatrix = AndroidColorMatrix(
        floatArrayOf(
            contrastScale, 0f, 0f, 0f, brightnessTranslate,
            0f, contrastScale, 0f, 0f, brightnessTranslate,
            0f, 0f, contrastScale, 0f, brightnessTranslate,
            0f, 0f, 0f, 1f, 0f
        )
    )

    bcMatrix.postConcat(satMatrix)

    return ColorFilter.colorMatrix(
        ComposeColorMatrix(bcMatrix.array)
    )
}

@Composable
private fun SelectedPhotoThumbnailCard(
    photo: PassportPhotoItem,
    badgeText: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val thumbColorFilter = remember(photo.brightness, photo.contrast, photo.saturation) {
        createLiveAdjustmentColorFilter(photo.brightness, photo.contrast, photo.saturation)
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = if (isSelected) 2.5.dp else 1.dp,
            color = if (isSelected) CrimsonPrimary else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .size(width = 68.dp, height = 82.dp)
            .clickable { onClick() }
            .testTag("photo_thumb_card_${photo.id}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!photo.thumbnailBitmap.isRecycled) {
                Image(
                    bitmap = photo.thumbnailBitmap.asImageBitmap(),
                    contentDescription = photo.displayName,
                    colorFilter = thumbColorFilter,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.LightGray)
                )
            }

            // Top-Left Index Badge (#1, #2, etc.)
            Surface(
                shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 6.dp),
                color = CrimsonPrimary,
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = badgeText,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }

            // Top-Right Delete (✕) button
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier
                    .padding(3.dp)
                    .size(20.dp)
                    .align(Alignment.TopEnd)
                    .clickable { onDelete() }
                    .testTag("btn_delete_photo_${photo.id}")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove photo",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            // Active indicator pill at bottom if selected
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(CrimsonPrimary.copy(alpha = 0.85f))
                        .padding(vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ACTIVE",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// STUDIO TAB 1: CROP, SIZE & ADJUST
// -------------------------------------------------------------------------------------------------

@Composable
private fun Step2CropAdjustContent(
    uiState: MultiplePassportUiState,
    onSelectPreset: (PassportSizePreset) -> Unit,
    onCustomSizeChanged: (String, String, SizeUnit) -> Unit,
    onToggleCustom: (Boolean) -> Unit,
    onCropChanged: (String, Float, Float, Float) -> Unit,
    onResizeOriginal: (String) -> Unit,
    onAdjustmentChanged: (String, Float, Float, Float) -> Unit,
    onResetAdjustments: (String) -> Unit
) {
    val currentPhoto = uiState.currentPhoto ?: return
    val focusManager = LocalFocusManager.current
    var isCustomCropMode by remember(currentPhoto.id) { mutableStateOf(false) }

    val liveColorFilter = remember(currentPhoto.brightness, currentPhoto.contrast, currentPhoto.saturation) {
        createLiveAdjustmentColorFilter(currentPhoto.brightness, currentPhoto.contrast, currentPhoto.saturation)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Card 1: "Passport Photo Size"
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header display: "Passport Photo Size" on left, and current dimensions on right (e.g., "33.0 × 43.0 mm @ 300 DPI")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Passport Photo Size",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.1f", uiState.effectiveWidthMm)} × ${String.format(java.util.Locale.US, "%.1f", uiState.effectiveHeightMm)} mm @ 300 DPI",
                            fontSize = 12.sp,
                            color = CrimsonPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Available Size Presets:
                    // * 3.3 × 4.3 cm (Default selected)
                    // * 3.5 × 4.5 cm
                    // * 1.2 × 1.5 in (Added preset chip)
                    // * Custom (Allow entering custom dimensions in cm, mm, or in)
                    val presetList = listOf(
                        PassportSizePreset.DEFAULT,
                        PassportSizePreset.STANDARD_PASSPORT,
                        PassportSizePreset.INCH_12_15
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(presetList) { preset ->
                            val isSelected = !uiState.isCustomSize && uiState.selectedSizePreset.id == preset.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectPreset(preset) },
                                label = { Text(preset.title) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                modifier = Modifier.testTag("preset_${preset.id}")
                            )
                        }

                        item {
                            FilterChip(
                                selected = uiState.isCustomSize,
                                onClick = { onToggleCustom(true) },
                                label = { Text("Custom") },
                                leadingIcon = if (uiState.isCustomSize) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                modifier = Modifier.testTag("chip_custom_size")
                            )
                        }
                    }

                    // Custom Dimensions Input if enabled
                    AnimatedVisibility(visible = uiState.isCustomSize) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = uiState.customWidthInput,
                                    onValueChange = {
                                        onCustomSizeChanged(it, uiState.customHeightInput, uiState.customUnit)
                                    },
                                    label = { Text("Width") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                    modifier = Modifier.weight(1f).testTag("input_custom_width")
                                )

                                OutlinedTextField(
                                    value = uiState.customHeightInput,
                                    onValueChange = {
                                        onCustomSizeChanged(uiState.customWidthInput, it, uiState.customUnit)
                                    },
                                    label = { Text("Height") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                    modifier = Modifier.weight(1f).testTag("input_custom_height")
                                )

                                // Unit selector chips (cm, mm, in)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    SizeUnit.values().forEach { unit ->
                                        val isUnitSelected = uiState.customUnit == unit
                                        FilterChip(
                                            selected = isUnitSelected,
                                            onClick = {
                                                onCustomSizeChanged(uiState.customWidthInput, uiState.customHeightInput, unit)
                                            },
                                            label = { Text(unit.label, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Card 2: "Align Face & Crop Frame"
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                            text = "Align Face & Crop Frame",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        // Header buttons providing BOTH crop options
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // "Resize Original": Automatically scales and fits the entire subject cleanly inside the frame without empty margins
                            OutlinedButton(
                                onClick = {
                                    isCustomCropMode = false
                                    onResizeOriginal(currentPhoto.id)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp).testTag("btn_resize_original"),
                                shape = RoundedCornerShape(8.dp),
                                colors = if (!isCustomCropMode) {
                                    ButtonDefaults.outlinedButtonColors(
                                        containerColor = CrimsonPrimary.copy(alpha = 0.12f),
                                        contentColor = CrimsonPrimary
                                    )
                                } else {
                                    ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (!isCustomCropMode) CrimsonPrimary else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Icon(Icons.Default.FitScreen, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Resize Original",
                                    fontSize = 11.sp,
                                    fontWeight = if (!isCustomCropMode) FontWeight.Bold else FontWeight.Normal
                                )
                            }

                            // "Custom Crop": Toggles a freeform manual crop frame
                            OutlinedButton(
                                onClick = {
                                    isCustomCropMode = !isCustomCropMode
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp).testTag("btn_custom_crop"),
                                shape = RoundedCornerShape(8.dp),
                                colors = if (isCustomCropMode) {
                                    ButtonDefaults.outlinedButtonColors(
                                        containerColor = CrimsonPrimary.copy(alpha = 0.12f),
                                        contentColor = CrimsonPrimary
                                    )
                                } else {
                                    ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isCustomCropMode) CrimsonPrimary else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Custom Crop",
                                    fontSize = 11.sp,
                                    fontWeight = if (isCustomCropMode) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val cropAspect = (uiState.effectiveWidthMm / uiState.effectiveHeightMm).coerceIn(0.4f, 2.5f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Aspect-ratio locked box
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(0.92f)
                                .aspectRatio(cropAspect)
                                .clipToBounds()
                                .border(2.dp, CrimsonPrimary, RoundedCornerShape(4.dp))
                                .pointerInput(currentPhoto.id) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        val newZoom = (currentPhoto.zoomScale * zoom).coerceIn(1.0f, 4.0f)
                                        val newPanX = currentPhoto.panOffsetX + pan.x
                                        val newPanY = currentPhoto.panOffsetY + pan.y
                                        onCropChanged(currentPhoto.id, newZoom, newPanX, newPanY)
                                    }
                                }
                        ) {
                            if (!currentPhoto.originalBitmap.isRecycled) {
                                Image(
                                    bitmap = currentPhoto.originalBitmap.asImageBitmap(),
                                    contentDescription = "Passport Photo Crop Preview",
                                    colorFilter = liveColorFilter,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clipToBounds()
                                        .graphicsLayer(
                                            scaleX = currentPhoto.zoomScale,
                                            scaleY = currentPhoto.zoomScale,
                                            translationX = currentPhoto.panOffsetX,
                                            translationY = currentPhoto.panOffsetY
                                        )
                                )
                            }

                            // Custom crop grid lines when Custom Crop is toggled on
                            if (isCustomCropMode) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val stepX = size.width / 3f
                                    val stepY = size.height / 3f
                                    val gridColor = Color.White.copy(alpha = 0.35f)
                                    drawLine(gridColor, Offset(stepX, 0f), Offset(stepX, size.height), strokeWidth = 1.dp.toPx())
                                    drawLine(gridColor, Offset(stepX * 2, 0f), Offset(stepX * 2, size.height), strokeWidth = 1.dp.toPx())
                                    drawLine(gridColor, Offset(0f, stepY), Offset(size.width, stepY), strokeWidth = 1.dp.toPx())
                                    drawLine(gridColor, Offset(0f, stepY * 2), Offset(size.width, stepY * 2), strokeWidth = 1.dp.toPx())
                                }
                            }

                            // Subtle Face Alignment Oval Guide
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                                    .border(
                                        1.dp,
                                        Color.White.copy(alpha = 0.45f),
                                        CircleShape
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Pinch to zoom • Drag to align face inside guide",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 3. Live Adjustment Sliders: Brightness, Contrast, Saturation
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Photo Adjustments",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            TextButton(onClick = { onResetAdjustments(currentPhoto.id) }) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset Filters", fontSize = 12.sp)
                            }
                        }

                        AdjustmentSliderRow(
                            title = "Brightness",
                            value = currentPhoto.brightness,
                            onValueChange = { b ->
                                onAdjustmentChanged(currentPhoto.id, b, currentPhoto.contrast, currentPhoto.saturation)
                            }
                        )

                        AdjustmentSliderRow(
                            title = "Contrast",
                            value = currentPhoto.contrast,
                            onValueChange = { c ->
                                onAdjustmentChanged(currentPhoto.id, currentPhoto.brightness, c, currentPhoto.saturation)
                            }
                        )

                        AdjustmentSliderRow(
                            title = "Saturation",
                            value = currentPhoto.saturation,
                            onValueChange = { s ->
                                onAdjustmentChanged(currentPhoto.id, currentPhoto.brightness, currentPhoto.contrast, s)
                            }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
}

@Composable
private fun AdjustmentSliderRow(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text("${value.toInt()}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = -100f..100f,
            colors = SliderDefaults.colors(
                thumbColor = CrimsonPrimary,
                activeTrackColor = CrimsonPrimary
            ),
            modifier = Modifier.height(36.dp)
        )
    }
}

// -------------------------------------------------------------------------------------------------
// STUDIO TAB 2: BACKGROUND SETTINGS
// -------------------------------------------------------------------------------------------------

@Composable
private fun Step3BackgroundContent(
    uiState: MultiplePassportUiState,
    onSelectBackground: (PassportBackgroundOption) -> Unit,
    onApplyToAll: (PassportBackgroundOption) -> Unit
) {
    val currentPhoto = uiState.currentPhoto ?: return
    var showColorPickerDialog by remember { mutableStateOf(false) }

    val liveColorFilter = remember(currentPhoto.brightness, currentPhoto.contrast, currentPhoto.saturation) {
        createLiveAdjustmentColorFilter(currentPhoto.brightness, currentPhoto.contrast, currentPhoto.saturation)
    }

    // Auto-trigger background removal if non-original background is active but no segmented foreground exists
    LaunchedEffect(currentPhoto.id, currentPhoto.backgroundOption.type) {
        if (currentPhoto.backgroundOption.type != BackgroundStyleType.ORIGINAL &&
            (currentPhoto.segmentedForeground == null || currentPhoto.segmentedForeground.isRecycled)
        ) {
            onSelectBackground(currentPhoto.backgroundOption)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Preview of Photo with Background
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Background Style Preview",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val cropAspect = (uiState.effectiveWidthMm / uiState.effectiveHeightMm).coerceIn(0.5f, 2.0f)
                    val bgOption = currentPhoto.backgroundOption
                    val isSegmenting = uiState.isProcessing && bgOption.type != BackgroundStyleType.ORIGINAL &&
                            (currentPhoto.segmentedForeground == null || currentPhoto.segmentedForeground.isRecycled)

                    Box(
                        modifier = Modifier
                            .height(260.dp)
                            .aspectRatio(cropAspect)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .then(
                                when (bgOption.type) {
                                    BackgroundStyleType.ORIGINAL -> Modifier.background(Color.White)
                                    BackgroundStyleType.SOLID -> Modifier.background(bgOption.solidColor)
                                    BackgroundStyleType.GRADIENT -> Modifier.background(
                                        Brush.verticalGradient(
                                            listOf(bgOption.gradientColors.first, bgOption.gradientColors.second)
                                        )
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Choose segmented transparent foreground when non-original background is active,
                        // otherwise original bitmap
                        val activeDisplayBitmap = when {
                            bgOption.type == BackgroundStyleType.ORIGINAL -> currentPhoto.originalBitmap
                            currentPhoto.segmentedForeground != null && !currentPhoto.segmentedForeground.isRecycled -> currentPhoto.segmentedForeground
                            else -> currentPhoto.originalBitmap
                        }

                        if (!activeDisplayBitmap.isRecycled) {
                            Image(
                                bitmap = activeDisplayBitmap.asImageBitmap(),
                                contentDescription = "Photo with ${bgOption.name} background",
                                colorFilter = liveColorFilter,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clipToBounds()
                                    .graphicsLayer(
                                        scaleX = currentPhoto.zoomScale,
                                        scaleY = currentPhoto.zoomScale,
                                        translationX = currentPhoto.panOffsetX,
                                        translationY = currentPhoto.panOffsetY
                                    )
                            )
                        }

                        // Subtle loading badge while AI background removal is in progress
                        if (isSegmenting) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Removing background...",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Active Style: ${bgOption.name}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CrimsonPrimary
                    )
                }
            }
        }

            // Background Style Options
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Choose Background Color",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )

                            Button(
                                onClick = { onApplyToAll(currentPhoto.backgroundOption) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ToolBlue),
                                modifier = Modifier.testTag("btn_apply_bg_all")
                            ) {
                                Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Apply to All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Standard Passport Colors", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            BackgroundTileChip(
                                title = "Original",
                                color = Color.LightGray,
                                isSelected = currentPhoto.backgroundOption.id == PassportBackgroundOption.ORIGINAL.id,
                                onClick = { onSelectBackground(PassportBackgroundOption.ORIGINAL) }
                            )

                            BackgroundTileChip(
                                title = "White",
                                color = Color.White,
                                isSelected = currentPhoto.backgroundOption.id == PassportBackgroundOption.WHITE.id,
                                onClick = { onSelectBackground(PassportBackgroundOption.WHITE) }
                            )

                            BackgroundTileChip(
                                title = "Light Blue",
                                color = PassportBackgroundOption.LIGHT_BLUE.solidColor,
                                isSelected = currentPhoto.backgroundOption.id == PassportBackgroundOption.LIGHT_BLUE.id,
                                onClick = { onSelectBackground(PassportBackgroundOption.LIGHT_BLUE) }
                            )

                            BackgroundTileChip(
                                title = "Soft Gray",
                                color = PassportBackgroundOption.SOFT_GRAY.solidColor,
                                isSelected = currentPhoto.backgroundOption.id == PassportBackgroundOption.SOFT_GRAY.id,
                                onClick = { onSelectBackground(PassportBackgroundOption.SOFT_GRAY) }
                            )

                            OutlinedButton(
                                onClick = { showColorPickerDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(48.dp).testTag("btn_custom_color")
                            ) {
                                Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Custom Color", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("Studio Gradient Styles", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            GradientTileChip(
                                title = "Studio Gray",
                                colors = PassportBackgroundOption.GRADIENT_STUDIO.gradientColors,
                                isSelected = currentPhoto.backgroundOption.id == PassportBackgroundOption.GRADIENT_STUDIO.id,
                                onClick = { onSelectBackground(PassportBackgroundOption.GRADIENT_STUDIO) }
                            )

                            GradientTileChip(
                                title = "Soft Blue",
                                colors = PassportBackgroundOption.GRADIENT_BLUE.gradientColors,
                                isSelected = currentPhoto.backgroundOption.id == PassportBackgroundOption.GRADIENT_BLUE.id,
                                onClick = { onSelectBackground(PassportBackgroundOption.GRADIENT_BLUE) }
                            )

                            GradientTileChip(
                                title = "Slate Gray",
                                colors = PassportBackgroundOption.GRADIENT_SLATE.gradientColors,
                                isSelected = currentPhoto.backgroundOption.id == PassportBackgroundOption.GRADIENT_SLATE.id,
                                onClick = { onSelectBackground(PassportBackgroundOption.GRADIENT_SLATE) }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }

    if (showColorPickerDialog) {
        CustomColorPaletteDialog(
            onDismiss = { showColorPickerDialog = false },
            onColorSelected = { chosenColor ->
                val customOption = PassportBackgroundOption(
                    id = "custom_${System.currentTimeMillis()}",
                    name = "Custom Color",
                    type = BackgroundStyleType.SOLID,
                    solidColor = chosenColor
                )
                onSelectBackground(customOption)
                showColorPickerDialog = false
            }
        )
    }
}

@Composable
private fun BackgroundTileChip(
    title: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 2.5.dp else 1.dp,
            color = if (isSelected) CrimsonPrimary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .clickable { onClick() }
            .padding(2.dp)
            .testTag("bg_chip_${title.replace(" ", "_")}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = color,
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
                modifier = Modifier.size(20.dp)
            ) {}
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable
private fun GradientTileChip(
    title: String,
    colors: Pair<Color, Color>,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 2.5.dp else 1.dp,
            color = if (isSelected) CrimsonPrimary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .clickable { onClick() }
            .padding(2.dp)
            .testTag("bg_grad_${title.replace(" ", "_")}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(colors.first, colors.second)))
                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable
private fun CustomColorPaletteDialog(
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    val palette = listOf(
        Color(0xFFFFFFFF), Color(0xFFF0F4F8), Color(0xFFD9E2EC),
        Color(0xFFA2C8EC), Color(0xFF64B5F6), Color(0xFF1976D2),
        Color(0xFFE8F5E9), Color(0xFFA5D6A7), Color(0xFF4CAF50),
        Color(0xFFFFF3E0), Color(0xFFFFCC80), Color(0xFFFF9800),
        Color(0xFFF3E5F5), Color(0xFFCE93D8), Color(0xFF9C27B0),
        Color(0xFFECEFF1), Color(0xFFB0BEC5), Color(0xFF37474F)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Custom Background Color", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column {
                Text("Select from standard passport studio backgrounds:", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(14.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(160.dp)
                ) {
                    gridItems(palette) { color ->
                        Surface(
                            shape = CircleShape,
                            color = color,
                            border = BorderStroke(1.dp, Color.Gray),
                            modifier = Modifier
                                .size(38.dp)
                                .clickable { onColorSelected(color) }
                        ) {}
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// -------------------------------------------------------------------------------------------------
// STUDIO TAB 3: PRINT SHEET LAYOUT & EXPORT (ISSUE 2 FIX: NO OOM / NO CRASH)
// -------------------------------------------------------------------------------------------------

@Composable
private fun Step4LayoutExportContent(
    uiState: MultiplePassportUiState,
    onPaperChanged: (PaperSheetFormat) -> Unit,
    onCopyDelta: (String, Int) -> Unit,
    onFillEvenly: () -> Unit,
    onBorderToggled: (Boolean) -> Unit,
    onGenerateSheet: () -> Unit,
    onPageChanged: (Int) -> Unit
) {
    val grid = PassportSheetRenderer.computeGrid(
        uiState.selectedPaper,
        uiState.effectiveWidthMm,
        uiState.effectiveHeightMm,
        targetDpi = 300
    )

    val maxCapacity = grid.maxCapacity
    val totalAllocated = uiState.totalCopiesAllocated

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Paper Size Selector
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Select Paper Size & Orientation",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PaperSheetFormat.values().forEach { paper ->
                            val isSelected = uiState.selectedPaper == paper
                            FilterChip(
                                selected = isSelected,
                                onClick = { onPaperChanged(paper) },
                                label = { Text(paper.title, fontSize = 12.sp) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                modifier = Modifier.weight(1f).testTag("paper_${paper.id}")
                            )
                        }
                    }
                }
            }
        }

        // Photo Copies Allocation Section (Compact, direct counter list without dead space)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Photo Copies Allocation",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Total Selected: $totalAllocated / Max Sheet Capacity: $maxCapacity",
                                fontSize = 12.sp,
                                color = if (totalAllocated > maxCapacity) ToolOrange else CrimsonPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = onFillEvenly,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                            modifier = Modifier.height(34.dp).testTag("btn_fill_evenly")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fill Evenly", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(4.dp))

                    // Direct compact photo copy counter items: Thumbnail, #1 Photo 1, [-], count, [+]
                    uiState.photos.forEachIndexed { index, photo ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    if (!photo.thumbnailBitmap.isRecycled) {
                                        Image(
                                            bitmap = photo.thumbnailBitmap.asImageBitmap(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = CrimsonPrimary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "#${index + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CrimsonPrimary,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = photo.displayName.ifBlank { "Photo ${index + 1}" },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilledIconButton(
                                    onClick = { onCopyDelta(photo.id, -1) },
                                    shape = CircleShape,
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.size(30.dp).testTag("btn_minus_${photo.id}")
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease copies", modifier = Modifier.size(15.dp))
                                }

                                Text(
                                    text = "${photo.copiesCount}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                )

                                FilledIconButton(
                                    onClick = { onCopyDelta(photo.id, 1) },
                                    shape = CircleShape,
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = CrimsonPrimary),
                                    modifier = Modifier.size(30.dp).testTag("btn_plus_${photo.id}")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase copies", tint = Color.White, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

            // Sheet Photo Border Options (Clean single toggle)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Photo Border",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Switch(
                                checked = uiState.isPhotoBorderEnabled,
                                onCheckedChange = { onBorderToggled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CrimsonPrimary,
                                    checkedTrackColor = CrimsonPrimary.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.testTag("switch_photo_border")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // SAFE GENERATE / REFRESH SHEET BUTTON
                        Button(
                            onClick = onGenerateSheet,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_generate_sheet"),
                            colors = ButtonDefaults.buttonColors(containerColor = ToolBlue)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate / Refresh Sheet", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // High-Resolution Sheet Preview (Interactive Pinch-to-Zoom & Drag-to-Pan)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF262626)),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Print Sheet Preview",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (uiState.generatedSheets.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = CrimsonPrimary.copy(alpha = 0.25f),
                                    border = BorderStroke(0.5.dp, CrimsonPrimary.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "Pinch / Pan 1x - 5x",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (uiState.generatedSheets.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Pinch to zoom • Drag to pan • Double-tap to reset",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 11.sp,
                                modifier = Modifier.align(Alignment.Start)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (uiState.generatedSheets.isNotEmpty()) {
                            val activeSheet = uiState.generatedSheets.getOrElse(uiState.currentSheetPage) { uiState.generatedSheets.first() }
                            InteractiveSheetPreview(
                                sheetBitmap = activeSheet,
                                pageNumber = uiState.currentSheetPage + 1
                            )

                            // Multi-page switcher if more than 1 page
                            if (uiState.generatedSheets.size > 1) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    IconButton(
                                        onClick = { onPageChanged(uiState.currentSheetPage - 1) },
                                        enabled = uiState.currentSheetPage > 0
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Prev Page", tint = Color.White)
                                    }
                                    Text(
                                        text = "Page ${uiState.currentSheetPage + 1} of ${uiState.generatedSheets.size}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    IconButton(
                                        onClick = { onPageChanged(uiState.currentSheetPage + 1) },
                                        enabled = uiState.currentSheetPage < uiState.generatedSheets.size - 1
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Next Page", tint = Color.White)
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        "Tap 'Generate / Refresh Sheet' to render sheet",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
}

/**
 * Interactive pinch-to-zoom and draggable print sheet preview container.
 * Supports smooth 1x to 5x scaling, bounded translation clamping, and double-tap toggle.
 */
@Composable
private fun InteractiveSheetPreview(
    sheetBitmap: Bitmap,
    pageNumber: Int,
    modifier: Modifier = Modifier
) {
    var scale by remember(sheetBitmap) { mutableFloatStateOf(1f) }
    var offsetX by remember(sheetBitmap) { mutableFloatStateOf(0f) }
    var offsetY by remember(sheetBitmap) { mutableFloatStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val sheetAspect = (sheetBitmap.width.toFloat() / sheetBitmap.height.toFloat()).coerceIn(0.4f, 2.5f)

    Surface(
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 8.dp,
        color = Color.White,
        modifier = modifier
            .fillMaxWidth(0.88f)
            .aspectRatio(sheetAspect)
            .clip(RoundedCornerShape(8.dp))
            .clipToBounds()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .onSizeChanged { containerSize = it }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1.2f) {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                scale = 2.5f
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                        scale = newScale

                        if (newScale <= 1.05f) {
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            val maxPanX = (containerSize.width * (newScale - 1f)) / 2f
                            val maxPanY = (containerSize.height * (newScale - 1f)) / 2f
                            offsetX = (offsetX + pan.x).coerceIn(-maxPanX, maxPanX)
                            offsetY = (offsetY + pan.y).coerceIn(-maxPanY, maxPanY)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (!sheetBitmap.isRecycled) {
                Image(
                    bitmap = sheetBitmap.asImageBitmap(),
                    contentDescription = "Sheet Page $pageNumber",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        },
                    contentScale = ContentScale.Fit
                )
            }

            // Quick reset button when zoomed in
            if (scale > 1.1f) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clickable {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ZoomOutMap,
                            contentDescription = "Reset Zoom",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.1f", scale)}x (Reset)",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
