package com.example.amardokan.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Customer entity for Amar Dokan.
 */
@Entity(tableName = "ad_customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Service/Product rate configuration.
 */
@Entity(tableName = "ad_services")
data class ServiceItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nameBn: String,
    val nameEn: String = "",
    val defaultRate: Double,
    val unitBn: String = "পাতা", // পাতা, টি, পিস, সার্ভিস
    val isDefault: Boolean = false
)

/**
 * Master Bill record.
 */
@Entity(
    tableName = "ad_bills",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["dateMillis"])
    ]
)
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billNumber: String, // e.g. "বিল-1001"
    val dateMillis: Long, // timestamp normalized to day or actual timestamp
    val dateFormatted: String, // e.g. "11/10/2026"
    val customerId: Long? = null, // null = Cash Counter (নগদ কাউন্টার)
    val customerName: String,
    val customerPhone: String = "",
    val paymentType: String, // "CASH" (নগদ) or "CREDIT" (বাকি)
    val subtotal: Double,
    val discount: Double = 0.0,
    val grandTotal: Double,
    val cashReceived: Double = 0.0, // For CASH bills = grandTotal; for CREDIT bills = initial cash paid (usually 0)
    val notes: String = "",
    val language: String = "BN", // "BN" or "EN"
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Individual items inside a Bill.
 */
@Entity(
    tableName = "ad_bill_items",
    foreignKeys = [
        ForeignKey(
            entity = Bill::class,
            parentColumns = ["id"],
            childColumns = ["billId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["billId"])]
)
data class BillItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billId: Long,
    val serviceName: String,
    val quantity: Double,
    val unitRate: Double,
    val lineTotal: Double
)

/**
 * Payment received from a customer to clear outstanding dues.
 */
@Entity(
    tableName = "ad_payments",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["dateMillis"])
    ]
)
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val customerName: String,
    val dateMillis: Long,
    val dateFormatted: String,
    val amount: Double,
    val paymentMode: String, // "CASH" (নগদ) or "UPI" (ইউপিআই)
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Helper model representing customer ledger line item (Bill or Payment).
 */
data class LedgerEntry(
    val id: Long,
    val dateMillis: Long,
    val dateFormatted: String,
    val type: LedgerEntryType,
    val description: String,
    val billAmount: Double = 0.0, // Debit (কাজ / বিল)
    val paymentAmount: Double = 0.0, // Credit (জমা)
    val runningBalance: Double = 0.0,
    val paymentMode: String = ""
)

enum class LedgerEntryType {
    BILL, PAYMENT
}

/**
 * Summary calculations for a Customer.
 */
data class CustomerBalanceSummary(
    val customer: Customer,
    val totalWorkAmount: Double,
    val totalPaidAmount: Double,
    val outstandingBalance: Double
)

/**
 * Metadata for tracking sync status of entities.
 */
@Entity(tableName = "ad_sync_metadata")
data class SyncMetadata(
    @PrimaryKey val id: String, // Combination of type and entityId or a UUID
    val entityType: String, // e.g., "customer", "bill", "payment"
    val entityId: Long,
    val lastSyncedAt: Long = 0,
    val isPending: Boolean = true
)
