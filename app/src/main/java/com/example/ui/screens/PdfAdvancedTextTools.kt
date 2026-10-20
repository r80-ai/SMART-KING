package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.graphics.Typeface as AndroidTypeface
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pdf.PdfEngine
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ToolBlue
import java.io.File
import kotlin.math.roundToInt

enum class TextToolActionId {
    EDIT, DELETE, COPY, TO_FRONT, TO_BACK,
    POSITION, REL_POSITION, SIZE, PADDING, COLOR,
    TEXTURE, OPACITY, ROTATE, MASK, FONT,
    STYLE, CURVE, BACKGROUND, ALIGN, SPACING,
    LINE_SPACING, STROKE, SHADOW, INNER_SHADOW, EMBOSS,
    PERSPECTIVE, ROTATE_3D, TEXT_3D, SHADOW_3D, REFLECTION
}

data class TextToolButtonDef(
    val id: TextToolActionId,
    val label: String,
    val icon: ImageVector
)

val ADVANCED_TEXT_TOOLS: List<TextToolButtonDef> = listOf(
    TextToolButtonDef(TextToolActionId.EDIT, "Edit", Icons.Default.Edit),
    TextToolButtonDef(TextToolActionId.DELETE, "Delete", Icons.Default.Delete),
    TextToolButtonDef(TextToolActionId.COPY, "Copy", Icons.Default.ContentCopy),
    TextToolButtonDef(TextToolActionId.TO_FRONT, "To Front", Icons.Default.FlipToFront),
    TextToolButtonDef(TextToolActionId.TO_BACK, "To Back", Icons.Default.FlipToBack),
    TextToolButtonDef(TextToolActionId.POSITION, "Position", Icons.Default.OpenWith),
    TextToolButtonDef(TextToolActionId.REL_POSITION, "Relative Position", Icons.Default.AspectRatio),
    TextToolButtonDef(TextToolActionId.SIZE, "Size", Icons.Default.FormatSize),
    TextToolButtonDef(TextToolActionId.PADDING, "Padding", Icons.Default.SpaceDashboard),
    TextToolButtonDef(TextToolActionId.COLOR, "Color", Icons.Default.Palette),
    TextToolButtonDef(TextToolActionId.TEXTURE, "Texture", Icons.Default.Image),
    TextToolButtonDef(TextToolActionId.OPACITY, "Opacity", Icons.Default.Opacity),
    TextToolButtonDef(TextToolActionId.ROTATE, "Rotate", Icons.Default.RotateRight),
    TextToolButtonDef(TextToolActionId.MASK, "Mask", Icons.Default.Crop),
    TextToolButtonDef(TextToolActionId.FONT, "Font", Icons.Default.FontDownload),
    TextToolButtonDef(TextToolActionId.STYLE, "Style", Icons.Default.FormatBold),
    TextToolButtonDef(TextToolActionId.CURVE, "Curve", Icons.Default.Timeline),
    TextToolButtonDef(TextToolActionId.BACKGROUND, "Background", Icons.Default.FormatColorFill),
    TextToolButtonDef(TextToolActionId.ALIGN, "Align", Icons.Default.FormatAlignCenter),
    TextToolButtonDef(TextToolActionId.SPACING, "Spacing", Icons.Default.SpaceBar),
    TextToolButtonDef(TextToolActionId.LINE_SPACING, "Line Spacing", Icons.Default.FormatLineSpacing),
    TextToolButtonDef(TextToolActionId.STROKE, "Stroke", Icons.Default.BorderColor),
    TextToolButtonDef(TextToolActionId.SHADOW, "Shadow", Icons.Default.WbSunny),
    TextToolButtonDef(TextToolActionId.INNER_SHADOW, "Inner Shadow", Icons.Default.InvertColors),
    TextToolButtonDef(TextToolActionId.EMBOSS, "Emboss", Icons.Default.AutoFixHigh),
    TextToolButtonDef(TextToolActionId.PERSPECTIVE, "Perspective", Icons.Default.Transform),
    TextToolButtonDef(TextToolActionId.ROTATE_3D, "3D Rotate", Icons.Default.ScreenRotation),
    TextToolButtonDef(TextToolActionId.TEXT_3D, "3D Text", Icons.Default.ViewInAr),
    TextToolButtonDef(TextToolActionId.SHADOW_3D, "3D Shadow", Icons.Default.Gradient),
    TextToolButtonDef(TextToolActionId.REFLECTION, "Reflection", Icons.Default.Flip)
)

val PRESET_COLORS: List<Int> = listOf(
    AndroidColor.BLACK,
    AndroidColor.WHITE,
    AndroidColor.rgb(30, 41, 59),
    AndroidColor.rgb(220, 38, 38),
    AndroidColor.rgb(239, 68, 68),
    AndroidColor.rgb(217, 119, 6),
    AndroidColor.rgb(234, 179, 8),
    AndroidColor.rgb(22, 163, 74),
    AndroidColor.rgb(16, 185, 129),
    AndroidColor.rgb(14, 165, 233),
    AndroidColor.rgb(37, 99, 235),
    AndroidColor.rgb(79, 70, 229),
    AndroidColor.rgb(147, 51, 234),
    AndroidColor.rgb(219, 39, 119),
    AndroidColor.rgb(107, 114, 128)
)

/**
 * Interactive text overlay for the PDF editor canvas.
 * Renders on top of the PDF page, handles touch selection, dragging, and the 4 active corner handles:
 * - Top-Left: Edit (pencil icon) -> opens text edit dialog
 * - Top-Right: Delete (cross icon) -> removes layer
 * - Bottom-Right: Resize & Scale handle -> adjusts size
 * - Bottom-Left: Rotate handle -> free rotation
 */
@Composable
fun InteractivePdfTextOverlay(
    item: PdfEngine.EditItem,
    isSelected: Boolean,
    zoomScale: Float,
    pageWidthPx: Float,
    pageHeightPx: Float,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: (PdfEngine.EditItem) -> Unit,
    onHistoryPush: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var measuredSize by remember { mutableStateOf(IntSize.Zero) }

    // Map font family
    val customFontFamily = remember(item.customFontPath, item.fontFamily) {
        when {
            item.customFontPath != null -> {
                try {
                    val tf = AndroidTypeface.createFromFile(item.customFontPath)
                    FontFamily(tf)
                } catch (_: Exception) {
                    FontFamily.Default
                }
            }
            item.fontFamily.equals("BENGALI", ignoreCase = true) -> {
                try {
                    val tf = androidx.core.content.res.ResourcesCompat.getFont(
                        context,
                        com.example.R.font.noto_sans_bengali
                    )
                    if (tf != null) FontFamily(tf) else FontFamily.Default
                } catch (_: Exception) {
                    FontFamily.Default
                }
            }
            item.fontFamily.equals("SERIF", ignoreCase = true) -> FontFamily.Serif
            item.fontFamily.equals("MONOSPACE", ignoreCase = true) -> FontFamily.Monospace
            item.fontFamily.equals("HANDWRITING", ignoreCase = true) || item.fontFamily.equals("CURSIVE", ignoreCase = true) -> FontFamily.Cursive
            else -> FontFamily.Default
        }
    }

    val textDecoration = remember(item.isUnderline, item.isStrikethrough) {
        when {
            item.isUnderline && item.isStrikethrough -> TextDecoration.combine(
                listOf(TextDecoration.Underline, TextDecoration.LineThrough)
            )
            item.isUnderline -> TextDecoration.Underline
            item.isStrikethrough -> TextDecoration.LineThrough
            else -> TextDecoration.None
        }
    }

    val textAlign = remember(item.alignment) {
        when (item.alignment.uppercase()) {
            "CENTER" -> TextAlign.Center
            "RIGHT" -> TextAlign.End
            "JUSTIFY" -> TextAlign.Justify
            else -> TextAlign.Start
        }
    }

    val textShadow = remember(item.shadowRadius, item.shadowColorHex, item.shadowDx, item.shadowDy) {
        if (item.shadowRadius > 0f && item.shadowColorHex != 0) {
            Shadow(
                color = Color(item.shadowColorHex),
                offset = androidx.compose.ui.geometry.Offset(item.shadowDx, item.shadowDy),
                blurRadius = item.shadowRadius
            )
        } else {
            null
        }
    }

    val textToDisplay = if (item.text.isEmpty()) " " else item.text

    Box(
        modifier = Modifier
            .offset { IntOffset(item.posX.roundToInt(), item.posY.roundToInt()) }
            .graphicsLayer {
                rotationZ = item.getRotationAngle()
                rotationX = item.rotate3dX
                rotationY = item.rotate3dY
            }
            .pointerInput(item.id) {
                detectTapGestures(
                    onTap = { onSelect() }
                )
            }
            .pointerInput(item.id) {
                detectDragGestures(
                    onDragStart = {
                        onHistoryPush()
                        onSelect()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val scale = zoomScale.coerceAtLeast(0.1f)
                        val dX = dragAmount.x / scale
                        val dY = dragAmount.y / scale
                        val newX = (item.posX + dX).coerceIn(-100f, pageWidthPx + 100f)
                        val newY = (item.posY + dY).coerceIn(-100f, pageHeightPx + 100f)
                        onUpdate(item.copy(posX = newX, posY = newY))
                    }
                )
            }
    ) {
        // Text Content Container with optional background styling
        Column(
            modifier = Modifier
                .onSizeChanged { measuredSize = it }
                .then(
                    if (item.backgroundColorHex != 0) {
                        Modifier.background(
                            Color(item.backgroundColorHex),
                            RoundedCornerShape(item.backgroundCornerRadius.dp)
                        )
                    } else Modifier
                )
                .padding(item.backgroundPadding.dp)
        ) {
            // Optional 3D Text Depth Extrusion
            if (item.is3dText && item.depth3d > 0f) {
                Text(
                    text = textToDisplay,
                    color = Color.DarkGray.copy(alpha = item.opacity * 0.7f),
                    fontSize = item.fontSize.sp,
                    fontWeight = if (item.isBold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (item.isItalic) FontStyle.Italic else FontStyle.Normal,
                    fontFamily = customFontFamily,
                    textAlign = textAlign,
                    letterSpacing = if (item.letterSpacing != 0f) (item.letterSpacing * 0.5f).sp else androidx.compose.ui.unit.TextUnit.Unspecified,
                    modifier = Modifier.offset(x = (item.depth3d * 0.5f).dp, y = (item.depth3d * 0.5f).dp)
                )
            }

            // Primary Main Text Layer
            Text(
                text = textToDisplay,
                color = Color(item.colorHex).copy(alpha = item.opacity),
                fontSize = item.fontSize.sp,
                fontWeight = if (item.isBold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (item.isItalic) FontStyle.Italic else FontStyle.Normal,
                fontFamily = customFontFamily,
                textAlign = textAlign,
                textDecoration = textDecoration,
                letterSpacing = if (item.letterSpacing != 0f) (item.letterSpacing * 0.5f).sp else androidx.compose.ui.unit.TextUnit.Unspecified,
                lineHeight = (item.fontSize * if (item.lineSpacingMultiplier > 0f) item.lineSpacingMultiplier else 1.15f).sp,
                style = TextStyle(shadow = textShadow)
            )

            // Optional Mirror Reflection
            if (item.isReflection) {
                Text(
                    text = textToDisplay,
                    color = Color(item.colorHex).copy(alpha = item.opacity * 0.25f),
                    fontSize = item.fontSize.sp,
                    fontWeight = if (item.isBold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (item.isItalic) FontStyle.Italic else FontStyle.Normal,
                    fontFamily = customFontFamily,
                    textAlign = textAlign,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleY = -0.6f
                            translationY = 8f
                        }
                )
            }
        }

        // Active Bounding Box and 4 Corner Interactive Handles when selected
        if (isSelected) {
            // Rectangular outline frame
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(1.5.dp, ToolBlue, RoundedCornerShape(2.dp))
            )

            val handleSize = 34.dp
            val innerSize = 22.dp

            // 1. TOP-LEFT CORNER: Edit Handle (Pencil Icon)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-17).dp, y = (-17).dp)
                    .size(handleSize)
                    .pointerInput(item.id) {
                        detectTapGestures { onEdit() }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(innerSize)
                        .background(Color.White, CircleShape)
                        .border(1.5.dp, ToolBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Text",
                        tint = ToolBlue,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            // 2. TOP-RIGHT CORNER: Delete Handle (Cross / Trash Icon)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 17.dp, y = (-17).dp)
                    .size(handleSize)
                    .pointerInput(item.id) {
                        detectTapGestures { onDelete() }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(innerSize)
                        .background(Color.White, CircleShape)
                        .border(1.5.dp, CrimsonPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Delete Text",
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            // 3. BOTTOM-LEFT CORNER: Free Rotate Handle
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-17).dp, y = 17.dp)
                    .size(handleSize)
                    .pointerInput(item.id) {
                        detectDragGestures(
                            onDragStart = { onHistoryPush() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val scale = zoomScale.coerceAtLeast(0.1f)
                                val delta = (dragAmount.x - dragAmount.y) * 0.5f / scale
                                val currentRot = item.getRotationAngle()
                                val newRot = ((currentRot + delta) % 360f + 360f) % 360f
                                onUpdate(item.copy(rotation = newRot, imageRotation = newRot))
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(innerSize)
                        .background(Color.White, CircleShape)
                        .border(1.5.dp, ToolBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Rotate Text",
                        tint = ToolBlue,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            // 4. BOTTOM-RIGHT CORNER: Resize & Scale Handle
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 17.dp, y = 17.dp)
                    .size(handleSize)
                    .pointerInput(item.id) {
                        detectDragGestures(
                            onDragStart = { onHistoryPush() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val scale = zoomScale.coerceAtLeast(0.1f)
                                val dX = dragAmount.x / scale
                                val dY = dragAmount.y / scale
                                val delta = (dX + dY) * 0.15f
                                val newSize = (item.fontSize + delta).coerceIn(8f, 140f)
                                onUpdate(item.copy(fontSize = newSize))
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(innerSize)
                        .background(Color.White, CircleShape)
                        .border(1.5.dp, ToolBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.OpenInFull,
                        contentDescription = "Resize Text",
                        tint = ToolBlue,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

/**
 * Bottom sub-tool control panel for PDF Text Edit tab.
 * Displays the 30 tools in a sleek horizontally scrollable action bar, plus interactive controls.
 */
@Composable
fun AdvancedTextToolPanel(
    selectedItem: PdfEngine.EditItem?,
    pageWidthPx: Float,
    pageHeightPx: Float,
    onAddNewText: () -> Unit,
    onOpenEditDialog: () -> Unit,
    onDeleteSelected: () -> Unit,
    onCopySelected: () -> Unit,
    onBringToFront: () -> Unit,
    onSendToBack: () -> Unit,
    onUpdateItem: (PdfEngine.EditItem) -> Unit,
    onHistoryPush: () -> Unit
) {
    val context = LocalContext.current
    var activeSubTool by remember { mutableStateOf<TextToolActionId?>(null) }
    var showFontDialog by remember { mutableStateOf(false) }

    // Launcher for picking texture image
    val texturePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null && selectedItem != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        onHistoryPush()
                        onUpdateItem(selectedItem.copy(textureBitmap = bmp))
                        Toast.makeText(context, "Texture applied to text", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Could not load texture", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Launcher for picking custom TTF/OTF font file
    val fontFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null && selectedItem != null) {
            try {
                val fontsDir = File(context.filesDir, "custom_fonts").apply { mkdirs() }
                val fontFile = File(fontsDir, "font_${System.currentTimeMillis()}.ttf")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    fontFile.outputStream().use { output -> input.copyTo(output) }
                }
                onHistoryPush()
                onUpdateItem(
                    selectedItem.copy(
                        customFontPath = fontFile.absolutePath,
                        customFontName = fontFile.name,
                        fontFamily = "CUSTOM"
                    )
                )
                Toast.makeText(context, "Custom font applied!", Toast.LENGTH_SHORT).show()
                showFontDialog = false
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load font file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        // Quick Header Action row: Add Text button & Selection status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.TextFields,
                    contentDescription = null,
                    tint = ToolBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (selectedItem != null) "Text Selected" else "Tap text or add new",
                    color = if (selectedItem != null) Color.White else Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = onAddNewText,
                colors = ButtonDefaults.buttonColors(containerColor = ToolBlue),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Text", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Active Sub-Tool Controls Panel (Sliders, Palettes, Pickers)
        AnimatedVisibility(visible = activeSubTool != null && selectedItem != null) {
            if (selectedItem != null && activeSubTool != null) {
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = ADVANCED_TEXT_TOOLS.firstOrNull { it.id == activeSubTool }?.label ?: "Control",
                                color = ToolBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { activeSubTool = null },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        when (activeSubTool) {
                            TextToolActionId.COLOR -> {
                                TextColorAndOpacityControl(
                                    item = selectedItem,
                                    onUpdate = onUpdateItem,
                                    onHistoryPush = onHistoryPush,
                                    onPickTexture = {
                                        texturePickerLauncher.launch(
                                            androidx.activity.result.PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    }
                                )
                            }
                            TextToolActionId.SIZE -> {
                                TextSizeControl(item = selectedItem, onUpdate = onUpdateItem, onHistoryPush = onHistoryPush)
                            }
                            TextToolActionId.STYLE -> {
                                TextStyleControl(item = selectedItem, onUpdate = onUpdateItem, onHistoryPush = onHistoryPush)
                            }
                            TextToolActionId.ALIGN -> {
                                TextAlignControl(item = selectedItem, onUpdate = onUpdateItem, onHistoryPush = onHistoryPush)
                            }
                            TextToolActionId.OPACITY -> {
                                TextOpacityControl(item = selectedItem, onUpdate = onUpdateItem, onHistoryPush = onHistoryPush)
                            }
                            TextToolActionId.ROTATE -> {
                                TextRotateControl(item = selectedItem, onUpdate = onUpdateItem, onHistoryPush = onHistoryPush)
                            }
                            TextToolActionId.POSITION -> {
                                TextPositionControl(
                                    item = selectedItem,
                                    pageWidthPx = pageWidthPx,
                                    pageHeightPx = pageHeightPx,
                                    onUpdate = onUpdateItem,
                                    onHistoryPush = onHistoryPush
                                )
                            }
                            TextToolActionId.REL_POSITION -> {
                                TextRelativePositionControl(
                                    item = selectedItem,
                                    pageWidthPx = pageWidthPx,
                                    pageHeightPx = pageHeightPx,
                                    onUpdate = onUpdateItem,
                                    onHistoryPush = onHistoryPush
                                )
                            }
                            TextToolActionId.PADDING -> {
                                TextPaddingControl(item = selectedItem, onUpdate = onUpdateItem, onHistoryPush = onHistoryPush)
                            }
                            TextToolActionId.SPACING -> {
                                TextLetterSpacingControl(item = selectedItem, onUpdate = onUpdateItem, onHistoryPush = onHistoryPush)
                            }
                            TextToolActionId.LINE_SPACING -> {
                                TextLineSpacingControl(item = selectedItem, onUpdate = onUpdateItem, onHistoryPush = onHistoryPush)
                            }
                            TextToolActionId.BACKGROUND -> {
                                TextBackgroundControl(item = selectedItem, onUpdate = onUpdateItem, onHistoryPush = onHistoryPush)
                            }
                            TextToolActionId.STROKE -> {
                                TextStrokeControl(item = selectedItem, onUpdate = onUpdateItem, onHistoryPush = onHistoryPush)
                            }
                            TextToolActionId.SHADOW -> {
                                TextShadowControl(item = selectedItem, onUpdate = onUpdateItem, onHistoryPush = onHistoryPush)
                            }
                            TextToolActionId.INNER_SHADOW -> {
                                TextInnerShadowControl(item = selectedItem, onUpdate = onUpdateItem, onHistoryPush = onHistoryPush)
                            }
                            TextToolActionId.TEXTURE -> {
                                TextTextureControl(
                                    item = selectedItem,
                                    onPickTexture = {
                                        texturePickerLauncher.launch(
                                            androidx.activity.result.PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    },
                                    onClearTexture = {
                                        onHistoryPush()
                                        onUpdateItem(selectedItem.copy(textureBitmap = null))
                                    }
                                )
                            }
                            TextToolActionId.CURVE -> {
                                TextCurveControl(item = selectedItem, onUpdate = onUpdateItem, onHistoryPush = onHistoryPush)
                            }
                            TextToolActionId.MASK -> {
                                TextMaskControl(item = selectedItem, onUpdate = onUpdateItem, onHistoryPush = onHistoryPush)
                            }
                            TextToolActionId.EMBOSS -> {
                                TextEmbossControl(item = selectedItem, onUpdate = onUpdateItem, onHistoryPush = onHistoryPush)
                            }
                            TextToolActionId.PERSPECTIVE -> {
                                TextPerspectiveControl(item = selectedItem, onUpdate = onUpdateItem, onHistoryPush = onHistoryPush)
                            }
                            TextToolActionId.ROTATE_3D -> {
                                Text3dRotateControl(item = selectedItem, onUpdate = onUpdateItem, onHistoryPush = onHistoryPush)
                            }
                            TextToolActionId.TEXT_3D -> {
                                Text3dTextControl(item = selectedItem, onUpdate = onUpdateItem, onHistoryPush = onHistoryPush)
                            }
                            TextToolActionId.SHADOW_3D -> {
                                Text3dShadowControl(item = selectedItem, onUpdate = onUpdateItem, onHistoryPush = onHistoryPush)
                            }
                            TextToolActionId.REFLECTION -> {
                                TextReflectionControl(item = selectedItem, onUpdate = onUpdateItem, onHistoryPush = onHistoryPush)
                            }
                            else -> {}
                        }
                    }
                }
            }
        }

        // Sleek Horizontally Scrollable Action Bar containing all 30 tool buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ADVANCED_TEXT_TOOLS.forEach { tool ->
                val isCurrentActive = activeSubTool == tool.id
                CompactTextToolButton(
                    def = tool,
                    isActive = isCurrentActive,
                    onClick = {
                        if (selectedItem == null && tool.id != TextToolActionId.EDIT) {
                            Toast.makeText(context, "Tap on text to select it first", Toast.LENGTH_SHORT).show()
                            return@CompactTextToolButton
                        }
                        when (tool.id) {
                            TextToolActionId.EDIT -> onOpenEditDialog()
                            TextToolActionId.DELETE -> onDeleteSelected()
                            TextToolActionId.COPY -> onCopySelected()
                            TextToolActionId.TO_FRONT -> onBringToFront()
                            TextToolActionId.TO_BACK -> onSendToBack()
                            TextToolActionId.FONT -> showFontDialog = true
                            else -> {
                                activeSubTool = if (activeSubTool == tool.id) null else tool.id
                            }
                        }
                    }
                )
            }
        }
    }

    // Font Selection Dialog (Tabbed: English & Bengali + Pick Custom TTF/OTF)
    if (showFontDialog && selectedItem != null) {
        FontSelectionDialog(
            currentFont = selectedItem.fontFamily,
            customFontName = selectedItem.customFontName,
            onDismiss = { showFontDialog = false },
            onSelectFont = { familyName, customPath, customName ->
                onHistoryPush()
                onUpdateItem(
                    selectedItem.copy(
                        fontFamily = familyName,
                        customFontPath = customPath,
                        customFontName = customName
                    )
                )
                showFontDialog = false
            },
            onPickCustomFont = {
                fontFilePickerLauncher.launch(arrayOf("*/*"))
            }
        )
    }
}

/**
 * Sleek compact button chip for each tool in the horizontal action bar
 */
@Composable
private fun CompactTextToolButton(
    def: TextToolButtonDef,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) ToolBlue.copy(alpha = 0.22f) else Color(0xFF1E293B),
        border = BorderStroke(
            1.dp,
            if (isActive) ToolBlue else Color(0xFF334155)
        ),
        modifier = Modifier.height(54.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .widthIn(min = 52.dp)
        ) {
            Icon(
                imageVector = def.icon,
                contentDescription = def.label,
                tint = if (isActive) ToolBlue else Color(0xFFE2E8F0),
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = def.label,
                fontSize = 10.sp,
                color = if (isActive) Color.White else Color(0xFF94A3B8),
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}

// ---------------------------------------------------------------------------
// SUB-CONTROLS
// ---------------------------------------------------------------------------

@Composable
private fun TextColorAndOpacityControl(
    item: PdfEngine.EditItem,
    onUpdate: (PdfEngine.EditItem) -> Unit,
    onHistoryPush: () -> Unit,
    onPickTexture: () -> Unit
) {
    var r by remember(item.colorHex) { mutableFloatStateOf(AndroidColor.red(item.colorHex).toFloat()) }
    var g by remember(item.colorHex) { mutableFloatStateOf(AndroidColor.green(item.colorHex).toFloat()) }
    var b by remember(item.colorHex) { mutableFloatStateOf(AndroidColor.blue(item.colorHex).toFloat()) }
    var a by remember(item.opacity) { mutableFloatStateOf(item.opacity) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Preset Palette Swatches
        Text("Quick Colors", fontSize = 11.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PRESET_COLORS.forEach { c ->
                val isSelected = item.colorHex == c
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(c))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) ToolBlue else Color.DarkGray,
                            shape = CircleShape
                        )
                        .clickable {
                            onHistoryPush()
                            r = AndroidColor.red(c).toFloat()
                            g = AndroidColor.green(c).toFloat()
                            b = AndroidColor.blue(c).toFloat()
                            onUpdate(item.copy(colorHex = c))
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        // RGB Sliders
        Text("RGB Sliders: R:${r.toInt()} G:${g.toInt()} B:${b.toInt()}", fontSize = 11.sp, color = Color.LightGray)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("R", fontSize = 10.sp, color = Color.Red, modifier = Modifier.width(16.dp))
            Slider(
                value = r,
                onValueChange = {
                    r = it
                    val newCol = AndroidColor.rgb(r.toInt(), g.toInt(), b.toInt())
                    onUpdate(item.copy(colorHex = newCol))
                },
                valueRange = 0f..255f,
                modifier = Modifier.weight(1f)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("G", fontSize = 10.sp, color = Color.Green, modifier = Modifier.width(16.dp))
            Slider(
                value = g,
                onValueChange = {
                    g = it
                    val newCol = AndroidColor.rgb(r.toInt(), g.toInt(), b.toInt())
                    onUpdate(item.copy(colorHex = newCol))
                },
                valueRange = 0f..255f,
                modifier = Modifier.weight(1f)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("B", fontSize = 10.sp, color = Color.Cyan, modifier = Modifier.width(16.dp))
            Slider(
                value = b,
                onValueChange = {
                    b = it
                    val newCol = AndroidColor.rgb(r.toInt(), g.toInt(), b.toInt())
                    onUpdate(item.copy(colorHex = newCol))
                },
                valueRange = 0f..255f,
                modifier = Modifier.weight(1f)
            )
        }

        // Opacity Slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Alpha ${(a * 100).toInt()}%", fontSize = 10.sp, color = Color.LightGray, modifier = Modifier.width(60.dp))
            Slider(
                value = a,
                onValueChange = {
                    a = it
                    onUpdate(item.copy(opacity = a))
                },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f)
            )
        }

        // Texture fill trigger
        OutlinedButton(
            onClick = onPickTexture,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
        ) {
            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Fill with Photo Texture", fontSize = 11.sp)
        }
    }
}

@Composable
private fun TextSizeControl(
    item: PdfEngine.EditItem,
    onUpdate: (PdfEngine.EditItem) -> Unit,
    onHistoryPush: () -> Unit
) {
    Column {
        Text("Font Size: ${item.fontSize.toInt()} sp", fontSize = 11.sp, color = Color.LightGray)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    onHistoryPush()
                    onUpdate(item.copy(fontSize = (item.fontSize - 1).coerceAtLeast(8f)))
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = ToolBlue)
            }
            Slider(
                value = item.fontSize,
                onValueChange = { onUpdate(item.copy(fontSize = it)) },
                valueRange = 8f..100f,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    onHistoryPush()
                    onUpdate(item.copy(fontSize = (item.fontSize + 1).coerceAtMost(120f)))
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase", tint = ToolBlue)
            }
        }
    }
}

@Composable
private fun TextStyleControl(
    item: PdfEngine.EditItem,
    onUpdate: (PdfEngine.EditItem) -> Unit,
    onHistoryPush: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = item.isBold,
            onClick = {
                onHistoryPush()
                onUpdate(item.copy(isBold = !item.isBold))
            },
            label = { Text("B", fontWeight = FontWeight.Bold) }
        )
        FilterChip(
            selected = item.isItalic,
            onClick = {
                onHistoryPush()
                onUpdate(item.copy(isItalic = !item.isItalic))
            },
            label = { Text("I", fontStyle = FontStyle.Italic) }
        )
        FilterChip(
            selected = item.isUnderline,
            onClick = {
                onHistoryPush()
                onUpdate(item.copy(isUnderline = !item.isUnderline))
            },
            label = { Text("U", textDecoration = TextDecoration.Underline) }
        )
        FilterChip(
            selected = item.isStrikethrough,
            onClick = {
                onHistoryPush()
                onUpdate(item.copy(isStrikethrough = !item.isStrikethrough))
            },
            label = { Text("S", textDecoration = TextDecoration.LineThrough) }
        )
    }
}

@Composable
private fun TextAlignControl(
    item: PdfEngine.EditItem,
    onUpdate: (PdfEngine.EditItem) -> Unit,
    onHistoryPush: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("LEFT", "CENTER", "RIGHT", "JUSTIFY").forEach { align ->
            FilterChip(
                selected = item.alignment.equals(align, ignoreCase = true),
                onClick = {
                    onHistoryPush()
                    onUpdate(item.copy(alignment = align))
                },
                label = { Text(align.lowercase().replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}

@Composable
private fun TextOpacityControl(
    item: PdfEngine.EditItem,
    onUpdate: (PdfEngine.EditItem) -> Unit,
    onHistoryPush: () -> Unit
) {
    Column {
        Text("Opacity: ${(item.opacity * 100).toInt()}%", fontSize = 11.sp, color = Color.LightGray)
        Slider(
            value = item.opacity,
            onValueChange = { onUpdate(item.copy(opacity = it)) },
            valueRange = 0.05f..1.0f
        )
    }
}

@Composable
private fun TextRotateControl(
    item: PdfEngine.EditItem,
    onUpdate: (PdfEngine.EditItem) -> Unit,
    onHistoryPush: () -> Unit
) {
    val rot = item.getRotationAngle()
    Column {
        Text("Rotation Angle: ${rot.toInt()}°", fontSize = 11.sp, color = Color.LightGray)
        Slider(
            value = rot,
            onValueChange = { onUpdate(item.copy(rotation = it, imageRotation = it)) },
            valueRange = 0f..360f
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = { onHistoryPush(); onUpdate(item.copy(rotation = 0f, imageRotation = 0f)) },
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) { Text("0°", fontSize = 10.sp) }
            Button(
                onClick = { onHistoryPush(); onUpdate(item.copy(rotation = 90f, imageRotation = 90f)) },
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) { Text("90°", fontSize = 10.sp) }
            Button(
                onClick = { onHistoryPush(); onUpdate(item.copy(rotation = 180f, imageRotation = 180f)) },
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) { Text("180°", fontSize = 10.sp) }
            Button(
                onClick = { onHistoryPush(); onUpdate(item.copy(rotation = 270f, imageRotation = 270f)) },
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) { Text("270°", fontSize = 10.sp) }
        }
    }
}

@Composable
private fun TextPositionControl(
    item: PdfEngine.EditItem,
    pageWidthPx: Float,
    pageHeightPx: Float,
    onUpdate: (PdfEngine.EditItem) -> Unit,
    onHistoryPush: () -> Unit
) {
    var step by remember { mutableFloatStateOf(5f) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(1f, 5f, 20f).forEach { s ->
                FilterChip(
                    selected = step == s,
                    onClick = { step = s },
                    label = { Text("${s.toInt()}px", fontSize = 10.sp) },
                    modifier = Modifier.height(26.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    onHistoryPush()
                    onUpdate(item.copy(posY = item.posY - step))
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "Up", tint = ToolBlue)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    onHistoryPush()
                    onUpdate(item.copy(posX = item.posX - step))
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Left", tint = ToolBlue)
            }
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(
                onClick = {
                    onHistoryPush()
                    onUpdate(item.copy(posX = item.posX + step))
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Right", tint = ToolBlue)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    onHistoryPush()
                    onUpdate(item.copy(posY = item.posY + step))
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.ArrowDownward, contentDescription = "Down", tint = ToolBlue)
            }
        }
    }
}

@Composable
private fun TextRelativePositionControl(
    item: PdfEngine.EditItem,
    pageWidthPx: Float,
    pageHeightPx: Float,
    onUpdate: (PdfEngine.EditItem) -> Unit,
    onHistoryPush: () -> Unit
) {
    Column {
        Text("Snap to Page Alignments", fontSize = 11.sp, color = Color.LightGray)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
                onClick = {
                    onHistoryPush()
                    onUpdate(item.copy(posX = 40f))
                },
                modifier = Modifier.weight(1f).height(30.dp),
                contentPadding = PaddingValues(2.dp)
            ) { Text("Left", fontSize = 10.sp) }

            OutlinedButton(
                onClick = {
                    onHistoryPush()
                    onUpdate(item.copy(posX = (pageWidthPx - 150f) / 2f))
                },
                modifier = Modifier.weight(1f).height(30.dp),
                contentPadding = PaddingValues(2.dp)
            ) { Text("Center H", fontSize = 10.sp) }

            OutlinedButton(
                onClick = {
                    onHistoryPush()
                    onUpdate(item.copy(posX = pageWidthPx - 200f))
                },
                modifier = Modifier.weight(1f).height(30.dp),
                contentPadding = PaddingValues(2.dp)
            ) { Text("Right", fontSize = 10.sp) }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
                onClick = {
                    onHistoryPush()
                    onUpdate(item.copy(posY = 40f))
                },
                modifier = Modifier.weight(1f).height(30.dp),
                contentPadding = PaddingValues(2.dp)
            ) { Text("Top", fontSize = 10.sp) }

            OutlinedButton(
                onClick = {
                    onHistoryPush()
                    onUpdate(item.copy(posY = (pageHeightPx - 50f) / 2f))
                },
                modifier = Modifier.weight(1f).height(30.dp),
                contentPadding = PaddingValues(2.dp)
            ) { Text("Center V", fontSize = 10.sp) }

            OutlinedButton(
                onClick = {
                    onHistoryPush()
                    onUpdate(item.copy(posY = pageHeightPx - 80f))
                },
                modifier = Modifier.weight(1f).height(30.dp),
                contentPadding = PaddingValues(2.dp)
            ) { Text("Bottom", fontSize = 10.sp) }
        }
    }
}

@Composable
private fun TextPaddingControl(
    item: PdfEngine.EditItem,
    onUpdate: (PdfEngine.EditItem) -> Unit,
    onHistoryPush: () -> Unit
) {
    Column {
        Text("Background Padding: ${item.backgroundPadding.toInt()} dp", fontSize = 11.sp, color = Color.LightGray)
        Slider(
            value = item.backgroundPadding,
            onValueChange = { onUpdate(item.copy(backgroundPadding = it)) },
            valueRange = 0f..30f
        )
    }
}

@Composable
private fun TextLetterSpacingControl(
    item: PdfEngine.EditItem,
    onUpdate: (PdfEngine.EditItem) -> Unit,
    onHistoryPush: () -> Unit
) {
    Column {
        Text("Character Spacing: ${item.letterSpacing.toInt()}", fontSize = 11.sp, color = Color.LightGray)
        Slider(
            value = item.letterSpacing,
            onValueChange = { onUpdate(item.copy(letterSpacing = it)) },
            valueRange = -2f..15f
        )
    }
}

@Composable
private fun TextLineSpacingControl(
    item: PdfEngine.EditItem,
    onUpdate: (PdfEngine.EditItem) -> Unit,
    onHistoryPush: () -> Unit
) {
    val mult = if (item.lineSpacingMultiplier > 0f) item.lineSpacingMultiplier else 1.15f
    Column {
        Text("Line Spacing Multiplier: ${String.format("%.2f", mult)}x", fontSize = 11.sp, color = Color.LightGray)
        Slider(
            value = mult,
            onValueChange = { onUpdate(item.copy(lineSpacingMultiplier = it)) },
            valueRange = 0.8f..2.5f
        )
    }
}

@Composable
private fun TextBackgroundControl(
    item: PdfEngine.EditItem,
    onUpdate: (PdfEngine.EditItem) -> Unit,
    onHistoryPush: () -> Unit
) {
    Column {
        Text("Background Color & Corner Radius", fontSize = 11.sp, color = Color.LightGray)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // None / Transparent
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .border(1.dp, Color.LightGray, CircleShape)
                    .clickable {
                        onHistoryPush()
                        onUpdate(item.copy(backgroundColorHex = 0))
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = "None", tint = Color.LightGray, modifier = Modifier.size(14.dp))
            }

            listOf(
                AndroidColor.WHITE,
                AndroidColor.BLACK,
                AndroidColor.rgb(254, 240, 138), // Pale yellow
                AndroidColor.rgb(219, 234, 254), // Pale blue
                AndroidColor.rgb(220, 252, 231), // Pale green
                AndroidColor.rgb(254, 226, 226), // Pale red
                AndroidColor.rgb(243, 232, 255)  // Pale purple
            ).forEach { c ->
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(c))
                        .border(
                            width = if (item.backgroundColorHex == c) 2.dp else 1.dp,
                            color = if (item.backgroundColorHex == c) ToolBlue else Color.Gray,
                            shape = CircleShape
                        )
                        .clickable {
                            onHistoryPush()
                            onUpdate(item.copy(backgroundColorHex = c))
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Corner Radius: ${item.backgroundCornerRadius.toInt()} dp", fontSize = 11.sp, color = Color.LightGray)
        Slider(
            value = item.backgroundCornerRadius,
            onValueChange = { onUpdate(item.copy(backgroundCornerRadius = it)) },
            valueRange = 0f..24f
        )
    }
}

@Composable
private fun TextStrokeControl(
    item: PdfEngine.EditItem,
    onUpdate: (PdfEngine.EditItem) -> Unit,
    onHistoryPush: () -> Unit
) {
    Column {
        Text("Stroke Width: ${item.textStrokeWidth.toInt()} dp", fontSize = 11.sp, color = Color.LightGray)
        Slider(
            value = item.textStrokeWidth,
            onValueChange = { onUpdate(item.copy(textStrokeWidth = it)) },
            valueRange = 0f..10f
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text("Stroke Color", fontSize = 11.sp, color = Color.LightGray)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PRESET_COLORS.take(8).forEach { c ->
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(c))
                        .border(
                            width = if (item.strokeColorHex == c) 2.dp else 1.dp,
                            color = if (item.strokeColorHex == c) ToolBlue else Color.DarkGray,
                            shape = CircleShape
                        )
                        .clickable {
                            onHistoryPush()
                            onUpdate(item.copy(strokeColorHex = c))
                        }
                )
            }
        }
    }
}

@Composable
private fun TextShadowControl(
    item: PdfEngine.EditItem,
    onUpdate: (PdfEngine.EditItem) -> Unit,
    onHistoryPush: () -> Unit
) {
    Column {
        Text("Shadow Blur: ${item.shadowRadius.toInt()}", fontSize = 11.sp, color = Color.LightGray)
        Slider(
            value = item.shadowRadius,
            onValueChange = {
                val col = if (item.shadowColorHex == 0) AndroidColor.BLACK else item.shadowColorHex
                onUpdate(item.copy(shadowRadius = it, shadowColorHex = col))
            },
            valueRange = 0f..25f
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Offset X", fontSize = 10.sp, color = Color.LightGray, modifier = Modifier.width(50.dp))
            Slider(
                value = item.shadowDx,
                onValueChange = { onUpdate(item.copy(shadowDx = it)) },
                valueRange = -20f..20f,
                modifier = Modifier.weight(1f)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Offset Y", fontSize = 10.sp, color = Color.LightGray, modifier = Modifier.width(50.dp))
            Slider(
                value = item.shadowDy,
                onValueChange = { onUpdate(item.copy(shadowDy = it)) },
                valueRange = -20f..20f,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TextInnerShadowControl(
    item: PdfEngine.EditItem,
    onUpdate: (PdfEngine.EditItem) -> Unit,
    onHistoryPush: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable Inner Shadow", fontSize = 12.sp, color = Color.White)
            Switch(
                checked = item.innerShadowRadius > 0f,
                onCheckedChange = { checked ->
                    onHistoryPush()
                    onUpdate(item.copy(innerShadowRadius = if (checked) 8f else 0f, innerShadowColorHex = AndroidColor.DKGRAY))
                }
            )
        }
        if (item.innerShadowRadius > 0f) {
            Slider(
                value = item.innerShadowRadius,
                onValueChange = { onUpdate(item.copy(innerShadowRadius = it)) },
                valueRange = 1f..20f
            )
        }
    }
}

@Composable
private fun TextTextureControl(
    item: PdfEngine.EditItem,
    onPickTexture: () -> Unit,
    onClearTexture: () -> Unit
) {
    Column {
        Text("Photo Texture Fill", fontSize = 11.sp, color = Color.LightGray)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onPickTexture,
                colors = ButtonDefaults.buttonColors(containerColor = ToolBlue),
                modifier = Modifier.height(30.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Select Texture Photo", fontSize = 11.sp)
            }
            if (item.textureBitmap != null) {
                OutlinedButton(
                    onClick = onClearTexture,
                    modifier = Modifier.height(30.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Clear", fontSize = 11.sp, color = CrimsonPrimary)
                }
            }
        }
    }
}

@Composable
private fun TextCurveControl(
    item: PdfEngine.EditItem,
    onUpdate: (PdfEngine.EditItem) -> Unit,
    onHistoryPush: () -> Unit
) {
    Column {
        Text("Text Curve / Arc: ${item.curveRadius.toInt()}°", fontSize = 11.sp, color = Color.LightGray)
        Slider(
            value = item.curveRadius,
            onValueChange = { onUpdate(item.copy(curveRadius = it)) },
            valueRange = -90f..90f
        )
    }
}

@Composable
private fun TextMaskControl(
    item: PdfEngine.EditItem,
    onUpdate: (PdfEngine.EditItem) -> Unit,
    onHistoryPush: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Mask / Whiteout Behind Text", fontSize = 12.sp, color = Color.White)
        Switch(
            checked = item.maskRect != null,
            onCheckedChange = { checked ->
                onHistoryPush()
                val rect = if (checked) android.graphics.Rect(0, 0, 100, 40) else null
                onUpdate(item.copy(maskRect = rect))
            }
        )
    }
}

@Composable
private fun TextEmbossControl(
    item: PdfEngine.EditItem,
    onUpdate: (PdfEngine.EditItem) -> Unit,
    onHistoryPush: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Emboss 3D Bevel Effect", fontSize = 12.sp, color = Color.White)
        Switch(
            checked = item.isEmboss,
            onCheckedChange = { checked ->
                onHistoryPush()
                onUpdate(item.copy(isEmboss = checked))
            }
        )
    }
}

@Composable
private fun TextPerspectiveControl(
    item: PdfEngine.EditItem,
    onUpdate: (PdfEngine.EditItem) -> Unit,
    onHistoryPush: () -> Unit
) {
    Column {
        Text("Slant / Perspective Skew: ${item.rotate3dY.toInt()}°", fontSize = 11.sp, color = Color.LightGray)
        Slider(
            value = item.rotate3dY,
            onValueChange = { onUpdate(item.copy(rotate3dY = it)) },
            valueRange = -45f..45f
        )
    }
}

@Composable
private fun Text3dRotateControl(
    item: PdfEngine.EditItem,
    onUpdate: (PdfEngine.EditItem) -> Unit,
    onHistoryPush: () -> Unit
) {
    Column {
        Text("3D X-Axis: ${item.rotate3dX.toInt()}°", fontSize = 11.sp, color = Color.LightGray)
        Slider(
            value = item.rotate3dX,
            onValueChange = { onUpdate(item.copy(rotate3dX = it)) },
            valueRange = -60f..60f
        )
        Text("3D Y-Axis: ${item.rotate3dY.toInt()}°", fontSize = 11.sp, color = Color.LightGray)
        Slider(
            value = item.rotate3dY,
            onValueChange = { onUpdate(item.copy(rotate3dY = it)) },
            valueRange = -60f..60f
        )
    }
}

@Composable
private fun Text3dTextControl(
    item: PdfEngine.EditItem,
    onUpdate: (PdfEngine.EditItem) -> Unit,
    onHistoryPush: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable 3D Text Extrusion", fontSize = 12.sp, color = Color.White)
            Switch(
                checked = item.is3dText,
                onCheckedChange = { checked ->
                    onHistoryPush()
                    onUpdate(item.copy(is3dText = checked, depth3d = if (checked) 8f else 0f))
                }
            )
        }
        if (item.is3dText) {
            Text("Extrusion Depth: ${item.depth3d.toInt()}px", fontSize = 11.sp, color = Color.LightGray)
            Slider(
                value = item.depth3d,
                onValueChange = { onUpdate(item.copy(depth3d = it)) },
                valueRange = 1f..25f
            )
        }
    }
}

@Composable
private fun Text3dShadowControl(
    item: PdfEngine.EditItem,
    onUpdate: (PdfEngine.EditItem) -> Unit,
    onHistoryPush: () -> Unit
) {
    Column {
        Text("3D Floor Shadow Intensity", fontSize = 11.sp, color = Color.LightGray)
        Slider(
            value = item.shadowRadius,
            onValueChange = {
                onUpdate(
                    item.copy(
                        shadowRadius = it,
                        shadowColorHex = AndroidColor.argb(160, 0, 0, 0),
                        shadowDy = it * 0.8f
                    )
                )
            },
            valueRange = 0f..30f
        )
    }
}

@Composable
private fun TextReflectionControl(
    item: PdfEngine.EditItem,
    onUpdate: (PdfEngine.EditItem) -> Unit,
    onHistoryPush: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Mirror Glass Reflection", fontSize = 12.sp, color = Color.White)
        Switch(
            checked = item.isReflection,
            onCheckedChange = { checked ->
                onHistoryPush()
                onUpdate(item.copy(isReflection = checked))
            }
        )
    }
}

/**
 * Tabbed Font Selection Dialog ("English" & "Bengali" tabs) + Custom TTF/OTF Font File picker
 */
@Composable
fun FontSelectionDialog(
    currentFont: String,
    customFontName: String?,
    onDismiss: () -> Unit,
    onSelectFont: (familyName: String, customPath: String?, customName: String?) -> Unit,
    onPickCustomFont: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Typography / Font") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF1E293B),
                    contentColor = ToolBlue
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("English", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("বাংলা / Bengali", fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    // English Preset Fonts
                    val englishPresets = listOf(
                        Pair("Default (Sans Serif)", "DEFAULT"),
                        Pair("Classic Serif", "SERIF"),
                        Pair("Monospace / Code", "MONOSPACE"),
                        Pair("Handwriting / Signature", "HANDWRITING")
                    )

                    englishPresets.forEach { (label, key) ->
                        val isChosen = currentFont.equals(key, ignoreCase = true)
                        Surface(
                            onClick = { onSelectFont(key, null, null) },
                            color = if (isChosen) ToolBlue.copy(alpha = 0.2f) else Color(0xFF0F172A),
                            border = BorderStroke(1.dp, if (isChosen) ToolBlue else Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, fontSize = 13.sp, color = Color.White)
                                if (isChosen) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = ToolBlue, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                } else {
                    // Bengali Preset Fonts
                    val bengaliPresets = listOf(
                        Pair("Noto Sans Bengali (নোটো বাংলা)", "BENGALI"),
                        Pair("Bengali Serif (কালপুরুষ শৈলী)", "BENGALI"),
                        Pair("Bengali Stylized (হাতের লেখা শৈলী)", "BENGALI")
                    )

                    bengaliPresets.forEach { (label, key) ->
                        val isChosen = currentFont.equals(key, ignoreCase = true)
                        Surface(
                            onClick = { onSelectFont(key, null, null) },
                            color = if (isChosen) ToolBlue.copy(alpha = 0.2f) else Color(0xFF0F172A),
                            border = BorderStroke(1.dp, if (isChosen) ToolBlue else Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(label, fontSize = 13.sp, color = Color.White)
                                    if (isChosen) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = ToolBlue, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Text("আমার সোনার বাংলা, আমি তোমায় ভালোবাসি", fontSize = 11.sp, color = Color.LightGray)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Custom Font Section
                OutlinedButton(
                    onClick = onPickCustomFont,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (customFontName != null) "Pick Another Font ($customFontName)" else "Pick Custom Font File (.ttf/.otf)",
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
