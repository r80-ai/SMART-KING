package com.example.amardokan.ui.memo

import android.content.Context
import com.example.amardokan.data.Bill
import com.example.amardokan.data.BillItem
import java.io.File

object AmarDokanCashMemoGenerator {
    data class MemoData(
        val bill: Bill,
        val items: List<BillItem>
    ) {
        companion object {
            fun fromBill(bill: Bill, items: List<BillItem>): MemoData {
                return MemoData(bill, items)
            }
        }
    }

    fun generatePdfFile(context: Context, memoData: MemoData): File {
        val dir = File(context.cacheDir, "memos").apply { mkdirs() }
        val cleanBillNo = memoData.bill.billNumber.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val cleanName = memoData.bill.customerName.ifBlank { "Unknown" }.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val file = File(dir, "${cleanName}_${cleanBillNo}.pdf")
        if (!file.exists()) {
            file.writeBytes(byteArrayOf())
        }
        return file
    }
}
