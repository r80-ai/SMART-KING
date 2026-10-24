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
    private fun renderAuthenticCashMemo(data: MemoData, isEn: Boolean, context: Context?): Bitmap {
        val bitmap = loadTemplateBitmap(context, isEn)
        val canvas = Canvas(bitmap)

        // 1. RECEIPT / BILL NUMBER (Solid black bold, centered inside white number box: x=[92..236], y=[563..597], center=(164, 580))
        val billNoPaint = Paint().apply {
            isAntiAlias = true
            isSubpixelText = true
            color = Color.BLACK // Solid Black
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val billNumClean = if (data.billNumber.isNotBlank()) {
            data.billNumber.replace("BILL-", "").replace("বিল-", "").trim()
        } else {
            "1001"
        }
        val billNumStr = if (isEn) billNumClean else BanglaNumberUtil.toBangla(billNumClean)
        canvas.drawText(billNumStr, 164f, 588f, billNoPaint)

        // 2. DATE (Solid black bold, centered inside white date box: x=[845..998], y=[564..596], center=(922, 580))
        val datePaint = Paint().apply {
            isAntiAlias = true
            isSubpixelText = true
            color = Color.BLACK // Solid Black
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val dateClean = if (data.dateFormatted.isNotBlank()) {
            data.dateFormatted
        } else {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
            sdf.format(Date())
        }
        val dateDisplay = if (isEn) dateClean else BanglaNumberUtil.toBangla(dateClean)
        canvas.drawText(dateDisplay, 922f, 588f, datePaint)

        // 3. CUSTOMER NAME & PHONE (Solid black bold directly on the baseline following labels at y=658f)
        val customerNamePaint = Paint().apply {
            isAntiAlias = true
            isSubpixelText = true
            color = Color.BLACK // Solid Black
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val customerPhonePaint = Paint().apply {
            isAntiAlias = true
            isSubpixelText = true
            color = Color.BLACK // Solid Black
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        if (data.customerName.isNotBlank()) {
            // Bengali label ends ~200f (dots start 205f..560f); English label ends ~260f (dots start 265f..560f)
            val nameStartX = if (isEn) 265f else 205f
            val maxNameW = if (isEn) 295f else 350f
            val nameStr = truncateText(data.customerName, customerNamePaint, maxNameW)
            canvas.drawText(nameStr, nameStartX, 658f, customerNamePaint)
        }
        if (data.customerPhone.isNotBlank()) {
            // Bengali label ends ~752f (dots start 758f..998f); English label ends ~745f (dots start 752f..998f)
            val phoneStartX = if (isEn) 752f else 758f
            val maxPhoneW = if (isEn) 245f else 240f
            val phoneClean = if (isEn) data.customerPhone else BanglaNumberUtil.toBangla(data.customerPhone)
            val phoneStr = truncateText(phoneClean, customerPhonePaint, maxPhoneW)
            canvas.drawText(phoneStr, phoneStartX, 658f, customerPhonePaint)
        }

        // 4. INVOICE TABLE ROWS (15 rows from y=747f to 1228f, rowHeight=32.05f) - All Solid Black & BOLD
        val tableBoldTextPaint = Paint().apply {
            isAntiAlias = true
            isSubpixelText = true
            color = Color.BLACK // Solid Black
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val rowCount = 15
        val rowHeight = 32.05f
        val tableStartY = 747f

        for (r in 0 until minOf(rowCount, data.items.size)) {
            val item = data.items[r]
            val rY = tableStartY + r * rowHeight
            val textBaseY = rY + 22.5f

            // Col 1: Sl. No is pre-printed in the template (১..১৫ / 1..15)

            // Col 2: Description (x=[104..527], left-aligned at 114f, max width 405f)
            tableBoldTextPaint.textAlign = Paint.Align.LEFT
            val desc = truncateText(item.serviceName, tableBoldTextPaint, 405f)
            canvas.drawText(desc, 114f, textBaseY, tableBoldTextPaint)

            // Col 3: Quantity (x=[528..656], centered at 592f)
            tableBoldTextPaint.textAlign = Paint.Align.CENTER
            val qtyRaw = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
            val qtyStr = if (isEn) qtyRaw else BanglaNumberUtil.toBangla(qtyRaw)
            canvas.drawText(qtyStr, 592f, textBaseY, tableBoldTextPaint)

            // Col 4: Rate (x=[657..826], right-aligned at 812f)
            tableBoldTextPaint.textAlign = Paint.Align.RIGHT
            val rateRaw = item.unitRate.toInt().toString()
            val rateStr = if (isEn) rateRaw else BanglaNumberUtil.toBangla(rateRaw)
            canvas.drawText(rateStr, 812f, textBaseY, tableBoldTextPaint)

            // Col 5: Amount (x=[827..1004], right-aligned at 990f)
            tableBoldTextPaint.textAlign = Paint.Align.RIGHT
            val amtRaw = item.lineTotal.toInt().toString()
            val amtStr = if (isEn) amtRaw else BanglaNumberUtil.toBangla(amtRaw)
            canvas.drawText(amtStr, 990f, textBaseY, tableBoldTextPaint)
        }

        // 5. SCANNABLE DYNAMIC UPI QR CODE (Box-fit snugly inside white square: x=[30..244], y=[1242..1448])
        val qrAmount = String.format(Locale.ENGLISH, "%.2f", data.grandTotal)
        val billEncoded = Uri.encode(data.billNumber)
        val upiUri = "upi://pay?pa=$MERCHANT_UPI_ID&pn=Telecom%20Cyber%20Cafe&am=$qrAmount&cu=INR&tn=Bill%20$billEncoded"
        val qrBoxSize = 208
        val qrBmp = QrCodeGenerator.generateUpiQr(upiUri, qrBoxSize)
        if (qrBmp != null) {
            canvas.drawBitmap(qrBmp, 33f, 1241f, null)
        }

        // 6. TOTALS PANEL VALUES (মোট টাকা, ছাড় (-), সর্বমোট টাকা) - Solid Black & Bold
        // Row 1: মোট টাকা / Subtotal (white box: x=[765..979], y=[1242..1295], center Y=1268.5f, baseline=1278f)
        val totalAmountVal = if (data.subtotal > 0.0) data.subtotal else data.grandTotal
        val totalPaint = Paint().apply {
            isAntiAlias = true
            isSubpixelText = true
            color = Color.BLACK // Solid Black
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val subRaw = totalAmountVal.toInt().toString()
        val subStr = if (isEn) "₹$subRaw" else "₹" + BanglaNumberUtil.toBangla(subRaw)
        canvas.drawText(subStr, 965f, 1278f, totalPaint)

        // Row 2: ছাড় (-) / Discount (white box: x=[770..979], y=[1302..1356], center Y=1329f, baseline=1338f)
        val discPaint = Paint().apply {
            isAntiAlias = true
            isSubpixelText = true
            color = Color.BLACK // Solid Black Bold
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val discRaw = data.discount.toInt().toString()
        val discStr = if (data.discount > 0.0) {
            if (isEn) "- ₹$discRaw" else "- ₹" + BanglaNumberUtil.toBangla(discRaw)
        } else {
            if (isEn) "₹0" else "₹০"
        }
        canvas.drawText(discStr, 965f, 1338f, discPaint)

        // Row 3: সর্বমোট টাকা / Grand Total (white box: x=[765..979], y=[1364..1429], center Y=1396.5f, baseline=1407f)
        val gtPaint = Paint().apply {
            isAntiAlias = true
            isSubpixelText = true
            color = Color.BLACK // Solid Black Bold
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val gtRaw = data.grandTotal.toInt().toString()
        val gtStr = if (isEn) "₹$gtRaw" else "₹" + BanglaNumberUtil.toBangla(gtRaw)
        canvas.drawText(gtStr, 965f, 1407f, gtPaint)

        // 7. PAYMENT STATUS STAMP / BOKEYA (পরিশোধিত / বকেয়া)
        // Positioned in status badge area (center x=410f, y=1405f)
        if (data.paymentType.equals("CASH", ignoreCase = true)) {
            val stampText = if (isEn) "PAID" else "পরিশোধিত"
            drawStamp(canvas, stampText, Color.rgb(22, 163, 74), 410f, 1405f, 0f)
        } else {
            val stampText = if (isEn) "DUE / বকেয়া" else "বকেয়া (DUE)"
            drawStamp(canvas, stampText, Color.rgb(220, 38, 38), 410f, 1405f, 0f)
        }

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
