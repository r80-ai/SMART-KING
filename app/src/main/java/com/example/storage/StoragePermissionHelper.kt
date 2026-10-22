package com.example.storage

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

object StoragePermissionHelper {
    private const val TAG = "StoragePermHelper"

    /**
     * Checks if the app has full broad external storage access.
     * On Android 11+ (API 30+), checks Environment.isExternalStorageManager().
     * On Android 10 and below, checks WRITE_EXTERNAL_STORAGE permission.
     */
    fun hasFullStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Environment.isExternalStorageManager()
            } catch (e: Exception) {
                Log.w(TAG, "isExternalStorageManager check failed: ${e.message}")
                false
            }
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Checks whether the root external folder (e.g. /storage/emulated/0/Smart King) can actually
     * be written to without throwing EPERM.
     */
    fun isRootExternalFolderWritable(context: Context, folderName: String = "Smart King"): Boolean {
        if (!hasFullStoragePermission(context)) return false
        return try {
            val dir = File(Environment.getExternalStorageDirectory(), folderName)
            if (!dir.exists()) {
                val created = dir.mkdirs()
                if (!created && !dir.exists()) return false
            }
            if (!dir.canWrite()) return false
            val testFile = File(dir, ".perm_test_${System.currentTimeMillis()}")
            testFile.outputStream().use { it.write(1) }
            testFile.delete()
            true
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Navigates the user to system settings to grant MANAGE_EXTERNAL_STORAGE permission
     * on Android 11+ or App Details on Android 10 and lower.
     */
    fun requestStoragePermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${activity.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                activity.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    activity.startActivity(intent)
                } catch (_: Exception) {
                    openAppSettings(activity)
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                1001
            )
        }
    }

    /**
     * Opens Application Details Settings page.
     */
    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app settings: ${e.message}")
        }
    }
}
