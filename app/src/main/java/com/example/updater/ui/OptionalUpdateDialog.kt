package com.example.updater.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrimsonPrimary
import com.example.updater.AppUpdateManager
import com.example.updater.UpdateManifest
import com.example.updater.UpdateState

@Composable
fun OptionalUpdateDialog(
    updateManager: AppUpdateManager,
    manifest: UpdateManifest,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val updateState by updateManager.updateState.collectAsState()

    AlertDialog(
        onDismissRequest = {
            if (updateState !is UpdateState.Downloading && updateState !is UpdateState.Installing) {
                onDismiss()
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NewReleases,
                    contentDescription = null,
                    tint = CrimsonPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "নতুন আপডেট উপলভ্য!",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "SMART KING v${manifest.latestVersionName} (Build ${manifest.latestVersionCode}) প্রস্তুত।",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                if (manifest.releaseNotes.isNotBlank()) {
                    Text(
                        text = "আপডেটের বিবরণ:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = manifest.releaseNotes,
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                when (val state = updateState) {
                    is UpdateState.Downloading -> {
                        Spacer(modifier = Modifier.height(6.dp))
                        if (state.progressPercent >= 0) {
                            LinearProgressIndicator(
                                progress = { state.progressPercent / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = CrimsonPrimary
                            )
                            Text(
                                text = "ডাউনলোড হচ্ছে: ${state.progressPercent}%",
                                style = MaterialTheme.typography.labelSmall
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = CrimsonPrimary
                            )
                            Text("ডাউনলোড হচ্ছে...", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    is UpdateState.Downloaded -> {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ডাউনলোড সফল হয়েছে! নিচের বাটনে চাপ দিয়ে ইনস্টল করুন।",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF16A34A), fontWeight = FontWeight.SemiBold)
                        )
                    }
                    is UpdateState.Error -> {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ত্রুটি: ${state.message}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error)
                        )
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            when (val state = updateState) {
                is UpdateState.Downloading -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = CrimsonPrimary)
                }
                is UpdateState.Downloaded -> {
                    Button(
                        onClick = {
                            activity?.let { updateManager.installApk(it, state.apkFile) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ইনস্টল করুন (Install)")
                    }
                }
                else -> {
                    Button(
                        onClick = { updateManager.startDownload(manifest) },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("এখনই আপডেট করুন")
                    }
                }
            }
        },
        dismissButton = {
            if (updateState !is UpdateState.Downloading && updateState !is UpdateState.Installing) {
                TextButton(onClick = onDismiss) {
                    Text("পরে")
                }
            }
        }
    )
}
