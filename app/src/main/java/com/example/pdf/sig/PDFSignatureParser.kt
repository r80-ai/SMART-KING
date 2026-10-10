package com.example.pdf.sig

import java.io.File
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern

data class ParsedSignatureEntry(
    val index: Int,
    val filter: String,
    val subFilter: String,
    val byteRange: LongArray,
    val contentsHex: String,
    val contentsBytes: ByteArray,
    val name: String,
    val signingDate: Date?,
    val reason: String,
    val location: String,
    val contactInfo: String,
    val dictionaryStartOffset: Long,
    val dictionaryEndOffset: Long
)

object PDFSignatureParser {

    /**
     * Parses all embedded digital signatures (/Type /Sig, /ByteRange, /Contents) in a PDF file
     */
    fun parseSignatures(pdfFile: File): List<ParsedSignatureEntry> {
        val signatures = mutableListOf<ParsedSignatureEntry>()
        if (!pdfFile.exists() || pdfFile.length() < 100) return signatures

        val fileBytes = pdfFile.readBytes()
        val fileLength = fileBytes.size.toLong()

        // Pattern for ByteRange: /ByteRange\s*\[\s*(\d+)\s+(\d+)\s+(\d+)\s+(\d+)\s*\]
        val byteRangePattern = Pattern.compile("/ByteRange\\s*\\[\\s*(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s*\\]")
        val matcher = byteRangePattern.matcher(String(fileBytes, Charsets.ISO_8859_1))

        var sigIndex = 1
        while (matcher.find()) {
            val range0 = matcher.group(1)?.toLongOrNull() ?: 0L
            val range1 = matcher.group(2)?.toLongOrNull() ?: 0L
            val range2 = matcher.group(3)?.toLongOrNull() ?: 0L
            val range3 = matcher.group(4)?.toLongOrNull() ?: 0L

            val byteRange = longArrayOf(range0, range1, range2, range3)
            val matchStart = matcher.start()

            // 1. First attempt: Extract /Contents from the exact gap between range1 and range2
            // In PDF specification ISO 32000-1, the gap between range1 and range2 is the /Contents <hex>
            var contentsHex = ""
            var contentsBytes = byteArrayOf()

            val gapStart = range1.toInt()
            val gapEnd = range2.toInt()
            if (gapStart in 0 until gapEnd && gapEnd <= fileBytes.size) {
                val gapBytes = fileBytes.sliceArray(gapStart until gapEnd)
                val startIdx = gapBytes.indexOf('<'.code.toByte())
                val endIdx = gapBytes.lastIndexOf('>'.code.toByte())
                val sliceStart = if (startIdx != -1) startIdx + 1 else 0
                val sliceEnd = if (endIdx != -1 && endIdx > sliceStart) endIdx else gapBytes.size
                val sb = StringBuilder()
                for (i in sliceStart until sliceEnd) {
                    val c = gapBytes[i].toInt().toChar()
                    if ((c in '0'..'9') || (c in 'a'..'f') || (c in 'A'..'F')) {
                        sb.append(c)
                    }
                }
                if (sb.isNotEmpty()) {
                    contentsHex = sb.toString()
                    contentsBytes = hexStringToByteArray(contentsHex)
                }
            }

            // 2. Second attempt: If gap extraction did not yield bytes, look in dictionary window
            if (contentsBytes.isEmpty()) {
                val searchWindowStart = (matchStart - 4096).coerceAtLeast(0)
                val searchWindowEnd = (matchStart + 65536).coerceAtMost(fileBytes.size)
                val windowStr = String(fileBytes.sliceArray(searchWindowStart until searchWindowEnd), Charsets.ISO_8859_1)
                val contentsPattern = Pattern.compile("/Contents\\s*<([0-9a-fA-F\\s]+)>")
                val contentsMatcher = contentsPattern.matcher(windowStr)
                if (contentsMatcher.find()) {
                    contentsHex = contentsMatcher.group(1)?.replace("\\s".toRegex(), "") ?: ""
                    contentsBytes = hexStringToByteArray(contentsHex)
                }
            }

            // Look backward and forward up to 8192 bytes around ByteRange for dictionary metadata
            val dictWindowStart = (matchStart - 4096).coerceAtLeast(0)
            val dictWindowEnd = (matchStart + 8192).coerceAtMost(fileBytes.size)
            val dictWindowStr = String(fileBytes.sliceArray(dictWindowStart until dictWindowEnd), Charsets.ISO_8859_1)

            // Extract /Filter
            val filter = extractDictionaryValue(dictWindowStr, "/Filter")
                .ifBlank { "Adobe.PPKLite" }

            // Extract /SubFilter
            val subFilter = extractDictionaryValue(dictWindowStr, "/SubFilter")
                .ifBlank { "adbe.pkcs7.detached" }

            // Extract /Name
            val name = extractDictionaryValue(dictWindowStr, "/Name")

            // Extract /M (Date)
            val dateStr = extractDictionaryValue(dictWindowStr, "/M")
            val signingDate = parsePdfDate(dateStr)

            // Extract /Reason
            val reason = extractDictionaryValue(dictWindowStr, "/Reason")

            // Extract /Location
            val location = extractDictionaryValue(dictWindowStr, "/Location")

            // Extract /ContactInfo
            val contactInfo = extractDictionaryValue(dictWindowStr, "/ContactInfo")

            if (contentsBytes.isNotEmpty() && byteRange[1] > 0 && byteRange[3] > 0) {
                signatures.add(
                    ParsedSignatureEntry(
                        index = sigIndex++,
                        filter = filter,
                        subFilter = subFilter,
                        byteRange = byteRange,
                        contentsHex = contentsHex,
                        contentsBytes = contentsBytes,
                        name = name,
                        signingDate = signingDate,
                        reason = reason,
                        location = location,
                        contactInfo = contactInfo,
                        dictionaryStartOffset = dictWindowStart.toLong(),
                        dictionaryEndOffset = dictWindowEnd.toLong()
                    )
                )
            }
        }

        return signatures
    }

    /**
     * Extracts byte stream signed by the signature ByteRange: [range0, range1, range2, range3]
     */
    fun extractSignedBytes(pdfFile: File, byteRange: LongArray): ByteArray {
        val r0 = byteRange[0].toInt()
        val r1 = byteRange[1].toInt()
        val r2 = byteRange[2].toInt()
        val r3 = byteRange[3].toInt()

        val fileLength = pdfFile.length().toInt()
        if (r0 < 0 || r1 <= 0 || r2 < (r0 + r1) || (r2 + r3) > fileLength) {
            return byteArrayOf()
        }

        val result = ByteArray(r1 + r3)
        RandomAccessFile(pdfFile, "r").use { raf ->
            raf.seek(r0.toLong())
            raf.readFully(result, 0, r1)
            raf.seek(r2.toLong())
            raf.readFully(result, r1, r3)
        }
        return result
    }

    private fun extractDictionaryValue(str: String, key: String): String {
        val keyIdx = str.indexOf(key)
        if (keyIdx != -1) {
            val afterKey = str.substring(keyIdx + key.length)
            var pIdx = 0
            while (pIdx < afterKey.length && (afterKey[pIdx] == ' ' || afterKey[pIdx] == '\t' || afterKey[pIdx] == '\r' || afterKey[pIdx] == '\n')) {
                pIdx++
            }

            // 1. Literal string: /Key (Value)
            if (pIdx < afterKey.length && afterKey[pIdx] == '(') {
                pIdx++
                var depth = 1
                val sb = StringBuilder()
                var escaped = false
                while (pIdx < afterKey.length && depth > 0) {
                    val c = afterKey[pIdx]
                    if (escaped) {
                        when (c) {
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            '(' -> sb.append('(')
                            ')' -> sb.append(')')
                            '\\' -> sb.append('\\')
                            else -> sb.append(c)
                        }
                        escaped = false
                    } else if (c == '\\') {
                        escaped = true
                    } else if (c == '(') {
                        depth++
                        sb.append(c)
                    } else if (c == ')') {
                        depth--
                        if (depth > 0) sb.append(c)
                    } else {
                        sb.append(c)
                    }
                    pIdx++
                }
                val result = sb.toString().trim()
                if (result.isNotEmpty()) return result
            }

            // 2. Name object: /Key /NameValue with #XX unescaping
            if (pIdx < afterKey.length && afterKey[pIdx] == '/') {
                pIdx++
                val sb = StringBuilder()
                while (pIdx < afterKey.length && !afterKey[pIdx].isWhitespace() && afterKey[pIdx] != '/' && afterKey[pIdx] != '>' && afterKey[pIdx] != ']') {
                    sb.append(afterKey[pIdx])
                    pIdx++
                }
                val rawName = sb.toString()
                val hexEscapePattern = Pattern.compile("#([0-9a-fA-F]{2})")
                val m = hexEscapePattern.matcher(rawName)
                val decSb = StringBuffer()
                while (m.find()) {
                    val code = m.group(1).toInt(16)
                    m.appendReplacement(decSb, Matcher.quoteReplacement(code.toChar().toString()))
                }
                m.appendTail(decSb)
                val result = decSb.toString().trim()
                if (result.isNotEmpty()) return result
            }

            // 3. Hex string: /Key <HEX>
            if (pIdx < afterKey.length && afterKey[pIdx] == '<') {
                pIdx++
                val endIdx = afterKey.indexOf('>', pIdx)
                if (endIdx != -1) {
                    val hex = afterKey.substring(pIdx, endIdx).replace("\\s".toRegex(), "")
                    val bytes = hexStringToByteArray(hex)
                    if (bytes.isNotEmpty()) {
                        val text = if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
                            String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
                        } else {
                            String(bytes, Charsets.UTF_8)
                        }
                        if (text.isNotBlank()) return text.trim()
                    }
                }
            }
        }

        return ""
    }

    fun parsePdfDate(rawDate: String): Date? {
        if (rawDate.isBlank()) return null
        // Format: D:YYYYMMDDHHmmSSOHH'mm'
        val clean = rawDate.replace("D:", "").replace("'", "")
        val formats = listOf(
            "yyyyMMddHHmmssZ",
            "yyyyMMddHHmmss",
            "yyyyMMddHHmm",
            "yyyyMMdd"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                val len = fmt.replace("Z", "+0000").length
                if (clean.length >= 8) {
                    return sdf.parse(clean.take(len))
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val clean = if (s.length % 2 != 0) s.dropLast(1) else s
        val len = clean.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len - 1) {
            val hi = Character.digit(clean[i], 16)
            val lo = Character.digit(clean[i + 1], 16)
            if (hi == -1 || lo == -1) break
            data[i / 2] = ((hi shl 4) + lo).toByte()
            i += 2
        }
        return data
    }
}
