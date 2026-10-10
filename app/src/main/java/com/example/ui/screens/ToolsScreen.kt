package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LowPriority
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ActiveModal
import com.example.ui.AppScreen
import com.example.ui.NavigationTab
import com.example.ui.PdfMasterViewModel
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ToolBlue
import com.example.ui.theme.ToolGreen
import com.example.ui.theme.ToolIndigo
import com.example.ui.theme.ToolOrange
import com.example.ui.theme.ToolPink
import com.example.ui.theme.ToolPurple
import com.example.ui.theme.ToolTeal

data class ToolDefinition(
    val id: String,
    val title: String,
    val subtitleBn: String,
    val subtitleEn: String,
    val icon: ImageVector,
    val color: Color,
    val modal: ActiveModal,
    val extraTag: String = ""
)

data class ToolCategory(
    val nameEn: String,
    val nameBn: String,
    val icon: ImageVector,
    val badgeColor: Color,
    val tools: List<ToolDefinition>
)

private data class CategoryChipItem(
    val id: String,
    val name: String,
    val icon: ImageVector
)

// Dark Theme Color Palette for Reference Styling
private val DarkScreenBackground = Color(0xFF121215)
private val DarkCardBackground = Color(0xFF1E1E24)
private val DarkCardBorder = Color(0xFF2C2C35)
private val DarkSearchBackground = Color(0xFF1B1B20)
private val DarkSearchBorder = Color(0xFF2E2E36)
private val TextWhite = Color(0xFFF3F4F6)
private val TextMuted = Color(0xFF9CA3AF)

@Composable
fun ToolsScreen(
    viewModel: PdfMasterViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.appLanguage.collectAsState()
    val isBn = language == "bn"
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importPhotosAndCreatePdf(uris)
        }
    }
    val pdfPickerForEditLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importPdfFromUri(uri) { record ->
                viewModel.setWorkspaceEditMode(true)
                viewModel.openModal(ActiveModal.EDIT, record)
            }
        }
    }
    val pdfPickerForPdfToJpgLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importPdfFromUri(uri) { record ->
                viewModel.openModal(ActiveModal.PDF_TO_JPG, record)
            }
        }
    }
    val pinnedTools by viewModel.pinnedTools.collectAsState()
    var selectedCategoryFilter by remember { mutableStateOf("all") }
    var filterQuery by remember { mutableStateOf("") }

    // Categorized tools structured into standard PDF sections:
    // PDF Organize, Create, and Convert
    val categories = remember {
        listOf(
            ToolCategory(
                nameEn = "PDF Organize",
                nameBn = "PDF সাজানো",
                icon = Icons.Default.CallMerge,
                badgeColor = ToolIndigo,
                tools = listOf(
                    ToolDefinition("merge", "Merge PDF", "একাধিক PDF একসাথে", "Combine multiple PDFs into one document", Icons.Default.CallMerge, ToolIndigo, ActiveModal.MERGE),
                    ToolDefinition("split", "Split PDF", "PDF ভাগ করা", "Separate PDF pages or extract sections", Icons.Default.CallSplit, ToolBlue, ActiveModal.SPLIT),
                    ToolDefinition("remove_pages", "Remove Pages", "পৃষ্ঠা বাদ দেওয়া", "Delete unwanted pages instantly", Icons.Default.LowPriority, CrimsonPrimary, ActiveModal.REMOVE_PAGES),
                    ToolDefinition("extract_pages", "Extract Pages", "পৃষ্ঠা আলাদা করা", "Select and export specific pages", Icons.Default.Description, ToolTeal, ActiveModal.EXTRACT_PAGES)
                )
            ),
            ToolCategory(
                nameEn = "Create",
                nameBn = "তৈরি ও এডিট",
                icon = Icons.Default.Edit,
                badgeColor = CrimsonPrimary,
                tools = listOf(
                    ToolDefinition("doc_enhancer", "Doc Enhancer", "ডকুমেন্ট পরিষ্কার", "Clean photocopy & magic color filters", Icons.Default.AutoAwesome, ToolPurple, ActiveModal.DOC_ENHANCER, "New"),
                    ToolDefinition("jpg_to_pdf", "JPG to PDF", "ছবি থেকে PDF", "Convert JPG/PNG images to PDF", Icons.Default.PictureAsPdf, CrimsonPrimary, ActiveModal.SCANNER),
                    ToolDefinition("edit_pdf", "Edit PDF", "ডকুমেন্ট এডিট", "Full document editor with text and stamps", Icons.Default.Edit, ToolBlue, ActiveModal.EDIT),
                    ToolDefinition("crop_pdf", "Crop PDF", "মার্জিন কাটা", "Trim margins and adjust page dimensions", Icons.Default.Crop, ToolTeal, ActiveModal.CROP),
                    ToolDefinition("rotate_pdf", "Rotate PDF", "পৃষ্ঠা ঘোরানো", "Rotate landscape or upside-down pages", Icons.Default.RotateRight, ToolBlue, ActiveModal.ROTATE),
                    ToolDefinition("add_watermark", "Watermark", "জলছাপ যোগ", "Custom diagonal watermark stamp", Icons.Default.Draw, ToolPink, ActiveModal.WATERMARK),
                    ToolDefinition("add_page_nums", "Page Numbers", "পৃষ্ঠা নম্বর", "Add page numbering in header/footer", Icons.Default.Description, ToolIndigo, ActiveModal.PAGE_NUMBERS)
                )
            ),
            ToolCategory(
                nameEn = "Convert",
                nameBn = "কনভার্ট ও অপ্টিমাইজ",
                icon = Icons.Default.SwapHoriz,
                badgeColor = ToolGreen,
                tools = listOf(
                    ToolDefinition("pdf_to_jpg", "PDF to JPG", "PDF থেকে JPG ছবি", "Convert all PDF pages to high-quality JPG images", Icons.Default.Image, ToolOrange, ActiveModal.PDF_TO_JPG),
                    ToolDefinition("compress", "Compress PDF", "আকার কমানো", "Set custom target size in KB or MB", Icons.Default.Compress, ToolGreen, ActiveModal.COMPRESS)
                )
            ),
            ToolCategory(
                nameEn = "Security",
                nameBn = "নিরাপত্তা ও স্বাক্ষর",
                icon = Icons.Default.VerifiedUser,
                badgeColor = ToolGreen,
                tools = listOf(
                    ToolDefinition(
                        "signature_validation",
                        "PDF Signature Valid",
                        "ডিজিটাল স্বাক্ষর যাচাই",
                        "Verify Adobe & UIDAI digital signatures",
                        Icons.Default.VerifiedUser,
                        ToolGreen,
                        ActiveModal.SIGNATURE_VALIDATION
                    )
                )
            )
        )
    }

    val allTools = remember(categories) { categories.flatMap { it.tools } }
    val pinnedToolDefs = remember(pinnedTools, allTools) {
        pinnedTools.mapNotNull { id -> allTools.find { it.id == id } }
    }

    val onToolClick: (ToolDefinition) -> Unit = { tool ->
        when (tool.id) {
            "pdf_to_jpg" -> {
                pdfPickerForPdfToJpgLauncher.launch(arrayOf("application/pdf"))
            }
            "jpg_to_pdf" -> {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
            "edit_pdf" -> {
                pdfPickerForEditLauncher.launch(arrayOf("application/pdf"))
            }
            else -> {
                viewModel.openModal(tool.modal)
            }
        }
    }

    val filterChips = remember(categories, isBn) {
        listOf(
            CategoryChipItem(
                id = "all",
                name = if (isBn) "সব টুলস" else "All",
                icon = Icons.Default.GridView
            )
        ) + categories.map {
            CategoryChipItem(
                id = it.nameEn,
                name = if (isBn) it.nameBn else it.nameEn,
                icon = it.icon
            )
        }
    }

    AppScreen(modifier = modifier, isScrollable = false) {
        BackHandler(enabled = true) {
            if (!viewModel.popTab()) {
                viewModel.setTab(NavigationTab.HOME)
            }
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkScreenBackground)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("tools_screen_content"),
                contentPadding = PaddingValues(bottom = 36.dp)
            ) {
                // Header & Top Controls
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 8.dp)
                    ) {
                        // Title Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (!viewModel.popTab()) {
                                        viewModel.setTab(NavigationTab.HOME)
                                    }
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("tools_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Home",
                                    tint = TextWhite
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = if (isBn) "পিডিএফ টুলস" else "PDF Tools",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    ),
                                    color = TextWhite
                                )
                                Text(
                                    text = if (isBn) "সব প্রয়োজনীয় টুলস এক নজরে" else "All essential tools in one place",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Compact Search / Filter Field
                        OutlinedTextField(
                            value = filterQuery,
                            onValueChange = { filterQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("tools_search_filter"),
                            placeholder = {
                                Text(
                                    text = if (isBn) "টুলস খুঁজুন (Merge, Split, Compress...)" else "Search tools (Merge, Split, Compress...)",
                                    fontSize = 13.sp,
                                    color = TextMuted
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = CrimsonPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (filterQuery.isNotBlank()) {
                                    IconButton(
                                        onClick = { filterQuery = "" },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkSearchBackground,
                                unfocusedContainerColor = DarkSearchBackground,
                                focusedBorderColor = CrimsonPrimary,
                                unfocusedBorderColor = DarkSearchBorder,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Horizontal Category Quick Filter Chips
                        if (filterQuery.isBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                filterChips.forEach { chip ->
                                    val isSelected = selectedCategoryFilter == chip.id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedCategoryFilter = chip.id },
                                        label = {
                                            Text(
                                                text = chip.name,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 12.sp
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = chip.icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp),
                                                tint = if (isSelected) CrimsonPrimary else TextMuted
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CrimsonPrimary.copy(alpha = 0.18f),
                                            selectedLabelColor = CrimsonPrimary,
                                            containerColor = DarkCardBackground,
                                            labelColor = TextMuted
                                        ),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (isSelected) CrimsonPrimary.copy(alpha = 0.6f) else DarkCardBorder
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Search Active View: 4-column grid of results
                if (filterQuery.isNotBlank()) {
                    val searchResults = allTools.filter {
                        it.title.contains(filterQuery, ignoreCase = true) ||
                                it.subtitleBn.contains(filterQuery, ignoreCase = true) ||
                                it.subtitleEn.contains(filterQuery, ignoreCase = true)
                    }

                    item {
                        Text(
                            text = if (isBn) "অনুসন্ধানের ফলাফল (${searchResults.size})" else "Results (${searchResults.size})",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    if (searchResults.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isBn) "কোনো টুল খুঁজে পাওয়া যায়নি" else "No matching tools found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted
                                )
                            }
                        }
                    } else {
                        val rows = searchResults.chunked(4)
                        items(rows) { rowTools ->
                            Tool4ColumnRow(
                                rowTools = rowTools,
                                isBn = isBn,
                                onToolClick = onToolClick
                            )
                        }
                    }
                } else {
                    // Pinned / Favorites Section (if any pinned tools exist and "all" filter active)
                    if (selectedCategoryFilter == "all" && pinnedToolDefs.isNotEmpty()) {
                        item {
                            DarkSectionHeader(
                                title = if (isBn) "প্রিয় টুলস" else "Favorites",
                                icon = Icons.Default.Star,
                                iconColor = ToolOrange,
                                count = pinnedToolDefs.size
                            )
                        }

                        val pinnedRows = pinnedToolDefs.chunked(4)
                        items(pinnedRows) { rowTools ->
                            Tool4ColumnRow(
                                rowTools = rowTools,
                                isBn = isBn,
                                onToolClick = onToolClick
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    // Categorized Sections: PDF Organize, Create, Convert
                    val activeCategories = if (selectedCategoryFilter == "all") {
                        categories
                    } else {
                        categories.filter { it.nameEn == selectedCategoryFilter }
                    }

                    activeCategories.forEach { category ->
                        item {
                            DarkSectionHeader(
                                title = if (isBn) category.nameBn else category.nameEn,
                                icon = category.icon,
                                iconColor = category.badgeColor,
                                count = category.tools.size
                            )
                        }

                        val rows = category.tools.chunked(4)
                        items(rows) { rowTools ->
                            Tool4ColumnRow(
                                rowTools = rowTools,
                                isBn = isBn,
                                onToolClick = onToolClick
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 4-column row container ensuring consistent 1/4 width per card
 */
@Composable
private fun Tool4ColumnRow(
    rowTools: List<ToolDefinition>,
    isBn: Boolean,
    onToolClick: (ToolDefinition) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (tool in rowTools) {
            SquareToolCard(
                tool = tool,
                isBn = isBn,
                onClick = { onToolClick(tool) },
                modifier = Modifier.weight(1f)
            )
        }
        // Fill remaining spaces in an incomplete row to preserve exact 1/4 column sizing
        for (i in 0 until (4 - rowTools.size)) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/**
 * Compact square/rounded tool card matching reference visual hierarchy:
 * Dark rounded surface, signature PDF-related icon at top, tool name below.
 */
@Composable
private fun SquareToolCard(
    tool: ToolDefinition,
    isBn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(88.dp)
            .testTag("tool_card_${tool.id}")
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon container with soft tinted background
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tool.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = tool.title,
                    tint = tool.color,
                    modifier = Modifier.size(21.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tool name
            Text(
                text = tool.title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 13.sp
                ),
                color = TextWhite,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Section Header matching dark theme styling with category icon, title, and badge count.
 */
@Composable
private fun DarkSectionHeader(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(iconColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = TextWhite
            )
        }

        Surface(
            shape = CircleShape,
            color = DarkCardBackground,
            border = BorderStroke(1.dp, DarkCardBorder)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * Kept for HomeScreen and PhotoToolsScreen compatibility without modifying other files
 */
@Composable
fun ToolItemCard(
    tool: ToolDefinition,
    isBn: Boolean,
    isPinned: Boolean = false,
    onPinToggle: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("tool_card_${tool.id}")
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tool.color.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = tool.title,
                        tint = tool.color,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tool.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (tool.extraTag.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(tool.color.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = tool.extraTag,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tool.color
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isBn) tool.subtitleBn else tool.subtitleEn,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onPinToggle != null) {
                    IconButton(
                        onClick = onPinToggle,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isPinned) Icons.Default.Star else Icons.Outlined.StarOutline,
                            contentDescription = "Pin Tool",
                            tint = if (isPinned) ToolOrange else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Go",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Kept for external callers compatibility
 */
@Composable
fun CompactToolCard(
    tool: ToolDefinition,
    isBn: Boolean,
    isPinned: Boolean = false,
    onPinToggle: (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .testTag("tool_card_${tool.id}")
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 138.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tool.color.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = tool.title,
                        tint = tool.color,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (tool.extraTag.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(tool.color.copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tool.extraTag,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = tool.color
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    if (onPinToggle != null) {
                        IconButton(
                            onClick = onPinToggle,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isPinned) Icons.Default.Star else Icons.Outlined.StarOutline,
                                contentDescription = "Pin Tool",
                                tint = if (isPinned) ToolOrange else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column {
                Text(
                    text = tool.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isBn) tool.subtitleBn else tool.subtitleEn,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        lineHeight = 14.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
