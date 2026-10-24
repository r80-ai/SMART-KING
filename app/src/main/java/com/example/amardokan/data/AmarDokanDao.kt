package com.example.amardokan.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AmarDokanDao {
    @Query("SELECT * FROM ad_customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM ad_customers")
    fun getAllCustomersSync(): List<Customer>

    @Query("SELECT * FROM ad_customers WHERE id = :customerId LIMIT 1")
    suspend fun getCustomerById(customerId: Long): Customer?

    @Query("SELECT * FROM ad_services ORDER BY id ASC")
    fun getAllServices(): Flow<List<ServiceItem>>

    @Query("SELECT * FROM ad_services")
    fun getAllServicesSync(): List<ServiceItem>

    @Query("SELECT COUNT(*) FROM ad_services")
    suspend fun countServices(): Int

    @Query("SELECT * FROM ad_bills ORDER BY dateMillis DESC")
    fun getAllBills(): Flow<List<Bill>>

    @Query("SELECT * FROM ad_bills")
    fun getAllBillsSync(): List<Bill>

    @Query("SELECT * FROM ad_bills WHERE customerId = :customerId ORDER BY dateMillis ASC")
    fun getBillsByCustomerId(customerId: Long): Flow<List<Bill>>

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

    @Query("SELECT * FROM ad_payments WHERE customerId = :customerId ORDER BY dateMillis ASC")
    fun getPaymentsByCustomerId(customerId: Long): Flow<List<Payment>>

    @Query("SELECT COALESCE(SUM(grandTotal), 0.0) FROM ad_bills WHERE customerId = :customerId AND paymentType = 'CREDIT'")
    suspend fun getTotalCreditBillsForCustomer(customerId: Long): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM ad_payments WHERE customerId = :customerId")
    suspend fun getTotalPaymentsForCustomer(customerId: Long): Double

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: ServiceItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServices(services: List<ServiceItem>)

    @Update
    suspend fun updateService(service: ServiceItem)

    @Delete
    suspend fun deleteService(service: ServiceItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: Bill): Long

    @Delete
    suspend fun deleteBill(bill: Bill)

    @Query("DELETE FROM ad_bill_items WHERE billId = :billId")
    suspend fun deleteBillItemsByBillId(billId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillItem(billItem: BillItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllBillItems(items: List<BillItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    @Delete
    suspend fun deletePayment(payment: Payment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCustomers(customers: List<Customer>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllServices(services: List<ServiceItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllBills(bills: List<Bill>)

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
    suspend fun saveBillWithItems(bill: Bill, items: List<BillItem>): Long {
        val billId = insertBill(bill)
        val itemsWithId = items.map { it.copy(billId = billId) }
        insertAllBillItems(itemsWithId)
        return billId
    }

    @Transaction
    suspend fun deleteBillCompletely(bill: Bill) {
        deleteBillItemsByBillId(bill.id)
        deleteBill(bill)
    }

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
