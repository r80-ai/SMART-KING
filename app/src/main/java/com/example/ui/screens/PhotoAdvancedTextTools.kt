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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photo.PhotoFonts
import com.example.photo.TextAlignment
import com.example.photo.TextItem
import com.example.ui.theme.CrimsonPrimary
import java.io.File
import kotlin.math.roundToInt

enum class PhotoTextToolActionId {
    EDIT, DELETE, COPY, TO_FRONT, TO_BACK, LAYERS,
    POSITION, REL_POSITION, SIZE, PADDING, COLOR,
    TEXTURE, OPACITY, ROTATE, MASK, FONT,
    STYLE, CURVE, BACKGROUND, ALIGN, SPACING,
    LINE_SPACING, STROKE, SHADOW, INNER_SHADOW, EMBOSS,
    PERSPECTIVE, ROTATE_3D, TEXT_3D, SHADOW_3D, REFLECTION
}

data class PhotoTextToolDef(
    val id: PhotoTextToolActionId,
    val label: String,
    val icon: ImageVector
)

val PHOTO_ADVANCED_TEXT_TOOLS: List<PhotoTextToolDef> = listOf(
    PhotoTextToolDef(PhotoTextToolActionId.EDIT, "Edit", Icons.Default.Edit),
    PhotoTextToolDef(PhotoTextToolActionId.DELETE, "Delete", Icons.Default.Delete),
    PhotoTextToolDef(PhotoTextToolActionId.COPY, "Copy", Icons.Default.ContentCopy),
    PhotoTextToolDef(PhotoTextToolActionId.LAYERS, "Layers", Icons.Default.Layers),
    PhotoTextToolDef(PhotoTextToolActionId.TO_FRONT, "To Front", Icons.Default.FlipToFront),
    PhotoTextToolDef(PhotoTextToolActionId.TO_BACK, "To Back", Icons.Default.FlipToBack),
    PhotoTextToolDef(PhotoTextToolActionId.POSITION, "Position", Icons.Default.OpenWith),
    PhotoTextToolDef(PhotoTextToolActionId.REL_POSITION, "Relative Position", Icons.Default.AspectRatio),
    PhotoTextToolDef(PhotoTextToolActionId.SIZE, "Size", Icons.Default.FormatSize),
    PhotoTextToolDef(PhotoTextToolActionId.PADDING, "Padding", Icons.Default.SpaceDashboard),
    PhotoTextToolDef(PhotoTextToolActionId.COLOR, "Color", Icons.Default.Palette),
    PhotoTextToolDef(PhotoTextToolActionId.TEXTURE, "Texture", Icons.Default.Image),
    PhotoTextToolDef(PhotoTextToolActionId.OPACITY, "Opacity", Icons.Default.Opacity),
    PhotoTextToolDef(PhotoTextToolActionId.ROTATE, "Rotate", Icons.Default.RotateRight),
    PhotoTextToolDef(PhotoTextToolActionId.MASK, "Mask", Icons.Default.Crop),
    PhotoTextToolDef(PhotoTextToolActionId.FONT, "Font", Icons.Default.FontDownload),
    PhotoTextToolDef(PhotoTextToolActionId.STYLE, "Style", Icons.Default.FormatBold),
    PhotoTextToolDef(PhotoTextToolActionId.CURVE, "Curve", Icons.Default.Timeline),
    PhotoTextToolDef(PhotoTextToolActionId.BACKGROUND, "Background", Icons.Default.FormatColorFill),
    PhotoTextToolDef(PhotoTextToolActionId.ALIGN, "Align", Icons.Default.FormatAlignCenter),
    PhotoTextToolDef(PhotoTextToolActionId.SPACING, "Spacing", Icons.Default.SpaceBar),
    PhotoTextToolDef(PhotoTextToolActionId.LINE_SPACING, "Line Spacing", Icons.Default.FormatLineSpacing),
    PhotoTextToolDef(PhotoTextToolActionId.STROKE, "Stroke", Icons.Default.BorderColor),
    PhotoTextToolDef(PhotoTextToolActionId.SHADOW, "Shadow", Icons.Default.WbSunny),
    PhotoTextToolDef(PhotoTextToolActionId.INNER_SHADOW, "Inner Shadow", Icons.Default.InvertColors),
    PhotoTextToolDef(PhotoTextToolActionId.EMBOSS, "Emboss", Icons.Default.AutoFixHigh),
    PhotoTextToolDef(PhotoTextToolActionId.PERSPECTIVE, "Perspective", Icons.Default.Transform),
    PhotoTextToolDef(PhotoTextToolActionId.ROTATE_3D, "3D Rotate", Icons.Default.ScreenRotation),
    PhotoTextToolDef(PhotoTextToolActionId.TEXT_3D, "3D Text", Icons.Default.ViewInAr),
    PhotoTextToolDef(PhotoTextToolActionId.SHADOW_3D, "3D Shadow", Icons.Default.Gradient),
    PhotoTextToolDef(PhotoTextToolActionId.REFLECTION, "Reflection", Icons.Default.Flip)
)

/**
 * PixelLab-Style Advanced Text Toolbar & Control Panel for Photo Editor Markup
 */
@Composable
fun PhotoAdvancedTextToolPanel(
    selectedItem: TextItem?,
    onAddText: () -> Unit,
    onUpdateItem: (TextItem) -> Unit,
    onDeleteItem: (String) -> Unit,
    onCopyItem: (TextItem) -> Unit,
    onBringToFront: (TextItem) -> Unit,
    onSendToBack: (TextItem) -> Unit,
    modifier: Modifier = Modifier,
    onOpenLayers: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var activeActionDialog by remember { mutableStateOf<PhotoTextToolActionId?>(null) }
    var showEditContentDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A))
            .padding(vertical = 4.dp)
    ) {
        // Top Header Row with Add Text and Status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onAddText,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Text", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            if (selectedItem != null) {
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "Layer: ${selectedItem.text.take(14)}...",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            } else {
                Text(
                    text = "Tap text to select & edit",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Horizontally Scrollable 30-Tool Action Menu (PixelLab Style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PHOTO_ADVANCED_TEXT_TOOLS.forEach { tool ->
                val isEnabled = selectedItem != null || tool.id == PhotoTextToolActionId.EDIT || tool.id == PhotoTextToolActionId.LAYERS
                val isSelected = activeActionDialog == tool.id

                Surface(
                    onClick = {
                        if (tool.id == PhotoTextToolActionId.LAYERS) {
                            onOpenLayers?.invoke()
                            return@Surface
                        }
                        if (selectedItem == null && tool.id != PhotoTextToolActionId.EDIT) {
                            Toast.makeText(context, "Please select or add a text layer first", Toast.LENGTH_SHORT).show()
                            return@Surface
                        }
                        when (tool.id) {
                            PhotoTextToolActionId.EDIT -> showEditContentDialog = true
                            PhotoTextToolActionId.DELETE -> onDeleteItem(selectedItem!!.id)
                            PhotoTextToolActionId.COPY -> onCopyItem(selectedItem!!)
                            PhotoTextToolActionId.TO_FRONT -> onBringToFront(selectedItem!!)
                            PhotoTextToolActionId.TO_BACK -> onSendToBack(selectedItem!!)
                            else -> activeActionDialog = if (activeActionDialog == tool.id) null else tool.id
                        }
                    },
                    enabled = isEnabled,
                    color = if (isSelected) CrimsonPrimary else Color(0xFF1E293B),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) CrimsonPrimary else if (isEnabled) Color(0xFF334155) else Color(0xFF1E293B)
                    ),
                    modifier = Modifier.height(48.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tool.icon,
                            contentDescription = tool.label,
                            tint = if (isSelected) Color.White else if (isEnabled) Color(0xFFE2E8F0) else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tool.label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else if (isEnabled) Color(0xFFCBD5E1) else Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }

    // Active tool sub-control panel (collapsible right below toolbar)
    if (selectedItem != null && activeActionDialog != null) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF090D16),
            border = BorderStroke(1.dp, CrimsonPrimary.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = PHOTO_ADVANCED_TEXT_TOOLS.find { it.id == activeActionDialog }?.label ?: "Control",
                        color = CrimsonPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { activeActionDialog = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                when (activeActionDialog) {
                    PhotoTextToolActionId.POSITION -> {
                        PositionPadControl(
                            item = selectedItem,
                            onUpdate = onUpdateItem
                        )
                    }
                    PhotoTextToolActionId.REL_POSITION -> {
                        RelativePositionControl(
                            item = selectedItem,
                            onUpdate = onUpdateItem
                        )
                    }
                    PhotoTextToolActionId.SIZE -> {
                        SizeControl(
                            item = selectedItem,
                            onUpdate = onUpdateItem
                        )
                    }
                    PhotoTextToolActionId.PADDING -> {
                        PaddingControl(
                            item = selectedItem,
                            onUpdate = onUpdateItem
                        )
                    }
                    PhotoTextToolActionId.COLOR -> {
                        ColorOpacityControl(
                            item = selectedItem,
                            onUpdate = onUpdateItem
                        )
                    }
                    PhotoTextToolActionId.TEXTURE -> {
                        TextureFillControl(
                            item = selectedItem,
                            onUpdate = onUpdateItem
                        )
                    }
                    PhotoTextToolActionId.OPACITY -> {
                        OpacityOnlyControl(
                            item = selectedItem,
                            onUpdate = onUpdateItem
                        )
                    }
                    PhotoTextToolActionId.ROTATE -> {
                        RotateControl(
                            item = selectedItem,
                            onUpdate = onUpdateItem
                        )
                    }
                    PhotoTextToolActionId.MASK -> {
                        MaskEffectControl(
                            item = selectedItem,
                            onUpdate = onUpdateItem
                        )
                    }
                    PhotoTextToolActionId.FONT -> {
                        FontFamilyControl(
                            item = selectedItem,
                            onUpdate = onUpdateItem
                        )
                    }
                    PhotoTextToolActionId.STYLE -> {
                        FontStyleControl(
                            item = selectedItem,
                            onUpdate = onUpdateItem
                        )
                    }
                    PhotoTextToolActionId.CURVE -> {
                        CurveControl(
                            item = selectedItem,
                            onUpdate = onUpdateItem
                        )
                    }
                    PhotoTextToolActionId.BACKGROUND -> {
                        BackgroundControl(
                            item = selectedItem,
                            onUpdate = onUpdateItem
                        )
                    }
                    PhotoTextToolActionId.ALIGN -> {
                        AlignControl(
                            item = selectedItem,
                            onUpdate = onUpdateItem
                        )
                    }
                    PhotoTextToolActionId.SPACING -> {
                        LetterSpacingControl(
                            item = selectedItem,
                            onUpdate = onUpdateItem
                        )
                    }
                    PhotoTextToolActionId.LINE_SPACING -> {
                        LineSpacingControl(
                            item = selectedItem,
                            onUpdate = onUpdateItem
                        )
                    }
                    PhotoTextToolActionId.STROKE -> {
                        StrokeControl(
                            item = selectedItem,
                            onUpdate = onUpdateItem
                        )
                    }
                    PhotoTextToolActionId.SHADOW -> {
                        OuterShadowControl(
                            item = selectedItem,
                            onUpdate = onUpdateItem
                        )
                    }
                    PhotoTextToolActionId.INNER_SHADOW -> {
                        InnerShadowControl(
                            item = selectedItem,
                            onUpdate = onUpdateItem
                        )
                    }
                    PhotoTextToolActionId.EMBOSS -> {
                        EmbossControl(
                            item = selectedItem,
                            onUpdate = onUpdateItem
                        )
                    }
                    PhotoTextToolActionId.PERSPECTIVE -> {
                        PerspectiveControl(
                            item = selectedItem,
                            onUpdate = onUpdateItem
                        )
                    }
                    PhotoTextToolActionId.ROTATE_3D -> {
                        Rotate3DControl(
                            item = selectedItem,
                            onUpdate = onUpdateItem
                        )
                    }
                    PhotoTextToolActionId.TEXT_3D -> {
                        Text3DControl(
                            item = selectedItem,
                            onUpdate = onUpdateItem
                        )
                    }
                    PhotoTextToolActionId.SHADOW_3D -> {
                        Shadow3DControl(
                            item = selectedItem,
                            onUpdate = onUpdateItem
                        )
                    }
                    PhotoTextToolActionId.REFLECTION -> {
                        ReflectionControl(
                            item = selectedItem,
                            onUpdate = onUpdateItem
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    // Direct Edit Text Dialog
    if (showEditContentDialog && selectedItem != null) {
        var draft by remember { mutableStateOf(selectedItem.text) }
        AlertDialog(
            onDismissRequest = { showEditContentDialog = false },
            title = { Text("Edit Text Layer", color = CrimsonPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text("Text Content") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CrimsonPrimary,
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateItem(selectedItem.copy(text = draft))
                        showEditContentDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditContentDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

// -------------------------------------------------------------
// SUB-CONTROLS FOR THE 30 TOOLS
// -------------------------------------------------------------

@Composable
private fun PositionPadControl(item: TextItem, onUpdate: (TextItem) -> Unit) {
    var stepSize by remember { mutableFloatStateOf(0.01f) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Nudge Position", color = Color.White, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Step: ${(stepSize * 100).roundToInt()}%", color = Color(0xFF94A3B8), fontSize = 10.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Slider(
                value = stepSize,
                onValueChange = { stepSize = it },
                valueRange = 0.002f..0.05f,
                modifier = Modifier.width(160.dp),
                colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        // D-Pad
        IconButton(
            onClick = { onUpdate(item.copy(normY = (item.normY - stepSize).coerceIn(0f, 1f))) },
            modifier = Modifier.size(36.dp).background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = Color.White)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconButton(
                onClick = { onUpdate(item.copy(normX = (item.normX - stepSize).coerceIn(0f, 1f))) },
                modifier = Modifier.size(36.dp).background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
            ) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Left", tint = Color.White)
            }
            IconButton(
                onClick = { onUpdate(item.copy(normX = 0.5f, normY = 0.5f)) },
                modifier = Modifier.size(36.dp).background(CrimsonPrimary, RoundedCornerShape(6.dp))
            ) {
                Icon(Icons.Default.CenterFocusStrong, contentDescription = "Center", tint = Color.White)
            }
            IconButton(
                onClick = { onUpdate(item.copy(normX = (item.normX + stepSize).coerceIn(0f, 1f))) },
                modifier = Modifier.size(36.dp).background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
            ) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Right", tint = Color.White)
            }
        }
        IconButton(
            onClick = { onUpdate(item.copy(normY = (item.normY + stepSize).coerceIn(0f, 1f))) },
            modifier = Modifier.size(36.dp).background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = Color.White)
        }
    }
}

@Composable
private fun RelativePositionControl(item: TextItem, onUpdate: (TextItem) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Screen-Relative Alignment", color = Color.White, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = { onUpdate(item.copy(normX = 0.1f)) },
                shape = RoundedCornerShape(6.dp)
            ) { Text("Align Left", fontSize = 11.sp, color = Color.White) }
            OutlinedButton(
                onClick = { onUpdate(item.copy(normX = 0.5f)) },
                shape = RoundedCornerShape(6.dp)
            ) { Text("Center H", fontSize = 11.sp, color = Color.White) }
            OutlinedButton(
                onClick = { onUpdate(item.copy(normX = 0.9f)) },
                shape = RoundedCornerShape(6.dp)
            ) { Text("Align Right", fontSize = 11.sp, color = Color.White) }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = { onUpdate(item.copy(normY = 0.1f)) },
                shape = RoundedCornerShape(6.dp)
            ) { Text("Align Top", fontSize = 11.sp, color = Color.White) }
            OutlinedButton(
                onClick = { onUpdate(item.copy(normY = 0.5f)) },
                shape = RoundedCornerShape(6.dp)
            ) { Text("Center V", fontSize = 11.sp, color = Color.White) }
            OutlinedButton(
                onClick = { onUpdate(item.copy(normY = 0.9f)) },
                shape = RoundedCornerShape(6.dp)
            ) { Text("Align Bottom", fontSize = 11.sp, color = Color.White) }
        }
    }
}

@Composable
private fun SizeControl(item: TextItem, onUpdate: (TextItem) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Font Size: ${item.fontSizeSp.roundToInt()} sp", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(110.dp))
            Slider(
                value = item.fontSizeSp,
                onValueChange = { onUpdate(item.copy(fontSizeSp = it)) },
                valueRange = 8f..140f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
            )
        }
    }
}

@Composable
private fun PaddingControl(item: TextItem, onUpdate: (TextItem) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Pad X: ${item.paddingX.roundToInt()} dp", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(100.dp))
            Slider(
                value = item.paddingX,
                onValueChange = { onUpdate(item.copy(paddingX = it)) },
                valueRange = 0f..60f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Pad Y: ${item.paddingY.roundToInt()} dp", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(100.dp))
            Slider(
                value = item.paddingY,
                onValueChange = { onUpdate(item.copy(paddingY = it)) },
                valueRange = 0f..60f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
            )
        }
    }
}

@Composable
private fun ColorOpacityControl(item: TextItem, onUpdate: (TextItem) -> Unit) {
    val presetColors = PRESET_COLORS
    val curColor = item.color
    var red by remember(curColor) { mutableIntStateOf(AndroidColor.red(curColor)) }
    var green by remember(curColor) { mutableIntStateOf(AndroidColor.green(curColor)) }
    var blue by remember(curColor) { mutableIntStateOf(AndroidColor.blue(curColor)) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Presets Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presetColors.forEach { col ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(col))
                        .border(2.dp, if (item.color == col) Color.White else Color.Transparent, CircleShape)
                        .clickable { onUpdate(item.copy(color = col)) }
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        // RGB Sliders
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("R: $red", color = Color(0xFFEF4444), fontSize = 11.sp, modifier = Modifier.width(44.dp))
            Slider(
                value = red.toFloat(),
                onValueChange = {
                    red = it.roundToInt()
                    onUpdate(item.copy(color = AndroidColor.rgb(red, green, blue)))
                },
                valueRange = 0f..255f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = Color(0xFFEF4444), activeTrackColor = Color(0xFFEF4444))
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("G: $green", color = Color(0xFF22C55E), fontSize = 11.sp, modifier = Modifier.width(44.dp))
            Slider(
                value = green.toFloat(),
                onValueChange = {
                    green = it.roundToInt()
                    onUpdate(item.copy(color = AndroidColor.rgb(red, green, blue)))
                },
                valueRange = 0f..255f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = Color(0xFF22C55E), activeTrackColor = Color(0xFF22C55E))
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("B: $blue", color = Color(0xFF3B82F6), fontSize = 11.sp, modifier = Modifier.width(44.dp))
            Slider(
                value = blue.toFloat(),
                onValueChange = {
                    blue = it.roundToInt()
                    onUpdate(item.copy(color = AndroidColor.rgb(red, green, blue)))
                },
                valueRange = 0f..255f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = Color(0xFF3B82F6), activeTrackColor = Color(0xFF3B82F6))
            )
        }
        // Alpha Slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Alpha: ${(item.opacity * 100).roundToInt()}%", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(76.dp))
            Slider(
                value = item.opacity,
                onValueChange = { onUpdate(item.copy(opacity = it)) },
                valueRange = 0.05f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
            )
        }
    }
}

@Composable
private fun TextureFillControl(item: TextItem, onUpdate: (TextItem) -> Unit) {
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        onUpdate(item.copy(textureBitmap = bmp))
                        Toast.makeText(context, "Texture applied!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load texture image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Fill Text With Image Texture", color = Color.White, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { imagePicker.launch("image/*") },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Pick Texture Image", fontSize = 11.sp)
            }
            if (item.textureBitmap != null) {
                OutlinedButton(
                    onClick = { onUpdate(item.copy(textureBitmap = null)) },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Remove Texture", color = Color(0xFFEF4444), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun OpacityOnlyControl(item: TextItem, onUpdate: (TextItem) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("Opacity: ${(item.opacity * 100).roundToInt()}%", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(80.dp))
        Slider(
            value = item.opacity,
            onValueChange = { onUpdate(item.copy(opacity = it)) },
            valueRange = 0.05f..1f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
        )
    }
}

@Composable
private fun RotateControl(item: TextItem, onUpdate: (TextItem) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Angle: ${item.rotationDeg.roundToInt()}°", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(80.dp))
            Slider(
                value = item.rotationDeg,
                onValueChange = { onUpdate(item.copy(rotationDeg = it)) },
                valueRange = -180f..180f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onUpdate(item.copy(rotationDeg = 0f)) }, shape = RoundedCornerShape(6.dp)) {
                Text("Reset 0°", fontSize = 11.sp, color = Color.White)
            }
            OutlinedButton(onClick = { onUpdate(item.copy(rotationDeg = 90f)) }, shape = RoundedCornerShape(6.dp)) {
                Text("90°", fontSize = 11.sp, color = Color.White)
            }
            OutlinedButton(onClick = { onUpdate(item.copy(rotationDeg = 180f)) }, shape = RoundedCornerShape(6.dp)) {
                Text("180°", fontSize = 11.sp, color = Color.White)
            }
            OutlinedButton(onClick = { onUpdate(item.copy(rotationDeg = 270f)) }, shape = RoundedCornerShape(6.dp)) {
                Text("270°", fontSize = 11.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun MaskEffectControl(item: TextItem, onUpdate: (TextItem) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Mask / Opacity Cutout", color = Color.White, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mask Cutout Strength", color = Color(0xFFCBD5E1), fontSize = 11.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = item.opacity < 0.99f,
                onCheckedChange = { checked ->
                    onUpdate(item.copy(opacity = if (checked) 0.5f else 1.0f))
                },
                colors = SwitchDefaults.colors(checkedThumbColor = CrimsonPrimary)
            )
        }
    }
}

@Composable
private fun FontFamilyControl(item: TextItem, onUpdate: (TextItem) -> Unit) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("English") }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val tempFile = File(context.cacheDir, "custom_font_${System.currentTimeMillis()}.ttf")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }
                val customTf = AndroidTypeface.createFromFile(tempFile)
                if (customTf != null) {
                    onUpdate(item.copy(customTypeface = customTf))
                    Toast.makeText(context, "Custom TTF/OTF Font Loaded!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Could not load font file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Tab Row: English vs Bengali vs Custom
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = selectedCategory == "English",
                    onClick = { selectedCategory = "English" },
                    label = { Text("English", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CrimsonPrimary,
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = selectedCategory == "Bengali",
                    onClick = { selectedCategory = "Bengali" },
                    label = { Text("বাংলা (Bengali)", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CrimsonPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
            Button(
                onClick = { filePicker.launch("*/*") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(12.dp), tint = CrimsonPrimary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Import Font", fontSize = 10.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Preset Fonts Horizontal List
        val fontsToShow = PhotoFonts.ALL_FONTS.filter { it.category == selectedCategory }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            fontsToShow.forEach { fontOption ->
                val isFontSelected = item.fontId == fontOption.id && item.customTypeface == null
                Surface(
                    color = if (isFontSelected) CrimsonPrimary.copy(alpha = 0.2f) else Color(0xFF1E293B),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.5.dp, if (isFontSelected) CrimsonPrimary else Color(0xFF334155)),
                    modifier = Modifier.clickable {
                        onUpdate(item.copy(fontId = fontOption.id, customTypeface = null))
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = fontOption.displayName,
                            color = if (isFontSelected) CrimsonPrimary else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (fontOption.category == "Bengali") "বাংলা লেখা" else "Photo Studio",
                            color = if (isFontSelected) Color.White else Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontFamily = fontOption.fontFamily,
                            fontWeight = if (fontOption.defaultBold) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (fontOption.defaultItalic) FontStyle.Italic else FontStyle.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FontStyleControl(item: TextItem, onUpdate: (TextItem) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onUpdate(item.copy(isBold = !item.isBold)) },
            modifier = Modifier.background(if (item.isBold) CrimsonPrimary else Color(0xFF1E293B), RoundedCornerShape(8.dp))
        ) {
            Icon(Icons.Default.FormatBold, contentDescription = "Bold", tint = Color.White)
        }
        IconButton(
            onClick = { onUpdate(item.copy(isItalic = !item.isItalic)) },
            modifier = Modifier.background(if (item.isItalic) CrimsonPrimary else Color(0xFF1E293B), RoundedCornerShape(8.dp))
        ) {
            Icon(Icons.Default.FormatItalic, contentDescription = "Italic", tint = Color.White)
        }
        IconButton(
            onClick = { onUpdate(item.copy(isUnderline = !item.isUnderline)) },
            modifier = Modifier.background(if (item.isUnderline) CrimsonPrimary else Color(0xFF1E293B), RoundedCornerShape(8.dp))
        ) {
            Icon(Icons.Default.FormatUnderlined, contentDescription = "Underline", tint = Color.White)
        }
        IconButton(
            onClick = { onUpdate(item.copy(isStrikethrough = !item.isStrikethrough)) },
            modifier = Modifier.background(if (item.isStrikethrough) CrimsonPrimary else Color(0xFF1E293B), RoundedCornerShape(8.dp))
        ) {
            Icon(Icons.Default.FormatStrikethrough, contentDescription = "Strikethrough", tint = Color.White)
        }
    }
}

@Composable
private fun CurveControl(item: TextItem, onUpdate: (TextItem) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Arc: ${item.curveArc.roundToInt()}°", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(80.dp))
            Slider(
                value = item.curveArc,
                onValueChange = { onUpdate(item.copy(curveArc = it)) },
                valueRange = -100f..100f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
            )
        }
        OutlinedButton(
            onClick = { onUpdate(item.copy(curveArc = 0f)) },
            shape = RoundedCornerShape(6.dp)
        ) {
            Text("Flat 0°", fontSize = 11.sp, color = Color.White)
        }
    }
}

@Composable
private fun BackgroundControl(item: TextItem, onUpdate: (TextItem) -> Unit) {
    val presetBgColors = PRESET_COLORS
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable Background", color = Color.White, fontSize = 12.sp)
            Switch(
                checked = item.backgroundColor != null,
                onCheckedChange = { checked ->
                    onUpdate(item.copy(backgroundColor = if (checked) AndroidColor.argb(200, 0, 0, 0) else null))
                },
                colors = SwitchDefaults.colors(checkedThumbColor = CrimsonPrimary)
            )
        }
        if (item.backgroundColor != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetBgColors.forEach { col ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(col))
                            .border(2.dp, if (item.backgroundColor == col) Color.White else Color.Transparent, CircleShape)
                            .clickable { onUpdate(item.copy(backgroundColor = col)) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Corner: ${item.backgroundCornerRadius.roundToInt()} dp", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(90.dp))
                Slider(
                    value = item.backgroundCornerRadius,
                    onValueChange = { onUpdate(item.copy(backgroundCornerRadius = it)) },
                    valueRange = 0f..32f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Padding: ${item.backgroundPadding.roundToInt()} dp", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(90.dp))
                Slider(
                    value = item.backgroundPadding,
                    onValueChange = { onUpdate(item.copy(backgroundPadding = it)) },
                    valueRange = 0f..40f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
                )
            }
        }
    }
}

@Composable
private fun AlignControl(item: TextItem, onUpdate: (TextItem) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onUpdate(item.copy(alignment = TextAlignment.LEFT)) },
            modifier = Modifier.background(if (item.alignment == TextAlignment.LEFT) CrimsonPrimary else Color(0xFF1E293B), RoundedCornerShape(8.dp))
        ) {
            Icon(Icons.Default.FormatAlignLeft, contentDescription = "Align Left", tint = Color.White)
        }
        IconButton(
            onClick = { onUpdate(item.copy(alignment = TextAlignment.CENTER)) },
            modifier = Modifier.background(if (item.alignment == TextAlignment.CENTER) CrimsonPrimary else Color(0xFF1E293B), RoundedCornerShape(8.dp))
        ) {
            Icon(Icons.Default.FormatAlignCenter, contentDescription = "Align Center", tint = Color.White)
        }
        IconButton(
            onClick = { onUpdate(item.copy(alignment = TextAlignment.RIGHT)) },
            modifier = Modifier.background(if (item.alignment == TextAlignment.RIGHT) CrimsonPrimary else Color(0xFF1E293B), RoundedCornerShape(8.dp))
        ) {
            Icon(Icons.Default.FormatAlignRight, contentDescription = "Align Right", tint = Color.White)
        }
        IconButton(
            onClick = { onUpdate(item.copy(alignment = TextAlignment.JUSTIFY)) },
            modifier = Modifier.background(if (item.alignment == TextAlignment.JUSTIFY) CrimsonPrimary else Color(0xFF1E293B), RoundedCornerShape(8.dp))
        ) {
            Icon(Icons.Default.FormatAlignJustify, contentDescription = "Justify", tint = Color.White)
        }
    }
}

@Composable
private fun LetterSpacingControl(item: TextItem, onUpdate: (TextItem) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Letter Spacing: ${(item.letterSpacing * 10).roundToInt()}", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(130.dp))
            Slider(
                value = item.letterSpacing,
                onValueChange = { onUpdate(item.copy(letterSpacing = it)) },
                valueRange = -0.1f..0.8f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
            )
        }
    }
}

@Composable
private fun LineSpacingControl(item: TextItem, onUpdate: (TextItem) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Line Spacing: ${(item.lineSpacingMultiplier * 10).roundToInt() / 10f}x", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(130.dp))
            Slider(
                value = item.lineSpacingMultiplier,
                onValueChange = { onUpdate(item.copy(lineSpacingMultiplier = it)) },
                valueRange = 0.6f..2.5f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
            )
        }
    }
}

@Composable
private fun StrokeControl(item: TextItem, onUpdate: (TextItem) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable Stroke", color = Color.White, fontSize = 12.sp)
            Switch(
                checked = item.strokeColor != null && item.strokeWidth > 0f,
                onCheckedChange = { checked ->
                    onUpdate(
                        item.copy(
                            strokeColor = if (checked) AndroidColor.BLACK else null,
                            strokeWidth = if (checked) 3f else 0f
                        )
                    )
                },
                colors = SwitchDefaults.colors(checkedThumbColor = CrimsonPrimary)
            )
        }
        if (item.strokeColor != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Width: ${item.strokeWidth.roundToInt()}", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(70.dp))
                Slider(
                    value = item.strokeWidth,
                    onValueChange = { onUpdate(item.copy(strokeWidth = it)) },
                    valueRange = 1f..25f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Stroke Color", color = Color(0xFF94A3B8), fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PRESET_COLORS.forEach { col ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(col))
                            .border(
                                width = if (item.strokeColor == col) 2.5.dp else 1.dp,
                                color = if (item.strokeColor == col) CrimsonPrimary else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable { onUpdate(item.copy(strokeColor = col)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OuterShadowControl(item: TextItem, onUpdate: (TextItem) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable Shadow", color = Color.White, fontSize = 12.sp)
            Switch(
                checked = item.shadowColor != null && item.shadowRadius > 0f,
                onCheckedChange = { checked ->
                    onUpdate(
                        item.copy(
                            shadowColor = if (checked) AndroidColor.argb(180, 0, 0, 0) else null,
                            shadowRadius = if (checked) 6f else 0f,
                            shadowDx = if (checked) 3f else 0f,
                            shadowDy = if (checked) 3f else 0f
                        )
                    )
                },
                colors = SwitchDefaults.colors(checkedThumbColor = CrimsonPrimary)
            )
        }
        if (item.shadowColor != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Radius: ${item.shadowRadius.roundToInt()}", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(70.dp))
                Slider(
                    value = item.shadowRadius,
                    onValueChange = { onUpdate(item.copy(shadowRadius = it)) },
                    valueRange = 1f..30f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Offset X: ${item.shadowDx.roundToInt()}", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(70.dp))
                Slider(
                    value = item.shadowDx,
                    onValueChange = { onUpdate(item.copy(shadowDx = it)) },
                    valueRange = -30f..30f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Offset Y: ${item.shadowDy.roundToInt()}", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(70.dp))
                Slider(
                    value = item.shadowDy,
                    onValueChange = { onUpdate(item.copy(shadowDy = it)) },
                    valueRange = -30f..30f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
                )
            }
        }
    }
}

@Composable
private fun InnerShadowControl(item: TextItem, onUpdate: (TextItem) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable Inner Shadow", color = Color.White, fontSize = 12.sp)
            Switch(
                checked = item.innerShadowColor != null,
                onCheckedChange = { checked ->
                    onUpdate(
                        item.copy(
                            innerShadowColor = if (checked) AndroidColor.BLACK else null,
                            innerShadowRadius = if (checked) 5f else 0f
                        )
                    )
                },
                colors = SwitchDefaults.colors(checkedThumbColor = CrimsonPrimary)
            )
        }
    }
}

@Composable
private fun EmbossControl(item: TextItem, onUpdate: (TextItem) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Emboss Light Bevel Effect", color = Color.White, fontSize = 12.sp)
        Switch(
            checked = item.isEmboss,
            onCheckedChange = { checked -> onUpdate(item.copy(isEmboss = checked)) },
            colors = SwitchDefaults.colors(checkedThumbColor = CrimsonPrimary)
        )
    }
}

@Composable
private fun PerspectiveControl(item: TextItem, onUpdate: (TextItem) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Slant / Perspective: ${item.perspectiveSlant.roundToInt()}", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(140.dp))
            Slider(
                value = item.perspectiveSlant,
                onValueChange = { onUpdate(item.copy(perspectiveSlant = it)) },
                valueRange = -35f..35f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
            )
        }
        OutlinedButton(onClick = { onUpdate(item.copy(perspectiveSlant = 0f)) }, shape = RoundedCornerShape(6.dp)) {
            Text("Reset Slant", fontSize = 11.sp, color = Color.White)
        }
    }
}

@Composable
private fun Rotate3DControl(item: TextItem, onUpdate: (TextItem) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Rotate X: ${item.rotationX.roundToInt()}°", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(90.dp))
            Slider(
                value = item.rotationX,
                onValueChange = { onUpdate(item.copy(rotationX = it)) },
                valueRange = -60f..60f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Rotate Y: ${item.rotationY.roundToInt()}°", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(90.dp))
            Slider(
                value = item.rotationY,
                onValueChange = { onUpdate(item.copy(rotationY = it)) },
                valueRange = -60f..60f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
            )
        }
        OutlinedButton(onClick = { onUpdate(item.copy(rotationX = 0f, rotationY = 0f)) }, shape = RoundedCornerShape(6.dp)) {
            Text("Reset 3D", fontSize = 11.sp, color = Color.White)
        }
    }
}

@Composable
private fun Text3DControl(item: TextItem, onUpdate: (TextItem) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("3D Text Extrusion", color = Color.White, fontSize = 12.sp)
            Switch(
                checked = item.depth3D > 0f,
                onCheckedChange = { checked ->
                    onUpdate(item.copy(depth3D = if (checked) 8f else 0f))
                },
                colors = SwitchDefaults.colors(checkedThumbColor = CrimsonPrimary)
            )
        }
        if (item.depth3D > 0f) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Depth: ${item.depth3D.roundToInt()}", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(70.dp))
                Slider(
                    value = item.depth3D,
                    onValueChange = { onUpdate(item.copy(depth3D = it)) },
                    valueRange = 1f..25f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = CrimsonPrimary, activeTrackColor = CrimsonPrimary)
                )
            }
        }
    }
}

@Composable
private fun Shadow3DControl(item: TextItem, onUpdate: (TextItem) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("3D Base Shadow", color = Color.White, fontSize = 12.sp)
        Switch(
            checked = item.shadow3D,
            onCheckedChange = { checked -> onUpdate(item.copy(shadow3D = checked)) },
            colors = SwitchDefaults.colors(checkedThumbColor = CrimsonPrimary)
        )
    }
}

@Composable
private fun ReflectionControl(item: TextItem, onUpdate: (TextItem) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Text Mirror Reflection", color = Color.White, fontSize = 12.sp)
        Switch(
            checked = item.hasReflection,
            onCheckedChange = { checked -> onUpdate(item.copy(hasReflection = checked)) },
            colors = SwitchDefaults.colors(checkedThumbColor = CrimsonPrimary)
        )
    }
}
