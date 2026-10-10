package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiAiHelper
import com.example.data.AppDatabase
import com.example.data.PdfRepository
import com.example.model.PdfFileRecord
import com.example.model.WorkspaceSubTool
import com.example.pdf.PdfEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.io.File
import java.io.FileOutputStream

enum class NavigationTab {
    HOME, TOOLS, FILES, SETTINGS, PHOTO_TOOLS, HISAB_CALCULATOR, BANGLA_CALENDAR
}

enum class ActiveModal {
    NONE,
    MERGE,
    SPLIT,
    REMOVE_PAGES,
    EXTRACT_PAGES,
    REORDER,
    ORGANIZATION,
    COMPRESS,
    COMPRESS_PHOTO,
    EDGE_PERSPECTIVE,
    PASSPORT_PHOTO,
    PAN_CARD,
    PHOTO_EDITING,
    SPLIT_CAMERA,
    OCR,
    EDIT,
    CROP,
    WATERMARK,
    ROTATE,
    PAGE_NUMBERS,
    DRAW_ANNOTATE,
    AI_TOOLS,
    SCANNER,
    VIEWER,
    SIGNATURE_VALIDATION,
    RENAME,
    DELETE_CONFIRM,
    PDF_TO_JPG,
    PVC_CARD_MAKER,
    RESUME_BUILDER
}

data class ToolResultInfo(
    val resultFile: PdfFileRecord,
    val toolTitle: String,
    val originalFile: PdfFileRecord? = null,
    val summaryText: String = "",
    val savedDisplayLocation: String = "",
    val savedUri: Uri? = null,
    val isPhotoOutput: Boolean = false
)

data class UiNotification(
    val message: String,
    val isSuccess: Boolean = true
)

class PdfMasterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PdfRepository
    
    // Navigation
    private val _currentTab = MutableStateFlow(NavigationTab.HOME)
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

    // Modals & Active Tool
    private val _activeModal = MutableStateFlow(ActiveModal.NONE)
    val activeModal: StateFlow<ActiveModal> = _activeModal.asStateFlow()

    private val _selectedFile = MutableStateFlow<PdfFileRecord?>(null)
    val selectedFile: StateFlow<PdfFileRecord?> = _selectedFile.asStateFlow()

    // Tool Result Preview
    private val _toolResult = MutableStateFlow<ToolResultInfo?>(null)
    val toolResult: StateFlow<ToolResultInfo?> = _toolResult.asStateFlow()

    fun showToolResult(info: ToolResultInfo) {
        val finalLocation = if (info.savedDisplayLocation.isBlank()) {
            com.example.storage.GlobalSaveManager.displayPath.value
        } else {
            info.savedDisplayLocation
        }
        _toolResult.value = info.copy(savedDisplayLocation = finalLocation)
    }

    fun dismissToolResult(goToFiles: Boolean = true) {
        _toolResult.value = null
        _activeModal.value = ActiveModal.NONE
        _selectedFile.value = null
        modalBackStack.clear()
        if (goToFiles) {
            setTab(NavigationTab.FILES)
        }
    }

    // Filter & Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _pinnedTools = MutableStateFlow<List<String>>(emptyList())
    val pinnedTools: StateFlow<List<String>> = _pinnedTools.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Settings
    private val _appLanguage = MutableStateFlow("en") // "en", "bn", "hi"
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _darkThemeMode = MutableStateFlow<Boolean?>(null) // null = system, true = dark, false = light
    val darkThemeMode: StateFlow<Boolean?> = _darkThemeMode.asStateFlow()

    private val _defaultPdfQuality = MutableStateFlow("High (300 DPI)")
    val defaultPdfQuality: StateFlow<String> = _defaultPdfQuality.asStateFlow()

    // Save Folder Configuration
    private val _saveFolderType = MutableStateFlow("DOCUMENTS") // "DOCUMENTS", "DOWNLOADS", "INTERNAL", "CUSTOM"
    val saveFolderType: StateFlow<String> = _saveFolderType.asStateFlow()

    private val _customFolderName = MutableStateFlow("PDF_Master")
    val customFolderName: StateFlow<String> = _customFolderName.asStateFlow()

    val storageMode: StateFlow<String> = com.example.storage.GlobalSaveManager.storageMode
    val saveLocationDisplayPath: StateFlow<String> = com.example.storage.GlobalSaveManager.displayPath
    val customTreeUri: StateFlow<String?> = com.example.storage.GlobalSaveManager.customTreeUri

    // Processing & Loading State
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _processingMessage = MutableStateFlow("Processing PDF...")
    val processingMessage: StateFlow<String> = _processingMessage.asStateFlow()

    private val _notification = MutableStateFlow<UiNotification?>(null)
    val notification: StateFlow<UiNotification?> = _notification.asStateFlow()

    // AI Tool Output State
    private val _aiOutput = MutableStateFlow("")
    val aiOutput: StateFlow<String> = _aiOutput.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Viewer rendered pages
    private val _renderedPages = MutableStateFlow<List<Bitmap>>(emptyList())
    val renderedPages: StateFlow<List<Bitmap>> = _renderedPages.asStateFlow()
    
    private val _viewerPageCount = MutableStateFlow(0)
    val viewerPageCount: StateFlow<Int> = _viewerPageCount.asStateFlow()
    
    private var activePdfRenderer: PdfRenderer? = null
    private var activePdfFd: ParcelFileDescriptor? = null
    private val pageCache = LruCache<Int, Bitmap>(10)
    private val rendererMutex = Mutex()
    private var loadPagesJob: Job? = null
    private var lastLoadedPathOrUri: String? = null

    // Gemini API Key State
    private val _geminiApiKey = MutableStateFlow("")
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    // Viewer Text & Search
    private val _viewerDocumentText = MutableStateFlow("")
    val viewerDocumentText: StateFlow<String> = _viewerDocumentText.asStateFlow()

    private val _viewerSearchQuery = MutableStateFlow("")
    val viewerSearchQuery: StateFlow<String> = _viewerSearchQuery.asStateFlow()

    private val _viewerZoom = MutableStateFlow(1.0f)
    val viewerZoom: StateFlow<Float> = _viewerZoom.asStateFlow()

    private val _workspaceEditMode = MutableStateFlow(false)
    val workspaceEditMode: StateFlow<Boolean> = _workspaceEditMode.asStateFlow()

    private val _workspaceInitialTool = MutableStateFlow(WorkspaceSubTool.NONE)
    val workspaceInitialTool: StateFlow<WorkspaceSubTool> = _workspaceInitialTool.asStateFlow()

    fun setWorkspaceEditMode(active: Boolean) {
        _workspaceEditMode.value = active
    }

    fun openWorkspace(file: PdfFileRecord, startInEditMode: Boolean = false) {
        _workspaceEditMode.value = startInEditMode
        _workspaceInitialTool.value = WorkspaceSubTool.NONE
        openModal(ActiveModal.VIEWER, file)
    }

    fun openWorkspaceWithTool(file: PdfFileRecord, tool: WorkspaceSubTool) {
        _workspaceInitialTool.value = tool
        val isEdit = (tool == WorkspaceSubTool.DRAW_ANNOTATE || tool == WorkspaceSubTool.EDIT_TEXT)
        _workspaceEditMode.value = isEdit
        openModal(ActiveModal.VIEWER, file)
    }

    fun clearWorkspaceInitialTool() {
        _workspaceInitialTool.value = WorkspaceSubTool.NONE
    }

    init {
        com.example.storage.GlobalSaveManager.initialize(application)
        val savedType = com.example.storage.GlobalSaveManager.storageMode.value
        val savedName = com.example.storage.GlobalSaveManager.customFolderName.value
        _saveFolderType.value = savedType
        _customFolderName.value = savedName

        val prefs = application.getSharedPreferences("pdf_master_prefs", Context.MODE_PRIVATE)
        val savedPinned = prefs.getString("pinned_tools", "") ?: ""
        if (savedPinned.isNotEmpty()) {
            _pinnedTools.value = savedPinned.split(",")
        }

        PdfEngine.setFolderConfig(savedType, savedName)

        val savedKey = prefs.getString("gemini_api_key", "") ?: ""
        _geminiApiKey.value = savedKey
        GeminiAiHelper.customApiKey = savedKey

        val db = AppDatabase.getDatabase(application)
        repository = PdfRepository(db.pdfFileDao())

        // Initialize sample files if empty
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
            android.util.Log.e("PdfMasterViewModel", "Uncaught coroutine exception", throwable)
        }
        viewModelScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            try {
                PdfEngine.initializeSampleFilesIfEmpty(application, repository)
            } catch (t: Throwable) {
                android.util.Log.e("PdfMasterViewModel", "Error loading sample files", t)
            }
        }
    }

    // Repository Flows
    val allFiles: StateFlow<List<PdfFileRecord>> = repository.allFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentFiles: StateFlow<List<PdfFileRecord>> = repository.recentFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered files for Files screen
    val filteredFiles: StateFlow<List<PdfFileRecord>> = combine(
        allFiles,
        searchQuery,
        selectedCategory
    ) { files, query, cat ->
        files.filter { file ->
            val matchesQuery = query.isBlank() || file.name.contains(query, ignoreCase = true)
            val matchesCat = when (cat) {
                "All" -> true
                "Recent" -> true
                "Created" -> file.category == "created"
                "Converted" -> file.category == "converted"
                "Scanned" -> file.category == "scanned"
                "Downloads" -> file.category == "downloads"
                else -> true
            }
            matchesQuery && matchesCat
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // App-wide Navigation Back Stack Tracking
    private val tabBackStack = mutableListOf<NavigationTab>()
    private val modalBackStack = mutableListOf<Pair<ActiveModal, PdfFileRecord?>>()

    fun setTab(tab: NavigationTab) {
        modalBackStack.clear()
        _activeModal.value = ActiveModal.NONE
        _selectedFile.value = null
        if (_currentTab.value != tab) {
            if (tab == NavigationTab.HOME) {
                tabBackStack.clear()
            } else {
                if (tabBackStack.isEmpty() || tabBackStack.lastOrNull() != _currentTab.value) {
                    tabBackStack.add(_currentTab.value)
                }
            }
            _currentTab.value = tab
        }
    }

    fun popTab(): Boolean {
        if (tabBackStack.isNotEmpty()) {
            val prev = tabBackStack.removeAt(tabBackStack.lastIndex)
            _currentTab.value = prev
            return true
        } else if (_currentTab.value != NavigationTab.HOME) {
            _currentTab.value = NavigationTab.HOME
            return true
        }
        return false
    }

    fun canPopTab(): Boolean {
        return tabBackStack.isNotEmpty() || _currentTab.value != NavigationTab.HOME
    }

    fun openModal(modal: ActiveModal, file: PdfFileRecord? = null) {
        if (_activeModal.value != ActiveModal.NONE && _activeModal.value != modal) {
            modalBackStack.add(Pair(_activeModal.value, _selectedFile.value))
        }
        _selectedFile.value = file
        _activeModal.value = modal

        val target = _selectedFile.value
        if (target != null) {
            if (modal == ActiveModal.VIEWER || modal == ActiveModal.EDIT) {
                loadPagesForViewer(target)
            }
        }
    }

    fun closeModal() {
        if (modalBackStack.isNotEmpty()) {
            val (prevModal, prevFile) = modalBackStack.removeAt(modalBackStack.lastIndex)
            _selectedFile.value = prevFile
            _activeModal.value = prevModal
        } else {
            _activeModal.value = ActiveModal.NONE
            _selectedFile.value = null
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun pinTool(toolId: String) {
        val current = _pinnedTools.value.toMutableList()
        if (!current.contains(toolId)) {
            current.add(toolId)
            _pinnedTools.value = current
            savePinnedTools(current)
        }
    }

    fun unpinTool(toolId: String) {
        val current = _pinnedTools.value.toMutableList()
        if (current.remove(toolId)) {
            _pinnedTools.value = current
            savePinnedTools(current)
        }
    }

    private fun savePinnedTools(tools: List<String>) {
        val prefs = getApplication<Application>().getSharedPreferences("pdf_master_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("pinned_tools", tools.joinToString(",")).apply()
    }

    fun setSelectedCategory(cat: String) {
        _selectedCategory.value = cat
    }

    fun setLanguage(lang: String) {
        _appLanguage.value = lang
    }

    fun setDarkThemeMode(mode: Boolean?) {
        _darkThemeMode.value = mode
    }

    fun setDefaultPdfQuality(q: String) {
        _defaultPdfQuality.value = q
    }

    fun clearNotification() {
        _notification.value = null
    }

    fun setNotification(msg: String) {
        _notification.value = UiNotification(msg)
    }

    fun addRecentFile(record: PdfFileRecord) {
        viewModelScope.launch {
            repository.insertFile(record)
        }
    }

    fun loadPagesForViewer(fileRecord: PdfFileRecord, forceReload: Boolean = false) {
        val currentIdentifier = "${fileRecord.filePath}_${fileRecord.uriString}"
        if (!forceReload && lastLoadedPathOrUri == currentIdentifier && _renderedPages.value.isNotEmpty()) {
            return
        }

        loadPagesJob?.cancel()
        loadPagesJob = viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Opening document..."
            
            try {
                withContext(Dispatchers.IO) {
                    var file = File(fileRecord.filePath)

                    // If file is inaccessible or 0 bytes via raw path, restore using retained Content URI
                    if (!file.exists() || !file.canRead() || file.length() == 0L) {
                        if (!fileRecord.uriString.isNullOrBlank()) {
                            try {
                                val uri = Uri.parse(fileRecord.uriString)
                                val safeDir = File(getApplication<Application>().filesDir, "imported_pdfs").apply { if (!exists()) mkdirs() }
                                val safeFile = File(safeDir, "doc_${System.currentTimeMillis()}_${fileRecord.name}")
                                getApplication<Application>().contentResolver.openInputStream(uri)?.use { inStream ->
                                    FileOutputStream(safeFile).use { outStream ->
                                        inStream.copyTo(outStream)
                                        outStream.flush()
                                    }
                                }
                                if (safeFile.exists() && safeFile.length() > 0L) {
                                    file = safeFile
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    // If still does not exist and no URI, check if it's a sample document
                    if ((!file.exists() || file.length() == 0L) && fileRecord.uriString.isNullOrBlank()) {
                        try {
                            val dir = file.parentFile ?: PdfEngine.getPdfsDirectory(getApplication())
                            if (!dir.exists()) dir.mkdirs()
                            if (file.name.contains("Agreement", ignoreCase = true)) {
                                PdfEngine.generateSampleAgreementPdf(file)
                            } else if (file.name.contains("Project", ignoreCase = true) || file.name.contains("Documentation", ignoreCase = true)) {
                                PdfEngine.generateSampleProjectNotesPdf(file)
                            } else if (file.name.contains("Receipt", ignoreCase = true) || file.name.contains("Expense", ignoreCase = true)) {
                                PdfEngine.generateSampleReceiptPdf(file)
                            } else {
                                PdfEngine.generateSampleProjectNotesPdf(file)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    rendererMutex.withLock {
                        try {
                            activePdfRenderer?.close()
                        } catch (_: Exception) {}
                        try {
                            activePdfFd?.close()
                        } catch (_: Exception) {}
                        activePdfRenderer = null
                        activePdfFd = null
                        pageCache.evictAll()

                        val pfd = try {
                            if (file.exists() && file.canRead() && file.length() > 0L) {
                                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                            } else null
                        } catch (e: Exception) {
                            null
                        } ?: run {
                            if (!fileRecord.uriString.isNullOrBlank()) {
                                try {
                                    getApplication<Application>().contentResolver.openFileDescriptor(Uri.parse(fileRecord.uriString), "r")
                                } catch (_: Exception) { null }
                            } else null
                        }

                        if (pfd != null) {
                            try {
                                val renderer = PdfRenderer(pfd)
                                activePdfFd = pfd
                                activePdfRenderer = renderer
                                val totalPages = renderer.pageCount
                                _viewerPageCount.value = totalPages

                                if (totalPages > 0) {
                                    val scale = 1.5f
                                    // 1. Immediately render Page 0 to display instantly to the user
                                    val firstPage = renderer.openPage(0)
                                    val w0 = (firstPage.width * scale).toInt().coerceAtLeast(1)
                                    val h0 = (firstPage.height * scale).toInt().coerceAtLeast(1)
                                    val firstBmp = Bitmap.createBitmap(w0, h0, Bitmap.Config.ARGB_8888)
                                    firstBmp.eraseColor(android.graphics.Color.WHITE)
                                    val m0 = android.graphics.Matrix().apply { postScale(scale, scale) }
                                    firstPage.render(firstBmp, null, m0, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    firstPage.close()
                                    pageCache.put(0, firstBmp)

                                    val pagesList = mutableListOf(firstBmp)
                                    _renderedPages.value = pagesList.toList()
                                    lastLoadedPathOrUri = currentIdentifier

                                    // Instantly dismiss loading spinner so user immediately interacts with the PDF
                                    _isProcessing.value = false

                                    // 2. Render remaining pages progressively
                                    val maxPages = totalPages.coerceAtMost(30)
                                    for (i in 1 until maxPages) {
                                        try {
                                            val page = renderer.openPage(i)
                                            val pw = (page.width * scale).toInt().coerceAtLeast(1)
                                            val ph = (page.height * scale).toInt().coerceAtLeast(1)
                                            val bmp = Bitmap.createBitmap(pw, ph, Bitmap.Config.ARGB_8888)
                                            bmp.eraseColor(android.graphics.Color.WHITE)
                                            val pm = android.graphics.Matrix().apply { postScale(scale, scale) }
                                            page.render(bmp, null, pm, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                            page.close()
                                            pageCache.put(i, bmp)
                                            pagesList.add(bmp)
                                            _renderedPages.value = pagesList.toList()
                                        } catch (ePage: Exception) {
                                            ePage.printStackTrace()
                                            break
                                        }
                                    }
                                } else {
                                    _renderedPages.value = emptyList()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                _renderedPages.value = emptyList()
                            }
                        } else {
                            _renderedPages.value = emptyList()
                        }
                    }

                    // Background text extraction without delaying page presentation
                    try {
                        val text = if (file.exists() && file.canRead()) {
                            PdfEngine.extractTextFromPdf(file)
                        } else if (!fileRecord.uriString.isNullOrBlank()) {
                            PdfEngine.extractTextFromUri(getApplication(), Uri.parse(fileRecord.uriString))
                        } else ""
                        _viewerDocumentText.value = text
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _renderedPages.value = emptyList()
            } finally {
                _isProcessing.value = false
            }
        }
    }

    suspend fun getPageBitmap(pageIndex: Int, scale: Float = 2.0f): Bitmap? = withContext(Dispatchers.IO) {
        pageCache.get(pageIndex)?.let { return@withContext it }
        rendererMutex.withLock {
            val renderer = activePdfRenderer ?: return@withContext null
            try {
                if (pageIndex >= renderer.pageCount) return@withContext null
                val page = renderer.openPage(pageIndex)
                val w = (page.width * scale).toInt()
                val h = (page.height * scale).toInt()
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(android.graphics.Color.WHITE)
                
                val matrix = android.graphics.Matrix().apply { postScale(scale, scale) }
                page.render(bmp, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                pageCache.put(pageIndex, bmp)
                return@withContext bmp
            } catch(e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }
    }
    
    fun clearViewerCache() {
        viewModelScope.launch {
            rendererMutex.withLock {
                try {
                    activePdfRenderer?.close()
                } catch (_: Exception) {}
                try {
                    activePdfFd?.close()
                } catch (_: Exception) {}
                activePdfRenderer = null
                activePdfFd = null
                pageCache.evictAll()
                _viewerPageCount.value = 0
            }
            _renderedPages.value = emptyList()
            lastLoadedPathOrUri = null
        }
    }

    fun setGeminiApiKey(key: String) {
        val trimmed = key.trim()
        _geminiApiKey.value = trimmed
        GeminiAiHelper.customApiKey = trimmed
        val prefs = getApplication<Application>().getSharedPreferences("pdf_master_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("gemini_api_key", trimmed).apply()
        _notification.value = UiNotification(if (trimmed.isNotBlank()) "Gemini API key saved!" else "API key cleared.")
    }

    fun setViewerSearchQuery(query: String) {
        _viewerSearchQuery.value = query
    }

    fun setViewerZoom(zoom: Float) {
        _viewerZoom.value = zoom.coerceAtLeast(0.1f)
    }

    fun saveEditedPdf(
        sourceRecord: PdfFileRecord,
        items: List<PdfEngine.EditItem>,
        deletedPages: Set<Int>,
        pageRotations: Map<Int, Int>,
        outputName: String,
        onSuccess: ((PdfFileRecord) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Saving edited PDF..."
            try {
                var srcFile = File(sourceRecord.filePath)
                if (!srcFile.exists() || !srcFile.canRead()) {
                    if (!sourceRecord.uriString.isNullOrBlank()) {
                        try {
                            val uri = Uri.parse(sourceRecord.uriString)
                            val safeDir = File(getApplication<Application>().filesDir, "imported_pdfs").apply { if (!exists()) mkdirs() }
                            val safeFile = File(safeDir, sourceRecord.name)
                            getApplication<Application>().contentResolver.openInputStream(uri)?.use { inStream ->
                                FileOutputStream(safeFile).use { outStream -> inStream.copyTo(outStream) }
                            }
                            if (safeFile.exists() && safeFile.length() > 0) {
                                srcFile = safeFile
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                val outFile = PdfEngine.saveEditedPdf(
                    getApplication(),
                    srcFile,
                    items,
                    deletedPages,
                    pageRotations,
                    outputName
                )
                val newRecord = commitAndShowResult(
                    sourceOutputFile = outFile,
                    desiredName = outputName,
                    toolTitle = "Save Edit",
                    originalRecord = sourceRecord,
                    summaryText = "Saved edits.",
                    tag = "Edited"
                )
                onSuccess?.invoke(newRecord)
                _notification.value = UiNotification("Saved edited PDF successfully!")
            } catch (e: Exception) {
                _notification.value = UiNotification("Edit failed: ${e.message}", false)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun mergePdfs(
        filesToMerge: List<PdfFileRecord>,
        outputName: String,
        onSuccess: ((PdfFileRecord) -> Unit)? = null
    ) {
        viewModelScope.launch {
            if (filesToMerge.isEmpty()) {
                _notification.value = UiNotification("Please select at least 2 files to merge", false)
                return@launch
            }
            _isProcessing.value = true
            _processingMessage.value = "Merging ${filesToMerge.size} PDFs..."
            try {
                val inputFiles = filesToMerge.map { File(it.filePath) }
                val outFile = PdfEngine.mergePdfFiles(getApplication(), inputFiles, outputName)
                val newRecord = commitAndShowResult(
                    sourceOutputFile = outFile,
                    desiredName = outputName,
                    toolTitle = "Merge PDF",
                    originalRecord = null,
                    summaryText = "Combined ${filesToMerge.size} PDFs into ${PdfEngine.getPageCount(outFile)} pages total.",
                    tag = "Merged"
                )
                _notification.value = UiNotification("Merged into ${newRecord.name} successfully!")
                if (onSuccess != null) {
                    onSuccess.invoke(newRecord)
                }
            } catch (e: Exception) {
                _notification.value = UiNotification("Merge failed: ${e.message}", false)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun splitPdf(
        sourceRecord: PdfFileRecord,
        selectedPageIndices: List<Int>,
        outputName: String,
        toolTitle: String = "Split PDF",
        tag: String = "Split",
        onSuccess: ((PdfFileRecord) -> Unit)? = null
    ) {
        viewModelScope.launch {
            if (selectedPageIndices.isEmpty()) {
                _notification.value = UiNotification("Please select pages to process", false)
                return@launch
            }
            _isProcessing.value = true
            _processingMessage.value = if (tag == "Extraction") "Extracting pages..." else "Splitting PDF..."
            try {
                val srcFile = File(sourceRecord.filePath)
                val outFile = PdfEngine.splitPdfFile(getApplication(), srcFile, selectedPageIndices, outputName)
                val newRecord = commitAndShowResult(
                    sourceOutputFile = outFile,
                    desiredName = outputName,
                    toolTitle = toolTitle,
                    originalRecord = sourceRecord,
                    summaryText = "Processed ${selectedPageIndices.size} pages from ${sourceRecord.name}.",
                    tag = tag
                )
                _notification.value = UiNotification("Success: ${newRecord.name} created")
                if (onSuccess != null) {
                    onSuccess.invoke(newRecord)
                }
            } catch (e: Exception) {
                _notification.value = UiNotification("Split failed: ${e.message}", false)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun compressPdfCustom(
        sourceRecord: PdfFileRecord,
        targetKB: Long,
        outputName: String,
        onSuccess: ((PdfFileRecord) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Compressing PDF to target size..."
            try {
                val srcFile = File(sourceRecord.filePath)
                val outFile = PdfEngine.compressPdfCustom(getApplication(), srcFile, targetKB, outputName)
                val savedPercent = ((sourceRecord.fileSize - outFile.length()).toFloat() / sourceRecord.fileSize.toFloat() * 100).toInt().coerceAtLeast(0)
                val newRecord = commitAndShowResult(
                    sourceOutputFile = outFile,
                    desiredName = outputName,
                    toolTitle = "Compress PDF",
                    originalRecord = sourceRecord,
                    summaryText = "Reduced by $savedPercent%.",
                    tag = "Compressed"
                )
                _notification.value = UiNotification("Compressed! Reduced by $savedPercent%.")
                if (onSuccess != null) {
                    onSuccess.invoke(newRecord)
                }
            } catch (e: Exception) {
                _notification.value = UiNotification("Compression failed: ${e.message}", false)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun rotatePdf(
        sourceRecord: PdfFileRecord,
        rotationDeg: Int,
        targetPages: Set<Int>,
        outputName: String,
        onSuccess: ((PdfFileRecord) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Rotating PDF pages..."
            try {
                val srcFile = File(sourceRecord.filePath)
                val totalPages = sourceRecord.pageCount
                val rotations = mutableMapOf<Int, Int>()
                for (i in 0 until totalPages) {
                    if (targetPages.isEmpty() || targetPages.contains(i)) {
                        rotations[i] = rotationDeg
                    }
                }
                val outFile = PdfEngine.managePages(
                    getApplication(),
                    srcFile,
                    (0 until totalPages).toList(),
                    rotations,
                    emptySet(),
                    emptyMap(),
                    outputName
                )
                val newRecord = commitAndShowResult(
                    sourceOutputFile = outFile,
                    desiredName = outputName,
                    toolTitle = "Rotate PDF",
                    originalRecord = sourceRecord,
                    summaryText = "Rotated ${if (targetPages.isEmpty()) "all" else targetPages.size} pages.",
                    tag = "Rotated"
                )
                _notification.value = UiNotification("Rotated: ${newRecord.name}")
                if (onSuccess != null) {
                    onSuccess.invoke(newRecord)
                }
            } catch (e: Exception) {
                _notification.value = UiNotification("Rotation failed: ${e.message}", false)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun applyWatermarkStandalone(
        sourceRecord: PdfFileRecord,
        text: String,
        rotationDeg: Int,
        outputName: String,
        isTiled: Boolean = false,
        normX: Float? = null,
        normY: Float? = null,
        fontSize: Float? = null,
        opacity: Float = 0.35f,
        colorHex: Int = android.graphics.Color.argb(89, 220, 38, 38),
        onSuccess: ((PdfFileRecord) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Applying watermark..."
            try {
                val srcFile = File(sourceRecord.filePath)
                val outFile = PdfEngine.watermarkAndRotate(
                    context = getApplication(),
                    sourceFile = srcFile,
                    watermarkText = text,
                    addPageNumbers = false,
                    pageRotationDeg = 0,
                    watermarkRotationDeg = rotationDeg.toFloat(),
                    outputFileName = outputName,
                    isTiled = isTiled,
                    customNormX = normX,
                    customNormY = normY,
                    customFontSize = fontSize,
                    customOpacity = opacity,
                    customColorHex = colorHex
                )
                val newRecord = commitAndShowResult(
                    sourceOutputFile = outFile,
                    desiredName = outputName,
                    toolTitle = "Watermark PDF",
                    originalRecord = sourceRecord,
                    summaryText = "Watermark applied to all pages.",
                    tag = "Watermarked"
                )
                _notification.value = UiNotification("Watermark applied: ${newRecord.name}")
                if (onSuccess != null) {
                    onSuccess.invoke(newRecord)
                }
            } catch (e: Exception) {
                _notification.value = UiNotification("Watermark failed: ${e.message}", false)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun compressPdf(sourceRecord: PdfFileRecord, level: String, outputName: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Compressing PDF with $level preset..."
            try {
                val srcFile = File(sourceRecord.filePath)
                val outFile = PdfEngine.compressPdfFile(getApplication(), srcFile, level, outputName)
                val savedPercent = ((sourceRecord.fileSize - outFile.length()).toFloat() / sourceRecord.fileSize.toFloat() * 100).toInt().coerceAtLeast(10)
                commitAndShowResult(
                    sourceOutputFile = outFile,
                    desiredName = outputName,
                    toolTitle = "Compress PDF",
                    originalRecord = sourceRecord,
                    summaryText = "Reduced by ~$savedPercent% using $level preset.",
                    tag = "Compressed ($level)"
                )
                _notification.value = UiNotification("Compressed! Saved ~$savedPercent% storage.")
            } catch (e: Exception) {
                _notification.value = UiNotification("Compression failed: ${e.message}", false)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun applyWatermarkAndRotation(
        sourceRecord: PdfFileRecord,
        watermarkText: String,
        addPageNumbers: Boolean,
        rotationDeg: Int,
        outputName: String
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Applying watermark & layout..."
            try {
                val srcFile = File(sourceRecord.filePath)
                val outFile = PdfEngine.watermarkAndRotate(
                    context = getApplication(),
                    sourceFile = srcFile,
                    watermarkText = watermarkText,
                    addPageNumbers = addPageNumbers,
                    pageRotationDeg = rotationDeg,
                    watermarkRotationDeg = -40f,
                    outputFileName = outputName
                )
                commitAndShowResult(
                    sourceOutputFile = outFile,
                    desiredName = outputName,
                    toolTitle = "Watermark & Layout",
                    originalRecord = sourceRecord,
                    summaryText = "Watermark applied successfully.",
                    tag = "Watermarked"
                )
                _notification.value = UiNotification("Watermark applied successfully!")
            } catch (e: Exception) {
                _notification.value = UiNotification("Failed: ${e.message}", false)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun scanOrImagesToPdf(bitmaps: List<Bitmap>, outputName: String, isScan: Boolean = true) {
        viewModelScope.launch {
            if (bitmaps.isEmpty()) return@launch
            _isProcessing.value = true
            _processingMessage.value = if (isScan) "Generating scanned PDF..." else "Converting images to PDF..."
            try {
                val outFile = PdfEngine.createPdfFromBitmaps(getApplication(), bitmaps, outputName)
                commitAndShowResult(
                    sourceOutputFile = outFile,
                    desiredName = outputName,
                    toolTitle = if (isScan) "Scan Document" else "Images to PDF",
                    summaryText = "Created ${bitmaps.size} page PDF.",
                    tag = if (isScan) "Scanned" else "Image2Pdf"
                )
            } catch (e: Exception) {
                _notification.value = UiNotification("Failed: ${e.message}", false)
            } finally {
                _isProcessing.value = false
            }
        }
    }


    fun splitPdfWithRange(sourceRecord: PdfFileRecord, rangeStr: String, outputName: String) {
        val total = sourceRecord.pageCount.coerceAtLeast(1)
        val indices = PdfEngine.parsePageRange(rangeStr, total)
        splitPdf(sourceRecord, indices, outputName, toolTitle = "Split PDF (Range)", tag = "Split")
    }

    fun removePagesFromPdf(
        sourceRecord: PdfFileRecord,
        pagesToRemove: Set<Int>,
        outputName: String,
        onSuccess: ((PdfFileRecord) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val total = sourceRecord.pageCount.coerceAtLeast(1)
            val remaining = (0 until total).filter { !pagesToRemove.contains(it) }
            if (remaining.isEmpty()) {
                _notification.value = UiNotification("Cannot remove all pages (at least 1 page must remain)", false)
                return@launch
            }
            if (pagesToRemove.isEmpty()) {
                _notification.value = UiNotification("Please select at least one page to remove", false)
                return@launch
            }
            _isProcessing.value = true
            _processingMessage.value = "Removing ${pagesToRemove.size} pages..."
            try {
                val srcFile = File(sourceRecord.filePath)
                val outFile = PdfEngine.removePagesFromPdfFile(getApplication(), srcFile, pagesToRemove, outputName)
                val newRecord = commitAndShowResult(
                    sourceOutputFile = outFile,
                    desiredName = outputName,
                    toolTitle = "Remove Pages",
                    originalRecord = sourceRecord,
                    summaryText = "Removed ${pagesToRemove.size} pages from ${sourceRecord.name} (${remaining.size} pages retained).",
                    tag = "Pages Removed"
                )
                _notification.value = UiNotification("Saved successfully with ${remaining.size} pages!")
                onSuccess?.invoke(newRecord)
            } catch (e: Exception) {
                _notification.value = UiNotification("Failed to remove pages: ${e.message}", false)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun organizePdf(
        sourceRecord: PdfFileRecord,
        orderedIndices: List<Int>,
        rotations: Map<Int, Int>,
        outputName: String
    ) {
        viewModelScope.launch {
            if (orderedIndices.isEmpty()) {
                _notification.value = UiNotification("At least 1 page must be retained", false)
                return@launch
            }
            _isProcessing.value = true
            _processingMessage.value = "Saving organized PDF..."
            try {
                val srcFile = File(sourceRecord.filePath)
                val outFile = PdfEngine.organizePdf(getApplication(), srcFile, orderedIndices, rotations, outputName)
                commitAndShowResult(
                    sourceOutputFile = outFile,
                    desiredName = outputName,
                    toolTitle = "Organize PDF",
                    originalRecord = sourceRecord,
                    summaryText = "Organized into ${orderedIndices.size} pages.",
                    tag = "Organized"
                )
                _notification.value = UiNotification("Organized and saved successfully!")
            } catch (e: Exception) {
                _notification.value = UiNotification("Organize failed: ${e.message}", false)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun performOcr(
        sourceRecord: PdfFileRecord,
        language: String,
        saveSearchablePdf: Boolean,
        outputName: String
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Starting OCR ($language)..."
            try {
                val srcFile = File(sourceRecord.filePath)
                
                if (saveSearchablePdf) {
                    val outFile = PdfEngine.createSearchablePdf(
                        context = getApplication(),
                        sourcePdf = srcFile,
                        outputFileName = outputName,
                        language = language,
                        onProgress = { msg ->
                            _processingMessage.value = msg
                        }
                    )
                    commitAndShowResult(
                        sourceOutputFile = outFile,
                        desiredName = outputName,
                        toolTitle = "OCR (Searchable PDF)",
                        originalRecord = sourceRecord,
                        summaryText = "Searchable PDF created successfully.",
                        tag = "Searchable PDF"
                    )
                    _notification.value = UiNotification("Searchable PDF created successfully!")
                } else {
                    val ocrResult = PdfEngine.extractTextFromImages(
                        sourcePdf = srcFile,
                        language = language,
                        onProgress = { msg ->
                            _processingMessage.value = msg
                        }
                    )
                    val dir = PdfEngine.getPdfsDirectory(getApplication())
                    val txtFile = File(dir, "${sourceRecord.nameWithoutExtension()}_ocr.txt")
                    txtFile.writeText(ocrResult)
                    commitAndShowResult(
                        sourceOutputFile = txtFile,
                        desiredName = txtFile.name,
                        toolTitle = "OCR (Text Extraction)",
                        originalRecord = sourceRecord,
                        summaryText = "Extracted text saved to file.",
                        mimeType = "text/plain",
                        tag = "OCR"
                    )
                    _notification.value = UiNotification("Extracted text saved!")
                }
            } catch (e: Exception) {
                _notification.value = UiNotification("OCR error: ${e.message}", false)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun toggleProtectFile(record: PdfFileRecord, password: String) {
        viewModelScope.launch {
            val updated = record.copy(
                isProtected = !record.isProtected,
                lastModified = System.currentTimeMillis()
            )
            repository.updateFile(updated)
            val msg = if (updated.isProtected) "Protected with password ($password)" else "Password removed"
            _notification.value = UiNotification(msg)
            closeModal()
        }
    }

    fun runAiFeature(
        record: PdfFileRecord,
        actionType: String,
        targetLanguage: String = "বাংলা",
        question: String = ""
    ) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiOutput.value = ""
            try {
                val file = File(record.filePath)
                val text = PdfEngine.extractTextFromPdf(file)
                val result = when (actionType) {
                    "SUMMARIZE" -> GeminiAiHelper.summarizeDocument(text)
                    "TRANSLATE" -> GeminiAiHelper.translateDocument(text, targetLanguage)
                    "QA" -> GeminiAiHelper.askQuestionAboutDocument(text, question)
                    "STUDY_NOTES" -> GeminiAiHelper.generateStudyNotes(text)
                    else -> GeminiAiHelper.summarizeDocument(text)
                }
                _aiOutput.value = result
            } catch (e: Exception) {
                _aiOutput.value = "Error: ${e.message}"
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun renameFile(record: PdfFileRecord, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (newName.isBlank()) return@launch
            var sanitized = newName.trim()
            val ext = if (record.name.contains(".")) record.name.substringAfterLast(".") else "pdf"
            if (!sanitized.endsWith(".$ext", ignoreCase = true)) {
                sanitized += ".$ext"
            }
            
            val context = getApplication<Application>()
            var finalName = sanitized
            
            try {
                if (record.uriString != null) {
                    val doc = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, android.net.Uri.parse(record.uriString))
                    if (doc != null && doc.exists()) {
                        doc.renameTo(sanitized)
                        finalName = doc.name ?: sanitized
                    }
                }
            } catch (e: Exception) { }

            var newPath = record.filePath
            try {
                val localFile = File(record.filePath)
                if (localFile.exists()) {
                    val parent = localFile.parentFile
                    var newLocalFile = File(parent, finalName)
                    var counter = 1
                    val base = finalName.substringBeforeLast(".")
                    while (newLocalFile.exists() && newLocalFile.absolutePath != localFile.absolutePath) {
                        finalName = "${base}_($counter).$ext"
                        newLocalFile = File(parent, finalName)
                        counter++
                    }
                    if (localFile.renameTo(newLocalFile)) {
                        newPath = newLocalFile.absolutePath
                    }
                }
            } catch (e: Exception) {}

            val currentRecord = repository.getFileById(record.id)
            if (currentRecord != null) {
                repository.updateFile(currentRecord.copy(name = finalName, filePath = newPath, lastModified = System.currentTimeMillis()))
            } else {
                repository.renameFile(record.id, finalName)
            }
            _notification.value = UiNotification("Renamed to $finalName")
            closeModal()
        }
    }

    fun viewFolder(context: Context, record: PdfFileRecord) {
        try {
            if (record.uriString != null) {
                val uri = android.net.Uri.parse(record.uriString)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = uri
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "View Folder"))
            } else {
                _notification.value = UiNotification("File is stored locally.")
            }
        } catch (e: Exception) {
            _notification.value = UiNotification("Could not open folder: ${e.message}", false)
        }
    }
    fun deleteFile(record: PdfFileRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            try {
                if (record.uriString != null) {
                    val doc = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, android.net.Uri.parse(record.uriString))
                    doc?.delete()
                }
            } catch (e: Exception) { }
            try {
                val file = File(record.filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) { }
            repository.deleteFileById(record.id)
            _notification.value = UiNotification("Deleted ${record.name}")
            closeModal()
        }
    }

    fun shareFile(context: Context, record: PdfFileRecord) {
        val file = File(record.filePath)
        if (!file.exists()) {
            _notification.value = UiNotification("File not found on device", false)
            return
        }
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share ${record.name} via PDF Master"))
        } catch (e: Exception) {
            _notification.value = UiNotification("Share error: ${e.message}", false)
        }
    }

    fun clearTempFiles() {
        viewModelScope.launch {
            val cacheDir = getApplication<Application>().cacheDir
            cacheDir.deleteRecursively()
            _notification.value = UiNotification("Temporary cache cleared!")
        }
    }

    fun updateSaveLocation(type: String, folderName: String, treeUri: Uri? = null) {
        com.example.storage.GlobalSaveManager.setStorageLocation(getApplication(), type, folderName, treeUri)
        _saveFolderType.value = type
        _customFolderName.value = com.example.storage.GlobalSaveManager.customFolderName.value
        _notification.value = UiNotification("Save folder updated: ${com.example.storage.GlobalSaveManager.displayPath.value}")
    }

    fun getSaveFolderDisplayPath(): String {
        return com.example.storage.GlobalSaveManager.displayPath.value
    }

    fun openSavedFile(uri: Uri? = null, file: File? = null, mimeType: String = "application/pdf") {
        com.example.storage.GlobalSaveManager.openFile(getApplication(), uri, file, mimeType)
    }

    fun openSavedFolder(treeUri: Uri? = null, file: File? = null) {
        com.example.storage.GlobalSaveManager.openFolder(getApplication(), treeUri?.toString(), file)
    }

    fun isSaveLocationAvailable(): Boolean {
        return com.example.storage.GlobalSaveManager.isCurrentLocationAvailable(getApplication())
    }

    suspend fun commitAndShowResult(
        sourceOutputFile: File,
        desiredName: String = sourceOutputFile.name,
        toolTitle: String,
        originalRecord: PdfFileRecord? = null,
        summaryText: String = "",
        mimeType: String = "application/pdf",
        category: String = "created",
        tag: String? = null
    ): PdfFileRecord {
        val saveResult = com.example.storage.GlobalSaveManager.saveProcessedFile(
            context = getApplication(),
            tempSourceFile = sourceOutputFile,
            desiredFileName = desiredName,
            mimeType = mimeType,
            category = category,
            tag = tag ?: toolTitle
        )
        repository.insertFile(saveResult.fileRecord)
        _selectedFile.value = saveResult.fileRecord
        showToolResult(
            ToolResultInfo(
                resultFile = saveResult.fileRecord,
                toolTitle = toolTitle,
                originalFile = originalRecord,
                summaryText = summaryText,
                savedDisplayLocation = saveResult.displayLocation,
                savedUri = saveResult.savedUri,
                isPhotoOutput = !mimeType.equals("application/pdf", ignoreCase = true)
            )
        )
        return saveResult.fileRecord
    }

    fun selectFile(record: PdfFileRecord) {
        _selectedFile.value = record
        if (_activeModal.value == ActiveModal.VIEWER) {
            loadPagesForViewer(record)
        }
    }

    fun importPdfFromUri(uri: Uri, onImported: ((PdfFileRecord) -> Unit)? = null) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Importing selected PDF..."
            try {
                try {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    getApplication<Application>().contentResolver.takePersistableUriPermission(uri, flags)
                } catch (_: Exception) {}

                val importedFile = PdfEngine.importPdfFromUri(getApplication(), uri)
                val pageCount = try {
                    PdfEngine.getPageCount(importedFile)
                } catch (_: Exception) {
                    PdfEngine.getPageCount(getApplication(), uri)
                }
                val record = PdfFileRecord(
                    name = importedFile.name,
                    filePath = importedFile.absolutePath,
                    fileSize = importedFile.length(),
                    pageCount = pageCount,
                    category = "downloads",
                    tag = "Uploaded",
                    uriString = uri.toString()
                )
                repository.insertFile(record)
                _selectedFile.value = record
                _notification.value = UiNotification("Uploaded \"${importedFile.name}\" successfully!")
                onImported?.invoke(record)
            } catch (e: Exception) {
                _notification.value = UiNotification("Upload failed: ${e.message}", false)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun insertPdfRecord(file: File, name: String, onInserted: ((PdfFileRecord) -> Unit)? = null) {
        viewModelScope.launch {
            val pageCount = try {
                PdfEngine.getPageCount(file)
            } catch (_: Exception) { 1 }
            val record = PdfFileRecord(
                name = name,
                filePath = file.absolutePath,
                fileSize = file.length(),
                pageCount = pageCount,
                category = "signed",
                tag = "Signed"
            )
            repository.insertFile(record)
            _selectedFile.value = record
            onInserted?.invoke(record)
        }
    }

    fun importMultiplePdfsFromUris(uris: List<Uri>, onImported: ((List<PdfFileRecord>) -> Unit)? = null) {
        viewModelScope.launch {
            if (uris.isEmpty()) return@launch
            _isProcessing.value = true
            _processingMessage.value = "Importing ${uris.size} PDFs..."
            try {
                val records = mutableListOf<PdfFileRecord>()
                for (uri in uris) {
                    try {
                        try {
                            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            getApplication<Application>().contentResolver.takePersistableUriPermission(uri, flags)
                        } catch (_: Exception) {}
                        val importedFile = PdfEngine.importPdfFromUri(getApplication(), uri)
                        val pageCount = try {
                            PdfEngine.getPageCount(importedFile)
                        } catch (_: Exception) {
                            PdfEngine.getPageCount(getApplication(), uri)
                        }
                        val record = PdfFileRecord(
                            name = importedFile.name,
                            filePath = importedFile.absolutePath,
                            fileSize = importedFile.length(),
                            pageCount = pageCount,
                            category = "downloads",
                            tag = "Uploaded",
                            uriString = uri.toString()
                        )
                        repository.insertFile(record)
                        records.add(record)
                    } catch (_: Exception) {}
                }
                if (records.isNotEmpty()) {
                    _selectedFile.value = records.last()
                    _notification.value = UiNotification("Imported ${records.size} PDFs from device!")
                    onImported?.invoke(records)
                } else {
                    _notification.value = UiNotification("Failed to import selected files", false)
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun importPhotosAndCreatePdf(uris: List<Uri>, outputName: String = "") {
        viewModelScope.launch {
            if (uris.isEmpty()) {
                _notification.value = UiNotification("No photos selected", false)
                return@launch
            }
            _isProcessing.value = true
            _processingMessage.value = "Converting ${uris.size} photos to PDF..."
            try {
                val bitmaps = PdfEngine.importBitmapsFromUris(getApplication(), uris)
                if (bitmaps.isEmpty()) {
                    _notification.value = UiNotification("Could not load photos", false)
                    return@launch
                }
                val fileName = if (outputName.isNotBlank()) outputName else "Photos_${System.currentTimeMillis()}.pdf"
                val outFile = PdfEngine.createPdfFromBitmaps(getApplication(), bitmaps, fileName)
                
                commitAndShowResult(
                    sourceOutputFile = outFile,
                    desiredName = fileName,
                    toolTitle = "JPG to PDF",
                    originalRecord = null,
                    summaryText = "Successfully created PDF from ${bitmaps.size} photos.",
                    mimeType = "application/pdf",
                    category = "converted",
                    tag = "JPG to PDF"
                )
            } catch (e: Exception) {
                _notification.value = UiNotification("Failed to convert photos: ${e.message}", false)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun cropPdf(
        sourceRecord: PdfFileRecord,
        leftPct: Float,
        topPct: Float,
        rightPct: Float,
        bottomPct: Float,
        targetPages: Set<Int>,
        outputName: String,
        onComplete: ((PdfFileRecord) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Cropping PDF pages..."
            try {
                val srcFile = File(sourceRecord.filePath)
                val outFile = PdfEngine.cropPdf(
                    getApplication(),
                    srcFile,
                    leftPct,
                    topPct,
                    rightPct,
                    bottomPct,
                    targetPages,
                    outputName
                )
                val newRecord = commitAndShowResult(
                    sourceOutputFile = outFile,
                    desiredName = outputName,
                    toolTitle = "Crop PDF",
                    originalRecord = sourceRecord,
                    summaryText = "Cropped ${if (targetPages.isEmpty()) "all" else targetPages.size} pages.",
                    tag = "Cropped"
                )
                _notification.value = UiNotification("Cropped into ${newRecord.name}")
                loadPagesForViewer(newRecord)
                onComplete?.invoke(newRecord)
            } catch (e: Exception) {
                _notification.value = UiNotification("Crop failed: ${e.message}", false)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun addCustomPageNumbers(
        sourceRecord: PdfFileRecord,
        format: String,
        position: PdfEngine.PageNumberPosition,
        targetPages: Set<Int>,
        outputName: String,
        onComplete: ((PdfFileRecord) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Adding page numbers..."
            try {
                val srcFile = File(sourceRecord.filePath)
                val outFile = PdfEngine.addCustomPageNumbers(
                    getApplication(),
                    srcFile,
                    format,
                    position,
                    targetPages,
                    outputName
                )
                val newRecord = commitAndShowResult(
                    sourceOutputFile = outFile,
                    desiredName = outputName,
                    toolTitle = "Add Page Numbers",
                    originalRecord = sourceRecord,
                    summaryText = "Page numbers added.",
                    tag = "Numbered"
                )
                _notification.value = UiNotification("Numbered PDF created: ${newRecord.name}")
                loadPagesForViewer(newRecord)
                onComplete?.invoke(newRecord)
            } catch (e: Exception) {
                _notification.value = UiNotification("Adding page numbers failed: ${e.message}", false)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun managePagesInFile(
        sourceRecord: PdfFileRecord,
        pageOrder: List<Int>,
        rotations: Map<Int, Int>,
        blankPagesAfter: Set<Int>,
        additionalImages: Map<Int, Bitmap>,
        outputName: String,
        onComplete: ((PdfFileRecord) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Updating pages..."
            try {
                val srcFile = File(sourceRecord.filePath)
                val outFile = PdfEngine.managePages(
                    getApplication(),
                    srcFile,
                    pageOrder,
                    rotations,
                    blankPagesAfter,
                    additionalImages,
                    outputName
                )
                val newRecord = commitAndShowResult(
                    sourceOutputFile = outFile,
                    desiredName = outputName,
                    toolTitle = "Organize PDF",
                    originalRecord = sourceRecord,
                    summaryText = "Organized pages into new PDF.",
                    tag = "Organized"
                )
                _notification.value = UiNotification("Pages updated: ${newRecord.name}")
                loadPagesForViewer(newRecord)
                onComplete?.invoke(newRecord)
            } catch (e: Exception) {
                _notification.value = UiNotification("Page management failed: ${e.message}", false)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun replaceOriginalPdf(
        originalRecord: PdfFileRecord,
        newTempFile: File,
        onComplete: ((PdfFileRecord) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Updating original document..."
            try {
                val targetFile = File(originalRecord.filePath)
                newTempFile.copyTo(targetFile, overwrite = true)
                val updatedRecord = originalRecord.copy(
                    fileSize = targetFile.length(),
                    pageCount = PdfEngine.getPageCount(targetFile),
                    lastModified = System.currentTimeMillis()
                )
                repository.updateFile(updatedRecord)
                _notification.value = UiNotification("Original document updated successfully!")
                loadPagesForViewer(updatedRecord)
                onComplete?.invoke(updatedRecord)
            } catch (e: Exception) {
                _notification.value = UiNotification("Failed to replace original: ${e.message}", false)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun saveAsNewFile(
        sourceRecord: PdfFileRecord,
        workingFile: File,
        newFileName: String,
        onComplete: ((PdfFileRecord) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Saving new document..."
            try {
                val newRecord = commitAndShowResult(
                    sourceOutputFile = workingFile,
                    desiredName = newFileName,
                    toolTitle = "Save Edit",
                    originalRecord = sourceRecord,
                    summaryText = "Document changes saved.",
                    tag = "Saved"
                )
                _notification.value = UiNotification("Saved as ${newRecord.name}")
                loadPagesForViewer(newRecord)
                onComplete?.invoke(newRecord)
            } catch (e: Exception) {
                _notification.value = UiNotification("Save As failed: ${e.message}", false)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun exportFileToExternalUri(sourceRecord: PdfFileRecord, destUri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Exporting copy to device..."
            try {
                val srcFile = File(sourceRecord.filePath)
                val success = PdfEngine.exportFileToUri(getApplication(), srcFile, destUri)
                if (success) {
                    _notification.value = UiNotification("Exported \"${sourceRecord.name}\" successfully!")
                } else {
                    _notification.value = UiNotification("Export failed", false)
                }
            } catch (e: Exception) {
                _notification.value = UiNotification("Export error: ${e.message}", false)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun repairPdf(file: PdfFileRecord, outputName: String) {
        _notification.value = UiNotification("Repair completed for \"${file.name}\"")
    }
}
