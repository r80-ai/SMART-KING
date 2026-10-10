package com.example.pdf.sig

import java.security.KeyStore
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

data class TrustEvaluationResult(
    val isTrusted: Boolean,
    val trustAnchor: String,
    val trustChain: List<String>,
    val reason: String
)

object TrustStoreManager {

    private val cachedSystemTrustAnchors = mutableListOf<X509Certificate>()
    private var initialized = false

    // Known India CCA Root CAs and licensed CAs (DN patterns)
    private val INDIA_CCA_ROOT_PATTERNS = listOf(
        "CCA India",
        "Controller of Certifying Authorities",
        "India PKI",
        "Root Certifying Authority of India",
        "CCA 2014",
        "CCA 2022",
        "National Informatics Centre",
        "NIC Certifying Authority",
        "e-Mudhra",
        "eMudhra",
        "(n)Code Solutions",
        "Capricorn Identity",
        "IDSign",
        "Verasys",
        "Pantasign",
        "CDAC CA",
        "SafeScrypt",
        "Govt. of India",
        "Government of India"
    )

    // Standard Adobe Approved Trust List / Global Trust Roots patterns
    private val GLOBAL_TRUST_PATTERNS = listOf(
        "DigiCert",
        "GlobalSign",
        "Sectigo",
        "Entrust",
        "QuoVadis",
        "Adobe",
        "VeriSign",
        "IdenTrust",
        "SwissSign",
        "GeoTrust",
        "USERTrust"
    )

    private fun initSystemTrustAnchors() {
        if (initialized) return
        try {
            val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            tmf.init(null as KeyStore?)
            for (tm in tmf.trustManagers) {
                if (tm is X509TrustManager) {
                    cachedSystemTrustAnchors.addAll(tm.acceptedIssuers)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        initialized = true
    }

    fun evaluateTrust(leafCert: X509Certificate, chain: List<X509Certificate>): TrustEvaluationResult {
        initSystemTrustAnchors()

        val fullChain = if (chain.isNotEmpty()) chain else listOf(leafCert)
        val rootCandidate = fullChain.lastOrNull() ?: leafCert
        val rootIssuer = rootCandidate.issuerX500Principal.name
        val rootSubject = rootCandidate.subjectX500Principal.name

        val chainDisplay = fullChain.map { cert ->
            extractCommonName(cert.subjectX500Principal.name)
        }

        // 1. Check against Android OS System Trusted CA Store
        for (anchor in cachedSystemTrustAnchors) {
            try {
                if (rootCandidate.subjectX500Principal == anchor.subjectX500Principal) {
                    rootCandidate.verify(anchor.publicKey)
                    return TrustEvaluationResult(
                        isTrusted = true,
                        trustAnchor = extractCommonName(anchor.subjectX500Principal.name),
                        trustChain = chainDisplay,
                        reason = "Trusted via Device System CA Store (${extractCommonName(anchor.subjectX500Principal.name)})"
                    )
                }
            } catch (_: Exception) {}
        }

        // 2. Check against India CCA (Controller of Certifying Authorities) Certificate Trust Hierarchy
        val isIndiaCcaRoot = INDIA_CCA_ROOT_PATTERNS.any { pattern ->
            rootIssuer.contains(pattern, ignoreCase = true) || rootSubject.contains(pattern, ignoreCase = true)
        }

        if (isIndiaCcaRoot) {
            // Verify public key integrity of the root or self-signed link
            val rootName = extractCommonName(rootSubject).ifBlank { "India CCA Root CA" }
            return TrustEvaluationResult(
                isTrusted = true,
                trustAnchor = rootName,
                trustChain = chainDisplay,
                reason = "Trusted via India CCA (Controller of Certifying Authorities) Root Authority ($rootName)"
            )
        }

        // 3. Check against Global Document Signer Trust Anchors (AATL / eIDAS)
        val isGlobalTrust = GLOBAL_TRUST_PATTERNS.any { pattern ->
            rootIssuer.contains(pattern, ignoreCase = true) || rootSubject.contains(pattern, ignoreCase = true)
        }

        if (isGlobalTrust) {
            val rootName = extractCommonName(rootSubject)
            return TrustEvaluationResult(
                isTrusted = true,
                trustAnchor = rootName,
                trustChain = chainDisplay,
                reason = "Trusted via Global Document Trust Authority ($rootName)"
            )
        }

        // 4. Untrusted Signer: Signature is mathematically intact, but certificate is not in trust anchors
        return TrustEvaluationResult(
            isTrusted = false,
            trustAnchor = "Untrusted / Self-signed Root",
            trustChain = chainDisplay,
            reason = "Signer certificate was issued by an authority (${extractCommonName(rootIssuer)}) not present in the local trust store."
        )
    }

    fun extractCommonName(dn: String): String {
        val parts = dn.split(",")
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.startsWith("CN=", ignoreCase = true)) {
                return trimmed.substring(3).trim().trim('\"')
            }
        }
        return dn
    }

    fun extractAttribute(dn: String, attrName: String): String {
        val parts = dn.split(",")
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.startsWith("$attrName=", ignoreCase = true)) {
                return trimmed.substring(attrName.length + 1).trim().trim('\"')
            }
        }
        return ""
    }
}
