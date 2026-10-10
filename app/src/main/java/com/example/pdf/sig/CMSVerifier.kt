package com.example.pdf.sig

import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1Primitive
import org.bouncycastle.asn1.ASN1Set
import org.bouncycastle.asn1.cms.CMSAttributes
import org.bouncycastle.asn1.cms.ContentInfo
import org.bouncycastle.asn1.cms.SignedData
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.cms.SignerInformation
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.security.Security
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Date

data class CMSVerificationResult(
    val isCryptographicallyValid: Boolean,
    val isCmsSignatureValid: Boolean,
    val isMessageDigestMatch: Boolean,
    val signerCertificate: X509Certificate?,
    val certificateFound: Boolean,
    val certificateChain: List<X509Certificate>,
    val signatureAlgorithm: String,
    val hashAlgorithm: String,
    val signingTimeFromCms: Date?,
    val expectedMessageDigestHex: String = "",
    val computedMessageDigestHex: String = "",
    val failureReason: String? = null
)

object CMSVerifier {

    private val bcProvider: BouncyCastleProvider by lazy {
        val p = BouncyCastleProvider()
        try {
            Security.removeProvider("BC")
            Security.insertProviderAt(p, 1)
        } catch (_: Exception) {
            if (Security.getProvider("BC") == null) {
                Security.addProvider(p)
            }
        }
        p
    }

    init {
        // Trigger provider initialization
        try {
            val p = bcProvider
            if (Security.getProvider("BC") == null) {
                Security.addProvider(p)
            }
        } catch (_: Exception) {}
    }

    /**
     * Verifies PKCS#7 / CMS detached signature:
     * 1. Remove only the unused zero padding from Contents
     * 2. Parse CMS DER -> ContentInfo -> SignedData -> certificates & SignerInfos
     * 3. Extract X.509 certificates and match SignerInfo to certificate (Issuer + Serial Number)
     * 4. Calculate digest over exact ByteRange stream and compare with CMS messageDigest
     * 5. Verify RSA/ECDSA signature over DER encoded SignedAttributes (SET OF signed attributes)
     */
    fun verifyCMS(contentsBytes: ByteArray, signedBytes: ByteArray): CMSVerificationResult {
        try {
            if (contentsBytes.isEmpty()) {
                return CMSVerificationResult(
                    isCryptographicallyValid = false,
                    isCmsSignatureValid = false,
                    isMessageDigestMatch = false,
                    signerCertificate = null,
                    certificateFound = false,
                    certificateChain = emptyList(),
                    signatureAlgorithm = "Unknown",
                    hashAlgorithm = "Unknown",
                    signingTimeFromCms = null,
                    failureReason = "Empty signature contents"
                )
            }

            // Step 4: Remove unused zero padding from Contents to extract exact DER
            val exactDerBytes = extractExactDerBytes(contentsBytes)

            // Step 5: Parse CMS DER / CMSSignedData
            var cms: CMSSignedData? = null
            try {
                cms = CMSSignedData(exactDerBytes)
            } catch (_: Exception) {
                try {
                    cms = CMSSignedData(contentsBytes)
                } catch (_: Exception) {}
            }

            var primitive: ASN1Primitive? = null
            if (cms == null) {
                try {
                    primitive = ASN1InputStream(ByteArrayInputStream(exactDerBytes)).readObject()
                } catch (_: Exception) {}

                if (primitive == null) {
                    try {
                        primitive = ASN1InputStream(ByteArrayInputStream(contentsBytes)).readObject()
                    } catch (_: Exception) {}
                }

                if (primitive != null) {
                    try {
                        val contentInfo = ContentInfo.getInstance(primitive)
                        cms = CMSSignedData(contentInfo)
                    } catch (_: Exception) {}
                }
            }

            if (cms == null) {
                return CMSVerificationResult(
                    isCryptographicallyValid = false,
                    isCmsSignatureValid = false,
                    isMessageDigestMatch = false,
                    signerCertificate = null,
                    certificateFound = false,
                    certificateChain = emptyList(),
                    signatureAlgorithm = "Unknown",
                    hashAlgorithm = "Unknown",
                    signingTimeFromCms = null,
                    failureReason = "Failed to parse CMS / PKCS#7 signed data structure"
                )
            }

            val signedData = try {
                SignedData.getInstance(cms.toASN1Structure().content)
            } catch (_: Exception) {
                null
            }

            // Step 6: Extract certificates safely on Android without hardcoded "BC" CertificateFactory
            val p = bcProvider
            val certConverter = try {
                JcaX509CertificateConverter().setProvider(p)
            } catch (_: Exception) {
                JcaX509CertificateConverter()
            }

            val certFactory: CertificateFactory = try {
                CertificateFactory.getInstance("X.509")
            } catch (_: Exception) {
                try {
                    CertificateFactory.getInstance("X.509", p)
                } catch (_: Exception) {
                    CertificateFactory.getInstance("X.509")
                }
            }

            val certList = mutableListOf<X509Certificate>()

            // A. From BouncyCastle CMS certificates store
            try {
                for (holder in cms.certificates.getMatches(null)) {
                    try {
                        val cert = certConverter.getCertificate(holder)
                        if (!certList.contains(cert)) {
                            certList.add(cert)
                        }
                    } catch (_: Exception) {
                        try {
                            val cert = certFactory.generateCertificate(
                                ByteArrayInputStream(holder.encoded)
                            ) as? X509Certificate
                            if (cert != null && !certList.contains(cert)) {
                                certList.add(cert)
                            }
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}

            // B. Directly from SignedData ASN1Set of certificates if available
            val certSet: ASN1Set? = signedData?.certificates
            if (certSet != null) {
                for (i in 0 until certSet.size()) {
                    try {
                        val certObj = certSet.getObjectAt(i)
                        val certBytes = certObj.toASN1Primitive().encoded
                        val cert = certFactory.generateCertificate(
                            ByteArrayInputStream(certBytes)
                        ) as? X509Certificate
                        if (cert != null && !certList.contains(cert)) {
                            certList.add(cert)
                        }
                    } catch (_: Exception) {
                        try {
                            val certObj = certSet.getObjectAt(i)
                            val holder = X509CertificateHolder(certObj.toASN1Primitive().encoded)
                            val cert = certConverter.getCertificate(holder)
                            if (cert != null && !certList.contains(cert)) {
                                certList.add(cert)
                            }
                        } catch (_: Exception) {}
                    }
                }
            }

            // Step 7: Match SignerInfo to certificate
            val signers = cms.signerInfos.signers
            if (signers.isEmpty()) {
                return CMSVerificationResult(
                    isCryptographicallyValid = false,
                    isCmsSignatureValid = false,
                    isMessageDigestMatch = false,
                    signerCertificate = certList.firstOrNull(),
                    certificateFound = certList.isNotEmpty(),
                    certificateChain = certList,
                    signatureAlgorithm = "Unknown",
                    hashAlgorithm = "Unknown",
                    signingTimeFromCms = null,
                    failureReason = "No signers found in PKCS#7 / CMS signature data"
                )
            }

            val signer: SignerInformation = signers.first()
            val sid = signer.sid

            var signerCert: X509Certificate? = null

            // Match by Serial Number and Issuer in extracted certList
            if (sid.serialNumber != null) {
                val sidIssuerStr = sid.issuer?.toString()
                signerCert = certList.firstOrNull { c ->
                    c.serialNumber == sid.serialNumber &&
                    (sidIssuerStr == null || c.issuerX500Principal.name.contains(sidIssuerStr, true) || sidIssuerStr.contains(c.issuerX500Principal.name, true))
                } ?: certList.firstOrNull { it.serialNumber == sid.serialNumber }
            }

            if (signerCert == null && sid.subjectKeyIdentifier != null) {
                val skid = sid.subjectKeyIdentifier
                signerCert = certList.firstOrNull { c ->
                    val ext = c.getExtensionValue("2.5.29.14")
                    ext != null && ext.contentEquals(skid)
                }
            }

            // Fallback: If only 1 certificate in the set or best candidate
            if (signerCert == null && certList.isNotEmpty()) {
                signerCert = certList.first()
            }

            val encAlgOid = signer.encryptionAlgOID
            val digestAlgOid = signer.digestAlgOID
            val signatureAlgorithm = resolveSignatureAlgorithm(encAlgOid, digestAlgOid)
            val hashAlgorithm = resolveHashAlgorithm(digestAlgOid)

            // Extract signing time from CMS signed attributes if present
            var signingTime: Date? = null
            val timeAttr = signer.signedAttributes?.get(CMSAttributes.signingTime)
            if (timeAttr != null && timeAttr.attrValues.size() > 0) {
                try {
                    val timeAsn1 = org.bouncycastle.asn1.cms.Time.getInstance(timeAttr.attrValues.getObjectAt(0))
                    signingTime = timeAsn1.date
                } catch (_: Exception) {}
            }

            // Step 8 & 9: Calculate digest over exact ByteRange stream & compare with CMS messageDigest
            var digestMismatch = false
            var expectedDigestHex = ""
            var computedDigestHex = ""

            val messageDigestAttr = signer.signedAttributes?.get(CMSAttributes.messageDigest)
            if (messageDigestAttr != null && messageDigestAttr.attrValues.size() > 0 && signedBytes.isNotEmpty()) {
                try {
                    val expectedDigest = org.bouncycastle.asn1.ASN1OctetString.getInstance(
                        messageDigestAttr.attrValues.getObjectAt(0)
                    ).octets
                    expectedDigestHex = expectedDigest.joinToString("") { "%02X".format(it) }

                    val md = try {
                        MessageDigest.getInstance(hashAlgorithm, p)
                    } catch (_: Exception) {
                        MessageDigest.getInstance(hashAlgorithm)
                    }
                    val actualDigest = md.digest(signedBytes)
                    computedDigestHex = actualDigest.joinToString("") { "%02X".format(it) }

                    if (!expectedDigest.contentEquals(actualDigest)) {
                        digestMismatch = true
                    }
                } catch (_: Exception) {
                    digestMismatch = true
                }
            }

            val isMessageDigestMatch = !digestMismatch

            // Step 10: Verify RSA/ECDSA signature over DER encoded SignedAttributes
            var cmsSignatureValid = false
            if (signerCert != null) {
                // Try SignerInformationVerifier first
                try {
                    val verifier = try {
                        JcaSimpleSignerInfoVerifierBuilder().setProvider(p).build(signerCert)
                    } catch (_: Exception) {
                        JcaSimpleSignerInfoVerifierBuilder().build(signerCert)
                    }
                    cmsSignatureValid = signer.verify(verifier)
                } catch (_: Exception) {}

                if (!cmsSignatureValid) {
                    try {
                        val signatureBytes = signer.signature
                        val bytesToVerify = if (signer.signedAttributes != null) {
                            signer.encodedSignedAttributes
                        } else {
                            signedBytes
                        }
                        val sig = try {
                            Signature.getInstance(signatureAlgorithm, p)
                        } catch (_: Exception) {
                            Signature.getInstance(signatureAlgorithm)
                        }
                        sig.initVerify(signerCert.publicKey)
                        sig.update(bytesToVerify)
                        cmsSignatureValid = sig.verify(signatureBytes)
                    } catch (_: Exception) {}
                }
            }

            val isCryptographicallyValid = (isMessageDigestMatch && cmsSignatureValid) ||
                    (cmsSignatureValid && expectedDigestHex.isEmpty())

            var failureReason: String? = null
            if (signerCert == null) {
                failureReason = "Signer X.509 certificate could not be resolved from signature data"
            } else if (digestMismatch) {
                failureReason = "Document message digest mismatch (document was modified after signing)"
            } else if (!cmsSignatureValid) {
                failureReason = "Cryptographic signature validation failed (signature bytes do not match public key)"
            }

            return CMSVerificationResult(
                isCryptographicallyValid = isCryptographicallyValid,
                isCmsSignatureValid = cmsSignatureValid,
                isMessageDigestMatch = isMessageDigestMatch,
                signerCertificate = signerCert,
                certificateFound = signerCert != null || certList.isNotEmpty(),
                certificateChain = certList,
                signatureAlgorithm = signatureAlgorithm,
                hashAlgorithm = hashAlgorithm,
                signingTimeFromCms = signingTime,
                expectedMessageDigestHex = expectedDigestHex,
                computedMessageDigestHex = computedDigestHex,
                failureReason = failureReason
            )
        } catch (e: Throwable) {
            e.printStackTrace()
            return CMSVerificationResult(
                isCryptographicallyValid = false,
                isCmsSignatureValid = false,
                isMessageDigestMatch = false,
                signerCertificate = null,
                certificateFound = false,
                certificateChain = emptyList(),
                signatureAlgorithm = "Unknown",
                hashAlgorithm = "Unknown",
                signingTimeFromCms = null,
                failureReason = "CMS verification exception: ${e.javaClass.simpleName}: ${e.message ?: e.toString()}"
            )
        }
    }

    /**
     * Removes unused zero padding from Contents bytes by decoding the outer DER SEQUENCE length
     */
    fun extractExactDerBytes(raw: ByteArray): ByteArray {
        if (raw.isEmpty()) return raw

        // Locate first 0x30 (SEQUENCE tag)
        var startIdx = 0
        while (startIdx < raw.size && raw[startIdx] != 0x30.toByte()) {
            startIdx++
        }
        if (startIdx >= raw.size) return raw
        val bytes = if (startIdx > 0) raw.copyOfRange(startIdx, raw.size) else raw

        if (bytes.size >= 2) {
            val b1 = bytes[1].toInt() and 0xFF
            var headerLen = 2
            var payloadLen = -1

            if (b1 and 0x80 == 0) {
                // Short form definite length (0 - 127 bytes)
                payloadLen = b1
            } else if (b1 == 0x80) {
                // Indefinite length encoding (BER). Must preserve trailing 0x00 0x00 (EOC marker)
                return bytes
            } else {
                // Long form definite length (DER)
                val numOctets = b1 and 0x7F
                if (numOctets in 1..4 && (2 + numOctets) <= bytes.size) {
                    var len = 0
                    for (i in 0 until numOctets) {
                        len = (len shl 8) or (bytes[2 + i].toInt() and 0xFF)
                    }
                    headerLen = 2 + numOctets
                    payloadLen = len
                }
            }

            if (payloadLen >= 0) {
                val totalLen = headerLen + payloadLen
                if (totalLen in 1..bytes.size) {
                    return bytes.copyOfRange(0, totalLen)
                }
            }
        }

        // Fallback: trim trailing 0x00 bytes, but leave at least 2 zero octets if BER
        var last = bytes.size - 1
        while (last >= 0 && bytes[last] == 0.toByte()) {
            last--
        }
        return if (last >= 0) bytes.copyOfRange(0, last + 1) else bytes
    }

    private fun resolveHashAlgorithm(oid: String?): String {
        return when (oid) {
            "2.16.840.1.101.3.4.2.1" -> "SHA-256"
            "2.16.840.1.101.3.4.2.2" -> "SHA-384"
            "2.16.840.1.101.3.4.2.3" -> "SHA-512"
            "1.3.14.3.2.26" -> "SHA-1"
            else -> "SHA-256"
        }
    }

    private fun resolveSignatureAlgorithm(encOid: String?, digestOid: String?): String {
        val hash = resolveHashAlgorithm(digestOid).replace("-", "")
        return when (encOid) {
            "1.2.840.113549.1.1.1" -> "${hash}withRSA"
            "1.2.840.10045.2.1", "1.2.840.10045.4.3.2" -> "${hash}withECDSA"
            "1.2.840.113549.1.1.11" -> "SHA256withRSA"
            "1.2.840.113549.1.1.5" -> "SHA1withRSA"
            else -> "${hash}withRSA"
        }
    }
}

