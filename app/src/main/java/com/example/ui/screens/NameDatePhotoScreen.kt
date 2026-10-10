package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.storage.GlobalSaveManager
import com.example.ui.ActiveModal
import com.example.ui.AppScreen
import com.example.ui.PdfMasterViewModel
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ToolBlue
import com.example.ui.theme.ToolGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class PassportPreset(val labelEn: String, val labelBn: String, val widthPx: Int, val heightPx: Int) {
    STANDARD_35_45("Standard 3.5×4.5 cm", "স্ট্যান্ডার্ড ৩.৫×৪.৫ সেমি", 413, 531),
    SSC_CENTRAL("SSC / Central Exam", "SSC / কেন্দ্রীয় পরীক্ষা", 413, 531),
    UPSC_RAILWAY("UPSC / Railway", "UPSC / রেলওয়ে", 413, 531),
    SQUARE_CUSTOM("2×2 inch (Visa)", "২×২ ইঞ্চি (ভিসা)", 600, 600),
    ORIGINAL_RATIO("Keep Original Ratio", "আসল অনুপাত বজায় রাখুন", 0, 0)
}

enum class DatePrefixType(val prefix: String, val labelEn: String, val labelBn: String) {
    DOP("D.O.P: ", "Date of Photo (DOP)", "ছবি তোলার তারিখ (DOP)"),
    DOB("D.O.B: ", "Date of Birth (DOB)", "জন্মতারিখ (DOB)"),
    DATE_ONLY("", "Date Only (No prefix)", "শুধুমাত্র তারিখ")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NameDatePhotoScreen(
    viewModel: PdfMasterViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val language by viewModel.appLanguage.collectAsState()
    val isBn = language == "bn"

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var candidateName by remember { mutableStateOf("RAHUL SHARMA") }
    var selectedDateText by remember {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        mutableStateOf(sdf.format(Date()))
    }
    var prefixType by remember { mutableStateOf(DatePrefixType.DOP) }
    var selectedPreset by remember { mutableStateOf(PassportPreset.STANDARD_35_45) }
    var stripHeightPercent by remember { mutableFloatStateOf(0.20f) } // 20% bottom strip
    var isSaving by remember { mutableStateOf(false) }

    // DatePicker Dialog state
    val calendar = remember { Calendar.getInstance() }
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(year, month, dayOfMonth)
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                selectedDateText = sdf.format(cal.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream)
                        loadedBitmap = bmp
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Re-render preview whenever inputs change
    LaunchedEffect(loadedBitmap, candidateName, selectedDateText, prefixType, selectedPreset, stripHeightPercent) {
        val bmp = loadedBitmap ?: return@LaunchedEffect
        withContext(Dispatchers.Default) {
            val rendered = renderNameDatePhoto(
                source = bmp,
                candidateName = candidateName,
                dateText = selectedDateText,
                prefix = prefixType.prefix,
                preset = selectedPreset,
                stripRatio = stripHeightPercent
            )
            previewBitmap = rendered
        }
    }

    BackHandler {
        onBack()
    }

    AppScreen(modifier = modifier, isScrollable = false) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = if (isBn) "ছবিতে নাম ও তারিখ" else "Name & Date on Photo",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isBn) "চাকরি ও পরীক্ষার ছবি তৈরি করুন" else "Govt Exam & Cyber Cafe Studio Tool",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("name_date_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        if (previewBitmap != null) {
                            Button(
                                onClick = {
                                    if (isSaving) return@Button
                                    isSaving = true
                                    coroutineScope.launch {
                                        try {
                                            val finalBmp = previewBitmap ?: return@launch
                                            val tempFile = File(context.cacheDir, "Passport_NameDate_${System.currentTimeMillis()}.jpg")
                                            FileOutputStream(tempFile).use { out ->
                                                finalBmp.compress(Bitmap.CompressFormat.JPEG, 95, out)
                                                out.flush()
                                            }
                                            val safeCandidate = candidateName.trim().replace("\\s+".toRegex(), "_").take(15)
                                            val cleanBaseName = if (safeCandidate.isNotBlank()) "Photo_${safeCandidate}_${System.currentTimeMillis()}" else "Passport_NameDate_${System.currentTimeMillis()}"
                                            viewModel.commitAndShowResult(
                                                sourceOutputFile = tempFile,
                                                desiredName = "$cleanBaseName.jpg",
                                                toolTitle = if (isBn) "ছবিতে নাম ও তারিখ" else "Name & Date on Photo",
                                                summaryText = if (isBn) "নাম ও তারিখ সহ পাসপোর্ট ছবি সফলভাবে সংরক্ষিত হয়েছে।" else "Passport photo with candidate name & date exported successfully.",
                                                mimeType = "image/jpeg",
                                                category = "photos",
                                                tag = "Name & Date Photo"
                                            )
                                        } catch (e: Exception) {
                                            viewModel.showNotification("Export error: ${e.message}", false)
                                        } finally {
                                            isSaving = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                enabled = !isSaving,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .testTag("export_name_date_photo_button")
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isBn) "সংরক্ষণ" else "Save Photo", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // PHOTO PICKER OR PREVIEW CARD
                if (previewBitmap == null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("select_photo_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(36.dp))
                                    .background(CrimsonPrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Upload",
                                    tint = CrimsonPrimary,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (isBn) "পাসপোর্ট ছবি সিলেক্ট করুন" else "Select Passport Photo",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isBn) "গ্যালারি থেকে প্রার্থীর ছবি নির্বাচন করুন" else "Tap to choose candidate photo from gallery",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // PREVIEW CARD
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("preview_photo_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isBn) "লাইভ প্রিভিউ (সাদা বর্ডারে নাম ও তারিখ)" else "Live Candidate Photo Preview",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Box(
                                modifier = Modifier
                                    .size(width = 200.dp, height = 250.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                previewBitmap?.let { bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Passport Preview",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.testTag("change_photo_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isBn) "অন্য ছবি নির্বাচন করুন" else "Change Photo")
                            }
                        }
                    }
                }

                // CANDIDATE DETAILS FORM
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = if (isBn) "প্রার্থীর তথ্য ও সেটিংস" else "Candidate Details & Strip Format",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // CANDIDATE NAME INPUT
                        OutlinedTextField(
                            value = candidateName,
                            onValueChange = { candidateName = it.uppercase() },
                            label = { Text(if (isBn) "প্রার্থীর নাম (Candidate Name)" else "Candidate Name") },
                            placeholder = { Text("e.g. RAHUL SHARMA") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            trailingIcon = {
                                if (candidateName.isNotEmpty()) {
                                    IconButton(onClick = { candidateName = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("candidate_name_input")
                        )

                        // DATE PREFIX TYPE CHIPS
                        Column {
                            Text(
                                text = if (isBn) "তারিখের ধরন" else "Date Type / Prefix",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DatePrefixType.values().forEach { type ->
                                    FilterChip(
                                        selected = prefixType == type,
                                        onClick = { prefixType = type },
                                        label = {
                                            Text(
                                                text = if (isBn) type.labelBn else type.labelEn,
                                                fontSize = 12.sp,
                                                fontWeight = if (prefixType == type) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ToolBlue.copy(alpha = 0.2f),
                                            selectedLabelColor = ToolBlue
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // DATE OF PHOTO / DOB INPUT WITH CALENDAR PICKER
                        OutlinedTextField(
                            value = selectedDateText,
                            onValueChange = { selectedDateText = it },
                            label = {
                                Text(
                                    when (prefixType) {
                                        DatePrefixType.DOP -> if (isBn) "ছবি তোলার তারিখ (DOP)" else "Date of Photo (DOP)"
                                        DatePrefixType.DOB -> if (isBn) "জন্মতারিখ (DOB)" else "Date of Birth (DOB)"
                                        DatePrefixType.DATE_ONLY -> if (isBn) "তারিখ" else "Date"
                                    }
                                )
                            },
                            placeholder = { Text("DD/MM/YYYY") },
                            leadingIcon = {
                                IconButton(
                                    onClick = { datePickerDialog.show() },
                                    modifier = Modifier.testTag("btn_open_calendar")
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date", tint = CrimsonPrimary)
                                }
                            },
                            trailingIcon = {
                                IconButton(onClick = { datePickerDialog.show() }) {
                                    Icon(Icons.Default.EditCalendar, contentDescription = "Calendar")
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("candidate_date_input")
                        )

                        // DIMENSION PRESETS
                        Column {
                            Text(
                                text = if (isBn) "পাসপোর্ট ছবির মাপ (Presets)" else "Output Size / Exam Specification",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            PassportPreset.values().forEach { preset ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedPreset = preset }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedPreset == preset,
                                        onClick = { selectedPreset = preset }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (isBn) preset.labelBn else preset.labelEn,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (selectedPreset == preset) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                        if (preset.widthPx > 0) {
                                            Text(
                                                text = "${preset.widthPx} × ${preset.heightPx} px (300 DPI)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // STRIP HEIGHT SLIDER
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isBn) "নিচের সাদা স্ট্রিপের উচ্চতা" else "Bottom White Strip Height",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${(stripHeightPercent * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = CrimsonPrimary
                                )
                            }
                            Slider(
                                value = stripHeightPercent,
                                onValueChange = { stripHeightPercent = it },
                                valueRange = 0.15f..0.28f,
                                steps = 12,
                                colors = SliderDefaults.colors(
                                    thumbColor = CrimsonPrimary,
                                    activeTrackColor = CrimsonPrimary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * High-precision canvas renderer that draws a crisp white bottom overlay bar
 * and bold black text centered, with auto-scaling to prevent overflow.
 */
private fun renderNameDatePhoto(
    source: Bitmap,
    candidateName: String,
    dateText: String,
    prefix: String,
    preset: PassportPreset,
    stripRatio: Float
): Bitmap {
    val targetWidth = if (preset.widthPx > 0) preset.widthPx else source.width
    val targetHeight = if (preset.heightPx > 0) preset.heightPx else source.height

    // Scale candidate source bitmap to fill target dimensions
    val scaledSource = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    val resultBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(resultBitmap)

    // 1. Draw base photo
    canvas.drawBitmap(scaledSource, 0f, 0f, null)

    // 2. Draw white rectangular overlay at the bottom
    val stripHeight = (targetHeight * stripRatio).coerceIn(targetHeight * 0.12f, targetHeight * 0.32f)
    val stripTop = targetHeight - stripHeight

    val stripPaint = Paint().apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawRect(0f, stripTop, targetWidth.toFloat(), targetHeight.toFloat(), stripPaint)

    // Draw subtle top border line for the white strip to make it crisp
    val borderPaint = Paint().apply {
        color = AndroidColor.argb(70, 0, 0, 0)
        strokeWidth = (targetWidth * 0.003f).coerceAtLeast(1f)
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    canvas.drawLine(0f, stripTop, targetWidth.toFloat(), stripTop, borderPaint)

    // 3. Render Candidate Name & Date
    val nameStr = candidateName.trim().uppercase()
    val fullDateStr = (prefix + dateText.trim()).uppercase()

    val availableWidth = targetWidth * 0.92f
    val halfStrip = stripHeight / 2f

    // Text paint for candidate name
    val namePaint = Paint().apply {
        color = AndroidColor.BLACK
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    // Auto-scale font size for candidate name so it never wraps or overflows
    var nameFontSize = stripHeight * 0.36f
    namePaint.textSize = nameFontSize
    while (nameStr.isNotEmpty() && namePaint.measureText(nameStr) > availableWidth && nameFontSize > 10f) {
        nameFontSize -= 1f
        namePaint.textSize = nameFontSize
    }

    // Text paint for Date
    val datePaint = Paint().apply {
        color = AndroidColor.BLACK
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    var dateFontSize = stripHeight * 0.32f
    datePaint.textSize = dateFontSize
    while (fullDateStr.isNotEmpty() && datePaint.measureText(fullDateStr) > availableWidth && dateFontSize > 10f) {
        dateFontSize -= 1f
        datePaint.textSize = dateFontSize
    }

    // Calculate vertical centers for two lines of text
    val nameY = stripTop + (stripHeight * 0.42f)
    val dateY = stripTop + (stripHeight * 0.82f)

    if (nameStr.isNotBlank()) {
        canvas.drawText(nameStr, targetWidth / 2f, nameY, namePaint)
    }
    if (fullDateStr.isNotBlank()) {
        canvas.drawText(fullDateStr, targetWidth / 2f, dateY, datePaint)
    }

    return resultBitmap
}
