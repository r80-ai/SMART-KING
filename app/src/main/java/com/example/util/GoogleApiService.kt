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
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.*
import java.io.File
import java.util.Collections

class GoogleApiService(private val context: Context) {

    private val tag = "GoogleApiService"

    private fun getCredential(): GoogleAccountCredential {
        val account = GoogleSignIn.getLastSignedInAccount(context)
            ?: throw IllegalStateException("Google account not signed in. Please connect your Google account.")
        
        return GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE, SheetsScopes.SPREADSHEETS)
        ).apply {
            selectedAccount = account.account
        }
    }

    fun getDriveService(): Drive {
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            getCredential()
        ).setApplicationName("Amar Dokan Backup").build()
    }

    fun getSheetsService(): Sheets {
        return Sheets.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            getCredential()
        ).setApplicationName("Amar Dokan Backup").build()
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
            Log.d(tag, "Found existing folder '$name' (id: ${files[0].id})")
            return files[0].id
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
     * Finds or creates the Google Spreadsheet [title] inside [folderId].
     * Prevents duplicate spreadsheet creation.
     */
    fun findOrCreateSpreadsheet(title: String, folderId: String): String {
        val drive = getDriveService()
        val query = "name = '$title' and mimeType = 'application/vnd.google-apps.spreadsheet' and '$folderId' in parents and trashed = false"
        
        val response = drive.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id, name)")
            .execute()

        val files = response.files
        if (!files.isNullOrEmpty()) {
            Log.d(tag, "Found existing spreadsheet '$title' (id: ${files[0].id})")
            return files[0].id
        }

        val fileMetadata = com.google.api.services.drive.model.File().apply {
            setName(title)
            setMimeType("application/vnd.google-apps.spreadsheet")
            setParents(Collections.singletonList(folderId))
        }
        val created = drive.files().create(fileMetadata)
            .setFields("id")
            .execute()
        Log.d(tag, "Created new spreadsheet '$title' (id: ${created.id})")
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

    /**
     * Ensures all required tabs exist in the spreadsheet.
     */
    fun ensureSheetTabs(spreadsheetId: String, requiredTabs: List<String>) {
        val sheets = getSheetsService()
        val spreadsheet = sheets.spreadsheets().get(spreadsheetId).execute()
        val existingTabs = spreadsheet.sheets?.map { it.properties.title }?.toSet() ?: emptySet()

        val requests = mutableListOf<Request>()
        for (tab in requiredTabs) {
            if (!existingTabs.contains(tab)) {
                val addSheet = AddSheetRequest().apply {
                    properties = SheetProperties().setTitle(tab)
                }
                requests.add(Request().setAddSheet(addSheet))
            }
        }

        if (requests.isNotEmpty()) {
            val batchRequest = BatchUpdateSpreadsheetRequest().setRequests(requests)
            sheets.spreadsheets().batchUpdate(spreadsheetId, batchRequest).execute()
            Log.d(tag, "Created missing tabs: ${requests.size} tabs")
        }
    }

    /**
     * Writes tabular data into [sheetName] starting at A1.
     * Clears previous content to ensure consistent snapshots without stale records.
     */
    fun writeSheetData(spreadsheetId: String, sheetName: String, data: List<List<Any>>) {
        if (data.isEmpty()) return
        val sheets = getSheetsService()

        try {
            sheets.spreadsheets().values()
                .clear(spreadsheetId, "$sheetName!A1:Z5000", ClearValuesRequest())
                .execute()
        } catch (e: Exception) {
            Log.w(tag, "Clear sheet failed for $sheetName: ${e.message}")
        }

        val range = "$sheetName!A1"
        val valueRange = ValueRange().setValues(data)
        sheets.spreadsheets().values()
            .update(spreadsheetId, range, valueRange)
            .setValueInputOption("USER_ENTERED")
            .execute()
        Log.d(tag, "Updated $sheetName with ${data.size} rows")
    }
}
