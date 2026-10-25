package com.example

import com.example.amardokan.data.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class AmarDokanBackupTest {

    @Test
    fun testTargetBackupDirectoryPath() {
        assertEquals("/storage/emulated/0/Smart King/Amar Dokan", AmarDokanBackupManager.TARGET_FOLDER_PATH)
    }

    @Test
    fun testBackupJsonSerializationAndDeserialization() {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(DatabaseBackup::class.java).indent("  ")

        val customer1 = Customer(id = 1, name = "রফিক শেখ", phone = "9876543210", address = "কলকাতা")
        val customer2 = Customer(id = 2, name = "আনিসুর রহমান", phone = "9123456780", address = "হাওড়া")
        val service1 = ServiceItem(id = 1, nameBn = "জেরক্স", nameEn = "Xerox", defaultRate = 2.0, unitBn = "পাতা")
        val service2 = ServiceItem(id = 2, nameBn = "প্রিন্টআউট", nameEn = "Print", defaultRate = 5.0, unitBn = "পাতা")
        val bill = Bill(
            id = 1,
            billNumber = "BILL-0001",
            customerId = 1,
            customerName = "রফিক শেখ",
            customerPhone = "9876543210",
            paymentType = "CREDIT",
            subtotal = 100.0,
            grandTotal = 100.0,
            cashReceived = 20.0
        )
        val billItem = BillItem(id = 1, billId = 1, serviceName = "জেরক্স", quantity = 50.0, unitRate = 2.0, lineTotal = 100.0)
        val payment = Payment(id = 1, customerId = 1, customerName = "রফিক শেখ", amount = 20.0, paymentMode = "CASH")

        val originalBackup = DatabaseBackup(
            customers = listOf(customer1, customer2),
            services = listOf(service1, service2),
            bills = listOf(bill),
            billItems = listOf(billItem),
            payments = listOf(payment),
            backupTimestamp = 1700000000000L,
            version = 1
        )

        val json = adapter.toJson(originalBackup)
        assertNotNull(json)
        assertTrue(json.contains("রফিক শেখ"))
        assertTrue(json.contains("BILL-0001"))
        assertTrue(json.contains("জেরক্স"))

        val restoredBackup = adapter.fromJson(json)
        assertNotNull(restoredBackup)
        assertEquals(2, restoredBackup!!.customers.size)
        assertEquals("রফিক শেখ", restoredBackup.customers[0].name)
        assertEquals(2, restoredBackup.services.size)
        assertEquals(1, restoredBackup.bills.size)
        assertEquals(1, restoredBackup.billItems.size)
        assertEquals(1, restoredBackup.payments.size)
        assertEquals(100.0, restoredBackup.bills[0].grandTotal, 0.001)
        assertEquals(20.0, restoredBackup.payments[0].amount, 0.001)
    }

    @Test
    fun testBackupFileNameFormat() {
        val regex = Regex("^AmarDokan_Backup_\\d{8}_\\d{6}\\.json$")
        val sampleName = "AmarDokan_Backup_20261025_012000.json"
        assertTrue(regex.matches(sampleName))
    }
}
