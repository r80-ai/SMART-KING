package com.example.pdf.sig

import java.io.File

data class IntegrityAnalysisResult(
    val status: DocumentIntegrity,
    val message: String,
    val permittedDetails: String,
    val appendedBytesCount: Long
)

object DocumentIntegrityChecker {

    fun checkIntegrity(pdfFile: File, byteRange: LongArray): IntegrityAnalysisResult {
        val fileLength = pdfFile.length()

        val r0 = byteRange[0]
        val r1 = byteRange[1]
        val r2 = byteRange[2]
        val r3 = byteRange[3]

        if (r0 != 0L || r1 <= 0 || r2 < r1 || r3 <= 0 || (r2 + r3) > fileLength) {
            return IntegrityAnalysisResult(
                status = DocumentIntegrity.INVALID_BYTERANGE,
                message = "The PDF ByteRange array is corrupted or out of bounds.",
                permittedDetails = "None",
                appendedBytesCount = 0
            )
        }

        val signedEnd = r2 + r3
        val appendedBytesCount = fileLength - signedEnd

        if (appendedBytesCount == 0L) {
            return IntegrityAnalysisResult(
                status = DocumentIntegrity.NO_CHANGES,
                message = "Document has not been modified since this signature was applied.",
                permittedDetails = "Zero post-signing modifications detected. Strict document integrity confirmed.",
                appendedBytesCount = 0
            )
        }

        // Incremental update detected. Read appended bytes to classify change type
        val appendedBytes = ByteArray(appendedBytesCount.toInt().coerceAtMost(65536))
        java.io.RandomAccessFile(pdfFile, "r").use { raf ->
            raf.seek(signedEnd)
            raf.readFully(appendedBytes)
        }
        val appendedStr = String(appendedBytes, Charsets.ISO_8859_1)

        val isAnnotationOrForm = appendedStr.contains("/Annot") ||
                appendedStr.contains("/Widget") ||
                appendedStr.contains("/AcroForm") ||
                appendedStr.contains("/Type /Sig")

        return if (isAnnotationOrForm) {
            IntegrityAnalysisResult(
                status = DocumentIntegrity.PERMITTED_CHANGES,
                message = "Document was modified after signing, but changes are permitted incremental updates (form fields, annotations, or counter-signatures).",
                permittedDetails = "Permitted changes: Interactive form field entries or annotations were added incrementally.",
                appendedBytesCount = appendedBytesCount
            )
        } else {
            IntegrityAnalysisResult(
                status = DocumentIntegrity.UNAUTHORIZED_CHANGES,
                message = "Document has been modified after this signature was applied. Structural changes detected.",
                permittedDetails = "Unauthorized changes: Unverified post-signing modifications exist.",
                appendedBytesCount = appendedBytesCount
            )
        }
    }
}
