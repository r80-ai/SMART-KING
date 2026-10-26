package com.example.ui.screens

/**
 * UIDAI Official Affidavits & Declarations Formats & Generators
 * Category 1 in SMART KING Application Maker
 */

fun getAadhaarAffidavitCategory(): LetterCategory {
    return LetterCategory(
        id = "aadhaar_affidavit",
        titleBn = "১. আধার এফিডেভিট ও স্ব-ঘোষণা (Aadhaar Affidavit & Declarations)",
        titleEn = "1. Aadhaar Affidavit & Declarations (UIDAI)",
        defaultRecipientBn = "ইউআইডিএআই আঞ্চলিক কার্যালয় / নোটারি পাবলিক",
        defaultRecipientEn = "The Regional Office, UIDAI / The Notary Public",
        letterTypes = listOf(
            LetterTypeItem(
                id = "aadhaar_annexure_1",
                titleBn = "Annexure-I: DoB Update Affidavit (প্রাপ্তবয়স্কদের জন্মতারিখ এফিডেভিট)",
                titleEn = "Annexure-I: Affidavit for DoB Updation (For Adults)",
                subjectBn = "প্রাপ্তবয়স্কদের আধার কার্ডে জন্মতারিখ সংশোধনের এফিডেভিট (Annexure-I)",
                subjectEn = "Affidavit for Date of Birth Updation in Aadhaar (For Adults - Annexure-I)",
                field1LabelBn = "সঠিক জন্ম তারিখ (DD/MM/YYYY)",
                field1LabelEn = "Correct Date of Birth (DD/MM/YYYY)",
                field2LabelBn = "আগের ভুল জন্ম তারিখ (Aadhaar Old DoB)",
                field2LabelEn = "Earlier Recorded DoB in Aadhaar",
                bodyTemplateBn = { name, addr, _, f1, f2 ->
                    "Annexure-I Adult DoB Updation Affidavit for $name, residing at $addr. Correct DoB: $f1, Old DoB: $f2."
                },
                bodyTemplateEn = { name, addr, _, f1, f2 ->
                    "Annexure-I Adult DoB Updation Affidavit for $name, residing at $addr. Correct DoB: $f1, Old DoB: $f2."
                }
            ),
            LetterTypeItem(
                id = "aadhaar_annexure_1a",
                titleBn = "Annexure-IA: DoB Update Affidavit (শিশুদের জন্মতারিখ এফিডেভিট)",
                titleEn = "Annexure-IA: Affidavit for DoB Updation (For Children / Minor)",
                subjectBn = "শিশুদের আধার কার্ডে জন্মতারিখ সংশোধনের এফিডেভিট (Annexure-IA)",
                subjectEn = "Affidavit for Date of Birth Updation in Aadhaar (For Children - Annexure-IA)",
                field1LabelBn = "সন্তানের সঠিক জন্ম তারিখ (DD/MM/YYYY)",
                field1LabelEn = "Child's Correct Date of Birth (DD/MM/YYYY)",
                field2LabelBn = "আগের ভুল জন্ম তারিখ (Old DoB)",
                field2LabelEn = "Earlier Recorded DoB in Child's Aadhaar",
                bodyTemplateBn = { name, addr, _, f1, f2 ->
                    "Annexure-IA Minor DoB Updation Affidavit by $name. Correct DoB: $f1, Old DoB: $f2."
                },
                bodyTemplateEn = { name, addr, _, f1, f2 ->
                    "Annexure-IA Minor DoB Updation Affidavit by $name. Correct DoB: $f1, Old DoB: $f2."
                }
            ),
            LetterTypeItem(
                id = "aadhaar_annexure_2",
                titleBn = "Annexure-II: DoB স্ব-ঘোষণা (অপারেটর ভুলের জন্য - প্রাপ্তবয়স্ক)",
                titleEn = "Annexure-II: DoB Update for Operator Error (For Adults)",
                subjectBn = "অপারেটর ভুলের জন্য জন্মতারিখ সংশোধনের স্ব-ঘোষণা (Annexure-II)",
                subjectEn = "Self-Declaration for DoB Update due to Operator Error (For Adults - Annexure-II)",
                field1LabelBn = "সঠিক জন্ম তারিখ (Correct DoB)",
                field1LabelEn = "Correct Date of Birth (DD/MM/YYYY)",
                field2LabelBn = "আধার নথিভুক্তির EID নম্বর (Enrolment EID)",
                field2LabelEn = "Enrolment / Update Request (EID) No",
                bodyTemplateBn = { name, addr, _, f1, f2 ->
                    "Annexure-II Self-Declaration by $name for Operator Typographical Error. Correct DoB: $f1, EID: $f2."
                },
                bodyTemplateEn = { name, addr, _, f1, f2 ->
                    "Annexure-II Self-Declaration by $name for Operator Typographical Error. Correct DoB: $f1, EID: $f2."
                }
            ),
            LetterTypeItem(
                id = "aadhaar_annexure_2a",
                titleBn = "Annexure-IIA: DoB স্ব-ঘোষণা (অপারেটর ভুলের জন্য - শিশু)",
                titleEn = "Annexure-IIA: DoB Update for Operator Error (For Children)",
                subjectBn = "অপারেটর ভুলের জন্য শিশুদের জন্মতারিখ সংশোধনের স্ব-ঘোষণা (Annexure-IIA)",
                subjectEn = "Self-Declaration for DoB Update due to Operator Error (For Children - Annexure-IIA)",
                field1LabelBn = "সন্তানের সঠিক জন্ম তারিখ (Correct DoB)",
                field1LabelEn = "Child's Correct Date of Birth (DD/MM/YYYY)",
                field2LabelBn = "সন্তানের আধার EID নম্বর (Enrolment EID)",
                field2LabelEn = "Child's Enrolment / Update (EID) No",
                bodyTemplateBn = { name, addr, _, f1, f2 ->
                    "Annexure-IIA Self-Declaration by $name for Minor Child's Operator Error. Correct DoB: $f1, EID: $f2."
                },
                bodyTemplateEn = { name, addr, _, f1, f2 ->
                    "Annexure-IIA Self-Declaration by $name for Minor Child's Operator Error. Correct DoB: $f1, EID: $f2."
                }
            ),
            LetterTypeItem(
                id = "aadhaar_annexure_3",
                titleBn = "Annexure-III: আধার পুনরায় সক্রিয়করণ এফিডেভিট (প্রাপ্তবয়স্ক)",
                titleEn = "Annexure-III: Affidavit for Reactivation of Aadhaar (For Adults)",
                subjectBn = "নিষ্ক্রিয় আধার কার্ড পুনরায় সক্রিয়করণের এফিডেভিট (Annexure-III)",
                subjectEn = "Affidavit for Reactivation of Deactivated Aadhaar (For Adults - Annexure-III)",
                field1LabelBn = "নিষ্ক্রিয় আধার নম্বর (Deactivated Aadhaar)",
                field1LabelEn = "Deactivated Aadhaar Number (12 Digits)",
                field2LabelBn = "পূর্বে আবেদনকৃত EID নম্বর (Enrolment EID)",
                field2LabelEn = "Enrolment Request (EID) Number",
                bodyTemplateBn = { name, addr, _, f1, f2 ->
                    "Annexure-III Reactivation Affidavit by $name for Aadhaar $f1. EID: $f2."
                },
                bodyTemplateEn = { name, addr, _, f1, f2 ->
                    "Annexure-III Reactivation Affidavit by $name for Aadhaar $f1. EID: $f2."
                }
            ),
            LetterTypeItem(
                id = "aadhaar_annexure_3a",
                titleBn = "Annexure-IIIA: আধার পুনরায় সক্রিয়করণ এফিডেভিট (শিশু)",
                titleEn = "Annexure-IIIA: Affidavit for Reactivation of Aadhaar (For Children)",
                subjectBn = "শিশুদের নিষ্ক্রিয় আধার কার্ড পুনরায় সক্রিয়করণের এফিডেভিট (Annexure-IIIA)",
                subjectEn = "Affidavit for Reactivation of Deactivated Aadhaar (For Children - Annexure-IIIA)",
                field1LabelBn = "সন্তানের নিষ্ক্রিয় আধার নম্বর (Child Aadhaar)",
                field1LabelEn = "Child's Deactivated Aadhaar Number",
                field2LabelBn = "পূর্বে আবেদনকৃত EID নম্বর (Enrolment EID)",
                field2LabelEn = "Enrolment Request (EID) Number",
                bodyTemplateBn = { name, addr, _, f1, f2 ->
                    "Annexure-IIIA Reactivation Affidavit by $name for Minor's Aadhaar $f1. EID: $f2."
                },
                bodyTemplateEn = { name, addr, _, f1, f2 ->
                    "Annexure-IIIA Reactivation Affidavit by $name for Minor's Aadhaar $f1. EID: $f2."
                }
            )
        )
    )
}

/**
 * Generate formatted plain text for Live Preview inside ApplicationMakerScreen
 */
fun generateAadhaarAffidavitPreview(
    annexureId: String,
    deponentName: String,
    guardianName: String,
    address: String,
    aadhaarNo: String,
    childName: String,
    childAadhaarNo: String,
    correctDob: String,
    oldDob: String,
    docName: String,
    docNumber: String,
    eidNumber: String,
    updateCount: String,
    stampValue: String,
    date: String,
    place: String,
    hasSignature: Boolean
): String {
    val sigLabel = if (hasSignature) "[স্বাক্ষর সংযুক্ত / Signature Attached]" else "........................................"
    val dName = deponentName.ifBlank { "................................" }
    val gName = guardianName.ifBlank { "................................" }
    val addr = address.ifBlank { "................................................" }
    val aNo = aadhaarNo.ifBlank { "XXXX-XXXX-XXXX" }
    val cName = childName.ifBlank { "................................" }
    val cAadhaar = childAadhaarNo.ifBlank { "XXXX-XXXX-XXXX" }
    val cDob = correctDob.ifBlank { "DD/MM/YYYY" }
    val oDob = oldDob.ifBlank { "DD/MM/YYYY" }
    val dDoc = docName.ifBlank { "Birth Certificate / Valid Proof" }
    val dNum = docNumber.ifBlank { "................" }
    val eid = eidNumber.ifBlank { "................................" }
    val pName = place.ifBlank { "................" }
    val sVal = stampValue.ifBlank { "₹ 10" }

    return when (annexureId) {
        "aadhaar_annexure_1" -> """
            ANNEXURE - I
            AFFIDAVIT FOR DATE OF BIRTH UPDATION (FOR ADULTS)
            (Applicable for cases where the DoB is verified in Aadhaar. To be printed on non-judicial stamp paper of minimum value of $sVal)
            ------------------------------------------------------------------------------------------------------------------------

            1. I, $dName, S/D/W/o $gName, resident of $addr, holding Aadhaar number $aNo, do hereby solemnly affirm and declare as under:-

            i) That I am the resident of the above said address.
            ii) That my correct date of birth is $cDob.
            iii) That the earlier recorded date of birth in Aadhaar is $oDob based on $dDoc document submitted by me.
            iv) That I have $updateCount updated my date of birth in Aadhaar.
            v) That currently a corrected version of the same document as mentioned in (iii) above / birth certificate is being provided in support of DoB update request.
            vi) That I wish to get my date of birth updated in the Aadhaar as $cDob for which I am submitting $dDoc as proof of date of birth.
            vii) That I further undertake that I shall not be eligible for any further updation of my date of birth in Aadhaar.

            2. I further undertake that if the document submitted by me is found to be false/forged, my Aadhaar may be deactivated/omitted as per the provisions of Regulation 28 of the Aadhaar (Enrolment and Update) Regulations, 2016, and UIDAI shall be free to initiate appropriate legal action against me.

            3. I solemnly declare that the above statements are true and correct to the best of my knowledge and belief and nothing material has been concealed therein.

            Date: $date
            Place: $pName

                                                                $sigLabel
                                                                DEPONENT
                                                                Name: $dName
                                                                Aadhaar No: $aNo

            ------------------------------------------------------------------------------------------------------------------------
            VERIFICATION:
            Verified at $pName on this $date that the contents of paragraphs 1 to 3 of the above affidavit are true and correct to the best of my knowledge and belief. No part of it is false and nothing material has been concealed therefrom.

                                                                DEPONENT

            ATTESTATION:
            (To be attested by Judicial / Executive Magistrate or Notary Public)
            Identified by me / Sworn before me:
            Date: $date
            [Seal & Signature of Magistrate / Notary Public]
        """.trimIndent()

        "aadhaar_annexure_1a" -> """
            ANNEXURE - IA
            AFFIDAVIT FOR DATE OF BIRTH UPDATION (FOR CHILDREN / MINOR)
            (To be printed on non-judicial stamp paper of minimum value of $sVal)
            ------------------------------------------------------------------------------------------------------------------------

            1. I, $dName, S/D/W/o $gName, resident of $addr, holding Aadhaar number $aNo, do hereby solemnly affirm and declare on behalf of my minor child $cName, holding Aadhaar number $cAadhaar, as under:-

            i) That my minor child $cName is residing with me at the above said address.
            ii) That the correct date of birth of my minor child is $cDob.
            iii) That the earlier recorded date of birth in Aadhaar is $oDob based on $dDoc document submitted earlier.
            iv) That the date of birth of my child has been updated $updateCount in Aadhaar.
            v) That currently Birth Certificate / valid document ($dDoc) issued by competent authority is being submitted in support of this DoB update request.
            vi) That I wish to get the date of birth updated in my child's Aadhaar as $cDob for which I am submitting $dDoc as proof of date of birth.
            vii) That I further undertake that my child shall not be eligible for any further updation of date of birth in Aadhaar.

            2. I further undertake that if the document submitted is found to be false/forged, the Aadhaar of my child may be deactivated/omitted as per Regulation 28 of the Aadhaar (Enrolment and Update) Regulations, 2016, and UIDAI shall be free to initiate appropriate legal action.

            3. I solemnly declare that the above statements are true and correct to the best of my knowledge and belief.

            Date: $date
            Place: $pName

                                                                $sigLabel
                                                                DEPONENT
                                                                (Parent / Guardian of Minor)
                                                                Name: $dName
                                                                Aadhaar No: $aNo

            ------------------------------------------------------------------------------------------------------------------------
            VERIFICATION & ATTESTATION:
            Verified at $pName on this $date by Judicial / Executive Magistrate or Notary Public.
            [Seal & Signature of Magistrate / Notary Public]
        """.trimIndent()

        "aadhaar_annexure_2" -> """
            ANNEXURE - II
            SELF-DECLARATION REQUESTING DoB UPDATE FOR CORRECTING OPERATOR ERROR / TYPOGRAPHICAL MISTAKE (FOR ADULTS)
            ------------------------------------------------------------------------------------------------------------------------

            1. I, $dName, S/D/W/o $gName, resident of $addr, holding Aadhaar number $aNo, do hereby state and declare as under:-

            i) That I enrolled / requested update for Aadhaar vide Enrolment/Update Request (EID) No. $eid dated $date.
            ii) That during enrolment / update, I submitted valid document namely $dDoc bearing document number $dNum showing my correct date of birth as $cDob.
            iii) That due to typographical error / mistake on the part of the enrolment operator, my date of birth was wrongly entered as $oDob instead of $cDob.
            iv) That the mistake is purely typographical and attributable to the operator, as evident from the document uploaded/submitted at the time of enrolment.
            v) That I request UIDAI to kindly correct the typographical error and update my date of birth as $cDob in Aadhaar records.

            2. I undertake that the information provided above is true to the best of my knowledge and belief. If any information or document is found to be false/incorrect, my Aadhaar may be deactivated under Regulation 28 of Aadhaar Regulations 2016.

            Date: $date
            Place: $pName

                                                                $sigLabel
                                                                Signature of Resident
                                                                Name: $dName
                                                                Aadhaar No: $aNo
        """.trimIndent()

        "aadhaar_annexure_2a" -> """
            ANNEXURE - IIA
            SELF-DECLARATION REQUESTING DoB UPDATE FOR CORRECTING OPERATOR ERROR / TYPOGRAPHICAL MISTAKE (FOR CHILDREN)
            ------------------------------------------------------------------------------------------------------------------------

            1. I, $dName, S/D/W/o $gName, resident of $addr, holding Aadhaar number $aNo, parent/guardian of minor child $cName (Aadhaar No: $cAadhaar), do hereby state and declare as under:-

            i) That my minor child was enrolled / updated vide EID No. $eid dated $date.
            ii) That a valid Birth Certificate / document namely $dDoc bearing number $dNum was submitted at the time of enrolment showing the child's correct date of birth as $cDob.
            iii) That due to operator typographical error, the date of birth was wrongly recorded as $oDob in the child's Aadhaar.
            iv) That I request UIDAI to kindly correct this operator error and update my child's date of birth as $cDob.

            2. I undertake that the information provided above is true to the best of my knowledge and belief.

            Date: $date
            Place: $pName

                                                                $sigLabel
                                                                Signature of Parent / Guardian
                                                                Name: $dName
                                                                Child's Name: $cName
                                                                Aadhaar No: $aNo
        """.trimIndent()

        "aadhaar_annexure_3" -> """
            ANNEXURE - III
            AFFIDAVIT FOR REACTIVATION OF AADHAAR (FOR ADULTS)
            (To be printed on non-judicial stamp paper of minimum value of $sVal)
            ------------------------------------------------------------------------------------------------------------------------

            1. I, $dName, S/D/W/o $gName, resident of $addr, holding Aadhaar number $aNo, do hereby solemnly affirm and state as under:-

            i) That my Aadhaar number $aNo has been deactivated by UIDAI under the provisions of Regulation 28 of the Aadhaar (Enrolment and Update) Regulations, 2016.
            ii) That the deactivation was on account of non-genuine / defective / invalid document submitted earlier during enrolment/update vide EID No. $eid.
            iii) That I tender my unconditional apology for the error/lapse that occurred during the earlier enrolment/update process, which was unintentional and without any malafide intent.
            iv) That I am now submitting genuine, verified, and legally valid supporting documents, namely $dDoc bearing number $dNum in support of my identity, date of birth, and address.
            v) That I solemnly pray that my past inadvertent lapse may kindly be pardoned and my Aadhaar number $aNo may be reactivated in the UIDAI system.

            2. I hereby undertake that all documents and information submitted now are 100% genuine and authentic. If found false at any stage, I shall be liable for prosecution under Indian Penal Code and Aadhaar Act, 2016.

            3. I solemnly declare that the above statements are true and correct to the best of my knowledge and belief.

            Date: $date
            Place: $pName

                                                                $sigLabel
                                                                DEPONENT
                                                                Name: $dName
                                                                Aadhaar No: $aNo

            ------------------------------------------------------------------------------------------------------------------------
            VERIFICATION & ATTESTATION:
            Verified at $pName on this $date before Judicial / Executive Magistrate or Notary Public.
            [Seal & Signature of Magistrate / Notary Public]
        """.trimIndent()

        "aadhaar_annexure_3a" -> """
            ANNEXURE - IIIA
            AFFIDAVIT FOR REACTIVATION OF AADHAAR (FOR CHILDREN / MINOR)
            (To be printed on non-judicial stamp paper of minimum value of $sVal)
            ------------------------------------------------------------------------------------------------------------------------

            1. I, $dName, S/D/W/o $gName, resident of $addr, holding Aadhaar number $aNo, parent/guardian of minor child $cName, holding deactivated Aadhaar number $cAadhaar, do hereby solemnly affirm and declare as under:-

            i) That the Aadhaar number $cAadhaar of my minor child $cName has been deactivated by UIDAI under Regulation 28 of Aadhaar (Enrolment and Update) Regulations, 2016.
            ii) That the deactivation occurred due to defective / invalid document submitted earlier during enrolment/update vide EID No. $eid.
            iii) That I, as parent/guardian, tender unconditional apology for the inadvertent lapse that occurred previously during my child's enrolment.
            iv) That I am now submitting genuine and verifiable original Birth Certificate / document $dDoc bearing number $dNum issued by competent authority in support of my child's enrolment.
            v) That I pray that the inadvertent lapse may be pardoned and my child's Aadhaar number $cAadhaar may kindly be reactivated in the UIDAI system.

            2. I hereby undertake that all documents submitted now are 100% genuine and authentic.

            3. I solemnly declare that the above statements are true and correct.

            Date: $date
            Place: $pName

                                                                $sigLabel
                                                                DEPONENT
                                                                (Parent / Guardian of Minor)
                                                                Name: $dName
                                                                Aadhaar No: $aNo

            ------------------------------------------------------------------------------------------------------------------------
            VERIFICATION & ATTESTATION:
            Verified at $pName on this $date before Judicial / Executive Magistrate or Notary Public.
            [Seal & Signature of Magistrate / Notary Public]
        """.trimIndent()

        else -> ""
    }
}

/**
 * Generate pixel-perfect single A4 Page HTML for Print & PDF
 */
fun generateAadhaarAffidavitHtml(
    annexureId: String,
    deponentName: String,
    guardianName: String,
    address: String,
    aadhaarNo: String,
    childName: String,
    childAadhaarNo: String,
    correctDob: String,
    oldDob: String,
    docName: String,
    docNumber: String,
    eidNumber: String,
    updateCount: String,
    stampValue: String,
    date: String,
    place: String,
    signatureBase64: String?
): String {
    val dName = if (deponentName.isNotBlank()) "<u>$deponentName</u>" else "................................"
    val gName = if (guardianName.isNotBlank()) "<u>$guardianName</u>" else "................................"
    val addr = if (address.isNotBlank()) "<u>$address</u>" else "................................................................"
    val aNo = if (aadhaarNo.isNotBlank()) "<u>$aadhaarNo</u>" else "................................"
    val cName = if (childName.isNotBlank()) "<u>$childName</u>" else "................................"
    val cAadhaar = if (childAadhaarNo.isNotBlank()) "<u>$childAadhaarNo</u>" else "................................"
    val cDob = if (correctDob.isNotBlank()) "<u>$correctDob</u>" else "................"
    val oDob = if (oldDob.isNotBlank()) "<u>$oldDob</u>" else "................"
    val dDoc = if (docName.isNotBlank()) "<u>$docName</u>" else "................................"
    val dNum = if (docNumber.isNotBlank()) "<u>$docNumber</u>" else "................"
    val eid = if (eidNumber.isNotBlank()) "<u>$eidNumber</u>" else "................................"
    val pName = if (place.isNotBlank()) "<u>$place</u>" else "................"
    val sVal = stampValue.ifBlank { "₹ 10" }

    val rawDeponentName = deponentName.ifBlank { "................" }
    val rawAadhaar = aadhaarNo.ifBlank { "................" }

    val signatureHtml = if (!signatureBase64.isNullOrBlank()) {
        """<img src="data:image/png;base64,$signatureBase64" class="sig-img" alt="Signature" />"""
    } else {
        """<div class="sig-dots">................................</div>"""
    }

    val isSelfDeclaration = annexureId == "aadhaar_annexure_2" || annexureId == "aadhaar_annexure_2a"

    val headerHtml: String
    val bodyHtml: String
    val footerHtml: String

    when (annexureId) {
        "aadhaar_annexure_1" -> {
            headerHtml = """
                <div class="annex-title">Annexure-I- For adults</div>
                <div class="main-title">Affidavit for date of birth updation</div>
                <div class="subtitle">(Applicable for cases where the DoB is verified in Aadhaar. To be printed on non-judicial stamp paper of minimum value of $sVal)</div>
            """.trimIndent()

            bodyHtml = """
                <p class="para">1. I, $dName S/D/W/o $gName, resident of $addr, holding Aadhaar number $aNo, do hereby solemnly affirm and declare as under:-</p>
                <div class="roman-list">
                    <div class="roman-item"><span class="roman-num">i)</span> That I am the resident of the above said address.</div>
                    <div class="roman-item"><span class="roman-num">ii)</span> That my correct date of birth is $cDob.</div>
                    <div class="roman-item"><span class="roman-num">iii)</span> That the earlier recorded date of birth in Aadhaar is $oDob based on $dDoc document submitted by me.</div>
                    <div class="roman-item"><span class="roman-num">iv)</span> That I have <u>$updateCount</u> updated my date of birth in Aadhaar.</div>
                    <div class="roman-item"><span class="roman-num">v)</span> That currently a corrected version of the same document as mentioned in (iii) above / birth certificate is being provided in support of DoB update request.</div>
                    <div class="roman-item"><span class="roman-num">vi)</span> That I wish to get my date of birth updated in the Aadhaar as $cDob for which I am submitting $dDoc as proof of date of birth.</div>
                    <div class="roman-item"><span class="roman-num">vii)</span> That I further undertake that I shall not be eligible for any further updation of my date of birth in Aadhaar.</div>
                </div>
                <p class="para">2. I further undertake that if the document submitted by me is found to be false/forged, my Aadhaar may be deactivated/omitted as per the provisions of Regulation 28 of the Aadhaar (Enrolment and Update) Regulations, 2016, and UIDAI shall be free to initiate appropriate legal action against me.</p>
                <p class="para">3. I solemnly declare that the above statements are true and correct to the best of my knowledge and belief and nothing material has been concealed therein.</p>
            """.trimIndent()

            footerHtml = """
                <table class="footer-table">
                    <tr>
                        <td style="width: 50%; vertical-align: bottom;">
                            <div>Date: <u>$date</u></div>
                            <div>Place: $pName</div>
                        </td>
                        <td style="width: 50%; text-align: right; vertical-align: bottom;">
                            <div class="sig-container">$signatureHtml</div>
                            <div class="deponent-label">DEPONENT</div>
                            <div>Name: $rawDeponentName</div>
                            <div>Aadhaar No: $rawAadhaar</div>
                        </td>
                    </tr>
                </table>

                <div class="box-section">
                    <div class="section-title">VERIFICATION</div>
                    <p style="margin: 0; font-size: 9.5pt; text-align: justify;">
                        Verified at $pName on this <u>$date</u> that the contents of paragraphs 1 to 3 of the above affidavit are true and correct to the best of my knowledge and belief. No part of it is false and nothing material has been concealed therefrom.
                    </p>
                    <div style="text-align: right; margin-top: 14px; font-weight: bold; font-size: 9.5pt;">DEPONENT</div>
                </div>

                <div class="box-section" style="margin-top: 8px;">
                    <div class="section-title">ATTESTATION</div>
                    <div style="font-size: 9pt; color: #333;">(To be attested by Judicial / Executive Magistrate or Notary Public)</div>
                    <table style="width: 100%; margin-top: 18px; font-size: 9pt;">
                        <tr>
                            <td style="width: 50%;">[Seal & Stamp of Notary / Magistrate]</td>
                            <td style="width: 50%; text-align: right;">Signature & Designation of Attesting Authority</td>
                        </tr>
                    </table>
                </div>
            """.trimIndent()
        }

        "aadhaar_annexure_1a" -> {
            headerHtml = """
                <div class="annex-title">Annexure IA- For children</div>
                <div class="main-title">Affidavit for date of birth updation</div>
                <div class="subtitle">(To be printed on non-judicial stamp paper of minimum value of $sVal)</div>
            """.trimIndent()

            bodyHtml = """
                <p class="para">1. I, $dName S/D/W/o $gName, resident of $addr, holding Aadhaar number $aNo, do hereby solemnly affirm and declare on behalf of my minor child $cName, holding Aadhaar number $cAadhaar, as under:-</p>
                <div class="roman-list">
                    <div class="roman-item"><span class="roman-num">i)</span> That my minor child $cName is residing with me at the above said address.</div>
                    <div class="roman-item"><span class="roman-num">ii)</span> That the correct date of birth of my minor child is $cDob.</div>
                    <div class="roman-item"><span class="roman-num">iii)</span> That the earlier recorded date of birth in Aadhaar is $oDob based on $dDoc document submitted earlier.</div>
                    <div class="roman-item"><span class="roman-num">iv)</span> That the date of birth of my child has been updated <u>$updateCount</u> in Aadhaar.</div>
                    <div class="roman-item"><span class="roman-num">v)</span> That currently Birth Certificate / valid document ($dDoc) issued by competent authority is being submitted in support of this DoB update request.</div>
                    <div class="roman-item"><span class="roman-num">vi)</span> That I wish to get the date of birth updated in my child's Aadhaar as $cDob for which I am submitting $dDoc as proof of date of birth.</div>
                    <div class="roman-item"><span class="roman-num">vii)</span> That I further undertake that my child shall not be eligible for any further updation of date of birth in Aadhaar.</div>
                </div>
                <p class="para">2. I further undertake that if the document submitted is found to be false/forged, the Aadhaar of my child may be deactivated/omitted as per Regulation 28 of the Aadhaar (Enrolment and Update) Regulations, 2016, and UIDAI shall be free to initiate legal action.</p>
                <p class="para">3. I solemnly declare that the above statements are true and correct to the best of my knowledge and belief.</p>
            """.trimIndent()

            footerHtml = """
                <table class="footer-table">
                    <tr>
                        <td style="width: 50%; vertical-align: bottom;">
                            <div>Date: <u>$date</u></div>
                            <div>Place: $pName</div>
                        </td>
                        <td style="width: 50%; text-align: right; vertical-align: bottom;">
                            <div class="sig-container">$signatureHtml</div>
                            <div class="deponent-label">DEPONENT</div>
                            <div style="font-size: 9.5pt;">(Parent / Guardian of Minor)</div>
                            <div>Name: $rawDeponentName</div>
                            <div>Aadhaar No: $rawAadhaar</div>
                        </td>
                    </tr>
                </table>

                <div class="box-section">
                    <div class="section-title">VERIFICATION & ATTESTATION</div>
                    <p style="margin: 0; font-size: 9.5pt; text-align: justify;">
                        Verified at $pName on this <u>$date</u> that the contents of the above affidavit are true and correct to the best of my knowledge and belief. Attested by Judicial / Executive Magistrate or Notary Public.
                    </p>
                    <table style="width: 100%; margin-top: 18px; font-size: 9pt;">
                        <tr>
                            <td style="width: 50%;">[Seal & Stamp of Notary / Magistrate]</td>
                            <td style="width: 50%; text-align: right;">Signature & Designation</td>
                        </tr>
                    </table>
                </div>
            """.trimIndent()
        }

        "aadhaar_annexure_2" -> {
            headerHtml = """
                <div class="annex-title">Annexure II- For adults</div>
                <div class="main-title">Self-Declaration requesting DoB update for correcting operator error / typographical mistake</div>
                <div class="subtitle">(To be submitted by resident for correction of Date of Birth in Aadhaar)</div>
            """.trimIndent()

            bodyHtml = """
                <p class="para">1. I, $dName S/D/W/o $gName, resident of $addr, holding Aadhaar number $aNo, do hereby state and declare as under:-</p>
                <div class="roman-list">
                    <div class="roman-item"><span class="roman-num">i)</span> That I enrolled / requested update for Aadhaar vide Enrolment/Update Request (EID) No. $eid dated <u>$date</u>.</div>
                    <div class="roman-item"><span class="roman-num">ii)</span> That during enrolment / update, I submitted valid document namely $dDoc bearing document number $dNum showing my correct date of birth as $cDob.</div>
                    <div class="roman-item"><span class="roman-num">iii)</span> That due to typographical error / mistake on the part of the enrolment operator, my date of birth was wrongly entered as $oDob instead of $cDob.</div>
                    <div class="roman-item"><span class="roman-num">iv)</span> That the mistake is purely typographical and attributable to the operator, as evident from the document uploaded/submitted at the time of enrolment.</div>
                    <div class="roman-item"><span class="roman-num">v)</span> That I request UIDAI to kindly correct the typographical error and update my date of birth as $cDob in Aadhaar records.</div>
                </div>
                <p class="para">2. I undertake that the information provided above is true to the best of my knowledge and belief. If any information or document is found to be false/incorrect, my Aadhaar may be deactivated under Regulation 28 of Aadhaar Regulations 2016.</p>
            """.trimIndent()

            footerHtml = """
                <table class="footer-table" style="margin-top: 40px;">
                    <tr>
                        <td style="width: 50%; vertical-align: bottom;">
                            <div>Date: <u>$date</u></div>
                            <div>Place: $pName</div>
                        </td>
                        <td style="width: 50%; text-align: right; vertical-align: bottom;">
                            <div class="sig-container">$signatureHtml</div>
                            <div class="deponent-label">Signature of Resident</div>
                            <div>Name: $rawDeponentName</div>
                            <div>Aadhaar No: $rawAadhaar</div>
                        </td>
                    </tr>
                </table>
            """.trimIndent()
        }

        "aadhaar_annexure_2a" -> {
            headerHtml = """
                <div class="annex-title">Annexure IIA- For children</div>
                <div class="main-title">Self-Declaration requesting DoB update for correcting operator error / typographical mistake</div>
                <div class="subtitle">(To be submitted by Parent / Guardian of minor child)</div>
            """.trimIndent()

            bodyHtml = """
                <p class="para">1. I, $dName S/D/W/o $gName, resident of $addr, holding Aadhaar number $aNo, parent/guardian of minor child $cName (Aadhaar No: $cAadhaar), do hereby state and declare as under:-</p>
                <div class="roman-list">
                    <div class="roman-item"><span class="roman-num">i)</span> That my minor child was enrolled / updated vide EID No. $eid dated <u>$date</u>.</div>
                    <div class="roman-item"><span class="roman-num">ii)</span> That a valid Birth Certificate / document namely $dDoc bearing number $dNum was submitted at the time of enrolment showing the child's correct date of birth as $cDob.</div>
                    <div class="roman-item"><span class="roman-num">iii)</span> That due to operator typographical error, the date of birth was wrongly recorded as $oDob in the child's Aadhaar.</div>
                    <div class="roman-item"><span class="roman-num">iv)</span> That I request UIDAI to kindly correct this operator error and update my child's date of birth as $cDob.</div>
                </div>
                <p class="para">2. I undertake that the information provided above is true to the best of my knowledge and belief.</p>
            """.trimIndent()

            footerHtml = """
                <table class="footer-table" style="margin-top: 40px;">
                    <tr>
                        <td style="width: 50%; vertical-align: bottom;">
                            <div>Date: <u>$date</u></div>
                            <div>Place: $pName</div>
                        </td>
                        <td style="width: 50%; text-align: right; vertical-align: bottom;">
                            <div class="sig-container">$signatureHtml</div>
                            <div class="deponent-label">Signature of Parent / Guardian</div>
                            <div>Name: $rawDeponentName</div>
                            <div>Child's Name: ${childName.ifBlank { "................" }}</div>
                            <div>Aadhaar No: $rawAadhaar</div>
                        </td>
                    </tr>
                </table>
            """.trimIndent()
        }

        "aadhaar_annexure_3" -> {
            headerHtml = """
                <div class="annex-title">Annexure III- For adults</div>
                <div class="main-title">Affidavit for reactivation of Aadhaar</div>
                <div class="subtitle">(To be printed on non-judicial stamp paper of minimum value of $sVal)</div>
            """.trimIndent()

            bodyHtml = """
                <p class="para">1. I, $dName S/D/W/o $gName, resident of $addr, holding Aadhaar number $aNo, do hereby solemnly affirm and state as under:-</p>
                <div class="roman-list">
                    <div class="roman-item"><span class="roman-num">i)</span> That my Aadhaar number $aNo has been deactivated by UIDAI under the provisions of Regulation 28 of the Aadhaar (Enrolment and Update) Regulations, 2016.</div>
                    <div class="roman-item"><span class="roman-num">ii)</span> That the deactivation was on account of non-genuine / defective / invalid document submitted earlier during enrolment/update vide EID No. $eid.</div>
                    <div class="roman-item"><span class="roman-num">iii)</span> That I tender my unconditional apology for the error/lapse that occurred during the earlier enrolment/update process, which was unintentional and without any malafide intent.</div>
                    <div class="roman-item"><span class="roman-num">iv)</span> That I am now submitting genuine, verified, and legally valid supporting documents, namely $dDoc bearing number $dNum in support of my identity, date of birth, and address.</div>
                    <div class="roman-item"><span class="roman-num">v)</span> That I solemnly pray that my past inadvertent lapse may kindly be pardoned and my Aadhaar number $aNo may be reactivated in the UIDAI system.</div>
                </div>
                <p class="para">2. I hereby undertake that all documents and information submitted now are 100% genuine and authentic. If found false at any stage, I shall be liable for prosecution under Indian Penal Code and Aadhaar Act, 2016.</p>
                <p class="para">3. I solemnly declare that the above statements are true and correct to the best of my knowledge and belief.</p>
            """.trimIndent()

            footerHtml = """
                <table class="footer-table">
                    <tr>
                        <td style="width: 50%; vertical-align: bottom;">
                            <div>Date: <u>$date</u></div>
                            <div>Place: $pName</div>
                        </td>
                        <td style="width: 50%; text-align: right; vertical-align: bottom;">
                            <div class="sig-container">$signatureHtml</div>
                            <div class="deponent-label">DEPONENT</div>
                            <div>Name: $rawDeponentName</div>
                            <div>Aadhaar No: $rawAadhaar</div>
                        </td>
                    </tr>
                </table>

                <div class="box-section">
                    <div class="section-title">VERIFICATION & ATTESTATION</div>
                    <p style="margin: 0; font-size: 9.5pt; text-align: justify;">
                        Verified at $pName on this <u>$date</u> before Judicial / Executive Magistrate or Notary Public.
                    </p>
                    <table style="width: 100%; margin-top: 18px; font-size: 9pt;">
                        <tr>
                            <td style="width: 50%;">[Seal & Stamp of Notary / Magistrate]</td>
                            <td style="width: 50%; text-align: right;">Signature & Designation</td>
                        </tr>
                    </table>
                </div>
            """.trimIndent()
        }

        "aadhaar_annexure_3a" -> {
            headerHtml = """
                <div class="annex-title">Annexure IIIA- For children</div>
                <div class="main-title">Affidavit for reactivation of Aadhaar</div>
                <div class="subtitle">(To be printed on non-judicial stamp paper of minimum value of $sVal)</div>
            """.trimIndent()

            bodyHtml = """
                <p class="para">1. I, $dName S/D/W/o $gName, resident of $addr, holding Aadhaar number $aNo, parent/guardian of minor child $cName, holding deactivated Aadhaar number $cAadhaar, do hereby solemnly affirm and declare as under:-</p>
                <div class="roman-list">
                    <div class="roman-item"><span class="roman-num">i)</span> That the Aadhaar number $cAadhaar of my minor child $cName has been deactivated by UIDAI under Regulation 28 of Aadhaar (Enrolment and Update) Regulations, 2016.</div>
                    <div class="roman-item"><span class="roman-num">ii)</span> That the deactivation occurred due to defective / invalid document submitted earlier during enrolment/update vide EID No. $eid.</div>
                    <div class="roman-item"><span class="roman-num">iii)</span> That I, as parent/guardian, tender unconditional apology for the inadvertent lapse that occurred previously during my child's enrolment.</div>
                    <div class="roman-item"><span class="roman-num">iv)</span> That I am now submitting genuine and verifiable original Birth Certificate / document $dDoc bearing number $dNum issued by competent authority in support of my child's enrolment.</div>
                    <div class="roman-item"><span class="roman-num">v)</span> That I pray that the inadvertent lapse may be pardoned and my child's Aadhaar number $cAadhaar may kindly be reactivated in the UIDAI system.</div>
                </div>
                <p class="para">2. I hereby undertake that all documents submitted now are 100% genuine and authentic.</p>
                <p class="para">3. I solemnly declare that the above statements are true and correct.</p>
            """.trimIndent()

            footerHtml = """
                <table class="footer-table">
                    <tr>
                        <td style="width: 50%; vertical-align: bottom;">
                            <div>Date: <u>$date</u></div>
                            <div>Place: $pName</div>
                        </td>
                        <td style="width: 50%; text-align: right; vertical-align: bottom;">
                            <div class="sig-container">$signatureHtml</div>
                            <div class="deponent-label">DEPONENT</div>
                            <div style="font-size: 9.5pt;">(Parent / Guardian of Minor)</div>
                            <div>Name: $rawDeponentName</div>
                            <div>Aadhaar No: $rawAadhaar</div>
                        </td>
                    </tr>
                </table>

                <div class="box-section">
                    <div class="section-title">VERIFICATION & ATTESTATION</div>
                    <p style="margin: 0; font-size: 9.5pt; text-align: justify;">
                        Verified at $pName on this <u>$date</u> before Judicial / Executive Magistrate or Notary Public.
                    </p>
                    <table style="width: 100%; margin-top: 18px; font-size: 9pt;">
                        <tr>
                            <td style="width: 50%;">[Seal & Stamp of Notary / Magistrate]</td>
                            <td style="width: 50%; text-align: right;">Signature & Designation</td>
                        </tr>
                    </table>
                </div>
            """.trimIndent()
        }

        else -> {
            headerHtml = ""
            bodyHtml = ""
            footerHtml = ""
        }
    }

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                @page {
                    size: A4 portrait;
                    margin: 18mm 18mm 16mm 18mm;
                }
                body {
                    font-family: 'Times New Roman', Times, 'Noto Sans Bengali', serif, sans-serif;
                    font-size: 10.5pt;
                    line-height: 1.45;
                    color: #000;
                    margin: 0;
                    padding: 0;
                }
                .annex-title {
                    text-align: center;
                    font-weight: bold;
                    font-size: 13pt;
                    text-transform: uppercase;
                    margin-bottom: 2px;
                }
                .main-title {
                    text-align: center;
                    font-weight: bold;
                    font-size: 12pt;
                    text-transform: uppercase;
                    text-decoration: underline;
                    margin-bottom: 3px;
                }
                .subtitle {
                    text-align: center;
                    font-size: 8.5pt;
                    font-style: italic;
                    color: #333;
                    margin-bottom: 14px;
                    line-height: 1.25;
                }
                .para {
                    text-align: justify;
                    margin: 8px 0;
                    font-size: 10.2pt;
                }
                .roman-list {
                    margin-left: 6px;
                    margin-bottom: 8px;
                }
                .roman-item {
                    display: flex;
                    align-items: flex-start;
                    margin: 4px 0;
                    text-align: justify;
                    font-size: 10.2pt;
                }
                .roman-num {
                    display: inline-block;
                    min-width: 28px;
                    font-weight: bold;
                }
                .footer-table {
                    width: 100%;
                    margin-top: 14px;
                    border-collapse: collapse;
                    font-size: 10pt;
                }
                .sig-container {
                    display: inline-block;
                    text-align: right;
                    min-width: 130px;
                    margin-bottom: 2px;
                }
                .sig-img {
                    max-height: 48px;
                    max-width: 150px;
                    object-fit: contain;
                    display: block;
                    margin-left: auto;
                    margin-right: 0;
                }
                .sig-dots {
                    letter-spacing: 2px;
                    color: #222;
                    font-size: 10pt;
                    margin-bottom: 4px;
                }
                .deponent-label {
                    font-weight: bold;
                    font-size: 10.5pt;
                    margin-top: 2px;
                }
                .box-section {
                    border: 1px solid #777;
                    padding: 8px 12px;
                    margin-top: 12px;
                    background-color: #fafafa;
                }
                .section-title {
                    font-weight: bold;
                    font-size: 10pt;
                    text-decoration: underline;
                    margin-bottom: 4px;
                }
            </style>
        </head>
        <body>
            $headerHtml
            $bodyHtml
            $footerHtml
        </body>
        </html>
    """.trimIndent()
}
