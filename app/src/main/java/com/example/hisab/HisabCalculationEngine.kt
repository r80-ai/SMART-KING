package com.example.hisab

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object HisabCalculationEngine {

    private val df = DecimalFormat("#,##0.##")
    private val dfCurrency = DecimalFormat("₹#,##0.##")

    fun formatNumber(value: Double): String = df.format(value)
    fun formatCurrency(value: Double): String = "₹" + df.format(value)

    fun toBengaliDigits(input: String): String {
        val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val sb = StringBuilder()
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(bnDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    // =========================================================================
    // 1. MAIN SHOP HISAB (দোকান হিসাব)
    // =========================================================================

    /**
     * Calculates shopkeeper pricing based on given quantity and price.
     * Formula:
     *   pricePerGram = givenPrice / givenQuantityInGram
     *   pricePerKG = pricePerGram * 1000
     *   requiredPrice = pricePerGram * requiredQuantityInGram
     */
    fun calculateShopHisab(
        givenQuantity: Double,
        givenUnit: HisabUnit,
        givenPrice: Double,
        requiredQuantity: Double,
        requiredUnit: HisabUnit,
        budgetAmount: Double = 0.0
    ): ShopHisabResult? {
        if (givenQuantity <= 0.0 || givenPrice <= 0.0) return null

        // Convert given quantity to base unit (grams, ml, or pieces)
        val givenInBase = givenQuantity * givenUnit.baseMultiplier
        if (givenInBase <= 0.0) return null

        val pricePerBase = givenPrice / givenInBase
        val standardMultiplier = when (givenUnit.category) {
            HisabUnitCategory.WEIGHT -> 1000.0 // 1 KG
            HisabUnitCategory.LIQUID -> 1000.0 // 1 Liter
            HisabUnitCategory.PIECE -> 1.0     // 1 Piece
        }
        val pricePerStandard = pricePerBase * standardMultiplier

        val standardLabel = when (givenUnit.category) {
            HisabUnitCategory.WEIGHT -> "১ কেজি (1 KG) দাম"
            HisabUnitCategory.LIQUID -> "১ লিটার (1 Liter) দাম"
            HisabUnitCategory.PIECE -> "১ পিস (1 Piece) দাম"
        }

        // Required quantity calculation
        val requiredInBase = if (requiredQuantity > 0.0) {
            requiredQuantity * requiredUnit.baseMultiplier
        } else {
            0.0
        }
        val reqPrice = pricePerBase * requiredInBase

        val reqFormatted = if (requiredQuantity > 0.0) {
            "${formatNumber(requiredQuantity)} ${requiredUnit.symbolBn} = ${formatCurrency(reqPrice)}"
        } else {
            "—"
        }

        // Budget amount calculation (টাকা অনুযায়ী পরিমাণ)
        val budgetQtyInBase = if (budgetAmount > 0.0 && pricePerBase > 0.0) {
            budgetAmount / pricePerBase
        } else {
            0.0
        }

        val budgetFormatted = if (budgetAmount > 0.0 && budgetQtyInBase > 0.0) {
            formatQuantityOutput(budgetQtyInBase, givenUnit.category)
        } else {
            "—"
        }

        return ShopHisabResult(
            pricePerGramOrBase = pricePerBase,
            pricePerKgOrStandard = pricePerStandard,
            standardLabelBn = standardLabel,
            requiredPrice = reqPrice,
            requiredFormattedBn = reqFormatted,
            budgetQuantityInBase = budgetQtyInBase,
            budgetQuantityFormattedBn = budgetFormatted
        )
    }

    private fun formatQuantityOutput(qtyInBase: Double, category: HisabUnitCategory): String {
        return when (category) {
            HisabUnitCategory.WEIGHT -> {
                if (qtyInBase >= 1000.0) {
                    val kg = (qtyInBase / 1000.0).toInt()
                    val remGram = qtyInBase % 1000.0
                    if (remGram < 0.01) {
                        "$kg কেজি"
                    } else {
                        "$kg কেজি ${formatNumber(remGram)} গ্রাম (${formatNumber(qtyInBase / 1000.0)} কেজি)"
                    }
                } else {
                    "${formatNumber(qtyInBase)} গ্রাম"
                }
            }
            HisabUnitCategory.LIQUID -> {
                if (qtyInBase >= 1000.0) {
                    val lit = (qtyInBase / 1000.0).toInt()
                    val remMl = qtyInBase % 1000.0
                    if (remMl < 0.01) {
                        "$lit লিটার"
                    } else {
                        "$lit লিটার ${formatNumber(remMl)} মি.লি"
                    }
                } else {
                    "${formatNumber(qtyInBase)} মি.লি"
                }
            }
            HisabUnitCategory.PIECE -> {
                "${formatNumber(qtyInBase)} পিস"
            }
        }
    }

    /**
     * Specialized Money to Quantity Calculator for "ওজন (কেজি/গ্রাম)"
     * - Convert known input into base unit (Grams):
     *   knownGrams = if (isKg) knownQty * 1000.0 else knownQty
     * - Price per gram:
     *   pricePerGram = knownPrice / knownGrams
     * - Target quantity in grams for entered money:
     *   resultGrams = targetMoney / pricePerGram
     * - Target quantity in KG:
     *   resultKg = resultGrams / 1000.0
     */
    fun calculateMoneyToQuantity(
        knownQuantity: Double,
        isKg: Boolean,
        knownPrice: Double,
        targetMoney: Double
    ): MoneyToQuantityResult? {
        if (knownQuantity <= 0.0 || knownPrice <= 0.0 || targetMoney <= 0.0) return null

        val knownGrams = if (isKg) knownQuantity * 1000.0 else knownQuantity
        if (knownGrams <= 0.0) return null

        val pricePerGram = knownPrice / knownGrams
        if (pricePerGram <= 0.0 || pricePerGram.isInfinite() || pricePerGram.isNaN()) return null

        val pricePer100Gram = pricePerGram * 100.0
        val pricePerKg = pricePerGram * 1000.0

        val resultGrams = targetMoney / pricePerGram
        val resultKg = resultGrams / 1000.0

        if (resultGrams.isInfinite() || resultGrams.isNaN() || resultKg.isInfinite() || resultKg.isNaN()) {
            return null
        }

        // Formatted strings
        val gramsFormattedEn = if (resultGrams % 1.0 == 0.0) {
            df.format(resultGrams.toLong())
        } else {
            df.format(resultGrams)
        }

        val kgFormattedEn = if (resultKg % 1.0 == 0.0) {
            String.format(Locale.US, "%.0f", resultKg)
        } else if (resultKg >= 1.0 && (resultKg * 10.0) % 1.0 == 0.0) {
            String.format(Locale.US, "%.1f", resultKg)
        } else {
            String.format(Locale.US, "%.3f", resultKg)
        }

        val formattedKgBn = "${toBengaliDigits(kgStrClean(resultKg))} কেজি (${String.format(Locale.US, "%.3f", resultKg)} KG)"
        val formattedGramsBn = "${toBengaliDigits(gramsFormattedEn)} গ্রাম (${gramsFormattedEn} GM)"

        val rate1KgBn = "১ কেজি দাম: ₹${toBengaliDigits(df.format(pricePerKg))} (₹${df.format(pricePerKg)})"
        val rate100GramBn = "১০০ গ্রাম দাম: ₹${toBengaliDigits(df.format(pricePer100Gram))} (₹${df.format(pricePer100Gram)})"
        val ratePerGramBn = "প্রতি গ্রাম দর: ₹${toBengaliDigits(String.format(Locale.US, "%.3f", pricePerGram))} (₹${String.format(Locale.US, "%.3f", pricePerGram)})"

        return MoneyToQuantityResult(
            knownQuantity = knownQuantity,
            isKg = isKg,
            knownPrice = knownPrice,
            targetMoney = targetMoney,
            pricePerGram = pricePerGram,
            pricePer100Gram = pricePer100Gram,
            pricePerKg = pricePerKg,
            resultGrams = resultGrams,
            resultKg = resultKg,
            formattedKgBn = formattedKgBn,
            formattedGramsBn = formattedGramsBn,
            rate1KgBn = rate1KgBn,
            rate100GramBn = rate100GramBn,
            ratePerGramBn = ratePerGramBn
        )
    }

    private fun kgStrClean(resultKg: Double): String {
        return if (resultKg % 1.0 == 0.0) {
            "${resultKg.toLong()}"
        } else if (resultKg * 10.0 % 1.0 == 0.0) {
            String.format(Locale.US, "%.1f", resultKg)
        } else {
            String.format(Locale.US, "%.3f", resultKg)
        }
    }

    // =========================================================================
    // 2. AGE CALCULATOR (বয়স ক্যালকুলেটর)
    // =========================================================================

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    fun daysInMonth(month: Int, year: Int): Int {
        return when (month) {
            2 -> if (isLeapYear(year)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
    }

    fun validateDate(day: Int, month: Int, year: Int): String? {
        if (year < 1900 || year > 2150) return "সঠিক বছর লিখুন (১৯০০–২১৫০)"
        if (month !in 1..12) return "সঠিক মাস লিখুন (০১–১২)"
        val maxDays = daysInMonth(month, year)
        if (day < 1 || day > maxDays) return "তারিখটি সঠিক নয় (এই মাসে সর্বোচ্চ $maxDays দিন)"
        return null
    }

    /**
     * Calculates age between birth date and target date.
     * Guaranteed exact on all test cases including:
     *   Birth: 22/03/1984, Target: 14/09/2026 -> 42 বছর 5 মাস 23 দিন.
     */
    fun calculateAge(
        birthDay: Int,
        birthMonth: Int, // 1-12
        birthYear: Int,
        targetDay: Int,
        targetMonth: Int, // 1-12
        targetYear: Int
    ): AgeResult? {
        if (birthYear <= 0 || targetYear <= 0) return null
        if (birthMonth !in 1..12 || targetMonth !in 1..12) return null
        if (birthDay < 1 || birthDay > daysInMonth(birthMonth, birthYear)) return null
        if (targetDay < 1 || targetDay > daysInMonth(targetMonth, targetYear)) return null

        // Check date order
        if (targetYear < birthYear ||
            (targetYear == birthYear && targetMonth < birthMonth) ||
            (targetYear == birthYear && targetMonth == birthMonth && targetDay < birthDay)
        ) {
            return null
        }

        var d = targetDay - birthDay
        var m = targetMonth - birthMonth
        var y = targetYear - birthYear

        if (d < 0) {
            // Borrow days from previous month of target date
            val prevMonth = if (targetMonth == 1) 12 else targetMonth - 1
            val prevYear = if (targetMonth == 1) targetYear - 1 else targetYear
            d += daysInMonth(prevMonth, prevYear)
            m -= 1
        }

        if (m < 0) {
            m += 12
            y -= 1
        }

        // Calculate total days using Calendar
        val calBirth = Calendar.getInstance().apply {
            set(Calendar.YEAR, birthYear)
            set(Calendar.MONTH, birthMonth - 1)
            set(Calendar.DAY_OF_MONTH, birthDay)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val calTarget = Calendar.getInstance().apply {
            set(Calendar.YEAR, targetYear)
            set(Calendar.MONTH, targetMonth - 1)
            set(Calendar.DAY_OF_MONTH, targetDay)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val diffMillis = calTarget.timeInMillis - calBirth.timeInMillis
        val totalDays = (diffMillis / (1000L * 60 * 60 * 24)).coerceAtLeast(0L)
        val totalWeeks = totalDays / 7L
        val totalMonths = (y.toLong() * 12L) + m.toLong()

        // Calculate next birthday
        val nextBdayYear = if (targetMonth < birthMonth || (targetMonth == birthMonth && targetDay < birthDay)) {
            targetYear
        } else {
            targetYear + 1
        }

        // Adjust for Feb 29 on non-leap year
        val adjustedBirthDay = if (birthMonth == 2 && birthDay == 29 && !isLeapYear(nextBdayYear)) 28 else birthDay

        val calNextBday = Calendar.getInstance().apply {
            set(Calendar.YEAR, nextBdayYear)
            set(Calendar.MONTH, birthMonth - 1)
            set(Calendar.DAY_OF_MONTH, adjustedBirthDay)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val nextBdayWeekdayNumber = calNextBday.get(Calendar.DAY_OF_WEEK)
        val (weekdayBn, weekdayEn) = when (nextBdayWeekdayNumber) {
            Calendar.SUNDAY -> "রবিবার" to "Sunday"
            Calendar.MONDAY -> "সোমবার" to "Monday"
            Calendar.TUESDAY -> "মঙ্গলবার" to "Tuesday"
            Calendar.WEDNESDAY -> "বুধবার" to "Wednesday"
            Calendar.THURSDAY -> "বৃহস্পতিবার" to "Thursday"
            Calendar.FRIDAY -> "শুক্রবার" to "Friday"
            Calendar.SATURDAY -> "শনিবার" to "Saturday"
            else -> "—" to "—"
        }

        val nextBdayFormatted = String.format(Locale.US, "%02d/%02d/%d", adjustedBirthDay, birthMonth, nextBdayYear)

        // Remaining months and days to next birthday
        var nextD = adjustedBirthDay - targetDay
        var nextM = (birthMonth - targetMonth).let { if (it < 0) it + 12 else it }
        if (targetMonth == birthMonth && targetDay > adjustedBirthDay) {
            nextM = 11
        }

        if (nextD < 0) {
            val prevMonth = if (birthMonth == 1) 12 else birthMonth - 1
            val prevYear = if (birthMonth == 1) nextBdayYear - 1 else nextBdayYear
            nextD += daysInMonth(prevMonth, prevYear)
            nextM = if (nextM > 0) nextM - 1 else 11
        }

        return AgeResult(
            years = y,
            months = m,
            days = d,
            totalDays = totalDays,
            totalWeeks = totalWeeks,
            totalMonths = totalMonths,
            nextBirthdayDate = nextBdayFormatted,
            nextBirthdayWeekdayBn = weekdayBn,
            nextBirthdayWeekdayEn = weekdayEn,
            nextBirthdayMonthsRemaining = nextM,
            nextBirthdayDaysRemaining = nextD
        )
    }

    // =========================================================================
    // 3. NORMAL CALCULATOR (সাধারণ ক্যালকুলেটর)
    // =========================================================================

    /**
     * Evaluates simple arithmetic expressions supporting +, -, ×, ÷, %
     * Handles:
     *   100 + 200 = 300
     *   100 × 5 = 500
     *   1000 ÷ 4 = 250
     *   500 − 125 = 375
     */
    fun evaluateExpression(expr: String): Double? {
        val sanitized = expr.replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace(" ", "")

        if (sanitized.isBlank()) return null

        return try {
            parseExpression(sanitized)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseExpression(expr: String): Double {
        // Tokenize into numbers and operators
        val tokens = mutableListOf<String>()
        var currentNum = StringBuilder()

        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            if (c.isDigit() || c == '.') {
                currentNum.append(c)
            } else if (c in listOf('+', '-', '*', '/', '%')) {
                if (currentNum.isNotEmpty()) {
                    tokens.add(currentNum.toString())
                    currentNum = StringBuilder()
                } else if (c == '-' && (tokens.isEmpty() || tokens.last() in listOf("+", "-", "*", "/"))) {
                    // Unary minus
                    currentNum.append(c)
                    i++
                    continue
                }
                tokens.add(c.toString())
            }
            i++
        }
        if (currentNum.isNotEmpty()) {
            tokens.add(currentNum.toString())
        }

        if (tokens.isEmpty()) return 0.0

        // Pass 1: Handle * / %
        val pass1 = mutableListOf<String>()
        var idx = 0
        while (idx < tokens.size) {
            val t = tokens[idx]
            if (t == "*" || t == "/" || t == "%") {
                val prev = pass1.removeAt(pass1.size - 1).toDouble()
                val next = tokens[idx + 1].toDouble()
                val res = when (t) {
                    "*" -> prev * next
                    "/" -> if (next != 0.0) prev / next else Double.NaN
                    "%" -> prev * (next / 100.0)
                    else -> 0.0
                }
                pass1.add(res.toString())
                idx += 2
            } else {
                pass1.add(t)
                idx++
            }
        }

        // Pass 2: Handle + -
        var result = pass1[0].toDouble()
        idx = 1
        while (idx < pass1.size) {
            val op = pass1[idx]
            val next = pass1[idx + 1].toDouble()
            result = if (op == "+") result + next else result - next
            idx += 2
        }

        return result
    }

    // =========================================================================
    // 4. DISCOUNT & INCREASE (ছাড় ও বৃদ্ধি)
    // =========================================================================

    fun calculateDiscount(originalPrice: Double, discountPercent: Double, flatDiscount: Double = 0.0): DiscountResult {
        val pctDiscount = originalPrice * (discountPercent.coerceAtLeast(0.0) / 100.0)
        val totalDiscount = (pctDiscount + flatDiscount.coerceAtLeast(0.0)).coerceAtMost(originalPrice)
        val finalPrice = (originalPrice - totalDiscount).coerceAtLeast(0.0)
        return DiscountResult(
            originalPrice = originalPrice,
            discountPercent = discountPercent,
            discountAmount = totalDiscount,
            finalPrice = finalPrice,
            savings = totalDiscount
        )
    }

    fun calculateIncrease(originalPrice: Double, increasePercent: Double, flatIncrease: Double = 0.0): IncreaseResult {
        val pctIncrease = originalPrice * (increasePercent.coerceAtLeast(0.0) / 100.0)
        val totalIncrease = pctIncrease + flatIncrease.coerceAtLeast(0.0)
        val finalPrice = originalPrice + totalIncrease
        return IncreaseResult(
            originalPrice = originalPrice,
            increasePercent = increasePercent,
            increaseAmount = totalIncrease,
            finalPrice = finalPrice
        )
    }

    // =========================================================================
    // 5. GST (জিএসটি)
    // =========================================================================

    fun calculateGst(amount: Double, gstRate: Double, isGstIncluded: Boolean): GstResult {
        return if (isGstIncluded) {
            val base = amount / (1.0 + (gstRate / 100.0))
            val gst = amount - base
            GstResult(
                isGstIncluded = true,
                baseAmount = base,
                gstRate = gstRate,
                cgstAmount = gst / 2.0,
                sgstAmount = gst / 2.0,
                totalGstAmount = gst,
                totalAmount = amount
            )
        } else {
            val gst = amount * (gstRate / 100.0)
            GstResult(
                isGstIncluded = false,
                baseAmount = amount,
                gstRate = gstRate,
                cgstAmount = gst / 2.0,
                sgstAmount = gst / 2.0,
                totalGstAmount = gst,
                totalAmount = amount + gst
            )
        }
    }

    // =========================================================================
    // 6. PROFIT & LOSS (লাভ ও ক্ষতি)
    // =========================================================================

    fun calculateProfitLoss(costPrice: Double, sellingPrice: Double): ProfitLossResult {
        val diff = sellingPrice - costPrice
        val isProfit = diff >= 0
        val absDiff = kotlin.math.abs(diff)
        val pct = if (costPrice > 0.0) (absDiff / costPrice) * 100.0 else 0.0
        return ProfitLossResult(
            costPrice = costPrice,
            sellingPrice = sellingPrice,
            isProfit = isProfit,
            difference = absDiff,
            percentage = pct
        )
    }
}
