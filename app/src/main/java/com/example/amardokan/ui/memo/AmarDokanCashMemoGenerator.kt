package com.example.amardokan.ui.memo

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import com.example.amardokan.data.Bill
import com.example.amardokan.data.BillItem
import com.example.amardokan.data.DraftBillItem
import com.example.util.QrCodeGenerator
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object AmarDokanCashMemoGenerator {
    const val MERCHANT_UPI_ID = "7718489@ybl"
    const val SHOP_NAME_BN = "আমার দোকান"
    const val SHOP_NAME_EN = "AMAR DOKAN"
    const val SHOP_SUBTITLE_BN = "টেলিকম সাইবার ক্যাফে ও জেরক্স"
    const val SHOP_SUBTITLE_EN = "Telecom Cyber Cafe & Xerox Center"
    const val SHOP_PHONE = "7718489405"

    data class MemoItem(
        val serviceName: String,
        val quantity: Double,
        val unitRate: Double,
        val lineTotal: Double,
        val unitName: String = "পাতা"
    )

    data class MemoData(
        val billNumber: String,
        val customerName: String,
        val customerPhone: String = "",
        val dateStr: String,
        val items: List<MemoItem>,
        val subtotal: Double,
        val discount: Double,
        val grandTotal: Double,
        val paymentType: String, // "CASH" or "CREDIT"
        val language: String = "BN"
    ) {
        companion object {
            fun fromBill(bill: Bill, items: List<BillItem>): MemoData {
                return MemoData(
                    billNumber = bill.billNumber,
                    customerName = bill.customerName,
                    customerPhone = bill.customerPhone,
                    dateStr = bill.dateFormatted,
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
                    paymentType = bill.paymentType,
                    language = bill.language
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
                language: String
            ): MemoData {
                return MemoData(
                    billNumber = billNumber,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    dateStr = dateStr,
                    items = items.map {
                        MemoItem(
                            serviceName = it.serviceName,
                            quantity = it.quantity,
                            unitRate = it.unitRate,
                            lineTotal = it.lineTotal,
                            unitName = it.unitName
                        )
                    },
                    subtotal = subtotal,
                    discount = discount,
                    grandTotal = grandTotal,
                    paymentType = paymentType,
                    language = language
                )
            }
        }
    }

    /**
     * Renders a crisp receipt bitmap by overlaying dynamic bill details onto
     * the pre-designed template image (cash_memo_bengali.png / cash_memo_english.png).
     */
    fun generateMemoBitmap(memoData: MemoData, context: Context): Bitmap {
        val isEn = memoData.language.uppercase() == "EN"
        val assetName = if (isEn) "cash_memo_english.png" else "cash_memo_bengali.png"

        val baseBitmap: Bitmap = try {
            context.assets.open(assetName).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            null
        } ?: Bitmap.createBitmap(1024, 1536, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.WHITE)
        }

        val bitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmap)

        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#111827")
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val regularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#111827")
            typeface = Typeface.DEFAULT
        }

        // 1. Bill No (inside red outline box: x=92..240, y=560..600, center=166f, baseline=588f)
        boldPaint.textSize = 25f
        boldPaint.textAlign = Paint.Align.CENTER
        boldPaint.color = android.graphics.Color.parseColor("#991B1B")
        canvas.drawText(memoData.billNumber, 166f, 588f, boldPaint)

        // 2. Date (inside date outline box: x=845..1005, y=565..600, center=925f, baseline=588f)
        boldPaint.textSize = 22f
        boldPaint.textAlign = Paint.Align.CENTER
        boldPaint.color = android.graphics.Color.parseColor("#1E3A8A")
        canvas.drawText(memoData.dateStr, 925f, 588f, boldPaint)

        // 3. Customer Name & Mobile No (baseline: 638f)
        boldPaint.textSize = 23f
        boldPaint.textAlign = Paint.Align.LEFT
        boldPaint.color = android.graphics.Color.parseColor("#0F172A")
        val maxCustWidth = 345f
        var custName = memoData.customerName.ifBlank { if (isEn) "Cash Customer" else "নগদ গ্রাহক" }
        if (boldPaint.measureText(custName) > maxCustWidth) {
            while (custName.length > 3 && boldPaint.measureText("$custName...") > maxCustWidth) {
                custName = custName.dropLast(1)
            }
            custName = "$custName..."
        }
        canvas.drawText(custName, 215f, 638f, boldPaint)

        if (memoData.customerPhone.isNotBlank()) {
            boldPaint.textSize = 23f
            boldPaint.textAlign = Paint.Align.LEFT
            boldPaint.color = android.graphics.Color.parseColor("#0F172A")
            canvas.drawText(memoData.customerPhone, 770f, 638f, boldPaint)
        }

        // 4. Item Table Rows (up to 15 items, row 0 baseline = 770f, step = 32f)
        val rowBaseY = 770f
        val rowHeight = 32f
        val maxItems = minOf(memoData.items.size, 15)

        val itemBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#0F172A")
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
        }

        val itemRegularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#111827")
            textSize = 20f
            typeface = Typeface.DEFAULT
        }

        for (i in 0 until maxItems) {
            val item = memoData.items[i]
            val currentY = rowBaseY + (i * rowHeight)

            // Column 2: Description (x: 115f..510f, max width = 395f)
            itemBoldPaint.textAlign = Paint.Align.LEFT
            var desc = item.serviceName
            val maxDescWidth = 395f
            if (itemBoldPaint.measureText(desc) > maxDescWidth) {
                while (desc.length > 3 && itemBoldPaint.measureText("$desc...") > maxDescWidth) {
                    desc = desc.dropLast(1)
                }
                desc = "$desc..."
            }
            canvas.drawText(desc, 115f, currentY, itemBoldPaint)

            // Column 3: Quantity (center: 592f)
            itemRegularPaint.textAlign = Paint.Align.CENTER
            val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
            canvas.drawText(qtyStr, 592f, currentY, itemRegularPaint)

            // Column 4: Rate (right align: 812f)
            itemRegularPaint.textAlign = Paint.Align.RIGHT
            val rateStr = if (item.unitRate % 1.0 == 0.0) item.unitRate.toInt().toString() else String.format(Locale.US, "%.2f", item.unitRate)
            canvas.drawText(rateStr, 812f, currentY, itemRegularPaint)

            // Column 5: Amount (right align: 990f)
            itemBoldPaint.textAlign = Paint.Align.RIGHT
            val totalStr = if (item.lineTotal % 1.0 == 0.0) item.lineTotal.toInt().toString() else String.format(Locale.US, "%.2f", item.lineTotal)
            canvas.drawText(totalStr, 990f, currentY, itemBoldPaint)
        }

        // 5. Payment Summary (Right Bottom Boxes)
        val summaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        // Subtotal (baseline: 1276f, right align: 985f)
        summaryPaint.textSize = 25f
        summaryPaint.color = android.graphics.Color.parseColor("#0F172A")
        val subtotalFormatted = if (memoData.subtotal % 1.0 == 0.0) memoData.subtotal.toInt().toString() else String.format(Locale.US, "%.2f", memoData.subtotal)
        canvas.drawText("₹$subtotalFormatted", 985f, 1276f, summaryPaint)

        // Discount (baseline: 1335f, right align: 985f)
        summaryPaint.textSize = 25f
        if (memoData.discount > 0.0) {
            summaryPaint.color = android.graphics.Color.parseColor("#DC2626")
            val discountFormatted = if (memoData.discount % 1.0 == 0.0) memoData.discount.toInt().toString() else String.format(Locale.US, "%.2f", memoData.discount)
            canvas.drawText("-₹$discountFormatted", 985f, 1335f, summaryPaint)
        } else {
            summaryPaint.color = android.graphics.Color.parseColor("#0F172A")
            canvas.drawText("₹0", 985f, 1335f, summaryPaint)
        }

        // Grand Total (baseline: 1405f, right align: 985f)
        summaryPaint.textSize = 28f
        summaryPaint.color = android.graphics.Color.parseColor("#047857")
        val grandTotalFormatted = if (memoData.grandTotal % 1.0 == 0.0) memoData.grandTotal.toInt().toString() else String.format(Locale.US, "%.2f", memoData.grandTotal)
        canvas.drawText("₹$grandTotalFormatted", 985f, 1405f, summaryPaint)

        // 6. Dynamic UPI QR Code in bottom-left white box (x: 35..246, y: 1249..1449)
        try {
            val upiAmount = String.format(Locale.US, "%.2f", memoData.grandTotal)
            val shopNameForUpi = if (isEn) SHOP_NAME_EN else SHOP_NAME_BN
            val upiUri = "upi://pay?pa=$MERCHANT_UPI_ID&pn=${Uri.encode(shopNameForUpi)}&am=$upiAmount&cu=INR&tn=${Uri.encode("Bill #" + memoData.billNumber)}"
            val qrBitmap = QrCodeGenerator.generateUpiQr(upiUri, size = 180)
            if (qrBitmap != null) {
                canvas.drawBitmap(qrBitmap, 48f, 1256f, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return bitmap
    }

    /**
     * Generates a genuine PDF file of the receipt and returns the File.
     */
    fun generatePdfFile(context: Context, memoData: MemoData): File {
        val dir = File(context.cacheDir, "memos").apply { mkdirs() }
        val cleanBillNo = memoData.billNumber.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val cleanName = memoData.customerName.ifBlank { "Unknown" }.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val file = File(dir, "${cleanName}_${cleanBillNo}.pdf")

        val document = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4
        val page = document.startPage(pageInfo)

        val bmp = generateMemoBitmap(memoData, context)
        val canvas = page.canvas

        val targetWidth = 555f
        val targetHeight = targetWidth * (bmp.height.toFloat() / bmp.width.toFloat())
        val left = (595f - targetWidth) / 2f
        val top = (842f - targetHeight) / 2f

        val destRect = RectF(left, top, left + targetWidth, top + targetHeight)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bmp, null, destRect, paint)
        document.finishPage(page)

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        return file
    }

    fun printMemo(context: Context, memoData: MemoData) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return

        val bitmap = generateMemoBitmap(memoData, context)
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val base64Img = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    @page {
                        size: A4 portrait;
                        margin: 0;
                    }
                    html, body {
                        margin: 0;
                        padding: 0;
                        width: 100%;
                        height: 100%;
                        background: #ffffff;
                        display: flex;
                        justify-content: center;
                        align-items: flex-start;
                    }
                    img {
                        width: 100%;
                        max-width: 100%;
                        height: auto;
                        display: block;
                        page-break-inside: avoid;
                    }
                </style>
            </head>
            <body>
                <img src="data:image/png;base64,$base64Img" alt="Cash Memo" />
            </body>
            </html>
        """.trimIndent()

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printAdapter = webView.createPrintDocumentAdapter("CashMemo_${memoData.billNumber}")
                val printAttributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
                printManager.print("CashMemo_${memoData.billNumber}", printAdapter, printAttributes)
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }

    fun shareOnWhatsAppOrApps(context: Context, memoData: MemoData, asPdf: Boolean = true) {
        val file = generatePdfFile(context, memoData)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Bill #${memoData.billNumber} - ${memoData.customerName}")
            putExtra(Intent.EXTRA_TEXT, "🧾 ${if (memoData.language.uppercase() == "EN") "Invoice" else "ক্যাশ মেমো"} #${memoData.billNumber} for ${memoData.customerName}. Total: ₹${memoData.grandTotal.toInt()}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Bill"))
    }
}
