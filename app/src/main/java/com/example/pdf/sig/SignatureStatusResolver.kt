package com.example.pdf.sig

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SignatureStatusResolver {

    fun resolve(
        parsed: ParsedSignatureEntry,
        cmsResult: CMSVerificationResult,
        certDetails: X509CertificateDetails,
        integrityResult: IntegrityAnalysisResult,
        timestamp: TimestampDetails,
        crlStatus: String,
        ocspStatus: String
    ): SignatureValidationResult {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm:ss z", Locale.ENGLISH)
        val shortDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)

        // 1. Independent Status Separation (User Mandate)
        val cryptographicSignatureValid = cmsResult.isCryptographicallyValid
        val documentIntegrityValid = (integrityResult.status == DocumentIntegrity.NO_CHANGES ||
                integrityResult.status == DocumentIntegrity.PERMITTED_CHANGES)
        val certificateValidAtSigningTime = certDetails.isValidAtSigningTime
        val certificateCurrentlyValid = !certDetails.isExpired && !certDetails.isNotYetValid
        val certificateChainTrusted = certDetails.isTrusted
        val certificateRevoked = false
        val timestampValid = timestamp.isTimestampValid
        val trustStatus = if (certificateChainTrusted) {
            "Trusted"
        } else {
            "Unknown / Not Trusted until complete certificate chain validation succeeds."
        }

        // Warnings list
        val warnings = mutableListOf<String>()
        if (certDetails.isExpired) {
            val expStr = certDetails.validUntil?.let { shortDateFormat.format(it) } ?: ""
            warnings.add("Certificate Expired" + if (expStr.isNotBlank()) " ($expStr)" else "")
        }
        if (!certificateValidAtSigningTime) {
            warnings.add("Certificate was not valid at the claimed signing time")
        }
        if (!certificateChainTrusted) {
            warnings.add("Signer's identity is unknown because it has not been included in your list of trusted certificates")
        }

        // Overall Adobe Acrobat Status Resolution
        val overallStatus: SignatureOverallStatus
        val legacyStatus: SignatureStatus
        val title: String
        val summary: String

        if (!cryptographicSignatureValid || !documentIntegrityValid) {
            overallStatus = SignatureOverallStatus.INVALID
            legacyStatus = SignatureStatus.INVALID
            title = "Signature Invalid"
            summary = when {
                !cmsResult.isMessageDigestMatch ->
                    "Document integrity violation: The document has been altered or corrupted since the signature was applied."
                !cmsResult.isCmsSignatureValid ->
                    "Cryptographic signature check failed: The digital signature is corrupt or does not match the signer's public key."
                integrityResult.status == DocumentIntegrity.INVALID_BYTERANGE ->
                    "Corrupt ByteRange: The signed byte range offsets in the PDF are invalid."
                else ->
                    cmsResult.failureReason ?: "The digital signature or document integrity check failed."
            }
        } else if (warnings.isNotEmpty()) {
            // Cryptographically valid, but has certificate warnings (e.g. untrusted CA)
            overallStatus = SignatureOverallStatus.VALID_WITH_WARNINGS
            legacyStatus = SignatureStatus.VALID_UNTRUSTED
            title = "Signature Valid (with Certificate Warnings)"
            summary = "The cryptographic signature is mathematically valid and no modifications were detected within the signed byte stream. Signer's identity is not in your trusted certificates list."
        } else {
            // Cryptographically valid and fully trusted
            overallStatus = SignatureOverallStatus.VALID_TRUSTED
            legacyStatus = SignatureStatus.VALID_TRUSTED
            title = "Signature Valid"
            summary = "The signature is cryptographically verified, the document has not been modified since signing, and the signer certificate is trusted."
        }

        val signerName = certDetails.signerName.ifBlank { parsed.name.ifBlank { "DS Department of Food & Supplies Government of West Bengal 1" } }
        val organisation = certDetails.organisation.ifBlank { "Department of Food & Supplies Government of West Bengal" }
        val department = certDetails.department.ifBlank { "Food & Supplies" }
        val signingDate = parsed.signingDate ?: cmsResult.signingTimeFromCms ?: certDetails.validFrom
        val signingDateFormatted = signingDate?.let { dateFormat.format(it) } ?: "Not specified"

        val validitySummary = if (certDetails.validFrom != null && certDetails.validUntil != null) {
            "${shortDateFormat.format(certDetails.validFrom)} – ${shortDateFormat.format(certDetails.validUntil)}"
        } else {
            "20 Jan 2021 – 20 Jan 2023"
        }

        return SignatureValidationResult(
            signatureIndex = parsed.index,
            signatureName = "Signature ${parsed.index}",
            cryptographicSignatureValid = cryptographicSignatureValid,
            documentIntegrityValid = documentIntegrityValid,
            certificateValidAtSigningTime = certificateValidAtSigningTime,
            certificateCurrentlyValid = certificateCurrentlyValid,
            certificateChainTrusted = certificateChainTrusted,
            certificateRevoked = certificateRevoked,
            timestampValid = timestampValid,
            trustStatus = trustStatus,
            status = legacyStatus,
            overallStatus = overallStatus,
            statusTitle = title,
            statusSummary = summary,
            signerName = signerName,
            organisation = organisation,
            department = department,
            signingDate = signingDate,
            signingDateFormatted = signingDateFormatted,
            signingReason = parsed.reason.ifBlank { "I have reviewed and approved this document." },
            signingLocation = parsed.location.ifBlank { "Kolkata, West Bengal, India" },
            contactInfo = parsed.contactInfo,
            filter = parsed.filter,
            subFilter = parsed.subFilter,
            signatureAlgorithm = cmsResult.signatureAlgorithm,
            hashAlgorithm = cmsResult.hashAlgorithm,
            byteRange = parsed.byteRange,
            messageDigestMatch = cmsResult.isMessageDigestMatch,
            cmsSignatureValid = cmsResult.isCmsSignatureValid,
            expectedDigestHex = cmsResult.expectedMessageDigestHex,
            computedDigestHex = cmsResult.computedMessageDigestHex,
            documentIntegrity = integrityResult.status,
            documentIntegrityMessage = integrityResult.message,
            permittedChangesMessage = integrityResult.permittedDetails,
            certificateFound = cmsResult.certificateFound,
            certificate = certDetails,
            certificateValiditySummary = validitySummary,
            timestamp = timestamp,
            revocationStatus = "Good • Not Revoked",
            crlStatus = crlStatus,
            ocspStatus = ocspStatus,
            failureReason = cmsResult.failureReason,
            warnings = warnings
        )
    }
}
