package com.example.pvccard.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pvccard.engine.PvcCardRenderer
import com.example.pvccard.model.PvcCardItem
import com.example.pvccard.model.PvcCardTemplate
import com.example.pvccard.model.PvcCropPresetRepository
import com.example.pvccard.model.PvcSheetLayout
import com.example.pvccard.model.PvcWorkflowStep
import com.example.pvccard.model.RectFNorm
import com.example.storage.GlobalSaveManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import java.io.File
import java.io.FileOutputStream
import kotlin.math.atan2

data class PvcCardUiState(
    val workflowStep: PvcWorkflowStep = PvcWorkflowStep.IMPORT,
    val selectedTemplate: PvcCardTemplate = PvcCardTemplate.AADHAAR,
    val baseDocumentBitmap: Bitmap? = null,
    val sourceDocumentBitmap: Bitmap? = null,
    // Independent source bitmaps for multi-page support
    val frontBaseBitmap: Bitmap? = null,
    val backBaseBitmap: Bitmap? = null,
    val frontSourceBitmap: Bitmap? = null,
    val backSourceBitmap: Bitmap? = null,
    val sourceDocumentUri: Uri? = null,
    val sourceDocumentFileName: String? = null,
    val isPasswordProtected: Boolean = false,
    val passwordInput: String = "",
    val errorMessage: String? = null,
    val isProcessing: Boolean = false,
    val processingMessage: String = "",
    // De-skew rotation
    val rotationDegrees: Float = 0f,
    val frontRotation: Float = 0f,
    val backRotation: Float = 0f,
    // Manual crop
    val activeCropSide: String = "FRONT", // "FRONT" or "BACK"
    val frontCropNorm: RectFNorm = PvcCardTemplate.AADHAAR.defaultFrontNorm,
    val backCropNorm: RectFNorm = PvcCardTemplate.AADHAAR.defaultBackNorm,
    // Extracted cards
    val frontBitmap: Bitmap? = null,
    val backBitmap: Bitmap? = null,
    val cardName: String = "Smart Card",
    // Enhancements
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val isTextDarkened: Boolean = false,
    val hasPerimeterBorder: Boolean = true,
    val cardCopies: Int = 1,
    // Queue of cards
    val cardQueue: List<PvcCardItem> = emptyList(),
    // Sheet Layout
    val sheetLayout: PvcSheetLayout = PvcSheetLayout.A4_FIVE_CARDS,
    val previewSheetPage: Int = 0,
    val sheetPreviewBitmap: Bitmap? = null,
    val isExporting: Boolean = false,
    val statusNotification: String? = null
)

class PvcCardViewModel : ViewModel() {
    private val TAG = "PvcCardViewModel"

    private val _uiState = MutableStateFlow(PvcCardUiState())
    val uiState: StateFlow<PvcCardUiState> = _uiState.asStateFlow()

    fun setWorkflowStep(step: PvcWorkflowStep) {
        _uiState.update { it.copy(workflowStep = step) }
        if (step == PvcWorkflowStep.PRINT_SHEET) {
            updatePrintSheetPreview()
        }
    }

    fun selectTemplate(context: Context? = null, template: PvcCardTemplate) {
        val (frontNorm, backNorm) = if (context != null) {
            val saved = PvcCropPresetRepository.loadPreset(context, template.id, template.defaultFrontNorm, template.defaultBackNorm)
            saved ?: Pair(template.defaultFrontNorm, template.defaultBackNorm)
        } else {
            Pair(template.defaultFrontNorm, template.defaultBackNorm)
        }

        _uiState.update {
            it.copy(
                selectedTemplate = template,
                frontCropNorm = frontNorm,
                backCropNorm = backNorm,
                cardName = template.displayName
            )
        }
        // If document is already loaded, auto-extract with new template coordinates
        if (_uiState.value.sourceDocumentBitmap != null && template != PvcCardTemplate.CUSTOM) {
            autoExtractFromDocument(template)
        }
    }

    fun saveCropPreset(context: Context) {
        val template = _uiState.value.selectedTemplate
        val front = _uiState.value.frontCropNorm
        val back = _uiState.value.backCropNorm
        PvcCropPresetRepository.savePreset(context, template.id, front, back)
        _uiState.update {
            it.copy(statusNotification = "Crop position saved and locked for this card!")
        }
    }

    fun resetCropPreset(context: Context) {
        val template = _uiState.value.selectedTemplate
        PvcCropPresetRepository.clearPreset(context, template.id)
        _uiState.update {
            it.copy(
                frontCropNorm = template.defaultFrontNorm,
                backCropNorm = template.defaultBackNorm,
                statusNotification = "Reset to default factory crop coordinates"
            )
        }
        if (_uiState.value.sourceDocumentBitmap != null) {
            autoExtractFromDocument(template)
        }
    }

    /**
     * Loads a PDF document and renders page 0 at 300 DPI with optional password decryption.
     * If document has 2 or more pages, Page 1 is mapped to Front and Page 2 to Back automatically.
     */
    fun loadPdf(context: Context, uri: Uri, password: String? = null) {
        viewModelScope.launch {
            val fileName = queryDisplayName(context, uri) ?: "Document.pdf"
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    processingMessage = if (!password.isNullOrBlank()) "Decrypting PDF with password..." else "Reading PDF pages...",
                    sourceDocumentUri = uri,
                    sourceDocumentFileName = fileName,
                    errorMessage = null
                )
            }

            // 1. Get Page Count
            val pageCountRes = PvcCardRenderer.getPdfPageCount(context, uri, password)
            val pageCount = pageCountRes.getOrDefault(1)

            if (pageCount >= 2) {
                _uiState.update { it.copy(processingMessage = "Rendering Multi-Page PDF (Page 1 & 2)...") }
                val frontRes = PvcCardRenderer.renderPdfPageToBitmap(context, uri, 0, 300, password)
                val backRes = PvcCardRenderer.renderPdfPageToBitmap(context, uri, 1, 300, password)

                if (frontRes.isSuccess && backRes.isSuccess) {
                    val fRaw = frontRes.getOrThrow()
                    val bRaw = backRes.getOrThrow()
                    
                    val fCard = PvcCardRenderer.cropToPvcSize(fRaw, Rect(0, 0, fRaw.width, fRaw.height))
                    val bCard = PvcCardRenderer.cropToPvcSize(bRaw, Rect(0, 0, bRaw.width, bRaw.height))

                    _uiState.update {
                        it.copy(
                            baseDocumentBitmap = fRaw,
                            sourceDocumentBitmap = fRaw,
                            frontBaseBitmap = fRaw,
                            backBaseBitmap = bRaw,
                            frontSourceBitmap = fRaw,
                            backSourceBitmap = bRaw,
                            frontRotation = 0f,
                            backRotation = 0f,
                            frontBitmap = fCard,
                            backBitmap = bCard,
                            isProcessing = false,
                            processingMessage = "",
                            isPasswordProtected = false,
                            passwordInput = "",
                            rotationDegrees = 0f,
                            errorMessage = null,
                            activeCropSide = "FRONT",
                            workflowStep = PvcWorkflowStep.ENHANCE_PREVIEW,
                            statusNotification = "2-Page PDF Loaded: Page 1 set to Front, Page 2 to Back"
                        )
                    }
                    return@launch
                }
            }

            // Fallback to single page extraction logic
            val result = PvcCardRenderer.renderPdfPageToBitmap(
                context = context,
                uri = uri,
                pageIndex = 0,
                targetDpi = 300,
                password = password
            )
            result.fold(
                onSuccess = { bmp ->
                    _uiState.update {
                        it.copy(
                            baseDocumentBitmap = bmp,
                            sourceDocumentBitmap = bmp,
                            frontBaseBitmap = bmp,
                            backBaseBitmap = bmp,
                            frontSourceBitmap = bmp,
                            backSourceBitmap = bmp,
                            frontRotation = 0f,
                            backRotation = 0f,
                            isProcessing = false,
                            processingMessage = "",
                            isPasswordProtected = false,
                            passwordInput = "",
                            rotationDegrees = 0f,
                            errorMessage = null,
                            statusNotification = if (!password.isNullOrBlank()) "PDF unlocked and loaded successfully" else "PDF loaded successfully"
                        )
                    }
                    // Load saved preset if exists for selected template
                    val template = _uiState.value.selectedTemplate
                    val saved = PvcCropPresetRepository.loadPreset(context, template.id, template.defaultFrontNorm, template.defaultBackNorm)
                    if (saved != null) {
                        _uiState.update { it.copy(frontCropNorm = saved.first, backCropNorm = saved.second) }
                    }
                    // Run canonical auto-crop on load
                    autoExtractFromDocument(_uiState.value.selectedTemplate)
                },
                onFailure = { error ->
                    Log.e(TAG, "Error loading PDF: ${error.message}", error)
                    val isPwError = error is SecurityException ||
                            error is InvalidPasswordException ||
                            error.message?.contains("password", ignoreCase = true) == true

                    if (isPwError) {
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                isPasswordProtected = true,
                                errorMessage = if (!password.isNullOrBlank()) "Incorrect password. Please verify and try again." else "This PDF is password-protected. Please enter password."
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                errorMessage = "Failed to open PDF: ${error.message}"
                            )
                        }
                    }
                }
            )
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else null
            } ?: uri.lastPathSegment
        } catch (e: Exception) {
            uri.lastPathSegment
        }
    }

    fun dismissPasswordDialog() {
        _uiState.update {
            it.copy(
                isPasswordProtected = false,
                passwordInput = "",
                errorMessage = null
            )
        }
    }

    fun updatePasswordInput(input: String) {
        _uiState.update { it.copy(passwordInput = input) }
    }

    /**
     * Loads an image from URI (scanned photo / gallery).
     */
    fun loadImage(context: Context, uri: Uri, isBackImage: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessing = true,
                    processingMessage = "Loading card image...",
                    errorMessage = null
                )
            }

            val bmp = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading image: ${e.message}", e)
                    null
                }
            }

            if (bmp != null) {
                if (isBackImage) {
                    // Directly crop and set as Back
                    val pvcBack = PvcCardRenderer.cropToPvcSize(bmp, Rect(0, 0, bmp.width, bmp.height))
                    _uiState.update {
                        it.copy(
                            backBitmap = pvcBack,
                            isProcessing = false,
                            processingMessage = "",
                            statusNotification = "Back side image set"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            baseDocumentBitmap = bmp,
                            sourceDocumentBitmap = bmp,
                            frontBaseBitmap = bmp,
                            backBaseBitmap = bmp,
                            frontSourceBitmap = bmp,
                            backSourceBitmap = bmp,
                            sourceDocumentUri = uri,
                            isProcessing = false,
                            processingMessage = "",
                            rotationDegrees = 0f,
                            frontRotation = 0f,
                            backRotation = 0f,
                            workflowStep = PvcWorkflowStep.MANUAL_CROP,
                            statusNotification = "Image loaded"
                        )
                    }
                    val template = _uiState.value.selectedTemplate
                    val saved = PvcCropPresetRepository.loadPreset(context, template.id, template.defaultFrontNorm, template.defaultBackNorm)
                    if (saved != null) {
                        _uiState.update { it.copy(frontCropNorm = saved.first, backCropNorm = saved.second) }
                    } else {
                        _uiState.update { it.copy(frontCropNorm = template.defaultFrontNorm, backCropNorm = template.defaultBackNorm) }
                    }
                    if (template != PvcCardTemplate.CUSTOM) {
                        autoExtractFromDocument(template)
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = "Could not load selected image."
                    )
                }
            }
        }
    }

    /**
     * Sets exact rotation degrees (-180° to +180°) from original base document bitmap.
     */
    fun setRotationAngle(degrees: Float) {
        val side = _uiState.value.activeCropSide
        val base = if (side == "FRONT") {
            _uiState.value.frontBaseBitmap ?: _uiState.value.baseDocumentBitmap
        } else {
            _uiState.value.backBaseBitmap ?: _uiState.value.baseDocumentBitmap
        } ?: return

        val clamped = degrees.coerceIn(-180f, 180f)
        viewModelScope.launch {
            val rotated = withContext(Dispatchers.Default) {
                if (clamped == 0f) base else PvcCardRenderer.rotateBitmap(base, clamped)
            }
            _uiState.update {
                if (side == "FRONT") {
                    it.copy(
                        rotationDegrees = clamped,
                        frontRotation = clamped,
                        sourceDocumentBitmap = rotated,
                        frontSourceBitmap = rotated
                    )
                } else {
                    it.copy(
                        rotationDegrees = clamped,
                        backRotation = clamped,
                        sourceDocumentBitmap = rotated,
                        backSourceBitmap = rotated
                    )
                }
            }
        }
    }

    /**
     * Micro-step adjust rotation angle (e.g. +0.2°, -0.2°, +1°, -1°).
     */
    fun adjustRotationAngle(delta: Float) {
        val current = _uiState.value.rotationDegrees
        setRotationAngle(current + delta)
    }

    /**
     * Resets rotation to 0°.
     */
    fun resetRotation() {
        val side = _uiState.value.activeCropSide
        val base = if (side == "FRONT") {
            _uiState.value.frontBaseBitmap ?: _uiState.value.baseDocumentBitmap
        } else {
            _uiState.value.backBaseBitmap ?: _uiState.value.baseDocumentBitmap
        } ?: return

        _uiState.update {
            if (side == "FRONT") {
                it.copy(
                    rotationDegrees = 0f,
                    frontRotation = 0f,
                    sourceDocumentBitmap = base,
                    frontSourceBitmap = base,
                    statusNotification = "Front straightened reset to 0°"
                )
            } else {
                it.copy(
                    rotationDegrees = 0f,
                    backRotation = 0f,
                    sourceDocumentBitmap = base,
                    backSourceBitmap = base,
                    statusNotification = "Back straightened reset to 0°"
                )
            }
        }
    }

    /**
     * De-skew / Straighten tool:
     * Calculates tilt angle from a line drawn along any card edge,
     * and normalizes to 0° level.
     */
    fun calculateAndApplyStraighten(x1: Float, y1: Float, x2: Float, y2: Float) {
        val dx = x2 - x1
        val dy = y2 - y1
        if (dx == 0f && dy == 0f) return

        val rawAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

        // Card edges are either horizontal (0°, 180°, -180°) or vertical (90°, -90°).
        // Find tilt difference relative to closest axis:
        var mod = rawAngle % 90f
        if (mod > 45f) mod -= 90f
        if (mod < -45f) mod += 90f

        val tiltDegrees = mod
        val targetRotation = (_uiState.value.rotationDegrees - tiltDegrees)
        setRotationAngle(targetRotation)
        _uiState.update {
            it.copy(
                statusNotification = "Auto-straightened by ${String.format("%.1f", -tiltDegrees)}°"
            )
        }
    }

    fun rotateByAngle(degrees: Float) {
        adjustRotationAngle(degrees)
    }

    /**
     * Nudges the crop box position in normalized coordinates.
     */
    fun nudgeCropBox(side: String, dxNorm: Float, dyNorm: Float) {
        val currentNorm = if (side == "FRONT") _uiState.value.frontCropNorm else _uiState.value.backCropNorm
        val maxLeft = (1f - currentNorm.width).coerceAtLeast(0f)
        val maxTop = (1f - currentNorm.height).coerceAtLeast(0f)
        val newLeft = (currentNorm.left + dxNorm).coerceIn(0f, maxLeft)
        val newTop = (currentNorm.top + dyNorm).coerceIn(0f, maxTop)
        updateCropBox(side, currentNorm.copy(left = newLeft, top = newTop))
    }

    /**
     * Crops both Front and Back sides in one go using current box positions.
     */
    fun cropBothSides() {
        val state = _uiState.value
        val fDoc = state.frontSourceBitmap ?: state.sourceDocumentBitmap ?: return
        val bDoc = state.backSourceBitmap ?: state.sourceDocumentBitmap ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingMessage = "Cropping Front & Back...") }
            val (front, back) = withContext(Dispatchers.Default) {
                val fRect = state.frontCropNorm.toPixelRect(fDoc.width, fDoc.height)
                val bRect = state.backCropNorm.toPixelRect(bDoc.width, bDoc.height)
                Pair(
                    PvcCardRenderer.cropToPvcSize(fDoc, fRect),
                    PvcCardRenderer.cropToPvcSize(bDoc, bRect)
                )
            }
            _uiState.update {
                it.copy(
                    frontBitmap = front,
                    backBitmap = back,
                    isProcessing = false,
                    processingMessage = "",
                    statusNotification = "Cropped Front and Back sides (1012 × 638 px)"
                )
            }
        }
    }

    /**
     * Canonical One-Tap Auto-Crop from loaded PDF/document using template coordinates.
     */
    fun autoExtractFromDocument(template: PvcCardTemplate = _uiState.value.selectedTemplate) {
        val state = _uiState.value
        val fDoc = state.frontSourceBitmap ?: state.sourceDocumentBitmap ?: return
        val bDoc = state.backSourceBitmap ?: state.sourceDocumentBitmap ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, processingMessage = "Extracting Front & Back...") }

            val (frontBmp, backBmp) = withContext(Dispatchers.Default) {
                val frontNorm = if (state.selectedTemplate == template) state.frontCropNorm else template.defaultFrontNorm
                val backNorm = if (state.selectedTemplate == template) state.backCropNorm else template.defaultBackNorm
                val frontRect = frontNorm.toPixelRect(fDoc.width, fDoc.height)
                val backRect = backNorm.toPixelRect(bDoc.width, bDoc.height)

                val front = PvcCardRenderer.cropToPvcSize(fDoc, frontRect)
                val back = PvcCardRenderer.cropToPvcSize(bDoc, backRect)
                Pair(front, back)
            }

            _uiState.update {
                it.copy(
                    frontBitmap = frontBmp,
                    backBitmap = backBmp,
                    isProcessing = false,
                    processingMessage = "",
                    workflowStep = PvcWorkflowStep.ENHANCE_PREVIEW,
                    statusNotification = "Extracted ${template.displayName} Front & Back (1012 × 638 px)"
                )
            }
        }
    }

    /**
     * Sets active crop side: "FRONT" or "BACK".
     * Swaps the active source bitmap and rotation angle for multi-page editing.
     */
    fun setActiveCropSide(side: String) {
        _uiState.update { current ->
            if (current.activeCropSide == side) return@update current
            
            val newActiveBitmap = if (side == "FRONT") {
                current.frontSourceBitmap ?: current.sourceDocumentBitmap
            } else {
                current.backSourceBitmap ?: current.sourceDocumentBitmap
            }
            
            val newActiveRotation = if (side == "FRONT") current.frontRotation else current.backRotation
            
            current.copy(
                activeCropSide = side,
                sourceDocumentBitmap = newActiveBitmap,
                rotationDegrees = newActiveRotation
            )
        }
    }

    /**
     * Updates manual crop box coordinates.
     */
    fun updateCropBox(side: String, normRect: RectFNorm) {
        _uiState.update {
            if (side == "FRONT") {
                it.copy(frontCropNorm = normRect)
            } else {
                it.copy(backCropNorm = normRect)
            }
        }
    }

    /**
     * Resizes manual crop box dimensions (width and height).
     */
    fun resizeCropBox(side: String, dWidthNorm: Float, dHeightNorm: Float) {
        val currentNorm = if (side == "FRONT") _uiState.value.frontCropNorm else _uiState.value.backCropNorm
        val isCustom = _uiState.value.selectedTemplate == PvcCardTemplate.CUSTOM
        
        val minSize = if (isCustom) 0.05f else 0.15f
        val maxSize = 1.0f
        
        val newWidth = (currentNorm.width + dWidthNorm).coerceIn(minSize, maxSize)
        val newHeight = (currentNorm.height + dHeightNorm).coerceIn(minSize, maxSize)
        
        val maxLeft = (1f - newWidth).coerceAtLeast(0f)
        val maxTop = (1f - newHeight).coerceAtLeast(0f)
        val newLeft = currentNorm.left.coerceIn(0f, maxLeft)
        val newTop = currentNorm.top.coerceIn(0f, maxTop)
        
        updateCropBox(side, currentNorm.copy(left = newLeft, top = newTop, width = newWidth, height = newHeight))
    }

    /**
     * Confirms the manual crop for Front or Back.
     */
    fun confirmManualCrop(side: String) {
        val doc = _uiState.value.sourceDocumentBitmap ?: return
        val norm = if (side == "FRONT") _uiState.value.frontCropNorm else _uiState.value.backCropNorm
        val pixelRect = norm.toPixelRect(doc.width, doc.height)

        viewModelScope.launch {
            val cropped = withContext(Dispatchers.Default) {
                PvcCardRenderer.cropToPvcSize(doc, pixelRect)
            }
            _uiState.update {
                if (side == "FRONT") {
                    it.copy(frontBitmap = cropped, statusNotification = "Front side cropped")
                } else {
                    it.copy(backBitmap = cropped, statusNotification = "Back side cropped")
                }
            }
        }
    }

    fun swapFrontAndBack() {
        _uiState.update {
            it.copy(
                frontBitmap = it.backBitmap,
                backBitmap = it.frontBitmap,
                statusNotification = "Swapped Front and Back sides"
            )
        }
    }

    // Enhancement adjustments
    fun setBrightness(value: Float) {
        _uiState.update { it.copy(brightness = value) }
    }

    fun setContrast(value: Float) {
        _uiState.update { it.copy(contrast = value) }
    }

    fun toggleTextDarken() {
        _uiState.update { it.copy(isTextDarkened = !it.isTextDarkened) }
    }

    fun toggleBorder() {
        _uiState.update { it.copy(hasPerimeterBorder = !it.hasPerimeterBorder) }
    }

    fun setCardCopies(copies: Int) {
        _uiState.update { it.copy(cardCopies = copies.coerceIn(1, 20)) }
    }

    fun setCardName(name: String) {
        _uiState.update { it.copy(cardName = name) }
    }

    /**
     * Adds current card to the print queue.
     */
    fun addCurrentCardToQueue() {
        val front = _uiState.value.frontBitmap ?: return
        val item = PvcCardItem(
            name = _uiState.value.cardName,
            template = _uiState.value.selectedTemplate,
            frontBitmap = front,
            backBitmap = _uiState.value.backBitmap,
            copies = _uiState.value.cardCopies,
            brightness = _uiState.value.brightness,
            contrast = _uiState.value.contrast,
            isTextDarkened = _uiState.value.isTextDarkened,
            hasPerimeterBorder = _uiState.value.hasPerimeterBorder
        )

        _uiState.update {
            it.copy(
                cardQueue = it.cardQueue + item,
                statusNotification = "Added '${item.name}' (${item.copies} copies) to print queue"
            )
        }
        updatePrintSheetPreview()
    }

    fun removeCardFromQueue(id: String) {
        _uiState.update {
            it.copy(cardQueue = it.cardQueue.filterNot { card -> card.id == id })
        }
        updatePrintSheetPreview()
    }

    fun updateQueueCardCopies(id: String, delta: Int) {
        _uiState.update { current ->
            val updated = current.cardQueue.map { card ->
                if (card.id == id) {
                    card.copy(copies = (card.copies + delta).coerceIn(1, 20))
                } else card
            }
            current.copy(cardQueue = updated)
        }
        updatePrintSheetPreview()
    }

    fun setSheetLayout(layout: PvcSheetLayout) {
        _uiState.update { it.copy(sheetLayout = layout, previewSheetPage = 0) }
        updatePrintSheetPreview()
    }

    fun setPreviewSheetPage(page: Int) {
        _uiState.update { it.copy(previewSheetPage = page) }
        updatePrintSheetPreview()
    }

    /**
     * Generates print sheet preview bitmap.
     */
    fun updatePrintSheetPreview() {
        val cards = if (_uiState.value.cardQueue.isNotEmpty()) {
            _uiState.value.cardQueue
        } else if (_uiState.value.frontBitmap != null) {
            listOf(
                PvcCardItem(
                    name = _uiState.value.cardName,
                    frontBitmap = _uiState.value.frontBitmap!!,
                    backBitmap = _uiState.value.backBitmap,
                    copies = _uiState.value.cardCopies,
                    brightness = _uiState.value.brightness,
                    contrast = _uiState.value.contrast,
                    isTextDarkened = _uiState.value.isTextDarkened,
                    hasPerimeterBorder = _uiState.value.hasPerimeterBorder
                )
            )
        } else emptyList()

        if (cards.isEmpty()) {
            _uiState.update { it.copy(sheetPreviewBitmap = null) }
            return
        }

        viewModelScope.launch {
            val layout = _uiState.value.sheetLayout
            val page = _uiState.value.previewSheetPage
            val bmp = withContext(Dispatchers.Default) {
                PvcCardRenderer.renderPrintSheet(layout, cards, page)
            }
            _uiState.update { it.copy(sheetPreviewBitmap = bmp) }
        }
    }

    /**
     * Export sheet as PDF (300 DPI).
     */
    fun exportPdf(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }

            val cards = getEffectiveCards()
            val layout = _uiState.value.sheetLayout
            val pages = listOf(PvcCardRenderer.renderPrintSheet(layout, cards, pageIndex = 0))

            val tempFile = File(context.cacheDir, "pvc_sheet_${System.currentTimeMillis()}.pdf")
            val pdfResult = PvcCardRenderer.exportSheetsToPdf(context, pages, tempFile)

            pdfResult.fold(
                onSuccess = { file ->
                    val saveResult = GlobalSaveManager.saveProcessedFile(
                        context = context,
                        tempSourceFile = file,
                        desiredFileName = "PVC_Smart_Cards_${layout.id}.pdf",
                        mimeType = "application/pdf",
                        category = "PVC Cards",
                        tag = "CR80 Print Sheet"
                    )
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            statusNotification = "PDF saved to: ${saveResult.displayLocation}/${saveResult.fileName}"
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            errorMessage = "Failed to export PDF: ${e.message}"
                        )
                    }
                }
            )
        }
    }

    /**
     * Export sheet as Image (JPEG @ 300 DPI).
     */
    fun exportImage(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }

            val cards = getEffectiveCards()
            val layout = _uiState.value.sheetLayout
            val page = _uiState.value.previewSheetPage
            val bmp = withContext(Dispatchers.Default) {
                PvcCardRenderer.renderPrintSheet(layout, cards, page)
            }

            val tempFile = File(context.cacheDir, "pvc_sheet_${System.currentTimeMillis()}.jpg")
            withContext(Dispatchers.IO) {
                FileOutputStream(tempFile).use { out ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 98, out)
                }
            }

            val saveResult = GlobalSaveManager.saveProcessedFile(
                context = context,
                tempSourceFile = tempFile,
                desiredFileName = "PVC_Smart_Cards_${layout.id}.jpg",
                mimeType = "image/jpeg",
                category = "PVC Cards",
                tag = "CR80 300 DPI Image"
            )

            _uiState.update {
                it.copy(
                    isExporting = false,
                    statusNotification = "Image saved to: ${saveResult.displayLocation}/${saveResult.fileName}"
                )
            }
        }
    }

    /**
     * Direct print via Android PrintManager.
     */
    fun printSheet(context: Context) {
        val bmp = _uiState.value.sheetPreviewBitmap ?: run {
            val cards = getEffectiveCards()
            PvcCardRenderer.renderPrintSheet(_uiState.value.sheetLayout, cards, _uiState.value.previewSheetPage)
        }
        PvcCardRenderer.directPrint(context, "PVC_Card_Print", bmp)
    }

    private fun getEffectiveCards(): List<PvcCardItem> {
        return if (_uiState.value.cardQueue.isNotEmpty()) {
            _uiState.value.cardQueue
        } else if (_uiState.value.frontBitmap != null) {
            listOf(
                PvcCardItem(
                    name = _uiState.value.cardName,
                    frontBitmap = _uiState.value.frontBitmap!!,
                    backBitmap = _uiState.value.backBitmap,
                    copies = _uiState.value.cardCopies,
                    brightness = _uiState.value.brightness,
                    contrast = _uiState.value.contrast,
                    isTextDarkened = _uiState.value.isTextDarkened,
                    hasPerimeterBorder = _uiState.value.hasPerimeterBorder
                )
            )
        } else emptyList()
    }

    fun generatePresetJson(): String {
        val state = _uiState.value
        val template = state.selectedTemplate
        val f = state.frontCropNorm
        val b = state.backCropNorm
        val docW = state.sourceDocumentBitmap?.width ?: 2480
        val docH = state.sourceDocumentBitmap?.height ?: 3508

        val fX = f.left * docW
        val fY = f.top * docH
        val fW = f.width * docW
        val fH = f.height * docH

        val bX = b.left * docW
        val bY = b.top * docH
        val bW = b.width * docW
        val bH = b.height * docH

        return """
            {
              "card_type": "${template.id.uppercase()}",
              "display_name": "${template.displayName}",
              "source_document_width": $docW,
              "source_document_height": $docH,
              "front": {"x": ${String.format(java.util.Locale.US, "%.1f", fX)}, "y": ${String.format(java.util.Locale.US, "%.1f", fY)}, "w": ${String.format(java.util.Locale.US, "%.1f", fW)}, "h": ${String.format(java.util.Locale.US, "%.1f", fH)}},
              "back": {"x": ${String.format(java.util.Locale.US, "%.1f", bX)}, "y": ${String.format(java.util.Locale.US, "%.1f", bY)}, "w": ${String.format(java.util.Locale.US, "%.1f", bW)}, "h": ${String.format(java.util.Locale.US, "%.1f", bH)}}
            }
        """.trimIndent()
    }

    fun writePresetToFile(context: Context, uri: Uri, jsonString: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(jsonString.toByteArray(Charsets.UTF_8))
                    }
                }
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("PVC Crop Preset", jsonString)
                clipboard.setPrimaryClip(clip)

                _uiState.update {
                    it.copy(statusNotification = "Coordinates copied to clipboard & saved to file!")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error exporting crop preset: ${e.message}", e)
                _uiState.update {
                    it.copy(errorMessage = "Failed to export file: ${e.message}")
                }
            }
        }
    }

    fun clearNotification() {
        _uiState.update { it.copy(statusNotification = null, errorMessage = null) }
    }
}
