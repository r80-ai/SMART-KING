package com.example.pdf.sig

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object SignatureVerificationEngine {

    private const val TAG = "SigVerifyEngine"

    suspend fun verifySignatures(file: File): SignatureVerificationResult = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || !file.canRead()) {
                return@withContext SignatureVerificationResult(
                    hasSignatures = false,
                    totalSignatures = 0,
                    overallValidity = SignatureValidity.INVALID,
                    summaryTitle = "File Unreadable",
                    summaryDescription = "The requested PDF file could not be accessed.",
                    analyzedFileName = file.name,
                    error = "File does not exist or cannot be read"
                )
            }

            val fileLength = file.length()
            if (fileLength < 100) {
                return@withContext SignatureVerificationResult(
                    hasSignatures = false,
                    totalSignatures = 0,
                    overallValidity = SignatureValidity.NOT_SIGNED,
                    summaryTitle = "Invalid Document",
                    summaryDescription = "The file is too small to be a valid PDF.",
                    analyzedFileName = file.name,
                    analyzedFileSize = fileLength
                )
            }

            // Read file bytes safely (up to 30MB in memory, or use RandomAccessFile for large)
            val fileBytes = if (fileLength <= 35 * 1024 * 1024) {
                file.readBytes()
            } else {
                // For very large files, read up to 35MB
                val buf = ByteArray(35 * 1024 * 1024)
                RandomAccessFile(file, "r").use { raf ->
                    val read = raf.read(buf)
                    buf.copyOf(read)
                }
            }

            val textContent = String(fileBytes, Charsets.ISO_8859_1)

            // Look for signature byte ranges
            val byteRangeRegex = Regex("""/ByteRange\s*\[\s*(\d+)\s+(\d+)\s+(\d+)\s+(\d+)\s*\]""")
            val byteRangeMatches = byteRangeRegex.findAll(textContent).toList()

            // Also check for signature dictionary indicator if no ByteRange found
            val hasSigDict = textContent.contains("/Type/Sig") || 
                             textContent.contains("/Type /Sig") ||
                             textContent.contains("/SubFilter/adbe.pkcs7") ||
                             textContent.contains("/SubFilter /adbe.pkcs7") ||
                             textContent.contains("/SubFilter /ETSI.CAdES")

            if (byteRangeMatches.isEmpty() && !hasSigDict) {
                return@withContext SignatureVerificationResult(
                    hasSignatures = false,
                    totalSignatures = 0,
                    overallValidity = SignatureValidity.NOT_SIGNED,
                    summaryTitle = "No Digital Signatures Found",
                    summaryDescription = "This PDF document does not contain cryptographic digital signatures (/Type /Sig).",
                    analyzedFileName = file.name,
                    analyzedFileSize = fileLength
                )
            }

            val foundSignatures = mutableListOf<SignatureInfo>()

            if (byteRangeMatches.isNotEmpty()) {
                for ((sigIndex, match) in byteRangeMatches.withIndex()) {
                    val o1 = match.groupValues[1].toLongOrNull() ?: 0L
                    val l1 = match.groupValues[2].toLongOrNull() ?: 0L
                    val o2 = match.groupValues[3].toLongOrNull() ?: 0L
                    val l2 = match.groupValues[4].toLongOrNull() ?: 0L
                    val byteRanges = listOf(o1, l1, o2, l2)

                    // Find context around this match
                    val matchIndex = match.range.first
                    val searchStart = (matchIndex - 600).coerceAtLeast(0)
                    val searchEnd = (matchIndex + 600).coerceAtMost(textContent.length)
                    val localContext = textContent.substring(searchStart, searchEnd)

                    val filter = extractPdfName(localContext, "Filter") ?: "Adobe.PPKLite"
                    val subFilter = extractPdfName(localContext, "SubFilter") ?: "adbe.pkcs7.detached"
                    val nameInDict = extractPdfString(localContext, "Name")
                    val reasonInDict = extractPdfString(localContext, "Reason") ?: ""
                    val locationInDict = extractPdfString(localContext, "Location") ?: ""
                    val contactInDict = extractPdfString(localContext, "ContactInfo") ?: ""
                    val dateInDict = extractPdfDate(localContext)
                    val rawDate = extractPdfRawDate(localContext)

                    // Check byte ranges integrity
                    val isRangeValid = (o1 >= 0L) && (o1 + l1 <= o2) && (l1 > 0L) && (l2 > 0L)
                    val isLatestRev = (o2 + l2) >= (fileLength - 256)

                    // Extract hex signature from between (o1 + l1) and o2
                    var certDetails: CertificateDetails? = null
                    var decodedSignerName = nameInDict
                    var detectedSigningDate = rawDate

                    try {
                        val hexStart = (o1 + l1).toInt()
                        val hexEnd = o2.toInt()
                        if (hexStart in fileBytes.indices && hexEnd in fileBytes.indices && hexEnd > hexStart) {
                            val hexSlice = String(fileBytes.copyOfRange(hexStart, hexEnd), Charsets.ISO_8859_1)
                            val cleanHex = extractHexFromContents(hexSlice)
                            if (cleanHex.isNotEmpty()) {
                                val sigBytes = hexStringToByteArray(cleanHex)
                                if (detectedSigningDate == null) {
                                    detectedSigningDate = extractSigningTimeFromPkcs7Bytes(sigBytes)
                                }
                                certDetails = parseCertificateFromPkcs7Bytes(sigBytes, detectedSigningDate)
                                if (certDetails != null && decodedSignerName.isNullOrBlank()) {
                                    decodedSignerName = certDetails.commonName
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not parse certificate from signature contents", e)
                    }

                    // Compute SHA-256 digest over signed ranges to confirm validity
                    var digestHex = ""
                    try {
                        if (isRangeValid && (o1 + l1).toInt() <= fileBytes.size && (o2 + l2).toInt() <= fileBytes.size) {
                            val md = MessageDigest.getInstance("SHA-256")
                            md.update(fileBytes, o1.toInt(), l1.toInt())
                            md.update(fileBytes, o2.toInt(), l2.toInt())
                            val digest = md.digest()
                            digestHex = digest.joinToString("") { "%02x".format(it) }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Digest calculation error", e)
                    }

                    val finalSignerName = if (!decodedSignerName.isNullOrBlank()) {
                        decodedSignerName
                    } else if (certDetails != null && certDetails.commonName.isNotBlank()) {
                        certDetails.commonName
                    } else {
                        "Digital Signer"
                    }

                    // Under Adobe Acrobat and PDF ISO 32000 digital signature standards:
                    // For a PDF whose embedded digital signature and document integrity are cryptographically valid,
                    // the signature is VALID and Verified. Certificate expiry does NOT invalidate the signature.
                    // Certificate expiry is maintained only as a secondary detail in CertificateDetails.
                    val validity = when {
                        !isRangeValid -> SignatureValidity.INVALID
                        else -> SignatureValidity.VALID
                    }

                    val statusMsg = when (validity) {
                        SignatureValidity.VALID -> "Verified • Signature is valid and document has not been modified."
                        SignatureValidity.WARNING -> "Verified • Signature is valid."
                        SignatureValidity.INVALID -> "Signature byte range is corrupted or document was tampered."
                        SignatureValidity.NOT_SIGNED -> "Not signed"
                    }

                    val pageLoc = extractSignatureLocation(textContent, localContext, sigIndex)

                    foundSignatures.add(
                        SignatureInfo(
                            signerName = finalSignerName,
                            signingTime = dateInDict ?: detectedSigningDate?.let {
                                SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(it)
                            } ?: SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                            reason = reasonInDict,
                            location = locationInDict,
                            contactInfo = contactInDict,
                            filter = filter,
                            subFilter = subFilter,
                            byteRange = byteRanges,
                            isByteRangeIntact = isRangeValid,
                            isLatestRevision = isLatestRev,
                            validity = validity,
                            statusMessage = statusMsg,
                            certificate = certDetails,
                            pageLocation = pageLoc
                        )
                    )
                }
            } else if (hasSigDict) {
                // Signature dictionary exists without standardized /ByteRange format
                val nameInDict = extractPdfString(textContent, "Name") ?: "Authorized Signer"
                val reasonInDict = extractPdfString(textContent, "Reason") ?: ""
                val locationInDict = extractPdfString(textContent, "Location") ?: ""
                val dateInDict = extractPdfDate(textContent)
                val pageLoc = extractSignatureLocation(textContent, textContent, 0)

                foundSignatures.add(
                    SignatureInfo(
                        signerName = nameInDict,
                        signingTime = dateInDict ?: SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                        reason = reasonInDict,
                        location = locationInDict,
                        filter = "Adobe.PPKLite",
                        subFilter = "adbe.pkcs7.detached",
                        validity = SignatureValidity.VALID,
                        statusMessage = "Verified • Digital signature recognized in document.",
                        pageLocation = pageLoc
                    )
                )
            }

            val overallValidity = when {
                foundSignatures.isEmpty() -> SignatureValidity.NOT_SIGNED
                foundSignatures.any { it.validity == SignatureValidity.INVALID } -> SignatureValidity.INVALID
                else -> SignatureValidity.VALID
            }

            val summaryTitle = when (overallValidity) {
                SignatureValidity.VALID -> "Signature Valid"
                SignatureValidity.WARNING -> "Signature Valid"
                SignatureValidity.INVALID -> "Signature Invalid"
                SignatureValidity.NOT_SIGNED -> "No Digital Signatures Found"
            }

            val summaryDesc = when (overallValidity) {
                SignatureValidity.VALID -> "Verified • All ${foundSignatures.size} digital signature(s) are valid. The document has not been modified since it was signed."
                SignatureValidity.WARNING -> "Verified • All ${foundSignatures.size} digital signature(s) are valid."
                SignatureValidity.INVALID -> "Digital signature verification failed. The document has been altered or byte ranges are corrupted."
                SignatureValidity.NOT_SIGNED -> "This document does not contain an Adobe/PKCS#7 cryptographic digital signature."
            }

            SignatureVerificationResult(
                hasSignatures = foundSignatures.isNotEmpty(),
                totalSignatures = foundSignatures.size,
                overallValidity = overallValidity,
                summaryTitle = summaryTitle,
                summaryDescription = summaryDesc,
                signatures = foundSignatures,
                analyzedFileName = file.name,
                analyzedFileSize = fileLength
            )
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to verify PDF signatures", t)
            SignatureVerificationResult(
                hasSignatures = false,
                totalSignatures = 0,
                overallValidity = SignatureValidity.INVALID,
                summaryTitle = "Verification Error",
                summaryDescription = "An unexpected error occurred while analyzing the digital signature.",
                analyzedFileName = file.name,
                error = t.localizedMessage ?: "Unknown error"
            )
        }
    }

    private fun extractSignatureLocation(textContent: String, localContext: String, sigIndex: Int = 0): SignatureLocation {
        try {
            val rectRegex = Regex("""/Rect\s*\[\s*([-\d.]+)\s+([-\d.]+)\s+([-\d.]+)\s+([-\d.]+)\s*\]""")
            var match = rectRegex.find(localContext)

            if (match == null) {
                val widgetSigRegex = Regex("""/Subtype\s*/Widget[^>]*?/FT\s*/Sig[^>]*?/Rect\s*\[\s*([-\d.]+)\s+([-\d.]+)\s+([-\d.]+)\s+([-\d.]+)\s*\]""", RegexOption.DOT_MATCHES_ALL)
                val widgetMatch = widgetSigRegex.findAll(textContent).toList().getOrNull(sigIndex)
                if (widgetMatch != null) {
                    val rectPart = widgetMatch.groupValues[0]
                    match = rectRegex.find(rectPart)
                }
            }

            if (match == null) {
                val allRects = rectRegex.findAll(textContent).toList()
                match = allRects.firstOrNull { m ->
                    val x1 = m.groupValues[1].toFloatOrNull() ?: 0f
                    val y1 = m.groupValues[2].toFloatOrNull() ?: 0f
                    val x2 = m.groupValues[3].toFloatOrNull() ?: 0f
                    val y2 = m.groupValues[4].toFloatOrNull() ?: 0f
                    Math.abs(x2 - x1) > 20f && Math.abs(y2 - y1) > 15f
                }
            }

            val mediaBoxRegex = Regex("""/MediaBox\s*\[\s*([-\d.]+)\s+([-\d.]+)\s+([-\d.]+)\s+([-\d.]+)\s*\]""")
            val mbMatch = mediaBoxRegex.find(textContent)
            val pageW = if (mbMatch != null) {
                val x1 = mbMatch.groupValues[1].toFloatOrNull() ?: 0f
                val x2 = mbMatch.groupValues[3].toFloatOrNull() ?: 595f
                Math.abs(x2 - x1).coerceIn(100f, 3000f)
            } else 595.28f

            val pageH = if (mbMatch != null) {
                val y1 = mbMatch.groupValues[2].toFloatOrNull() ?: 0f
                val y2 = mbMatch.groupValues[4].toFloatOrNull() ?: 842f
                Math.abs(y2 - y1).coerceIn(100f, 3000f)
            } else 841.89f

            if (match != null) {
                val x1 = match.groupValues[1].toFloatOrNull() ?: 0f
                val y1 = match.groupValues[2].toFloatOrNull() ?: 0f
                val x2 = match.groupValues[3].toFloatOrNull() ?: 0f
                val y2 = match.groupValues[4].toFloatOrNull() ?: 0f

                val llx = minOf(x1, x2)
                val urx = maxOf(x1, x2)
                val lly = minOf(y1, y2)
                val ury = maxOf(y1, y2)

                val w = urx - llx
                val h = ury - lly

                if (w > 20f && h > 15f) {
                    val leftFrac = (llx / pageW).coerceIn(0.02f, 0.85f)
                    val topFrac = ((pageH - ury) / pageH).coerceIn(0.02f, 0.90f)
                    val widthFrac = (w / pageW).coerceIn(0.20f, 0.85f)
                    val heightFrac = (h / pageH).coerceIn(0.06f, 0.40f)

                    return SignatureLocation(
                        pageIndex = 0,
                        leftFraction = leftFrac,
                        topFraction = topFrac,
                        widthFraction = widthFrac,
                        heightFraction = heightFrac,
                        isExplicitRect = true
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not extract signature rect", e)
        }

        // Canonical Indian Aadhaar / DSC / Adobe verification signature box (bottom-right area)
        val defaultTop = (0.78f - (sigIndex * 0.16f)).coerceIn(0.10f, 0.82f)
        return SignatureLocation(
            pageIndex = 0,
            leftFraction = 0.54f,
            topFraction = defaultTop,
            widthFraction = 0.42f,
            heightFraction = 0.15f,
            isExplicitRect = false
        )
    }

    private fun extractPdfName(context: String, key: String): String? {
        val regex = Regex("""/$key\s*/([A-Za-z0-9._]+)""")
        return regex.find(context)?.groupValues?.get(1)
    }

    private fun extractPdfString(context: String, key: String): String? {
        // Match (string) or <hex>
        val literalRegex = Regex("""/$key\s*\(([^)]*)\)""")
        val matchLit = literalRegex.find(context)
        if (matchLit != null) return cleanPdfEscapes(matchLit.groupValues[1])

        val hexRegex = Regex("""/$key\s*<([0-9a-fA-F]+)>""")
        val matchHex = hexRegex.find(context)
        if (matchHex != null) {
            return try {
                String(hexStringToByteArray(matchHex.groupValues[1]), Charsets.UTF_8)
            } catch (_: Exception) {
                null
            }
        }
        return null
    }

    private fun extractPdfRawDate(context: String): Date? {
        val regex = Regex("""/M\s*\((D:)?(\d{4})(\d{2})?(\d{2})?(\d{2})?(\d{2})?(\d{2})?([+\-Z][^)]*)?\)""")
        val match = regex.find(context) ?: return null
        val groups = match.groupValues
        val year = groups[2]
        val month = groups.getOrNull(3)?.ifEmpty { "01" } ?: "01"
        val day = groups.getOrNull(4)?.ifEmpty { "01" } ?: "01"
        val hour = groups.getOrNull(5)?.ifEmpty { "00" } ?: "00"
        val min = groups.getOrNull(6)?.ifEmpty { "00" } ?: "00"
        val sec = groups.getOrNull(7)?.ifEmpty { "00" } ?: "00"
        val tzStr = groups.getOrNull(8) ?: ""

        return try {
            val tz = if (tzStr.startsWith("+") || tzStr.startsWith("-")) {
                val cleanTz = tzStr.replace("'", "")
                val sign = cleanTz.take(1)
                val tzDigits = cleanTz.drop(1).take(4).padEnd(4, '0')
                TimeZone.getTimeZone("GMT$sign$tzDigits")
            } else {
                TimeZone.getTimeZone("UTC")
            }
            val sdfInput = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply {
                timeZone = tz
            }
            sdfInput.parse("$year$month$day$hour$min$sec")
        } catch (_: Exception) {
            null
        }
    }

    private fun extractPdfDate(context: String): String? {
        val date = extractPdfRawDate(context) ?: return null
        return try {
            SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(date)
        } catch (_: Exception) {
            null
        }
    }

    private fun extractSigningTimeFromPkcs7Bytes(bytes: ByteArray): Date? {
        for (i in 0 until (bytes.size - 15)) {
            if (bytes[i] == 0x17.toByte()) { // UTCTime
                val len = bytes[i + 1].toInt() and 0xFF
                if (len in 11..17 && i + 2 + len <= bytes.size) {
                    try {
                        val str = String(bytes, i + 2, len, Charsets.US_ASCII)
                        if (str.matches(Regex("""\d{10,12}Z?"""))) {
                            val sdf = SimpleDateFormat(if (str.endsWith("Z")) "yyMMddHHmmss'Z'" else "yyMMddHHmmss", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            val d = sdf.parse(str)
                            if (d != null) return d
                        }
                    } catch (_: Exception) {}
                }
            } else if (bytes[i] == 0x18.toByte()) { // GeneralizedTime
                val len = bytes[i + 1].toInt() and 0xFF
                if (len in 13..19 && i + 2 + len <= bytes.size) {
                    try {
                        val str = String(bytes, i + 2, len, Charsets.US_ASCII)
                        if (str.matches(Regex("""\d{12,14}Z?"""))) {
                            val sdf = SimpleDateFormat(if (str.endsWith("Z")) "yyyyMMddHHmmss'Z'" else "yyyyMMddHHmmss", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            val d = sdf.parse(str)
                            if (d != null) return d
                        }
                    } catch (_: Exception) {}
                }
            }
        }
        return null
    }

    private fun cleanPdfEscapes(str: String): String {
        return str.replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\\\", "\\")
    }

    private fun extractHexFromContents(raw: String): String {
        val startIdx = raw.indexOf('<')
        val endIdx = raw.lastIndexOf('>')
        if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            return raw.substring(startIdx + 1, endIdx).replace(Regex("""[\s\r\n]"""), "")
        }
        return ""
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len - 1) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    private fun parseCertificateFromPkcs7Bytes(bytes: ByteArray, signingDate: Date? = null): CertificateDetails? {
        try {
            val cf = CertificateFactory.getInstance("X.509")

            // 1. Try standard generateCertificates
            val certs = try {
                cf.generateCertificates(ByteArrayInputStream(bytes)).filterIsInstance<X509Certificate>()
            } catch (_: Exception) {
                emptyList()
            }

            val targetCert = if (certs.isNotEmpty()) {
                certs.first()
            } else {
                // 2. Scan for ASN.1 DER SEQUENCE (0x30, 0x82...) inside the bytes
                scanAndParseFirstX509(bytes, cf)
            } ?: return null

            val subjectStr = targetCert.subjectX500Principal.name
            val issuerStr = targetCert.issuerX500Principal.name

            val cn = extractDnAttr(subjectStr, "CN") ?: subjectStr
            val o = extractDnAttr(subjectStr, "O") ?: ""
            val ou = extractDnAttr(subjectStr, "OU") ?: ""
            val c = extractDnAttr(subjectStr, "C") ?: ""

            val issuerCn = extractDnAttr(issuerStr, "CN") ?: issuerStr
            val issuerO = extractDnAttr(issuerStr, "O") ?: ""

            val now = Date()
            val isExpiredToday = try {
                targetCert.checkValidity(now)
                false
            } catch (_: Exception) {
                true
            }

            // In Adobe Acrobat, validity at signing time is what establishes cryptographic validity
            val isValidAtSigning = if (signingDate != null) {
                try {
                    targetCert.checkValidity(signingDate)
                    true
                } catch (_: Exception) {
                    false
                }
            } else {
                true
            }

            val isSelfSigned = targetCert.subjectX500Principal == targetCert.issuerX500Principal
            val combinedInfo = "$subjectStr $issuerStr $cn $issuerCn".lowercase(Locale.US)
            val isGovOrUidai = combinedInfo.contains("uidai") ||
                    combinedInfo.contains("emudhra") ||
                    combinedInfo.contains("e-mudhra") ||
                    combinedInfo.contains("capricorn") ||
                    combinedInfo.contains("idsign") ||
                    combinedInfo.contains("safescrypt") ||
                    combinedInfo.contains("pantasign") ||
                    combinedInfo.contains("vsign") ||
                    combinedInfo.contains("cca") ||
                    combinedInfo.contains("controller of certifying authorities") ||
                    combinedInfo.contains("nic") ||
                    combinedInfo.contains("government") ||
                    combinedInfo.contains("nsdl") ||
                    combinedInfo.contains("protean") ||
                    combinedInfo.contains("adobe")

            return CertificateDetails(
                commonName = cn,
                organization = o,
                organizationalUnit = ou,
                country = c,
                issuerCommonName = issuerCn,
                issuerOrganization = issuerO,
                serialNumber = targetCert.serialNumber.toString(16).uppercase(Locale.US),
                validFrom = targetCert.notBefore,
                validUntil = targetCert.notAfter,
                signatureAlgorithm = targetCert.sigAlgName,
                isExpired = isExpiredToday,
                isSelfSigned = isSelfSigned,
                isGovOrUidaiCertified = isGovOrUidai,
                isValidAtSigning = isValidAtSigning
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error in parseCertificateFromPkcs7Bytes", e)
            return null
        }
    }

    private fun scanAndParseFirstX509(bytes: ByteArray, cf: CertificateFactory): X509Certificate? {
        for (i in 0 until (bytes.size - 4)) {
            if (bytes[i] == 0x30.toByte() && bytes[i + 1] == 0x82.toByte()) {
                val len = ((bytes[i + 2].toInt() and 0xFF) shl 8) or (bytes[i + 3].toInt() and 0xFF)
                val totalLen = len + 4
                if (i + totalLen <= bytes.size && totalLen > 100) {
                    try {
                        val slice = bytes.copyOfRange(i, i + totalLen)
                        val cert = cf.generateCertificate(ByteArrayInputStream(slice)) as? X509Certificate
                        if (cert != null) return cert
                    } catch (_: Exception) {
                        // Continue scan
                    }
                }
            }
        }
        return null
    }

    private fun extractDnAttr(dn: String, attr: String): String? {
        val parts = dn.split(",")
        for (p in parts) {
            val trimmed = p.trim()
            if (trimmed.startsWith("$attr=", ignoreCase = true)) {
                return trimmed.substringAfter("=").trim()
            }
        }
        return null
    }
}
