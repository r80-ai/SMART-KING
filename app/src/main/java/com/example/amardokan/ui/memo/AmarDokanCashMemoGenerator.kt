package com.example.amardokan.ui.memo

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.AmarDokanApp
import com.example.amardokan.data.Bill
import com.example.amardokan.data.BillItem
import com.example.amardokan.ui.BanglaNumberUtil
import com.example.amardokan.ui.screens.DraftBillItem
import com.example.util.QrCodeGenerator
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * High-Resolution Authentic Bengali Cash Memo Generator.
 * Reproduces the exact Xerox & Cyber Cafe Cash Memo template with:
 * - Traditional Bengali Shop Banner & Graphics (টেলিকম সাইবার ক্যাফে ও জেরক্স)
 * - Exact Table Layout with Ruled Lines & Columns
 * - Automatic Field Placement (Bill No, Date, Customer, Items, Totals)
 * - Dynamic Scannable UPI QR Code (Merchant ID: 7718489@ybl)
 * - High-Quality PDF, PrintManager, and WhatsApp Sharing
 */
object AmarDokanCashMemoGenerator {

    const val MERCHANT_UPI_ID = "7718489@ybl"
    const val MERCHANT_SHOP_NAME = "Telecom Cyber Cafe"
    const val SHOP_PHONE = "7718489405"

    // Memo Dimensions (Exact 1024x1536 resolution, 2:3 aspect ratio)
    const val MEMO_WIDTH = 1024
    const val MEMO_HEIGHT = 1536

    /**
     * Data wrapper for rendering a Cash Memo.
     */
    data class MemoData(
        val billNumber: String,
        val dateFormatted: String,
        val customerName: String,
        val customerPhone: String = "",
        val paymentType: String = "CASH", // "CASH" or "CREDIT"
        val items: List<MemoItem>,
        val subtotal: Double,
        val discount: Double = 0.0,
        val grandTotal: Double,
        val notes: String = "",
        val language: String = "BN" // "BN" or "EN"
    ) {
        companion object {
            fun fromBill(bill: Bill, items: List<BillItem>): MemoData {
                val detectedLang = if (bill.language.isNotBlank()) {
                    bill.language
                } else if (bill.billNumber.startsWith("BILL-", ignoreCase = true)) {
                    "EN"
                } else {
                    "BN"
                }
                return MemoData(
                    billNumber = bill.billNumber,
                    dateFormatted = bill.dateFormatted,
                    customerName = bill.customerName,
                    customerPhone = bill.customerPhone,
                    paymentType = bill.paymentType,
                    items = items.map {
                        MemoItem(
                            serviceName = it.serviceName,
                            quantity = it.quantity,
                            unitRate = it.unitRate,
                            lineTotal = it.lineTotal
                        )
                    },
                    subtotal = bill.subtotal,
                    discount = bill.discount,
                    grandTotal = bill.grandTotal,
                    notes = bill.notes,
                    language = detectedLang
                )
            }

            fun fromDraft(
                billNumber: String,
                customerName: String,
                customerPhone: String,
                dateStr: String,
                items: List<DraftBillItem>,
                subtotal: Double,
                discount: Double,
                grandTotal: Double,
                paymentType: String,
                language: String = "BN"
            ): MemoData {
                return MemoData(
                    billNumber = billNumber,
                    dateFormatted = dateStr,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    paymentType = paymentType,
                    items = items.map {
                        MemoItem(
                            serviceName = it.serviceName,
                            quantity = it.quantity,
                            unitRate = it.unitRate,
                            lineTotal = it.lineTotal
                        )
                    },
                    subtotal = subtotal,
                    discount = discount,
                    grandTotal = grandTotal,
                    language = language
                )
            }
        }
    }

    data class MemoItem(
        val serviceName: String,
        val quantity: Double,
        val unitRate: Double,
        val lineTotal: Double
    )

    /**
     * Generates a high-resolution Bitmap of the cash memo using the selected template (Bengali or English).
     */
    fun generateMemoBitmap(data: MemoData, context: Context? = null): Bitmap {
        val appContext = context ?: try { AmarDokanApp.instance.applicationContext } catch (e: Exception) { null }
        return if (data.language.uppercase() == "EN") {
            generateEnglishMemoBitmap(data, appContext)
        } else {
            generateBengaliMemoBitmap(data, appContext)
        }
    }

    /**
     * Generates a high-resolution Bitmap of the complete Bengali Cash Memo using cash_memo_bengali.png.
     */
    fun generateBengaliMemoBitmap(data: MemoData, context: Context? = null): Bitmap {
        val appContext = context ?: try { AmarDokanApp.instance.applicationContext } catch (e: Exception) { null }
        return renderAuthenticCashMemo(data, isEn = false, context = appContext)
    }

    /**
     * Generates a high-resolution Bitmap of the complete English Cash Memo using cash_memo_english.png.
     */
    fun generateEnglishMemoBitmap(data: MemoData, context: Context? = null): Bitmap {
        val appContext = context ?: try { AmarDokanApp.instance.applicationContext } catch (e: Exception) { null }
        return renderAuthenticCashMemo(data, isEn = true, context = appContext)
    }

    /**
     * Master authentic cash memo renderer using the real background templates:
     * - app/src/main/assets/cash_memo_bengali.png
     * - app/src/main/assets/cash_memo_english.png
     *
     * Preserves the full template design, logo, tables, borders, and colors untouched,
     * only placing dynamic bill data into the designated blank fields.
     */
    fun MemoData.toBillModel(): BillModel {
        val isDue = !paymentType.equals("CASH", ignoreCase = true)
        val dueVal = if (isDue) grandTotal else 0.0
        val discRaw = if (discount % 1.0 == 0.0) discount.toInt() else discount
        val subRaw = if (subtotal % 1.0 == 0.0) subtotal.toInt() else subtotal
        val gtRaw = if (grandTotal % 1.0 == 0.0) grandTotal.toInt() else grandTotal
        val dueRaw = if (dueVal % 1.0 == 0.0) dueVal.toInt() else dueVal

        return BillModel(
            billNumber = billNumber,
            dateString = dateFormatted,
            customerName = customerName,
            customerMobile = customerPhone,
            items = items.map { item ->
                val qtyRaw = if (item.quantity % 1.0 == 0.0) item.quantity.toInt() else item.quantity
                val rateRaw = if (item.unitRate % 1.0 == 0.0) item.unitRate.toInt() else item.unitRate
                val totalRaw = if (item.lineTotal % 1.0 == 0.0) item.lineTotal.toInt() else item.lineTotal
                BillItemModel(
                    nameEnglish = item.serviceName,
                    nameBengali = item.serviceName,
                    quantity = qtyRaw,
                    rate = rateRaw,
                    total = totalRaw
                )
            },
            subTotal = subRaw,
            discountAmount = discount,
            grandTotal = gtRaw,
            dueAmount = dueRaw
        )
    }

    /**
     * Master authentic cash memo renderer using the real background templates:
     * - app/src/main/assets/cash_memo_bengali.png
     * - app/src/main/assets/cash_memo_english.png
     *
     * Preserves the full template design, logo, tables, borders, and colors untouched,
     * only placing dynamic bill data into the designated blank fields via drawBillContentOnCanvas.
     */
    private fun renderAuthenticCashMemo(data: MemoData, isEn: Boolean, context: Context?): Bitmap {
        val bitmap = loadTemplateBitmap(context, isEn)
        val canvas = Canvas(bitmap)
        val pageWidth = bitmap.width.toFloat()
        val pageHeight = bitmap.height.toFloat()

        // Generate dynamic UPI QR Code
        val qrAmount = String.format(Locale.ENGLISH, "%.2f", data.grandTotal)
        val billEncoded = Uri.encode(data.billNumber)
        val upiUri = "upi://pay?pa=$MERCHANT_UPI_ID&pn=Telecom%20Cyber%20Cafe&am=$qrAmount&cu=INR&tn=Bill%20$billEncoded"
        val qrSize = (pageWidth * 0.22f).toInt()
        val qrBitmap = if (data.grandTotal > 0.0) QrCodeGenerator.generateUpiQr(upiUri, qrSize) else null

        val billModel = data.toBillModel()
        drawBillContentOnCanvas(
            canvas = canvas,
            pageWidth = pageWidth,
            pageHeight = pageHeight,
            billData = billModel,
            qrBitmap = qrBitmap,
            isEnglish = isEn
        )

        // Payment status stamp (PAID / DUE)
        if (data.paymentType.equals("CASH", ignoreCase = true)) {
            val stampText = if (isEn) "PAID" else "পরিশোধিত"
            drawStamp(canvas, stampText, Color.rgb(22, 163, 74), pageWidth * 0.38f, pageHeight * 0.915f, -8f)
        } else {
            val stampText = if (isEn) "DUE / CREDIT" else "বকেয়া / বাকি"
            drawStamp(canvas, stampText, Color.rgb(220, 38, 38), pageWidth * 0.38f, pageHeight * 0.915f, -8f)
        }

        return bitmap
    }

    fun generateMemoBitmap(billModel: BillModel, isEnglish: Boolean, context: Context? = null): Bitmap {
        val appContext = context ?: try { AmarDokanApp.instance.applicationContext } catch (e: Exception) { null }
        val bitmap = loadTemplateBitmap(appContext, isEnglish)
        val canvas = Canvas(bitmap)
        val pageWidth = bitmap.width.toFloat()
        val pageHeight = bitmap.height.toFloat()

        val qrAmount = billModel.grandTotal.toString()
        val billEncoded = Uri.encode(billModel.billNumber)
        val upiUri = "upi://pay?pa=$MERCHANT_UPI_ID&pn=Telecom%20Cyber%20Cafe&am=$qrAmount&cu=INR&tn=Bill%20$billEncoded"
        val qrSize = (pageWidth * 0.22f).toInt()
        val qrBitmap = QrCodeGenerator.generateUpiQr(upiUri, qrSize)

        drawBillContentOnCanvas(
            canvas = canvas,
            pageWidth = pageWidth,
            pageHeight = pageHeight,
            billData = billModel,
            qrBitmap = qrBitmap,
            isEnglish = isEnglish
        )
        return bitmap
    }

    private fun loadTemplateBitmap(context: Context?, isEn: Boolean): Bitmap {
        val assetName = if (isEn) "cash_memo_english.png" else "cash_memo_bengali.png"
        if (context != null) {
            try {
                context.assets.open(assetName).use { inputStream ->
                    val options = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                        inMutable = true
                    }
                    val decoded = BitmapFactory.decodeStream(inputStream, null, options)
                    if (decoded != null) {
                        return if (decoded.width != MEMO_WIDTH || decoded.height != MEMO_HEIGHT) {
                            Bitmap.createScaledBitmap(decoded, MEMO_WIDTH, MEMO_HEIGHT, true).copy(Bitmap.Config.ARGB_8888, true)
                        } else {
                            decoded
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CashMemoGenerator", "Failed to load template $assetName from assets", e)
            }
        }
        // Fallback: create empty white bitmap if context or asset unavailable
        val fallback = Bitmap.createBitmap(MEMO_WIDTH, MEMO_HEIGHT, Bitmap.Config.ARGB_8888)
        fallback.eraseColor(Color.WHITE)
        return fallback
    }

    private fun tableSlPad(colSl: Float): Float = colSl + 12f

    private fun truncateText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var truncated = text
        while (truncated.isNotEmpty() && paint.measureText("$truncated...") > maxWidth) {
            truncated = truncated.dropLast(1)
        }
        return "$truncated..."
    }

    private fun drawStamp(canvas: Canvas, text: String, color: Int, cx: Float, cy: Float, rotation: Float) {
        canvas.save()
        canvas.rotate(rotation, cx, cy)

        val stampPaint = Paint().apply {
            this.color = color
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }

        val textPaint = Paint().apply {
            this.color = color
            isAntiAlias = true
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val textW = textPaint.measureText(text)
        val stampRect = RectF(cx - textW / 2f - 12f, cy - 20f, cx + textW / 2f + 12f, cy + 12f)
        canvas.drawRoundRect(stampRect, 6f, 6f, stampPaint)
        canvas.drawText(text, cx, cy + 2f, textPaint)

        canvas.restore()
    }

    /**
     * Converts integer amount to Bengali words.
     */
    fun convertAmountToBanglaWords(amount: Long): String {
        if (amount == 0L) return "শূন্য টাকা মাত্র"

        val ones = arrayOf("", "এক", "দুই", "তিন", "চার", "পাঁচ", "ছয়", "সাত", "আট", "নয়", "দশ",
            "এগারো", "বারো", "তেরো", "চৌদ্দ", "পনেরো", "ষোল", "সতেরো", "আঠারো", "উনিশ")
        val tens = arrayOf("", "", "কুড়ি", "ত্রিশ", "চল্লিশ", "পঞ্চাশ", "ষাট", "সত্তর", "আশি", "নব্বই")

        fun convertLessThanThousand(n: Int): String {
            if (n == 0) return ""
            var result = ""
            val hundred = n / 100
            val rem = n % 100

            if (hundred > 0) {
                result += "${ones[hundred]} শত "
            }

            if (rem in 1..19) {
                result += ones[rem]
            } else if (rem >= 20) {
                val t = rem / 10
                val o = rem % 10
                result += tens[t] + (if (o > 0) " ${ones[o]}" else "")
            }
            return result.trim()
        }

        var num = amount
        var words = ""

        val crore = num / 10000000
        num %= 10000000

        val lakh = num / 100000
        num %= 100000

        val thousand = num / 1000
        num %= 1000

        if (crore > 0) words += "${convertLessThanThousand(crore.toInt())} কোটি "
        if (lakh > 0) words += "${convertLessThanThousand(lakh.toInt())} লক্ষ "
        if (thousand > 0) words += "${convertLessThanThousand(thousand.toInt())} হাজার "
        if (num > 0) words += convertLessThanThousand(num.toInt())

        return "${words.trim()} টাকা মাত্র"
    }

    /**
     * Converts integer amount to English words.
     */
    fun convertAmountToEnglishWords(amount: Long): String {
        if (amount == 0L) return "Zero Rupees Only"

        val ones = arrayOf("", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen")
        val tens = arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")

        fun convertLessThanThousand(n: Int): String {
            if (n == 0) return ""
            val hundred = n / 100
            val rem = n % 100
            var s = ""
            if (hundred > 0) s += "${ones[hundred]} Hundred "
            if (rem in 1..19) {
                s += ones[rem]
            } else if (rem >= 20) {
                val t = rem / 10
                val o = rem % 10
                s += tens[t] + (if (o > 0) " ${ones[o]}" else "")
            }
            return s.trim()
        }

        var num = amount
        var words = ""

        val crore = num / 10000000
        num %= 10000000

        val lakh = num / 100000
        num %= 100000

        val thousand = num / 1000
        num %= 1000

        if (crore > 0) words += "${convertLessThanThousand(crore.toInt())} Crore "
        if (lakh > 0) words += "${convertLessThanThousand(lakh.toInt())} Lakh "
        if (thousand > 0) words += "${convertLessThanThousand(thousand.toInt())} Thousand "
        if (num > 0) words += convertLessThanThousand(num.toInt())

        return "${words.trim()} Rupees Only"
    }

    /**
     * Generates a high-quality PDF file of the cash memo and writes it to a file.
     */
    fun generatePdfFile(context: Context, data: MemoData): File {
        val bitmap = generateMemoBitmap(data, context)

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(MEMO_WIDTH, MEMO_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        pdfDocument.finishPage(page)

        val sanitizedCustomerName = data.customerName.ifBlank { "Unknown" }.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val sanitizedBillNo = data.billNumber.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val fileName = "${sanitizedCustomerName}-${sanitizedBillNo}.pdf"
        val outputDir = File(context.cacheDir, "memos").apply { mkdirs() }
        val outputFile = File(outputDir, fileName)

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return outputFile
    }

    /**
     * Generates a JPEG file of the cash memo for image sharing.
     */
    fun generateImageFile(context: Context, data: MemoData): File {
        val bitmap = generateMemoBitmap(data, context)
        val sanitizedCustomerName = data.customerName.ifBlank { "Unknown" }.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val sanitizedBillNo = data.billNumber.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val fileName = "${sanitizedCustomerName}-${sanitizedBillNo}.jpg"
        val outputDir = File(context.cacheDir, "memos").apply { mkdirs() }
        val outputFile = File(outputDir, fileName)

        FileOutputStream(outputFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 96, out)
        }
        return outputFile
    }

    /**
     * Prints the Cash Memo directly using Android PrintManager.
     */
    fun printMemo(context: Context, data: MemoData) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "CashMemo_${data.billNumber}"

        printManager.print(
            jobName,
            object : PrintDocumentAdapter() {
                private var mPdfDocument: PdfDocument? = null

                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }

                    val info = PrintDocumentInfo.Builder("cash_memo_${data.billNumber}.pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onWriteCancelled()
                        return
                    }

                    val pdfDocument = PdfDocument()
                    val pageInfo = PdfDocument.PageInfo.Builder(MEMO_WIDTH, MEMO_HEIGHT, 1).create()
                    val page = pdfDocument.startPage(pageInfo)

                    val bitmap = generateMemoBitmap(data, context)
                    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    pdfDocument.finishPage(page)

                    try {
                        FileOutputStream(destination?.fileDescriptor).use {
                            pdfDocument.writeTo(it)
                        }
                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    } finally {
                        pdfDocument.close()
                    }
                }
            },
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build()
        )
    }

    /**
     * Shares the actual generated PDF cash memo file to WhatsApp or any compatible app.
     */
    fun shareOnWhatsAppOrApps(context: Context, data: MemoData, asPdf: Boolean = true) {
        try {
            val file = if (asPdf) generatePdfFile(context, data) else generateImageFile(context, data)
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val isEn = data.language.uppercase() == "EN"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (asPdf) "application/pdf" else "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    if (isEn) "Telecom Cyber Cafe - Cash Memo (${data.billNumber})" else "টেলিকম সাইবার ক্যাফে - ক্যাশ মেমো (${data.billNumber})"
                )
                putExtra(
                    Intent.EXTRA_TEXT,
                    if (isEn) {
                        "🧾 *TELECOM CYBER CAFE & XEROX*\n" +
                                "Bill No: ${data.billNumber}\n" +
                                "Customer: ${data.customerName}\n" +
                                "Date: ${data.dateFormatted}\n" +
                                "Total Bill: ₹${data.grandTotal.toInt()}\n" +
                                "Payment: ${if (data.paymentType == "CASH") "PAID (Cash)" else "DUE (Credit)"}\n" +
                                "Thank you! Visit again."
                    } else {
                        "🧾 *টেলিকম সাইবার ক্যাফে ও জেরক্স*\n" +
                                "বিল নং: ${data.billNumber}\n" +
                                "গ্রাহক: ${data.customerName}\n" +
                                "তারিখ: ${data.dateFormatted}\n" +
                                "সর্বমোট বিল: ₹${data.grandTotal.toInt()}\n" +
                                "পেমেন্ট: ${if (data.paymentType == "CASH") "পরিশোধিত" else "বকেয়া"}\n" +
                                "ধন্যবাদ! আবার আসবেন।"
                    }
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, if (isEn) "Share Cash Memo" else "ক্যাশ মেমো শেয়ার করুন")
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "শেয়ার করতে সমস্যা হয়েছে: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}

// ==========================================
// BILL DATA MODELS
// ==========================================

data class BillModel(
    val billNumber: String = "",
    val dateString: String = "",
    val customerName: String = "",
    val customerMobile: String = "",
    val items: List<BillItemModel> = emptyList(),
    val subTotal: Any = 0,
    val discountAmount: Double = 0.0,
    val grandTotal: Any = 0,
    val dueAmount: Any = 0
)

data class BillItemModel(
    val nameEnglish: String = "",
    val nameBengali: String = "",
    val quantity: Any = 0,
    val rate: Any = 0,
    val total: Any = 0
)

// ==========================================
// UPDATED CANVAS & PDF RENDERING LOGIC
// (Applies to both Bengali & English Bills)
// ==========================================

fun drawBillContentOnCanvas(
    canvas: Canvas,
    pageWidth: Float,
    pageHeight: Float,
    billData: BillModel, // Customer & billing data
    qrBitmap: Bitmap?,
    isEnglish: Boolean
) {
    val fontScale = if (pageWidth > 600f) (pageWidth / 595f) else 1f

    // 1. HIGH CONTRAST & BOLD PAINT SETUP
    val textPaint = Paint().apply {
        color = Color.BLACK
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    val valuePaint = Paint().apply {
        color = Color.BLACK
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    // 2. FIXED COORDINATES FOR HEADER & METADATA
    // Bill Number & Date
    valuePaint.textSize = 13f * fontScale
    val billNoX = pageWidth * 0.22f
    val billNoY = pageHeight * 0.205f
    val displayBillNo = billData.billNumber.ifEmpty { "1001" }
    canvas.drawText(displayBillNo, billNoX, billNoY, valuePaint)

    val dateX = pageWidth * 0.72f
    val dateY = pageHeight * 0.205f
    val displayDate = billData.dateString.ifEmpty { "13/10/2026" }
    canvas.drawText(displayDate, dateX, dateY, valuePaint)

    // Customer Name (Precisely after "গ্রাহকের নাম:" / "Customer Name:")
    valuePaint.textSize = 14f * fontScale
    val customerNameX = pageWidth * 0.28f
    val customerNameY = pageHeight * 0.235f
    val customerName = billData.customerName.ifEmpty { "Cash Customer" }
    canvas.drawText(customerName, customerNameX, customerNameY, valuePaint)

    // Customer Mobile Number
    if (billData.customerMobile.isNotEmpty()) {
        val mobileX = pageWidth * 0.72f
        val mobileY = pageHeight * 0.235f
        canvas.drawText(billData.customerMobile, mobileX, mobileY, valuePaint)
    }

    // 3. TABLE ITEMS DRAWING (BOLD & PROPER ROW HEIGHT)
    valuePaint.textSize = 12f * fontScale
    var startRowY = pageHeight * 0.315f
    val rowStep = pageHeight * 0.024f

    billData.items.forEachIndexed { index, item ->
        val currentY = startRowY + (index * rowStep)

        // SL No.
        val slNo = if (isEnglish) "${index + 1}" else convertToBengaliNumber(index + 1)
        canvas.drawText(slNo, pageWidth * 0.07f, currentY, valuePaint)

        // Item Name
        val itemName = if (isEnglish) item.nameEnglish else item.nameBengali
        canvas.drawText(itemName, pageWidth * 0.16f, currentY, valuePaint)

        // Quantity
        val qty = if (isEnglish) "${item.quantity}" else convertToBengaliNumber(item.quantity)
        canvas.drawText(qty, pageWidth * 0.53f, currentY, valuePaint)

        // Rate
        val rate = if (isEnglish) "₹${item.rate}" else "₹${convertToBengaliNumber(item.rate)}"
        canvas.drawText(rate, pageWidth * 0.68f, currentY, valuePaint)

        // Total
        val total = if (isEnglish) "₹${item.total}" else "₹${convertToBengaliNumber(item.total)}"
        canvas.drawText(total, pageWidth * 0.85f, currentY, valuePaint)
    }

    // 4. SUMMARY TOTALS (BOX-FIT ALIGNMENT & BOLD)
    valuePaint.textSize = 14f * fontScale

    // মোট টাকা / Subtotal
    val subtotalX = pageWidth * 0.84f
    val subtotalY = pageHeight * 0.755f
    val subtotalText = if (isEnglish) "₹${billData.subTotal}" else "₹${convertToBengaliNumber(billData.subTotal)}"
    canvas.drawText(subtotalText, subtotalX, subtotalY, valuePaint)

    // ছাড় (-) / Discount
    val discountY = pageHeight * 0.785f
    val discountVal = billData.discountAmount
    val discountText = if (discountVal > 0) {
        if (isEnglish) "₹$discountVal" else "₹${convertToBengaliNumber(discountVal)}"
    } else {
        "-"
    }
    canvas.drawText(discountText, subtotalX, discountY, valuePaint)

    // সর্বমোট টাকা / Grand Total
    val grandTotalY = pageHeight * 0.815f
    val grandTotalText = if (isEnglish) "₹${billData.grandTotal}" else "₹${convertToBengaliNumber(billData.grandTotal)}"
    canvas.drawText(grandTotalText, subtotalX, grandTotalY, valuePaint)

    // বকেয়া / Due Amount
    val dueY = pageHeight * 0.855f
    val dueText = if (isEnglish) "₹${billData.dueAmount}" else "₹${convertToBengaliNumber(billData.dueAmount)}"
    canvas.drawText(dueText, subtotalX, dueY, valuePaint)

    // 5. UPI QR CODE (SCALED TO FILL BOX FULLY)
    qrBitmap?.let { bitmap ->
        val qrBoxLeft = pageWidth * 0.08f
        val qrBoxTop = pageHeight * 0.735f
        val qrBoxSize = pageWidth * 0.22f // Perfectly fits inside the QR boundary box

        val destRect = RectF(
            qrBoxLeft,
            qrBoxTop,
            qrBoxLeft + qrBoxSize,
            qrBoxTop + qrBoxSize
        )
        canvas.drawBitmap(bitmap, null, destRect, null)
    }
}

// Utility function for Bengali digit localization
fun convertToBengaliNumber(number: Any): String {
    val englishDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    var str = number.toString()
    for (i in 0..9) {
        str = str.replace(englishDigits[i], bengaliDigits[i])
    }
    return str
}
