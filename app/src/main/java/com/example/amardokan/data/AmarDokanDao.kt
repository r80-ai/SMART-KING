package com.example.amardokan.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AmarDokanDao {
    @Query("SELECT * FROM ad_customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM ad_customers")
    fun getAllCustomersSync(): List<Customer>

    @Query("SELECT * FROM ad_services ORDER BY name ASC")
    fun getAllServices(): Flow<List<ServiceItem>>

    @Query("SELECT * FROM ad_services")
    fun getAllServicesSync(): List<ServiceItem>

    @Query("SELECT * FROM ad_bills ORDER BY dateMillis DESC")
    fun getAllBills(): Flow<List<Bill>>

    @Query("SELECT * FROM ad_bills")
    fun getAllBillsSync(): List<Bill>

    @Query("SELECT * FROM ad_bill_items")
    fun getAllBillItemsSync(): List<BillItem>

    @Query("SELECT * FROM ad_bill_items WHERE billId = :billId")
    fun getBillItemsByBillIdSync(billId: Long): List<BillItem>

    @Query("SELECT * FROM ad_bill_items WHERE billId = :billId")
    fun getBillItemsByBillId(billId: Long): Flow<List<BillItem>>

    @Query("SELECT * FROM ad_payments ORDER BY dateMillis DESC")
    fun getAllPayments(): Flow<List<Payment>>

    @Query("SELECT * FROM ad_payments")
    fun getAllPaymentsSync(): List<Payment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: ServiceItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: Bill): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillItem(billItem: BillItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCustomers(customers: List<Customer>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllServices(services: List<ServiceItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllBills(bills: List<Bill>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllBillItems(items: List<BillItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPayments(payments: List<Payment>)

    @Query("DELETE FROM ad_customers")
    suspend fun clearCustomers()

    @Query("DELETE FROM ad_services")
    suspend fun clearServices()

    @Query("DELETE FROM ad_bills")
    suspend fun clearBills()

    @Query("DELETE FROM ad_bill_items")
    suspend fun clearBillItems()

    @Query("DELETE FROM ad_payments")
    suspend fun clearPayments()

    @Transaction
    suspend fun restoreAllData(
        customers: List<Customer>,
        services: List<ServiceItem>,
        bills: List<Bill>,
        billItems: List<BillItem>,
        payments: List<Payment>
    ) {
        clearBillItems()
        clearPayments()
        clearBills()
        clearServices()
        clearCustomers()
        insertAllCustomers(customers)
        insertAllServices(services)
        insertAllBills(bills)
        insertAllBillItems(billItems)
        insertAllPayments(payments)
    }
}
