package com.example.passport

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.print.PrintHelper
import com.example.photo.PhotoEditorEngine
import com.example.photo.birefnet.BiRefNetLiteEngine
import com.example.storage.GlobalSaveManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * UI State for Multiple Passport Photo Maker.
 */
data class MultiplePassportUiState(
    val currentStep: PassportFlowStep = PassportFlowStep.STEP_1_UPLOAD,
    val activeStudioTab: PassportStudioTab = PassportStudioTab.CROP_SIZE,
    val photos: List<PassportPhotoItem> = emptyList(),
    val selectedPhotoIndex: Int = 0,
    // Size settings
    val selectedSizePreset: PassportSizePreset = PassportSizePreset.DEFAULT,
    val isCustomSize: Boolean = false,
    val customWidthInput: String = "3.3",
    val customHeightInput: String = "4.3",
    val customUnit: SizeUnit = SizeUnit.CM,
    // Background global selection
    val activeBackgroundOption: PassportBackgroundOption = PassportBackgroundOption.ORIGINAL,
    // Sheet layout settings
    val selectedPaper: PaperSheetFormat = PaperSheetFormat.A4,
    val isPhotoBorderEnabled: Boolean = true,
    val borderColor: Color = Color.Black,
    // Output sheet bitmaps & preview
    val generatedSheets: List<Bitmap> = emptyList(),
    val currentSheetPage: Int = 0,
    // Loading / Progress
    val isProcessing: Boolean = false,
    val processingMessage: String = "",
    val statusMessage: String? = null
) {
    /**
     * Effective photo width in millimeters.
     */
    val effectiveWidthMm: Float
        get() {
            if (!isCustomSize) return selectedSizePreset.widthMm
            val w = customWidthInput.toFloatOrNull() ?: 33.0f
            return w * customUnit.toMmMultiplier
        }

    /**
     * Effective photo height in millimeters.
     */
    val effectiveHeightMm: Float
        get() {
            if (!isCustomSize) return selectedSizePreset.heightMm
            val h = customHeightInput.toFloatOrNull() ?: 43.0f
            return h * customUnit.toMmMultiplier
        }

    val currentPhoto: PassportPhotoItem?
        get() = photos.getOrNull(selectedPhotoIndex)

    val totalCopiesAllocated: Int
        get() = photos.sumOf { it.copiesCount }
}

/**
 * ViewModel managing the state machine and workflows for Multiple Passport Photo Maker.
 */
class MultiplePassportViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MultiplePassportUiState())
    val uiState: StateFlow<MultiplePassportUiState> = _uiState.asStateFlow()

    private val TAG = "MultiplePassportVM"

    /**
     * Add multiple photos from Uri selection.
     */
    fun addPhotos(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingMessage = "Loading ${uris.size} photo(s)...") }

            val newItems = mutableListOf<PassportPhotoItem>()
            withContext(Dispatchers.IO) {
                uris.forEachIndexed { index, uri ->
                    try {
                        val bmp = decodeSampledBitmapFromUri(context, uri, reqWidth = 1200, reqHeight = 1200)
                        if (bmp != null) {
                            val thumb = createSquareThumbnail(bmp, 240)
                            newItems.add(
                                PassportPhotoItem(
                                    sourceUri = uri,
                                    displayName = "Photo ${_uiState.value.photos.size + index + 1}",
                                    originalBitmap = bmp,
                                    thumbnailBitmap = thumb,
                                    copiesCount = 1
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed loading image: ${e.message}", e)
                    }
                }
            }

            _uiState.update { current ->
                val updatedPhotos = current.photos + newItems
                val nextSelectedIdx = if (current.photos.isEmpty() && newItems.isNotEmpty()) 0 else current.selectedPhotoIndex
                current.copy(
                    photos = updatedPhotos,
                    selectedPhotoIndex = nextSelectedIdx,
                    currentStep = if (updatedPhotos.isNotEmpty()) PassportFlowStep.STEP_2_CROP_ADJUST else current.currentStep,
                    isProcessing = false,
                    processingMessage = "",
                    statusMessage = if (newItems.isNotEmpty()) "Added ${newItems.size} photo(s)" else "Could not load selected image(s)"
                )
            }
        }
    }

    /**
     * Remove a single photo by id.
     */
    fun removePhoto(photoId: String) {
        _uiState.update { current ->
            val itemToRemove = current.photos.find { it.id == photoId }
            try { if (itemToRemove?.thumbnailBitmap?.isRecycled == false) itemToRemove.thumbnailBitmap.recycle() } catch (_: Exception) {}
            try { if (itemToRemove?.originalBitmap?.isRecycled == false) itemToRemove.originalBitmap.recycle() } catch (_: Exception) {}
            try { if (itemToRemove?.segmentedForeground?.isRecycled == false) itemToRemove.segmentedForeground?.recycle() } catch (_: Exception) {}

            val updated = current.photos.filterNot { it.id == photoId }
            val nextIdx = current.selectedPhotoIndex.coerceAtMost(max(0, updated.size - 1))
            current.copy(
                photos = updated,
                selectedPhotoIndex = nextIdx,
                generatedSheets = emptyList(), // Invalidate old sheet on photo removal
                currentStep = if (updated.isEmpty()) PassportFlowStep.STEP_1_UPLOAD else current.currentStep
            )
        }
    }

    /**
     * Switch currently selected photo index.
     */
    fun selectPhotoIndex(index: Int) {
        _uiState.update { current ->
            val safeIdx = index.coerceIn(0, max(0, current.photos.size - 1))
            current.copy(selectedPhotoIndex = safeIdx)
        }
    }

    /**
     * Switch active studio tab (Crop & Size, Background, Print Sheet).
     */
    fun setStudioTab(tab: PassportStudioTab) {
        _uiState.update { it.copy(activeStudioTab = tab) }
    }

    /**
     * Select a preset photo size.
     */
    fun setSizePreset(preset: PassportSizePreset) {
        _uiState.update {
            it.copy(
                selectedSizePreset = preset,
                isCustomSize = false
            )
        }
    }

    /**
     * Toggle or activate custom size mode.
     */
    fun setCustomSize(widthStr: String, heightStr: String, unit: SizeUnit) {
        _uiState.update {
            it.copy(
                isCustomSize = true,
                customWidthInput = widthStr,
                customHeightInput = heightStr,
                customUnit = unit
            )
        }
    }

    /**
     * Enable/disable custom size mode.
     */
    fun toggleCustomSize(enabled: Boolean) {
        _uiState.update { it.copy(isCustomSize = enabled) }
    }

    /**
     * Update crop zoom and pan offset for current photo.
     */
    fun updateCrop(photoId: String, zoom: Float, panX: Float, panY: Float) {
        _uiState.update { current ->
            val updated = current.photos.map { item ->
                if (item.id == photoId) {
                    item.copy(
                        zoomScale = zoom.coerceIn(1.0f, 4.0f),
                        panOffsetX = panX,
                        panOffsetY = panY
                    )
                } else item
            }
            current.copy(photos = updated)
        }
    }

    /**
     * "Resize Original" button: auto-fits the critical photo content inside the frame.
     */
    fun resizeOriginal(photoId: String) {
        _uiState.update { current ->
            val updated = current.photos.map { item ->
                if (item.id == photoId) {
                    item.copy(
                        zoomScale = 1.0f,
                        panOffsetX = 0f,
                        panOffsetY = 0f
                    )
                } else item
            }
            current.copy(photos = updated)
        }
    }

    /**
     * Live adjustment sliders: Brightness, Contrast, Saturation.
     */
    fun updateAdjustments(photoId: String, brightness: Float, contrast: Float, saturation: Float) {
        _uiState.update { current ->
            val updated = current.photos.map { item ->
                if (item.id == photoId) {
                    item.copy(
                        brightness = brightness.coerceIn(-100f, 100f),
                        contrast = contrast.coerceIn(-100f, 100f),
                        saturation = saturation.coerceIn(-100f, 100f)
                    )
                } else item
            }
            current.copy(photos = updated)
        }
    }

    /**
     * Reset adjustments to 0.
     */
    fun resetAdjustments(photoId: String) {
        updateAdjustments(photoId, 0f, 0f, 0f)
    }

    /**
     * Update background option for a specific photo with instant live preview.
     * Caches the AI-segmented subject mask so subsequent color taps render immediately.
     */
     fun updateBackground(context: Context, photoId: String, option: PassportBackgroundOption) {
        val targetPhoto = _uiState.value.photos.find { it.id == photoId } ?: return

        // 1. Immediately update backgroundOption so the UI state / preview card updates instantly
        _uiState.update { current ->
            val updated = current.photos.map { item ->
                if (item.id == photoId) {
                    item.copy(backgroundOption = option)
                } else item
            }
            current.copy(
                photos = updated,
                activeBackgroundOption = option
            )
        }

        // 2. If Original background is selected, no segmentation required
        if (option.type == BackgroundStyleType.ORIGINAL) {
            return
        }

        // 3. If photo already has cached segmentedForeground (not recycled), segmentation is already done! Instant update!
        if (targetPhoto.segmentedForeground != null && !targetPhoto.segmentedForeground.isRecycled) {
            return
        }

        // 4. Run instant background removal + AI background refinement in background
        viewModelScope.launch {
            // Step 4a: Fast foreground extraction (< 50-80ms) for instant UI live preview
            val fastForeground = withContext(Dispatchers.Default) {
                try {
                    if (!targetPhoto.originalBitmap.isRecycled) {
                        PhotoEditorEngine.removeBackground(targetPhoto.originalBitmap)
                    } else null
                } catch (t: Throwable) {
                    Log.w(TAG, "Fast background removal error: ${t.message}")
                    null
                }
            }

            if (fastForeground != null && !fastForeground.isRecycled) {
                _uiState.update { current ->
                    val updated = current.photos.map { item ->
                        if (item.id == photoId) {
                            item.copy(segmentedForeground = fastForeground)
                        } else item
                    }
                    current.copy(photos = updated)
                }
            }

            // Step 4b: High-precision BiRefNet AI mask refinement in background
            _uiState.update { it.copy(isProcessing = true, processingMessage = "Refining background...") }
            try {
                val aiBmp = withContext(Dispatchers.Default) {
                    if (!targetPhoto.originalBitmap.isRecycled) {
                        try {
                            val res = BiRefNetLiteEngine.removeBackground(context, targetPhoto.originalBitmap)
                            res.getOrNull()?.transparentForeground
                        } catch (t: Throwable) {
                            Log.w(TAG, "BiRefNetLiteEngine exception: ${t.message}")
                            null
                        }
                    } else null
                }

                _uiState.update { current ->
                    val updated = current.photos.map { item ->
                        if (item.id == photoId) {
                            item.copy(
                                segmentedForeground = if (aiBmp != null && !aiBmp.isRecycled) aiBmp else (fastForeground ?: item.segmentedForeground)
                            )
                        } else item
                    }
                    current.copy(
                        photos = updated,
                        isProcessing = false,
                        processingMessage = ""
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refining background: ${e.message}", e)
                _uiState.update { it.copy(isProcessing = false, processingMessage = "") }
            }
        }
    }

    /**
     * Update background option for a specific photo (synchronous fallback).
     */
    fun updateBackground(photoId: String, option: PassportBackgroundOption) {
        _uiState.update { current ->
            val updated = current.photos.map { item ->
                if (item.id == photoId) {
                    item.copy(backgroundOption = option)
                } else item
            }
            current.copy(
                photos = updated,
                activeBackgroundOption = option
            )
        }
    }

    /**
     * "Apply to All": applies chosen background option to all selected photos and caches segmentation.
     */
    fun applyBackgroundToAll(context: Context, option: PassportBackgroundOption) {
        _uiState.update { current ->
            val updated = current.photos.map { it.copy(backgroundOption = option) }
            current.copy(
                photos = updated,
                activeBackgroundOption = option,
                statusMessage = "Applied '${option.name}' background to all ${updated.size} photos"
            )
        }

        if (option.type == BackgroundStyleType.ORIGINAL) return

        val needSegmentation = _uiState.value.photos.filter {
            (it.segmentedForeground == null || it.segmentedForeground.isRecycled) && !it.originalBitmap.isRecycled
        }
        if (needSegmentation.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingMessage = "Processing background for all photos...") }
            // Fast pass first for immediate UI response
            withContext(Dispatchers.Default) {
                for (photo in needSegmentation) {
                    try {
                        val fastBmp = PhotoEditorEngine.removeBackground(photo.originalBitmap)
                        _uiState.update { current ->
                            val updated = current.photos.map { item ->
                                if (item.id == photo.id && (item.segmentedForeground == null || item.segmentedForeground.isRecycled)) {
                                    item.copy(segmentedForeground = fastBmp)
                                } else item
                            }
                            current.copy(photos = updated)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Fast background removal failed for photo ${photo.id}: ${e.message}")
                    }
                }
            }

            // AI refinement pass
            try {
                withContext(Dispatchers.Default) {
                    for (photo in needSegmentation) {
                        try {
                            val res = BiRefNetLiteEngine.removeBackground(context, photo.originalBitmap)
                            val segBmp = res.getOrNull()?.transparentForeground
                            if (segBmp != null && !segBmp.isRecycled) {
                                _uiState.update { current ->
                                    val updated = current.photos.map { item ->
                                        if (item.id == photo.id) {
                                            item.copy(segmentedForeground = segBmp)
                                        } else item
                                    }
                                    current.copy(photos = updated)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error segmenting photo ${photo.id}: ${e.message}")
                        }
                    }
                }
            } finally {
                _uiState.update { it.copy(isProcessing = false, processingMessage = "") }
            }
        }
    }

    /**
     * "Apply to All": applies chosen background option to all selected photos.
     */
    fun applyBackgroundToAll(option: PassportBackgroundOption) {
        _uiState.update { current ->
            val updated = current.photos.map { it.copy(backgroundOption = option) }
            current.copy(
                photos = updated,
                activeBackgroundOption = option,
                statusMessage = "Applied '${option.name}' background to all ${updated.size} photos"
            )
        }
    }

    /**
     * Increment or decrement copy count for a photo.
     */
    fun updateCopies(photoId: String, delta: Int) {
        _uiState.update { current ->
            val updated = current.photos.map { item ->
                if (item.id == photoId) {
                    val nextCount = (item.copiesCount + delta).coerceIn(0, 100)
                    item.copy(copiesCount = nextCount)
                } else item
            }
            current.copy(photos = updated)
        }
    }

    /**
     * Set explicit copies count.
     */
    fun setCopies(photoId: String, count: Int) {
        _uiState.update { current ->
            val updated = current.photos.map { item ->
                if (item.id == photoId) {
                    item.copy(copiesCount = count.coerceIn(0, 100))
                } else item
            }
            current.copy(photos = updated)
        }
    }

    /**
     * Set paper sheet format.
     */
    fun setPaperFormat(paper: PaperSheetFormat) {
        _uiState.update { it.copy(selectedPaper = paper) }
    }

    /**
     * Toggle photo border (fixed 2px when enabled, 0px when disabled).
     */
    fun setPhotoBorderEnabled(enabled: Boolean) {
        _uiState.update {
            it.copy(
                isPhotoBorderEnabled = enabled,
                generatedSheets = emptyList()
            )
        }
    }

    /**
     * "Fill Evenly" button: Distributes copies equally among selected photos to reach max capacity.
     */
    fun fillEvenly() {
        val state = _uiState.value
        val photos = state.photos
        if (photos.isEmpty()) return

        val grid = PassportSheetRenderer.computeGrid(
            state.selectedPaper,
            state.effectiveWidthMm,
            state.effectiveHeightMm,
            targetDpi = 300
        )
        val capacity = grid.maxCapacity
        val numPhotos = photos.size
        val baseCount = capacity / numPhotos
        val remainder = capacity % numPhotos

        _uiState.update { current ->
            val updated = current.photos.mapIndexed { index, item ->
                val extra = if (index < remainder) 1 else 0
                item.copy(copiesCount = (baseCount + extra).coerceAtLeast(1))
            }
            current.copy(
                photos = updated,
                statusMessage = "Evenly distributed $capacity copies among $numPhotos photo(s)"
            )
        }
    }

    /**
     * Generate high-resolution sheet preview safely on background coroutines.
     * Prevents OOM by rendering downscaled preview bitmaps and never recycling bitmaps in active Compose state.
     */
    fun generateSheets(context: Context) {
        val state = _uiState.value
        if (state.photos.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "Please add at least 1 photo first") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    processingMessage = "Rendering sheet preview..."
                )
            }

            try {
                // Render on Dispatchers.Default with preview resolution
                val sheets = withContext(Dispatchers.Default) {
                    PassportSheetRenderer.renderPrintSheets(
                        context = context,
                        photos = state.photos,
                        paper = state.selectedPaper,
                        photoWidthMm = state.effectiveWidthMm,
                        photoHeightMm = state.effectiveHeightMm,
                        borderEnabled = state.isPhotoBorderEnabled,
                        borderColor = state.borderColor,
                        isPreview = true
                    )
                }

                _uiState.update {
                    it.copy(
                        generatedSheets = sheets,
                        currentSheetPage = 0,
                        isProcessing = false,
                        processingMessage = "",
                        statusMessage = "Sheet preview generated successfully"
                    )
                }
            } catch (oom: OutOfMemoryError) {
                System.gc()
                Log.e(TAG, "OutOfMemoryError in generateSheets", oom)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        processingMessage = "",
                        statusMessage = "Device memory low. Please try with fewer photos or copies."
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in generateSheets: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        processingMessage = "",
                        statusMessage = "Could not render sheet: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun setSheetPage(page: Int) {
        _uiState.update { current ->
            val maxPage = max(0, current.generatedSheets.size - 1)
            current.copy(currentSheetPage = page.coerceIn(0, maxPage))
        }
    }

    /**
     * Export as vector print-ready PDF document.
     * Generates full 300 DPI sheet in background, writes to file, and frees memory.
     */
    fun saveAsPdf(context: Context, onComplete: (Boolean, String) -> Unit) {
        val state = _uiState.value
        if (state.photos.isEmpty()) {
            onComplete(false, "Please select at least 1 photo")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingMessage = "Creating 300 DPI vector PDF...") }
            try {
                val fullSheets = withContext(Dispatchers.Default) {
                    PassportSheetRenderer.renderPrintSheets(
                        context = context,
                        photos = state.photos,
                        paper = state.selectedPaper,
                        photoWidthMm = state.effectiveWidthMm,
                        photoHeightMm = state.effectiveHeightMm,
                        borderEnabled = state.isPhotoBorderEnabled,
                        borderColor = state.borderColor,
                        isPreview = false
                    )
                }

                val tempPdfFile = PassportSheetRenderer.createPdf(
                    context = context,
                    sheets = fullSheets,
                    paper = state.selectedPaper
                )

                // Clean up local fullSheets safely
                fullSheets.forEach { try { it.recycle() } catch (_: Exception) {} }

                val result = GlobalSaveManager.saveProcessedFile(
                    context = context,
                    tempSourceFile = tempPdfFile,
                    desiredFileName = "Passport_Photos_${state.selectedPaper.title.replace(" ", "_")}.pdf",
                    mimeType = "application/pdf",
                    category = "passport_print"
                )

                _uiState.update { it.copy(isProcessing = false, processingMessage = "") }
                onComplete(true, "PDF saved to: ${result.displayLocation}")
            } catch (oom: OutOfMemoryError) {
                System.gc()
                _uiState.update { it.copy(isProcessing = false, processingMessage = "") }
                onComplete(false, "Memory low. Try with fewer photos.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save PDF: ${e.message}", e)
                _uiState.update { it.copy(isProcessing = false, processingMessage = "") }
                onComplete(false, "Save PDF failed: ${e.message}")
            }
        }
    }

    /**
     * Export as high-resolution Image (JPEG).
     */
    fun saveAsImage(context: Context, onComplete: (Boolean, String) -> Unit) {
        val state = _uiState.value
        if (state.photos.isEmpty()) {
            onComplete(false, "Please select at least 1 photo")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingMessage = "Saving 300 DPI high-res image...") }
            try {
                val fullSheets = withContext(Dispatchers.Default) {
                    PassportSheetRenderer.renderPrintSheets(
                        context = context,
                        photos = state.photos,
                        paper = state.selectedPaper,
                        photoWidthMm = state.effectiveWidthMm,
                        photoHeightMm = state.effectiveHeightMm,
                        borderEnabled = state.isPhotoBorderEnabled,
                        borderColor = state.borderColor,
                        isPreview = false
                    )
                }

                val activeSheet = fullSheets.getOrElse(state.currentSheetPage) { fullSheets.first() }
                val tempImgFile = PassportSheetRenderer.saveImageFile(
                    context = context,
                    bitmap = activeSheet,
                    isPng = false
                )

                fullSheets.forEach { try { it.recycle() } catch (_: Exception) {} }

                val result = GlobalSaveManager.saveProcessedFile(
                    context = context,
                    tempSourceFile = tempImgFile,
                    desiredFileName = "Passport_Sheet_${state.currentSheetPage + 1}.jpg",
                    mimeType = "image/jpeg",
                    category = "passport_print"
                )

                _uiState.update { it.copy(isProcessing = false, processingMessage = "") }
                onComplete(true, "Image saved to: ${result.displayLocation}")
            } catch (oom: OutOfMemoryError) {
                System.gc()
                _uiState.update { it.copy(isProcessing = false, processingMessage = "") }
                onComplete(false, "Memory low during image export.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save Image: ${e.message}", e)
                _uiState.update { it.copy(isProcessing = false, processingMessage = "") }
                onComplete(false, "Save Image failed: ${e.message}")
            }
        }
    }

    /**
     * Send sheet directly to Android PrintManager.
     */
    fun printSheet(context: Context) {
        val state = _uiState.value
        if (state.generatedSheets.isEmpty()) {
            generateSheets(context)
            Toast.makeText(context, "Rendering sheet for printing...", Toast.LENGTH_SHORT).show()
            return
        }

        val sheetToPrint = state.generatedSheets.getOrElse(state.currentSheetPage) { state.generatedSheets.first() }
        if (sheetToPrint.isRecycled) {
            generateSheets(context)
            return
        }

        try {
            PrintHelper(context).apply {
                scaleMode = PrintHelper.SCALE_MODE_FIT
            }.printBitmap("Passport_Photos_${state.selectedPaper.title}", sheetToPrint)
        } catch (e: Exception) {
            Toast.makeText(context, "Printing error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Step Navigation for backwards compatibility.
     */
    fun goToStep(step: PassportFlowStep) {
        _uiState.update { it.copy(currentStep = step) }
    }

    fun nextStep(context: Context? = null) {
        val current = _uiState.value.currentStep
        when (current) {
            PassportFlowStep.STEP_1_UPLOAD -> {
                if (_uiState.value.photos.isNotEmpty()) {
                    goToStep(PassportFlowStep.STEP_2_CROP_ADJUST)
                    _uiState.update { it.copy(activeStudioTab = PassportStudioTab.CROP_SIZE) }
                }
            }
            PassportFlowStep.STEP_2_CROP_ADJUST -> {
                goToStep(PassportFlowStep.STEP_3_BACKGROUND)
                _uiState.update { it.copy(activeStudioTab = PassportStudioTab.BACKGROUND) }
            }
            PassportFlowStep.STEP_3_BACKGROUND -> {
                goToStep(PassportFlowStep.STEP_4_LAYOUT_EXPORT)
                _uiState.update { it.copy(activeStudioTab = PassportStudioTab.PRINT_SHEET) }
                if (context != null && _uiState.value.generatedSheets.isEmpty()) {
                    generateSheets(context)
                }
            }
            PassportFlowStep.STEP_4_LAYOUT_EXPORT -> {}
        }
    }

    fun prevStep() {
        val current = _uiState.value.currentStep
        when (current) {
            PassportFlowStep.STEP_1_UPLOAD -> {}
            PassportFlowStep.STEP_2_CROP_ADJUST -> {
                goToStep(PassportFlowStep.STEP_1_UPLOAD)
            }
            PassportFlowStep.STEP_3_BACKGROUND -> {
                goToStep(PassportFlowStep.STEP_2_CROP_ADJUST)
                _uiState.update { it.copy(activeStudioTab = PassportStudioTab.CROP_SIZE) }
            }
            PassportFlowStep.STEP_4_LAYOUT_EXPORT -> {
                goToStep(PassportFlowStep.STEP_3_BACKGROUND)
                _uiState.update { it.copy(activeStudioTab = PassportStudioTab.BACKGROUND) }
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    /**
     * Clean up all memory resources when leaving screen.
     */
    fun cleanupSession() {
        val state = _uiState.value
        state.photos.forEach {
            try { if (!it.originalBitmap.isRecycled) it.originalBitmap.recycle() } catch (_: Exception) {}
            try { if (!it.thumbnailBitmap.isRecycled) it.thumbnailBitmap.recycle() } catch (_: Exception) {}
            try { if (it.segmentedForeground != null && !it.segmentedForeground.isRecycled) it.segmentedForeground.recycle() } catch (_: Exception) {}
        }
        state.generatedSheets.forEach {
            try { if (!it.isRecycled) it.recycle() } catch (_: Exception) {}
        }
        _uiState.value = MultiplePassportUiState()
    }

    override fun onCleared() {
        super.onCleared()
        cleanupSession()
    }

    // Bitmap helper functions
    private fun decodeSampledBitmapFromUri(context: Context, uri: Uri, reqWidth: Int = 1200, reqHeight: Int = 1200): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }

            var sampleSize = 1
            val (width, height) = options.outWidth to options.outHeight
            if (width <= 0 || height <= 0) return null

            if (width > reqWidth || height > reqHeight) {
                val halfWidth = width / 2
                val halfHeight = height / 2
                while ((halfWidth / sampleSize) >= reqWidth && (halfHeight / sampleSize) >= reqHeight) {
                    sampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // Memory friendly
            }

            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            }
        } catch (oom: OutOfMemoryError) {
            System.gc()
            Log.e(TAG, "OOM decoding sampled bitmap from uri", oom)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding bitmap from uri: ${e.message}", e)
            null
        }
    }

    private fun createSquareThumbnail(source: Bitmap, size: Int = 240): Bitmap {
        return try {
            if (source.isRecycled) {
                Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).apply { eraseColor(android.graphics.Color.LTGRAY) }
            } else {
                val srcW = source.width
                val srcH = source.height
                val minDim = min(srcW, srcH).coerceAtLeast(1)
                val x = ((srcW - minDim) / 2).coerceAtLeast(0)
                val y = ((srcH - minDim) / 2).coerceAtLeast(0)
                val square = Bitmap.createBitmap(source, x, y, minDim, minDim)
                Bitmap.createScaledBitmap(square, size, size, true)
            }
        } catch (e: Exception) {
            Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).apply { eraseColor(android.graphics.Color.LTGRAY) }
        }
    }
}
