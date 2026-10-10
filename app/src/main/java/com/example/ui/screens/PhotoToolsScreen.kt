package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ActiveModal
import com.example.ui.NavigationTab
import com.example.ui.PdfMasterViewModel
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.ToolBlue
import com.example.ui.theme.ToolOrange
import com.example.ui.theme.ToolGreen
import com.example.ui.AppScreen

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

    val tools = listOf(
        ToolDefinition("name_date_photo", "Name & Date on Photo", "ছবিতে নাম ও তারিখ", "Add candidate name & DOB/DOP on passport photo with auto-scaled bottom white strip", Icons.Default.Badge, CrimsonPrimary, ActiveModal.NAME_DATE_PHOTO),
        ToolDefinition("doc_enhancer", "Doc Scanner & Auto-Brightness", "ডক স্ক্যানার ও ব্রাইটনেস", "Clean photocopy filter, remove yellow tint, whitening & contrast enhancement", Icons.Default.CameraAlt, ToolBlue, ActiveModal.DOC_ENHANCER),
        ToolDefinition("pvc_smart_card", "PVC Smart Card Maker & Printer", "Standard CR80 300 DPI", "e-Aadhaar, PAN, Voter, Ayushman, Auto-Crop, De-Skew & A4/4×6 Print Sheets", Icons.Default.Badge, CrimsonPrimary, ActiveModal.PVC_CARD_MAKER),
        ToolDefinition("split_camera", "Split Camera", "Live Dual-View Camera", "Interactive split screen comparison with real-time natural enhancement", Icons.Default.CameraAlt, CrimsonPrimary, ActiveModal.SPLIT_CAMERA),
        ToolDefinition("photo_editing", "Photo Editing", "Crop, Adjust, Markup & Pro Tools", "Crop, rotate, resize, brightness, blur, erase, perspective, text & drawing", Icons.Default.Edit, ToolOrange, ActiveModal.PHOTO_EDITING),
        ToolDefinition("pan_card", "PAN Card", "NSDL / Protean & UTIITSL", "Auto crop, resize, DPI & compress Photo, Signature, Document", Icons.Default.Badge, CrimsonPrimary, ActiveModal.PAN_CARD),
        ToolDefinition("compress_photo", "Compress Photo", "Reduce image file size", "Custom target size in KB/MB", Icons.Default.Compress, ToolGreen, ActiveModal.COMPRESS_PHOTO),
        ToolDefinition("edge_detect", "Edge & Perspective", "Fix perspective and crop", "Automatically straighten and crop documents", Icons.Default.Crop, ToolOrange, ActiveModal.EDGE_PERSPECTIVE),
        ToolDefinition("passport_photo", "Multiple Passport Photo Maker", "4-Step Studio Workflow", "Add multiple photos, crop to 3.3×4.3cm or custom, adjust BG, top-aligned A4/4×6 sheet", Icons.Default.Person, ToolBlue, ActiveModal.PASSPORT_PHOTO)
    )

AppScreen(modifier = modifier, isScrollable = false) {
    BackHandler(enabled = true) {
        if (!viewModel.popTab()) {
            viewModel.setTab(NavigationTab.HOME)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("photo_tools_screen"),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
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
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Photo Tools",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "All Image and Scanner utilities",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        items(tools) { tool ->
            ToolItemCard(
                tool = tool,
                isBn = false,
                onClick = {
                    if (tool.id == "jpg_to_pdf") {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    } else {
                        viewModel.openModal(tool.modal)
                    }
                }
            )
        }
    }
    }
}
