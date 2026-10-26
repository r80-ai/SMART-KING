package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.photo.birefnet.BiRefNetLiteEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Signature ink color mode options.
 */
enum class SignatureInkColor(val labelBn: String, val shortLabel: String, val previewColor: Color) {
    BLACK("কালো", "কালো (Black)", Color(0xFF111827)),
    BLUE("নীল", "নীল (Blue)", Color(0xFF1D4ED8)),
    ORIGINAL("আসল", "আসল (Orig)", Color(0xFFF59E0B))
}

/**
 * Reusable Signature Studio component.
 * Features:
 * 1. Image selection from gallery
 * 2. Real touch-interactive draggable and resizable green crop box dialog
 * 3. Compact line-by-line controls (Ink Color, Cutoff, Contrast) with no wasted space
 * 4. AI Background Removal (BiRefNet Lite ONNX) + Manual Threshold fallback
 * 5. Natural aspect ratio preview box with transparency checkerboard
 */
@Composable
fun SignatureUploadSection(
    signatureBitmap: Bitmap?,
    onSignatureChanged: (Bitmap?) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "স্বাক্ষর আপলোড (Upload Signature - ঐচ্ছিক)",
    subtitle: String = "স্বাক্ষর আপলোড করলে প্রিন্ট কপিতে সই বসে যাবে। না দিলে ডট-লাইনে (................) হাতে সই করতে পারবেন।"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var rawOriginalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var croppedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showCropDialog by remember { mutableStateOf(false) }

    var isAiBgRemove by remember { mutableStateOf(true) }
    var inkColor by remember { mutableStateOf(SignatureInkColor.BLACK) }
    var thresholdValue by remember { mutableIntStateOf(180) }
    var contrastValue by remember { mutableFloatStateOf(1.2f) }

    var isProcessing by remember { mutableStateOf(false) }
    var debounceJob by remember { mutableStateOf<Job?>(null) }

    // Synchronize if signature cleared externally
    LaunchedEffect(signatureBitmap) {
        if (signatureBitmap == null) {
            rawOriginalBitmap = null
            croppedBitmap = null
        }
    }

    // Function to re-process the signature when settings change
    fun executeProcessing(sourceBmp: Bitmap, isAi: Boolean, color: SignatureInkColor, thresh: Int, cont: Float) {
        debounceJob?.cancel()
        debounceJob = scope.launch(Dispatchers.Default) {
            isProcessing = true
            try {
                if (isAi) {
                    val aiResult = BiRefNetLiteEngine.removeBackground(context, sourceBmp).getOrNull()
                    val baseBmp = aiResult?.transparentForeground ?: sourceBmp
                    val isAlreadyTransparent = (aiResult?.transparentForeground != null)
                    val finalized = applyInkAndContrastToBitmap(
                        source = baseBmp,
                        inkColor = color,
                        contrast = cont,
                        threshold = thresh,
                        isAlreadyTransparent = isAlreadyTransparent
                    )
                    withContext(Dispatchers.Main) {
                        isProcessing = false
                        onSignatureChanged(finalized)
                    }
                } else {
                    val finalized = applyInkAndContrastToBitmap(
                        source = sourceBmp,
                        inkColor = color,
                        contrast = cont,
                        threshold = thresh,
                        isAlreadyTransparent = false
                    )
                    withContext(Dispatchers.Main) {
                        isProcessing = false
                        onSignatureChanged(finalized)
                    }
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    isProcessing = false
                    val fallback = applyInkAndContrastToBitmap(sourceBmp, color, cont, thresh, false)
                    onSignatureChanged(fallback)
                }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val decoded = decodeSampledBitmap(context, uri)
                    withContext(Dispatchers.Main) {
                        if (decoded != null) {
                            rawOriginalBitmap = decoded
                            croppedBitmap = decoded
                            showCropDialog = true
                        } else {
                            Toast.makeText(context, "ছবি লোড করা যায়নি", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "স্বাক্ষর লোড করতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // Top Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color(0xFFFBBF24),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = subtitle,
                    color = Color(0xFF94A3B8),
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Button(
                onClick = { photoPickerLauncher.launch("image/*") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.UploadFile,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (signatureBitmap == null && croppedBitmap == null) "গ্যালারি" else "পরিবর্তন",
                    fontSize = 11.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Action Toolbar & Settings (when image loaded)
        if (signatureBitmap != null || croppedBitmap != null) {
            // Row 0: Action Toolbar [ক্রপ] [AI BG ON/OFF] [মুছে ফেলুন]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Crop button
                    OutlinedButton(
                        onClick = { showCropDialog = true },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Crop, contentDescription = "Crop", tint = Color(0xFF38BDF8), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ক্রপ", color = Color(0xFF38BDF8), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }

                    // AI BG Remove toggle
                    FilterChip(
                        selected = isAiBgRemove,
                        onClick = {
                            isAiBgRemove = !isAiBgRemove
                            croppedBitmap?.let { bmp ->
                                executeProcessing(bmp, isAiBgRemove, inkColor, thresholdValue, contrastValue)
                            }
                        },
                        label = { Text(if (isAiBgRemove) "AI BG ON" else "AI BG OFF", fontSize = 10.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp))
                        },
                        modifier = Modifier.height(28.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF22C55E),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF0F172A),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }

                // Delete / Remove button
                TextButton(
                    onClick = {
                        rawOriginalBitmap = null
                        croppedBitmap = null
                        onSignatureChanged(null)
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("মুছে ফেলুন", color = Color(0xFFEF4444), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Row 1: কালির রং (Ink Color) Segmented Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "কালির রং:",
                    color = Color(0xFFE2E8F0),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(62.dp)
                )
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SignatureInkColor.values().forEach { option ->
                        val isSelected = (inkColor == option)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) option.previewColor else Color(0xFF0F172A),
                            border = BorderStroke(1.dp, if (isSelected) Color.White else Color(0xFF334155)),
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .clickable {
                                    inkColor = option
                                    croppedBitmap?.let { bmp ->
                                        executeProcessing(bmp, isAiBgRemove, option, thresholdValue, contrastValue)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(option.previewColor)
                                        .border(1.dp, Color.White, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = option.shortLabel,
                                    fontSize = 10.sp,
                                    color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // Row 2: সাদা কাগজ কাটঅফ (Threshold) Slider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "কাগজ কাটঅফ:",
                    color = Color(0xFFCBD5E1),
                    fontSize = 10.5.sp,
                    modifier = Modifier.width(72.dp)
                )
                Slider(
                    value = thresholdValue.toFloat(),
                    onValueChange = { newVal ->
                        thresholdValue = newVal.toInt()
                        croppedBitmap?.let { bmp ->
                            executeProcessing(bmp, isAiBgRemove, inkColor, thresholdValue, contrastValue)
                        }
                    },
                    valueRange = 80f..240f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF38BDF8),
                        activeTrackColor = Color(0xFF38BDF8),
                        inactiveTrackColor = Color(0xFF334155)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                )
                Text(
                    text = "$thresholdValue",
                    color = Color(0xFF38BDF8),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(28.dp),
                    textAlign = TextAlign.End
                )
            }

            // Row 3: কালির গাঢ়ত্ব (Contrast) Slider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "কালির গাঢ়ত্ব:",
                    color = Color(0xFFCBD5E1),
                    fontSize = 10.5.sp,
                    modifier = Modifier.width(72.dp)
                )
                Slider(
                    value = contrastValue,
                    onValueChange = { newVal ->
                        contrastValue = newVal
                        croppedBitmap?.let { bmp ->
                            executeProcessing(bmp, isAiBgRemove, inkColor, thresholdValue, contrastValue)
                        }
                    },
                    valueRange = 1.0f..3.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF22C55E),
                        activeTrackColor = Color(0xFF22C55E),
                        inactiveTrackColor = Color(0xFF334155)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                )
                Text(
                    text = "${String.format(Locale.US, "%.1f", contrastValue)}x",
                    color = Color(0xFF22C55E),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(28.dp),
                    textAlign = TextAlign.End
                )
            }

            // Natural Aspect Ratio Preview Box with Transparent Checkerboard
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                CheckerboardPattern(modifier = Modifier.fillMaxSize(), squareSize = 10f)

                val displayBitmap = signatureBitmap ?: croppedBitmap
                if (displayBitmap != null) {
                    Image(
                        bitmap = displayBitmap.asImageBitmap(),
                        contentDescription = "Signature Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(4.dp)
                    )
                }

                if (isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(15.dp),
                                color = Color(0xFF38BDF8),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "প্রসেসিং হচ্ছে...",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }

    // Real Touch-Interactive Draggable & Resizable Crop Dialog
    if (showCropDialog && (rawOriginalBitmap != null || croppedBitmap != null)) {
        val targetCropSource = rawOriginalBitmap ?: croppedBitmap!!
        InteractiveCropDialog(
            sourceBitmap = targetCropSource,
            onCropConfirmed = { newCropped ->
                croppedBitmap = newCropped
                showCropDialog = false
                executeProcessing(newCropped, isAiBgRemove, inkColor, thresholdValue, contrastValue)
            },
            onDismiss = {
                showCropDialog = false
                if (croppedBitmap == null) {
                    croppedBitmap = targetCropSource
                }
                executeProcessing(croppedBitmap!!, isAiBgRemove, inkColor, thresholdValue, contrastValue)
            }
        )
    }
}

/**
 * Real Touch-Interactive Draggable & Resizable Green Crop Box Dialog.
 * User can drag corners/edges to resize and drag the box to move anywhere.
 */
@Composable
fun InteractiveCropDialog(
    sourceBitmap: Bitmap,
    onCropConfirmed: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current

    // State for crop rect in image coordinate pixels
    var imageDisplayWidth by remember { mutableFloatStateOf(0f) }
    var imageDisplayHeight by remember { mutableFloatStateOf(0f) }

    var cropLeft by remember { mutableFloatStateOf(0f) }
    var cropTop by remember { mutableFloatStateOf(0f) }
    var cropRight by remember { mutableFloatStateOf(0f) }
    var cropBottom by remember { mutableFloatStateOf(0f) }
    var isInitialized by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Crop,
                            contentDescription = null,
                            tint = Color(0xFF22C55E),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "স্বাক্ষর ক্রপ করুন (Interactive Crop)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }

                Text(
                    text = "সবুজ বক্সটি টেনে বা কোণা ধরে টেনে সই-এর অংশটুকু নির্বাচন করুন:",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Interactive Crop Area
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(Color(0xFF020617), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val availableW = constraints.maxWidth.toFloat()
                    val availableH = constraints.maxHeight.toFloat()
                    val bmpW = sourceBitmap.width.toFloat()
                    val bmpH = sourceBitmap.height.toFloat()
                    val bmpAspect = bmpW / bmpH
                    val contAspect = availableW / availableH

                    val dispW: Float
                    val dispH: Float
                    if (bmpAspect > contAspect) {
                        dispW = availableW
                        dispH = availableW / bmpAspect
                    } else {
                        dispH = availableH
                        dispW = availableH * bmpAspect
                    }

                    LaunchedEffect(dispW, dispH) {
                        imageDisplayWidth = dispW
                        imageDisplayHeight = dispH
                        if (!isInitialized && dispW > 0f && dispH > 0f) {
                            cropLeft = dispW * 0.05f
                            cropTop = dispH * 0.05f
                            cropRight = dispW * 0.95f
                            cropBottom = dispH * 0.95f
                            isInitialized = true
                        }
                    }

                    val dispWDp = with(density) { dispW.toDp() }
                    val dispHDp = with(density) { dispH.toDp() }

                    val minSizePx = with(density) { 36.dp.toPx() }

                    Box(
                        modifier = Modifier
                            .size(dispWDp, dispHDp)
                    ) {
                        // Background image
                        Image(
                            bitmap = sourceBitmap.asImageBitmap(),
                            contentDescription = "Crop Target",
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Canvas: Dimmed dark overlay outside crop box + Grid lines
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Dark semi-transparent dimming outside crop rect
                            // Top
                            drawRect(Color(0x99000000), topLeft = Offset(0f, 0f), size = Size(size.width, cropTop))
                            // Bottom
                            drawRect(Color(0x99000000), topLeft = Offset(0f, cropBottom), size = Size(size.width, size.height - cropBottom))
                            // Left
                            drawRect(Color(0x99000000), topLeft = Offset(0f, cropTop), size = Size(cropLeft, cropBottom - cropTop))
                            // Right
                            drawRect(Color(0x99000000), topLeft = Offset(cropRight, cropTop), size = Size(size.width - cropRight, cropBottom - cropTop))

                            val boxW = cropRight - cropLeft
                            val boxH = cropBottom - cropTop

                            // Green crop boundary stroke
                            drawRect(
                                color = Color(0xFF22C55E),
                                topLeft = Offset(cropLeft, cropTop),
                                size = Size(boxW, boxH),
                                style = Stroke(width = 2.dp.toPx())
                            )

                            // Rule of thirds faint grid lines inside
                            val oneThirdW = boxW / 3f
                            val oneThirdH = boxH / 3f
                            val gridColor = Color(0x6622C55E)
                            drawLine(gridColor, Offset(cropLeft + oneThirdW, cropTop), Offset(cropLeft + oneThirdW, cropBottom), strokeWidth = 1f)
                            drawLine(gridColor, Offset(cropLeft + 2 * oneThirdW, cropTop), Offset(cropLeft + 2 * oneThirdW, cropBottom), strokeWidth = 1f)
                            drawLine(gridColor, Offset(cropLeft, cropTop + oneThirdH), Offset(cropRight, cropTop + oneThirdH), strokeWidth = 1f)
                            drawLine(gridColor, Offset(cropLeft, cropTop + 2 * oneThirdH), Offset(cropRight, cropTop + 2 * oneThirdH), strokeWidth = 1f)
                        }

                        // Draggable Center Body (Moves the entire crop box)
                        val cropLeftDp = with(density) { cropLeft.toDp() }
                        val cropTopDp = with(density) { cropTop.toDp() }
                        val cropWDp = with(density) { (cropRight - cropLeft).coerceAtLeast(0f).toDp() }
                        val cropHDp = with(density) { (cropBottom - cropTop).coerceAtLeast(0f).toDp() }

                        Box(
                            modifier = Modifier
                                .offset(x = cropLeftDp, y = cropTopDp)
                                .size(cropWDp, cropHDp)
                                .pointerInput(dispW, dispH) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val curW = cropRight - cropLeft
                                        val curH = cropBottom - cropTop
                                        val newLeft = (cropLeft + dragAmount.x).coerceIn(0f, dispW - curW)
                                        val newTop = (cropTop + dragAmount.y).coerceIn(0f, dispH - curH)
                                        cropLeft = newLeft
                                        cropRight = newLeft + curW
                                        cropTop = newTop
                                        cropBottom = newTop + curH
                                    }
                                }
                        )

                        // Corner & Edge Drag Handles (Touch Targets)
                        val handleRadiusPx = with(density) { 14.dp.toPx() }

                        // 1. Top-Left Corner Handle
                        CropHandleCircle(
                            xPx = cropLeft,
                            yPx = cropTop,
                            radiusPx = handleRadiusPx,
                            onDrag = { dx, dy ->
                                cropLeft = (cropLeft + dx).coerceIn(0f, cropRight - minSizePx)
                                cropTop = (cropTop + dy).coerceIn(0f, cropBottom - minSizePx)
                            }
                        )

                        // 2. Top-Right Corner Handle
                        CropHandleCircle(
                            xPx = cropRight,
                            yPx = cropTop,
                            radiusPx = handleRadiusPx,
                            onDrag = { dx, dy ->
                                cropRight = (cropRight + dx).coerceIn(cropLeft + minSizePx, dispW)
                                cropTop = (cropTop + dy).coerceIn(0f, cropBottom - minSizePx)
                            }
                        )

                        // 3. Bottom-Left Corner Handle
                        CropHandleCircle(
                            xPx = cropLeft,
                            yPx = cropBottom,
                            radiusPx = handleRadiusPx,
                            onDrag = { dx, dy ->
                                cropLeft = (cropLeft + dx).coerceIn(0f, cropRight - minSizePx)
                                cropBottom = (cropBottom + dy).coerceIn(cropTop + minSizePx, dispH)
                            }
                        )

                        // 4. Bottom-Right Corner Handle
                        CropHandleCircle(
                            xPx = cropRight,
                            yPx = cropBottom,
                            radiusPx = handleRadiusPx,
                            onDrag = { dx, dy ->
                                cropRight = (cropRight + dx).coerceIn(cropLeft + minSizePx, dispW)
                                cropBottom = (cropBottom + dy).coerceIn(cropTop + minSizePx, dispH)
                            }
                        )

                        // 5. Top Center Edge Handle
                        CropHandleBar(
                            xPx = (cropLeft + cropRight) / 2f,
                            yPx = cropTop,
                            isHorizontal = true,
                            onDrag = { _, dy ->
                                cropTop = (cropTop + dy).coerceIn(0f, cropBottom - minSizePx)
                            }
                        )

                        // 6. Bottom Center Edge Handle
                        CropHandleBar(
                            xPx = (cropLeft + cropRight) / 2f,
                            yPx = cropBottom,
                            isHorizontal = true,
                            onDrag = { _, dy ->
                                cropBottom = (cropBottom + dy).coerceIn(cropTop + minSizePx, dispH)
                            }
                        )

                        // 7. Left Center Edge Handle
                        CropHandleBar(
                            xPx = cropLeft,
                            yPx = (cropTop + cropBottom) / 2f,
                            isHorizontal = false,
                            onDrag = { dx, _ ->
                                cropLeft = (cropLeft + dx).coerceIn(0f, cropRight - minSizePx)
                            }
                        )

                        // 8. Right Center Edge Handle
                        CropHandleBar(
                            xPx = cropRight,
                            yPx = (cropTop + cropBottom) / 2f,
                            isHorizontal = false,
                            onDrag = { dx, _ ->
                                cropRight = (cropRight + dx).coerceIn(cropLeft + minSizePx, dispW)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Presets (Full, Center, Tight)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = {
                            cropLeft = 0f
                            cropTop = 0f
                            cropRight = imageDisplayWidth
                            cropBottom = imageDisplayHeight
                        },
                        label = { Text("পুরো ছবি", fontSize = 10.5.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                    AssistChip(
                        onClick = {
                            val padX = imageDisplayWidth * 0.12f
                            val padY = imageDisplayHeight * 0.15f
                            cropLeft = padX
                            cropTop = padY
                            cropRight = imageDisplayWidth - padX
                            cropBottom = imageDisplayHeight - padY
                        },
                        label = { Text("মাঝারি ক্রপ", fontSize = 10.5.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                    AssistChip(
                        onClick = {
                            val padX = imageDisplayWidth * 0.22f
                            val padY = imageDisplayHeight * 0.25f
                            cropLeft = padX
                            cropTop = padY
                            cropRight = imageDisplayWidth - padX
                            cropBottom = imageDisplayHeight - padY
                        },
                        label = { Text("টাইট সই", fontSize = 10.5.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("বাতিল", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (imageDisplayWidth > 0f && imageDisplayHeight > 0f) {
                                val scaleX = sourceBitmap.width.toFloat() / imageDisplayWidth
                                val scaleY = sourceBitmap.height.toFloat() / imageDisplayHeight

                                val pixelLeft = (cropLeft * scaleX).roundToInt().coerceIn(0, sourceBitmap.width - 2)
                                val pixelTop = (cropTop * scaleY).roundToInt().coerceIn(0, sourceBitmap.height - 2)
                                val pixelRight = (cropRight * scaleX).roundToInt().coerceIn(pixelLeft + 2, sourceBitmap.width)
                                val pixelBottom = (cropBottom * scaleY).roundToInt().coerceIn(pixelTop + 2, sourceBitmap.height)
                                val cropW = (pixelRight - pixelLeft).coerceAtLeast(1)
                                val cropH = (pixelBottom - pixelTop).coerceAtLeast(1)

                                val cropped = Bitmap.createBitmap(sourceBitmap, pixelLeft, pixelTop, cropW, cropH)
                                onCropConfirmed(cropped)
                            } else {
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ক্রপ নিশ্চিত করুন", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }
            }
        }
    }
}

/**
 * Touch handle circle for 4 corners of crop box.
 */
@Composable
private fun CropHandleCircle(
    xPx: Float,
    yPx: Float,
    radiusPx: Float,
    onDrag: (Float, Float) -> Unit
) {
    val density = LocalDensity.current
    val sizeDp = with(density) { (radiusPx * 2).toDp() }
    val offsetX = with(density) { (xPx - radiusPx).toDp() }
    val offsetY = with(density) { (yPx - radiusPx).toDp() }

    Box(
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .size(sizeDp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .shadow(3.dp, CircleShape)
                .background(Color.White, CircleShape)
                .border(2.5.dp, Color(0xFF22C55E), CircleShape)
        )
    }
}

/**
 * Touch handle bar for 4 edges of crop box.
 */
@Composable
private fun CropHandleBar(
    xPx: Float,
    yPx: Float,
    isHorizontal: Boolean,
    onDrag: (Float, Float) -> Unit
) {
    val density = LocalDensity.current
    val wDp = with(density) { if (isHorizontal) 28.dp else 18.dp }
    val hDp = with(density) { if (isHorizontal) 18.dp else 28.dp }
    val offsetX = with(density) { (xPx - if (isHorizontal) 14.dp.toPx() else 9.dp.toPx()).toDp() }
    val offsetY = with(density) { (yPx - if (isHorizontal) 9.dp.toPx() else 14.dp.toPx()).toDp() }

    Box(
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .size(wDp, hDp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (isHorizontal) 20.dp else 6.dp, if (isHorizontal) 6.dp else 20.dp)
                .background(Color.White, RoundedCornerShape(3.dp))
                .border(1.5.dp, Color(0xFF22C55E), RoundedCornerShape(3.dp))
        )
    }
}

/**
 * Checkerboard pattern canvas to display transparent PNG signatures authentically.
 */
@Composable
private fun CheckerboardPattern(
    modifier: Modifier = Modifier,
    squareSize: Float = 10f,
    color1: Color = Color(0xFFF1F5F9),
    color2: Color = Color(0xFFE2E8F0)
) {
    Canvas(modifier = modifier) {
        val cols = (size.width / squareSize).toInt() + 1
        val rows = (size.height / squareSize).toInt() + 1
        for (i in 0 until cols) {
            for (j in 0 until rows) {
                val color = if ((i + j) % 2 == 0) color1 else color2
                drawRect(
                    color = color,
                    topLeft = Offset(i * squareSize, j * squareSize),
                    size = Size(squareSize, squareSize)
                )
            }
        }
    }
}

/**
 * Ink threshold, contrast enhancement, and color tinting engine.
 */
fun applyInkAndContrastToBitmap(
    source: Bitmap,
    inkColor: SignatureInkColor,
    contrast: Float,
    threshold: Int,
    isAlreadyTransparent: Boolean
): Bitmap {
    val width = source.width
    val height = source.height
    val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)

    for (i in pixels.indices) {
        val pixel = pixels[i]
        val a = (pixel shr 24) and 0xFF
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF

        // If pixel is already transparent from AI removal
        if (a < 20) {
            pixels[i] = 0
            continue
        }

        val lum = (0.299f * r + 0.587f * g + 0.114f * b).toInt()

        // If not already transparent, check paper threshold
        if (!isAlreadyTransparent && lum > threshold) {
            pixels[i] = 0
            continue
        }

        // Ink calculation
        val inkDarkness = if (isAlreadyTransparent) {
            (a / 255f) * (1f - (lum / 255f).coerceIn(0f, 1f))
        } else {
            (1f - (lum.toFloat() / threshold.toFloat())).coerceIn(0f, 1f)
        }

        // Apply contrast curve
        val boostedInk = Math.pow(inkDarkness.toDouble(), (1.0 / contrast).coerceIn(0.4, 2.0)).toFloat().coerceIn(0f, 1f)
        val finalAlpha = if (isAlreadyTransparent) {
            (a * (0.5f + 0.5f * boostedInk)).toInt().coerceIn(0, 255)
        } else {
            (boostedInk * 255).toInt().coerceIn(0, 255)
        }

        if (finalAlpha < 15) {
            pixels[i] = 0
            continue
        }

        when (inkColor) {
            SignatureInkColor.ORIGINAL -> {
                val factor = (1f - 0.25f * (contrast - 1f)).coerceIn(0.5f, 1.2f)
                val nr = (r * factor).toInt().coerceIn(0, 255)
                val ng = (g * factor).toInt().coerceIn(0, 255)
                val nb = (b * factor).toInt().coerceIn(0, 255)
                pixels[i] = (finalAlpha shl 24) or (nr shl 16) or (ng shl 8) or nb
            }
            SignatureInkColor.BLACK -> {
                val darkVal = (25 * (1f - boostedInk)).toInt().coerceIn(0, 40)
                pixels[i] = (finalAlpha shl 24) or (darkVal shl 16) or (darkVal shl 8) or darkVal
            }
            SignatureInkColor.BLUE -> {
                val blueR = (12 * (1f - boostedInk)).toInt().coerceIn(0, 30)
                val blueG = (45 + 15 * (1f - boostedInk)).toInt().coerceIn(30, 75)
                val blueB = (175 + 40 * boostedInk).toInt().coerceIn(160, 235)
                pixels[i] = (finalAlpha shl 24) or (blueR shl 16) or (blueG shl 8) or blueB
            }
        }
    }
    outBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return outBitmap
}

/**
 * Converts a Bitmap to a Base64-encoded PNG string for embedding in HTML print output.
 */
fun Bitmap.toBase64Png(): String {
    val baos = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.PNG, 100, baos)
    return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
}

/**
 * Decodes a scaled bitmap from Uri to prevent memory spikes on large camera images.
 */
fun decodeSampledBitmap(context: Context, uri: Uri, reqWidth: Int = 800, reqHeight: Int = 800): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
        var inSampleSize = 1
        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        }
    } catch (e: Exception) {
        null
    }
}
