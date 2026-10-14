package com.example.ui.screens

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.model.WorkspaceSubTool


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PdfFileRecord
import com.example.ui.ActiveModal
import com.example.ui.NavigationTab
import com.example.ui.PdfMasterViewModel
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ToolBlue
import com.example.ui.theme.ToolOrange
import com.example.ui.theme.ToolPurple
import com.example.ui.theme.ToolTeal
import com.example.ui.theme.ToolGreen
import com.example.ui.theme.ToolIndigo
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import com.example.ui.screens.ToolDefinition
import com.example.ui.screens.ToolItemCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.ui.AppScreen
import com.example.util.stringResource

@Composable
fun HomeScreen(
    viewModel: PdfMasterViewModel,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val recentFiles by viewModel.recentFiles.collectAsState()
    val pinnedTools by viewModel.pinnedTools.collectAsState()
    val language by viewModel.appLanguage.collectAsState()
    val lastBackupTime by com.example.workers.BackupManager.lastBackupTime.collectAsState()

    val headerTitle = stringResource(viewModel, "SMART KING", "স্মার্ট কিং", "स्मार्ट किंग")
    val headerSub = stringResource(viewModel, "All PDF Tools in One App", "সব টুলস এক অ্যাপে", "सभी पीडीएफ टूल्स")
    val pdfBtnSub = stringResource(viewModel, "All PDF Tools", "সব পিডিএফ টুলস", "सभी पीडीएफ टूल्स")
    val photoBtnSub = stringResource(viewModel, "Photo Tools", "ফটো টুলস", "फोटो टूल्स")

    val hisabTitle = stringResource(viewModel, "HISAB CALCULATOR", "হিসাব ক্যালকুলেটর", "हिसाब कैलकुलेटर")
    val hisabSub1 = stringResource(viewModel, "দোকান ও দৈনন্দিন হিসাব", "দোকান ও দৈনন্দিন হিসাব", "दुकान और दैनिक हिसाब")
    val hisabSub2 = stringResource(viewModel, "Price, weight, age, GST & more", "দাম, পরিমাণ, বয়স, ছাড়, GST, লাভ-ক্ষতি ও আরও অনেক হিসাব", "कीमत, वजन, आयु, जीएसटी और अधिक")

    val calendarTitle = stringResource(viewModel, "BANGLA CALENDAR", "বাংলা ক্যালেন্ডার", "बंगाली कैलेंडर")
    val calendarSub1 = stringResource(viewModel, "বাংলা ক্যালেন্ডার", "বাংলা ক্যালেন্ডার", "बंगाली कैलेंडर")
    val calendarSub2 = stringResource(viewModel, "Bangla Year, Tithi, Puja & Festivals", "বঙ্গাব্দ, তিথি, পূজা, উৎসব, সরকারি ছুটি ও উইজেট", "बंगाली वर्ष, तिथि, पूजा और त्योहार")

    val resumeTitle = stringResource(viewModel, "RESUME / CV MAKER", "রেজ্যুমে / সিভি মেকার", "रिज्यूमे / सीवी मेकर")
    val resumeSub1 = stringResource(viewModel, "Resume / CV Maker", "রেজ্যুমে / সিভি মেকার", "रिज्यूमे / सीवी मेकर")
    val resumeSub2 = stringResource(viewModel, "Create professional CV in minutes", "প্রফেশনাল রেজ্যুমে ও সিভি তৈরি করুন", "मिनटों में पेशेवर सीवी बनाएं")

    val pinnedHeader = stringResource(viewModel, "Pinned Tools", "প্রিয় টুলস", "पिन किए गए टूल")
    val recentHeader = stringResource(viewModel, "Recent Files", "রিসেন্ট ফাইলস", "हाल ही की फ़ाइलें")

    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importPhotosAndCreatePdf(uris)
        }
    }
    
    // Hardcoded subset of all tools to reference for Pinned Tools
    // To keep it simple, we construct a dummy ToolDefinition based on the ID for pinned items
    val pinnedToolDefs = pinnedTools.map { getToolById(it) }.filterNotNull()

AppScreen(modifier = modifier, isScrollable = false) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_content"),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // HEADER
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CrimsonPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "SMART KING Logo",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = headerTitle,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = headerSub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.setTab(NavigationTab.SETTINGS) },
                    modifier = Modifier.testTag("home_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            if (lastBackupTime > 0) {
                Text(
                    text = "Last backup: ${java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(lastBackupTime))}",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // PDF + PHOTO BUTTONS
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp)
                        .clickable { viewModel.setTab(NavigationTab.TOOLS) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CrimsonPrimary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = Color.White, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("PDF", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        Text(pdfBtnSub, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    }
                }
                
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp)
                        .clickable { viewModel.setTab(NavigationTab.PHOTO_TOOLS) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ToolBlue),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Image, contentDescription = "Photo", tint = Color.White, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("PHOTO", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        Text(photoBtnSub, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
        }


        // AMAR DOKAN SHOP MANAGEMENT CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clickable { viewModel.setTab(NavigationTab.AMAR_DOKAN) }
                    .testTag("card_amar_dokan"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2042)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = "Amar Dokan",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "আমার দোকান – Xerox & Service Hisab",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "জেরক্স, প্রিন্ট, বাকি খাতা ও ক্যাশ বিলিং হিসাব",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // HISAB CALCULATOR CARD (3RD MAIN TOOL)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 2.dp)
                    .clickable { viewModel.setTab(NavigationTab.HISAB_CALCULATOR) }
                    .testTag("card_hisab_calculator"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ToolGreen),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Hisab Calculator",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = hisabTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.White.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "NEW",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = hisabSub1,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = hisabSub2,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Open Hisab Calculator",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // RESUME / CV MAKER CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clickable { viewModel.openModal(ActiveModal.RESUME_BUILDER) }
                    .testTag("card_resume_maker"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ToolIndigo),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Resume Maker",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = resumeTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.White.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "NEW",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = resumeSub1,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = resumeSub2,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Open Resume Maker",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // CALENDAR CARD (MAIN HOME SCREEN BUTTON)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clickable { viewModel.setTab(NavigationTab.BANGLA_CALENDAR) }
                    .testTag("card_calendar_main"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ToolPurple),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Calendar",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = calendarTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.White.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "NEW",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = calendarSub1,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = calendarSub2,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Open Calendar",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // PINNED TOOLS
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = "Pinned", tint = ToolOrange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = pinnedHeader,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            if (pinnedToolDefs.isEmpty()) {
                Text(
                    text = if (language == "bn") "কোনো পিন করা টুল নেই। টুলস থেকে আপনার পছন্দের টুল পিন করুন।" else "No pinned tools. Go to Tools to pin your favorites.",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                pinnedToolDefs.forEach { tool ->
                    ToolItemCard(
                        tool = tool,
                        isBn = language == "bn",
                        onClick = { if (tool.id == "jpg_to_pdf") photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) else viewModel.openModal(tool.modal) }
                    )
                }
            }
        }

        // RECENT FILES
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = "Recent", tint = ToolPurple, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = recentHeader,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            if (recentFiles.isEmpty()) {
                Text(
                    text = if (language == "bn") "কোনো রিসেন্ট ফাইল নেই।" else "No recent files available.",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        items(recentFiles.take(5)) { file ->
            RecentFileRowItem(
                file = file,
                onOpen = { viewModel.openWorkspace(file, startInEditMode = false) },
                onEdit = { viewModel.openWorkspace(file, startInEditMode = true) },
                onShare = { viewModel.shareFile(context, file) },
                onRename = { viewModel.openModal(ActiveModal.RENAME, file) },
                onDelete = { viewModel.openModal(ActiveModal.DELETE_CONFIRM, file) },
                onFileInfo = { viewModel.openWorkspaceWithTool(file, WorkspaceSubTool.DOC_INFO) },
                onViewFolder = { viewModel.viewFolder(context, file) },
                onVerifySignatures = { viewModel.openModal(ActiveModal.SIGNATURE_VALIDATION, file) }
            )
        }
    }
    }
}

fun getToolById(id: String): ToolDefinition? {
    // Quick mapper for Pinned Tools feature.
    return when(id) {
        "signature_validation" -> ToolDefinition("signature_validation", "PDF Signature Valid", "ডিজিটাল স্বাক্ষর যাচাই", "Validate digital signatures", Icons.Default.VerifiedUser, ToolGreen, ActiveModal.SIGNATURE_VALIDATION)
        "pdf_to_jpg" -> ToolDefinition("pdf_to_jpg", "PDF to JPG", "PDF থেকে ছবি", "Convert PDF to JPG", Icons.Default.Image, ToolOrange, ActiveModal.PDF_TO_JPG)
        "merge" -> ToolDefinition("merge", "Merge PDF", "একাধিক PDF", "Combine PDFs", Icons.Default.CallMerge, CrimsonPrimary, ActiveModal.MERGE)
        "split" -> ToolDefinition("split", "Split PDF", "PDF ভাগ করা", "Split into multiple PDFs", Icons.Default.CallSplit, ToolBlue, ActiveModal.SPLIT)
        "remove_pages" -> ToolDefinition("remove_pages", "Remove Pages", "পৃষ্ঠা বাদ দেওয়া", "Delete unwanted pages instantly", Icons.Default.LowPriority, CrimsonPrimary, ActiveModal.REMOVE_PAGES)
        "compress" -> ToolDefinition("compress", "Compress PDF", "আকার কমানো", "Reduce file size", Icons.Default.Compress, Color(0xFF10B981), ActiveModal.COMPRESS)
        "crop_pdf" -> ToolDefinition("crop_pdf", "Crop PDF", "অংশ কাটা", "Trim margins", Icons.Default.Crop, Color(0xFF14B8A6), ActiveModal.CROP)
        "rotate_pdf" -> ToolDefinition("rotate_pdf", "Rotate PDF", "ঘোরানো", "Rotate pages", Icons.Default.RotateRight, ToolBlue, ActiveModal.ROTATE)
        "add_watermark" -> ToolDefinition("add_watermark", "Add Watermark", "জলছাপ", "Diagonal text", Icons.Default.Draw, Color(0xFFEC4899), ActiveModal.WATERMARK)
        "add_page_nums" -> ToolDefinition("add_page_nums", "Page Numbers", "নম্বর যোগ", "Footer numbers", Icons.Default.Description, Color(0xFF6366F1), ActiveModal.PAGE_NUMBERS)
        "edit_pdf" -> ToolDefinition("edit_pdf", "Edit PDF", "টেক্সট এডিট", "Full editor", Icons.Default.Edit, ToolBlue, ActiveModal.EDIT)
        "draw_annotate" -> ToolDefinition("draw_annotate", "Draw & Annotate", "ফ্রিহ্যান্ড আঁকা", "Pen, brush, shapes", Icons.Default.Highlight, ToolOrange, ActiveModal.DRAW_ANNOTATE)
        "extract_pages" -> ToolDefinition("extract_pages", "Extract Pages", "আলাদা করে নতুন PDF", "Select and export specific pages", Icons.Default.Description, Color(0xFF14B8A6), ActiveModal.EXTRACT_PAGES)
        "jpg_to_pdf" -> ToolDefinition("jpg_to_pdf", "JPG to PDF", "ছবি থেকে সরাসরি PDF তৈরি", "Convert JPG/PNG gallery images to PDF", Icons.Default.PictureAsPdf, CrimsonPrimary, ActiveModal.SCANNER)
        "doc_scanner" -> ToolDefinition("doc_scanner", "Scan Document", "ক্যামেরা দিয়ে ছবি তুলে PDF", "Capture multi-page paper documents", Icons.Default.CameraAlt, ToolBlue, ActiveModal.SCANNER)
        else -> null
    }
}

@Composable
fun RecentFileRowItem(
    file: PdfFileRecord,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onFileInfo: () -> Unit,
    onViewFolder: () -> Unit,
    onVerifySignatures: () -> Unit = {}
) {

    var menuExpanded by remember { mutableStateOf(false) }
    val sizeKb = (file.fileSize / 1024).coerceAtLeast(1)
    val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(file.lastModified))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("recent_file_${file.id}")
            .clickable { onOpen() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
                        .background(CrimsonPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF",
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$dateStr • ${file.pageCount} ${if (file.pageCount == 1) "page" else "pages"} • $sizeKb KB",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (file.isProtected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Protected",
                                tint = ToolOrange,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More actions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Open") },
                            leadingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null, tint = CrimsonPrimary) },
                            onClick = { menuExpanded = false; onOpen() }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = ToolBlue) },
                            onClick = { menuExpanded = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Share") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = { menuExpanded = false; onShare() }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = ToolBlue) },
                            onClick = { menuExpanded = false; onRename() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { menuExpanded = false; onDelete() }
                        )
                        DropdownMenuItem(
                            text = { Text("Verify Signatures") },
                            leadingIcon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = ToolGreen) },
                            onClick = { menuExpanded = false; onVerifySignatures() }
                        )
                        DropdownMenuItem(
                            text = { Text("File Information") },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = ToolTeal) },
                            onClick = { menuExpanded = false; onFileInfo() }
                        )
                        DropdownMenuItem(
                            text = { Text("View Folder") },
                            leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null, tint = ToolOrange) },
                            onClick = { menuExpanded = false; onViewFolder() }
                        )
                    }
                }
            }
        }
    }
}
    


