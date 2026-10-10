package com.example.hisab

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HisabEngineTest {

    @Test
    fun testMainHisab_Test1() {
        // Test: 100 Gram = ₹15 -> 1 KG = ₹150, 500 Gram = ₹75
        val result = HisabCalculationEngine.calculateShopHisab(
            givenQuantity = 100.0,
            givenUnit = HisabUnit.GRAM,
            givenPrice = 15.0,
            requiredQuantity = 500.0,
            requiredUnit = HisabUnit.GRAM,
            budgetAmount = 0.0
        )
        assertNotNull(result)
        assertEquals(150.0, result!!.pricePerKgOrStandard, 0.001)
        assertEquals(75.0, result.requiredPrice, 0.001)
    }

    @Test
    fun testMainHisab_Test2() {
        // Test: 250 Gram = ₹40 -> 1 KG = ₹160, 500 Gram = ₹80
        val result = HisabCalculationEngine.calculateShopHisab(
            givenQuantity = 250.0,
            givenUnit = HisabUnit.GRAM,
            givenPrice = 40.0,
            requiredQuantity = 500.0,
            requiredUnit = HisabUnit.GRAM,
            budgetAmount = 50.0
        )
        assertNotNull(result)
        assertEquals(160.0, result!!.pricePerKgOrStandard, 0.001)
        assertEquals(80.0, result.requiredPrice, 0.001)
        // 50 / (160/1000) = 312.5 Gram
        assertEquals(312.5, result.budgetQuantityInBase, 0.001)
    }

    @Test
    fun testAgeCalculator_RequiredCase() {
        // Test: Birth Date: 22/03/1984, Current/Custom Date: 14/09/2026
        // Expected: 42 বছর 5 মাস 23 দিন
        val age = HisabCalculationEngine.calculateAge(
            birthDay = 22,
            birthMonth = 3,
            birthYear = 1984,
            targetDay = 14,
            targetMonth = 9,
            targetYear = 2026
        )
        assertNotNull(age)
        assertEquals(42, age!!.years)
        assertEquals(5, age.months)
        assertEquals(23, age.days)
        assertTrue(age.totalDays > 0)
        assertTrue(age.totalWeeks > 0)
        assertEquals(42 * 12 + 5, age.totalMonths.toInt())
        assertEquals("22/03/2027", age.nextBirthdayDate)
        assertEquals("সোমবার", age.nextBirthdayWeekdayBn)
        assertEquals("Monday", age.nextBirthdayWeekdayEn)
    }

    @Test
    fun testNormalCalculator_RequiredCases() {
        // 100 + 200 = 300
        assertEquals(300.0, HisabCalculationEngine.evaluateExpression("100 + 200")!!, 0.001)

        // 100 × 5 = 500
        assertEquals(500.0, HisabCalculationEngine.evaluateExpression("100 × 5")!!, 0.001)

        // 1000 ÷ 4 = 250
        assertEquals(250.0, HisabCalculationEngine.evaluateExpression("1000 ÷ 4")!!, 0.001)

        // 500 − 125 = 375
        assertEquals(375.0, HisabCalculationEngine.evaluateExpression("500 − 125")!!, 0.001)
    }

    @Test
    fun testDiscountAndIncrease() {
        val discount = HisabCalculationEngine.calculateDiscount(1000.0, 10.0)
        assertEquals(100.0, discount.discountAmount, 0.001)
        assertEquals(900.0, discount.finalPrice, 0.001)

        val increase = HisabCalculationEngine.calculateIncrease(1000.0, 15.0)
        assertEquals(150.0, increase.increaseAmount, 0.001)
        assertEquals(1150.0, increase.finalPrice, 0.001)
    }

    @Test
    fun testGstAndProfitLoss() {
        val gstExcl = HisabCalculationEngine.calculateGst(1000.0, 18.0, false)
        assertEquals(180.0, gstExcl.totalGstAmount, 0.001)
        assertEquals(1180.0, gstExcl.totalAmount, 0.001)

        val pl = HisabCalculationEngine.calculateProfitLoss(800.0, 1000.0)
        assertTrue(pl.isProfit)
        assertEquals(200.0, pl.difference, 0.001)
        assertEquals(25.0, pl.percentage, 0.001)
    }
}
