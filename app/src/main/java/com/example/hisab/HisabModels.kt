package com.example.hisab

/**
 * Data models and enumerations for the Hisab Calculator module.
 */

enum class HisabUnitCategory(val labelBn: String, val labelEn: String) {
    WEIGHT("ওজন (কেজি/গ্রাম)", "Weight (KG/Gram)"),
    LIQUID("তরল (লিটার/মি.লি)", "Liquid (Liter/ML)"),
    PIECE("গণনা (পিস/ডজন)", "Count (Piece/Dozen)")
}

enum class HisabUnit(
    val category: HisabUnitCategory,
    val symbolBn: String,
    val symbolEn: String,
    val baseMultiplier: Double // multiplier to convert 1 of this unit to base unit (Gram, ML, Piece)
) {
    // Weight (Base: Gram)
    GRAM(HisabUnitCategory.WEIGHT, "গ্রাম", "Gram", 1.0),
    KG(HisabUnitCategory.WEIGHT, "কেজি", "KG", 1000.0),

    // Liquid (Base: ML)
    ML(HisabUnitCategory.LIQUID, "মি.লি", "ML", 1.0),
    LITER(HisabUnitCategory.LIQUID, "লিটার", "Liter", 1000.0),

    // Count (Base: Piece)
    PIECE(HisabUnitCategory.PIECE, "পিস", "Piece", 1.0),
    HALI(HisabUnitCategory.PIECE, "হালি (৪ পিস)", "Hali (4 pcs)", 4.0),
    DOZEN(HisabUnitCategory.PIECE, "ডজন (১২ পিস)", "Dozen (12 pcs)", 12.0)
}

enum class HisabSubScreen {
    HOME,
    SHOP_HISAB,
    AGE_CALCULATOR,
    NORMAL_CALCULATOR
}

enum class HisabMainTab(val titleBn: String, val titleEn: String) {
    SHOP("দোকান হিসাব", "Shop Hisab"),
    AGE("বয়স ক্যালকুলেটর", "Age Calculator"),
    CALCULATOR("সাধারণ ক্যালকুলেটর", "Calculator"),
    EXTRA_TOOLS("অন্যান্য টুলস", "More Tools")
}

enum class HisabExtraTool(val titleBn: String, val titleEn: String, val descBn: String) {
    DISCOUNT("ছাড় (Discount)", "Discount", "শতাংশ বা সরাসরি টাকার ছাড় হিসাব"),
    INCREASE("দাম বৃদ্ধি (Markup)", "Price Increase", "দাম বৃদ্ধির শতাংশ ও নতুন মূল্য হিসাব"),
    GST("জিএসটি (GST)", "GST Calculator", "GST অন্তর্ভুক্তি বা বহির্ভুক্তি ও CGST/SGST হিসাব"),
    PROFIT_LOSS("লাভ ও ক্ষতি (Profit/Loss)", "Profit & Loss", "ক্রয় ও বিক্রয় মূল্যের লাভ-ক্ষতি হিসাব")
}

data class ShopHisabResult(
    val pricePerGramOrBase: Double,
    val pricePerKgOrStandard: Double,
    val standardLabelBn: String,
    val requiredPrice: Double,
    val requiredFormattedBn: String,
    val budgetQuantityInBase: Double,
    val budgetQuantityFormattedBn: String
)

data class AgeResult(
    val years: Int,
    val months: Int,
    val days: Int,
    val totalDays: Long,
    val totalWeeks: Long,
    val totalMonths: Long,
    val nextBirthdayDate: String,
    val nextBirthdayWeekdayBn: String,
    val nextBirthdayWeekdayEn: String,
    val nextBirthdayMonthsRemaining: Int,
    val nextBirthdayDaysRemaining: Int
)

data class DiscountResult(
    val originalPrice: Double,
    val discountPercent: Double,
    val discountAmount: Double,
    val finalPrice: Double,
    val savings: Double
)

data class IncreaseResult(
    val originalPrice: Double,
    val increasePercent: Double,
    val increaseAmount: Double,
    val finalPrice: Double
)

data class GstResult(
    val isGstIncluded: Boolean,
    val baseAmount: Double,
    val gstRate: Double,
    val cgstAmount: Double,
    val sgstAmount: Double,
    val totalGstAmount: Double,
    val totalAmount: Double
)

data class ProfitLossResult(
    val costPrice: Double,
    val sellingPrice: Double,
    val isProfit: Boolean,
    val difference: Double,
    val percentage: Double
)

data class MoneyToQuantityResult(
    val knownQuantity: Double,
    val isKg: Boolean,
    val knownPrice: Double,
    val targetMoney: Double,
    val pricePerGram: Double,
    val pricePer100Gram: Double,
    val pricePerKg: Double,
    val resultGrams: Double,
    val resultKg: Double,
    val formattedKgBn: String,
    val formattedGramsBn: String,
    val rate1KgBn: String,
    val rate100GramBn: String,
    val ratePerGramBn: String
)

data class HisabHistoryItem(
    val id: Long = System.currentTimeMillis(),
    val type: String,
    val titleBn: String,
    val inputDescriptionBn: String,
    val resultBn: String,
    val timestamp: Long = System.currentTimeMillis()
)
