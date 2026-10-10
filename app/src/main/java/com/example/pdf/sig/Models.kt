package com.example.pdf.sig

import java.util.Date

enum class SignatureStatus {
    VALID_TRUSTED,       // Green Checkmark: Cryptographically valid & trusted root
    VALID_UNTRUSTED,     // Yellow Warning: Cryptographically valid, but signer certificate is not in trusted store
    INVALID              // Red Cross: Cryptographic verification failed, document modified, etc.
}

enum class SignatureOverallStatus {
    VALID_TRUSTED,       // Green Checkmark
    VALID_WITH_WARNINGS, // Yellow / Amber: Cryptographically valid, but cert expired, untrusted root, or not valid at signing time
    INVALID              // Red Cross: Signature math failed, altered content, or corrupt ByteRange
}

enum class TrustStatus {
    TRUSTED,
    UNTRUSTED,
    UNKNOWN
}

enum class DocumentIntegrity {
    NO_CHANGES,               // ✓ No changes since signing
    PERMITTED_CHANGES,        // ⚠ Permitted incremental changes (e.g. form filling, annotations)
    UNAUTHORIZED_CHANGES,     // ✕ Unauthorized structural changes detected after signing
    INVALID_BYTERANGE         // ✕ Corrupted or invalid ByteRange
}

data class X509CertificateDetails(
    val subjectDn: String,
    val signerName: String,
    val organisation: String,
    val department: String,
    val country: String,
    val issuerDn: String,
    val issuerName: String,
    val serialNumberHex: String,
    val version: Int,
    val validFrom: Date?,
    val validUntil: Date?,
    val isExpired: Boolean,            // Currently expired relative to now
    val isValidAtSigningTime: Boolean, // Valid at the claimed signing time
    val isNotYetValid: Boolean,
    val publicKeyAlgorithm: String,
    val keySize: Int,
    val signatureAlgorithm: String,
    val sha256Fingerprint: String,
    val sha1Fingerprint: String,
    val keyUsages: List<String>,
    val isTrusted: Boolean,
    val trustReason: String,
    val certificateChain: List<String> = emptyList(),
    val rootCaName: String = ""
)

data class TimestampDetails(
    val hasTimestamp: Boolean,
    val timestampDate: Date? = null,
    val timestampAuthority: String = "",
    val isTimestampValid: Boolean = false
)

data class SignatureValidationResult(
    val signatureIndex: Int,
    val signatureName: String,

    // Independent Status Flags (User Requirement: Do NOT combine into one boolean)
    val cryptographicSignatureValid: Boolean,
    val documentIntegrityValid: Boolean,
    val certificateValidAtSigningTime: Boolean,
    val certificateCurrentlyValid: Boolean,
    val certificateChainTrusted: Boolean,
    val certificateRevoked: Boolean,
    val timestampValid: Boolean,
    val trustStatus: String,

    // Overall / UI status
    val status: SignatureStatus, // Kept for backwards compatibility
    val overallStatus: SignatureOverallStatus,
    val statusTitle: String,
    val statusSummary: String,

    // Signer and signing metadata
    val signerName: String,
    val organisation: String,
    val department: String,
    val signingDate: Date?,
    val signingDateFormatted: String,
    val signingReason: String,
    val signingLocation: String,
    val contactInfo: String,

    // Low-level verification indicators
    val filter: String,
    val subFilter: String,
    val signatureAlgorithm: String,
    val hashAlgorithm: String,
    val byteRange: LongArray,
    val messageDigestMatch: Boolean,
    val cmsSignatureValid: Boolean,
    val expectedDigestHex: String = "",
    val computedDigestHex: String = "",

    // Document integrity
    val documentIntegrity: DocumentIntegrity,
    val documentIntegrityMessage: String,
    val permittedChangesMessage: String,

    // Certificate details
    val certificateFound: Boolean,
    val certificate: X509CertificateDetails,
    val certificateValiditySummary: String,

    // Timestamp and revocation
    val timestamp: TimestampDetails,
    val revocationStatus: String,
    val crlStatus: String,
    val ocspStatus: String,

    val failureReason: String? = null,
    val warnings: List<String> = emptyList()
)
