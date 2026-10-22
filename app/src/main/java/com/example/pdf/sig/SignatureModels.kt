package com.example.pdf.sig

import java.util.Date

enum class SignatureValidity {
    VALID,           // Cryptographically recognized & valid signature
    WARNING,         // Recognized but expired, untrusted root, or revised
    INVALID,         // Corrupted, byte range mismatch, or tampered
    NOT_SIGNED       // No digital signature in PDF
}

data class SignatureLocation(
    val pageIndex: Int = 0,
    val leftFraction: Float = 0.54f,
    val topFraction: Float = 0.78f,
    val widthFraction: Float = 0.42f,
    val heightFraction: Float = 0.15f,
    val isExplicitRect: Boolean = false
)

data class CertificateDetails(
    val commonName: String,
    val organization: String = "",
    val organizationalUnit: String = "",
    val country: String = "",
    val issuerCommonName: String,
    val issuerOrganization: String = "",
    val serialNumber: String,
    val validFrom: Date?,
    val validUntil: Date?,
    val signatureAlgorithm: String,
    val isExpired: Boolean,
    val isSelfSigned: Boolean,
    val isGovOrUidaiCertified: Boolean,
    val isValidAtSigning: Boolean = true
)

data class SignatureInfo(
    val signerName: String,
    val signingTime: String,
    val reason: String = "",
    val location: String = "",
    val contactInfo: String = "",
    val filter: String = "",
    val subFilter: String = "",
    val byteRange: List<Long> = emptyList(),
    val isByteRangeIntact: Boolean = true,
    val isLatestRevision: Boolean = true,
    val validity: SignatureValidity = SignatureValidity.VALID,
    val statusMessage: String = "",
    val certificate: CertificateDetails? = null,
    val pageLocation: SignatureLocation = SignatureLocation()
)

data class SignatureVerificationResult(
    val hasSignatures: Boolean,
    val totalSignatures: Int,
    val overallValidity: SignatureValidity,
    val summaryTitle: String,
    val summaryDescription: String,
    val signatures: List<SignatureInfo> = emptyList(),
    val analyzedFileSize: Long = 0L,
    val analyzedFileName: String = "",
    val error: String? = null
)
