package com.example.ui

import androidx.compose.runtime.Composable
import com.example.model.PdfFileRecord
import com.example.ui.screens.RemovePagesScreen

/**
 * Route identifiers and navigation endpoints for SMART KING tools.
 */
object PdfNavRoutes {
    const val ROUTE_HOME = "home"
    const val ROUTE_TOOLS = "tools"
    const val ROUTE_FILES = "files"
    const val ROUTE_SETTINGS = "settings"
    const val ROUTE_REMOVE_PAGES = "remove_pages"
    const val ROUTE_MERGE = "merge"
    const val ROUTE_SPLIT = "split"
    const val ROUTE_EXTRACT_PAGES = "extract_pages"
    const val ROUTE_COMPRESS = "compress"
    const val ROUTE_CROP = "crop"
    const val ROUTE_ROTATE = "rotate"
    const val ROUTE_WATERMARK = "watermark"
    const val ROUTE_PAGE_NUMBERS = "page_numbers"
    const val ROUTE_PDF_TO_JPG = "pdf_to_jpg"
    const val ROUTE_EDIT_PDF = "edit_pdf"
}

/**
 * Route composable wrapper for Remove Pages screen.
 */
@Composable
fun RemovePagesNavDestination(
    initialFile: PdfFileRecord? = null,
    viewModel: PdfMasterViewModel,
    onBack: () -> Unit
) {
    RemovePagesScreen(
        initialFile = initialFile,
        viewModel = viewModel,
        onBack = onBack
    )
}

/**
 * Helper to resolve tool route string to its ActiveModal destination.
 */
fun routeToActiveModal(route: String): ActiveModal? = when (route) {
    PdfNavRoutes.ROUTE_REMOVE_PAGES -> ActiveModal.REMOVE_PAGES
    PdfNavRoutes.ROUTE_MERGE -> ActiveModal.MERGE
    PdfNavRoutes.ROUTE_SPLIT -> ActiveModal.SPLIT
    PdfNavRoutes.ROUTE_EXTRACT_PAGES -> ActiveModal.EXTRACT_PAGES
    PdfNavRoutes.ROUTE_COMPRESS -> ActiveModal.COMPRESS
    PdfNavRoutes.ROUTE_CROP -> ActiveModal.CROP
    PdfNavRoutes.ROUTE_ROTATE -> ActiveModal.ROTATE
    PdfNavRoutes.ROUTE_WATERMARK -> ActiveModal.WATERMARK
    PdfNavRoutes.ROUTE_PAGE_NUMBERS -> ActiveModal.PAGE_NUMBERS
    PdfNavRoutes.ROUTE_PDF_TO_JPG -> ActiveModal.PDF_TO_JPG
    PdfNavRoutes.ROUTE_EDIT_PDF -> ActiveModal.EDIT
    else -> null
}
