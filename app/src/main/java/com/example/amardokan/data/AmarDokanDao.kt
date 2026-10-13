package com.example.amardokan.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import com.example.amardokan.data.SyncMetadata

@Dao
interface AmarDokanDao {

    // ---------------- CUSTOMERS ----------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCustomers(customers: List<Customer>)

    @Query("DELETE FROM ad_customers")
    suspend fun deleteAllCustomers()

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("SELECT * FROM ad_customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM ad_customers")
    suspend fun getAllCustomersSync(): List<Customer>

    @Query("SELECT * FROM ad_customers WHERE id = :customerId LIMIT 1")
    suspend fun getCustomerById(customerId: Long): Customer?

    // ---------------- SERVICES ----------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: ServiceItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServices(services: List<ServiceItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllServices(services: List<ServiceItem>)

    @Query("DELETE FROM ad_services")
    suspend fun deleteAllServices()

    @Update
    suspend fun updateService(service: ServiceItem)

    @Delete
    suspend fun deleteService(service: ServiceItem)

    @Query("SELECT * FROM ad_services ORDER BY isDefault DESC, id ASC")
    fun getAllServices(): Flow<List<ServiceItem>>

    @Query("SELECT * FROM ad_services")
    suspend fun getAllServicesSync(): List<ServiceItem>

    @Query("SELECT COUNT(*) FROM ad_services")
    suspend fun countServices(): Int

    // ---------------- BILLS ----------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: Bill): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllBills(bills: List<Bill>)

    @Query("DELETE FROM ad_bills")
    suspend fun deleteAllBills()

    @Update
    suspend fun updateBill(bill: Bill)

    @Delete
    suspend fun deleteBill(bill: Bill)

    @Query("SELECT * FROM ad_bills ORDER BY dateMillis DESC, id DESC")
    fun getAllBills(): Flow<List<Bill>>

    @Query("SELECT * FROM ad_bills")
    suspend fun getAllBillsSync(): List<Bill>

    @Query("SELECT * FROM ad_bills WHERE id = :billId LIMIT 1")
    suspend fun getBillById(billId: Long): Bill?

    @Query("SELECT * FROM ad_bills WHERE customerId = :customerId ORDER BY dateMillis ASC, id ASC")
    fun getBillsByCustomerId(customerId: Long): Flow<List<Bill>>

    @Query("SELECT * FROM ad_bills WHERE dateMillis >= :startMillis AND dateMillis <= :endMillis ORDER BY dateMillis DESC, id DESC")
    fun getBillsByDateRange(startMillis: Long, endMillis: Long): Flow<List<Bill>>

    // ---------------- BILL ITEMS ----------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillItems(items: List<BillItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllBillItems(items: List<BillItem>)

    @Query("DELETE FROM ad_bill_items")
    suspend fun deleteAllBillItems()

    @Query("SELECT * FROM ad_bill_items WHERE billId = :billId ORDER BY id ASC")
    fun getBillItemsByBillId(billId: Long): Flow<List<BillItem>>

    @Query("SELECT * FROM ad_bill_items")
    suspend fun getAllBillItemsSync(): List<BillItem>

    @Query("SELECT * FROM ad_bill_items WHERE billId = :billId ORDER BY id ASC")
    suspend fun getBillItemsByBillIdSync(billId: Long): List<BillItem>

    @Query("DELETE FROM ad_bill_items WHERE billId = :billId")
    suspend fun deleteBillItemsByBillId(billId: Long)

    // ---------------- PAYMENTS ----------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPayments(payments: List<Payment>)

    @Query("DELETE FROM ad_payments")
    suspend fun deleteAllPayments()

    @Update
    suspend fun updatePayment(payment: Payment)

    @Delete
    suspend fun deletePayment(payment: Payment)

    @Query("SELECT * FROM ad_payments ORDER BY dateMillis DESC, id DESC")
    fun getAllPayments(): Flow<List<Payment>>

    @Query("SELECT * FROM ad_payments")
    suspend fun getAllPaymentsSync(): List<Payment>

    @Query("SELECT * FROM ad_payments WHERE customerId = :customerId ORDER BY dateMillis ASC, id ASC")
    fun getPaymentsByCustomerId(customerId: Long): Flow<List<Payment>>

    @Query("SELECT * FROM ad_payments WHERE dateMillis >= :startMillis AND dateMillis <= :endMillis ORDER BY dateMillis DESC, id DESC")
    fun getPaymentsByDateRange(startMillis: Long, endMillis: Long): Flow<List<Payment>>

    // ---------------- FULL TRANSACTIONS ----------------
    @Transaction
    suspend fun saveBillWithItems(bill: Bill, items: List<BillItem>): Long {
        val billId = insertBill(bill)
        val itemsWithBillId = items.map { it.copy(billId = billId) }
        insertBillItems(itemsWithBillId)
        return billId
    }

    @Transaction
    suspend fun updateBillWithItems(bill: Bill, items: List<BillItem>) {
        updateBill(bill)
        deleteBillItemsByBillId(bill.id)
        val itemsWithBillId = items.map { it.copy(billId = bill.id) }
        insertBillItems(itemsWithBillId)
    }

    @Transaction
    suspend fun restoreAllData(
        customers: List<Customer>,
        services: List<ServiceItem>,
        bills: List<Bill>,
        billItems: List<BillItem>,
        payments: List<Payment>
    ) {
        deleteAllCustomers()
        deleteAllServices()
        deleteAllBills()
        deleteAllBillItems()
        deleteAllPayments()

        insertAllCustomers(customers)
        insertAllServices(services)
        insertAllBills(bills)
        insertAllBillItems(billItems)
        insertAllPayments(payments)
    }

    @Transaction
    suspend fun deleteBillCompletely(bill: Bill) {
        deleteBillItemsByBillId(bill.id)
        deleteBill(bill)
    }

    // Direct sum queries for fast aggregation
    @Query("SELECT COALESCE(SUM(grandTotal), 0.0) FROM ad_bills WHERE customerId = :customerId AND paymentType = 'CREDIT'")
    suspend fun getTotalCreditBillsForCustomer(customerId: Long): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM ad_payments WHERE customerId = :customerId")
    suspend fun getTotalPaymentsForCustomer(customerId: Long): Double

    // ---------------- SYNC METADATA ----------------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncMetadata(metadata: SyncMetadata)

    @Query("SELECT * FROM ad_sync_metadata WHERE isPending = 1")
    suspend fun getPendingSyncs(): List<SyncMetadata>

    @Update
    suspend fun updateSyncMetadata(metadata: SyncMetadata)
}
