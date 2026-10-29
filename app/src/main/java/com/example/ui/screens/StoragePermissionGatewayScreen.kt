package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.storage.StoragePermissionHelper

/**
 * Mandatory startup gate for MANAGE_EXTERNAL_STORAGE (All Files Access).
 * Blocks access to all application features until the user explicitly grants
 * unrestricted storage access to ensure permanent write permissions to
 * /storage/emulated/0/Smart King.
 */
@Composable
fun StoragePermissionGatewayScreen(
    onPermissionGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Legacy permission launcher for Android 10 and below (API <= 29)
    val legacyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true &&
            results[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        ) {
            StoragePermissionHelper.ensureRootDirectoriesExist()
            onPermissionGranted()
        }
    }

    // BackHandler: If user tries to back out, close app completely so gate cannot be bypassed
    BackHandler {
        activity?.finishAffinity()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Glowing Shield & Folder Icon
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Security Gateway",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(52.dp)
                )
            }

            // Brand Header & Badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "প্রয়োজনীয় নিরাপত্তা অনুমতি • Required Storage Permission",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Text(
                text = "স্মার্ট কিং • Smart King",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "ডিভাইসের সব ফাইল অ্যাক্সেস (All Files Access) প্রয়োজন",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "স্মার্ট কিং অ্যাপের সমস্ত ফিচার (আমার দোকান হিসাব ব্যাকআপ, পিডিএফ ক্রপ ও মার্জিং) স্থায়ীভাবে ব্যবহারের জন্য ডিভাইসের স্টোরেজ অ্যাক্সেস দেওয়া আবশ্যক। ফাইলগুলো সরাসরি আপনার ফোনের /storage/emulated/0/Smart King ফোল্ডারে সেভ হবে এবং আর কখনো পারমিশন চাইতে হবে না।",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 21.sp
                ),
                textAlign = TextAlign.Center
            )

            // Feature Explanation Cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PermissionFeatureRow(
                    icon = Icons.Default.Folder,
                    title = "রুট স্টোরেজ ডিরেক্টরি",
                    path = "/storage/emulated/0/Smart King",
                    description = "সরাসরি ফোন মেমোরিতে ফাইল সংরক্ষণ; কোনো সাব-ফোল্ডার সিলেক্টের ঝামেলা নেই।"
                )

                PermissionFeatureRow(
                    icon = Icons.Default.Storefront,
                    title = "আমার দোকান ব্যাকআপ ও রিস্টোর",
                    path = "/storage/emulated/0/Smart King/Amar Dokan",
                    description = "দোকানের সমস্ত কাস্টমার খাতা ও বিলের ডাটাবেস সম্পূর্ণ অফলাইনে নিরাপদ থাকবে।"
                )

                PermissionFeatureRow(
                    icon = Icons.Default.PictureAsPdf,
                    title = "পিডিএফ ও ফটো টুলস",
                    path = "তাত্ক্ষণিক ফাইল সেভ ও ক্রপিং",
                    description = "পিডিএফ কনভার্ট, পেজ ক্রপ, ব্যাকগ্রাউন্ড রিমুভাল ও প্রিন্ট রেডি আউটপুট।"
                )

                PermissionFeatureRow(
                    icon = Icons.Default.Lock,
                    title = "১০০% অফলাইন ও নিরাপদ",
                    path = "ডিভাইস-অনলি প্রাইভেসি",
                    description = "আপনার ব্যক্তিগত হিসাব বা ডকুমেন্টস কোনো ক্লাউডে যায় না; সম্পূর্ণ নিজের ফোনে থাকে।"
                )
            }

            // Step-by-Step Instructions Banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "অনুমতি দেওয়ার নিয়মাবলী:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "১. নিচের 'অনুমতি দিন (Grant Access)' বাটনে চাপুন।\n" +
                               "২. ফোন সেটিংসে গিয়ে 'Allow access to manage all files' সুইচটি চালু করুন।\n" +
                               "৩. ব্যাক করে অ্যাপে ফিরলে সম্পূর্ণ অ্যাপ সরাসরি খুলে যাবে।",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            Button(
                onClick = {
                    if (activity != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            StoragePermissionHelper.requestStoragePermission(activity)
                        } else {
                            legacyLauncher.launch(
                                arrayOf(
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                                )
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btn_grant_all_files_access"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "অনুমতি দিন (Grant All Files Access)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    activity?.finishAffinity()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_exit_app"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "অ্যাপ বন্ধ করুন (Exit App)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Text(
                text = "অনুমতি না দিলে অ্যাপের কোনো মডিউলে প্রবেশ করা সম্ভব নয়।",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun PermissionFeatureRow(
    icon: ImageVector,
    title: String,
    path: String,
    description: String
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = path,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
