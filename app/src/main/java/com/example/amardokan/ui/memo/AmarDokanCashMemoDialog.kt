package com.example.amardokan.ui.memo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.amardokan.ui.AmarDokanColors

@Composable
fun AmarDokanCashMemoDialog(
    memoData: AmarDokanCashMemoGenerator.MemoData,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val memoBitmap = remember(memoData) {
        AmarDokanCashMemoGenerator.generateMemoBitmap(memoData, context)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("dialog_cash_memo_preview")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ক্যাশ মেমো প্রিভিউ (#${memoData.billNumber})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = AmarDokanColors.NavyDarkHeader
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Bitmap Preview
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Image(
                        bitmap = memoBitmap.asImageBitmap(),
                        contentDescription = "ক্যাশ মেমো",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.FillWidth
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            AmarDokanCashMemoGenerator.shareOnWhatsAppOrApps(context, memoData)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_share_memo"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("হোয়াটসঅ্যাপ", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            AmarDokanCashMemoGenerator.printMemo(context, memoData)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AmarDokanColors.NavyDarkHeader),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_print_memo"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("প্রিন্ট স্লিপ", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
