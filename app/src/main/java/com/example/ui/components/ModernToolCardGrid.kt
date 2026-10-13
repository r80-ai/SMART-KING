package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.ToolDefinition

// Palette constants matching reference specs
val ModernScreenBackground = Color(0xFFF8FAFC)
val ModernCardBackground = Color(0xFFFFFFFF)
val ModernCardBorder = Color(0xFFE2E8F0)
val ModernTitleColor = Color(0xFF1E293B)
val ModernSubtitleColor = Color(0xFF64748B)

/**
 * Responsive Multi-column Grid for Tools:
 * - 5 columns on desktop / tablet web (>= 900.dp)
 * - 4 columns on large tablets (>= 680.dp)
 * - 3 columns on small tablets / foldables / landscape (>= 480.dp)
 * - 2 columns on mobile (< 480.dp)
 */
@Composable
fun ModernToolCardGrid(
    tools: List<ToolDefinition>,
    isBn: Boolean,
    modifier: Modifier = Modifier,
    pinnedToolIds: Set<String> = emptySet(),
    onPinToggle: ((String) -> Unit)? = null,
    onToolClick: (ToolDefinition) -> Unit,
    headerContent: (@Composable () -> Unit)? = null,
    footerContent: (@Composable () -> Unit)? = null
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(ModernScreenBackground)
    ) {
        val screenWidth = maxWidth
        val columns = when {
            screenWidth >= 900.dp -> 5
            screenWidth >= 680.dp -> 4
            screenWidth >= 480.dp -> 3
            else -> 2
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (headerContent != null) {
                item(span = { GridItemSpan(columns) }) {
                    headerContent()
                }
            }

            items(tools, key = { it.id }) { tool ->
                val isPinned = pinnedToolIds.contains(tool.id)
                ModernToolCard(
                    tool = tool,
                    isBn = isBn,
                    isFavorite = isPinned,
                    onFavoriteToggle = if (onPinToggle != null) {
                        { onPinToggle(tool.id) }
                    } else null,
                    onClick = { onToolClick(tool) }
                )
            }

            if (footerContent != null) {
                item(span = { GridItemSpan(columns) }) {
                    footerContent()
                }
            }
        }
    }
}

/**
 * Modern Tool Card Component:
 * - White rounded card (border-radius: 16px) with soft shadow
 * - Top-left bookmark heart outline icon
 * - Top-right badge ("NEW", "HOT", "FREE", "PRO")
 * - Glossy 3D realistic icon card in center
 * - Title: Bold, centered, 13sp (#1E293B)
 * - Subtitle: Muted gray, centered, 11sp (#64748B), max 2 lines
 */
@Composable
fun ModernToolCard(
    tool: ToolDefinition,
    isBn: Boolean,
    isFavorite: Boolean = false,
    onFavoriteToggle: (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0x0F000000),
                ambientColor = Color(0x08000000)
            )
            .testTag("tool_card_${tool.id}")
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ModernCardBackground),
        border = BorderStroke(1.dp, ModernCardBorder.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Row: Heart Bookmark (Top-Left) and Badge (Top-Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Heart bookmark icon
                if (onFavoriteToggle != null) {
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier
                            .size(26.dp)
                            .testTag("bookmark_${tool.id}")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Bookmark",
                            tint = if (isFavorite) Color(0xFFEF4444) else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Bookmark",
                        tint = if (isFavorite) Color(0xFFEF4444) else Color(0xFFCBD5E1),
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(15.dp)
                    )
                }

                // Badge Tag ("NEW", "HOT", "FREE", "PRO")
                val badgeText = determineBadge(tool)
                if (badgeText.isNotBlank()) {
                    ModernBadge(badge = badgeText)
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Center: High-Resolution Glossy 3D Realistic Icon Card
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                GlossyRealisticToolIcon(
                    toolId = tool.id,
                    title = tool.title,
                    modifier = Modifier.size(62.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title: Bold, centered, 13sp (#1E293B)
            Text(
                text = tool.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp
                ),
                color = ModernTitleColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Subtitle: Muted gray, centered, 11sp (#64748B), max 2 lines
            val subtitle = if (isBn) {
                tool.subtitleBn.ifBlank { tool.subtitleEn }
            } else {
                tool.subtitleEn.ifBlank { tool.subtitleBn }
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 14.sp
                ),
                color = ModernSubtitleColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 28.dp)
            )
        }
    }
}

/**
 * Top-Right corner badge styling matching specs:
 * - "HOT" in red
 * - "NEW" in green
 * - "FREE" in cyan
 * - "PRO" in purple
 */
@Composable
fun ModernBadge(badge: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (badge.uppercase()) {
        "HOT" -> Color(0xFFFEE2E2) to Color(0xFFEF4444)
        "NEW" -> Color(0xFFDCFCE7) to Color(0xFF16A34A)
        "FREE" -> Color(0xFFE0F2FE) to Color(0xFF0284C7)
        "PRO" -> Color(0xFFF3E8FF) to Color(0xFF7C3AED)
        else -> Color(0xFFF1F5F9) to Color(0xFF475569)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = badge.uppercase(),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            letterSpacing = 0.4.sp
        )
    }
}

private fun determineBadge(tool: ToolDefinition): String {
    if (tool.extraTag.isNotBlank()) return tool.extraTag
    return when (tool.id) {
        "doc_enhancer", "merge", "compress", "aadhaar_cropper", "passport_photo" -> "HOT"
        "name_date_photo", "pvc_smart_card", "split_camera", "voter_card", "add_watermark" -> "NEW"
        "pan_card", "split", "jpg_to_pdf", "pdf_to_jpg", "compress_photo", "rotate_pdf", "crop_pdf", "remove_pages", "extract_pages" -> "FREE"
        "lock_remover", "signature_validation", "edit_pdf", "photo_editing" -> "PRO"
        else -> ""
    }
}

/**
 * Glossy 3D Realistic Icon Renderer:
 * Uses Canvas and custom layered graphics to render high-resolution, glossy 3D realistic cards:
 * - Aadhaar AI Cropper: Biometric fingerprint / Aadhaar emblem badge with gold & navy rings
 * - PAN Card AI Cropper: Income Tax PAN card realistic preview with hologram & blue header
 * - Voter Card AI Cropper: Tricolor voter ID badge with tricolor gradient & chakra
 * - Epson L8050 / PVC Maker: Photo printer and PVC card mockup with chip
 * - PDF Tools: Glossy red 3D PDF badges (Merge, Compress, Split, Lock Remover)
 * - Photo Tools: Camera lens, upscale face portrait, signature stamp, doc scanner
 */
@Composable
fun GlossyRealisticToolIcon(
    toolId: String,
    title: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (toolId) {
            "aadhaar_cropper", "pvc_aadhaar" -> drawAadhaarIcon(w, h)
            "pan_card" -> drawPanCardIcon(w, h)
            "voter_card" -> drawVoterCardIcon(w, h)
            "pvc_smart_card", "epson_l8050" -> drawPvcPrinterIcon(w, h)
            "merge" -> drawGlossyPdfIcon(w, h, subType = PdfSubType.MERGE)
            "split" -> drawGlossyPdfIcon(w, h, subType = PdfSubType.SPLIT)
            "compress" -> drawGlossyPdfIcon(w, h, subType = PdfSubType.COMPRESS)
            "lock_remover" -> drawGlossyPdfIcon(w, h, subType = PdfSubType.LOCK_REMOVER)
            "pdf_to_jpg" -> drawGlossyPdfIcon(w, h, subType = PdfSubType.PDF_TO_JPG)
            "jpg_to_pdf" -> drawGlossyPdfIcon(w, h, subType = PdfSubType.JPG_TO_PDF)
            "edit_pdf" -> drawGlossyPdfIcon(w, h, subType = PdfSubType.EDIT)
            "crop_pdf" -> drawGlossyPdfIcon(w, h, subType = PdfSubType.CROP)
            "rotate_pdf" -> drawGlossyPdfIcon(w, h, subType = PdfSubType.ROTATE)
            "remove_pages" -> drawGlossyPdfIcon(w, h, subType = PdfSubType.REMOVE)
            "extract_pages" -> drawGlossyPdfIcon(w, h, subType = PdfSubType.EXTRACT)
            "add_watermark" -> drawGlossyPdfIcon(w, h, subType = PdfSubType.WATERMARK)
            "add_page_nums" -> drawGlossyPdfIcon(w, h, subType = PdfSubType.PAGE_NUMS)
            "signature_validation" -> drawSignatureValidIcon(w, h)
            "doc_enhancer" -> drawDocScannerIcon(w, h)
            "split_camera" -> drawCameraLensIcon(w, h)
            "photo_editing" -> drawUpscalePortraitIcon(w, h)
            "name_date_photo" -> drawNameDatePhotoIcon(w, h)
            "passport_photo" -> drawPassportStudioIcon(w, h)
            "compress_photo" -> drawCompressPhotoIcon(w, h)
            "edge_detect" -> drawEdgePerspectiveIcon(w, h)
            else -> {
                // Fallback realistic glossy card with icon badge
                drawGlossyPdfIcon(w, h, subType = PdfSubType.GENERIC)
            }
        }
    }
}

private enum class PdfSubType {
    MERGE, SPLIT, COMPRESS, LOCK_REMOVER, PDF_TO_JPG, JPG_TO_PDF,
    EDIT, CROP, ROTATE, REMOVE, EXTRACT, WATERMARK, PAGE_NUMS, GENERIC
}

/**
 * 1. Aadhaar AI Cropper: Biometric fingerprint + Aadhaar sunburst emblem
 */
private fun DrawScope.drawAadhaarIcon(w: Float, h: Float) {
    // 3D Outer base squircle with metallic navy gradient
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF1E3A8A), Color(0xFF0F172A))
        ),
        size = Size(w, h),
        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
    )

    // Inner glowing ring
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.35f), Color.Transparent),
            center = Offset(w * 0.5f, h * 0.5f),
            radius = w * 0.45f
        ),
        radius = w * 0.45f,
        center = Offset(w * 0.5f, h * 0.5f)
    )

    // Indian flag micro ribbon on top
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color(0xFFFF9933), Color(0xFFFFFFFF), Color(0xFF138808))
        ),
        topLeft = Offset(w * 0.22f, h * 0.12f),
        size = Size(w * 0.56f, 3.5.dp.toPx()),
        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )

    // Aadhaar golden rays / sunburst circle
    drawCircle(
        color = Color(0xFFF59E0B),
        radius = w * 0.16f,
        center = Offset(w * 0.5f, h * 0.38f),
        style = Stroke(width = 2.dp.toPx())
    )
    drawCircle(
        color = Color(0xFFEF4444),
        radius = w * 0.08f,
        center = Offset(w * 0.5f, h * 0.38f)
    )

    // Biometric fingerprint arcs below
    val cyanGlow = Color(0xFF38BDF8)
    for (r in listOf(w * 0.12f, w * 0.18f, w * 0.24f, w * 0.30f)) {
        drawArc(
            color = cyanGlow,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.5f - r, h * 0.72f - r),
            size = Size(r * 2, r * 2),
            style = Stroke(width = 2.dp.toPx())
        )
    }

    // Top Glossy Specular Highlight Sweep
    drawGlossOverlay(w, h)
}

/**
 * 2. PAN Card AI Cropper: Realistic miniature Income Tax PAN card
 */
private fun DrawScope.drawPanCardIcon(w: Float, h: Float) {
    // Card Drop Shadow Base
    drawRoundRect(
        color = Color(0xFFCBD5E1),
        topLeft = Offset(w * 0.06f, h * 0.14f),
        size = Size(w * 0.88f, h * 0.74f),
        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
    )

    // Card Body with realistic soft pale blue gradient
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFF0F9FF), Color(0xFFE0F2FE), Color(0xFFBAE6FD))
        ),
        topLeft = Offset(w * 0.08f, h * 0.12f),
        size = Size(w * 0.84f, h * 0.72f),
        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
    )

    // Top Navy Header Bar (Income Tax Dept)
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color(0xFF1E3A8A), Color(0xFF0284C7))
        ),
        topLeft = Offset(w * 0.08f, h * 0.12f),
        size = Size(w * 0.84f, h * 0.18f),
        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
    )

    // Golden Hologram Chip
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFFDE047), Color(0xFFEAB308), Color(0xFFCA8A04))
        ),
        topLeft = Offset(w * 0.16f, h * 0.35f),
        size = Size(w * 0.18f, h * 0.15f),
        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
    )

    // Photo Box on left
    drawRoundRect(
        color = Color(0xFF94A3B8),
        topLeft = Offset(w * 0.16f, h * 0.54f),
        size = Size(w * 0.22f, h * 0.24f),
        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
    )
    // Head silhouette in photo
    drawCircle(
        color = Color(0xFF475569),
        radius = w * 0.05f,
        center = Offset(w * 0.27f, h * 0.62f)
    )

    // Realistic micro text lines (Name, Father, DOB, PAN)
    val textLineColor = Color(0xFF334155)
    drawLine(textLineColor, Offset(w * 0.44f, h * 0.40f), Offset(w * 0.82f, h * 0.40f), strokeWidth = 2.dp.toPx())
    drawLine(textLineColor, Offset(w * 0.44f, h * 0.50f), Offset(w * 0.75f, h * 0.50f), strokeWidth = 2.dp.toPx())
    drawLine(textLineColor, Offset(w * 0.44f, h * 0.60f), Offset(w * 0.85f, h * 0.60f), strokeWidth = 2.dp.toPx())

    // Signature box with pen stroke
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(w * 0.44f, h * 0.68f),
        size = Size(w * 0.40f, h * 0.12f),
        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )
    drawLine(Color(0xFF2563EB), Offset(w * 0.48f, h * 0.74f), Offset(w * 0.78f, h * 0.74f), strokeWidth = 1.5.dp.toPx())

    drawGlossOverlay(w, h)
}

/**
 * 3. Voter Card AI Cropper: Tricolor voter ID badge
 */
private fun DrawScope.drawVoterCardIcon(w: Float, h: Float) {
    // Card Base with Tricolor gradient
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFF9933), // Saffron
                Color(0xFFFFFFFF), // White
                Color(0xFF138808)  // Green
            )
        ),
        topLeft = Offset(w * 0.08f, h * 0.10f),
        size = Size(w * 0.84f, h * 0.78f),
        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
        style = Fill
    )

    // Border
    drawRoundRect(
        color = Color(0xFFCBD5E1),
        topLeft = Offset(w * 0.08f, h * 0.10f),
        size = Size(w * 0.84f, h * 0.78f),
        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
        style = Stroke(width = 1.5.dp.toPx())
    )

    // Blue Ashok Chakra in center
    drawCircle(
        color = Color(0xFF000080),
        radius = w * 0.12f,
        center = Offset(w * 0.62f, h * 0.48f),
        style = Stroke(width = 1.5.dp.toPx())
    )

    // Photo Box on left
    drawRoundRect(
        color = Color(0xFFE2E8F0),
        topLeft = Offset(w * 0.16f, h * 0.28f),
        size = Size(w * 0.26f, h * 0.42f),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )
    drawCircle(
        color = Color(0xFF64748B),
        radius = w * 0.06f,
        center = Offset(w * 0.29f, h * 0.42f)
    )

    // EPIC Number header badge
    drawRoundRect(
        color = Color(0xFF000080),
        topLeft = Offset(w * 0.16f, h * 0.16f),
        size = Size(w * 0.68f, h * 0.08f),
        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )

    drawGlossOverlay(w, h)
}

/**
 * 4. Epson L8050 / PVC Maker: Photo printer and PVC card mockup
 */
private fun DrawScope.drawPvcPrinterIcon(w: Float, h: Float) {
    // Printer Base Chassis in Dark Graphite
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF334155), Color(0xFF0F172A))
        ),
        topLeft = Offset(w * 0.10f, h * 0.20f),
        size = Size(w * 0.80f, h * 0.45f),
        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
    )

    // Top Scanner Glass Lid / Accent
    drawRoundRect(
        color = Color(0xFF2563EB),
        topLeft = Offset(w * 0.14f, h * 0.24f),
        size = Size(w * 0.72f, h * 0.08f),
        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
    )

    // Printer Tray Output Slot
    drawRoundRect(
        color = Color(0xFF020617),
        topLeft = Offset(w * 0.20f, h * 0.48f),
        size = Size(w * 0.60f, h * 0.10f),
        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )

    // Glossy CR80 PVC Card emerging from printer tray!
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0), Color(0xFFCBD5E1))
        ),
        topLeft = Offset(w * 0.22f, h * 0.44f),
        size = Size(w * 0.56f, h * 0.42f),
        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
    )

    // Smart chip on PVC card
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFFDE047), Color(0xFFEAB308))
        ),
        topLeft = Offset(w * 0.28f, h * 0.54f),
        size = Size(w * 0.12f, h * 0.10f),
        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )

    // Power / Status LED light on printer
    drawCircle(
        color = Color(0xFF22C55E),
        radius = 2.5.dp.toPx(),
        center = Offset(w * 0.78f, h * 0.38f)
    )

    drawGlossOverlay(w, h)
}

/**
 * 5. Glossy Red 3D PDF Badges (Merge, Compress, Split, Lock Remover, etc.)
 */
private fun DrawScope.drawGlossyPdfIcon(w: Float, h: Float, subType: PdfSubType) {
    // Outer 3D Document Body in Glossy Crimson Red
    val redGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFEF4444), Color(0xFFDC2626), Color(0xFF991B1B))
    )

    // Folded document path
    val docPath = Path().apply {
        moveTo(w * 0.18f, h * 0.12f)
        lineTo(w * 0.64f, h * 0.12f)
        lineTo(w * 0.82f, h * 0.30f)
        lineTo(w * 0.82f, h * 0.88f)
        lineTo(w * 0.18f, h * 0.88f)
        close()
    }
    drawPath(docPath, redGradient)

    // Folded top-right triangle corner
    val foldPath = Path().apply {
        moveTo(w * 0.64f, h * 0.12f)
        lineTo(w * 0.64f, h * 0.30f)
        lineTo(w * 0.82f, h * 0.30f)
        close()
    }
    drawPath(
        foldPath,
        Brush.linearGradient(listOf(Color(0xFFFCA5A5), Color(0xFFB91C1C)))
    )

    // White bold "PDF" label badge banner across top
    drawRoundRect(
        color = Color.White.copy(alpha = 0.92f),
        topLeft = Offset(w * 0.22f, h * 0.36f),
        size = Size(w * 0.56f, h * 0.15f),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )

    // Sub-Action Graphic Emblem based on PDF tool type
    when (subType) {
        PdfSubType.MERGE -> {
            // Two interlocking merge arrows
            drawCircle(Color.White, radius = w * 0.14f, center = Offset(w * 0.5f, h * 0.68f), style = Stroke(width = 3.dp.toPx()))
            drawLine(Color(0xFFFDE047), Offset(w * 0.40f, h * 0.68f), Offset(w * 0.60f, h * 0.68f), strokeWidth = 3.dp.toPx())
        }
        PdfSubType.SPLIT -> {
            // Glowing split line with scissors effect
            drawLine(Color(0xFFFDE047), Offset(w * 0.32f, h * 0.68f), Offset(w * 0.68f, h * 0.68f), strokeWidth = 2.5.dp.toPx())
            drawCircle(Color.White, radius = 3.dp.toPx(), center = Offset(w * 0.36f, h * 0.62f))
            drawCircle(Color.White, radius = 3.dp.toPx(), center = Offset(w * 0.64f, h * 0.74f))
        }
        PdfSubType.COMPRESS -> {
            // Inward compression arrows
            drawLine(Color(0xFF34D399), Offset(w * 0.30f, h * 0.68f), Offset(w * 0.46f, h * 0.68f), strokeWidth = 3.dp.toPx())
            drawLine(Color(0xFF34D399), Offset(w * 0.70f, h * 0.68f), Offset(w * 0.54f, h * 0.68f), strokeWidth = 3.dp.toPx())
            drawCircle(Color.White, radius = 4.dp.toPx(), center = Offset(w * 0.5f, h * 0.68f))
        }
        PdfSubType.LOCK_REMOVER -> {
            // Golden 3D Padlock (open)
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFFFDE047), Color(0xFFCA8A04))),
                topLeft = Offset(w * 0.38f, h * 0.62f),
                size = Size(w * 0.24f, h * 0.22f),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
            // Open shackle
            drawArc(
                color = Color.White,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.42f, h * 0.52f),
                size = Size(w * 0.16f, h * 0.16f),
                style = Stroke(width = 2.5.dp.toPx())
            )
        }
        PdfSubType.PDF_TO_JPG -> {
            // Cyan photo print badge alongside
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7))),
                topLeft = Offset(w * 0.46f, h * 0.56f),
                size = Size(w * 0.38f, h * 0.30f),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            drawCircle(Color.White, radius = 3.dp.toPx(), center = Offset(w * 0.56f, h * 0.64f))
        }
        PdfSubType.JPG_TO_PDF -> {
            // Camera / Image to PDF icon
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFFFB923C), Color(0xFFEA580C))),
                topLeft = Offset(w * 0.28f, h * 0.56f),
                size = Size(w * 0.44f, h * 0.26f),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            drawCircle(Color.White, radius = 4.dp.toPx(), center = Offset(w * 0.5f, h * 0.69f))
        }
        else -> {
            // Generic 3D stars/lines
            drawLine(Color.White.copy(alpha = 0.8f), Offset(w * 0.30f, h * 0.62f), Offset(w * 0.70f, h * 0.62f), strokeWidth = 2.dp.toPx())
            drawLine(Color.White.copy(alpha = 0.8f), Offset(w * 0.30f, h * 0.72f), Offset(w * 0.60f, h * 0.72f), strokeWidth = 2.dp.toPx())
        }
    }

    drawGlossOverlay(w, h)
}

/**
 * 6. Photo Tools: Doc Scanner flatbed with laser sweep
 */
private fun DrawScope.drawDocScannerIcon(w: Float, h: Float) {
    // Dark scanner frame
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF334155), Color(0xFF0F172A))),
        topLeft = Offset(w * 0.10f, h * 0.12f),
        size = Size(w * 0.80f, h * 0.76f),
        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
    )

    // Flatbed glass
    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(w * 0.16f, h * 0.18f),
        size = Size(w * 0.68f, h * 0.64f),
        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
    )

    // Paper document inside
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(w * 0.22f, h * 0.24f),
        size = Size(w * 0.56f, h * 0.52f),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )

    // Cyan glowing laser scanning beam!
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0x0000D2FF),
                Color(0xFF00E5FF),
                Color(0x0000D2FF)
            )
        ),
        topLeft = Offset(w * 0.16f, h * 0.46f),
        size = Size(w * 0.68f, h * 0.10f)
    )
    drawLine(
        color = Color.White,
        start = Offset(w * 0.16f, h * 0.51f),
        end = Offset(w * 0.84f, h * 0.51f),
        strokeWidth = 2.dp.toPx()
    )

    drawGlossOverlay(w, h)
}

/**
 * 7. Photo Tools: Camera Lens realistic metallic ring & aperture
 */
private fun DrawScope.drawCameraLensIcon(w: Float, h: Float) {
    // Outer metallic lens barrel
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF475569), Color(0xFF1E293B), Color(0xFF0F172A))
        ),
        radius = w * 0.42f,
        center = Offset(w * 0.5f, h * 0.5f)
    )

    // Ribbed focus grip ring
    drawCircle(
        color = Color(0xFF334155),
        radius = w * 0.36f,
        center = Offset(w * 0.5f, h * 0.5f),
        style = Stroke(width = 3.dp.toPx())
    )

    // Deep glass optic reflection
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF0284C7), Color(0xFF1E1B4B), Color(0xFF020617)),
            center = Offset(w * 0.45f, h * 0.45f)
        ),
        radius = w * 0.30f,
        center = Offset(w * 0.5f, h * 0.5f)
    )

    // Aperture cyan light flare
    drawCircle(
        color = Color(0xFF38BDF8).copy(alpha = 0.65f),
        radius = w * 0.12f,
        center = Offset(w * 0.58f, h * 0.42f)
    )

    drawGlossOverlay(w, h)
}

/**
 * 8. Photo Tools: Upscale portrait with golden sparkle magic wand
 */
private fun DrawScope.drawUpscalePortraitIcon(w: Float, h: Float) {
    // Gradient card base
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9), Color(0xFF4C1D95))
        ),
        topLeft = Offset(w * 0.10f, h * 0.12f),
        size = Size(w * 0.80f, h * 0.76f),
        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
    )

    // Face silhouette portrait circle
    drawCircle(
        color = Color.White.copy(alpha = 0.9f),
        radius = w * 0.18f,
        center = Offset(w * 0.5f, h * 0.44f)
    )

    // Golden 4-point AI magic sparkles!
    val sparkleColor = Color(0xFFFDE047)
    drawCircle(sparkleColor, radius = 3.dp.toPx(), center = Offset(w * 0.75f, h * 0.25f))
    drawCircle(sparkleColor, radius = 4.dp.toPx(), center = Offset(w * 0.25f, h * 0.65f))
    drawCircle(Color.White, radius = 2.dp.toPx(), center = Offset(w * 0.72f, h * 0.68f))

    drawGlossOverlay(w, h)
}

/**
 * 9. Name & Date on Photo Icon: Candidate portrait with name banner
 */
private fun DrawScope.drawNameDatePhotoIcon(w: Float, h: Float) {
    // White photo card with thin black border
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(w * 0.14f, h * 0.10f),
        size = Size(w * 0.72f, h * 0.80f),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )
    drawRoundRect(
        color = Color.Black,
        topLeft = Offset(w * 0.14f, h * 0.10f),
        size = Size(w * 0.72f, h * 0.80f),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        style = Stroke(width = 2.dp.toPx())
    )

    // Candidate head silhouette
    drawCircle(Color(0xFF3B82F6), radius = w * 0.14f, center = Offset(w * 0.5f, h * 0.38f))

    // Name & DOP/DOB banner box at bottom
    drawRect(
        color = Color.White,
        topLeft = Offset(w * 0.16f, h * 0.65f),
        size = Size(w * 0.68f, h * 0.20f)
    )
    drawLine(Color.Black, Offset(w * 0.14f, h * 0.65f), Offset(w * 0.86f, h * 0.65f), strokeWidth = 1.5.dp.toPx())
    drawLine(Color.Black, Offset(w * 0.22f, h * 0.72f), Offset(w * 0.78f, h * 0.72f), strokeWidth = 2.dp.toPx())
    drawLine(Color.Black, Offset(w * 0.28f, h * 0.80f), Offset(w * 0.72f, h * 0.80f), strokeWidth = 2.dp.toPx())

    drawGlossOverlay(w, h)
}

/**
 * 10. Passport Photo Studio 2x2 grid
 */
private fun DrawScope.drawPassportStudioIcon(w: Float, h: Float) {
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF0284C7), Color(0xFF0369A1))),
        topLeft = Offset(w * 0.08f, h * 0.10f),
        size = Size(w * 0.84f, h * 0.80f),
        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
    )

    // 4 mini passport photo cuts
    val offsets = listOf(
        Offset(w * 0.15f, h * 0.18f),
        Offset(w * 0.53f, h * 0.18f),
        Offset(w * 0.15f, h * 0.52f),
        Offset(w * 0.53f, h * 0.52f)
    )

    for (pos in offsets) {
        drawRoundRect(
            color = Color.White,
            topLeft = pos,
            size = Size(w * 0.32f, h * 0.28f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
        drawCircle(
            color = Color(0xFF2563EB),
            radius = w * 0.06f,
            center = Offset(pos.x + w * 0.16f, pos.y + h * 0.12f)
        )
    }

    drawGlossOverlay(w, h)
}

/**
 * 11. Compress Photo Icon
 */
private fun DrawScope.drawCompressPhotoIcon(w: Float, h: Float) {
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF10B981), Color(0xFF059669))),
        topLeft = Offset(w * 0.10f, h * 0.12f),
        size = Size(w * 0.80f, h * 0.76f),
        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
    )

    // Center Image Frame with downward compress arrows
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(w * 0.22f, h * 0.24f),
        size = Size(w * 0.56f, h * 0.52f),
        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
    )

    // Compression arrows pointing in
    drawLine(Color(0xFF059669), Offset(w * 0.5f, h * 0.32f), Offset(w * 0.5f, h * 0.56f), strokeWidth = 3.dp.toPx())
    drawLine(Color(0xFF059669), Offset(w * 0.42f, h * 0.48f), Offset(w * 0.5f, h * 0.56f), strokeWidth = 3.dp.toPx())
    drawLine(Color(0xFF059669), Offset(w * 0.58f, h * 0.48f), Offset(w * 0.5f, h * 0.56f), strokeWidth = 3.dp.toPx())

    // "KB" pill
    drawRoundRect(
        color = Color(0xFF059669),
        topLeft = Offset(w * 0.34f, h * 0.60f),
        size = Size(w * 0.32f, h * 0.12f),
        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
    )

    drawGlossOverlay(w, h)
}

/**
 * 12. Edge & Perspective Crop Icon
 */
private fun DrawScope.drawEdgePerspectiveIcon(w: Float, h: Float) {
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFFF97316), Color(0xFFC2410C))),
        topLeft = Offset(w * 0.10f, h * 0.12f),
        size = Size(w * 0.80f, h * 0.76f),
        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
    )

    // Perspective mesh plane
    val path = Path().apply {
        moveTo(w * 0.30f, h * 0.26f)
        lineTo(w * 0.76f, h * 0.32f)
        lineTo(w * 0.66f, h * 0.76f)
        lineTo(w * 0.20f, h * 0.68f)
        close()
    }
    drawPath(path, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.7f))))

    // 4 Corner adjustment handles
    val handles = listOf(
        Offset(w * 0.30f, h * 0.26f),
        Offset(w * 0.76f, h * 0.32f),
        Offset(w * 0.66f, h * 0.76f),
        Offset(w * 0.20f, h * 0.68f)
    )
    for (handle in handles) {
        drawCircle(Color(0xFFF97316), radius = 4.dp.toPx(), center = handle)
        drawCircle(Color.White, radius = 2.dp.toPx(), center = handle)
    }

    drawGlossOverlay(w, h)
}

/**
 * 13. Digital Signature Validation Emerald Shield
 */
private fun DrawScope.drawSignatureValidIcon(w: Float, h: Float) {
    // Emerald Shield
    val shieldPath = Path().apply {
        moveTo(w * 0.5f, h * 0.12f)
        lineTo(w * 0.82f, h * 0.24f)
        lineTo(w * 0.82f, h * 0.54f)
        cubicTo(w * 0.82f, h * 0.74f, w * 0.5f, h * 0.88f, w * 0.5f, h * 0.88f)
        cubicTo(w * 0.5f, h * 0.88f, w * 0.18f, h * 0.74f, w * 0.18f, h * 0.54f)
        lineTo(w * 0.18f, h * 0.24f)
        close()
    }
    drawPath(
        shieldPath,
        Brush.verticalGradient(listOf(Color(0xFF10B981), Color(0xFF059669), Color(0xFF064E3B)))
    )

    // Golden Verified Checkmark
    val checkPath = Path().apply {
        moveTo(w * 0.32f, h * 0.46f)
        lineTo(w * 0.45f, h * 0.60f)
        lineTo(w * 0.68f, h * 0.36f)
    }
    drawPath(
        checkPath,
        Color(0xFFFDE047),
        style = Stroke(width = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
    )

    drawGlossOverlay(w, h)
}

/**
 * Top Specular 3D Gloss Highlight curve overlay
 */
private fun DrawScope.drawGlossOverlay(w: Float, h: Float) {
    val glossPath = Path().apply {
        moveTo(0f, 0f)
        lineTo(w, 0f)
        lineTo(w, h * 0.35f)
        cubicTo(w * 0.6f, h * 0.48f, w * 0.3f, h * 0.32f, 0f, h * 0.42f)
        close()
    }
    drawPath(
        glossPath,
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.35f),
                Color.White.copy(alpha = 0.05f)
            )
        )
    )
}
