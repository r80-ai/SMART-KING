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
    val address: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "ad_services")
@JsonClass(generateAdapter = true)
data class ServiceItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val price: Double = 0.0,
    val category: String? = null,
    val description: String? = null
)

@Entity(tableName = "ad_bills")
@JsonClass(generateAdapter = true)
data class Bill(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val billNumber: String = "",
    val customerName: String = "",
    val customerId: Long? = null,
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dueAmount: Double = 0.0,
    val dateMillis: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val language: String = "BN"
)

@Entity(tableName = "ad_bill_items")
@JsonClass(generateAdapter = true)
data class BillItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val billId: Long = 0,
    val serviceName: String = "",
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
    val totalPrice: Double = 0.0
)

@Entity(tableName = "ad_payments")
@JsonClass(generateAdapter = true)
data class Payment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long = 0,
    val billId: Long? = null,
    val amount: Double = 0.0,
    val dateMillis: Long = System.currentTimeMillis(),
    val paymentMode: String = "CASH",
    val note: String? = null
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
    val payments: List<Payment> = emptyList()
)
