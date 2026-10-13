package com.example.util

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import java.io.File
import java.io.FileOutputStream
import java.util.Collections

class GoogleApiService(private val context: Context) {

    private val tag = "GoogleApiService"

    private val transport: NetHttpTransport by lazy { NetHttpTransport() }
    private val jsonFactory: GsonFactory by lazy { GsonFactory.getDefaultInstance() }

    private val credential: GoogleAccountCredential by lazy {
        val account = GoogleSignIn.getLastSignedInAccount(context)
            ?: throw IllegalStateException("Google account not signed in. Please connect your Google account.")
        GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE)
        ).apply {
            selectedAccount = account.account
        }
    }

    private val cachedDrive: Drive by lazy {
        Drive.Builder(transport, jsonFactory, credential)
            .setApplicationName("Amar Dokan Backup")
            .build()
    }

    fun getDriveService(): Drive = cachedDrive

    /**
     * Checks if a folder with [name] already exists under [parentId] (or root if null).
     * Returns the folder ID if found, or null otherwise.
     */
    fun findFolder(name: String, parentId: String? = null): String? {
        val drive = getDriveService()
        val parentCondition = if (parentId != null) "'$parentId' in parents" else "'root' in parents"
        val query = "name = '$name' and mimeType = 'application/vnd.google-apps.folder' and $parentCondition and trashed = false"

        val response = drive.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()

        val files = response.files
        return if (!files.isNullOrEmpty()) files[0].id else null
    }

    /**
     * Finds or creates a folder with [name] under [parentId] (or root if null).
     * Prevents duplicate folder creation.
     */
    fun findOrCreateFolder(name: String, parentId: String? = null): String {
        val drive = getDriveService()
        val parentCondition = if (parentId != null) "'$parentId' in parents" else "'root' in parents"
        val query = "name = '$name' and mimeType = 'application/vnd.google-apps.folder' and $parentCondition and trashed = false"

        val response = drive.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()

        val files = response.files
        if (!files.isNullOrEmpty()) {
            val folderId = files[0].id
            Log.d(tag, "Found existing folder '$name' (id: $folderId)")
            return folderId
        }

        val folderMetadata = com.google.api.services.drive.model.File().apply {
            setName(name)
            setMimeType("application/vnd.google-apps.folder")
            if (parentId != null) {
                setParents(Collections.singletonList(parentId))
            }
        }
        val created = drive.files().create(folderMetadata)
            .setFields("id")
            .execute()
        Log.d(tag, "Created new folder '$name' (id: ${created.id})")
        return created.id
    }

    /**
     * Checks if a file with [fileName] already exists in [folderId].
     * Returns its file ID if found, or null otherwise.
     */
    fun findFileInFolder(fileName: String, folderId: String): String? {
        val drive = getDriveService()
        val query = "name = '$fileName' and '$folderId' in parents and trashed = false"
        val response = drive.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()
        val files = response.files
        return if (!files.isNullOrEmpty()) files[0].id else null
    }

    /**
     * Uploads a file to [folderId].
     * If a file with [fileName] already exists in that folder, returns the existing ID without duplicating.
     */
    fun uploadFile(file: File, folderId: String, fileName: String, mimeType: String): String {
        if (!file.exists() || file.length() == 0L) {
            throw IllegalArgumentException("Cannot upload empty or non-existent file: ${file.absolutePath}")
        }
        val existingId = findFileInFolder(fileName, folderId)
        if (existingId != null) {
            Log.d(tag, "File '$fileName' already exists in folder (id: $existingId). Skipping upload.")
            return existingId
        }

        val drive = getDriveService()
        val fileMetadata = com.google.api.services.drive.model.File().apply {
            setName(fileName)
            setParents(Collections.singletonList(folderId))
        }
        val mediaContent = FileContent(mimeType, file)
        val created = drive.files().create(fileMetadata, mediaContent)
            .setFields("id")
            .execute()
        Log.d(tag, "Uploaded '$fileName' successfully (id: ${created.id})")
        return created.id
    }

    fun downloadFile(fileId: String, outputFile: File) {
        val drive = getDriveService()
        drive.files().get(fileId).executeMediaAndDownloadTo(FileOutputStream(outputFile))
    }
}
