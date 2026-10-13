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

        // 1. RECEIPT / BILL NUMBER (Printed bold next to "নং:" / "No:" inside the yellow box)
        // Printed "নং:" ends around x=115f, dots continue. Bill number starts right after label at x=175f
        val billNoPaint = Paint().apply {
            isAntiAlias = true
            isSubpixelText = true
            color = Color.rgb(211, 47, 47) // Bold Red text
            textSize = 23f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val billNumClean = if (data.billNumber.isNotBlank()) {
            data.billNumber.replace("BILL-", "").replace("বিল-", "").trim()
        } else {
            "1001"
        }
        val billNumStr = if (isEn) billNumClean else BanglaNumberUtil.toBangla(billNumClean)
        canvas.drawText(billNumStr, 175f, 482f, billNoPaint)

        // 2. DATE (Printed bold next to "তারিখ:" / "Date:" inside the yellow box)
        // Printed "তারিখ:" / "Date:" label occupies x=[855..915], date text starts at x=918f
        val datePaint = Paint().apply {
            isAntiAlias = true
            isSubpixelText = true
            color = Color.rgb(30, 41, 59)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val dateClean = if (data.dateFormatted.isNotBlank()) {
            data.dateFormatted
        } else {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
            sdf.format(Date())
        }
        val dateDisplay = if (isEn) dateClean else BanglaNumberUtil.toBangla(dateClean)
        canvas.drawText(dateDisplay, 918f, 482f, datePaint)

        // 3. CUSTOMER NAME & PHONE (Printed bold next to "গ্রাহকের নাম:" / "Customer Name:")
        // Blank field starts directly beside the printed label at x=220f
        val customerNamePaint = Paint().apply {
            isAntiAlias = true
            isSubpixelText = true
            color = Color.rgb(15, 23, 42)
            textSize = 23f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val customerPhonePaint = Paint().apply {
            isAntiAlias = true
            isSubpixelText = true
            color = Color.rgb(30, 41, 59)
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        if (data.customerName.isNotBlank()) {
            val nameStartX = if (isEn) 225f else 220f
            val maxNameW = 500f
            val nameStr = truncateText(data.customerName, customerNamePaint, maxNameW)
            canvas.drawText(nameStr, nameStartX, 528f, customerNamePaint)
        }
        if (data.customerPhone.isNotBlank()) {
            val phoneStartX = if (isEn) 745f else 735f
            val maxPhoneW = 255f
            val phoneClean = if (isEn) data.customerPhone else BanglaNumberUtil.toBangla(data.customerPhone)
            val phoneStr = truncateText(phoneClean, customerPhonePaint, maxPhoneW)
            canvas.drawText(phoneStr, phoneStartX, 528f, customerPhonePaint)
        }

        // 4. INVOICE TABLE ROWS (14 rows from y=746f to 1195f, rowHeight=32.1f) - All BOLD
        val tableBoldTextPaint = Paint().apply {
            isAntiAlias = true
            isSubpixelText = true
            color = Color.rgb(15, 23, 42)
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val rowCount = 14
        val rowHeight = 32.1f
        val tableStartY = 746f

        for (r in 0 until minOf(rowCount, data.items.size)) {
            val item = data.items[r]
            val rY = tableStartY + r * rowHeight
            val textBaseY = rY + 22.5f

            // Col 1: Sl. No (centered at 63.5f)
            tableBoldTextPaint.textAlign = Paint.Align.CENTER
            val slNoStr = if (isEn) (r + 1).toString() else BanglaNumberUtil.toBangla((r + 1).toString())
            canvas.drawText(slNoStr, 63.5f, textBaseY, tableBoldTextPaint)

            // Col 2: Description (left-aligned at 115f, max width 395f)
            tableBoldTextPaint.textAlign = Paint.Align.LEFT
            val desc = truncateText(item.serviceName, tableBoldTextPaint, 395f)
            canvas.drawText(desc, 115f, textBaseY, tableBoldTextPaint)

            // Col 3: Quantity (centered at 592f)
            tableBoldTextPaint.textAlign = Paint.Align.CENTER
            val qtyRaw = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
            val qtyStr = if (isEn) qtyRaw else BanglaNumberUtil.toBangla(qtyRaw)
            canvas.drawText(qtyStr, 592f, textBaseY, tableBoldTextPaint)

            // Col 4: Rate (right-aligned at 810f)
            tableBoldTextPaint.textAlign = Paint.Align.RIGHT
            val rateRaw = item.unitRate.toInt().toString()
            val rateStr = if (isEn) rateRaw else BanglaNumberUtil.toBangla(rateRaw)
            canvas.drawText(rateStr, 810f, textBaseY, tableBoldTextPaint)

            // Col 5: Amount (right-aligned at 985f)
            tableBoldTextPaint.textAlign = Paint.Align.RIGHT
            val amtRaw = item.lineTotal.toInt().toString()
            val amtStr = if (isEn) amtRaw else BanglaNumberUtil.toBangla(amtRaw)
            canvas.drawText(amtStr, 985f, textBaseY, tableBoldTextPaint)
        }

        // 5. SCANNABLE DYNAMIC UPI QR CODE (Box-fit snugly inside QR white square: x=[35..246], y=[1245..1450])
        val qrAmount = String.format(Locale.ENGLISH, "%.2f", data.grandTotal)
        val billEncoded = Uri.encode(data.billNumber)
        val upiUri = "upi://pay?pa=$MERCHANT_UPI_ID&pn=Telecom%20Cyber%20Cafe&am=$qrAmount&cu=INR&tn=Bill%20$billEncoded"
        val qrBoxSize = 206
        val qrBmp = QrCodeGenerator.generateUpiQr(upiUri, qrBoxSize)
        if (qrBmp != null) {
            // Snugly fits container x=[36..245], y=[1245..1449]
            canvas.drawBitmap(qrBmp, 37f, 1246f, null)
        }

        // 6. TOTALS PANEL VALUES (মোট টাকা, ছাড় (-), সর্বমোট টাকা / বকেয়া)
        // Row 2: মোট টাকা / Subtotal (y=1238..1296, white box x=[790..1005], center y=1267f)
        val totalAmountVal = if (data.subtotal > 0.0) data.subtotal else data.grandTotal
        val totalPaint = Paint().apply {
            isAntiAlias = true
            isSubpixelText = true
            color = Color.rgb(15, 23, 42)
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val subRaw = totalAmountVal.toInt().toString()
        val subStr = if (isEn) "₹$subRaw" else "₹" + BanglaNumberUtil.toBangla(subRaw)
        canvas.drawText(subStr, 990f, 1276f, totalPaint)

        // Row 3: ছাড় (-) / Discount (y=1298..1358, white box x=[790..1005], center y=1328f)
        val discPaint = Paint().apply {
            isAntiAlias = true
            isSubpixelText = true
            color = Color.rgb(22, 101, 52) // Dark forest green bold
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
        canvas.drawText(discStr, 990f, 1337f, discPaint)

        // Row 4: সর্বমোট টাকা / Grand Total (y=1360..1430, white box x=[788..1005], center y=1395f)
        val gtPaint = Paint().apply {
            isAntiAlias = true
            isSubpixelText = true
            color = if (data.paymentType.equals("CREDIT", ignoreCase = true)) Color.rgb(220, 38, 38) else Color.rgb(185, 28, 28)
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val gtRaw = data.grandTotal.toInt().toString()
        val gtStr = if (isEn) "₹$gtRaw" else "₹" + BanglaNumberUtil.toBangla(gtRaw)
        canvas.drawText(gtStr, 990f, 1406f, gtPaint)

        // 7. PAYMENT STATUS STAMP / BOKEYA (পরিশোধিত / বকেয়া)
        // Positioned neatly in the payment status badge area (x=380..520, y=1380..1425)
        if (data.paymentType.equals("CASH", ignoreCase = true)) {
            val stampText = if (isEn) "PAID" else "পরিশোধিত"
            drawStamp(canvas, stampText, Color.rgb(22, 163, 74), 450f, 1405f, 0f)
        } else {
            val stampText = if (isEn) "DUE / বকেয়া" else "বকেয়া (DUE)"
            drawStamp(canvas, stampText, Color.rgb(220, 38, 38), 450f, 1405f, 0f)
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
