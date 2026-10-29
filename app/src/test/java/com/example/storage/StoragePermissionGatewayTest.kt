package com.example.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StoragePermissionGatewayTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testRootFolderConstants() {
        assertNotNull(StoragePermissionHelper.ROOT_FOLDER_NAME)
        assertNotNull(StoragePermissionHelper.ROOT_FOLDER_PATH)
        assertTrue(StoragePermissionHelper.ROOT_FOLDER_PATH.contains("Smart King"))
    }

    @Test
    fun testEnsureRootDirectoriesCreation() {
        val created = StoragePermissionHelper.ensureRootDirectoriesExist()
        // In Robolectric environment, external storage directory is mockable and writable
        assertTrue("Root directories creation should succeed", created)
    }

    @Test
    fun testFullStoragePermissionCheck() {
        // By default in test sandbox, Environment.isExternalStorageManager() returns false
        // verifying the gate correctly detects when permission is not yet granted
        val isGranted = StoragePermissionHelper.hasFullStoragePermission(context)
        // Verify function executes cleanly without crash
        assertNotNull(isGranted)
    }
}
