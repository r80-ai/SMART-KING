package com.example.util

import android.content.Context
import com.google.android.gms.auth.api.signin.*
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import java.util.Collections

class GoogleApiService(private val context: Context) {

    private fun getCredential(): GoogleAccountCredential {
        val account = GoogleSignIn.getLastSignedInAccount(context)
            ?: throw IllegalStateException("No user signed in")
        
        return GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE, SheetsScopes.SPREADSHEETS)
        ).apply {
            setSelectedAccount(account.account)
        }
    }

    fun getDriveService(): Drive {
        val credential = getCredential()
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Amar Dokan Backup").build()
    }

    fun getSheetsService(): Sheets {
        val credential = getCredential()
        return Sheets.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Amar Dokan Backup").build()
    }

    fun findOrCreateFolder(name: String, parentId: String? = null): String {
        val query = "name = '$name' and mimeType = 'application/vnd.google-apps.folder' and trashed = false" +
                (if (parentId != null) " and '$parentId' in parents" else " and 'root' in parents")
        val files = getDriveService().files().list().setQ(query).execute().files
        
        if (files.isNotEmpty()) {
            return files[0].id
        }
        
        val folderMetadata = com.google.api.services.drive.model.File().apply {
            setName(name)
            setMimeType("application/vnd.google-apps.folder")
            if (parentId != null) setParents(Collections.singletonList(parentId))
        }
        return getDriveService().files().create(folderMetadata).setFields("id").execute().id
    }

    fun uploadFile(file: java.io.File, folderId: String, fileName: String, mimeType: String): String {
        val fileMetadata = com.google.api.services.drive.model.File().apply {
            setName(fileName)
            setParents(Collections.singletonList(folderId))
        }
        val mediaContent = com.google.api.client.http.FileContent(mimeType, file)
        return getDriveService().files().create(fileMetadata, mediaContent).setFields("id").execute().id
    }

    fun uploadToSheets(spreadsheetId: String, sheetName: String, data: List<List<Any>>) {
        val range = "$sheetName!A1"
        val valueRange = com.google.api.services.sheets.v4.model.ValueRange().setValues(data)
        getSheetsService().spreadsheets().values()
            .update(spreadsheetId, range, valueRange)
            .setValueInputOption("RAW")
            .execute()
    }

    fun findOrCreateSpreadsheet(title: String, folderId: String): String {
        // Implementation to find or create spreadsheet
        return "" // Placeholder for now
    }
