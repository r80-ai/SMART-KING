package com.example.amardokan.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "ad_customers")
@JsonClass(generateAdapter = true)
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "ad_services")
@JsonClass(generateAdapter = true)
data class ServiceItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nameBn: String = "",
    val nameEn: String = "",
    val defaultRate: Double = 0.0,
    val unitBn: String = "পাতা",
    val isDefault: Boolean = false,
    val category: String? = null,
    val description: String? = null
) {
    val name: String get() = nameBn.ifBlank { nameEn }
    val price: Double get() = defaultRate
}

@Entity(tableName = "ad_bills")
@JsonClass(generateAdapter = true)
data class Bill(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val billNumber: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val dateFormatted: String = "",
    val customerId: Long? = null,
    val customerName: String = "",
    val customerPhone: String = "",
    val paymentType: String = "CASH", // "CASH" or "CREDIT"
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val grandTotal: Double = 0.0,
    val cashReceived: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val language: String = "BN"
) {
    val totalAmount: Double get() = grandTotal
    val paidAmount: Double get() = cashReceived
    val dueAmount: Double get() = (grandTotal - cashReceived).coerceAtLeast(0.0)
}

@Entity(tableName = "ad_bill_items")
@JsonClass(generateAdapter = true)
data class BillItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val billId: Long = 0,
    val serviceName: String = "",
    val quantity: Double = 1.0,
    val unitRate: Double = 0.0,
    val lineTotal: Double = 0.0
) {
    val unitPrice: Double get() = unitRate
    val totalPrice: Double get() = lineTotal
}

@JsonClass(generateAdapter = true)
data class DraftBillItem(
    val serviceName: String = "",
    val quantity: Double = 1.0,
    val unitRate: Double = 0.0,
    val unitName: String = "পাতা"
) {
    val lineTotal: Double get() = quantity * unitRate
}

@Entity(tableName = "ad_payments")
@JsonClass(generateAdapter = true)
data class Payment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long = 0,
    val customerName: String = "",
    val billId: Long? = null,
    val dateMillis: Long = System.currentTimeMillis(),
    val dateFormatted: String = "",
    val amount: Double = 0.0,
    val paymentMode: String = "CASH", // "CASH" or "UPI"
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class CustomerBalanceSummary(
    val customer: Customer,
    val totalWorkAmount: Double = 0.0,
    val totalPaidAmount: Double = 0.0,
    val outstandingBalance: Double = 0.0
)

enum class LedgerEntryType {
    BILL,
    PAYMENT
}

data class LedgerEntry(
    val id: Long,
    val dateMillis: Long,
    val dateFormatted: String,
    val type: LedgerEntryType,
    val description: String,
    val billAmount: Double = 0.0,
    val paymentAmount: Double = 0.0,
    val runningBalance: Double = 0.0,
    val paymentMode: String = "",
    val rawBillId: Long? = null
)

@Entity(tableName = "ad_sync_metadata")
@JsonClass(generateAdapter = true)
data class SyncMetadata(
    @PrimaryKey
    val id: String,
    val entityType: String,
    val entityId: Long,
    val lastSyncedAt: Long,
    val isPending: Boolean
)

@JsonClass(generateAdapter = true)
data class DatabaseBackup(
    val customers: List<Customer> = emptyList(),
    val services: List<ServiceItem> = emptyList(),
    val bills: List<Bill> = emptyList(),
    val billItems: List<BillItem> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val backupTimestamp: Long = System.currentTimeMillis(),
    val version: Int = 1
)

