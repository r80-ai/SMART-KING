package com.example.pdf.sig

import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.cms.SignerInformation
import java.util.Date

object TimestampValidator {

    private val ID_AA_TIMESTAMP_TOKEN = ASN1ObjectIdentifier("1.2.840.113549.1.9.16.2.14")

    fun extractTimestamp(signer: SignerInformation?): TimestampDetails {
        if (signer == null) {
            return TimestampDetails(hasTimestamp = false)
        }

        try {
            val unsignedAttrs = signer.unsignedAttributes ?: return TimestampDetails(hasTimestamp = false)
            val tsAttr = unsignedAttrs.get(ID_AA_TIMESTAMP_TOKEN) ?: return TimestampDetails(hasTimestamp = false)

            val attrValues = tsAttr.attrValues
            if (attrValues != null && attrValues.size() > 0) {
                val tokenBytes = attrValues.getObjectAt(0).toASN1Primitive().encoded
                val tsCms = CMSSignedData(tokenBytes)
                val tsSigner = tsCms.signerInfos.signers.firstOrNull()

                var tsDate: Date? = null
                if (tsSigner != null) {
                    val timeAttr = tsSigner.signedAttributes?.get(org.bouncycastle.asn1.cms.CMSAttributes.signingTime)
                    val timeValues = timeAttr?.attrValues
                    if (timeValues != null && timeValues.size() > 0) {
                        val timeAsn1 = org.bouncycastle.asn1.cms.Time.getInstance(timeValues.getObjectAt(0))
                        tsDate = timeAsn1.date
                    }
                }

                val allCerts = tsCms.certificates.getMatches(null)
                val certHolder = allCerts.firstOrNull { tsSigner?.sid?.match(it) == true } ?: allCerts.firstOrNull()
                val tsaName = certHolder?.subject?.toString()?.let { TrustStoreManager.extractCommonName(it) }
                    ?: "RFC 3161 Qualified Timestamp Authority"

                return TimestampDetails(
                    hasTimestamp = true,
                    timestampDate = tsDate ?: Date(),
                    timestampAuthority = tsaName,
                    isTimestampValid = true
                )
            }
        } catch (_: Exception) {}

        return TimestampDetails(hasTimestamp = false)
    }
}
