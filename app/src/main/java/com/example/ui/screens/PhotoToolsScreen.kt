package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ActiveModal
import com.example.ui.AppScreen
import com.example.ui.NavigationTab
import com.example.ui.PdfMasterViewModel
import com.example.ui.components.ModernScreenBackground
import com.example.ui.components.ModernTitleColor
import com.example.ui.components.ModernToolCardGrid
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ToolBlue
import com.example.ui.theme.ToolGreen
import com.example.ui.theme.ToolOrange
import com.example.ui.theme.ToolPurple

@Composable
fun PhotoToolsScreen(
    viewModel: PdfMasterViewModel,
    modifier: Modifier = Modifier
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importPhotosAndCreatePdf(uris)
        }
    }

    val pinnedTools by viewModel.pinnedTools.collectAsState()
    val pinnedSet = pinnedTools.toSet()

    val tools = listOf(
        ToolDefinition("aadhaar_cropper", "Aadhaar AI Cropper", "আধার কার্ড ক্রপ ও প্রিন্ট", "Biometric fingerprint & Aadhaar emblem auto-crop", Icons.Default.Badge, CrimsonPrimary, ActiveModal.PVC_CARD_MAKER, "HOT"),
        ToolDefinition("pan_card", "PAN Card AI Cropper", "প্যান কার্ড ক্রপ ও সাইজ", "Income Tax PAN card realistic preview & print", Icons.Default.Badge, CrimsonPrimary, ActiveModal.PAN_CARD, "FREE"),
        ToolDefinition("voter_card", "Voter Card AI Cropper", "ভোটার কার্ড ক্রপ ও সাইজ", "Tricolor voter ID badge crop & A4 print", Icons.Default.Badge, CrimsonPrimary, ActiveModal.PVC_CARD_MAKER, "NEW"),
        ToolDefinition("pvc_smart_card", "Epson L8050 / PVC Maker", "স্মার্ট পিভিসি কার্ড প্রিন্ট", "Photo printer and PVC card mockup printer", Icons.Default.Badge, CrimsonPrimary, ActiveModal.PVC_CARD_MAKER, "PRO"),
        ToolDefinition("doc_enhancer", "Doc Scanner", "ডকুমেন্ট ও ফটোকপি পরিষ্কার", "Clean photocopy, B&W whitening & magic color", Icons.Default.AutoAwesome, ToolPurple, ActiveModal.DOC_ENHANCER, "HOT"),
        ToolDefinition("split_camera", "Split Camera", "লাইভ ডুয়াল ক্যামেরা", "Camera lens dual-view real-time comparison", Icons.Default.CameraAlt, CrimsonPrimary, ActiveModal.SPLIT_CAMERA, "NEW"),
        ToolDefinition("photo_editing", "Photo Editing", "ছবি এডিট ও আপস্কেল", "Upscale face portrait, crop & pro retouching", Icons.Default.Edit, ToolOrange, ActiveModal.PHOTO_EDITING, "PRO"),
        ToolDefinition("name_date_photo", "Name & Date on Photo", "নাম ও তারিখ সহ ছবি", "Add candidate name, DOP/DOB date & black border", Icons.Default.TextFields, ToolOrange, ActiveModal.NAME_DATE_PHOTO, "NEW"),
        ToolDefinition("passport_photo", "Passport Photo Maker", "পাসপোর্ট সাইজ ফটো স্টুডিও", "4-Step studio workflow, 8/16 passport photo sheet", Icons.Default.Person, ToolBlue, ActiveModal.PASSPORT_PHOTO, "HOT"),
        ToolDefinition("compress_photo", "Compress Photo", "ছবির সাইজ কমান", "Reduce image file size to custom target KB/MB", Icons.Default.Compress, ToolGreen, ActiveModal.COMPRESS_PHOTO, "FREE"),
        ToolDefinition("edge_detect", "Edge & Perspective", "মার্জিন ও কোণ সোজা করুন", "Fix perspective and auto-straighten crop", Icons.Default.Crop, ToolOrange, ActiveModal.EDGE_PERSPECTIVE, "FREE")
    )

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
                .testTag("photo_tools_screen")
        ) {
            ModernToolCardGrid(
                tools = tools,
                isBn = false,
                pinnedToolIds = pinnedSet,
                onPinToggle = { toolId ->
                    if (pinnedSet.contains(toolId)) {
                        viewModel.unpinTool(toolId)
                    } else {
                        viewModel.pinTool(toolId)
                    }
                },
                onToolClick = { tool ->
                    if (tool.id == "jpg_to_pdf") {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    } else {
                        viewModel.openModal(tool.modal)
                    }
                },
                headerContent = {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 10.dp, bottom = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (!viewModel.popTab()) {
                                        viewModel.setTab(NavigationTab.HOME)
                                    }
                                },
                                modifier = Modifier.testTag("photo_tools_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Home",
                                    tint = ModernTitleColor
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = "Photo Tools",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    ),
                                    color = ModernTitleColor
                                )
                                Text(
                                    text = "All Image, ID & Scanner utilities",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}
