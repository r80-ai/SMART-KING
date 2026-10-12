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

    private val transport: NetHttpTransport by lazy { NetHttpTransport() }
    private val jsonFactory: GsonFactory by lazy { GsonFactory.getDefaultInstance() }

    private val credential: GoogleAccountCredential by lazy {
        val account = GoogleSignIn.getLastSignedInAccount(context)
            ?: throw IllegalStateException("Google account not signed in. Please connect your Google account.")
        GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE, SheetsScopes.SPREADSHEETS)
        ).apply {
            selectedAccount = account.account
        }
    }

    private val cachedDrive: Drive by lazy {
        Drive.Builder(transport, jsonFactory, credential)
            .setApplicationName("Amar Dokan Backup")
            .build()
    }

    private val cachedSheets: Sheets by lazy {
        Sheets.Builder(transport, jsonFactory, credential)
            .setApplicationName("Amar Dokan Backup")
            .build()
    }

    fun getDriveService(): Drive = cachedDrive
    fun getSheetsService(): Sheets = cachedSheets

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
     * Finds or creates the Google Spreadsheet [title] inside [folderId].
     * Creates with empty content or finds existing spreadsheet without duplicating.
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
            val spreadsheetId = files[0].id
            Log.d(tag, "Found existing spreadsheet '$title' (id: $spreadsheetId)")
            return spreadsheetId
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

    /**
     * Ensures all required tabs exist in the spreadsheet.
     * Also renames default 'Sheet1' to the first required tab if needed, or deletes it once tabs exist.
     */
    fun ensureSheetTabs(spreadsheetId: String, requiredTabs: List<String>) {
        val sheets = getSheetsService()
        val spreadsheet = sheets.spreadsheets().get(spreadsheetId).execute()
        val existingSheets = spreadsheet.sheets ?: emptyList()
        val existingTabNames = existingSheets.map { it.properties.title }.toSet()

        val requests = mutableListOf<Request>()

        // 1. If only default "Sheet1" exists and it's not in requiredTabs, rename it to the first required tab
        val defaultSheet = existingSheets.find { it.properties.title.equals("Sheet1", ignoreCase = true) }
        var firstTabUsedForRename = false
        if (defaultSheet != null && !requiredTabs.contains("Sheet1") && requiredTabs.isNotEmpty()) {
            val firstRequired = requiredTabs.first()
            if (!existingTabNames.contains(firstRequired)) {
                val renameReq = UpdateSheetPropertiesRequest().apply {
                    properties = SheetProperties().apply {
                        sheetId = defaultSheet.properties.sheetId
                        title = firstRequired
                    }
                    fields = "title"
                }
                requests.add(Request().setUpdateSheetProperties(renameReq))
                firstTabUsedForRename = true
                Log.d(tag, "Renaming default Sheet1 to '$firstRequired'")
            }
        }

        // 2. Add any other missing tabs
        for (tab in requiredTabs) {
            if (firstTabUsedForRename && tab == requiredTabs.first()) {
                continue
            }
            if (!existingTabNames.contains(tab)) {
                val addSheet = AddSheetRequest().apply {
                    properties = SheetProperties().setTitle(tab)
                }
                requests.add(Request().setAddSheet(addSheet))
            }
        }

        if (requests.isNotEmpty()) {
            val batchRequest = BatchUpdateSpreadsheetRequest().setRequests(requests)
            val response = sheets.spreadsheets().batchUpdate(spreadsheetId, batchRequest).execute()
            Log.d(tag, "Tabs ensured: executed ${requests.size} requests successfully. Total replies: ${response.replies?.size ?: 0}")
        } else {
            Log.d(tag, "All required tabs (${requiredTabs.joinToString()}) already exist.")
        }
    }

    /**
     * Reads all existing rows from [sheetName] starting at A1.
     * Returns empty list if sheet has no values or error occurs.
     */
    fun readSheetRows(spreadsheetId: String, sheetName: String): List<List<Any>> {
        val sheets = getSheetsService()
        val range = "'$sheetName'!A:Z"
        return try {
            val response = sheets.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute()
            @Suppress("UNCHECKED_CAST")
            (response.getValues() as? List<List<Any>>) ?: emptyList()
        } catch (e: Exception) {
            Log.w(tag, "Failed to read values from $sheetName: ${e.message}")
            emptyList()
        }
    }

    /**
     * Writes full tabular data into [sheetName] starting at A1 using [valueInputOption].
     * Properly escapes sheet name with single quotes.
     * Returns the UpdateValuesResponse with updated row count.
     */
    fun writeSheetData(
        spreadsheetId: String,
        sheetName: String,
        data: List<List<Any>>,
        valueInputOption: String = "USER_ENTERED"
    ): UpdateValuesResponse? {
        if (data.isEmpty()) return null
        val sheets = getSheetsService()
        val range = "'$sheetName'!A1"
        val valueRange = ValueRange().setValues(data)

        Log.d(tag, "Writing ${data.size} rows to $range with ValueInputOption.$valueInputOption")
        val response = sheets.spreadsheets().values()
            .update(spreadsheetId, range, valueRange)
            .setValueInputOption(valueInputOption)
            .execute()

        Log.d(tag, "writeSheetData finished for $sheetName: updatedRows=${response.updatedRows}, updatedCells=${response.updatedCells}, updatedRange=${response.updatedRange}")
        return response
    }

    /**
     * Appends rows to [sheetName] using ValueInputOption.RAW or USER_ENTERED.
     * Returns AppendValuesResponse.
     */
    fun appendSheetRows(
        spreadsheetId: String,
        sheetName: String,
        rows: List<List<Any>>,
        valueInputOption: String = "RAW"
    ): AppendValuesResponse? {
        if (rows.isEmpty()) return null
        val sheets = getSheetsService()
        val range = "'$sheetName'!A1"
        val valueRange = ValueRange().setValues(rows)

        Log.d(tag, "Appending ${rows.size} rows to $range with ValueInputOption.$valueInputOption")
        val response = sheets.spreadsheets().values()
            .append(spreadsheetId, range, valueRange)
            .setValueInputOption(valueInputOption)
            .setInsertDataOption("INSERT_ROWS")
            .execute()

        Log.d(tag, "appendSheetRows finished for $sheetName: updatedRows=${response.updates?.updatedRows}, tableRange=${response.tableRange}")
        return response
    }

    /**
     * Updates an exact row range in [sheetName].
     * [rowNumber] is 1-based sheet row index.
     */
    fun updateSheetRow(
        spreadsheetId: String,
        sheetName: String,
        rowNumber: Int,
        rowValues: List<Any>,
        valueInputOption: String = "RAW"
    ): UpdateValuesResponse? {
        val sheets = getSheetsService()
        val range = "'$sheetName'!A$rowNumber"
        val valueRange = ValueRange().setValues(listOf(rowValues))

        return sheets.spreadsheets().values()
            .update(spreadsheetId, range, valueRange)
            .setValueInputOption(valueInputOption)
            .execute()
    }

    /**
     * Synchronizes a table into [sheetName]:
     * 1. If tab has no rows at all, writes [headers] + all [records] starting at A1 with RAW.
     * 2. If tab has existing rows:
     *    - Validates/writes headers at A1 if empty.
     *    - Checks existing record IDs (column index [idColumnIndex]) to prevent duplicates.
     *    - If an existing ID is found and data changed, updates that specific row in place.
     *    - If an ID is new, appends it with appendSheetRows(valueInputOption = "RAW").
     *
     * Returns a Pair(rowsWrittenOrUpdated, totalRecordsProcessed).
     */
    fun syncTableWithIdDeduplication(
        spreadsheetId: String,
        sheetName: String,
        headers: List<Any>,
        records: List<List<Any>>,
        idColumnIndex: Int = 0,
        valueInputOption: String = "RAW"
    ): Pair<Int, Int> {
        val existingRows = readSheetRows(spreadsheetId, sheetName)
        Log.d(tag, "[$sheetName] Existing rows in sheet: ${existingRows.size}, New records to sync: ${records.size}")

        if (existingRows.isEmpty()) {
            // Sheet is completely empty. Initialize headers and all records at once!
            val fullData = mutableListOf<List<Any>>()
            fullData.add(headers)
            fullData.addAll(records)

            val response = writeSheetData(spreadsheetId, sheetName, fullData, valueInputOption)
            val updatedRows = response?.updatedRows ?: fullData.size
            Log.i(tag, "[$sheetName] Initialized empty sheet with headers and ${records.size} records. Updated rows: $updatedRows")
            return Pair(records.size, records.size)
        }

        // Sheet already has data. First verify header at row 1
        val firstRow = existingRows.firstOrNull()
        if (firstRow.isNullOrEmpty()) {
            writeSheetData(spreadsheetId, sheetName, listOf(headers), valueInputOption)
        }

        if (records.isEmpty()) {
            Log.d(tag, "[$sheetName] No local records to sync. Existing headers preserved.")
            return Pair(0, 0)
        }

        // Map existing record IDs to row numbers (1-indexed)
        // Row 1 is header, so row 2 corresponds to index 1
        val existingIdToRowIndex = mutableMapOf<String, Int>()
        for (i in 1 until existingRows.size) {
            val row = existingRows[i]
            if (row.size > idColumnIndex) {
                val idStr = row[idColumnIndex].toString().trim()
                if (idStr.isNotEmpty()) {
                    existingIdToRowIndex[idStr] = i + 1 // 1-based row index in Google Sheets
                }
            }
        }

        val rowsToAppend = mutableListOf<List<Any>>()
        var updatedCount = 0

        for (record in records) {
            val recordId = if (record.size > idColumnIndex) record[idColumnIndex].toString().trim() else ""
            val existingRowNumber = existingIdToRowIndex[recordId]

            if (existingRowNumber != null) {
                // Row already exists in Sheet. Check if values differ, update in place if needed
                val currentSheetRow = existingRows[existingRowNumber - 1]
                val recordStrings = record.map { it.toString() }
                val currentStrings = currentSheetRow.map { it.toString() }

                if (recordStrings != currentStrings) {
                    try {
                        updateSheetRow(spreadsheetId, sheetName, existingRowNumber, record, valueInputOption)
                        updatedCount++
                        Log.d(tag, "[$sheetName] Updated existing record ID=$recordId at row $existingRowNumber")
                    } catch (e: Exception) {
                        Log.w(tag, "[$sheetName] Failed to update row $existingRowNumber for ID $recordId: ${e.message}")
                    }
                }
            } else {
                // New record, queue for append
                rowsToAppend.add(record)
            }
        }

        var appendedCount = 0
        if (rowsToAppend.isNotEmpty()) {
            val appendResponse = appendSheetRows(spreadsheetId, sheetName, rowsToAppend, valueInputOption)
            appendedCount = appendResponse?.updates?.updatedRows ?: rowsToAppend.size
            Log.i(tag, "[$sheetName] Appended $appendedCount new rows.")
        }

        val totalWritten = updatedCount + appendedCount
        Log.i(tag, "[$sheetName] Sync completed: $totalWritten changes ($updatedCount updated, $appendedCount appended) out of ${records.size} local records.")
        return Pair(totalWritten, records.size)
    }
}
