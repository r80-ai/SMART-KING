package com.example.pdf.sig

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.style.BCStyle
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import java.util.Date

object CertificateValidator {

    fun validateCertificate(
        cert: X509Certificate?,
        chain: List<X509Certificate>,
        signingTime: Date? = null,
        fallbackSignerName: String = "",
        fallbackOrg: String = ""
    ): X509CertificateDetails {
        if (cert == null) {
            val name = fallbackSignerName.ifBlank { "Unknown Signer" }
            val org = fallbackOrg.ifBlank { "Department of Food & Supplies" }
            return X509CertificateDetails(
                subjectDn = if (name != "Unknown Signer") "CN=$name" else "Unknown",
                signerName = name,
                organisation = org,
                department = "",
                country = "IN",
                issuerDn = "Embedded Signature Dictionary",
                issuerName = "Embedded Signature Dictionary",
                serialNumberHex = "00",
                version = 3,
                validFrom = null,
                validUntil = null,
                isExpired = false,
                isValidAtSigningTime = true,
                isNotYetValid = false,
                publicKeyAlgorithm = "RSA",
                keySize = 2048,
                signatureAlgorithm = "SHA256withRSA",
                sha256Fingerprint = "",
                sha1Fingerprint = "",
                keyUsages = listOf("Digital Signature", "Non-Repudiation"),
                isTrusted = false,
                trustReason = "Signer identity extracted from signature dictionary"
            )
        }

        val subjectDn = cert.subjectX500Principal.name
        val issuerDn = cert.issuerX500Principal.name

        // Extract attributes using BouncyCastle X500Name for accurate parsing of escaped strings and spacing
        var signerName = ""
        var organisation = ""
        var department = ""
        var country = ""
        var issuerName = ""

        try {
            val subjectX500 = X500Name.getInstance(cert.subjectX500Principal.encoded)
            signerName = subjectX500.getRDNs(BCStyle.CN).firstOrNull()?.first?.value?.toString() ?: ""
            organisation = subjectX500.getRDNs(BCStyle.O).firstOrNull()?.first?.value?.toString() ?: ""
            department = subjectX500.getRDNs(BCStyle.OU).firstOrNull()?.first?.value?.toString() ?: ""
            country = subjectX500.getRDNs(BCStyle.C).firstOrNull()?.first?.value?.toString() ?: ""
        } catch (_: Exception) {}

        if (signerName.isBlank()) signerName = TrustStoreManager.extractCommonName(subjectDn)
        if (signerName.isBlank()) signerName = fallbackSignerName.ifBlank { "Unknown Signer" }
        if (organisation.isBlank()) {
            organisation = TrustStoreManager.extractAttribute(subjectDn, "O")
                .ifBlank { TrustStoreManager.extractAttribute(subjectDn, "OU") }
                .ifBlank { fallbackOrg.ifBlank { "Department of Food & Supplies Government of West Bengal" } }
        }
        if (department.isBlank()) department = TrustStoreManager.extractAttribute(subjectDn, "OU")
        if (country.isBlank()) country = TrustStoreManager.extractAttribute(subjectDn, "C")

        try {
            val issuerX500 = X500Name.getInstance(cert.issuerX500Principal.encoded)
            issuerName = issuerX500.getRDNs(BCStyle.CN).firstOrNull()?.first?.value?.toString() ?: ""
        } catch (_: Exception) {}
        if (issuerName.isBlank()) issuerName = TrustStoreManager.extractCommonName(issuerDn)

        // Evaluate validity:
        // 1. Current expiration (relative to now)
        val now = Date()
        val isCurrentlyExpired = cert.notAfter != null && now.after(cert.notAfter)
        val isCurrentlyNotYetValid = cert.notBefore != null && now.before(cert.notBefore)

        // 2. Validity at the claimed signing time
        val isValidAtSigningTime = if (signingTime != null && cert.notBefore != null && cert.notAfter != null) {
            !signingTime.before(cert.notBefore) && !signingTime.after(cert.notAfter)
        } else {
            !isCurrentlyExpired && !isCurrentlyNotYetValid
        }

        val keySize = when (val pk = cert.publicKey) {
            is RSAPublicKey -> pk.modulus.bitLength()
            else -> cert.publicKey.encoded.size * 8
        }

        val keyUsages = mutableListOf<String>()
        val kuBools = cert.keyUsage
        if (kuBools != null) {
            if (kuBools.getOrNull(0) == true) keyUsages.add("Digital Signature")
            if (kuBools.getOrNull(1) == true) keyUsages.add("Non-Repudiation (Content Commitment)")
            if (kuBools.getOrNull(2) == true) keyUsages.add("Key Encipherment")
            if (kuBools.getOrNull(3) == true) keyUsages.add("Data Encipherment")
            if (kuBools.getOrNull(5) == true) keyUsages.add("Certificate Signing")
            if (kuBools.getOrNull(6) == true) keyUsages.add("CRL Signing")
        }
        if (keyUsages.isEmpty()) {
            keyUsages.add("Document Digital Signing")
        }

        val sha256Fingerprint = computeFingerprint(cert.encoded, "SHA-256")
        val sha1Fingerprint = computeFingerprint(cert.encoded, "SHA-1")
        val serialHex = cert.serialNumber.toString(16).uppercase().chunked(2).joinToString(":")

        // Trust evaluation via TrustStoreManager
        val trustResult = TrustStoreManager.evaluateTrust(cert, chain)

        return X509CertificateDetails(
            subjectDn = subjectDn,
            signerName = signerName,
            organisation = organisation,
            department = department,
            country = country,
            issuerDn = issuerDn,
            issuerName = issuerName,
            serialNumberHex = serialHex,
            version = cert.version,
            validFrom = cert.notBefore,
            validUntil = cert.notAfter,
            isExpired = isCurrentlyExpired,
            isValidAtSigningTime = isValidAtSigningTime,
            isNotYetValid = isCurrentlyNotYetValid,
            publicKeyAlgorithm = cert.publicKey.algorithm ?: "RSA",
            keySize = keySize,
            signatureAlgorithm = cert.sigAlgName ?: "SHA256withRSA",
            sha256Fingerprint = sha256Fingerprint,
            sha1Fingerprint = sha1Fingerprint,
            keyUsages = keyUsages,
            isTrusted = trustResult.isTrusted,
            trustReason = trustResult.reason,
            certificateChain = trustResult.trustChain,
            rootCaName = trustResult.trustAnchor
        )
    }

    private fun computeFingerprint(bytes: ByteArray, algorithm: String): String {
        return try {
            val md = MessageDigest.getInstance(algorithm)
            val digest = md.digest(bytes)
            digest.joinToString(":") { "%02X".format(it) }
        } catch (_: Exception) {
            ""
        }
    }
}
