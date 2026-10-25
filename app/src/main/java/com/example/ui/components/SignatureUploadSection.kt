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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Reusable Signature Upload and Background Removal component.
 * Supports:
 * 1. Gallery selection via ActivityResultContracts.GetContent()
 * 2. On-device AI Background Removal using BiRefNet Lite (birefnet_lite_fp16.onnx)
 * 3. Original / Manual mode
 * 4. Remove signature to restore blank dot-line (................)
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

    var rawBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isAiBgRemove by remember { mutableStateOf(true) }
    var isProcessingAi by remember { mutableStateOf(false) }

    // Synchronize if signature is cleared externally
    LaunchedEffect(signatureBitmap) {
        if (signatureBitmap == null) {
            rawBitmap = null
        }
    }

    fun executeAiBgRemoval(bmp: Bitmap) {
        isProcessingAi = true
        scope.launch(Dispatchers.Default) {
            try {
                val result = BiRefNetLiteEngine.removeBackground(context, bmp).getOrNull()
                withContext(Dispatchers.Main) {
                    isProcessingAi = false
                    if (result != null) {
                        onSignatureChanged(result.transparentForeground)
                    } else {
                        // Fallback: apply ink luminance threshold
                        onSignatureChanged(applyInkThreshold(bmp, 180))
                    }
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    isProcessingAi = false
                    onSignatureChanged(applyInkThreshold(bmp, 180))
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
                            rawBitmap = decoded
                            if (isAiBgRemove) {
                                executeAiBgRemoval(decoded)
                            } else {
                                onSignatureChanged(decoded)
                            }
                        } else {
                            Toast.makeText(context, "ছবি লোড করা যায়নি", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "স্বাক্ষর লোড করতে ত্রুটি হয়েছে", Toast.LENGTH_SHORT).show()
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
                    text = if (signatureBitmap == null) "গ্যালারি থেকে বাছুন" else "পরিবর্তন",
                    fontSize = 11.5.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (signatureBitmap != null || rawBitmap != null) {
            Spacer(modifier = Modifier.height(12.dp))

            // Background Removal & Toggle Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // [AI BG Remove] Toggle
                    FilterChip(
                        selected = isAiBgRemove,
                        onClick = {
                            isAiBgRemove = true
                            rawBitmap?.let { bmp ->
                                executeAiBgRemoval(bmp)
                            }
                        },
                        label = { Text("AI BG Remove", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF22C55E),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF0F172A),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )

                    // [Original / Manual] Toggle
                    FilterChip(
                        selected = !isAiBgRemove,
                        onClick = {
                            isAiBgRemove = false
                            rawBitmap?.let { bmp ->
                                onSignatureChanged(bmp)
                            }
                        },
                        label = { Text("Original / Manual", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF3B82F6),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF0F172A),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }

                // [Remove Signature (মুছে ফেলুন)] button
                TextButton(
                    onClick = {
                        rawBitmap = null
                        onSignatureChanged(null)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("মুছে ফেলুন", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Signature Preview Canvas (White paper / transparent grid feel)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessingAi) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFF2563EB),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI ব্যাকগ্রাউন্ড রিমুভ হচ্ছে...",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    val displayBitmap = signatureBitmap ?: rawBitmap
                    if (displayBitmap != null) {
                        Image(
                            bitmap = displayBitmap.asImageBitmap(),
                            contentDescription = "Uploaded Signature",
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(6.dp)
                        )
                    }
                }
            }
        }
    }
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
 * Ink luminance threshold fallback: converts bright paper pixels to transparent,
 * preserving dark ink strokes.
 */
fun applyInkThreshold(source: Bitmap, threshold: Int = 180): Bitmap {
    val width = source.width
    val height = source.height
    val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)
    for (i in pixels.indices) {
        val pixel = pixels[i]
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        val lum = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
        if (lum > threshold) {
            pixels[i] = 0 // Transparent background
        } else {
            pixels[i] = (255 shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
    outBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return outBitmap
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
