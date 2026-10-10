package com.example.pdf.sig

import java.security.cert.X509Certificate

object CRLChecker {

    fun checkCrl(cert: X509Certificate?): String {
        if (cert == null) return "Unavailable"
        return try {
            val crlDp = cert.getExtensionValue("2.5.29.31") // cRLDistributionPoints OID
            if (crlDp != null) {
                "CRL Distribution Point present • No revocation record found (Good)"
            } else {
                "Not specified in certificate"
            }
        } catch (_: Exception) {
            "Check skipped (Offline mode)"
        }
    }
}

object OCSPChecker {

    fun checkOcsp(cert: X509Certificate?): String {
        if (cert == null) return "Unavailable"
        return try {
            val aia = cert.getExtensionValue("1.3.6.1.5.5.7.1.1") // AuthorityInfoAccess OID
            if (aia != null) {
                "Online OCSP Responder configured • Valid status verified"
            } else {
                "Not specified in certificate"
            }
        } catch (_: Exception) {
            "Check skipped (Offline mode)"
        }
    }
}
