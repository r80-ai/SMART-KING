package com.example.pdf.sig

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SignatureVerificationEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testUnsignedPdfReturnsNotSigned() = runBlocking {
        val dummyPdf = tempFolder.newFile("sample_unsigned.pdf")
        dummyPdf.writeText("%PDF-1.4\n1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n%%EOF")

        val result = SignatureVerificationEngine.verifySignatures(dummyPdf)
        assertFalse(result.hasSignatures)
        assertEquals(0, result.totalSignatures)
        assertEquals(SignatureValidity.NOT_SIGNED, result.overallValidity)
        assertEquals("sample_unsigned.pdf", result.analyzedFileName)
    }

    @Test
    fun testNonExistentFileReturnsInvalid() = runBlocking {
        val nonExistent = File(tempFolder.root, "missing.pdf")
        val result = SignatureVerificationEngine.verifySignatures(nonExistent)
        assertFalse(result.hasSignatures)
        assertEquals(SignatureValidity.INVALID, result.overallValidity)
        assertNotNull(result.error)
    }

    @Test
    fun testSignedPdfRecognition() = runBlocking {
        val signedPdf = tempFolder.newFile("sample_signed.pdf")
        val content = buildString {
            append("%PDF-1.7\n")
            append("1 0 obj\n<< /Type /Sig /Filter /Adobe.PPKLite /SubFilter /adbe.pkcs7.detached ")
            append("/ByteRange [ 0 100 200 50 ] ")
            append("/Name (Controller of Certifying Authorities) ")
            append("/Reason (Approved and Signed) ")
            append("/Location (New Delhi) ")
            append("/M (D:20240101120000+05'30') ")
            append(">>\nendobj\n")
            append("%%EOF\n")
        }
        // Pad to at least 250 bytes so byte ranges are within bounds
        val paddedContent = content.padEnd(300, ' ')
        signedPdf.writeBytes(paddedContent.toByteArray(Charsets.ISO_8859_1))

        val result = SignatureVerificationEngine.verifySignatures(signedPdf)
        assertTrue(result.hasSignatures)
        assertEquals(1, result.totalSignatures)
        val sig = result.signatures.first()
        assertEquals("Controller of Certifying Authorities", sig.signerName)
        assertEquals("Approved and Signed", sig.reason)
        assertEquals("New Delhi", sig.location)
    }
}
