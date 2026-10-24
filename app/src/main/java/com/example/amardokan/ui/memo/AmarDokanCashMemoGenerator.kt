package com.example.amardokan.ui.memo

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import com.example.amardokan.data.Bill
import com.example.amardokan.data.BillItem
import com.example.amardokan.ui.screens.DraftBillItem
import java.io.File
import java.io.FileOutputStream

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
     * Renders a crisp receipt bitmap for preview and printing.
     */
    fun generateMemoBitmap(memoData: MemoData, context: Context): Bitmap {
        val width = 720
        val isEn = memoData.language.uppercase() == "EN"
        val rowHeight = 44
        val baseHeight = 560
        val totalHeight = (baseHeight + (memoData.items.size * rowHeight)).coerceAtLeast(800)

        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = 28f
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#0F172A")
            textSize = 42f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.DKGRAY
            textSize = 22f
            textAlign = Paint.Align.CENTER
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.LTGRAY
            strokeWidth = 2f
        }

        var y = 60f
        canvas.drawText(if (isEn) SHOP_NAME_EN else SHOP_NAME_BN, width / 2f, y, headerPaint)
        y += 36f
        canvas.drawText(if (isEn) SHOP_SUBTITLE_EN else SHOP_SUBTITLE_BN, width / 2f, y, subPaint)
        y += 30f
        canvas.drawText(if (isEn) "Phone: $SHOP_PHONE" else "ফোন: $SHOP_PHONE", width / 2f, y, subPaint)
        y += 30f
        canvas.drawLine(30f, y, width - 30f, y, linePaint)

        y += 40f
        textPaint.textSize = 24f
        canvas.drawText(if (isEn) "Bill No: ${memoData.billNumber}" else "বিল নং: ${memoData.billNumber}", 36f, y, textPaint)
        val dateText = if (isEn) "Date: ${memoData.dateStr}" else "তারিখ: ${memoData.dateStr}"
        val dateWidth = textPaint.measureText(dateText)
        canvas.drawText(dateText, width - 36f - dateWidth, y, textPaint)

        y += 34f
        val custText = if (isEn) "Customer: ${memoData.customerName}" else "গ্রাহক: ${memoData.customerName}"
        canvas.drawText(custText, 36f, y, textPaint)

        val payText = if (isEn) {
            if (memoData.paymentType == "CASH") "Status: Paid (Cash)" else "Status: Due (Credit)"
        } else {
            if (memoData.paymentType == "CASH") "পেমেন্ট: নগদ (পরিশোধিত)" else "পেমেন্ট: বাকি (বকেয়া)"
        }
        val payWidth = textPaint.measureText(payText)
        canvas.drawText(payText, width - 36f - payWidth, y, textPaint)

        y += 30f
        canvas.drawLine(30f, y, width - 30f, y, linePaint)

        // Table Header
        y += 36f
        boldPaint.textSize = 22f
        canvas.drawText(if (isEn) "Item Description" else "বিবরণ", 36f, y, boldPaint)
        canvas.drawText(if (isEn) "Qty x Rate" else "পরিমাণ × দর", 400f, y, boldPaint)
        val totalHeader = if (isEn) "Total" else "মোট"
        val thWidth = boldPaint.measureText(totalHeader)
        canvas.drawText(totalHeader, width - 36f - thWidth, y, boldPaint)

        y += 18f
        canvas.drawLine(30f, y, width - 30f, y, linePaint)

        // Items
        textPaint.textSize = 22f
        for (item in memoData.items) {
            y += 36f
            canvas.drawText(item.serviceName, 36f, y, textPaint)
            val qtyStr = "${item.quantity.toInt()} x ₹${item.unitRate.toInt()}"
            canvas.drawText(qtyStr, 400f, y, textPaint)
            val lineTotalStr = "₹${item.lineTotal.toInt()}"
            val ltWidth = textPaint.measureText(lineTotalStr)
            canvas.drawText(lineTotalStr, width - 36f - ltWidth, y, textPaint)
        }

        y += 26f
        canvas.drawLine(30f, y, width - 30f, y, linePaint)

        if (memoData.discount > 0.0) {
            y += 34f
            canvas.drawText(if (isEn) "Subtotal:" else "সাবটোটাল:", 400f, y, textPaint)
            val subStr = "₹${memoData.subtotal.toInt()}"
            canvas.drawText(subStr, width - 36f - textPaint.measureText(subStr), y, textPaint)

            y += 30f
            canvas.drawText(if (isEn) "Discount:" else "ছাড়:", 400f, y, textPaint)
            val discStr = "-₹${memoData.discount.toInt()}"
            canvas.drawText(discStr, width - 36f - textPaint.measureText(discStr), y, textPaint)
            y += 18f
            canvas.drawLine(400f, y, width - 30f, y, linePaint)
        }

        y += 42f
        boldPaint.textSize = 26f
        canvas.drawText(if (isEn) "Grand Total:" else "সর্বমোট প্রদেয়:", 340f, y, boldPaint)
        val grandStr = "₹${memoData.grandTotal.toInt()}"
        boldPaint.textSize = 30f
        canvas.drawText(grandStr, width - 36f - boldPaint.measureText(grandStr), y, boldPaint)

        y += 40f
        canvas.drawLine(30f, y, width - 30f, y, linePaint)
        y += 40f
        subPaint.textSize = 20f
        canvas.drawText(if (isEn) "Thank you for visiting! Please visit again." else "ধন্যবাদ! আবার আসবেন।", width / 2f, y, subPaint)

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
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = document.startPage(pageInfo)

        val bmp = generateMemoBitmap(memoData, context)
        val canvas = page.canvas
        val scale = 540f / bmp.width.toFloat()
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(28f, 30f)
        }
        canvas.drawBitmap(bmp, matrix, null)
        document.finishPage(page)

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        return file
    }

    fun printMemo(context: Context, memoData: MemoData) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val isEn = memoData.language.uppercase() == "EN"

        val html = StringBuilder().apply {
            append("<!DOCTYPE html><html><head><meta charset='utf-8'>")
            append("<style>")
            append("body { font-family: monospace; width: 280px; margin: 0 auto; padding: 10px; color: #000; font-size: 12px; }")
            append(".center { text-align: center; }")
            append(".bold { font-weight: bold; }")
            append(".line { border-top: 1px dashed #000; margin: 6px 0; }")
            append(".row { display: flex; justify-content: space-between; margin: 3px 0; }")
            append(".title { font-size: 16px; font-weight: bold; }")
            append("</style></head><body>")
            append("<div class='center title'>${if (isEn) SHOP_NAME_EN else SHOP_NAME_BN}</div>")
            append("<div class='center'>${if (isEn) SHOP_SUBTITLE_EN else SHOP_SUBTITLE_BN}</div>")
            append("<div class='center'>Phone: $SHOP_PHONE</div>")
            append("<div class='line'></div>")
            append("<div class='row'><span>${if (isEn) "Bill:" else "বিল নং:"} ${memoData.billNumber}</span><span>${memoData.dateStr}</span></div>")
            append("<div class='row'><span>${if (isEn) "Cust:" else "গ্রাহক:"} ${memoData.customerName}</span></div>")
            append("<div class='row'><span>${if (isEn) "Status: " + memoData.paymentType else "পেমেন্ট: " + if (memoData.paymentType == "CASH") "নগদ" else "বাকি"}</span></div>")
            append("<div class='line'></div>")
            append("<div class='row bold'><span>${if (isEn) "Item" else "বিবরণ"}</span><span>Qty x Rate</span><span>${if (isEn) "Total" else "মোট"}</span></div>")
            append("<div class='line'></div>")
            memoData.items.forEach {
                append("<div class='row'>")
                append("<span>${it.serviceName}</span>")
                append("<span>${it.quantity.toInt()} × ₹${it.unitRate.toInt()}</span>")
                append("<span>₹${it.lineTotal.toInt()}</span>")
                append("</div>")
            }
            append("<div class='line'></div>")
            if (memoData.discount > 0.0) {
                append("<div class='row'><span>Subtotal:</span><span>₹${memoData.subtotal.toInt()}</span></div>")
                append("<div class='row'><span>Discount:</span><span>-₹${memoData.discount.toInt()}</span></div>")
            }
            append("<div class='row bold' style='font-size: 14px;'><span>Grand Total:</span><span>₹${memoData.grandTotal.toInt()}</span></div>")
            append("<div class='line'></div>")
            append("<div class='center'>${if (isEn) "Thank you! Visit again." else "ধন্যবাদ! আবার আসবেন।"}</div>")
            append("</body></html>")
        }.toString()

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printAdapter = webView.createPrintDocumentAdapter("CashMemo_${memoData.billNumber}")
                printManager.print("CashMemo_${memoData.billNumber}", printAdapter, PrintAttributes.Builder().build())
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
