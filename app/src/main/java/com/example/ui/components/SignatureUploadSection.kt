package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photo.birefnet.BiRefNetLiteEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Locale

/**
 * Signature ink color mode options.
 */
enum class SignatureInkColor(val labelBn: String, val labelEn: String, val previewColor: Color) {
    BLACK("কালো (Black)", "Black", Color(0xFF111827)),
    BLUE("নীল (Blue)", "Blue", Color(0xFF1D4ED8)),
    ORIGINAL("মূল রং (Original)", "Original", Color(0xFFF59E0B))
}

/**
 * Reusable Signature Studio component.
 * Features:
 * 1. Image selection from gallery
 * 2. Visual interactive crop tool dialog (Top, Bottom, Sides sliders + Presets)
 * 3. AI Background Removal (BiRefNet Lite ONNX) + Manual Threshold fallback
 * 4. Ink Color Choice: Black, Blue, Original
 * 5. Cutoff (Threshold) and Contrast (Sharpness) sliders
 * 6. Live transparent preview
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
                    // Fallback to simple manual ink threshold
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
            .padding(14.dp)
    ) {
        // Header
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
                    fontSize = 13.5.sp
                )
                Text(
                    text = subtitle,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { photoPickerLauncher.launch("image/*") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.UploadFile,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (signatureBitmap == null && croppedBitmap == null) "গ্যালারি থেকে বাছুন" else "ছবি পরিবর্তন",
                    fontSize = 11.5.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Action Toolbar & Settings (when image loaded)
        if (signatureBitmap != null || croppedBitmap != null) {
            Spacer(modifier = Modifier.height(12.dp))

            // Tool actions: [Crop] [AI BG Remove / Manual] [Clear]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Crop button
                    Button(
                        onClick = { showCropDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                        border = borderStrokeSafe(Color(0xFF38BDF8)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Crop, contentDescription = "Crop", tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ক্রপ", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                        label = { Text(if (isAiBgRemove) "AI BG ON" else "AI BG OFF", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp))
                        },
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
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("মুছে ফেলুন", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Signature Ink Color Picker / Toggle
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "কালির রং (Ink Color):",
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SignatureInkColor.values().forEach { colorOption ->
                            val isSelected = (inkColor == colorOption)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) colorOption.previewColor else Color(0xFF1E293B),
                                border = if (isSelected) borderStrokeSafe(Color.White) else borderStrokeSafe(Color(0xFF334155)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(colorOption.previewColor)
                                            .border(1.dp, Color.White, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = colorOption.labelEn,
                                        fontSize = 10.5.sp,
                                        color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                // Ink options click handler
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SignatureInkColor.values().forEach { option ->
                        Button(
                            onClick = {
                                inkColor = option
                                croppedBitmap?.let { bmp ->
                                    executeProcessing(bmp, isAiBgRemove, option, thresholdValue, contrastValue)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (inkColor == option) Color(0xFF2563EB) else Color(0xFF1E293B)
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = option.labelBn,
                                fontSize = 10.5.sp,
                                color = if (inkColor == option) Color.White else Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Cutoff / Threshold Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "সাদা কাগজ কাটঅফ (Threshold): $thresholdValue",
                        color = Color(0xFFE2E8F0),
                        fontSize = 11.sp
                    )
                    Text(
                        text = if (thresholdValue < 150) "কড়া ব্যাকগ্রাউন্ড সাফ" else "হালকা কাট",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }
                Slider(
                    value = thresholdValue.toFloat(),
                    onValueChange = { newVal ->
                        thresholdValue = newVal.toInt()
                        croppedBitmap?.let { bmp ->
                            executeProcessing(bmp, isAiBgRemove, inkColor, thresholdValue, contrastValue)
                        }
                    },
                    valueRange = 100f..230f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF38BDF8),
                        activeTrackColor = Color(0xFF38BDF8),
                        inactiveTrackColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.height(30.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Ink Contrast / Sharpness Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "কালির গাঢ়ত্ব (Contrast): ${String.format(Locale.US, "%.1f", contrastValue)}x",
                        color = Color(0xFFE2E8F0),
                        fontSize = 11.sp
                    )
                    Text(
                        text = if (contrastValue > 1.4f) "অতিরিক্ত স্পষ্ট" else "স্বাভাবিক",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }
                Slider(
                    value = contrastValue,
                    onValueChange = { newVal ->
                        contrastValue = newVal
                        croppedBitmap?.let { bmp ->
                            executeProcessing(bmp, isAiBgRemove, inkColor, thresholdValue, contrastValue)
                        }
                    },
                    valueRange = 0.8f..2.2f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF22C55E),
                        activeTrackColor = Color(0xFF22C55E),
                        inactiveTrackColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.height(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Signature Live Transparent Canvas Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFF2563EB),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "স্বাক্ষর প্রসেসিং হচ্ছে...",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    val displayBitmap = signatureBitmap ?: croppedBitmap
                    if (displayBitmap != null) {
                        Image(
                            bitmap = displayBitmap.asImageBitmap(),
                            contentDescription = "Processed Signature",
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(6.dp)
                        )
                    }
                }
            }
        }
    }

    // Interactive Crop Dialog
    if (showCropDialog && (rawOriginalBitmap != null || croppedBitmap != null)) {
        val targetCropSource = rawOriginalBitmap ?: croppedBitmap!!
        SignatureCropDialog(
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
 * Visual interactive crop dialog with margin sliders and quick presets.
 */
@Composable
fun SignatureCropDialog(
    sourceBitmap: Bitmap,
    onCropConfirmed: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    var cropTopPercent by remember { mutableFloatStateOf(0f) }
    var cropBottomPercent by remember { mutableFloatStateOf(0f) }
    var cropLeftPercent by remember { mutableFloatStateOf(0f) }
    var cropRightPercent by remember { mutableFloatStateOf(0f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Crop,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "স্বাক্ষর ক্রপ করুন (Crop)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "কাগজের অতিরিক্ত বর্ডার কেটে শুধু স্বাক্ষরের অংশটি বাছুন:",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Crop Preview Area with visual green box boundary
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = sourceBitmap.asImageBitmap(),
                        contentDescription = "Crop Source",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )

                    // Overlay boundary showing cropped area
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .padding(
                                start = (cropLeftPercent * 180).dp,
                                top = (cropTopPercent * 130).dp,
                                end = (cropRightPercent * 180).dp,
                                bottom = (cropBottomPercent * 130).dp
                            )
                            .border(2.dp, Color(0xFF22C55E), RoundedCornerShape(4.dp))
                            .background(Color(0x3322C55E))
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = cropTopPercent == 0f && cropBottomPercent == 0f && cropLeftPercent == 0f && cropRightPercent == 0f,
                        onClick = {
                            cropTopPercent = 0f
                            cropBottomPercent = 0f
                            cropLeftPercent = 0f
                            cropRightPercent = 0f
                        },
                        label = { Text("পুরো ছবি", fontSize = 10.5.sp) }
                    )
                    FilterChip(
                        selected = cropTopPercent == 0.12f,
                        onClick = {
                            cropTopPercent = 0.12f
                            cropBottomPercent = 0.12f
                            cropLeftPercent = 0.08f
                            cropRightPercent = 0.08f
                        },
                        label = { Text("মাঝারি ক্রপ", fontSize = 10.5.sp) }
                    )
                    FilterChip(
                        selected = cropTopPercent == 0.22f,
                        onClick = {
                            cropTopPercent = 0.22f
                            cropBottomPercent = 0.22f
                            cropLeftPercent = 0.15f
                            cropRightPercent = 0.15f
                        },
                        label = { Text("টাইট ক্রপ", fontSize = 10.5.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sliders
                Text(
                    text = "মার্জিন অ্যাডজাস্ট (Sliders):",
                    color = Color(0xFFFBBF24),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("উপর (Top):", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(68.dp))
                    Slider(
                        value = cropTopPercent,
                        onValueChange = { cropTopPercent = it },
                        valueRange = 0f..0.45f,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${(cropTopPercent * 100).toInt()}%", color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.width(36.dp))
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("নিচ (Bottom):", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(68.dp))
                    Slider(
                        value = cropBottomPercent,
                        onValueChange = { cropBottomPercent = it },
                        valueRange = 0f..0.45f,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${(cropBottomPercent * 100).toInt()}%", color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.width(36.dp))
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("পাশ (Sides):", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(68.dp))
                    Slider(
                        value = cropLeftPercent,
                        onValueChange = {
                            cropLeftPercent = it
                            cropRightPercent = it
                        },
                        valueRange = 0f..0.45f,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${(cropLeftPercent * 100).toInt()}%", color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.width(36.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = sourceBitmap.width
                    val h = sourceBitmap.height
                    val left = (cropLeftPercent * w).toInt().coerceIn(0, (w - 10).coerceAtLeast(0))
                    val top = (cropTopPercent * h).toInt().coerceIn(0, (h - 10).coerceAtLeast(0))
                    val right = (w - cropRightPercent * w).toInt().coerceIn((left + 10).coerceAtMost(w), w)
                    val bottom = (h - cropBottomPercent * h).toInt().coerceIn((top + 10).coerceAtMost(h), h)
                    val cropW = (right - left).coerceAtLeast(1)
                    val cropH = (bottom - top).coerceAtLeast(1)
                    val cropped = Bitmap.createBitmap(sourceBitmap, left, top, cropW, cropH)
                    onCropConfirmed(cropped)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("ক্রপ নিশ্চিত করুন", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল", color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
        },
        containerColor = Color(0xFF1E293B)
    )
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

private fun borderStrokeSafe(color: Color): androidx.compose.foundation.BorderStroke {
    return androidx.compose.foundation.BorderStroke(1.dp, color)
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
