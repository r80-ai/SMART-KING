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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.LowPriority
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TextFields
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ActiveModal
import com.example.ui.AppScreen
import com.example.ui.NavigationTab
import com.example.ui.PdfMasterViewModel
import com.example.ui.components.ModernCardBackground
import com.example.ui.components.ModernCardBorder
import com.example.ui.components.ModernScreenBackground
import com.example.ui.components.ModernSubtitleColor
import com.example.ui.components.ModernTitleColor
import com.example.ui.components.ModernToolCard
import com.example.ui.components.ModernToolCardGrid
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
    val pinnedSet = pinnedTools.toSet()
    var selectedCategoryFilter by remember { mutableStateOf("all") }
    var filterQuery by remember { mutableStateOf("") }

    val categories = remember {
        listOf(
            ToolCategory(
                nameEn = "PDF Organize",
                nameBn = "PDF সাজানো",
                icon = Icons.Default.CallMerge,
                badgeColor = ToolIndigo,
                tools = listOf(
                    ToolDefinition("merge", "Merge PDF", "একাধিক PDF একসাথে", "Combine multiple PDFs into one document", Icons.Default.CallMerge, ToolIndigo, ActiveModal.MERGE, "HOT"),
                    ToolDefinition("split", "Split PDF", "PDF ভাগ করা", "Separate PDF pages or extract sections", Icons.Default.CallSplit, ToolBlue, ActiveModal.SPLIT, "FREE"),
                    ToolDefinition("remove_pages", "Remove Pages", "পৃষ্ঠা বাদ দেওয়া", "Delete unwanted pages instantly", Icons.Default.LowPriority, CrimsonPrimary, ActiveModal.REMOVE_PAGES, "FREE"),
                    ToolDefinition("extract_pages", "Extract Pages", "পৃষ্ঠা আলাদা করা", "Select and export specific pages", Icons.Default.Description, ToolTeal, ActiveModal.EXTRACT_PAGES, "FREE")
                )
            ),
            ToolCategory(
                nameEn = "Create & Edit",
                nameBn = "তৈরি ও এডিট",
                icon = Icons.Default.Edit,
                badgeColor = CrimsonPrimary,
                tools = listOf(
                    ToolDefinition("doc_enhancer", "Doc Scanner", "ডকুমেন্ট ও ফটোকপি পরিষ্কার", "Clean photocopy, B&W whitening & magic color", Icons.Default.AutoAwesome, ToolPurple, ActiveModal.DOC_ENHANCER, "HOT"),
                    ToolDefinition("jpg_to_pdf", "JPG to PDF", "ছবি থেকে PDF", "Convert JPG/PNG images to PDF", Icons.Default.PictureAsPdf, CrimsonPrimary, ActiveModal.SCANNER, "FREE"),
                    ToolDefinition("edit_pdf", "Edit PDF", "ডকুমেন্ট এডিট", "Full document editor with text and stamps", Icons.Default.Edit, ToolBlue, ActiveModal.EDIT, "PRO"),
                    ToolDefinition("crop_pdf", "Crop PDF", "মার্জিন কাটা", "Trim margins and adjust page dimensions", Icons.Default.Crop, ToolTeal, ActiveModal.CROP, "FREE"),
                    ToolDefinition("rotate_pdf", "Rotate PDF", "পৃষ্ঠা ঘোরানো", "Rotate landscape or upside-down pages", Icons.Default.RotateRight, ToolBlue, ActiveModal.ROTATE, "FREE"),
                    ToolDefinition("add_watermark", "Watermark", "জলছাপ যোগ", "Custom diagonal watermark stamp", Icons.Default.Draw, ToolPink, ActiveModal.WATERMARK, "NEW"),
                    ToolDefinition("add_page_nums", "Page Numbers", "পৃষ্ঠা নম্বর", "Add page numbering in header/footer", Icons.Default.Description, ToolIndigo, ActiveModal.PAGE_NUMBERS, "FREE")
                )
            ),
            ToolCategory(
                nameEn = "Convert",
                nameBn = "কনভার্ট ও অপ্টিমাইজ",
                icon = Icons.Default.SwapHoriz,
                badgeColor = ToolGreen,
                tools = listOf(
                    ToolDefinition("pdf_to_jpg", "PDF to JPG", "PDF থেকে JPG ছবি", "Convert all PDF pages to high-quality JPG images", Icons.Default.Image, ToolOrange, ActiveModal.PDF_TO_JPG, "FREE"),
                    ToolDefinition("compress", "Compress PDF", "আকার কমানো", "Set custom target size in KB or MB", Icons.Default.Compress, ToolGreen, ActiveModal.COMPRESS, "HOT")
                )
            ),
            ToolCategory(
                nameEn = "Security",
                nameBn = "নিরাপত্তা ও আনলক",
                icon = Icons.Default.VerifiedUser,
                badgeColor = ToolGreen,
                tools = listOf(
                    ToolDefinition("lock_remover", "Lock Remover", "পাসওয়ার্ড আনলক", "Remove restrictions & unlock password", Icons.Default.LockOpen, CrimsonPrimary, ActiveModal.EDIT, "PRO"),
                    ToolDefinition("signature_validation", "Signature Valid", "ডিজিタル স্বাক্ষর যাচাই", "Verify Adobe & UIDAI digital signatures", Icons.Default.VerifiedUser, ToolGreen, ActiveModal.SIGNATURE_VALIDATION, "PRO")
                )
            )
        )
    }

    val allTools = remember(categories) { categories.flatMap { it.tools } }

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
            "edit_pdf", "lock_remover" -> {
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

    // Active tools based on search and category filter
    val displayedTools = remember(filterQuery, selectedCategoryFilter, allTools) {
        if (filterQuery.isNotBlank()) {
            allTools.filter {
                it.title.contains(filterQuery, ignoreCase = true) ||
                        it.subtitleBn.contains(filterQuery, ignoreCase = true) ||
                        it.subtitleEn.contains(filterQuery, ignoreCase = true)
            }
        } else if (selectedCategoryFilter == "all") {
            allTools
        } else {
            categories.find { it.nameEn == selectedCategoryFilter }?.tools ?: allTools
        }
    }

    AppScreen(modifier = modifier, isScrollable = false) {
        BackHandler(enabled = true) {
            if (!viewModel.popTab()) {
                viewModel.setTab(NavigationTab.HOME)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ModernScreenBackground)
                .testTag("tools_screen_content")
        ) {
            ModernToolCardGrid(
                tools = displayedTools,
                isBn = isBn,
                pinnedToolIds = pinnedSet,
                onPinToggle = { toolId ->
                    if (pinnedSet.contains(toolId)) {
                        viewModel.unpinTool(toolId)
                    } else {
                        viewModel.pinTool(toolId)
                    }
                },
                onToolClick = onToolClick,
                headerContent = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 6.dp)
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
                                    tint = ModernTitleColor
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
                                    color = ModernTitleColor
                                )
                                Text(
                                    text = if (isBn) "সব প্রয়োজনীয় টুলস এক নজরে" else "All essential tools in one place",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = ModernSubtitleColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Modern Search Bar
                        OutlinedTextField(
                            value = filterQuery,
                            onValueChange = { filterQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .shadow(2.dp, RoundedCornerShape(12.dp), spotColor = Color(0x0A000000))
                                .testTag("tools_search_filter"),
                            placeholder = {
                                Text(
                                    text = if (isBn) "টুলস খুঁজুন (Merge, Split, Compress...)" else "Search tools (Merge, Split, Compress...)",
                                    fontSize = 13.sp,
                                    color = ModernSubtitleColor
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
                                            tint = ModernSubtitleColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = ModernCardBackground,
                                unfocusedContainerColor = ModernCardBackground,
                                focusedBorderColor = CrimsonPrimary,
                                unfocusedBorderColor = ModernCardBorder,
                                focusedTextColor = ModernTitleColor,
                                unfocusedTextColor = ModernTitleColor
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
                                                tint = if (isSelected) CrimsonPrimary else ModernSubtitleColor
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CrimsonPrimary.copy(alpha = 0.12f),
                                            selectedLabelColor = CrimsonPrimary,
                                            containerColor = ModernCardBackground,
                                            labelColor = ModernSubtitleColor
                                        ),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (isSelected) CrimsonPrimary.copy(alpha = 0.5f) else ModernCardBorder
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

/**
 * Kept for HomeScreen compatibility so any existing calls work seamlessly
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
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
