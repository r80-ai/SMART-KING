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
    fun generateMemoBitmap(data: MemoData): Bitmap {
        return if (data.language.uppercase() == "EN") {
            generateEnglishMemoBitmap(data)
        } else {
            generateBengaliMemoBitmap(data)
        }
    }

    /**
     * Generates a high-resolution Bitmap of the complete Bengali Cash Memo.
     */
    fun generateBengaliMemoBitmap(data: MemoData): Bitmap {
        return renderExactCashMemo(data, isEn = false)
    }

    /**
     * Generates a high-resolution Bitmap of the complete English Cash Memo.
     */
    fun generateEnglishMemoBitmap(data: MemoData): Bitmap {
        return renderExactCashMemo(data, isEn = true)
    }

    /**
     * Master exact cash memo reproduction renderer matching the uploaded reference image.
     */
    private fun renderExactCashMemo(data: MemoData, isEn: Boolean): Bitmap {
        val bitmap = Bitmap.createBitmap(MEMO_WIDTH, MEMO_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Base Background with Sky Blue Top
        canvas.drawColor(Color.WHITE)
        val skyShader = LinearGradient(0f, 0f, 0f, 480f, Color.rgb(186, 230, 253), Color.WHITE, Shader.TileMode.CLAMP)
        val bgPaint = Paint().apply { shader = skyShader }
        canvas.drawRect(0f, 0f, MEMO_WIDTH.toFloat(), 480f, bgPaint)

        val paint = Paint().apply {
            isAntiAlias = true
            isSubpixelText = true
        }

        // 2. VIBRANT RAINBOW BORDER
        val rainbowColors = intArrayOf(
            Color.rgb(239, 68, 68),   // Red
            Color.rgb(249, 115, 22),  // Orange
            Color.rgb(250, 204, 21),  // Yellow
            Color.rgb(34, 197, 94),   // Green
            Color.rgb(6, 182, 212),   // Cyan
            Color.rgb(59, 130, 246),  // Blue
            Color.rgb(168, 85, 247)   // Purple
        )
        for (idx in rainbowColors.indices) {
            val offset = idx * 2.2f
            paint.style = Paint.Style.STROKE
            paint.color = rainbowColors[idx]
            paint.strokeWidth = 2.4f
            canvas.drawRect(offset + 3f, offset + 3f, MEMO_WIDTH - offset - 3f, MEMO_HEIGHT - offset - 3f, paint)
        }

        // Inner clean boundary
        paint.color = Color.rgb(13, 27, 42)
        paint.strokeWidth = 2.5f
        canvas.drawRect(18f, 18f, MEMO_WIDTH - 18f, MEMO_HEIGHT - 18f, paint)

        // 3. TOP HEADER
        // Left: Computer / Monitor illustration
        val pcLeft = 32f
        val pcTop = 32f
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawRoundRect(RectF(pcLeft, pcTop, pcLeft + 155f, pcTop + 105f), 8f, 8f, paint) // Monitor frame
        paint.color = Color.rgb(37, 99, 235)
        canvas.drawRect(RectF(pcLeft + 8f, pcTop + 8f, pcLeft + 147f, pcTop + 92f), paint) // Blue screen
        // Windows Desktop graphic
        paint.color = Color.rgb(255, 255, 255)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Windows", pcLeft + 20f, pcTop + 30f, paint)
        // Stand
        paint.color = Color.rgb(71, 85, 105)
        canvas.drawRect(RectF(pcLeft + 65f, pcTop + 105f, pcLeft + 90f, pcTop + 120f), paint)
        canvas.drawRoundRect(RectF(pcLeft + 45f, pcTop + 120f, pcLeft + 110f, pcTop + 128f), 4f, 4f, paint)
        // PC Tower
        paint.color = Color.rgb(30, 41, 59)
        canvas.drawRoundRect(RectF(pcLeft + 165f, pcTop + 25f, pcLeft + 215f, pcTop + 130f), 6f, 6f, paint)
        paint.color = Color.rgb(59, 130, 246)
        canvas.drawCircle(pcLeft + 190f, pcTop + 45f, 6f, paint)
        // Printer & Photo Print
        paint.color = Color.rgb(51, 65, 85)
        canvas.drawRoundRect(RectF(pcLeft + 145f, pcTop + 135f, pcLeft + 225f, pcTop + 185f), 6f, 6f, paint)
        paint.color = Color.rgb(249, 115, 22)
        canvas.drawRect(RectF(pcLeft + 160f, pcTop + 175f, pcLeft + 210f, pcTop + 205f), paint) // Colorful photo

        // Center: Title "TELECOM" / "টেলিকম"
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        // Shadow & Outline
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f
        paint.color = Color.rgb(13, 27, 42) // Dark Navy
        paint.textSize = 66f
        val titleText = if (isEn) "TELECOM" else "টেলিকম"
        canvas.drawText(titleText, 512f, 102f, paint)

        // 3D Yellow-Orange Title Fill
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(255, 179, 0) // Amber-Yellow
        canvas.drawText(titleText, 512f, 102f, paint)

        // Second Line: Red Rounded Panel with "CYBER CAFE" / "সাইবার ক্যাফ"
        val subPill = RectF(330f, 118f, 694f, 168f)
        paint.color = Color.rgb(211, 47, 47) // Glossy Red
        canvas.drawRoundRect(subPill, 25f, 25f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.WHITE
        paint.strokeWidth = 2.5f
        canvas.drawRoundRect(subPill, 25f, 25f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textSize = 34f
        val subtitleText = if (isEn) "CYBER CAFE" else "সাইবার ক্যাফ"
        canvas.drawText(subtitleText, 512f, 155f, paint)

        // Subtitle Banner (Dark Blue with Yellow Text)
        val bannerRect = RectF(245f, 178f, 779f, 208f)
        paint.color = Color.rgb(13, 32, 64)
        canvas.drawRoundRect(bannerRect, 6f, 6f, paint)
        paint.color = Color.rgb(255, 235, 59) // Bright Yellow
        paint.textSize = 14.5f
        val bannerText = if (isEn) {
            "Print • Photocopy • Online Form • Mobile Recharge • Other Services"
        } else {
            "প্রিন্ট • ফটোকপি • অনলাইন সেবা • মোবাইল রিচার্জ • অন্যান্য সেবা"
        }
        canvas.drawText(bannerText, 512f, 199f, paint)

        // Right Header Artwork (PDF, Aadhaar, Phone, Passport Photos)
        val rightX = 800f
        val rightY = 32f
        // PDF doc
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRoundRect(RectF(rightX, rightY, rightX + 60f, rightY + 80f), 4f, 4f, paint)
        paint.color = Color.rgb(220, 38, 38)
        canvas.drawRect(RectF(rightX, rightY, rightX + 60f, rightY + 22f), paint)
        paint.color = Color.WHITE
        paint.textSize = 11f
        canvas.drawText("PDF", rightX + 30f, rightY + 16f, paint)
        // Aadhaar illustration
        paint.color = Color.WHITE
        canvas.drawRoundRect(RectF(rightX + 70f, rightY + 10f, rightX + 155f, rightY + 75f), 4f, 4f, paint)
        paint.color = Color.rgb(234, 88, 12)
        canvas.drawCircle(rightX + 112f, rightY + 32f, 12f, paint) // Aadhaar sun
        paint.color = Color.rgb(22, 101, 52)
        paint.textSize = 9f
        canvas.drawText("AADHAAR", rightX + 112f, rightY + 54f, paint)
        // Passport Photo Grid Sheet
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(RectF(rightX + 30f, rightY + 85f, rightX + 130f, rightY + 145f), 4f, 4f, paint)
        paint.color = Color.rgb(59, 130, 246)
        for (r in 0..1) {
            for (c in 0..2) {
                canvas.drawRect(
                    rightX + 36f + c * 28f,
                    rightY + 90f + r * 25f,
                    rightX + 58f + c * 28f,
                    rightY + 112f + r * 25f,
                    paint
                )
            }
        }
        // Smartphone & Pen Stand
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawRoundRect(RectF(rightX + 140f, rightY + 80f, rightX + 175f, rightY + 155f), 6f, 6f, paint)
        paint.color = Color.rgb(56, 189, 248)
        canvas.drawRect(RectF(rightX + 144f, rightY + 88f, rightX + 171f, rightY + 145f), paint)

        // 4. EXACT 11 SERVICE CARDS ROW (y = 216f to 308f)
        val cardWidth = 83f
        val cardHeight = 88f
        val cardSpacing = 5.6f
        val cardStartX = 24f

        val enServiceLabels = listOf(
            "Print",
            "Photocopy",
            "Scan / PDF",
            "Online Form",
            "Aadhaar Card",
            "Pan Card",
            "Mobile Recharge",
            "ELECTRICITY BILL",
            "Ration Card",
            "Electricity Bill",
            "Other Services"
        )

        val bnServiceLabels = listOf(
            "প্রিন্ট",
            "ফটোকপি",
            "স্ক্যান / PDF",
            "অনলাইন ফর্ম",
            "আধার কার্ডের কাজ হয়",
            "Pan Card এর কাজ",
            "মোবাইল রিচার্জ",
            "ELECTRICITY BILL",
            "রেশন কার্ডের কাজ",
            "বিদ্যুৎ বিল",
            "অন্যান্য সেবা"
        )

        val cardBgColors = intArrayOf(
            Color.rgb(219, 234, 254), // blue light
            Color.rgb(254, 226, 226), // red light
            Color.rgb(220, 252, 231), // green light
            Color.rgb(254, 243, 199), // yellow light
            Color.rgb(243, 232, 255), // purple light
            Color.rgb(255, 237, 213), // orange light
            Color.rgb(252, 231, 243), // pink light
            Color.rgb(224, 231, 255), // indigo light
            Color.rgb(204, 251, 241), // teal light
            Color.rgb(254, 240, 138), // warm yellow
            Color.rgb(241, 245, 249)  // slate light
        )

        val cardBannerColors = intArrayOf(
            Color.rgb(37, 99, 235),
            Color.rgb(220, 38, 38),
            Color.rgb(22, 163, 74),
            Color.rgb(217, 119, 6),
            Color.rgb(147, 51, 234),
            Color.rgb(234, 88, 12),
            Color.rgb(219, 39, 119),
            Color.rgb(79, 70, 229),
            Color.rgb(13, 148, 136),
            Color.rgb(202, 138, 4),
            Color.rgb(71, 85, 105)
        )

        for (i in 0..10) {
            val cX = cardStartX + i * (cardWidth + cardSpacing)
            val cY = 216f
            val cRect = RectF(cX, cY, cX + cardWidth, cY + cardHeight)

            // Card body
            paint.style = Paint.Style.FILL
            paint.color = cardBgColors[i]
            canvas.drawRoundRect(cRect, 8f, 8f, paint)

            paint.style = Paint.Style.STROKE
            paint.color = cardBannerColors[i]
            paint.strokeWidth = 1.5f
            canvas.drawRoundRect(cRect, 8f, 8f, paint)

            // Upper mini illustration icon
            paint.style = Paint.Style.FILL
            paint.color = cardBannerColors[i]
            val iconCx = cX + cardWidth / 2f
            val iconCy = cY + 28f
            canvas.drawCircle(iconCx, iconCy, 16f, paint)
            paint.color = Color.WHITE
            canvas.drawCircle(iconCx, iconCy, 7f, paint)

            // Bottom Colored Label Banner
            val bannerH = 28f
            val bRect = RectF(cX, cY + cardHeight - bannerH, cX + cardWidth, cY + cardHeight)
            paint.color = cardBannerColors[i]
            canvas.drawRoundRect(bRect, 6f, 6f, paint)

            // Label text
            paint.color = Color.WHITE
            paint.textSize = if (isEn) 10.5f else 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val lbl = if (isEn) enServiceLabels[i] else bnServiceLabels[i]
            val maxLblW = cardWidth - 6f
            val truncLbl = truncateText(lbl, paint, maxLblW)
            canvas.drawText(truncLbl, cX + cardWidth / 2f, cY + cardHeight - 9f, paint)
        }

        // 5. CONTACT & OWNER STRIP (y = 314f to 366f)
        val stripRect = RectF(24f, 314f, 1000f, 366f)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(13, 27, 42) // Dark Navy
        canvas.drawRoundRect(stripRect, 26f, 26f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.WHITE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(stripRect, 26f, 26f, paint)

        // Left Section: Yellow Badge with Prop Name
        val propBadge = RectF(28f, 318f, 275f, 362f)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(255, 215, 0) // Yellow
        canvas.drawRoundRect(propBadge, 22f, 22f, paint)

        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.rgb(13, 27, 42)
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val propText = if (isEn) "Prop : Rafikul Gayen" else "প্রোঃ রফিকুল গায়েন"
        canvas.drawText(propText, 45f, 345f, paint)

        // Center Section: Red Pin & Address
        paint.color = Color.rgb(239, 68, 68)
        canvas.drawCircle(298f, 340f, 6f, paint) // Map pin
        paint.color = Color.WHITE
        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val addressText = if (isEn) {
            "Chorgeria, Konarpur, Sihar, Kotulpur, Bankura, West Bengal - 722161"
        } else {
            "চোরগেড়িয়া, সিহড়, কোতুলপুর, বাঁকুড়া, পশ্চিমবঙ্গ - 722161"
        }
        canvas.drawText(addressText, 312f, 346f, paint)

        // Right Section: WhatsApp & Yellow Mobile
        paint.color = Color.rgb(37, 211, 102) // WhatsApp Green
        canvas.drawCircle(825f, 340f, 10f, paint)
        paint.color = Color.rgb(255, 235, 59) // Yellow Phone Text
        paint.textSize = 21f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("7718489405", 845f, 347f, paint)

        // 6. CASH MEMO TITLE PANEL (y = 376f to 434f)
        val memoPanel = RectF(280f, 376f, 744f, 434f)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(13, 71, 161) // Deep Royal Blue
        canvas.drawRoundRect(memoPanel, 29f, 29f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.WHITE
        paint.strokeWidth = 2.5f
        canvas.drawRoundRect(memoPanel, 29f, 29f, paint)

        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        if (isEn) {
            paint.color = Color.rgb(255, 215, 0)
            paint.textSize = 36f
            canvas.drawText("CASH MEMO", 512f, 418f, paint)
        } else {
            paint.color = Color.rgb(255, 215, 0)
            paint.textSize = 32f
            canvas.drawText("নগদ বিক্রয় রসিদ", 512f, 408f, paint)
            // Small red badge with "CASH MEMO"
            val bnPill = RectF(430f, 412f, 594f, 430f)
            paint.color = Color.rgb(220, 38, 38)
            canvas.drawRoundRect(bnPill, 8f, 8f, paint)
            paint.color = Color.WHITE
            paint.textSize = 12f
            canvas.drawText("CASH MEMO", 512f, 425f, paint)
        }

        // 7. RECEIPT NUMBER & DATE (y = 444f to 486f)
        // Red Receipt Number Block
        val numPanel = RectF(24f, 444f, 96f, 486f)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(211, 47, 47)
        canvas.drawRoundRect(numPanel, 6f, 6f, paint)
        paint.color = Color.WHITE
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(if (isEn) "No :" else "নং :", 60f, 471f, paint)

        val numBox = RectF(98f, 444f, 260f, 486f)
        paint.color = Color.WHITE
        canvas.drawRect(numBox, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(211, 47, 47)
        paint.strokeWidth = 2f
        canvas.drawRect(numBox, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(211, 47, 47)
        paint.textSize = 22f
        val billNumStr = if (data.billNumber.isNotBlank()) data.billNumber else "0001"
        canvas.drawText(billNumStr, 179f, 473f, paint)

        // Blue Date Block
        val datePanel = RectF(740f, 444f, 1000f, 486f)
        paint.color = Color.rgb(21, 101, 192)
        canvas.drawRoundRect(datePanel, 6f, 6f, paint)
        paint.color = Color.WHITE
        paint.textSize = 17f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(if (isEn) "📅 Date :" else "📅 তারিখ :", 755f, 471f, paint)
        // Date White Box
        val dateWhiteBox = RectF(845f, 448f, 994f, 482f)
        paint.color = Color.WHITE
        canvas.drawRoundRect(dateWhiteBox, 4f, 4f, paint)
        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 16f
        paint.textAlign = Paint.Align.CENTER
        val dateDisplay = if (data.dateFormatted.isNotBlank()) data.dateFormatted else if (isEn) "___/___/20___" else "___/___/২০___"
        canvas.drawText(dateDisplay, 920f, 471f, paint)

        // 8. CUSTOMER INFORMATION FIELDS (y = 496f to 534f)
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        // Customer Name
        paint.color = Color.rgb(37, 99, 235)
        canvas.drawCircle(36f, 514f, 8f, paint) // User icon
        paint.color = Color.rgb(15, 23, 42)
        val nameLabel = if (isEn) "Customer Name : " else "গ্রাহকের নাম : "
        canvas.drawText(nameLabel, 52f, 520f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val nameVal = if (data.customerName.isNotBlank()) data.customerName else ".................................................."
        canvas.drawText(nameVal, 52f + paint.measureText(nameLabel) + 4f, 520f, paint)

        // Customer Mobile
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.rgb(37, 99, 235)
        canvas.drawCircle(596f, 514f, 8f, paint) // Phone icon
        paint.color = Color.rgb(15, 23, 42)
        val phoneLabel = if (isEn) "Mobile No : " else "মোবাইল নং : "
        canvas.drawText(phoneLabel, 612f, 520f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val phoneVal = if (data.customerPhone.isNotBlank()) data.customerPhone else "......................................................"
        canvas.drawText(phoneVal, 612f + paint.measureText(phoneLabel) + 4f, 520f, paint)

        // 9. INVOICE TABLE (5 COLUMNS, EXACTLY 15 ROWS) (y = 540f to 1205f)
        val tableLeft = 24f
        val tableRight = 1000f
        val tableTop = 540f
        val tableBottom = 1205f

        val col1End = 105f
        val col2End = 525f
        val col3End = 655f
        val col4End = 825f
        val col5End = 1000f

        val headerH = 42f

        // Column Header Colors
        val colHeaderColors = intArrayOf(
            Color.rgb(21, 101, 192),  // Col 1: Royal Blue
            Color.rgb(216, 27, 96),   // Col 2: Magenta / Pink
            Color.rgb(46, 125, 50),   // Col 3: Green
            Color.rgb(239, 108, 0),   // Col 4: Orange
            Color.rgb(106, 27, 154)   // Col 5: Purple
        )

        // Draw Colored Header Cells
        paint.style = Paint.Style.FILL
        // Col 1
        paint.color = colHeaderColors[0]
        canvas.drawRect(tableLeft, tableTop, col1End, tableTop + headerH, paint)
        // Col 2
        paint.color = colHeaderColors[1]
        canvas.drawRect(col1End, tableTop, col2End, tableTop + headerH, paint)
        // Col 3
        paint.color = colHeaderColors[2]
        canvas.drawRect(col2End, tableTop, col3End, tableTop + headerH, paint)
        // Col 4
        paint.color = colHeaderColors[3]
        canvas.drawRect(col3End, tableTop, col4End, tableTop + headerH, paint)
        // Col 5
        paint.color = colHeaderColors[4]
        canvas.drawRect(col4End, tableTop, col5End, tableTop + headerH, paint)

        // Header Labels (Bold White, Centered)
        paint.color = Color.WHITE
        paint.textSize = 17f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER

        canvas.drawText(if (isEn) "Sl. No." else "ক্রম নং", (tableLeft + col1End) / 2f, tableTop + 27f, paint)
        canvas.drawText(if (isEn) "Description" else "বিবরণী", (col1End + col2End) / 2f, tableTop + 27f, paint)
        canvas.drawText(if (isEn) "Quantity" else "পরিমাণ", (col2End + col3End) / 2f, tableTop + 27f, paint)
        canvas.drawText(if (isEn) "Rate (Rs)" else "একক মূল্য (টাকা)", (col3End + col4End) / 2f, tableTop + 27f, paint)
        canvas.drawText(if (isEn) "Amount (Rs)" else "মোট টাকা (টাকা)", (col4End + col5End) / 2f, tableTop + 27f, paint)

        // EXACTLY 15 ROWS (rowHeight = 44.2f)
        val rowCount = 15
        val rowHeight = (tableBottom - (tableTop + headerH)) / rowCount.toFloat()
        val paleCyan = Color.rgb(225, 245, 254)
        val gridBlue = Color.rgb(66, 165, 245)

        for (r in 0 until rowCount) {
            val rY = tableTop + headerH + r * rowHeight

            // Alternating Row Background
            paint.style = Paint.Style.FILL
            paint.color = if (r % 2 == 1) paleCyan else Color.WHITE
            canvas.drawRect(tableLeft, rY, tableRight, rY + rowHeight, paint)

            // Row Data or Blank
            val item = data.items.getOrNull(r)
            val textBaseY = rY + rowHeight * 0.68f

            // Sl No
            paint.color = Color.rgb(15, 23, 42)
            paint.textSize = 16.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER
            val slNoStr = if (isEn) (r + 1).toString() else BanglaNumberUtil.toBangla((r + 1).toString())
            canvas.drawText(slNoStr, (tableLeft + col1End) / 2f, textBaseY, paint)

            if (item != null) {
                // Description
                paint.textAlign = Paint.Align.LEFT
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                val desc = truncateText(item.serviceName, paint, (col2End - col1End) - 20f)
                canvas.drawText(desc, col1End + 12f, textBaseY, paint)

                // Quantity
                paint.textAlign = Paint.Align.CENTER
                val qtyStr = if (isEn) {
                    if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                } else {
                    if (item.quantity % 1.0 == 0.0) BanglaNumberUtil.toBangla(item.quantity.toInt().toString()) else BanglaNumberUtil.toBangla(item.quantity.toString())
                }
                canvas.drawText(qtyStr, (col2End + col3End) / 2f, textBaseY, paint)

                // Rate
                paint.textAlign = Paint.Align.RIGHT
                val rateStr = if (isEn) item.unitRate.toInt().toString() else BanglaNumberUtil.toBangla(item.unitRate.toInt().toString())
                canvas.drawText(rateStr, col4End - 16f, textBaseY, paint)

                // Amount
                val amtStr = if (isEn) item.lineTotal.toInt().toString() else BanglaNumberUtil.toBangla(item.lineTotal.toInt().toString())
                canvas.drawText(amtStr, col5End - 16f, textBaseY, paint)
            }
        }

        // Table Grid Lines (Thin Bright Blue)
        paint.style = Paint.Style.STROKE
        paint.color = gridBlue
        paint.strokeWidth = 1.3f

        // Horizontal Row Lines
        for (r in 0..rowCount) {
            val rY = tableTop + headerH + r * rowHeight
            canvas.drawLine(tableLeft, rY, tableRight, rY, paint)
        }
        // Vertical Column Lines
        canvas.drawLine(col1End, tableTop, col1End, tableBottom, paint)
        canvas.drawLine(col2End, tableTop, col2End, tableBottom, paint)
        canvas.drawLine(col3End, tableTop, col3End, tableBottom, paint)
        canvas.drawLine(col4End, tableTop, col4End, tableBottom, paint)

        // Outer Table Border (Rounded)
        paint.color = Color.rgb(21, 101, 192)
        paint.strokeWidth = 2.2f
        canvas.drawRoundRect(RectF(tableLeft, tableTop, tableRight, tableBottom), 8f, 8f, paint)

        // 10. BOTTOM-LEFT PAYMENT SECTION (y = 1215f to 1480f)
        val payBox = RectF(24f, 1215f, 570f, 1480f)
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawRoundRect(payBox, 14f, 14f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(220, 38, 38)
        paint.strokeWidth = 2.5f
        canvas.drawRoundRect(payBox, 14f, 14f, paint)

        // Scannable Dynamic UPI QR Code
        val qrAmount = String.format(Locale.ENGLISH, "%.2f", data.grandTotal)
        val billEncoded = Uri.encode(data.billNumber)
        val upiUri = "upi://pay?pa=$MERCHANT_UPI_ID&pn=Telecom%20Cyber%20Cafe&am=$qrAmount&cu=INR&tn=Bill%20$billEncoded"
        val qrBmp = QrCodeGenerator.generateUpiQr(upiUri, 185)
        if (qrBmp != null) {
            canvas.drawBitmap(qrBmp, 38f, 1235f, null)
        }
        // QR Code Frame
        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(15, 23, 42)
        paint.strokeWidth = 1.5f
        canvas.drawRoundRect(RectF(36f, 1233f, 226f, 1423f), 8f, 8f, paint)

        // Payment Here Red Banner
        val payBanner = RectF(240f, 1230f, 555f, 1276f)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(220, 38, 38)
        canvas.drawRoundRect(payBanner, 12f, 12f, paint)

        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 23f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.rgb(255, 235, 59) // Yellow text
        canvas.drawText(if (isEn) "Payment Here" else "পেমেন্ট করুন", 397f, 1262f, paint)

        // UPI / QR Code Text
        paint.color = Color.rgb(13, 27, 42)
        paint.textSize = 19f
        canvas.drawText("UPI / QR Code", 397f, 1310f, paint)

        // Logos: GPay, PhonePe, Paytm
        // GPay badge
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRoundRect(RectF(245f, 1335f, 335f, 1375f), 6f, 6f, paint)
        paint.color = Color.rgb(66, 133, 244)
        paint.textSize = 14f
        canvas.drawText("GPay", 290f, 1360f, paint)

        // PhonePe badge
        paint.color = Color.rgb(103, 58, 183)
        canvas.drawRoundRect(RectF(350f, 1335f, 445f, 1375f), 6f, 6f, paint)
        paint.color = Color.WHITE
        canvas.drawText("PhonePe", 397f, 1360f, paint)

        // Paytm badge
        paint.color = Color.rgb(0, 186, 242)
        canvas.drawRoundRect(RectF(460f, 1335f, 550f, 1375f), 6f, 6f, paint)
        paint.color = Color.WHITE
        canvas.drawText("Paytm", 505f, 1360f, paint)

        // 11. BOTTOM-RIGHT TOTALS PANEL (3 ROWS) (y = 1215f to 1480f)
        val totalsLeft = 585f
        val totalsRight = 1000f

        // Row 1: Total Amount (Light Pink)
        val r1 = RectF(totalsLeft, 1215f, totalsRight, 1295f)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(252, 228, 236) // Light Pink
        canvas.drawRoundRect(r1, 10f, 10f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(13, 27, 42)
        paint.strokeWidth = 2f
        canvas.drawRoundRect(r1, 10f, 10f, paint)

        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.rgb(13, 27, 42)
        paint.textSize = 21f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(if (isEn) "Total Amount" else "মোট টাকা", totalsLeft + 18f, 1262f, paint)

        // White entry box
        val r1Box = RectF(785f, 1225f, totalsRight - 10f, 1285f)
        paint.color = Color.WHITE
        canvas.drawRoundRect(r1Box, 6f, 6f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(13, 27, 42)
        canvas.drawRoundRect(r1Box, 6f, 6f, paint)
        if (data.subtotal > 0.0) {
            paint.style = Paint.Style.FILL
            paint.textAlign = Paint.Align.RIGHT
            val subStr = if (isEn) "₹${data.subtotal.toInt()}" else "₹" + BanglaNumberUtil.toBangla(data.subtotal.toInt().toString())
            canvas.drawText(subStr, totalsRight - 22f, 1262f, paint)
        }

        // Row 2: Discount (-) (Light Green)
        val r2 = RectF(totalsLeft, 1303f, totalsRight, 1383f)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(232, 245, 233) // Light Green
        canvas.drawRoundRect(r2, 10f, 10f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(13, 27, 42)
        paint.strokeWidth = 2f
        canvas.drawRoundRect(r2, 10f, 10f, paint)

        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.rgb(46, 125, 50)
        paint.textSize = 21f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(if (isEn) "Discount (-)" else "ছাড় (-)", totalsLeft + 18f, 1350f, paint)

        val r2Box = RectF(785f, 1313f, totalsRight - 10f, 1373f)
        paint.color = Color.WHITE
        canvas.drawRoundRect(r2Box, 6f, 6f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(13, 27, 42)
        canvas.drawRoundRect(r2Box, 6f, 6f, paint)
        if (data.discount > 0.0) {
            paint.style = Paint.Style.FILL
            paint.textAlign = Paint.Align.RIGHT
            val discStr = if (isEn) "- ₹${data.discount.toInt()}" else "- ₹" + BanglaNumberUtil.toBangla(data.discount.toInt().toString())
            canvas.drawText(discStr, totalsRight - 22f, 1350f, paint)
        }

        // Row 3: Grand Total (Dark Navy, White Text)
        val r3 = RectF(totalsLeft, 1391f, totalsRight, 1480f)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(13, 27, 42) // Dark Navy
        canvas.drawRoundRect(r3, 10f, 10f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.WHITE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(r3, 10f, 10f, paint)

        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.WHITE
        paint.textSize = 23f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(if (isEn) "Grand Total" else "সর্বমোট টাকা", totalsLeft + 18f, 1445f, paint)

        val r3Box = RectF(785f, 1401f, totalsRight - 10f, 1470f)
        paint.color = Color.WHITE
        canvas.drawRoundRect(r3Box, 6f, 6f, paint)
        if (data.grandTotal > 0.0) {
            paint.color = Color.rgb(220, 38, 38)
            paint.textAlign = Paint.Align.RIGHT
            paint.textSize = 24f
            val gtStr = if (isEn) "₹${data.grandTotal.toInt()}" else "₹" + BanglaNumberUtil.toBangla(data.grandTotal.toInt().toString())
            canvas.drawText(gtStr, totalsRight - 22f, 1445f, paint)
        }

        // 12. BOTTOM RAINBOW WAVES DECORATION (y = 1495f to 1536f)
        val wavePaint = Paint().apply { isAntiAlias = true }
        for (w in 0..4) {
            wavePaint.color = rainbowColors[w % rainbowColors.size]
            wavePaint.alpha = 210
            val path = Path()
            path.moveTo(0f, 1500f + w * 7f)
            path.quadTo(256f, 1485f + w * 6f, 512f, 1510f + w * 5f)
            path.quadTo(768f, 1535f + w * 4f, 1024f, 1505f + w * 6f)
            path.lineTo(1024f, 1536f)
            path.lineTo(0f, 1536f)
            path.close()
            canvas.drawPath(path, wavePaint)
        }

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
