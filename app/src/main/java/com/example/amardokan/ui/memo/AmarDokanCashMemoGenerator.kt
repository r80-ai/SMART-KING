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

    // Memo Dimensions (High DPI for crisp text and print clarity)
    const val MEMO_WIDTH = 1000
    const val MEMO_HEIGHT = 1450

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
        val notes: String = ""
    ) {
        companion object {
            fun fromBill(bill: Bill, items: List<BillItem>): MemoData {
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
                    notes = bill.notes
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
                paymentType: String
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
                    grandTotal = grandTotal
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
     * Generates a high-resolution Bitmap of the complete Bengali Cash Memo.
     */
    fun generateMemoBitmap(data: MemoData): Bitmap {
        val bitmap = Bitmap.createBitmap(MEMO_WIDTH, MEMO_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Clean Background
        canvas.drawColor(Color.WHITE)

        val navyDark = Color.rgb(15, 32, 66)
        val blueHeader = Color.rgb(21, 101, 192)
        val crimsonRed = Color.rgb(178, 34, 34)
        val borderDark = Color.rgb(30, 41, 59)
        val tableBgHeader = Color.rgb(241, 245, 249)
        val lightGrayLine = Color.rgb(203, 213, 225)
        val cashGreen = Color.rgb(46, 125, 50)

        val paint = Paint().apply {
            isAntiAlias = true
            isSubpixelText = true
        }

        // 2. DOUBLE ORNAMENTAL BORDER
        paint.style = Paint.Style.STROKE
        paint.color = navyDark
        paint.strokeWidth = 3.5f
        canvas.drawRect(24f, 24f, MEMO_WIDTH - 24f, MEMO_HEIGHT - 24f, paint)

        paint.strokeWidth = 1.2f
        canvas.drawRect(32f, 32f, MEMO_WIDTH - 32f, MEMO_HEIGHT - 32f, paint)

        // Corner decorative ornaments
        val cornerSize = 20f
        paint.strokeWidth = 2.5f
        paint.color = crimsonRed
        // Top-Left
        canvas.drawLine(24f, 24f + cornerSize, 24f + cornerSize, 24f, paint)
        // Top-Right
        canvas.drawLine(MEMO_WIDTH - 24f - cornerSize, 24f, MEMO_WIDTH - 24f, 24f + cornerSize, paint)
        // Bottom-Left
        canvas.drawLine(24f, MEMO_HEIGHT - 24f - cornerSize, 24f + cornerSize, MEMO_HEIGHT - 24f, paint)
        // Bottom-Right
        canvas.drawLine(MEMO_WIDTH - 24f - cornerSize, MEMO_HEIGHT - 24f, MEMO_WIDTH - 24f, MEMO_HEIGHT - 24f - cornerSize, paint)

        // 3. TOP INVOCATION & CONTACT BAR
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.color = crimsonRed
        paint.textSize = 21f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("॥ শ্রী শ্রী গণেশায় নমঃ ॥", MEMO_WIDTH / 2f, 62f, paint)

        // Top Contact on Right and Proprietor on Left
        paint.textSize = 17f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.rgb(71, 85, 105)

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("প্রোপ্রাইটার: রফিকুল গায়েন", 45f, 62f, paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("মোবাইল: 7718489405", MEMO_WIDTH - 45f, 62f, paint)

        // Thin separator
        paint.color = lightGrayLine
        paint.strokeWidth = 1.2f
        canvas.drawLine(45f, 76f, MEMO_WIDTH - 45f, 76f, paint)

        // 4. MAIN SHOP BANNER
        paint.textAlign = Paint.Align.CENTER
        paint.color = navyDark
        paint.textSize = 46f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("টেলিকম সাইবার ক্যাফে ও জেরক্স", MEMO_WIDTH / 2f, 126f, paint)

        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = blueHeader
        canvas.drawText("TELECOM CYBER CAFE & XEROX CENTER", MEMO_WIDTH / 2f, 156f, paint)

        // Shop Sub-services banner
        val bannerRect = RectF(60f, 172f, MEMO_WIDTH - 60f, 206f)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(238, 242, 255)
        canvas.drawRoundRect(bannerRect, 8f, 8f, paint)

        paint.color = Color.rgb(30, 58, 138)
        paint.textSize = 17.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("এখানে জেরক্স, কালার প্রিন্ট, ল্যামিনেশন, অনলাইন ফর্ম ফিলাপ, ছবি তোলা ও যাবতীয় কাজ যত্নসহকারে করা হয়", MEMO_WIDTH / 2f, 195f, paint)

        // Address
        paint.color = Color.rgb(51, 65, 85)
        paint.textSize = 19f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("গ্রাম - চোরগেড়িয়া, পোঃ - সিহড়, থানা - কোতুলপুর, জেলা - বাঁকুড়া (পশ্চিমবঙ্গ)", MEMO_WIDTH / 2f, 233f, paint)

        // 5. "ক্যাশ মেমো / CASH MEMO" BADGE
        val memoBadge = RectF(MEMO_WIDTH / 2f - 140f, 252f, MEMO_WIDTH / 2f + 140f, 290f)
        paint.style = Paint.Style.FILL
        paint.color = navyDark
        canvas.drawRoundRect(memoBadge, 19f, 19f, paint)

        paint.color = Color.WHITE
        paint.textSize = 21f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ক্যাশ মেমো / CASH MEMO", MEMO_WIDTH / 2f, 279f, paint)

        // 6. CUSTOMER & BILL INFO BOX
        val metaBox = RectF(45f, 305f, MEMO_WIDTH - 45f, 400f)
        paint.style = Paint.Style.STROKE
        paint.color = borderDark
        paint.strokeWidth = 1.5f
        canvas.drawRoundRect(metaBox, 6f, 6f, paint)

        // Mid vertical line in info box
        canvas.drawLine(MEMO_WIDTH / 2f + 50f, 305f, MEMO_WIDTH / 2f + 50f, 400f, paint)

        // Inside Customer Details (Left)
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 20f

        val custNameLabel = "গ্রাহকের নাম: "
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(custNameLabel, 60f, 340f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val maxCustWidth = (MEMO_WIDTH / 2f + 30f) - (60f + paint.measureText(custNameLabel))
        val truncatedCustName = truncateText(data.customerName, paint, maxCustWidth)
        canvas.drawText(truncatedCustName, 60f + paint.measureText(custNameLabel), 340f, paint)

        val phoneLabel = "মোবাইল নম্বর: "
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(phoneLabel, 60f, 378f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val phoneText = if (data.customerPhone.isNotBlank()) data.customerPhone else "প্রযোজ্য নহে"
        canvas.drawText(phoneText, 60f + paint.measureText(phoneLabel), 378f, paint)

        // Inside Bill Meta (Right)
        val rightX = MEMO_WIDTH / 2f + 65f
        val billNoLabel = "বিল নম্বর: "
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(billNoLabel, rightX, 338f, paint)
        paint.color = crimsonRed
        canvas.drawText(data.billNumber, rightX + paint.measureText(billNoLabel), 338f, paint)

        paint.color = Color.rgb(30, 41, 59)
        val dateLabel = "তারিখ: "
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(dateLabel, rightX, 368f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(data.dateFormatted, rightX + paint.measureText(dateLabel), 368f, paint)

        val modeLabel = "পেমেন্ট: "
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(modeLabel, rightX, 394f, paint)
        if (data.paymentType == "CASH") {
            paint.color = cashGreen
            canvas.drawText("নগদ (CASH - PAID)", rightX + paint.measureText(modeLabel), 394f, paint)
        } else {
            paint.color = crimsonRed
            canvas.drawText("বাকি (CREDIT - DUE)", rightX + paint.measureText(modeLabel), 394f, paint)
        }

        // 7. THE ITEMIZED TABLE
        val tableTop = 415f
        val tableBottom = 960f
        val tableLeft = 45f
        val tableRight = MEMO_WIDTH - 45f

        // Columns X markers:
        // Col 1: Sl No (45 to 115)
        // Col 2: Particulars (115 to 600)
        // Col 3: Qty (600 to 710)
        // Col 4: Rate (710 to 820)
        // Col 5: Amount (820 to 955)
        val colSl = 115f
        val colDesc = 600f
        val colQty = 710f
        val colRate = 820f

        // Table Outer Border
        paint.style = Paint.Style.STROKE
        paint.color = borderDark
        paint.strokeWidth = 2f
        canvas.drawRect(tableLeft, tableTop, tableRight, tableBottom, paint)

        // Header Background
        val headerH = 46f
        val headerRect = RectF(tableLeft, tableTop, tableRight, tableTop + headerH)
        paint.style = Paint.Style.FILL
        paint.color = tableBgHeader
        canvas.drawRect(headerRect, paint)

        // Header Line
        paint.style = Paint.Style.STROKE
        paint.color = borderDark
        paint.strokeWidth = 1.8f
        canvas.drawLine(tableLeft, tableTop + headerH, tableRight, tableTop + headerH, paint)

        // Header Vertical Column Lines
        canvas.drawLine(colSl, tableTop, colSl, tableBottom, paint)
        canvas.drawLine(colDesc, tableTop, colDesc, tableBottom, paint)
        canvas.drawLine(colQty, tableTop, colQty, tableBottom, paint)
        canvas.drawLine(colRate, tableTop, colRate, tableBottom, paint)

        // Header Column Labels
        paint.style = Paint.Style.FILL
        paint.color = navyDark
        paint.textSize = 19f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("ক্রঃ নং", (tableLeft + colSl) / 2f, tableTop + 31f, paint)

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("কাজের বিবরণ (Particulars)", tableSlPad(colSl), tableTop + 31f, paint)

        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("পরিমাণ", (colDesc + colQty) / 2f, tableTop + 31f, paint)
        canvas.drawText("দর (₹)", (colQty + colRate) / 2f, tableTop + 31f, paint)
        canvas.drawText("মোট টাকা (₹)", (colRate + tableRight) / 2f, tableTop + 31f, paint)

        // 8. DATA ROWS
        val rowHeight = 49f
        val maxRows = 10
        var currentY = tableTop + headerH

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 18.5f
        paint.color = Color.rgb(15, 23, 42)

        val dividerPaint = Paint().apply {
            color = lightGrayLine
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        for (i in 0 until maxRows) {
            val item = data.items.getOrNull(i)
            val textY = currentY + 32f

            if (item != null) {
                // Sl No
                paint.textAlign = Paint.Align.CENTER
                val slNoStr = BanglaNumberUtil.toBangla((i + 1).toString())
                canvas.drawText(slNoStr, (tableLeft + colSl) / 2f, textY, paint)

                // Description
                paint.textAlign = Paint.Align.LEFT
                val maxDescW = colDesc - colSl - 20f
                val descText = truncateText(item.serviceName, paint, maxDescW)
                canvas.drawText(descText, colSl + 12f, textY, paint)

                // Qty
                paint.textAlign = Paint.Align.CENTER
                val qtyStr = if (item.quantity % 1.0 == 0.0) {
                    BanglaNumberUtil.toBangla(item.quantity.toInt().toString())
                } else {
                    BanglaNumberUtil.toBangla(String.format(Locale.ENGLISH, "%.1f", item.quantity))
                }
                canvas.drawText(qtyStr, (colDesc + colQty) / 2f, textY, paint)

                // Rate
                paint.textAlign = Paint.Align.RIGHT
                val rateStr = "₹" + BanglaNumberUtil.toBangla(item.unitRate.toInt().toString())
                canvas.drawText(rateStr, colRate - 12f, textY, paint)

                // Line Total
                paint.textAlign = Paint.Align.RIGHT
                val totalStr = "₹" + BanglaNumberUtil.toBangla(item.lineTotal.toInt().toString())
                canvas.drawText(totalStr, tableRight - 14f, textY, paint)
            }

            // Draw horizontal row line
            currentY += rowHeight
            if (i < maxRows - 1) {
                canvas.drawLine(tableLeft, currentY, tableRight, currentY, dividerPaint)
            }
        }

        // 9. TOTALS & SUMMARY SECTION (y=970 to 1100)
        val summaryTop = 970f
        val summaryBox = RectF(tableLeft, summaryTop, tableRight, 1100f)

        // Draw Summary Box Border
        paint.style = Paint.Style.STROKE
        paint.color = borderDark
        paint.strokeWidth = 1.5f
        canvas.drawRoundRect(summaryBox, 6f, 6f, paint)

        // In Words on Left (45 to 580)
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("কথায় (In Words):", 60f, summaryTop + 36f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.rgb(71, 85, 105)
        val inWordsBn = convertAmountToBanglaWords(data.grandTotal.toLong())
        canvas.drawText(inWordsBn, 60f, summaryTop + 68f, paint)

        // Notes / Payment mode reminder
        paint.textSize = 16f
        if (data.notes.isNotBlank()) {
            canvas.drawText("নোট: ${data.notes}", 60f, summaryTop + 104f, paint)
        } else {
            val statusNote = if (data.paymentType == "CASH") "✓ নগদ পেমেন্ট সম্পূর্ণ পরিশোধিত।" else "⚠ বাকি বিল - অনুগ্রহ করে যথাশীঘ্র বকেয়া পরিশোধ করুন।"
            paint.color = if (data.paymentType == "CASH") cashGreen else crimsonRed
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(statusNote, 60f, summaryTop + 104f, paint)
        }

        // Mid vertical separator in summary
        val sumMidX = 580f
        paint.style = Paint.Style.STROKE
        paint.color = lightGrayLine
        canvas.drawLine(sumMidX, summaryTop, sumMidX, 1100f, paint)

        // Right side: Subtotal, Discount, Grand Total
        paint.style = Paint.Style.FILL
        paint.textSize = 19f

        // Subtotal
        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.rgb(71, 85, 105)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("সাবটোটাল (Subtotal):", sumMidX + 20f, summaryTop + 32f, paint)
        paint.textAlign = Paint.Align.RIGHT
        paint.color = Color.rgb(30, 41, 59)
        canvas.drawText("₹" + BanglaNumberUtil.toBangla(data.subtotal.toInt().toString()), tableRight - 20f, summaryTop + 32f, paint)

        // Discount (if any)
        if (data.discount > 0.0) {
            paint.textAlign = Paint.Align.LEFT
            paint.color = cashGreen
            canvas.drawText("ছাড় (Discount):", sumMidX + 20f, summaryTop + 62f, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("- ₹" + BanglaNumberUtil.toBangla(data.discount.toInt().toString()), tableRight - 20f, summaryTop + 62f, paint)
        }

        // Grand Total Row (Highlighted Badge)
        val totalBadge = RectF(sumMidX + 10f, summaryTop + 76f, tableRight - 10f, 1092f)
        paint.style = Paint.Style.FILL
        paint.color = if (data.paymentType == "CASH") Color.rgb(240, 253, 244) else Color.rgb(254, 242, 242)
        canvas.drawRoundRect(totalBadge, 6f, 6f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = if (data.paymentType == "CASH") cashGreen else crimsonRed
        paint.strokeWidth = 2f
        canvas.drawRoundRect(totalBadge, 6f, 6f, paint)

        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 21f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = navyDark
        canvas.drawText("সর্বমোট প্রদেয় বিল:", sumMidX + 22f, summaryTop + 108f, paint)

        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 24f
        paint.color = if (data.paymentType == "CASH") cashGreen else crimsonRed
        canvas.drawText("₹ " + BanglaNumberUtil.toBangla(data.grandTotal.toInt().toString()), tableRight - 22f, summaryTop + 108f, paint)

        // 10. DYNAMIC SCANNABLE UPI QR CODE BOX (y=1115 to 1355)
        val qrBox = RectF(tableLeft, 1115f, 440f, 1365f)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(qrBox, 8f, 8f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(203, 213, 225)
        paint.strokeWidth = 1.2f
        canvas.drawRoundRect(qrBox, 8f, 8f, paint)

        // Generate genuine UPI QR
        // UPI URI format: upi://pay?pa=7718489@ybl&pn=Telecom%20Cyber%20Cafe&am=<grandTotal>&cu=INR&tn=Bill%20<billNo>
        val upiAmountStr = String.format(Locale.ENGLISH, "%.2f", data.grandTotal)
        val billEncoded = Uri.encode(data.billNumber)
        val upiUri = "upi://pay?pa=$MERCHANT_UPI_ID&pn=Telecom%20Cyber%20Cafe&am=$upiAmountStr&cu=INR&tn=Bill%20$billEncoded"

        val qrSize = 170
        val qrBitmap = QrCodeGenerator.generateUpiQr(upiUri, qrSize)

        if (qrBitmap != null) {
            val qrLeft = tableLeft + (440f - tableLeft - qrSize) / 2f
            canvas.drawBitmap(qrBitmap, qrLeft, 1128f, null)
        }

        // Under QR label
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = navyDark
        canvas.drawText("ইউপিআই স্ক্যান করে পেমেন্ট করুন", (tableLeft + 440f) / 2f, 1318f, paint)

        paint.textSize = 14.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.rgb(71, 85, 105)
        canvas.drawText("UPI ID: $MERCHANT_UPI_ID", (tableLeft + 440f) / 2f, 1342f, paint)

        // Status Stamp next to / above QR
        if (data.paymentType == "CASH") {
            drawStamp(canvas, "পরিশোধিত (PAID)", cashGreen, 320f, 1170f, -12f)
        } else {
            drawStamp(canvas, "বকেয়া (DUE)", crimsonRed, 320f, 1170f, -12f)
        }

        // 11. SIGNATURES & AUTHORIZATION (Right side, y=1115 to 1365)
        val sigX = 740f

        // Customer Signature Line
        paint.style = Paint.Style.STROKE
        paint.color = lightGrayLine
        paint.strokeWidth = 1.2f
        canvas.drawLine(520f, 1220f, 700f, 1220f, paint)

        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 16f
        paint.color = Color.rgb(71, 85, 105)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("ক্রেতার স্বাক্ষর", 610f, 1242f, paint)

        // Shop Authorized Signatory Line
        canvas.drawLine(780f, 1310f, tableRight - 20f, 1310f, paint)

        paint.color = navyDark
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 16.5f
        canvas.drawText("টেলিকম সাইবার ক্যাফে পক্ষে", (780f + tableRight - 20f) / 2f, 1332f, paint)

        paint.color = Color.rgb(100, 116, 139)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 14f
        canvas.drawText("(Authorized Signatory)", (780f + tableRight - 20f) / 2f, 1352f, paint)

        // 12. FOOTER LEGAL & SHOP WISHES
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 14f
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText("* ভুলত্রুটি সংশোধনযোগ্য (E. & O.E.)", 45f, 1395f, paint)

        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 16f
        paint.color = navyDark
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("★ আমাদের সেবায় আমরা আনন্দিত • ধন্যবাদ! আবার আসবেন ★", MEMO_WIDTH / 2f, 1400f, paint)

        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 13f
        paint.color = Color.rgb(148, 163, 184)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Amar Dokan Xerox & Service Hisab", MEMO_WIDTH - 45f, 1395f, paint)

        return bitmap
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
     * Generates a high-quality PDF file of the cash memo and writes it to a file.
     */
    fun generatePdfFile(context: Context, data: MemoData): File {
        val bitmap = generateMemoBitmap(data)

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(MEMO_WIDTH, MEMO_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        pdfDocument.finishPage(page)

        val sanitizedBillNo = data.billNumber.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val fileName = "CashMemo_${sanitizedBillNo}_${System.currentTimeMillis()}.pdf"
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
        val bitmap = generateMemoBitmap(data)
        val sanitizedBillNo = data.billNumber.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val fileName = "CashMemo_${sanitizedBillNo}_${System.currentTimeMillis()}.jpg"
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

                    val bitmap = generateMemoBitmap(data)
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

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (asPdf) "application/pdf" else "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "টেলিকম সাইবার ক্যাফে - ক্যাশ মেমো (${data.billNumber})")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "🧾 *টেলিকম সাইবার ক্যাফে ও জেরক্স*\n" +
                            "বিল নং: ${data.billNumber}\n" +
                            "গ্রাহক: ${data.customerName}\n" +
                            "তারিখ: ${data.dateFormatted}\n" +
                            "সর্বমোট বিল: ₹${data.grandTotal.toInt()}\n" +
                            "পেমেন্ট: ${if (data.paymentType == "CASH") "পরিশোধিত" else "বকেয়া"}\n" +
                            "ধন্যবাদ! আবার আসবেন।"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "ক্যাশ মেমো শেয়ার করুন")
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "শেয়ার করতে সমস্যা হয়েছে: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
