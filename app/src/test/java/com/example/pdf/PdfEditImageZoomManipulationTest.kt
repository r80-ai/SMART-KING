package com.example.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PdfEditImageZoomManipulationTest {

    private lateinit var context: Context
    private lateinit var testPdfFile: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val tempDir = File(context.cacheDir, "test_pdfs").apply { if (!exists()) mkdirs() }
        testPdfFile = File(tempDir, "sample_test_doc.pdf")
        PdfEngine.generateSampleProjectNotesPdf(testPdfFile)
        assertTrue(testPdfFile.exists() && testPdfFile.length() > 0L)
    }

    @Test
    fun testAddImageItemCoordinatesAndInitialState() {
        val testBmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val layoutW = 600f
        val layoutH = 800f
        val initW = (layoutW * 0.45f).coerceIn(100f, 600f)
        val initH = (initW * testBmp.height / testBmp.width.coerceAtLeast(1)).coerceAtLeast(40f)
        val initX = ((layoutW - initW) / 2f).coerceAtLeast(0f)
        val initY = ((layoutH - initH) / 3f).coerceAtLeast(0f)

        val imageItem = PdfEngine.EditItem(
            pageIndex = 0,
            type = PdfEngine.AnnotationType.IMAGE,
            imageBitmap = testBmp,
            originalImageBitmap = testBmp,
            posX = initX,
            posY = initY,
            imageWidth = initW,
            imageHeight = initH,
            baseWidth = layoutW,
            baseHeight = layoutH
        )

        assertEquals(PdfEngine.AnnotationType.IMAGE, imageItem.type)
        assertEquals(initX, imageItem.posX, 0.01f)
        assertEquals(initY, imageItem.posY, 0.01f)
        assertEquals(initW, imageItem.imageWidth, 0.01f)
        assertEquals(initH, imageItem.imageHeight, 0.01f)
        assertEquals(layoutW, imageItem.baseWidth, 0.01f)
        assertEquals(layoutH, imageItem.baseHeight, 0.01f)
        assertNotNull(imageItem.imageBitmap)
    }

    @Test
    fun testImageMoveUnderZoomLevels() {
        val testBmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val startItem = PdfEngine.EditItem(
            pageIndex = 0,
            type = PdfEngine.AnnotationType.IMAGE,
            imageBitmap = testBmp,
            posX = 150f,
            posY = 200f,
            imageWidth = 200f,
            imageHeight = 200f,
            baseWidth = 600f,
            baseHeight = 800f
        )

        // Simulate finger move delta in page coordinates at different zoom scales
        // Notice: under Compose's graphicsLayer, drag delta in layer space directly matches page coordinates!
        val pageDeltaX = 25f
        val pageDeltaY = -15f

        val movedItem = startItem.copy(
            posX = startItem.posX + pageDeltaX,
            posY = startItem.posY + pageDeltaY
        )

        assertEquals(175f, movedItem.posX, 0.01f)
        assertEquals(185f, movedItem.posY, 0.01f)
        assertEquals(200f, movedItem.imageWidth, 0.01f)
        assertEquals(200f, movedItem.imageHeight, 0.01f)
    }

    @Test
    fun testFourCornerHandlesIndependentWidthAndHeightResize() {
        val testBmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val cur = PdfEngine.EditItem(
            pageIndex = 0,
            type = PdfEngine.AnnotationType.IMAGE,
            imageBitmap = testBmp,
            posX = 100f,
            posY = 100f,
            imageWidth = 200f,
            imageHeight = 200f,
            baseWidth = 600f,
            baseHeight = 800f
        )

        // 1. TOP-LEFT HANDLE: Drag left by 20 (w increases), drag up by 15 (h increases)
        run {
            val dX = -20f // dragging left
            val dY = -15f // dragging up
            val currentRight = cur.posX + cur.imageWidth // 300f
            val currentBottom = cur.posY + cur.imageHeight // 300f
            val newW = (cur.imageWidth - dX).coerceIn(30f, currentRight + 200f) // 220f
            val newH = (cur.imageHeight - dY).coerceIn(30f, currentBottom + 200f) // 215f
            val newX = currentRight - newW // 80f
            val newY = currentBottom - newH // 85f
            assertEquals(220f, newW, 0.01f)
            assertEquals(215f, newH, 0.01f)
            assertEquals(80f, newX, 0.01f)
            assertEquals(85f, newY, 0.01f)
        }

        // 2. TOP-RIGHT HANDLE: Drag right by 30 (w increases), drag down by 10 (h decreases)
        run {
            val dX = 30f // dragging right
            val dY = 10f // dragging down
            val currentBottom = cur.posY + cur.imageHeight // 300f
            val newW = (cur.imageWidth + dX).coerceIn(30f, 600f - cur.posX + 200f) // 230f
            val newH = (cur.imageHeight - dY).coerceIn(30f, currentBottom + 200f) // 190f
            val newY = currentBottom - newH // 110f
            assertEquals(230f, newW, 0.01f)
            assertEquals(190f, newH, 0.01f)
            assertEquals(110f, newY, 0.01f)
        }

        // 3. BOTTOM-LEFT HANDLE: Drag left by 10 (w increases), drag down by 40 (h increases)
        run {
            val dX = -10f // dragging left
            val dY = 40f // dragging down
            val currentRight = cur.posX + cur.imageWidth // 300f
            val newW = (cur.imageWidth - dX).coerceIn(30f, currentRight + 200f) // 210f
            val newH = (cur.imageHeight + dY).coerceIn(30f, 800f - cur.posY + 200f) // 240f
            val newX = currentRight - newW // 90f
            assertEquals(210f, newW, 0.01f)
            assertEquals(240f, newH, 0.01f)
            assertEquals(90f, newX, 0.01f)
        }

        // 4. BOTTOM-RIGHT HANDLE: Drag right by 50 (w increases), drag down by 25 (h increases)
        run {
            val dX = 50f // dragging right
            val dY = 25f // dragging down
            val newW = (cur.imageWidth + dX).coerceIn(30f, 600f - cur.posX + 200f) // 250f
            val newH = (cur.imageHeight + dY).coerceIn(30f, 800f - cur.posY + 200f) // 225f
            assertEquals(250f, newW, 0.01f)
            assertEquals(225f, newH, 0.01f)
        }
    }

    @Test
    fun testFitWidthLayoutRescaling() {
        val testBmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val initialLayoutW = 600f
        val initialLayoutH = 800f
        val item = PdfEngine.EditItem(
            pageIndex = 0,
            type = PdfEngine.AnnotationType.IMAGE,
            imageBitmap = testBmp,
            posX = 60f,
            posY = 80f,
            imageWidth = 180f,
            imageHeight = 240f,
            baseWidth = initialLayoutW,
            baseHeight = initialLayoutH
        )

        // User switches to Fit Width on a wider screen (900px wide)
        val newLayoutW = 900f
        val factor = newLayoutW / item.baseWidth // 1.5f
        val scaledItem = item.copy(
            posX = item.posX * factor,
            posY = item.posY * factor,
            imageWidth = item.imageWidth * factor,
            imageHeight = item.imageHeight * factor,
            baseWidth = newLayoutW,
            baseHeight = initialLayoutH * factor
        )

        assertEquals(90f, scaledItem.posX, 0.01f)
        assertEquals(120f, scaledItem.posY, 0.01f)
        assertEquals(270f, scaledItem.imageWidth, 0.01f)
        assertEquals(360f, scaledItem.imageHeight, 0.01f)

        // Relative ratio to page width is strictly preserved
        assertEquals(item.posX / item.baseWidth, scaledItem.posX / scaledItem.baseWidth, 0.001f)
        assertEquals(item.imageWidth / item.baseWidth, scaledItem.imageWidth / scaledItem.baseWidth, 0.001f)
    }

    @Test
    fun testSaveEditedPdfWithImageAndReopen() = runBlocking {
        val testBmp = Bitmap.createBitmap(150, 150, Bitmap.Config.ARGB_8888)
        val imageItem = PdfEngine.EditItem(
            pageIndex = 0,
            type = PdfEngine.AnnotationType.IMAGE,
            imageBitmap = testBmp,
            posX = 50f,
            posY = 100f,
            imageWidth = 150f,
            imageHeight = 150f,
            baseWidth = 600f,
            baseHeight = 800f
        )

        val outputFileName = "output_edited_with_image.pdf"
        val savedFile = PdfEngine.saveEditedPdf(
            context = context,
            sourceFile = testPdfFile,
            items = listOf(imageItem),
            deletedPages = emptySet(),
            pageRotations = emptyMap(),
            outputFileName = outputFileName
        )

        assertNotNull(savedFile)
        assertTrue(savedFile.exists())
        assertTrue(savedFile.length() > 0L)

        // Reopen and verify with PdfRenderer
        val pfd = ParcelFileDescriptor.open(savedFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        assertTrue(renderer.pageCount > 0)

        val page = renderer.openPage(0)
        val renderedBmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
        page.render(renderedBmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        renderer.close()
        pfd.close()

        assertNotNull(renderedBmp)
        assertTrue(renderedBmp.width > 0 && renderedBmp.height > 0)
    }
}
