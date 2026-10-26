package com.example.ui.screens

/**
 * Data model for Application Categories
 */
data class LetterCategory(
    val id: String,
    val titleBn: String,
    val titleEn: String,
    val defaultRecipientBn: String,
    val defaultRecipientEn: String,
    val letterTypes: List<LetterTypeItem>
)

/**
 * Data model for individual Letter Types inside a Category
 */
data class LetterTypeItem(
    val id: String,
    val titleBn: String,
    val titleEn: String,
    val subjectBn: String,
    val subjectEn: String,
    val bodyTemplateBn: (name: String, addr: String, office: String, field1: String, field2: String) -> String,
    val bodyTemplateEn: (name: String, addr: String, office: String, field1: String, field2: String) -> String,
    val field1LabelBn: String = "",
    val field1LabelEn: String = "",
    val field2LabelBn: String = "",
    val field2LabelEn: String = ""
)

/**
 * Complete Repository containing all 21 Categories and their corresponding Letter Types.
 */
fun getApplicationCategories(): List<LetterCategory> {
    return listOf(
        // 1. ব্যাংক (Bank)
        LetterCategory(
            id = "bank",
            titleBn = "১. ব্যাংক (Bank)",
            titleEn = "1. Bank",
            defaultRecipientBn = "শাখা ব্যবস্থাপক (Branch Manager)",
            defaultRecipientEn = "The Branch Manager",
            letterTypes = listOf(
                LetterTypeItem(
                    id = "bank_mobile",
                    titleBn = "মোবাইল নম্বর পরিবর্তন",
                    titleEn = "Mobile Number Update",
                    subjectBn = "ব্যাংক অ্যাকাউন্টে নতুন মোবাইল নম্বর সংযুক্তির আবেদন",
                    subjectEn = "Application for updating registered mobile number in bank account",
                    field1LabelBn = "অ্যাকাউন্ট নম্বর / Account No",
                    field1LabelEn = "Account Number",
                    field2LabelBn = "নতুন মোবাইল নম্বর / New Mobile No",
                    field2LabelEn = "New Mobile Number",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। আপনার শাখায় আমার সেভিংস অ্যাকাউন্ট রয়েছে, যার নম্বর: ${f1.ifBlank { "................" }}। আমার পূর্বের মোবাইল নম্বরটি বর্তমানে অচল হয়ে গেছে। অতএব, আমার অ্যাকাউন্টের সাথে নতুন মোবাইল নম্বর: ${f2.ifBlank { "................" }} সংযুক্ত করার জন্য বিনীত প্রার্থনা জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "Most respectfully, I state that I am ${name.ifBlank { "................" }}, holding Savings Account No: ${f1.ifBlank { "................" }} in your esteemed branch. As my previous mobile number is no longer active, I request you to kindly update and link my new mobile number: ${f2.ifBlank { "................" }} with my account."
                    }
                ),
                LetterTypeItem(
                    id = "bank_passbook",
                    titleBn = "পাসবুকের আবেদন",
                    titleEn = "New Passbook Issue",
                    subjectBn = "নতুন / ডুপ্লিকেট পাসবুক ইস্যু করার আবেদন",
                    subjectEn = "Application for issuance of new / duplicate passbook",
                    field1LabelBn = "অ্যাকাউন্ট নম্বর / Account No",
                    field1LabelEn = "Account Number",
                    field2LabelBn = "কারণ (যেমন: পাতা শেষ / হারিয়ে গেছে)",
                    field2LabelEn = "Reason (e.g. Completed / Lost)",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। আপনার শাখায় আমার অ্যাকাউন্ট নং ${f1.ifBlank { "................" }}। আমার বর্তমান পাসবইটি (${f2.ifBlank { "পাতা শেষ হয়ে যাওয়ায়" }}) নতুন একটি পাসবুক প্রয়োজন। অতএব, আমাকে অবিলম্বে একটি নতুন পাসবুক প্রদান করার জন্য বিনীত অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I beg to submit that I, ${name.ifBlank { "................" }}, maintain Account No: ${f1.ifBlank { "................" }} at your branch. Due to (${f2.ifBlank { "exhaustion of pages" }}), I require a new passbook. Kindly issue a fresh passbook to me at your earliest convenience."
                    }
                ),
                LetterTypeItem(
                    id = "bank_atm",
                    titleBn = "ATM কার্ডের আবেদন",
                    titleEn = "New ATM / Debit Card",
                    subjectBn = "নতুন এটিএম (Debit) কার্ড প্রদানের আবেদন",
                    subjectEn = "Application for issuance of new ATM / Debit Card",
                    field1LabelBn = "অ্যাকাউন্ট নম্বর / Account No",
                    field1LabelEn = "Account Number",
                    field2LabelBn = "মোবাইল নম্বর / Mobile No",
                    field2LabelEn = "Mobile Number",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। আপনার শাখায় আমার অ্যাকাউন্ট নং ${f1.ifBlank { "................" }}। জরুরি লেনদেনের সুবিধার্থে আমার একটি এটিএম/ডেবিট কার্ডের অত্যন্ত প্রয়োজন। যোগাযোগ নম্বর: ${f2.ifBlank { "................" }}। অতএব, আমার নামে একটি এটিএম কার্ড মঞ্জুর করার অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "Respectfully, I state that I am ${name.ifBlank { "................" }}, holding Account No: ${f1.ifBlank { "................" }} at your branch. For banking convenience, I request you to issue a new ATM/Debit Card for my account. My contact number is ${f2.ifBlank { "................" }}."
                    }
                ),
                LetterTypeItem(
                    id = "bank_cheque",
                    titleBn = "চেকবুকের আবেদন",
                    titleEn = "Cheque Book Request",
                    subjectBn = "নতুন চেকবুক ইস্যু করার আবেদন",
                    subjectEn = "Application for issuance of a new cheque book",
                    field1LabelBn = "অ্যাকাউন্ট নম্বর / Account No",
                    field1LabelEn = "Account Number",
                    field2LabelBn = "চেক পাতার সংখ্যা (যেমন: ২৫ / ৫০ পাতা)",
                    field2LabelEn = "Number of leaves (e.g. 25 / 50)",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। আপনার শাখায় আমার অ্যাকাউন্ট নং ${f1.ifBlank { "................" }}। আমার বর্তমান চেকবইটির পাতা শেষ হয়ে গেছে। নিয়মিত ব্যবসায়িক ও ব্যক্তিগত লেনদেনের জন্য আমার একটি ${f2.ifBlank { "২৫ পাতার" }} নতুন চেকবুক প্রয়োজন। অনুগ্রহ করে নতুন চেকবুক প্রদানের ব্যবস্থা করুন।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I state that I, ${name.ifBlank { "................" }}, hold Account No: ${f1.ifBlank { "................" }}. All leaves in my previous cheque book have been exhausted. Kindly issue a fresh ${f2.ifBlank { "25-leaf" }} cheque book to my registered mailing address."
                    }
                ),
                LetterTypeItem(
                    id = "bank_name",
                    titleBn = "নাম সংশোধন",
                    titleEn = "Name Correction in Bank",
                    subjectBn = "ব্যাংক অ্যাকাউন্টে নামের বানান সংশোধনের আবেদন",
                    subjectEn = "Application for correction of name spelling in bank account",
                    field1LabelBn = "অ্যাকাউন্ট নম্বর / Account No",
                    field1LabelEn = "Account Number",
                    field2LabelBn = "সঠিক নাম ও পরিচয়পত্র নম্বর",
                    field2LabelEn = "Correct Name & ID Reference",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। আপনার শাখায় আমার অ্যাকাউন্ট নং ${f1.ifBlank { "................" }}। উক্ত অ্যাকাউন্টে আমার নামের বানানে সামান্য ভুল রয়েছে। আমার সরকারি পরিচয়পত্র অনুযায়ী সঠিক নাম: ${f2.ifBlank { name }}। এর সপক্ষে আধার/প্যান কার্ডের জেরক্স সংযুক্ত করলাম। দয়া করে নাম সংশোধন করে বাধিত করবেন।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I wish to bring to your kind notice that I maintain Account No: ${f1.ifBlank { "................" }}. There is a typographical error in my name in your bank records. As per my government ID, my correct name is ${f2.ifBlank { name }}. Supporting KYC documents are attached. Kindly rectify the record."
                    }
                ),
                LetterTypeItem(
                    id = "bank_address",
                    titleBn = "ঠিকানা পরিবর্তন",
                    titleEn = "Address Change in Bank",
                    subjectBn = "ব্যাংক অ্যাকাউন্টে ঠিকানা পরিবর্তনের আবেদন",
                    subjectEn = "Application for updating communication address in bank account",
                    field1LabelBn = "অ্যাকাউন্ট নম্বর / Account No",
                    field1LabelEn = "Account Number",
                    field2LabelBn = "নতুন পূর্ণাঙ্গ ঠিকানা / New Address",
                    field2LabelEn = "New Complete Address",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। অ্যাকাউন্ট নং ${f1.ifBlank { "................" }}। বাসস্থান স্থানান্তরের কারণে আমার যোগাযোগের ঠিকানা পরিবর্তিত হয়েছে। নতুন ঠিকানা: ${f2.ifBlank { "................" }}। ঠিকানার প্রমাণপত্র হিসেবে আধার কার্ড সংযুক্ত করলাম। অনুগ্রহ করে আমার অ্যাকাউন্টে নতুন ঠিকানা আপডেট করার অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, maintaining Account No: ${f1.ifBlank { "................" }}, have relocated my residence. My new communication address is: ${f2.ifBlank { "................" }}. I request you to kindly update my address in your records. Relevant address proof is attached."
                    }
                ),
                LetterTypeItem(
                    id = "bank_close",
                    titleBn = "অ্যাকাউন্ট বন্ধের আবেদন",
                    titleEn = "Account Closure Request",
                    subjectBn = "ব্যাংক অ্যাকাউন্ট বন্ধ করার আবেদন",
                    subjectEn = "Application for closure of bank account",
                    field1LabelBn = "অ্যাকাউন্ট নম্বর / Account No",
                    field1LabelEn = "Account Number",
                    field2LabelBn = "অ্যাকাউন্ট বন্ধের কারণ",
                    field2LabelEn = "Reason for closure",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। আপনার শাখায় আমার অ্যাকাউন্ট নং ${f1.ifBlank { "................" }}। অনিবার্য ব্যক্তিগত কারণ (${f2.ifBlank { "স্থান পরিবর্তন ও অপর অ্যাকাউন্টের ব্যবহার" }})-এর জন্য উক্ত অ্যাকাউন্টটি আর পরিচালনা করতে পারছি না। অবশিষ্ট ব্যালেন্স নগদ বা আমার অপর অ্যাকাউন্টে প্রদান করে অ্যাকাউন্টটি বন্ধ করার বিনীত প্রার্থনা জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, holding Account No: ${f1.ifBlank { "................" }}, wish to permanently close this account due to (${f2.ifBlank { "relocation / personal reasons" }}). Kindly disburse remaining balance and close the account along with surrender of my passbook and debit card."
                    }
                ),
                LetterTypeItem(
                    id = "bank_statement",
                    titleBn = "অ্যাকাউন্ট স্টেটমেন্টের আবেদন",
                    titleEn = "Account Statement Request",
                    subjectBn = "ব্যাংক অ্যাকাউন্ট স্টেটমেন্ট প্রদানের আবেদন",
                    subjectEn = "Application for bank account statement",
                    field1LabelBn = "অ্যাকাউন্ট নম্বর / Account No",
                    field1LabelEn = "Account Number",
                    field2LabelBn = "সময়কাল (যেমন: ০১/০১/২০২৪ থেকে ৩০/০৬/২০২৪)",
                    field2LabelEn = "Period (e.g. From Date to To Date)",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। আপনার শাখায় আমার অ্যাকাউন্ট নং ${f1.ifBlank { "................" }}। আয়কর/অফিসিয়াল কাজের প্রয়োজনে উক্ত অ্যাকাউন্টের ${f2.ifBlank { "বিগত ৬ মাসের" }} সিলমোহর ও স্বাক্ষরযুক্ত ব্যাংক স্টেটমেন্ট প্রয়োজন। অনুগ্রহ করে দ্রুত স্টেটমেন্টটি প্রদান করার অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I state that I am ${name.ifBlank { "................" }}, holding Account No: ${f1.ifBlank { "................" }}. For official verification/tax filing, I require a certified bank statement for the period: ${f2.ifBlank { "last 6 months" }}. Please arrange to provide the same."
                    }
                ),
                LetterTypeItem(
                    id = "bank_refund",
                    titleBn = "ব্যর্থ লেনদেন রিফান্ড আবেদন",
                    titleEn = "Failed Transaction Refund",
                    subjectBn = "ATM/অনলাইন ব্যর্থ লেনদেনের টাকা ফেরতের আবেদন",
                    subjectEn = "Application for refund of failed transaction amount",
                    field1LabelBn = "অ্যাকাউন্ট নম্বর / Account No",
                    field1LabelEn = "Account Number",
                    field2LabelBn = "লেনদেনের তারিখ, সময় ও টাকার পরিমাণ",
                    field2LabelEn = "Txn Date, Time, Amount & Txn ID",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। অ্যাকাউন্ট নং ${f1.ifBlank { "................" }}। গত ${f2.ifBlank { "তারিখ" }} লেনদেন করার সময় টাকা না পাওয়া সত্ত্বেও অ্যাকাউন্ট থেকে টাকা কেটে নেওয়া হয়েছে। অনুগ্রহ করে ব্যর্থ লেনদেনটি তদন্ত করে টাকা আমার অ্যাকাউন্টে ফেরত দেওয়ার ব্যবস্থা করুন।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I hold Account No: ${f1.ifBlank { "................" }}. On ${f2.ifBlank { "Txn Date & Amount" }}, an ATM/Online transaction failed, yet the amount was debited from my account. I request you to investigate and reverse the failed transaction at the earliest."
                    }
                ),
                LetterTypeItem(
                    id = "bank_kyc",
                    titleBn = "KYC আপডেটের আবেদন",
                    titleEn = "KYC / Re-KYC Update",
                    subjectBn = "অ্যাকাউন্টে পুনরায় কেওয়াইসি (Re-KYC) আপডেটের আবেদন",
                    subjectEn = "Application for updating Re-KYC documents in account",
                    field1LabelBn = "অ্যাকাউন্ট নম্বর / Account No",
                    field1LabelEn = "Account Number",
                    field2LabelBn = "আধার ও প্যান নম্বর / Aadhaar & PAN",
                    field2LabelEn = "Aadhaar & PAN Number",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। আমার অ্যাকাউন্ট নং ${f1.ifBlank { "................" }}। দীর্ঘদিন কেওয়াইসি জমা না দেওয়ায় অ্যাকাউন্টটি লেনদেনের জন্য আটকে গেছে। আমি আমার বৈধ পরিচয়পত্র (নম্বর: ${f2.ifBlank { "................" }}) দাখিল করছি। অ্যাকাউন্টটি দ্রুত পুনরায় সচল করার অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I maintain Account No: ${f1.ifBlank { "................" }}. Due to pending KYC compliance, account operations are restricted. I am enclosing self-attested copies of my Aadhaar & PAN (${f2.ifBlank { "................" }}). Kindly update my KYC and regularize account operations."
                    }
                ),
                LetterTypeItem(
                    id = "bank_nominee",
                    titleBn = "নমিনি পরিবর্তনের আবেদন",
                    titleEn = "Nomination Update / Change",
                    subjectBn = "ব্যাংক অ্যাকাউন্টে নতুন নমিনি অন্তর্ভুক্তকরণের আবেদন",
                    subjectEn = "Application for adding / updating nominee in bank account",
                    field1LabelBn = "অ্যাকাউন্ট নম্বর / Account No",
                    field1LabelEn = "Account Number",
                    field2LabelBn = "নমিনির নাম ও সম্পর্ক (যেমন: স্ত্রী / পুত্র)",
                    field2LabelEn = "Nominee Name & Relationship",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। আমার অ্যাকাউন্ট নং ${f1.ifBlank { "................" }}। আমি উক্ত অ্যাকাউন্টের নমিনি হিসেবে ${f2.ifBlank { "নাম ও সম্পর্ক" }}-কে মনোনীত করতে ইচ্ছুক। প্রয়োজনীয় নমিনেশন ফরম পূরণপূর্বক জমা দিলাম। অনুগ্রহ করে নমিনির নাম নথিবদ্ধ করার আবেদন জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I hold Account No: ${f1.ifBlank { "................" }}. I wish to register ${f2.ifBlank { "Nominee Name & Relationship" }} as the legal nominee for my account. Filled Form DA-1 is submitted herewith. Please update the nomination records."
                    }
                ),
                LetterTypeItem(
                    id = "bank_block_atm",
                    titleBn = "ATM কার্ড ব্লকের আবেদন",
                    titleEn = "Block Lost ATM Card",
                    subjectBn = "হারানো/চুরি হওয়া এটিএম কার্ড জরুরি ব্লকের আবেদন",
                    subjectEn = "Application for urgent blocking of lost / stolen ATM Card",
                    field1LabelBn = "অ্যাকাউন্ট নম্বর / Account No",
                    field1LabelEn = "Account Number",
                    field2LabelBn = "কার্ডের শেষ ৪ ডিজিট ও মোবাইল নম্বর",
                    field2LabelEn = "Last 4 Digits & Mobile No",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। অ্যাকাউন্ট নং ${f1.ifBlank { "................" }}। আমার এটিএম কার্ডটি (নম্বর/তথ্য: ${f2.ifBlank { "তথ্য" }}) দুর্ভাগ্যবশত হারিয়ে গেছে। কোনো প্রকার আর্থিক জালিয়াতি রোধে কার্ডটি অবিলম্বে ব্লক করে দেওয়ার বিনীত অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, hold Account No: ${f1.ifBlank { "................" }}. My debit card (Last digits/Ref: ${f2.ifBlank { "Ref info" }}) was unfortunately lost/misplaced today. To prevent unauthorized transactions, kindly block the card immediately."
                    }
                ),
                LetterTypeItem(
                    id = "bank_charge_refund",
                    titleBn = "ব্যাংক চার্জ ফেরতের আবেদন",
                    titleEn = "Refund of Inadmissible Bank Charges",
                    subjectBn = "ভুলবশত কেটে নেওয়া ব্যাংক চার্জ ফেরতের আবেদন",
                    subjectEn = "Application for waiver / refund of bank service charges",
                    field1LabelBn = "অ্যাকাউন্ট নম্বর / Account No",
                    field1LabelEn = "Account Number",
                    field2LabelBn = "কর্তিত চার্জের পরিমাণ ও তারিখ",
                    field2LabelEn = "Deducted Amount & Date",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। অ্যাকাউন্ট নং ${f1.ifBlank { "................" }}। বিগত ${f2.ifBlank { "তারিখে" }} আমার অ্যাকাউন্ট থেকে অপ্রত্যাশিতভাবে অতিরিক্ত চার্জ বাবদ টাকা কেটে নেওয়া হয়েছে। উক্ত চার্জটি মানবিক দিক বিবেচনায় মকুব করে আমার অ্যাকাউন্টে রিফান্ড করার অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I maintain Account No: ${f1.ifBlank { "................" }}. On ${f2.ifBlank { "Date & Amount" }}, an unexpected penalty/service charge was debited from my account. Considering my regular banking relationship, I earnestly request you to waive and reverse the charged amount."
                    }
                )
            )
        ),

        // 2. স্কুল ও কলেজ (School & College)
        LetterCategory(
            id = "school",
            titleBn = "২. স্কুল ও কলেজ (School & College)",
            titleEn = "2. School & College",
            defaultRecipientBn = "প্রধান শিক্ষক / অধ্যক্ষ মহাশয় (Headmaster / Principal)",
            defaultRecipientEn = "The Headmaster / Principal",
            letterTypes = listOf(
                LetterTypeItem(
                    id = "school_leave",
                    titleBn = "ছুটির আবেদন (Leave Application)",
                    titleEn = "Leave of Absence",
                    subjectBn = "অনুপস্থিতির জন্য ছুটির আবেদন",
                    subjectEn = "Application for grant of leave of absence",
                    field1LabelBn = "শ্রেণি ও বিভাগ / Class & Sec",
                    field1LabelEn = "Class & Section",
                    field2LabelBn = "রোল নম্বর ও ছুটির দিনসংখ্যা",
                    field2LabelEn = "Roll No & Days of absence",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি আপনার বিদ্যালয়ের ${f1.ifBlank { "........" }} শ্রেণির ছাত্র/ছাত্রী ${name.ifBlank { "................" }}, রোল নং: ${f2.ifBlank { "...." }}। অসুস্থতা / পারিবারিক অনিবার্য কারণে আমি বিদ্যালয়ে উপস্থিত হতে পারিনি। অতএব, অনুগ্রহ করে উক্ত দিনগুলির ছুটি মঞ্জুর করে বাধিত করবেন।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "Respected Sir/Madam, I am ${name.ifBlank { "................" }}, student of Class: ${f1.ifBlank { "........" }}, Roll No: ${f2.ifBlank { "...." }}. Owing to illness / unavoidable family circumstances, I was unable to attend classes. I kindly request you to grant me leave of absence."
                    }
                ),
                LetterTypeItem(
                    id = "school_tc",
                    titleBn = "ট্রান্সফার সার্টিফিকেট (TC)",
                    titleEn = "Transfer Certificate (TC)",
                    subjectBn = "বিদ্যালয় ছাড়ার ছাড়পত্র (TC) প্রাপ্তির আবেদন",
                    subjectEn = "Application for issuance of Transfer Certificate",
                    field1LabelBn = "শ্রেণি ও রোল / Class & Roll",
                    field1LabelEn = "Class & Roll No",
                    field2LabelBn = "টিসি নেওয়ার কারণ (যেমন: পিতার বদলি)",
                    field2LabelEn = "Reason for Transfer",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "বিনীত নিবেদন এই যে, আমি আপনার বিদ্যালয়ের ${f1.ifBlank { "........" }} শ্রেণির ছাত্র/ছাত্রী ${name.ifBlank { "................" }}। আমার অভিভাবকের বাসস্থান পরিবর্তনের কারণে (${f2.ifBlank { "অন্যত্র বাসস্থান পরিবর্তন" }}) আমাকে নতুন বিদ্যালয়ে ভর্তি হতে হবে। অতএব, আমাকে একটি ট্রান্সফার সার্টিফিকেট প্রদান করার অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "With due respect, I state that I am ${name.ifBlank { "................" }}, studying in Class: ${f1.ifBlank { "........" }}. Due to family relocation (${f2.ifBlank { "Relocation" }}), I need to enroll in another institution. Kindly issue my Transfer Certificate at an early date."
                    }
                ),
                LetterTypeItem(
                    id = "school_character",
                    titleBn = "ক্যারেক্টার সার্টিফিকেট",
                    titleEn = "Character Certificate",
                    subjectBn = "চরিত্র শংসাপত্র (Character Certificate) প্রদানের আবেদন",
                    subjectEn = "Application for issuance of Character / Conduct Certificate",
                    field1LabelBn = "পাসের বছর ও শ্রেণি / Class & Year",
                    field1LabelEn = "Passed Class & Year",
                    field2LabelBn = "সার্টিফিকেট ব্যবহারের উদ্দেশ্য (যেমন: চাকরির পরীক্ষা)",
                    field2LabelEn = "Purpose (e.g. Higher Studies / Job)",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি আপনার বিদ্যালয় থেকে ${f1.ifBlank { "বিগত বছরে" }} কৃতকার্য হয়েছি। উচ্চশিক্ষায় ভর্তি / সরকারি আবেদনের (${f2.ifBlank { "ভর্তি প্রক্রিয়া" }}) জন্য আমার একটি ক্যারেক্টার সার্টিফিকেট প্রয়োজন। অতএব, আমাকে উক্ত প্রশংসাপত্র প্রদান করে বাধিত করবেন।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I beg to submit that I was a bonafide student of your institution in ${f1.ifBlank { "previous batch" }}. For admission to higher studies / job verification (${f2.ifBlank { "Higher Education" }}), I require a Character Certificate. I request you to kindly issue the certificate."
                    }
                ),
                LetterTypeItem(
                    id = "school_fee",
                    titleBn = "ফি মকুবের আবেদন",
                    titleEn = "Fee Concession",
                    subjectBn = "দরিদ্র তহবিল থেকে বিদ্যালয় ফি মকুবের আবেদন",
                    subjectEn = "Application for full / half school fee concession",
                    field1LabelBn = "শ্রেণি ও রোল / Class & Roll",
                    field1LabelEn = "Class & Roll No",
                    field2LabelBn = "অভিভাবকের মাসিক আয় / Family Income",
                    field2LabelEn = "Guardian Monthly Income",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, আপনার বিদ্যালয়ের ${f1.ifBlank { "........" }} শ্রেণির শিক্ষার্থী। আমার পরিবারের মাসিক আয় মাত্র ${f2.ifBlank { "অত্যন্ত সীমিত" }} টাকা হওয়ায় বিদ্যালয়ের ফি নিয়মিত পরিশোধ করা অসম্ভব হয়ে পড়েছে। অতএব, মানবিক বিবেচনায় আমার ফি মকুবের বিনীত প্রার্থনা জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I am ${name.ifBlank { "................" }}, student of Class: ${f1.ifBlank { "........" }}. Due to severe financial hardship in our family (Monthly income: ${f2.ifBlank { "limited" }}), my guardian cannot afford the fees. I earnestly request you to grant me concession in school fees."
                    }
                ),
                LetterTypeItem(
                    id = "school_admit_rectify",
                    titleBn = "মার্কশিট বা অ্যাডমিট কার্ড সংশোধন",
                    titleEn = "Admit / Marksheet Correction",
                    subjectBn = "অ্যাডমিট কার্ড / মার্কশিটে নামের ভুল সংশোধনের আবেদন",
                    subjectEn = "Application for rectification of error in Admit Card / Marksheet",
                    field1LabelBn = "রোল নম্বর ও রেজিস্ট্রেশন নম্বর",
                    field1LabelEn = "Roll No & Registration No",
                    field2LabelBn = "সঠিক বানান ও তথ্য",
                    field2LabelEn = "Correct Details & Proof",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। রোল ও রেজিস্ট্রেশন নং: ${f1.ifBlank { "................" }}। আমার প্রকাশিত অ্যাডমিট কার্ড / মার্কশিটে নামের বানানে ভুল লিপিবদ্ধ হয়েছে। সঠিক তথ্য: ${f2.ifBlank { name }}। সংশোধনের জন্য প্রয়োজনীয় নথিপত্র সংযুক্ত করলাম।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I state that I am ${name.ifBlank { "................" }}, Roll & Reg No: ${f1.ifBlank { "................" }}. There is an inadvertent error in my name in the issued Marksheet/Admit Card. The correct information is: ${f2.ifBlank { name }}. Kindly initiate correction with the Board."
                    }
                ),
                LetterTypeItem(
                    id = "school_recheck",
                    titleBn = "পুনরায় খাতা মূল্যায়নের আবেদন",
                    titleEn = "Answer Script Re-evaluation",
                    subjectBn = "পরীক্ষার উত্তরপত্র পুনরায় মূল্যায়নের (Re-check) আবেদন",
                    subjectEn = "Application for re-evaluation / scrutiny of answer scripts",
                    field1LabelBn = "রোল নম্বর ও পরীক্ষার নাম",
                    field1LabelEn = "Roll No & Exam Name",
                    field2LabelBn = "বিষয়ের নাম ও প্রাপ্ত নম্বর",
                    field2LabelEn = "Subject & Awarded Marks",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, রোল নং ${f1.ifBlank { "................" }}। সম্প্রতি প্রকাশিত ফলাফলে আমার ${f2.ifBlank { "নির্দিষ্ট বিষয়ে" }} প্রত্যাশার চেয়ে অনেক কম নম্বর এসেছে। আমি নিশ্চিত উত্তরপত্রটি পুনর্বিবেচনা করলে নম্বর বৃদ্ধি পাবে। অতএব, উত্তরপত্রটি স্ক্রুটিনি করার প্রার্থনা জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I appeared for the examination under Roll No: ${f1.ifBlank { "................" }}. In the subject ${f2.ifBlank { "Subject details" }}, the awarded marks are significantly lower than expected. I request you to kindly permit post-publication scrutiny/re-evaluation of my paper."
                    }
                )
            )
        ),

        // 3. বিদ্যুৎ দপ্তর (Electricity / WBSEDCL)
        LetterCategory(
            id = "electricity",
            titleBn = "৩. বিদ্যুৎ দপ্তর (Electricity / WBSEDCL)",
            titleEn = "3. Electricity / WBSEDCL",
            defaultRecipientBn = "স্টেশন ম্যানেজার (Station Manager / AE, WBSEDCL)",
            defaultRecipientEn = "The Station Manager / AE, WBSEDCL",
            letterTypes = listOf(
                LetterTypeItem(
                    id = "elec_new_meter",
                    titleBn = "নতুন বিদ্যুৎ সংযোগের আবেদন",
                    titleEn = "New Electric Meter Connection",
                    subjectBn = "নতুন গার্হস্থ্য বিদ্যুৎ মিটার সংযোগের জন্য আবেদন",
                    subjectEn = "Application for new domestic electric meter connection",
                    field1LabelBn = "আবেদন / কোটেশন নম্বর (ঐচ্ছিক)",
                    field1LabelEn = "Application / Quotation No",
                    field2LabelBn = "মোবাইল নম্বর ও সংযোগের স্থান",
                    field2LabelEn = "Mobile No & Premise Location",
                    bodyTemplateBn = { name, addr, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}। আমার নতুন নির্মিত বাসস্থানে নতুন বিদ্যুৎ সংযোগের প্রয়োজন। আমি সমস্ত প্রয়োজনীয় শর্তাবলী ও নিয়ম মানতে প্রস্তুত (রেফারেন্স নং: ${f1.ifBlank { "প্রযোজ্য নয়" }}), যোগাযোগ: ${f2.ifBlank { "................" }}। অবিলম্বে মিটার সংযোগের ব্যবস্থা করার অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, addr, _, f1, f2 ->
                        "I am ${name.ifBlank { "................" }}, resident of ${addr.ifBlank { "................" }}. I require a new domestic electric meter connection at my premises (Ref: ${f1.ifBlank { "N/A" }}). Contact: ${f2.ifBlank { "................" }}. I request you to kindly sanction and install the meter."
                    }
                ),
                LetterTypeItem(
                    id = "elec_bill_correct",
                    titleBn = "ভুল বা অতিরিক্ত বিল সংশোধনের আবেদন",
                    titleEn = "Inflated Electric Bill Correction",
                    subjectBn = "অস্বাভাবিক অতিরিক্ত বিদ্যুৎ বিল সংশোধনের আবেদন",
                    subjectEn = "Application for correction of inflated electric bill",
                    field1LabelBn = "কনজিউমার আইডি / Consumer ID",
                    field1LabelEn = "Consumer ID",
                    field2LabelBn = "মিটার নম্বর ও অতিরিক্ত বিলের পরিমাণ",
                    field2LabelEn = "Meter No & Disputed Bill Amount",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। আমার বিদ্যুৎ কনজিউমার আইডি নং ${f1.ifBlank { "................" }} এবং মিটার নং: ${f2.ifBlank { "................" }}। চলতি মাসে আমার স্বাভাবিক ব্যবহারের তুলনায় অস্বাভাবিক অতিরিক্ত বিল পাঠানো হয়েছে। অনুগ্রহ করে মিটারটি পরীক্ষা করে সঠিক বিল প্রস্তুত করার অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I hold Consumer ID: ${f1.ifBlank { "................" }} and Meter No: ${f2.ifBlank { "................" }}. For the current billing period, an abnormally high bill has been generated contrary to my regular consumption. Please inspect the meter and revise the bill."
                    }
                ),
                LetterTypeItem(
                    id = "elec_meter_replace",
                    titleBn = "মিটার পরিবর্তন বা মেরামতের আবেদন",
                    titleEn = "Defective Meter Replacement",
                    subjectBn = "বিকল / বন্ধ বিদ্যুৎ মিটার পরিবর্তনের আবেদন",
                    subjectEn = "Application for replacement of defective / burnt electric meter",
                    field1LabelBn = "কনজিউমার আইডি / Consumer ID",
                    field1LabelEn = "Consumer ID",
                    field2LabelBn = "মিটার নম্বর ও সমস্যা (যেমন: ডিসপ্লে বন্ধ)",
                    field2LabelEn = "Meter No & Nature of defect",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। আমার কনজিউমার আইডি ${f1.ifBlank { "................" }} এবং মিটার নম্বর: ${f2.ifBlank { "................" }}। বেশ কিছুদিন যাবৎ মিটারটি সম্পূর্ণ বিকল হয়ে বন্ধ রয়েছে এবং ডিসপ্লেতে কোনো রিডিং প্রদর্শিত হচ্ছে না। অতএব, দ্রুত মিটারটি পরিবর্তন করে দেওয়ার অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, Consumer ID: ${f1.ifBlank { "................" }}, Meter: ${f2.ifBlank { "................" }}, bring to your notice that the electric meter at my premises is burnt/faulty and not recording reading. Kindly arrange for its prompt replacement."
                    }
                ),
                LetterTypeItem(
                    id = "elec_name_change",
                    titleBn = "নাম ও ঠিকানা পরিবর্তনের আবেদন",
                    titleEn = "Ownership / Name Transfer in Meter",
                    subjectBn = "বিদ্যুৎ মিটারে গ্রাহকের নাম পরিবর্তনের আবেদন",
                    subjectEn = "Application for change of consumer name in electric meter",
                    field1LabelBn = "কনজিউমার আইডি / Consumer ID",
                    field1LabelEn = "Consumer ID",
                    field2LabelBn = "বর্তমান নাম ও নতুন আবেদনকারীর নাম",
                    field2LabelEn = "Existing Name & New Owner Name",
                    bodyTemplateBn = { name, addr, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}। কনজিউমার আইডি: ${f1.ifBlank { "................" }}। উক্ত মিটারের পূর্ববর্তী গ্রাহকের থেকে সম্পত্তি হস্তান্তরের সূত্রে (${f2.ifBlank { "উত্তরাধিকার/ক্রয়" }}) আমি মিটারের নাম পরিবর্তন করতে ইচ্ছুক। প্রয়োজনীয় দলিল সহ নাম পরিবর্তনের আবেদন জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, addr, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, residing at ${addr.ifBlank { "................" }}, Consumer ID: ${f1.ifBlank { "................" }}, have acquired ownership of the premise (${f2.ifBlank { "Transfer Details" }}). I request you to kindly effect change of consumer name in the electric records."
                    }
                ),
                LetterTypeItem(
                    id = "elec_load_change",
                    titleBn = "মিটার লোড বৃদ্ধি বা হ্রাসের আবেদন",
                    titleEn = "Connected Load Enhancement / Reduction",
                    subjectBn = "বিদ্যুৎ মিটারের অনুমোদিত লোড পরিবর্তনের আবেদন",
                    subjectEn = "Application for enhancement / reduction of sanctioned electric load",
                    field1LabelBn = "কনজিউমার আইডি / Consumer ID",
                    field1LabelEn = "Consumer ID",
                    field2LabelBn = "বর্তমান লোড ও প্রস্তাবিত লোড (যেমন: 1KW থেকে 3KW)",
                    field2LabelEn = "Existing & Proposed Load (e.g. 1KW to 3KW)",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। কনজিউমার আইডি: ${f1.ifBlank { "................" }}। আমার বাসস্থানে বৈদ্যুতিক যন্ত্রপাতির ব্যবহার বৃদ্ধি/হ্রাসের কারণে অনুমোদিত লোড (${f2.ifBlank { "লোড বিবরণ" }}) পরিবর্তন করা প্রয়োজন। প্রয়োজনীয় ফি সহ লোড পরিবর্তনের আবেদন মঞ্জুর করার অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I maintain Consumer ID: ${f1.ifBlank { "................" }}. Owing to modified electrical equipment usage, I request enhancement/reduction of sanctioned load (${f2.ifBlank { "Load details" }}). I am ready to pay requisite security deposit and charges."
                    }
                )
            )
        ),

        // 4. পঞ্চায়েত ও পৌরসভা (Panchayat / Municipality)
        LetterCategory(
            id = "panchayat",
            titleBn = "৪. পঞ্চায়েত ও পৌরসভা (Panchayat / Municipality)",
            titleEn = "4. Panchayat / Municipality",
            defaultRecipientBn = "প্রধান / পৌরপ্রধান মহাশয় (Pradhan / Chairman)",
            defaultRecipientEn = "The Pradhan / Municipal Chairman",
            letterTypes = listOf(
                LetterTypeItem(
                    id = "panch_residential",
                    titleBn = "স্থায়ী বাসিন্দার শংসাপত্রের আবেদন",
                    titleEn = "Residential / Domicile Certificate",
                    subjectBn = "স্থায়ী বাসিন্দার শংসাপত্র (Residential Certificate) প্রদানের আবেদন",
                    subjectEn = "Application for issuance of Residential / Domicile Certificate",
                    field1LabelBn = "গ্রাম / ওয়ার্ড ও পিতা/স্বামীর নাম",
                    field1LabelEn = "Village / Ward & Father/Husband Name",
                    field2LabelBn = "আধার ও ভোটার নম্বর",
                    field2LabelEn = "Aadhaar & Voter Card No",
                    bodyTemplateBn = { name, addr, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, পিতা/স্বামী- ${f1.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}। আমি আপনার পঞ্চায়েত/পৌরসভা এলাকার একজন স্থায়ী বাসিন্দা (নথি: ${f2.ifBlank { "................" }})। অফিশিয়াল কাজের জন্য আমার একটি স্থায়ী বাসিন্দার শংসাপত্র প্রয়োজন। দয়া করে শংসাপত্রটি প্রদানের ব্যবস্থা করুন।"
                    },
                    bodyTemplateEn = { name, addr, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, Son/Wife of ${f1.ifBlank { "................" }}, resident of ${addr.ifBlank { "................" }}, am a permanent resident of your jurisdiction. Identification: ${f2.ifBlank { "................" }}. For official submission, I request you to kindly issue a Residential Certificate."
                    }
                ),
                LetterTypeItem(
                    id = "panch_birth_death",
                    titleBn = "জন্ম / মৃত্যু শংসাপত্রের আবেদন",
                    titleEn = "Birth / Death Certificate",
                    subjectBn = "জন্ম / মৃত্যু শংসাপত্র ইস্যু করার আবেদন",
                    subjectEn = "Application for issuance of Birth / Death Certificate",
                    field1LabelBn = "ব্যক্তির নাম ও পিতা/মাতার নাম",
                    field1LabelEn = "Person Name & Parents Name",
                    field2LabelBn = "ঘটনার তারিখ ও স্থান",
                    field2LabelEn = "Date & Place of Occurrence",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। আমার পরিবারের সদস্য ${f1.ifBlank { "সদস্যের নাম" }} গত ${f2.ifBlank { "তারিখে" }} জন্মগ্রহণ/মৃত্যুবরণ করেছেন। হাসপাতাল ও সংশ্লিষ্ট প্রমাণের ভিত্তিতে প্রয়োজনীয় সরকারি নথিতে নাম অন্তর্ভুক্তপূর্বক শংসাপত্রটি প্রদান করার অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I state that I am ${name.ifBlank { "................" }}. My family member, ${f1.ifBlank { "Name" }}, was born/passed away on ${f2.ifBlank { "Date & Place" }}. Supporting medical discharge/burial receipts are attached. Kindly issue the certified Certificate."
                    }
                ),
                LetterTypeItem(
                    id = "panch_trade_lic",
                    titleBn = "ট্রেড লাইসেন্স আবেদন বা নবায়ন",
                    titleEn = "Trade License Issue / Renewal",
                    subjectBn = "নতুন ট্রেড লাইসেন্স প্রদান / নবায়নের আবেদন",
                    subjectEn = "Application for issuance / renewal of Trade License",
                    field1LabelBn = "ব্যবসা প্রতিষ্ঠানের নাম ও ধরন",
                    field1LabelEn = "Business Name & Nature of Trade",
                    field2LabelBn = "দোকানের ঠিকানা ও মোবাইল নম্বর",
                    field2LabelEn = "Shop Address & Contact No",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। আপনার এলাকায় '${f1.ifBlank { "ব্যবসা প্রতিষ্ঠান" }}' নামক ব্যবসা পরিচালনা করছি (ঠিকানা: ${f2.ifBlank { "................" }})। বিধিসম্মতভাবে ব্যবসা পরিচালনার জন্য ট্রেড লাইসেন্স প্রয়োজন। সরকারি ফি সহ লাইসেন্স প্রদানের অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I am ${name.ifBlank { "................" }}, proprietor of '${f1.ifBlank { "Business Name" }}', situated at ${f2.ifBlank { "Address" }}. I request you to kindly issue / renew Trade License for my enterprise upon acceptance of requisite fees."
                    }
                ),
                LetterTypeItem(
                    id = "panch_road_drain",
                    titleBn = "রাস্তা বা ড্রেন মেরামতের আবেদন",
                    titleEn = "Repair of Road / Drainage",
                    subjectBn = "এলাকার ক্ষতিগ্রস্ত রাস্তা ও নিকাশি নালা মেরামতের আবেদন",
                    subjectEn = "Application for urgent repair of public road and drainage",
                    field1LabelBn = "এলাকা / পাড়া ও ওয়ার্ডের নাম",
                    field1LabelEn = "Locality / Para & Ward No",
                    field2LabelBn = "সমস্যার বিবরণ (যেমন: খানাখন্দ / জল জমা)",
                    field2LabelEn = "Problem Details (e.g. Waterlogging)",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমরা ${f1.ifBlank { "গ্রাম/ওয়ার্ডের" }} স্থানীয় বাসিন্দাবৃন্দ। এলাকার প্রধান রাস্তা ও ড্রেনটি (${f2.ifBlank { "জলমগ্ন ও বিপজ্জনক" }}) অবস্থায় রয়েছে, যার ফলে জনসাধারণের যাতায়াতে চরম দুর্ভোগ সৃষ্টি হচ্ছে। অতএব, রাস্তাটি জরুরি ভিত্তিতে সংস্কার করার বিনীত অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "On behalf of the residents of ${f1.ifBlank { "Locality/Ward" }}, I bring to your notice the dilapidated state of the road and drainage (${f2.ifBlank { "Severe waterlogging" }}). We earnestly request urgent repair and renovation of the civic infrastructure."
                    }
                ),
                LetterTypeItem(
                    id = "panch_warisan",
                    titleBn = "পারিবারিক ওয়ারিশান সার্টিফিকেট",
                    titleEn = "Legal Heir / Warisan Certificate",
                    subjectBn = "পারিবারিক ওয়ারিশান (Legal Heir) সার্টিফিকেট প্রদানের আবেদন",
                    subjectEn = "Application for issuance of Legal Heir / Warisan Certificate",
                    field1LabelBn = "মৃত ব্যক্তির নাম ও মৃত্যুর তারিখ",
                    field1LabelEn = "Deceased Name & Date of Death",
                    field2LabelBn = "ওয়ারিশদের নাম ও সংখ্যা",
                    field2LabelEn = "List / Count of Surviving Heirs",
                    bodyTemplateBn = { name, addr, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}। আমার স্বর্গীয় অভিভাবক ${f1.ifBlank { "মৃত ব্যক্তির নাম" }} পরলোকগমন করেছেন। তাঁর বৈধ ওয়ারিশগণ হলেন: ${f2.ifBlank { "ওয়ারিশদের তালিকা" }}। প্রয়োজনীয় তদন্ত সাপেক্ষে একটি পারিবারিক ওয়ারিশান শংসাপত্র প্রদান করার অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, addr, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, resident of ${addr.ifBlank { "................" }}, state that my late guardian, ${f1.ifBlank { "Deceased Name" }}, passed away on ${f1.ifBlank { "Date" }}. The surviving legal heirs are: ${f2.ifBlank { "Heirs details" }}. Kindly issue the official Legal Heir Certificate."
                    }
                ),
                LetterTypeItem(
                    id = "panch_tax_mutation",
                    titleBn = "হোল্ডিং ট্যাক্স বা মিউটেশন আবেদন",
                    titleEn = "Holding Tax / Municipal Mutation",
                    subjectBn = "পৌরসভা/পঞ্চায়েতে হোল্ডিং নামপত্তন (Mutation) আবেদন",
                    subjectEn = "Application for assessment / mutation of Holding Tax",
                    field1LabelBn = "হোল্ডিং নম্বর ও ওয়ার্ড নম্বর",
                    field1LabelEn = "Holding No & Ward No",
                    field2LabelBn = "দলিল নম্বর ও জমির খতিয়ান",
                    field2LabelEn = "Deed No & Property Reference",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। হোল্ডিং নং: ${f1.ifBlank { "................" }}। রেজিস্ট্রিকৃত দলিল নং: ${f2.ifBlank { "দলিল তথ্য" }} মূলে সম্পত্তি অর্জন করেছি। পৌর/পঞ্চায়েত নথিতে আমার নামে হোল্ডিং ট্যাক্স নির্ধারণ ও মিউটেশন করার প্রার্থনা জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, have purchased property covered under Holding No: ${f1.ifBlank { "Holding No" }} via registered Deed: ${f2.ifBlank { "Deed Ref" }}. I request you to mutate the holding record in my name and issue the revised tax assessment."
                    }
                )
            )
        ),

        // 5. থানা ও পুলিশ (Police / GD / FIR)
        LetterCategory(
            id = "police",
            titleBn = "৫. থানা ও পুলিশ (Police / GD / FIR)",
            titleEn = "5. Police / GD / FIR",
            defaultRecipientBn = "ভারপ্রাপ্ত আধিকারিক (Officer-in-Charge / Inspector)",
            defaultRecipientEn = "The Officer-in-Charge / Inspector of Police",
            letterTypes = listOf(
                LetterTypeItem(
                    id = "police_gd",
                    titleBn = "নথিপত্র বা জিনিসপত্র হারানোর জিডি (GD)",
                    titleEn = "General Diary (GD) for Lost Articles",
                    subjectBn = "প্রয়োজনীয় নথিপত্র/জিনিসপত্র হারানোর সাধারণ ডায়েরি (GD)",
                    subjectEn = "General Diary regarding loss of valuable document / article",
                    field1LabelBn = "হারানো জিনিস (যেমন: আধার / মাধ্যমিক অ্যাডমিট / মোবাইল)",
                    field1LabelEn = "Lost Item (e.g. Aadhaar / Phone / Docs)",
                    field2LabelBn = "হারানোর স্থান, তারিখ ও আনুমানিক সময়",
                    field2LabelEn = "Location, Date & Time of loss",
                    bodyTemplateBn = { name, addr, _, f1, f2 ->
                        "মহাশয়, আমি ${name.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}। বিগত ${f2.ifBlank { "তারিখে" }} যাতায়াতকালে অসাবধানতাবশত আমার ${f1.ifBlank { "নথিপত্র/মোবাইল" }} হারিয়ে গেছে। অনেক খোঁজাখুঁজি করেও পাওয়া যায়নি। ভবিষ্যতে কোনো অপব্যবহার রোধে বিষয়টি আপনার থানায় সাধারণ ডায়েরি (GD) হিসেবে নথিবদ্ধ করার অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, addr, _, f1, f2 ->
                        "Sir, I, ${name.ifBlank { "................" }}, residing at ${addr.ifBlank { "................" }}, state that on ${f2.ifBlank { "Date & Time" }}, my valuable item, namely ${f1.ifBlank { "Lost Documents / Phone" }}, was accidentally lost while travelling. Please register a General Diary (GD) entry for the same."
                    }
                ),
                LetterTypeItem(
                    id = "police_fir",
                    titleBn = "চুরির অভিযোগ (FIR)",
                    titleEn = "First Information Report (FIR) - Theft",
                    subjectBn = "চুরির ঘটনার বিরুদ্ধে এফআইআর (FIR) দায়েরের আবেদন",
                    subjectEn = "First Information Report (FIR) regarding burglary / theft",
                    field1LabelBn = "চুরি হওয়া সামগ্রীর বিবরণ ও আনুমানিক মূল্য",
                    field1LabelEn = "Stolen Items Description & Value",
                    field2LabelBn = "ঘটনার স্থান, তারিখ ও সন্দেহভাজনের তথ্য",
                    field2LabelEn = "Incident Date, Location & Suspect info",
                    bodyTemplateBn = { name, addr, _, f1, f2 ->
                        "মহাশয়, আমি ${name.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}। অত্যন্ত দুঃখের সাথে জানাচ্ছি যে, গত ${f2.ifBlank { "তারিখে" }} অজ্ঞাতপরিচয় দুষ্কৃতীরা আমার বাসস্থান/দোকান থেকে ${f1.ifBlank { "সামগ্রীর বিবরণ" }} চুরি করে নিয়ে গেছে। অবিলম্বে আইনানুগ এফআইআর (FIR) রুজু করে তদন্তের ব্যবস্থা করার বিনীত প্রার্থনা জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, addr, _, f1, f2 ->
                        "Sir, I lodge this complaint stating that on ${f2.ifBlank { "Date & Location" }}, unknown miscreants committed burglary and stole ${f1.ifBlank { "Items and Value" }}. I request you to register a formal FIR and initiate immediate investigation to recover the stolen goods."
                    }
                ),
                LetterTypeItem(
                    id = "police_harassment",
                    titleBn = "সামাজিক হয়রানি বা হুমকির অভিযোগ",
                    titleEn = "Complaint on Harassment / Threat",
                    subjectBn = "অসামাজিক হুমকি ও মানসিক হয়রানির বিরুদ্ধে অভিযোগ",
                    subjectEn = "Complaint regarding criminal intimidation and harassment",
                    field1LabelBn = "অভিযুক্তের নাম ও পরিচয় (যদি জানা থাকে)",
                    field1LabelEn = "Accused Name & Address (if known)",
                    field2LabelBn = "ঘটনার তারিখ, স্থান ও হুমকির বিবরণ",
                    field2LabelEn = "Date, Place & Nature of Threat",
                    bodyTemplateBn = { name, addr, _, f1, f2 ->
                        "মহাশয়, আমি ${name.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}। অভিযুক্ত ${f1.ifBlank { "নাম/অজ্ঞাত" }} গত ${f2.ifBlank { "তারিখ" }} আমাকে অশালীন ভাষায় গালিগালাজ এবং প্রাণনাশের হুমকি প্রদর্শন করেছে। আমি ও আমার পরিবার চরম নিরাপত্তাহীনতায় ভুগছি। আইনানুগ ব্যবস্থা গ্রহণ করে আমাদের নিরাপত্তা সুনিশ্চিত করার আবেদন জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, addr, _, f1, f2 ->
                        "Sir, I bring to your notice that ${f1.ifBlank { "Accused Person" }} has subjected me to criminal intimidation and mental harassment on ${f2.ifBlank { "Date & Details" }}. I fear for the safety of my family and request you to take stern legal action against the perpetrator."
                    }
                ),
                LetterTypeItem(
                    id = "police_verification",
                    titleBn = "পুলিশ ভেরিফিকেশন সার্টিফিকেট আবেদন",
                    titleEn = "Police Verification Certificate (PCC)",
                    subjectBn = "চাকরি/পাসপোর্টের জন্য পুলিশ ভেরিফিকেশন শংসাপত্রের আবেদন",
                    subjectEn = "Application for Police Clearance Certificate (PCC)",
                    field1LabelBn = "আবেদনের রেফারেন্স নম্বর ও বিভাগ",
                    field1LabelEn = "Application Ref No & Department",
                    field2LabelBn = "আধার ও ভোটার নম্বর",
                    field2LabelEn = "Aadhaar & Voter Card No",
                    bodyTemplateBn = { name, addr, _, f1, f2 ->
                        "মহাশয়, আমি ${name.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}। আমার সরকারি চাকরির নিয়োগ / পাসপোর্টের জন্য (রেফারেন্স: ${f1.ifBlank { "................" }}) পুলিশ ভেরিফিকেশন প্রতিবেদন প্রয়োজন (নথি: ${f2.ifBlank { "................" }})। আমার বিরুদ্ধে কোনো ফৌজদারি অভিযোগ নেই। শংসাপত্রটি দ্রুত প্রদান করার অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, addr, _, f1, f2 ->
                        "I am ${name.ifBlank { "................" }}, resident of ${addr.ifBlank { "................" }}. For my employment/passport clearance (Ref: ${f1.ifBlank { "Ref No" }}), a Police Verification Report is mandated. Identification: ${f2.ifBlank { "Aadhaar/Voter" }}. Kindly conduct inquiry and issue the clearance."
                    }
                )
            )
        ),

        // 6. সরকারি দপ্তর ও বিডিও (Govt Office / BDO)
        LetterCategory(
            id = "govt_bdo",
            titleBn = "৬. সরকারি দপ্তর ও বিডিও (Govt Office / BDO)",
            titleEn = "6. Govt Office / BDO",
            defaultRecipientBn = "ব্লক উন্নয়ন আধিকারিক (Block Development Officer - BDO)",
            defaultRecipientEn = "The Block Development Officer (BDO)",
            letterTypes = listOf(
                LetterTypeItem(
                    id = "bdo_scheme",
                    titleBn = "সরকারি প্রকল্পে নাম নথিভুক্তকরণ",
                    titleEn = "Govt Welfare Scheme Enrollment",
                    subjectBn = "সরকারি কল্যাণমূলক প্রকল্পে নাম অন্তর্ভুক্তির আবেদন",
                    subjectEn = "Application for enrollment under Government Welfare Scheme",
                    field1LabelBn = "প্রকল্পের নাম (যেমন: আবাস যোজনা / লক্ষ্মীর ভাণ্ডার)",
                    field1LabelEn = "Scheme Name (e.g. Awas / Lakshmir Bhandar)",
                    field2LabelBn = "আধার ও পারিবারিক আয়ের তথ্য",
                    field2LabelEn = "Aadhaar No & Family Income",
                    bodyTemplateBn = { name, addr, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}। আমি অত্যন্ত দরিদ্র পরিবারের সদস্য এবং সরকারি '${f1.ifBlank { "সরকারি প্রকল্প" }}' প্রকল্পের সমস্ত শর্ত পূরণ করি। তথ্যসূত্র: ${f2.ifBlank { "আধার ও আয়" }}। অতএব, মানবিক বিবেচনায় উক্ত প্রকল্পে আমার নাম অন্তর্ভুক্ত করার অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, addr, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, residing at ${addr.ifBlank { "................" }}, belong to an economically weaker section and fulfill all criteria for '${f1.ifBlank { "Welfare Scheme" }}'. My details: ${f2.ifBlank { "Aadhaar & Income" }}. Kindly register my candidature for the scheme."
                    }
                ),
                LetterTypeItem(
                    id = "bdo_rti",
                    titleBn = "তথ্য জানার অধিকার (RTI) আবেদন",
                    titleEn = "Right to Information (RTI)",
                    subjectBn = "RTI Act 2005 অনুযায়ী তথ্যের আবেদন (Application under RTI Act)",
                    subjectEn = "Application for obtaining information under Section 6(1) of RTI Act 2005",
                    field1LabelBn = "বিষয়বস্তু / প্রকল্প সংক্রান্ত তথ্য",
                    field1LabelEn = "Subject / Matter of Inquiry",
                    field2LabelBn = "নির্দিষ্ট সময়কাল বা ফাইলের তথ্য",
                    field2LabelEn = "Specific Period / Reference File",
                    bodyTemplateBn = { name, addr, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}। তথ্য জানার অধিকার আইন, ২০০৫ (RTI Act 2005)-এর ৬(১) ধারা অনুযায়ী আমি '${f1.ifBlank { "বিষয়ের বিবরণ" }}' সংক্রান্ত তথ্য (${f2.ifBlank { "সময়কাল" }}) লিখিতভাবে প্রত্যয়িত কপি আকারে পেতে ইচ্ছুক। বিধিসম্মত ফি সহ আবেদন দাখিল করছি।"
                    },
                    bodyTemplateEn = { name, addr, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, address: ${addr.ifBlank { "................" }}, seek certified information under Section 6(1) of RTI Act, 2005 regarding: '${f1.ifBlank { "Subject" }}' for the period: ${f2.ifBlank { "Period" }}. Statutory application fee is enclosed."
                    }
                ),
                LetterTypeItem(
                    id = "bdo_grant",
                    titleBn = "সরকারি আর্থিক অনুদান বা সহায়তার আবেদন",
                    titleEn = "Govt Financial Aid / Relief",
                    subjectBn = "প্রাকৃতিক দুর্যোগ / চিকিৎসার জন্য আর্থিক সহায়তার আবেদন",
                    subjectEn = "Application for emergency financial relief / medical grant",
                    field1LabelBn = "সহায়তার উদ্দেশ্য (যেমন: দুর্যোগে ক্ষতি / দুরারোগ্য ব্যাধি)",
                    field1LabelEn = "Purpose of Grant (e.g. Disaster / Disease)",
                    field2LabelBn = "ক্ষতির পরিমাণ বা চিকিৎসকের সার্টিফিকেট",
                    field2LabelEn = "Damage assessment / Doctor certificate",
                    bodyTemplateBn = { name, addr, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}। আকস্মিক দুর্যোগ / পারিবারিক সংকট (${f1.ifBlank { "সংকটের বিবরণ" }})-এর কারণে চরম আর্থিক ক্ষতির সম্মুখীন হয়েছি (প্রমাণ: ${f2.ifBlank { "সংযুক্তি" }})। মাননীয় বিডিও মহাশয়ের তহবিল থেকে জরুরি অনুদান প্রদানের জন্য বিনীত প্রার্থনা জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, addr, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, resident of ${addr.ifBlank { "................" }}, have suffered acute distress due to (${f1.ifBlank { "Disaster/Crisis" }}). Relevant medical/damage proofs are enclosed (${f2.ifBlank { "Enclosures" }}). Kindly sanction emergency relief assistance."
                    }
                ),
                LetterTypeItem(
                    id = "bdo_certificate_correct",
                    titleBn = "সরকারি নথি বা শংসাপত্র সংশোধন",
                    titleEn = "Govt Certificate Correction",
                    subjectBn = "সরকারি শংসাপত্রে ভুল তথ্য সংশোধনের আবেদন",
                    subjectEn = "Application for rectification of official certificate / roster",
                    field1LabelBn = "শংসাপত্রের নাম ও সার্টিফিকেট নম্বর",
                    field1LabelEn = "Certificate Name & Serial No",
                    field2LabelBn = "সঠিক তথ্য ও সহায়ক প্রমাণপত্র",
                    field2LabelEn = "Correct Details & Proof",
                    bodyTemplateBn = { name, addr, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। আমার পূর্বে ইস্যুকৃত শংসাপত্রে (${f1.ifBlank { "সার্টিফিকেট তথ্য" }}) নামের বানান/বয়সে অসাবধানতাবশত ভুল লিপিবদ্ধ হয়েছে। সরকারি আধার/ভোটার অনুযায়ী সঠিক তথ্য: ${f2.ifBlank { "সঠিক তথ্য" }}। সংশোধিত শংসাপত্র প্রদানের অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, addr, _, f1, f2 ->
                        "I bring to your notice that Certificate No: ${f1.ifBlank { "Cert Details" }} contains a clerical error. As per my primary identification documents, the correct entry is: ${f2.ifBlank { "Correct Data" }}. Kindly issue a rectified certificate."
                    }
                )
            )
        ),

        // 7. ভূমি ও রেজিস্ট্রি (Land & Registry / BL&LRO)
        LetterCategory(
            id = "land_bllro",
            titleBn = "৭. ভূমি ও রেজিস্ট্রি (Land & Registry / BL&LRO)",
            titleEn = "7. Land & Registry / BL&LRO",
            defaultRecipientBn = "ব্লক ভূমি ও ভূমি সংস্কার আধিকারিক (BL&LRO)",
            defaultRecipientEn = "The Block Land & Land Reforms Officer (BL&LRO)",
            letterTypes = listOf(
                LetterTypeItem(
                    id = "land_mutation",
                    titleBn = "জমির মিউটেশন (নামপত্তন) আবেদন",
                    titleEn = "Land Mutation Application",
                    subjectBn = "জমির মিউটেশন ও রেকর্ডভুক্তিকরণের জন্য আবেদন",
                    subjectEn = "Application for mutation and recording of land title",
                    field1LabelBn = "মৌজা ও খতিয়ান নম্বর",
                    field1LabelEn = "Mouza & Khatian No",
                    field2LabelBn = "দাগ নম্বর ও জমির পরিমাণ",
                    field2LabelEn = "Plot (Dag) No & Land Area",
                    bodyTemplateBn = { name, addr, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}। মৌজা ও খতিয়ান নং: ${f1.ifBlank { "মৌজা ও খতিয়ান" }}, দাগ নং ও জমির পরিমাণ: ${f2.ifBlank { "দাগ ও পরিমাণ" }} স্থিত জমি আমি বিধিসম্মতভাবে ক্রয়/উত্তরাধিকার সূত্রে অর্জন করেছি। প্রয়োজনীয় দলিল ও দাখিলা সহ জমিটির মিউটেশন মঞ্জুর করার আবেদন জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, addr, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, resident of ${addr.ifBlank { "................" }}, have legally acquired land at Mouza & Khatian: ${f1.ifBlank { "Mouza/Khatian" }}, Plot & Area: ${f2.ifBlank { "Plot/Area" }}. Registered deed and rent receipts are enclosed. Kindly sanction mutation in my favor."
                    }
                ),
                LetterTypeItem(
                    id = "land_porcha",
                    titleBn = "জমির রেকর্ড বা পর্চা তোলার আবেদন",
                    titleEn = "ROR / Certified Porcha Copy",
                    subjectBn = "জমির খতিয়ান / পর্চার প্রত্যয়িত অনুলিপি (ROR) প্রাপ্তির আবেদন",
                    subjectEn = "Application for certified copy of Record of Rights (ROR / Porcha)",
                    field1LabelBn = "মৌজা ও খতিয়ান নম্বর",
                    field1LabelEn = "Mouza & Khatian No",
                    field2LabelBn = "আবেদনকারীর মোবাইল ও জাতীয় পরিচয়পত্র",
                    field2LabelEn = "Applicant Mobile & ID Proof",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। আমার মালিকানাধীন জমি মৌজা ও খতিয়ান: ${f1.ifBlank { "মৌজা ও খতিয়ান" }}-এর একটি প্রত্যয়িত পর্চা (Certified ROR) বিশেষ আইনি কাজের জন্য প্রয়োজন। সরকারি কোর্ট ফি দাখিলপূর্বক আমাকে পর্চাটি প্রদান করার অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, require a certified copy of the Record of Rights (ROR / Porcha) for Mouza & Khatian: ${f1.ifBlank { "Mouza & Khatian" }}. Necessary fees are paid. Kindly issue the certified copy."
                    }
                ),
                LetterTypeItem(
                    id = "land_conversion",
                    titleBn = "জমির শ্রেণি পরিবর্তন (Conversion)",
                    titleEn = "Land Classification Conversion",
                    subjectBn = "জমির শ্রেণি পরিবর্তন (শালি/বাস্তু রূপান্তর) করার আবেদন",
                    subjectEn = "Application for conversion of land classification (Sali to Bastu)",
                    field1LabelBn = "মৌজা, খতিয়ান ও দাগ নম্বর",
                    field1LabelEn = "Mouza, Khatian & Dag No",
                    field2LabelBn = "বর্তমান শ্রেণি ও প্রস্তাবিত শ্রেণি (যেমন: শালি থেকে বাস্তু)",
                    field2LabelEn = "Existing to Proposed Class (e.g. Sali to Bastu)",
                    bodyTemplateBn = { name, addr, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}। মৌজা, খতিয়ান ও দাগ নং: ${f1.ifBlank { "দাগ ও খতিয়ান" }}-এ আমার মালিকানাধীন জমির শ্রেণি ${f2.ifBlank { "শালি হইতে বাস্তু" }} রূপান্তর করা আবশ্যক। প্রয়োজনীয় সাইট প্ল্যান সহ শ্রেণি পরিবর্তনের অনুমোদন প্রদানের আবেদন জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, addr, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, hold land at Mouza, Khatian & Dag: ${f1.ifBlank { "Plot Details" }}. I seek permission for land classification conversion (${f2.ifBlank { "Sali to Bastu" }}). Site plan and rent receipts are annexed."
                    }
                ),
                LetterTypeItem(
                    id = "land_demarcation",
                    titleBn = "জমির সীমানা পরিমাপের আবেদন",
                    titleEn = "Land Demarcation / Measurement",
                    subjectBn = "সরকারি আমিন দ্বারা জমির সীমানা পরিমাপের (Demarcation) আবেদন",
                    subjectEn = "Application for official land demarcation / survey measurement",
                    field1LabelBn = "মৌজা, দাগ ও খতিয়ান নম্বর",
                    field1LabelEn = "Mouza, Dag & Khatian No",
                    field2LabelBn = "সীমানা বিরোধ বা পরিমাপের কারণ",
                    field2LabelEn = "Reason for demarcation / Dispute",
                    bodyTemplateBn = { name, addr, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}। আমার ক্রয়কৃত জমি মৌজা, দাগ ও খতিয়ান: ${f1.ifBlank { "দাগ ও খতিয়ান" }}-এর সীমানা নির্ধারণে প্রতিবেশীর সাথে জটিলতা সৃষ্টি হয়েছে (${f2.ifBlank { "সীমানা বিরোধ" }})। সরকারি আমিন দ্বারা জমিটি বিধিসম্মত পরিমাপ করার আবেদন জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, addr, _, f1, f2 ->
                        "I am ${name.ifBlank { "................" }}, owning land at Mouza, Dag & Khatian: ${f1.ifBlank { "Land info" }}. Due to boundary confusion (${f2.ifBlank { "Demarcation dispute" }}), I request you to depute a Government Surveyor/Amin to officially demarcate my plot boundaries."
                    }
                )
            )
        ),

        // 8. হাসপাতাল ও স্বাস্থ্য (Health & Hospital)
        LetterCategory(
            id = "health",
            titleBn = "৮. হাসপাতাল ও স্বাস্থ্য (Health & Hospital)",
            titleEn = "8. Health & Hospital",
            defaultRecipientBn = "মেডিকেল সুপারিনটেনডেন্ট (Medical Superintendent / BMOH)",
            defaultRecipientEn = "The Medical Superintendent / BMOH",
            letterTypes = listOf(
                LetterTypeItem(
                    id = "health_fitness",
                    titleBn = "মেডিকেল বা ফিটনেস সার্টিফিকেট আবেদন",
                    titleEn = "Medical / Fitness Certificate",
                    subjectBn = "শারীরিক সুস্থতা শংসাপত্র (Medical Fitness Certificate) প্রদানের আবেদন",
                    subjectEn = "Application for issuance of Medical Fitness Certificate",
                    field1LabelBn = "চাকরি / কোর্সের নাম ও রেফারেন্স",
                    field1LabelEn = "Job / Course Name & Ref No",
                    field2LabelBn = "শারীরিক পরীক্ষার তারিখ ও ওপিডি টিকিট নং",
                    field2LabelEn = "OPD Ticket No & Exam Date",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। আমার চাকরিতে যোগদান / শিক্ষাপ্রতিষ্ঠানে ভর্তির জন্য (${f1.ifBlank { "নিয়োগ পরীক্ষা" }}) একটি অনুমোদিত মেডিকেল ফিটনেস সার্টিফিকেট প্রয়োজন (OPD টিকিট: ${f2.ifBlank { "................" }})। আমার স্বাস্থ্য পরীক্ষা সম্পন্নপূর্বক শংসাপত্রটি প্রদান করার অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, have been selected for (${f1.ifBlank { "Employment / Admission" }}). OPD Ticket: ${f2.ifBlank { "OPD No" }}. I request you to conduct the requisite physical examination and issue a Medical Fitness Certificate."
                    }
                ),
                LetterTypeItem(
                    id = "health_waiver",
                    titleBn = "চিকিৎসার খরচ মকুবের আবেদন",
                    titleEn = "Hospital Treatment Fee Waiver",
                    subjectBn = "হাসপাতাল চিকিৎসার খরচ মকুবের বিনীত আবেদন",
                    subjectEn = "Application for concession / waiver in hospital medical expenses",
                    field1LabelBn = "রোগীর নাম ও বেড/কেবিন নম্বর",
                    field1LabelEn = "Patient Name & Bed/Ward No",
                    field2LabelBn = "বিপিএল কার্ড বা আর্থিক অসচ্ছলতা প্রমাণ",
                    field2LabelEn = "BPL / Low Income Proof No",
                    bodyTemplateBn = { name, addr, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}। আমার পরিবারের রোগী ${f1.ifBlank { "রোগীর নাম ও বেড" }} হাসপাতালে চিকিৎসাধীন। আমাদের পরিবারের আর্থিক অবস্থা অত্যন্ত অসচ্ছল (বিপিএল: ${f2.ifBlank { "বিপিএল তথ্য" }})। চিকিৎসার ব্যয়বহুল বিল বহন করা অসম্ভব। অতএব, মানবিক দিক বিবেচনায় বিল মকুবের প্রার্থনা জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, addr, _, f1, f2 ->
                        "I am ${name.ifBlank { "................" }}. My family member, ${f1.ifBlank { "Patient Name & Ward" }}, is admitted in your hospital. As we belong to BPL category (${f2.ifBlank { "BPL Details" }}), I earnestly appeal for concession/waiver in hospital investigation and treatment charges."
                    }
                ),
                LetterTypeItem(
                    id = "health_birth_rectify",
                    titleBn = "জন্ম রেকর্ড সংশোধন আবেদন",
                    titleEn = "Hospital Birth Record Correction",
                    subjectBn = "হাসপাতালের ডিসচার্জ ফর্মে শিশুর নাম বা জন্মতারিখ সংশোধনের আবেদন",
                    subjectEn = "Application for correction in Hospital Birth Register",
                    field1LabelBn = "মাতার নাম ও বেড/ভর্তি নম্বর",
                    field1LabelEn = "Mother Name & Admission No",
                    field2LabelBn = "সঠিক নাম, লিঙ্গ ও জন্মতারিখ",
                    field2LabelEn = "Correct Child Name, Gender & Date",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। মাতা: ${f1.ifBlank { "মাতার নাম ও ভর্তি নং" }}। হাসপাতালে ইস্যুকৃত জন্ম রেজিস্ট্রেশনে শিশুর নাম/বানানে ত্রুটি রয়েছে। সঠিক তথ্য: ${f2.ifBlank { "সঠিক তথ্য" }}। নথিপত্র সংশোধনপূর্বক সঠিক ডিসচার্জ সার্টিফিকেট প্রদান করার অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, state that my child was delivered under Admission No: ${f1.ifBlank { "Mother Details" }}. There is an error in birth recording. Correct details are: ${f2.ifBlank { "Correct Data" }}. Kindly rectify the birth register entry."
                    }
                ),
                LetterTypeItem(
                    id = "health_card_issue",
                    titleBn = "সরকারি স্বাস্থ্য কার্ড বা পরিষেবা সংক্রান্ত আবেদন",
                    titleEn = "Health Scheme (Swasthya Sathi / Ayushman)",
                    subjectBn = "স্বাস্থ্যসাথী / আয়ুষ্মান কার্ডে চিকিৎসার অনুমতি প্রদানের আবেদন",
                    subjectEn = "Application for approval of treatment under Swasthya Sathi / Ayushman Bharat",
                    field1LabelBn = "স্বাস্থ্য কার্ড নম্বর ও রোগীর নাম",
                    field1LabelEn = "Health Card No & Patient Name",
                    field2LabelBn = "রোগের নাম ও প্রয়োজনীয় অস্ত্রোপচার",
                    field2LabelEn = "Ailment & Proposed Treatment",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। স্বাস্থ্য কার্ড নং: ${f1.ifBlank { "কার্ড নং" }}। রোগীর জরুরি চিকিৎসার (${f2.ifBlank { "রোগ ও অপারেশন" }}) জন্য কার্ডের মাধ্যমে প্যাকেজ অনুমোদন আবশ্যক। অবিলম্বে সরকারি সুবিধা প্রদান করার আবেদন জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I hold Government Health Card No: ${f1.ifBlank { "Card No" }} for patient: ${f1.ifBlank { "Patient" }}. The patient requires urgent procedure for ${f2.ifBlank { "Treatment" }}. Kindly authorize the package coverage under cashless scheme."
                    }
                )
            )
        ),

        // 9. রেলওয়ে (Railway)
        LetterCategory(
            id = "railway",
            titleBn = "৯. রেলওয়ে (Railway)",
            titleEn = "9. Railway",
            defaultRecipientBn = "প্রধান বাণিজ্যিক ব্যবস্থাপক (Chief Commercial Manager / Station Master)",
            defaultRecipientEn = "The Chief Commercial Manager / Station Master",
            letterTypes = listOf(
                LetterTypeItem(
                    id = "rail_refund",
                    titleBn = "ট্রেন টিকিট বাতিল ও রিফান্ডের আবেদন",
                    titleEn = "Train Ticket Cancellation Refund",
                    subjectBn = "বাতিল ট্রেনের টিকিটের টাকা ফেরত (TDR Refund) পাওয়ার আবেদন",
                    subjectEn = "Application for refund against cancelled train / TDR",
                    field1LabelBn = "PNR নম্বর ও ট্রেন নম্বর",
                    field1LabelEn = "PNR Number & Train Number",
                    field2LabelBn = "যাত্রার তারিখ ও টিকিটের মূল্য",
                    field2LabelEn = "Journey Date & Ticket Amount",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। PNR নং: ${f1.ifBlank { "PNR নং" }}। গত ${f2.ifBlank { "তারিখে" }} ট্রেনটি বাতিল / অতিরিক্ত বিলম্বে চলায় আমি যাত্রা করতে পারিনি। বিধিসম্মতভাবে টিকিট বাতিল করে আমার ব্যাংক অ্যাকাউন্টে রিফান্ডের অর্থ প্রেরণের অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, booked PNR: ${f1.ifBlank { "PNR No" }} on Train: ${f1.ifBlank { "Train" }}. Due to train cancellation / severe delay on ${f2.ifBlank { "Date" }}, journey was aborted. Please process full TDR refund to my source account."
                    }
                ),
                LetterTypeItem(
                    id = "rail_lost",
                    titleBn = "ট্রেনে হারানো জিনিসপত্র খোঁজার অভিযোগ",
                    titleEn = "Lost Property in Train Complaint",
                    subjectBn = "ট্রেনের কামরায় হারানো লাগেজ উদ্ধারের আবেদন",
                    subjectEn = "Complaint regarding baggage / valuables left behind in train",
                    field1LabelBn = "ট্রেন নম্বর, বগি ও সিট নম্বর",
                    field1LabelEn = "Train No, Coach & Berth No",
                    field2LabelBn = "হারানো ব্যাগের বিবরণ ও ভ্রমণের তারিখ",
                    field2LabelEn = "Bag Description & Journey Date",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "মহাশয়, আমি ${name.ifBlank { "................" }}। ট্রেন ও বগি নং: ${f1.ifBlank { "ট্রেন ও সিট" }}। ভ্রমণের সময় ভুলবশত আমার ব্যাগটি (${f2.ifBlank { "ব্যাগের বিবরণ" }}) কামরায় ফেলে নেমে আসি। ব্যাগটি উদ্ধার করে ফিরিয়ে দেওয়ার জন্য আরপিএফ ও সংশ্লিষ্ট দপ্তরকে অবগত করার অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, traveled in Coach: ${f1.ifBlank { "Coach & Seat" }}. I inadvertently left my luggage (${f2.ifBlank { "Luggage details" }}) on the train. I request RPF / Commercial staff to track and retrieve my belongings."
                    }
                ),
                LetterTypeItem(
                    id = "rail_concession",
                    titleBn = "কনসেশন (ভাড়ায় ছাড়) আবেদন",
                    titleEn = "Railway Concession Request",
                    subjectBn = "বিশেষ পরিস্থিতিতে রেলভাড়ায় ছাড় (Concession) পাওয়ার আবেদন",
                    subjectEn = "Application for railway fare concession for patient / student",
                    field1LabelBn = "আবেদনকারীর ধরন (যেমন: ক্যানসার রোগী / প্রতিবন্ধী)",
                    field1LabelEn = "Category (e.g. Patient / Divyangjan)",
                    field2LabelBn = "চিকিৎসা শংসাপত্র ও ভ্রমণ রুট",
                    field2LabelEn = "Medical Certificate No & Travel Route",
                    bodyTemplateBn = { name, addr, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}। আমি একজন ${f1.ifBlank { "রোগী/প্রতিবন্ধী" }} (সার্টিফিকেট: ${f2.ifBlank { "সার্টিফিকেট তথ্য" }})। চিকিৎসার উদ্দেশ্যে দূরপাল্লার ট্রেনে যাতায়াতের জন্য রেলের নিয়ম অনুযায়ী কনসেশন পাস মঞ্জুর করার অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, addr, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, fall under ${f1.ifBlank { "Concession Category" }}. Valid medical certificate is attached (${f2.ifBlank { "Cert Details" }}). I request you to sanction rail travel concession as per railway guidelines."
                    }
                ),
                LetterTypeItem(
                    id = "rail_parcel",
                    titleBn = "পার্সেল বা লাগেজ সংক্রান্ত আবেদন",
                    titleEn = "Railway Parcel Booking / Query",
                    subjectBn = "রেলওয়ে পার্সেল বিলম্ব বা ক্ষতিপূরণের আবেদন",
                    subjectEn = "Application regarding delayed railway parcel delivery / claim",
                    field1LabelBn = "পার্সেল ওয়েবিল (PWB) নম্বর ও তারিখ",
                    field1LabelEn = "Parcel Way Bill (PWB) No & Date",
                    field2LabelBn = "বুকিং স্টেশন ও গন্তব্য স্টেশন",
                    field2LabelEn = "Source & Destination Stations",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। PWB নং: ${f1.ifBlank { "PWB নং" }}। বুকিং রুট: ${f2.ifBlank { "স্টেশন রুট" }}। নির্দিষ্ট সময় অতিক্রান্ত হওয়া সত্ত্বেও বুকিংকৃত পার্সেলটি গন্তব্যে পৌঁছায়নি। অবিলম্বে পার্সেলটি খালাসের ব্যবস্থা করার অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I booked parcel under PWB No: ${f1.ifBlank { "PWB No" }} between stations: ${f2.ifBlank { "Route" }}. The consignment has not arrived within scheduled transit time. Kindly expedite tracing and delivery."
                    }
                )
            )
        ),

        // 10. পোস্ট অফিস (Post Office)
        LetterCategory(
            id = "post_office",
            titleBn = "১০. পোস্ট অফিস (Post Office)",
            titleEn = "10. Post Office",
            defaultRecipientBn = "পোস্টমাস্টার মহাশয় (The Postmaster)",
            defaultRecipientEn = "The Postmaster",
            letterTypes = listOf(
                LetterTypeItem(
                    id = "post_parcel_delay",
                    titleBn = "স্পিড পোস্ট বা রেজিস্ট্রি পার্সেল না পাওয়ার অভিযোগ",
                    titleEn = "Speed Post / Registered Article Non-Delivery",
                    subjectBn = "স্পিড পোস্ট / রেজিস্ট্রি পার্সেল না পাওয়ার অভিযোগ",
                    subjectEn = "Complaint regarding non-delivery / delay of Speed Post article",
                    field1LabelBn = "কনসাইনমেন্ট / ট্র্যাকিং নম্বর (Tracking No)",
                    field1LabelEn = "Consignment / Tracking Number",
                    field2LabelBn = "প্রেরকের নাম ও বুকিংয়ের তারিখ",
                    field2LabelEn = "Sender Details & Booking Date",
                    bodyTemplateBn = { name, addr, _, f1, f2 ->
                        "মহাশয়, আমি ${name.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}। ট্র্যাকিং নং: ${f1.ifBlank { "ট্র্যাকিং নং" }}। বিগত ${f2.ifBlank { "তারিখে" }} বুকিং হওয়া গুরুত্বপূর্ণ নথিপত্র এখনও আমার ঠিকানায় বিলি হয়নি। অনুগ্রহ করে পার্সেলটি সন্ধান করে দ্রুত বিলির ব্যবস্থা করুন।"
                    },
                    bodyTemplateEn = { name, addr, _, f1, f2 ->
                        "Sir, I, ${name.ifBlank { "................" }}, residing at ${addr.ifBlank { "................" }}, have not received Speed Post Consignment: ${f1.ifBlank { "Tracking No" }} booked on ${f2.ifBlank { "Booking Date" }}. Kindly trace and expedite delivery."
                    }
                ),
                LetterTypeItem(
                    id = "post_passbook",
                    titleBn = "সেভিংস অ্যাকাউন্ট পাসবুক আবেদন",
                    titleEn = "Post Office Savings Passbook Issue",
                    subjectBn = "ডাকঘর সেভিংস অ্যাকাউন্টের নতুন পাসবুক প্রদানের আবেদন",
                    subjectEn = "Application for duplicate / fresh Post Office Savings Passbook",
                    field1LabelBn = "অ্যাকাউন্ট নম্বর / POSB Account No",
                    field1LabelEn = "POSB Account Number",
                    field2LabelBn = "কারণ (যেমন: পুরনো বই শেষ / নষ্ট হয়েছে)",
                    field2LabelEn = "Reason (e.g. Pages exhausted)",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। আমার ডাকঘর সঞ্চয় অ্যাকাউন্ট নং: ${f1.ifBlank { "অ্যাকাউন্ট নং" }}। বর্তমান পাসবুকটি (${f2.ifBlank { "পাতা শেষ হওয়ায়" }}) নতুন পাসবুক প্রয়োজন। আমাকে নতুন পাসবুক ইস্যু করার অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I hold Post Office Savings Account No: ${f1.ifBlank { "Account No" }}. As my passbook is (${f2.ifBlank { "exhausted" }}), I request you to kindly issue a fresh passbook."
                    }
                ),
                LetterTypeItem(
                    id = "post_address",
                    titleBn = "ঠিকানা পরিবর্তন সংক্রান্ত আবেদন",
                    titleEn = "Address Change in Post Office Account",
                    subjectBn = "ডাকঘর সঞ্চয় স্কিমে ঠিকানা পরিবর্তনের আবেদন",
                    subjectEn = "Application for updating address in Post Office Small Savings Scheme",
                    field1LabelBn = "স্কিম ও অ্যাকাউন্ট নম্বর (যেমন: MIS / NSC / RD)",
                    field1LabelEn = "Scheme & Account No (e.g. MIS/RD)",
                    field2LabelBn = "নতুন ঠিকানা ও মোবাইল নম্বর",
                    field2LabelEn = "New Address & Contact No",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। অ্যাকাউন্ট নং: ${f1.ifBlank { "অ্যাকাউন্ট নং" }}। বাসস্থান স্থানান্তরের কারণে আমার নতুন ঠিকানা: ${f2.ifBlank { "নতুন ঠিকানা" }}। ঠিকানার সপক্ষে আধার কার্ড সংযুক্ত করে ঠিকানা পরিবর্তনের আবেদন জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I maintain Postal Scheme Account No: ${f1.ifBlank { "Account No" }}. Having shifted my residence, I request you to update my address to: ${f2.ifBlank { "New Address" }}. KYC proof is enclosed."
                    }
                ),
                LetterTypeItem(
                    id = "post_reissue",
                    titleBn = "হারানো সার্টিফিকেট বা ড্রাফট রি-ইস্যু",
                    titleEn = "Re-issue of Lost NSC / KVP Certificate",
                    subjectBn = "হারানো NSC / KVP সার্টিফিকেটের ডুপ্লিকেট কপির আবেদন",
                    subjectEn = "Application for issuance of duplicate NSC / KVP / Certificate",
                    field1LabelBn = "সার্টিফিকেট নম্বর ও ইস্যু তারিখ",
                    field1LabelEn = "Certificate No & Issue Date",
                    field2LabelBn = "বিনিয়োগের পরিমাণ ও জিডি নম্বর",
                    field2LabelEn = "Invested Amount & Police GD No",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। আমার জাতীয় সঞ্চয়পত্র (NSC/KVP) নং: ${f1.ifBlank { "সার্টিফিকেট নং" }} দুর্ভাগ্যবশত হারিয়ে গেছে (থানা জিডি: ${f2.ifBlank { "জিডি নং" }})। বন্ড সম্পাদনপূর্বক ডুপ্লিকেট সার্টিফিকেট ইস্যু করার প্রার্থনা জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, lost my original NSC/KVP Certificate: ${f1.ifBlank { "Cert No" }} (GD: ${f2.ifBlank { "GD No" }}). Upon submission of indemnity bond, kindly issue duplicate certificate."
                    }
                )
            )
        ),

        // 11. পাসপোর্ট (Passport)
        LetterCategory(
            id = "passport",
            titleBn = "১১. পাসপোর্ট (Passport)",
            titleEn = "11. Passport",
            defaultRecipientBn = "আঞ্চলিক পাসপোর্ট আধিকারিক (Regional Passport Officer - RPO)",
            defaultRecipientEn = "The Regional Passport Officer (RPO)",
            letterTypes = listOf(
                LetterTypeItem(
                    id = "pass_pcc",
                    titleBn = "পাসপোর্ট পুলিশ ক্লিয়ারেন্স সার্টিফিকেট (PCC)",
                    titleEn = "Police Clearance Certificate (PCC) Request",
                    subjectBn = "পাসপোর্ট সংক্রান্ত পুলিশ ক্লিয়ারেন্স সার্টিফিকেট (PCC) দ্রুত নিষ্পত্তির আবেদন",
                    subjectEn = "Application for expediting Police Clearance Certificate (PCC)",
                    field1LabelBn = "পাসপোর্ট আবেদন ফাইল নম্বর (File No)",
                    field1LabelEn = "Passport Application File No",
                    field2LabelBn = "বিদেশযাত্রার উদ্দেশ্য ও ভিসার তথ্য",
                    field2LabelEn = "Foreign Travel Purpose / Visa Info",
                    bodyTemplateBn = { name, addr, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}। পাসপোর্ট ফাইল নং: ${f1.ifBlank { "ফাইল নং" }}। জরুরি কর্মসংস্থান/শিক্ষার উদ্দেশ্যে (${f2.ifBlank { "ভিসা প্রক্রিয়া" }}) আমার অবিলম্বে PCC প্রয়োজন। সংশ্লিষ্ট পুলিশ রিপোর্ট যাচাই করে দ্রুত সার্টিফিকেট প্রদানের অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, addr, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, address: ${addr.ifBlank { "................" }}, have applied for PCC under File No: ${f1.ifBlank { "File No" }} for (${f2.ifBlank { "Visa / Employment" }}). I request you to kindly expedite issuance of my PCC."
                    }
                ),
                LetterTypeItem(
                    id = "pass_reschedule",
                    titleBn = "অ্যাপয়েন্টমেন্ট রিসিডিউল আবেদন",
                    titleEn = "Passport Appointment Reschedule",
                    subjectBn = "পাসপোর্ট সেবা কেন্দ্রে অ্যাপয়েন্টমেন্টের তারিখ পরিবর্তনের আবেদন",
                    subjectEn = "Application for rescheduling PSK Appointment date",
                    field1LabelBn = "অ্যাপ্লিকেশন রেফারেন্স নম্বর (ARN)",
                    field1LabelEn = "Application Reference No (ARN)",
                    field2LabelBn = "পূর্বের তারিখ ও পরিবর্তনের অনিবার্য কারণ",
                    field2LabelEn = "Existing Date & Compelling Reason",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। ARN নং: ${f1.ifBlank { "ARN নং" }}। অনিবার্য অসুস্থতা/ব্যক্তিগত কারণবশত (${f2.ifBlank { "কারণ" }}) পূর্বনির্ধারিত তারিখে উপস্থিত হতে পারছি না। পরবর্তী সুবিধাজনক কর্মদিবসে অ্যাপয়েন্টমেন্ট রিসিডিউল করার অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I have booked PSK appointment under ARN: ${f1.ifBlank { "ARN" }}. Due to (${f2.ifBlank { "emergency" }}), I cannot attend on the scheduled date. Kindly reschedule my appointment slot."
                    }
                ),
                LetterTypeItem(
                    id = "pass_rectify",
                    titleBn = "পাসপোর্টে নাম বা ঠিকানা সংশোধন",
                    titleEn = "Passport Name / Address Correction",
                    subjectBn = "বিদ্যমান পাসপোর্টে নাম/ঠিকানা সংশোধনের আবেদন",
                    subjectEn = "Application for re-issue of passport with changed name / address",
                    field1LabelBn = "বর্তমান পাসপোর্ট নম্বর ও মেয়াদ",
                    field1LabelEn = "Existing Passport No & Validity",
                    field2LabelBn = "সংশোধিত নাম/ঠিকানা ও আধার নম্বর",
                    field2LabelEn = "Correct Details & Supporting Doc",
                    bodyTemplateBn = { name, addr, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। বর্তমান পাসপোর্ট নং: ${f1.ifBlank { "পাসপোর্ট নং" }}। ঠিকানার পরিবর্তন ও নামের বানান সংশোধন হেতু (${f2.ifBlank { "সংশোধিত তথ্য" }}) নতুন পাসপোর্ট রি-ইস্যু করা প্রয়োজন। প্রয়োজনীয় গেজেট ও নথি সহ আবেদন দাখিল করছি।"
                    },
                    bodyTemplateEn = { name, addr, _, f1, f2 ->
                        "I hold Passport No: ${f1.ifBlank { "Passport No" }}. Due to relocation and name correction (${f2.ifBlank { "Rectification details" }}), I request re-issuance of passport with updated records."
                    }
                ),
                LetterTypeItem(
                    id = "pass_hold",
                    titleBn = "পাসপোর্ট ভেরিফিকেশন সমস্যা সমাধান",
                    titleEn = "Clearance of Hold / Pending Passport",
                    subjectBn = "পেন্ডিং বা হোল্ড হয়ে থাকা পাসপোর্ট আবেদন নিষ্পত্তির আবেদন",
                    subjectEn = "Application for clearance of hold status on passport application",
                    field1LabelBn = "ফাইল নম্বর ও পিএসকে কেন্দ্রের নাম",
                    field1LabelEn = "File No & PSK Centre Name",
                    field2LabelBn = "অতিরিক্ত জমা দেওয়া নথির বিবরণ",
                    field2LabelEn = "Additional Submitted Documents",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। ফাইল নং: ${f1.ifBlank { "ফাইল নং" }}। নথিপত্র অসম্পূর্ণ থাকার কারণে আমার পাসপোর্ট আবেদনটি হোল্ডে রয়েছে। আমি চাহিত সকল মূল নথি (${f2.ifBlank { "নথির বিবরণ" }}) দাখিল করলাম। আবেদনটি অবিলম্বে নিষ্পত্তি করার অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, submitted application under File: ${f1.ifBlank { "File No" }}. The file was kept on hold for documentary clarification. I submit the required records (${f2.ifBlank { "Docs" }}). Kindly release and process the passport."
                    }
                )
            )
        ),

        // 12. পরিবহণ ও RTO (Transport & RTO)
        LetterCategory(
            id = "transport_rto",
            titleBn = "১২. পরিবহণ ও RTO (Transport & RTO)",
            titleEn = "12. Transport & RTO",
            defaultRecipientBn = "আঞ্চলিক পরিবহণ আধিকারিক (Regional Transport Officer - RTO)",
            defaultRecipientEn = "The Regional Transport Officer (RTO)",
            letterTypes = listOf(
                LetterTypeItem(
                    id = "rto_dl",
                    titleBn = "ড্রাইভিং লাইসেন্স ইস্যু বা রিনিউ",
                    titleEn = "Driving Licence Issue / Renewal",
                    subjectBn = "ড্রাইভিং লাইসেন্স নবায়ন / নতুন স্মার্ট কার্ডের আবেদন",
                    subjectEn = "Application for renewal / issuance of Driving Licence",
                    field1LabelBn = "ড্রাইভিং লাইসেন্স নম্বর (DL No)",
                    field1LabelEn = "Driving Licence Number",
                    field2LabelBn = "মেয়াদ শেষের তারিখ ও মোবাইল নম্বর",
                    field2LabelEn = "Expiry Date & Mobile Number",
                    bodyTemplateBn = { name, addr, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}। আমার ড্রাইভিং লাইসেন্স নং: ${f1.ifBlank { "DL নং" }}-এর মেয়াদ গত ${f2.ifBlank { "তারিখে" }} উত্তীর্ণ হয়েছে। আমি প্রয়োজনীয় মেডিকেল সার্টিফিকেট ও ফি জমা দিয়ে লাইসেন্সটি নবায়নের বিনীত প্রার্থনা জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, addr, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, holding Driving Licence No: ${f1.ifBlank { "DL No" }}, expired on ${f2.ifBlank { "Date" }}. Medical fitness Form 1A and fees are submitted. Please renew my driving licence."
                    }
                ),
                LetterTypeItem(
                    id = "rto_rc_transfer",
                    titleBn = "গাড়ির মালিকানা স্থানান্তর (RC Transfer)",
                    titleEn = "Vehicle Ownership Transfer (RC Transfer)",
                    subjectBn = "যানবাহনের মালিকানা হস্তান্তরের (RC Transfer) আবেদন",
                    subjectEn = "Application for transfer of vehicle ownership in Registration Certificate",
                    field1LabelBn = "গাড়ির রেজিস্ট্ৰেশন নম্বর (Vehicle Reg No)",
                    field1LabelEn = "Vehicle Registration Number",
                    field2LabelBn = "ক্রেতা ও বিক্রেতার নাম ও ফর্ম ২৯/৩০",
                    field2LabelEn = "Buyer & Seller Name / Form 29-30 Ref",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। গাড়ি রেজিস্ট্রেশন নং: ${f1.ifBlank { "গাড়ির নং" }}। উক্ত যানবাহনটি বিধিসম্মতভাবে ${f2.ifBlank { "ক্রেতার নাম" }}-এর নিকট হস্তান্তর করা হয়েছে। ফর্ম ২৯ এবং ৩০ সহ নথিপত্রে নতুন মালিকের নামে আরসি হস্তান্তরের আবেদন জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, have transferred vehicle Reg No: ${f1.ifBlank { "Vehicle No" }} to ${f2.ifBlank { "Transferee Name" }}. Duly executed Forms 29 and 30 with NOC are submitted. Please record transfer of ownership."
                    }
                ),
                LetterTypeItem(
                    id = "rto_duplicate_rc",
                    titleBn = "ডুপ্লিকেট RC বা লাইসেন্স আবেদন",
                    titleEn = "Duplicate RC / Driving Licence",
                    subjectBn = "হারানো রেজিস্ট্রেশন সার্টিফিকেটের (RC) ডুপ্লিকেট কপির আবেদন",
                    subjectEn = "Application for issuance of duplicate Registration Certificate (RC)",
                    field1LabelBn = "গাড়ির রেজিস্ট্রেশন নম্বর ও চেসিস নম্বর",
                    field1LabelEn = "Vehicle Reg No & Chassis No",
                    field2LabelBn = "থানা জিডি নম্বর ও তারিখ",
                    field2LabelEn = "Police GD No & Date",
                    bodyTemplateBn = { name, addr, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। গাড়ি নং: ${f1.ifBlank { "গাড়ি নং" }}। আমার গাড়ির মূল আরসি বইটি হারিয়ে গেছে (থানা জিডি: ${f2.ifBlank { "জিডি নং" }})। ফর্ম ২৬ পূরণ করে সরকারি ফি সহ ডুপ্লিকেট আরসি প্রদানের অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, addr, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, state that the original RC of vehicle ${f1.ifBlank { "Vehicle No" }} has been lost (Police GD: ${f2.ifBlank { "GD No" }}). Kindly issue a duplicate Registration Certificate."
                    }
                ),
                LetterTypeItem(
                    id = "rto_address_change",
                    titleBn = "গাড়ির ঠিকানা পরিবর্তন আবেদন",
                    titleEn = "Address Change in Vehicle RC",
                    subjectBn = "গাড়ির আরসিতে ঠিকানার পরিবর্তনের আবেদন",
                    subjectEn = "Application for recording change of address in Vehicle RC",
                    field1LabelBn = "গাড়ির নম্বর ও আরসি বুক নম্বর",
                    field1LabelEn = "Vehicle No & RC Book No",
                    field2LabelBn = "নতুন ঠিকানা ও আধার কার্ড তথ্য",
                    field2LabelEn = "New Address & Aadhaar Proof",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। গাড়ি নং: ${f1.ifBlank { "গাড়ির নং" }}। নতুন বাসস্থান গ্রহণের কারণে আমার ঠিকানার পরিবর্তন হয়েছে (${f2.ifBlank { "নতুন ঠিকানা" }})। প্রয়োজনীয় ফর্ম ৩৩ দাখিলপূর্বক আরসিতে নতুন ঠিকানা অন্তর্ভুক্তির আবেদন জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I am ${name.ifBlank { "................" }}, owner of vehicle ${f1.ifBlank { "Vehicle No" }}. Having relocated, my new residential address is: ${f2.ifBlank { "Address" }}. Please update address in RC as per Form 33."
                    }
                )
            )
        ),

        // 13. জল ও স্যানিটেশন (Water Supply & Drainage)
        LetterCategory(
            id = "water_sanitation",
            titleBn = "১৩. জল ও স্যানিটেশন (Water Supply & Drainage)",
            titleEn = "13. Water Supply & Drainage",
            defaultRecipientBn = "নির্বাহী বাস্তুকার (Executive Engineer, PHE / Water Works)",
            defaultRecipientEn = "The Executive Engineer (PHE / Water Works)",
            letterTypes = listOf(
                LetterTypeItem(
                    id = "water_new_conn",
                    titleBn = "নতুন পানীয় জলের সংযোগ আবেদন",
                    titleEn = "New Drinking Water Connection",
                    subjectBn = "গার্হস্থ্য পানীয় জলের নতুন পাইপলাইন সংযোগের আবেদন",
                    subjectEn = "Application for new domestic drinking water pipe connection",
                    field1LabelBn = "হোল্ডিং/গ্রাম ও দাগ নম্বর",
                    field1LabelEn = "Holding/Village & Plot No",
                    field2LabelBn = "মোবাইল নম্বর ও সংযোগের পয়েন্ট",
                    field2LabelEn = "Mobile No & Connection Point",
                    bodyTemplateBn = { name, addr, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}। আমার নবনির্মিত বাসস্থানে (হোল্ডিং: ${f1.ifBlank { "................" }}) পানীয় জলের তীব্র সংকট রয়েছে। বিধিসম্মত ফি দাখিল করে সংযোগ প্রদান করার বিনীত প্রার্থনা জানাচ্ছি (মোবাইল: ${f2.ifBlank { "................" }})।"
                    },
                    bodyTemplateEn = { name, addr, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, resident of ${addr.ifBlank { "................" }}, require a piped domestic water connection at my premises (${f1.ifBlank { "Holding" }}). Contact: ${f2.ifBlank { "Mobile" }}. Kindly sanction the water connection."
                    }
                ),
                LetterTypeItem(
                    id = "water_leakage",
                    titleBn = "পাইপলাইন লিকেজ বা নোংরা জলের অভিযোগ",
                    titleEn = "Pipeline Leakage / Contaminated Water Complaint",
                    subjectBn = "প্রধান পানীয় জলের পাইপলাইনে লিকেজ ও দূষিত জলের অভিযোগ",
                    subjectEn = "Complaint regarding drinking water pipeline leakage and contamination",
                    field1LabelBn = "লিকেজের সুনির্দিষ্ট স্থান ও পাড়া",
                    field1LabelEn = "Exact Leakage Location / Para",
                    field2LabelBn = "কতদিন ধরে সমস্যা ও ক্ষয়ক্ষতি",
                    field2LabelEn = "Duration of problem & Severity",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "মহাশয়, আমি ${name.ifBlank { "................" }}। আমাদের এলাকার ${f1.ifBlank { "স্থানের নাম" }} পানীয় জলের প্রধান পাইপলাইন ফেটে গিয়ে (${f2.ifBlank { "বিগত কয়েকদিন ধরে" }}) লক্ষ লক্ষ লিটার জল অপচয় হচ্ছে এবং নোংরা জল সরবরাহ হচ্ছে। অবিলম্বে পাইপটি মেরামত করার আবেদন জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I draw your urgent attention to a severe pipeline burst at ${f1.ifBlank { "Location" }} since (${f2.ifBlank { "past few days" }}), causing dirty water supply and water wastage. Please deploy maintenance team immediately."
                    }
                ),
                LetterTypeItem(
                    id = "water_bill",
                    titleBn = "অতিরিক্ত বিল সংশোধনের আবেদন",
                    titleEn = "Water Tax / Bill Revision",
                    subjectBn = "অস্বাভাবিক জলের বিল বা কর সংশোধনের আবেদন",
                    subjectEn = "Application for rectification of disputed water supply charges",
                    field1LabelBn = "কনজিউমার আইডি / বিল নম্বর",
                    field1LabelEn = "Consumer ID / Water Bill No",
                    field2LabelBn = "অতিরিক্ত টাকার পরিমাণ ও বিলের তারিখ",
                    field2LabelEn = "Disputed Bill Amount & Date",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। গ্রাহক নম্বর: ${f1.ifBlank { "আইডি নং" }}। বিগত ত্রৈমাসিকে আমার স্বাভাবিক ব্যবহারের তুলনায় অতিরিক্ত বিল (${f2.ifBlank { "টাকার পরিমাণ" }}) পাঠানো হয়েছে। মিটার পরীক্ষা করে সঠিক বিল নির্ধারণের অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I hold Water Consumer ID: ${f1.ifBlank { "Consumer ID" }}. An erratic water bill (${f2.ifBlank { "Amount" }}) has been served against my domestic connection. Kindly inspect meter and adjust charges."
                    }
                ),
                LetterTypeItem(
                    id = "water_drain_clean",
                    titleBn = "নিকাশি নালা বা ড্রেন পরিষ্কারের আবেদন",
                    titleEn = "Drainage Cleaning / De-silting Request",
                    subjectBn = "নিকাশি নালা জমে থাকা আবর্জনা ও ড্রেন পরিষ্কারের আবেদন",
                    subjectEn = "Application for de-silting and cleaning of choked public drainage",
                    field1LabelBn = "রাস্তা / ড্রেনের অবস্থান ও ওয়ার্ড নম্বর",
                    field1LabelEn = "Drain Location & Ward Number",
                    field2LabelBn = "মশার উপদ্রব ও জল জমার সমস্যা",
                    field2LabelEn = "Waterlogging & Health hazard",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "মহাশয়, আমরা ${f1.ifBlank { "এলাকা ও ওয়ার্ডের" }} এলাকাবাসী। আমাদের পাড়ার প্রধান নিকাশি নালাটি পলিতে সম্পূর্ণ বুজে যাওয়ায় ড্রেনের নোংরা জল রাস্তায় উপচে পড়ছে এবং মশার প্রকোপ বৃদ্ধি পেয়েছে (${f2.ifBlank { "স্বাস্থ্য সংকট" }})। অবিলম্বে ড্রেনটি পরিষ্কার করার অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "The residents of ${f1.ifBlank { "Location/Ward" }} face severe inconvenience due to choked public drainage causing overflowing sewage and mosquito breeding (${f2.ifBlank { "Health risk" }}). Please arrange urgent cleaning."
                    }
                )
            )
        ),

        // 14. ভোক্তা ও পরিষেবা (Consumer Affairs)
        LetterCategory(
            id = "consumer_affairs",
            titleBn = "১৪. ভোক্তা ও পরিষেবা (Consumer Affairs)",
            titleEn = "14. Consumer Affairs",
            defaultRecipientBn = "ভোক্তা ফোরাম / কাস্টমার সাপোর্ট ম্যানেজার",
            defaultRecipientEn = "The Consumer Grievance Officer / Support Manager",
            letterTypes = listOf(
                LetterTypeItem(
                    id = "consumer_defect",
                    titleBn = "ত্রুটিযুক্ত পণ্য বদল বা টাকা ফেরতের দাবি",
                    titleEn = "Defective Product Replacement / Refund",
                    subjectBn = "ত্রুটিযুক্ত পণ্য প্রতিস্থাপন বা সম্পূর্ণ মূল্য ফেরতের দাবি",
                    subjectEn = "Notice claiming replacement / full refund for defective product",
                    field1LabelBn = "পণ্যের নাম, মডেল ও ইনভয়েস নম্বর",
                    field1LabelEn = "Product Name, Model & Invoice No",
                    field2LabelBn = "ক্রয়ের তারিখ ও ত্রুটির বিবরণ",
                    field2LabelEn = "Purchase Date & Nature of defect",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "মহাশয়, আমি ${name.ifBlank { "................" }}। ইনভয়েস নং: ${f1.ifBlank { "ইনভয়েস" }}। আপনার শোরুম/অনলাইন থেকে ক্রয়কৃত পণ্যটি কেনার মাত্র কয়েকদিনের মধ্যে অচল হয়ে গেছে (${f2.ifBlank { "ত্রুটি বিবরণ" }})। ওয়ারেন্টির আওতায় অবিলম্বে পণ্যটি পরিবর্তন অথবা সম্পূর্ণ টাকা রিফান্ড করার দাবি জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I purchased ${f1.ifBlank { "Product & Invoice" }} on ${f2.ifBlank { "Date" }}. The product is inherently defective and stopped functioning (${f2.ifBlank { "Defect" }}). Under Consumer Protection norms, I demand replacement or full refund."
                    }
                ),
                LetterTypeItem(
                    id = "consumer_fraud",
                    titleBn = "অনলাইন শপিং জালিয়াতির অভিযোগ",
                    titleEn = "E-Commerce / Online Fraud Complaint",
                    subjectBn = "অনলাইন অর্ডারে ভুয়া/ভুল পণ্য প্রদান সংক্রান্ত জালিয়াতির অভিযোগ",
                    subjectEn = "Complaint regarding fake / wrong item delivered by e-commerce platform",
                    field1LabelBn = "অর্ডার আইডি ও পোর্টালের নাম (Order ID)",
                    field1LabelEn = "Order ID & Portal Name",
                    field2LabelBn = "প্রদত্ত টাকা ও প্রাপ্ত ভুল সামগ্রীর বিবরণ",
                    field2LabelEn = "Paid Amount & Wrong Item details",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "মহাশয়, আমি ${name.ifBlank { "................" }}। অর্ডার আইডি: ${f1.ifBlank { "অর্ডার নং" }}। অনলাইনে আসল সামগ্রীর মূল্য বাবদ ${f2.ifBlank { "টাকার পরিমাণ" }} পরিশোধ করা হলেও আমাকে সম্পূর্ণ ভুল ও নিম্নমানের পণ্য পাঠানো হয়েছে। অভিযোগ জানানোর পরেও কোনো সুরাহা হয়নি। টাকা রিফান্ডের দাবি জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, placed order ${f1.ifBlank { "Order ID" }} paying ${f2.ifBlank { "Amount" }}. However, a spurious/wrong item was delivered. Despite repeated tickets, issue remains unresolved. I demand immediate refund."
                    }
                ),
                LetterTypeItem(
                    id = "consumer_warranty",
                    titleBn = "বিক্রয়োত্তর ওয়ারেন্টি সার্ভিস না পাওয়ার অভিযোগ",
                    titleEn = "Refusal of Warranty Service Complaint",
                    subjectBn = "বৈধ ওয়ারেন্টি থাকা সত্ত্বেও ফ্রি সার্ভিস দিতে অস্বীকৃতির বিরুদ্ধে অভিযোগ",
                    subjectEn = "Complaint against unauthorized refusal of after-sales warranty service",
                    field1LabelBn = "পণ্যের সিরিয়াল নম্বর ও সার্ভিস জবশিট নং",
                    field1LabelEn = "Product Serial No & Job Sheet No",
                    field2LabelBn = "সার্ভিস সেন্টারের নাম ও অযৌক্তিক দাবির বিবরণ",
                    field2LabelEn = "Service Centre Name & Refusal reason",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। সিরিয়াল নং: ${f1.ifBlank { "সিরিয়াল নং" }}। পণ্যটির বৈধ ওয়ারেন্টি থাকা সত্ত্বেও অনুমোদিত সার্ভিস সেন্টার (${f2.ifBlank { "সার্ভিস সেন্টার" }}) অন্যায়ভাবে মেরামতের জন্য অতিরিক্ত টাকা দাবি করছে। ওয়ারেন্টির অধীনে দ্রুত বিনামূল্যে মেরামতের নির্দেশ দিন।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "My product (Serial: ${f1.ifBlank { "Serial No" }}) is under active manufacturer warranty. However, Service Centre (${f2.ifBlank { "Centre" }}) wrongfully refuses free repair. I request intervention to honor warranty terms."
                    }
                ),
                LetterTypeItem(
                    id = "consumer_overcharge",
                    titleBn = "অতিরিক্ত চার্জ কাটার বিরুদ্ধে অভিযোগ",
                    titleEn = "Excess Charging / Hidden Fees Complaint",
                    subjectBn = "এমআরপির চেয়ে অতিরিক্ত মূল্য বা অনৈতিক চার্জ নেওয়ার বিরুদ্ধে অভিযোগ",
                    subjectEn = "Complaint against charging above MRP / undisclosed fees",
                    field1LabelBn = "বিল নম্বর ও প্রতিষ্ঠানের নাম",
                    field1LabelEn = "Bill No & Merchant / Entity Name",
                    field2LabelBn = "প্রকৃত দাম বনাম আদায়কৃত টাকার বিবরণ",
                    field2LabelEn = "Actual MRP vs Overcharged Amount",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "মহাশয়, আমি ${name.ifBlank { "................" }}। বিল নং: ${f1.ifBlank { "বিল নং" }}। উক্ত প্রতিষ্ঠানটি পণ্যের গায়ে মুদ্রিত সর্বোচ্চ খুচরা মূল্যের (MRP) চেয়ে অন্যায়ভাবে অতিরিক্ত টাকা (${f2.ifBlank { "অতিরিক্ত টাকা" }}) আদায় করেছে। এই অন্যায্য বাণিজ্যিক আচরণের বিরুদ্ধে ব্যবস্থা ও টাকা ফেরতের দাবি করছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I bring to notice that Merchant (${f1.ifBlank { "Merchant" }}) charged above MRP under Bill: ${f1.ifBlank { "Bill" }}. Extra deducted: ${f2.ifBlank { "Amount" }}. This is unfair trade practice. I demand refund and corrective penalty."
                    }
                )
            )
        ),

        // 15. চাকরি ও কর্মসংস্থান (Employment / Job)
        LetterCategory(
            id = "employment_job",
            titleBn = "১৫. চাকরি ও কর্মসংস্থান (Employment / Job)",
            titleEn = "15. Employment / Job",
            defaultRecipientBn = "নিয়োগকারী আধিকারিক / এইচআর ম্যানেজার",
            defaultRecipientEn = "The Hiring Authority / HR Manager",
            letterTypes = listOf(
                LetterTypeItem(
                    id = "job_joining",
                    titleBn = "চাকরিতে যোগদান পত্র (Joining Letter)",
                    titleEn = "Joining Letter / Report of Duty",
                    subjectBn = "নিয়োগপত্র অনুযায়ী চাকরিতে যোগদানের আবেদনপত্র",
                    subjectEn = "Joining report for the post pursuant to Appointment Letter",
                    field1LabelBn = "নিয়োগপত্র মেমো নম্বর ও পদের নাম",
                    field1LabelEn = "Appointment Letter Memo & Designation",
                    field2LabelBn = "যোগদানের তারিখ (পূর্বাহ্ণ/অপরাহ্ণ)",
                    field2LabelEn = "Joining Date (Forenoon / Afternoon)",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "মহাশয়, আপনার দপ্তরের নিয়োগপত্র নং: ${f1.ifBlank { "নিয়োগপত্র তথ্য" }}-এর প্রেক্ষিতে আমি ${name.ifBlank { "................" }} অদ্য ${f2.ifBlank { "তারিখ" }} পূর্বাহ্ণে আমার নির্ধারিত পদে যোগদান করছি। আমাকে সেবায় গ্রহণ করে বাধিত করবেন।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "Respected Sir/Madam, In reference to Appointment Memo: ${f1.ifBlank { "Memo No" }}, I, ${name.ifBlank { "................" }}, report for duty on ${f2.ifBlank { "Date (FN)" }}. Kindly accept my joining report."
                    }
                ),
                LetterTypeItem(
                    id = "job_resignation",
                    titleBn = "ইস্তফাপত্র (Resignation Letter)",
                    titleEn = "Resignation Letter",
                    subjectBn = "বর্তমান পদ থেকে পদত্যাগের আবেদনপত্র",
                    subjectEn = "Resignation from current employment",
                    field1LabelBn = "কর্মচারী আইডি ও পদের নাম (Employee ID)",
                    field1LabelEn = "Employee ID & Current Designation",
                    field2LabelBn = "ইস্তফার কারণ ও শেষ কর্মদিবস",
                    field2LabelEn = "Reason & Proposed Last Working Day",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "মহাশয়, আমি ${name.ifBlank { "................" }}, কর্মচারী আইডি: ${f1.ifBlank { "আইডি" }}। একান্ত ব্যক্তিগত কারণবশত (${f2.ifBlank { "ব্যক্তিগত কারণ" }}) আমি আমার বর্তমান পদ থেকে পদত্যাগ করতে বাধ্য হচ্ছি। আমার নোটিশ পিরিয়ড বিবেচনা করে ইস্তফাপত্রটি গ্রহণ ও রিলিজ অর্ডার প্রদানের অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, Employee ID: ${f1.ifBlank { "Emp ID" }}, hereby tender my resignation due to (${f2.ifBlank { "Personal reasons" }}). Kindly accept my resignation and relieve me on ${f2.ifBlank { "Last Working Date" }}."
                    }
                ),
                LetterTypeItem(
                    id = "job_noc",
                    titleBn = "নো অবজেকশন সার্টিফিকেট (NOC) আবেদন",
                    titleEn = "No Objection Certificate (NOC)",
                    subjectBn = "উচ্চতর চাকরি/পাসপোর্টের জন্য এনওসি (NOC) প্রদানের আবেদন",
                    subjectEn = "Application for issuance of No Objection Certificate (NOC)",
                    field1LabelBn = "কর্মচারী আইডি ও বর্তমান পদ",
                    field1LabelEn = "Employee ID & Department",
                    field2LabelBn = "এনওসির উদ্দেশ্য (যেমন: পিএসসি পরীক্ষা / পাসপোর্ট)",
                    field2LabelEn = "Purpose of NOC (e.g. PSC Exam / Passport)",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, আপনার সংস্থায় কর্মরত (আইডি: ${f1.ifBlank { "আইডি" }})। আমার ${f2.ifBlank { "চাকরির পরীক্ষা/পাসপোর্ট" }}-এর আবেদনের জন্য দপ্তরের পক্ষ থেকে একটি অনাপত্তি শংসাপত্র (NOC) প্রয়োজন। শংসাপত্রটি মঞ্জুর করার প্রার্থনা জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, working as ${f1.ifBlank { "Designation & ID" }}, intend to apply for (${f2.ifBlank { "PSC Exam / Passport" }}). I request you to issue a No Objection Certificate (NOC)."
                    }
                ),
                LetterTypeItem(
                    id = "job_exp",
                    titleBn = "অভিজ্ঞতার শংসাপত্র (Experience Certificate)",
                    titleEn = "Work Experience Certificate",
                    subjectBn = "কর্ম অভিজ্ঞতার শংসাপত্র (Experience Certificate) প্রদানের আবেদন",
                    subjectEn = "Application for issuance of Service / Experience Certificate",
                    field1LabelBn = "কর্মচারী আইডি ও চাকরির মেয়াদকাল",
                    field1LabelEn = "Employee ID & Service Tenure",
                    field2LabelBn = "দায়িত্বের বিবরণ ও উদ্দেশ্য",
                    field2LabelEn = "Key Role Description & Purpose",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, আইডি: ${f1.ifBlank { "আইডি" }}। আপনার প্রতিষ্ঠানে আমি সুনামের সাথে দায়িত্ব পালন করেছি (${f2.ifBlank { "কাজের বিবরণ" }})। ভবিষ্যৎ কর্মসংস্থানের স্বার্থে আমার একটি কাজের অভিজ্ঞতার শংসাপত্র প্রদান করে বাধিত করবেন।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I was employed with your organization (Emp ID: ${f1.ifBlank { "Emp ID" }}). For prospective professional opportunities, I request you to kindly issue my formal Experience Certificate detailing my tenure and designation."
                    }
                )
            )
        ),

        // 16. বিমা (Insurance / LIC)
        LetterCategory(
            id = "insurance_lic",
            titleBn = "১৬. বিমা (Insurance / LIC)",
            titleEn = "16. Insurance / LIC",
            defaultRecipientBn = "শাখা ব্যবস্থাপক (Branch Manager, LIC / Insurance)",
            defaultRecipientEn = "The Branch Manager (LIC / Insurance Co.)",
            letterTypes = listOf(
                LetterTypeItem(
                    id = "ins_claim",
                    titleBn = "জীবন বা স্বাস্থ্য বিমার ক্লেম (Claim) আবেদন",
                    titleEn = "Insurance Policy Claim Settlement",
                    subjectBn = "বিমা পলিসির দাবি (Claim Settlement) নিষ্পত্তির আবেদন",
                    subjectEn = "Application for settlement of Insurance Policy Claim",
                    field1LabelBn = "পলিসি নম্বর ও বিমাকারীর নাম",
                    field1LabelEn = "Policy Number & Insured Name",
                    field2LabelBn = "ক্লেমের ধরন ও কারণ (যেমন: চিকিৎসা / মেয়াদপূর্তি)",
                    field2LabelEn = "Claim Type & Occurrence Date",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। পলিসি নং: ${f1.ifBlank { "পলিসি নং" }}। পলিসির শর্তানুযায়ী (${f2.ifBlank { "মেয়াদপূর্তি/চিকিৎসা" }}) ক্লেমের স্বপক্ষে মূল পলিসি বন্ড ও প্রয়োজনীয় নথি জমা দিলাম। দ্রুত ক্লেমের অর্থ আমার ব্যাংক অ্যাকাউন্টে জমা করার অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I hold Policy No: ${f1.ifBlank { "Policy No" }}. In accordance with terms regarding (${f2.ifBlank { "Maturity / Hospitalization" }}), I submit the claim papers and discharge voucher. Kindly expedite settlement."
                    }
                ),
                LetterTypeItem(
                    id = "ins_surrender",
                    titleBn = "পলিসি সারেন্ডার বা বন্ধের আবেদন",
                    titleEn = "Policy Surrender / Foreclosure",
                    subjectBn = "বিমা পলিসি সারেন্ডার ও মূল্য উত্তোলনের আবেদন",
                    subjectEn = "Application for policy surrender and release of surrender value",
                    field1LabelBn = "পলিসি নম্বর ও প্রিমিয়াম পরিশোধের বছর",
                    field1LabelEn = "Policy No & Years of Premium Paid",
                    field2LabelBn = "সারেন্ডার করার আর্থিক কারণ",
                    field2LabelEn = "Reason for Surrender",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। পলিসি নং: ${f1.ifBlank { "পলিসি নং" }}। চরম আর্থিক টানাপোড়েনের কারণে (${f2.ifBlank { "আর্থিক সংকট" }}) আমি আর নিয়মিত প্রিমিয়াম চালাতে পারছি না। অতএব, পলিসি সারেন্ডার ভ্যালু হিসাবপূর্বক অর্থ প্রদানের অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I maintain Policy No: ${f1.ifBlank { "Policy No" }}. Due to unforeseen financial constraints (${f2.ifBlank { "Financial hardship" }}), I cannot continue premium payments. Please calculate and disburse the surrender value."
                    }
                ),
                LetterTypeItem(
                    id = "ins_nominee",
                    titleBn = "পলিসিতে নমিনি বা ঠিকানা পরিবর্তন",
                    titleEn = "Nominee / Address Change in Policy",
                    subjectBn = "পলিসি নথিতে নতুন নমিনি ও যোগাযোগের ঠিকানা আপডেটের আবেদন",
                    subjectEn = "Application for recording change of nominee / address in policy",
                    field1LabelBn = "পলিসি নম্বর ও গ্রাহকের মোবাইল",
                    field1LabelEn = "Policy No & Registered Mobile",
                    field2LabelBn = "নতুন নমিনির নাম, সম্পর্ক ও ঠিকানা",
                    field2LabelEn = "New Nominee Name, Relation & Address",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। পলিসি নং: ${f1.ifBlank { "পলিসি নং" }}। আমি আমার পলিসিতে নতুন নমিনি হিসেবে ${f2.ifBlank { "নমিনি বিবরণ" }}-কে অন্তর্ভুক্ত করতে ইচ্ছুক। প্রয়োজনীয় সংশোধিত তথ্য রেকর্ড করার আবেদন জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I am ${name.ifBlank { "................" }}, Policyholder under No: ${f1.ifBlank { "Policy No" }}. I wish to alter the nomination to: ${f2.ifBlank { "Nominee Details" }}. Please update records and endorse my policy bond."
                    }
                ),
                LetterTypeItem(
                    id = "ins_receipt",
                    titleBn = "প্রিমিয়াম রশিদ বা তথ্য আপডেট আবেদন",
                    titleEn = "Duplicate Premium Receipt / Certificate",
                    subjectBn = "প্রিমিয়াম পেমেন্ট রশিদ ও আইটি সার্টিফিকেটের আবেদন",
                    subjectEn = "Application for duplicate premium paid certificate for Income Tax",
                    field1LabelBn = "পলিসি নম্বর ও আর্থিক বছর",
                    field1LabelEn = "Policy No & Financial Year",
                    field2LabelBn = "জমা দেওয়া প্রিমিয়ামের পরিমাণ",
                    field2LabelEn = "Paid Premium Amount & Mode",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। পলিসি নং: ${f1.ifBlank { "পলিসি নং" }}। আয়কর রিবেট পাওয়ার জন্য বিগত আর্থিক বছরের প্রিমিয়াম পেইড সার্টিফিকেট (IT Certificate) প্রয়োজন। অবিলম্বে শংসাপত্রটি ইস্যু করার অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I hold Policy No: ${f1.ifBlank { "Policy No" }}. For Income Tax rebate under 80C, I require the Premium Paid Certificate for FY: ${f1.ifBlank { "FY" }}. Kindly issue the certificate."
                    }
                )
            )
        ),

        // 17. বিশ্ববিদ্যালয় ও পরীক্ষা (University & Examination)
        LetterCategory(
            id = "university_exam",
            titleBn = "১৭. বিশ্ববিদ্যালয় ও পরীক্ষা (University & Examination)",
            titleEn = "17. University & Examination",
            defaultRecipientBn = "রেজিস্ট্রার / পরীক্ষা নিয়ন্ত্রক (Registrar / Controller of Exams)",
            defaultRecipientEn = "The Registrar / Controller of Examinations",
            letterTypes = listOf(
                LetterTypeItem(
                    id = "univ_migration",
                    titleBn = "মাইগ্রেশন সার্টিফিকেট আবেদন",
                    titleEn = "Migration Certificate Application",
                    subjectBn = "অন্যান্য বিশ্ববিদ্যালয়ে ভর্তির জন্য মাইগ্রেশন সার্টিফিকেটের আবেদন",
                    subjectEn = "Application for issuance of Migration Certificate",
                    field1LabelBn = "রেজিস্ট্রেশন নম্বর ও পাসের বছর",
                    field1LabelEn = "Registration No & Passing Year",
                    field2LabelBn = "নতুন বিশ্ববিদ্যালয়ের নাম ও কোর্স",
                    field2LabelEn = "New University & Course Name",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। রেজিস্ট্রেশন নং: ${f1.ifBlank { "রেজিস্ট্রেশন নং" }}। আমি অন্য প্রতিষ্ঠানে উচ্চশিক্ষায় (${f2.ifBlank { "বিশ্ববিদ্যালয়" }}) ভর্তি হতে ইচ্ছুক। প্রয়োজনীয় ফি সহ মাইগ্রেশন সার্টিফিকেট প্রদানের প্রার্থনা জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I completed my graduation under Reg No: ${f1.ifBlank { "Reg No" }}. Having secured admission at ${f2.ifBlank { "New University" }}, I request you to kindly issue my Migration Certificate."
                    }
                ),
                LetterTypeItem(
                    id = "univ_duplicate_degree",
                    titleBn = "ডুপ্লিকেট ডিগ্রি বা ডিপ্লোমা আবেদন",
                    titleEn = "Duplicate Degree / Diploma Certificate",
                    subjectBn = "হারানো মূল ডিগ্রি সার্টিফিকেটের ডুপ্লিকেট কপির আবেদন",
                    subjectEn = "Application for duplicate Degree Certificate due to loss",
                    field1LabelBn = "রোল নম্বর ও রেজিস্ট্রেশন নম্বর",
                    field1LabelEn = "Roll No & Registration No",
                    field2LabelBn = "ডিগ্রির নাম ও থানা জিডি নম্বর",
                    field2LabelEn = "Degree Name & Police GD No",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। ডিগ্রি: ${f2.ifBlank { "ডিগ্রি" }}, রেজিস্ট্রেশন নং: ${f1.ifBlank { "রেজিস্ট্রেশন" }}। আমার মূল ডিগ্রি শংসাপত্রটি হারিয়ে গেছে। থানা জিডি ও হলফনামা সহ ডুপ্লিকেট ডিগ্রি ইস্যু করার অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I graduated under Reg No: ${f1.ifBlank { "Reg No" }}. My original degree certificate was lost (GD: ${f2.ifBlank { "GD No" }}). Requisite indemnity affidavit and fees are enclosed. Kindly issue duplicate certificate."
                    }
                ),
                LetterTypeItem(
                    id = "univ_review",
                    titleBn = "পরীক্ষার খাতা পুনঃমূল্যায়ন (Review)",
                    titleEn = "Post Publication Review (PPR)",
                    subjectBn = "সেমিস্টার পরীক্ষার উত্তরপত্র পুনর্মূল্যায়নের আবেদন",
                    subjectEn = "Application for Post Publication Review (PPR) of answer scripts",
                    field1LabelBn = "রোল নম্বর ও সেমিস্টার",
                    field1LabelEn = "Roll No & Semester",
                    field2LabelBn = "পেপার কোড ও বিষয়ের নাম",
                    field2LabelEn = "Paper Code & Subject Name",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, রোল নং: ${f1.ifBlank { "রোল" }}। সম্প্রতি প্রকাশিত ফলাফলে পেপার: ${f2.ifBlank { "পেপার কোড" }}-এ প্রত্যাশার তুলনায় কম নম্বর পেয়েছি। পরীক্ষার খাতাটি স্ক্রুটিনি/রিভিউ করার বিনীত প্রার্থনা জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, appeared in semester exam under Roll: ${f1.ifBlank { "Roll" }}. I seek review of Paper: ${f2.ifBlank { "Paper Code" }}. Requisite scrutiny fee has been remitted online."
                    }
                ),
                LetterTypeItem(
                    id = "univ_backlog",
                    titleBn = "ব্যাকলগ বা সাপ্লিমেন্টারি পরীক্ষার আবেদন",
                    titleEn = "Supplementary / Backlog Examination",
                    subjectBn = "সাপ্লিমেন্টারি / ব্যাকলগ পরীক্ষায় বসার অনুমতির আবেদন",
                    subjectEn = "Application for appearing in Backlog / Supplementary Examination",
                    field1LabelBn = "রোল নম্বর ও ব্যাকলগ সেমিস্টার",
                    field1LabelEn = "Roll No & Backlog Semester",
                    field2LabelBn = "বকেয়া পেপারগুলির নাম",
                    field2LabelEn = "Backlog Paper Codes",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। রোল নং: ${f1.ifBlank { "রোল" }}। অসুস্থতার কারণে পূর্বে উত্তীর্ণ হতে না পারা পেপারগুলিতে (${f2.ifBlank { "পেপার কোড" }}) সাপ্লিমেন্টারি পরীক্ষায় বসার আবেদন মঞ্জুর করার অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, Roll: ${f1.ifBlank { "Roll" }}, request permission to clear pending backlogs in Paper: ${f2.ifBlank { "Papers" }}. Application form and fees are submitted."
                    }
                )
            )
        ),

        // 18. সাইবার অভিযোগ (Cyber Crime / Complaint)
        LetterCategory(
            id = "cyber_crime",
            titleBn = "১৮. সাইবার অভিযোগ (Cyber Crime / Complaint)",
            titleEn = "18. Cyber Crime / Complaint",
            defaultRecipientBn = "ভারপ্রাপ্ত আধিকারিক (Cyber Crime Police Station / Cell)",
            defaultRecipientEn = "The Officer-in-Charge, Cyber Crime Police Station",
            letterTypes = listOf(
                LetterTypeItem(
                    id = "cyber_upi",
                    titleBn = "অনলাইন ব্যাংকিং বা UPI প্রতারণার অভিযোগ",
                    titleEn = "Online Banking / UPI Fraud Complaint",
                    subjectBn = "অনলাইন ওটিপি / ইউপিআই আর্থিক প্রতারণার বিরুদ্ধে অভিযোগ",
                    subjectEn = "Complaint regarding fraudulent online UPI / Net Banking financial theft",
                    field1LabelBn = "লেনদেনের পরিমাণ ও UTR / ট্রানজ্যাকশন আইডি",
                    field1LabelEn = "Debited Amount & UTR / Txn ID",
                    field2LabelBn = "প্রতারকের মোবাইল/ইউপিআই ও ঘটনার তারিখ",
                    field2LabelEn = "Fraudster Mobile/UPI & Incident Date",
                    bodyTemplateBn = { name, addr, _, f1, f2 ->
                        "মহাশয়, আমি ${name.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}। গত ${f2.ifBlank { "তারিখে" }} প্রতারক চক্র কৌশলে আমার ব্যাংক থেকে ${f1.ifBlank { "টাকা ও UTR তথ্য" }} অননুমোদিতভাবে কেটে নিয়েছে। অপরাধীর অ্যাকাউন্ট ফ্রিজ করে আমার অর্থ উদ্ধার ও আইনানুগ ব্যবস্থার দাবি জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, addr, _, f1, f2 ->
                        "Sir, I, ${name.ifBlank { "................" }}, resident of ${addr.ifBlank { "................" }}, fell victim to online banking fraud on ${f2.ifBlank { "Date & Suspect details" }}, losing ${f1.ifBlank { "Amount & UTR" }}. Kindly freeze the recipient account and initiate investigation."
                    }
                ),
                LetterTypeItem(
                    id = "cyber_hacking",
                    titleBn = "সোশ্যাল মিডিয়া অ্যাকাউন্ট হ্যাকিং অভিযোগ",
                    titleEn = "Social Media Account Hacking",
                    subjectBn = "সোশ্যাল মিডিয়া অ্যাকাউন্ট হ্যাকিং ও অপব্যবহারের বিরুদ্ধে অভিযোগ",
                    subjectEn = "Complaint regarding hacking and misuse of personal social media account",
                    field1LabelBn = "অ্যাকাউন্টের ধরন ও ইউজারনেম (e.g. FB / Insta)",
                    field1LabelEn = "Platform & Username / Profile URL",
                    field2LabelBn = "ঘটনার তারিখ ও অপব্যবহারের প্রমাণ",
                    field2LabelEn = "Incident Date & Misuse Screenshot Ref",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "মহাশয়, আমি ${name.ifBlank { "................" }}। আমার সোশ্যাল মিডিয়া প্রোফাইল (${f1.ifBlank { "ইউজারনেম" }}) হ্যাকাররা হ্যাক করে বন্ধুদের নিকট বিভ্রান্তিকর মেসেজ পাঠাচ্ছে (${f2.ifBlank { "তারিখ" }})। অ্যাকাউন্টটির অ্যাক্সেস বন্ধ ও হ্যাকারদের শনাক্তকরণের অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I report that my profile (${f1.ifBlank { "Platform & Handle" }}) has been illegally breached on ${f2.ifBlank { "Date" }}. The hacker is impersonating me. I request you to block unauthorized access and trace the IP."
                    }
                ),
                LetterTypeItem(
                    id = "cyber_harassment",
                    titleBn = "ব্ল্যাকমেলিং বা আপত্তিকর মেসেজের অভিযোগ",
                    titleEn = "Online Blackmail / Cyber Bullying",
                    subjectBn = "ডিজিটাল ব্ল্যাকমেলিং ও আপত্তিকর ছবি/মেসেজের বিরুদ্ধে অভিযোগ",
                    subjectEn = "Complaint against cyber blackmailing and morphed content",
                    field1LabelBn = "প্রতারকের ব্যবহৃত মোবাইল নম্বর বা আইডি",
                    field1LabelEn = "Perpetrator Mobile Number / Handle",
                    field2LabelBn = "হুমকি ও অর্থ দাবির বিবরণ",
                    field2LabelEn = "Nature of Threat & Extortion demand",
                    bodyTemplateBn = { name, addr, _, f1, f2 ->
                        "মহাশয়, আমি ${name.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}। অভিযুক্ত নম্বর: ${f1.ifBlank { "নম্বর" }} থেকে আমাকে অশ্লীল ছবি এডিট করে ব্ল্যাকমেল করা হচ্ছে ও অর্থ দাবি করা হচ্ছে (${f2.ifBlank { "হুমকি" }})। অবিলম্বে দুষ্কৃতীদের বিরুদ্ধে কড়া ব্যবস্থা গ্রহণের আবেদন জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, addr, _, f1, f2 ->
                        "I am being subjected to criminal extortion from contact: ${f1.ifBlank { "Number" }}. The perpetrator is threatening to circulate morphed pictures unless ransom is paid (${f2.ifBlank { "Threat" }}). Please register FIR under IT Act."
                    }
                ),
                LetterTypeItem(
                    id = "cyber_sim",
                    titleBn = "অননুমোদিত সিম কার্ড তোলার অভিযোগ",
                    titleEn = "Unauthorized SIM Issuance Complaint",
                    subjectBn = "আমার আধার নম্বর দিয়ে অননুমোদিত সিম কার্ড তোলার বিরুদ্ধে অভিযোগ",
                    subjectEn = "Complaint regarding unauthorized SIM cards issued using my Aadhaar",
                    field1LabelBn = "আধার নম্বর ও অননুমোদিত মোবাইল নম্বরগুলি",
                    field1LabelEn = "Aadhaar No & Unauthorized Numbers",
                    field2LabelBn = "টেলিকম অপারেটরের নাম",
                    field2LabelEn = "Telecom Service Provider Name",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "মহাশয়, আমি ${name.ifBlank { "................" }}। পোর্টাল মারফত জানতে পারলাম যে আমার আধার নম্বর দিয়ে অজ্ঞাত ব্যক্তিরা অবৈধভাবে সিম কার্ড (${f1.ifBlank { "সিম নম্বর" }}) তুলেছে। কোনো অপরাধমূলক কাজে ব্যবহারের পূর্বে উক্ত ভুয়া সিমগুলি অবিলম্বে ব্লক করার অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I discovered that multiple mobile numbers (${f1.ifBlank { "Numbers" }}) were fraudulently issued on my Aadhaar through operator ${f2.ifBlank { "Operator" }}. I disown these numbers and request immediate deactivation."
                    }
                )
            )
        ),

        // 19. অফিস ও কর্মক্ষেত্র (Office / Workplace)
        LetterCategory(
            id = "office_workplace",
            titleBn = "১৯. অফিস ও কর্মক্ষেত্র (Office / Workplace)",
            titleEn = "19. Office / Workplace",
            defaultRecipientBn = "বিভাগীয় প্রধান / ম্যানেজার (Head of Department / Director)",
            defaultRecipientEn = "The Head of Department / Managing Director",
            letterTypes = listOf(
                LetterTypeItem(
                    id = "office_advance",
                    titleBn = "অগ্রিম বেতন বা খরচের আবেদন",
                    titleEn = "Salary Advance / Tour Expense Request",
                    subjectBn = "জরুরি পারিবারিক প্রয়োজনে অগ্রিম বেতন বরাদ্দের আবেদন",
                    subjectEn = "Application for sanction of salary advance / tour allowance",
                    field1LabelBn = "কর্মচারী আইডি ও পদের নাম",
                    field1LabelEn = "Employee ID & Designation",
                    field2LabelBn = "প্রার্থিত অগ্রিম টাকার পরিমাণ ও কারণ",
                    field2LabelEn = "Requested Advance Amount & Reason",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "মহাশয়, আমি ${name.ifBlank { "................" }}, আইডি: ${f1.ifBlank { "আইডি" }}। জরুরি পারিবারিক সংকটের কারণে (${f2.ifBlank { "চিকিৎসা/জরুরি কারণ" }}) আমার অগ্রিম বেতনের প্রয়োজন। মাসিক কিস্তিতে বেতন থেকে সমন্বয় সাপেক্ষে অগ্রিম মঞ্জুর করার প্রার্থনা জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, Emp ID: ${f1.ifBlank { "Emp ID" }}, request an advance of ${f2.ifBlank { "Amount" }} due to urgent personal obligations. The amount may be recovered in convenient monthly installments."
                    }
                ),
                LetterTypeItem(
                    id = "office_transfer",
                    titleBn = "অভ্যন্তরীণ বদলি (Transfer) আবেদন",
                    titleEn = "Internal Transfer Application",
                    subjectBn = "অন্যান্য শাখা/বিভাগে অভ্যন্তরীণ বদলির আবেদন",
                    subjectEn = "Application for internal departmental / branch transfer",
                    field1LabelBn = "বর্তমান শাখা ও কর্মচারী আইডি",
                    field1LabelEn = "Current Branch & Employee ID",
                    field2LabelBn = "কাঙ্ক্ষিত নতুন শাখা ও বদলির কারণ",
                    field2LabelEn = "Desired Branch & Grounds for transfer",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "মহাশয়, আমি ${name.ifBlank { "................" }}, আইডি: ${f1.ifBlank { "আইডি" }}। পিতা-মাতার অসুস্থতা ও দূরত্বের কারণে (${f2.ifBlank { "কারণ" }}) আমাকে ${f2.ifBlank { "কাঙ্ক্ষিত শাখা" }}-য় অভ্যন্তরীণ বদলি করার বিনীত অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, serving at ${f1.ifBlank { "Current Branch" }}, request an internal transfer to ${f2.ifBlank { "Preferred Branch" }} on compassionate grounds (${f2.ifBlank { "Grounds" }})."
                    }
                ),
                LetterTypeItem(
                    id = "office_equipment",
                    titleBn = "দাপ্তরিক সামগ্রী বরাদ্দের আবেদন",
                    titleEn = "Office Equipment / Laptop Requisition",
                    subjectBn = "অফিস কাজের সুবিধার্থে ল্যাপটপ/সরঞ্জাম বরাদ্দের আবেদন",
                    subjectEn = "Requisition for allotment of office laptop / workstation assets",
                    field1LabelBn = "কর্মচারী আইডি ও প্রকল্পের নাম",
                    field1LabelEn = "Employee ID & Project Name",
                    field2LabelBn = "প্রয়োজনীয় সামগ্রীর বিবরণ",
                    field2LabelEn = "Required Equipment Details",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "মহাশয়, আমি ${name.ifBlank { "................" }}, আইডি: ${f1.ifBlank { "আইডি" }}। অফিশিয়াল প্রজেক্টের কাজের সুষ্ঠু সম্পাদনের জন্য আমার (${f2.ifBlank { "ল্যাপটপ/সরঞ্জাম" }}) বরাদ্দের অত্যন্ত প্রয়োজন। প্রয়োজনীয় সামগ্রী প্রদান করে বাধিত করবেন।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "For project operations under ${f1.ifBlank { "Project" }}, I request requisition of ${f2.ifBlank { "Laptop / Assets" }}. Kindly approve the equipment requisition."
                    }
                ),
                LetterTypeItem(
                    id = "office_permission",
                    titleBn = "বিশেষ কাজের অনুমতি পত্র",
                    titleEn = "Special Permission / Duty Pass",
                    subjectBn = "বিশেষ অফিশিয়াল দায়িত্ব পালনের অনুমতির আবেদন",
                    subjectEn = "Application for special duty permission / outstation travel",
                    field1LabelBn = "কর্মচারী আইডি ও অনুমোদনের ক্ষেত্র",
                    field1LabelEn = "Employee ID & Duty Nature",
                    field2LabelBn = "তারিখ ও সময়কাল",
                    field2LabelEn = "Date & Duration of permission",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "মহাশয়, আমি ${name.ifBlank { "................" }}, আইডি: ${f1.ifBlank { "আইডি" }}। অফিসের বিশেষ প্রয়োজনে (${f2.ifBlank { "সময়কাল" }}) নির্দিষ্ট দায়িত্ব পালনের জন্য আমার দাপ্তরিক অনুমতির প্রয়োজন। প্রয়োজনীয় অনুমতি প্রদানের আবেদন জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I, ${name.ifBlank { "................" }}, seek official permission for special assignment on ${f2.ifBlank { "Dates" }}. Kindly accord the necessary sanction."
                    }
                )
            )
        ),

        // 20. আবাসন ও ফ্ল্যাট (Housing / Society)
        LetterCategory(
            id = "housing_society",
            titleBn = "২০. আবাসন ও ফ্ল্যাট (Housing / Society)",
            titleEn = "20. Housing / Society",
            defaultRecipientBn = "সম্পাদক মহাশয় (The Secretary, Housing Society / RWA)",
            defaultRecipientEn = "The Secretary, Housing Society / RWA",
            letterTypes = listOf(
                LetterTypeItem(
                    id = "house_maintenance",
                    titleBn = "সোসাইটি মেইনটেনেন্স সমস্যা সমাধান",
                    titleEn = "Society Maintenance Grievance",
                    subjectBn = "ফ্ল্যাট মেইনটেনেন্স ও কমন সার্ভিস মেরামতের আবেদন",
                    subjectEn = "Grievance regarding apartment maintenance / lift / seepage",
                    field1LabelBn = "ফ্ল্যাট নম্বর ও ব্লক নম্বর",
                    field1LabelEn = "Flat Number & Block Number",
                    field2LabelBn = "সমস্যার বিবরণ (যেমন: লিফট বন্ধ / জল লিকেজ)",
                    field2LabelEn = "Nature of Maintenance Issue",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "মহাশয়, আমি ফ্ল্যাট নং: ${f1.ifBlank { "ফ্ল্যাট নং" }}-এর বাসিন্দা ${name.ifBlank { "................" }}। আমাদের ব্লকে দীর্ঘদিন ধরে (${f2.ifBlank { "সমস্যার বিবরণ" }}) সৃষ্টি হয়েছে। নিয়মিত মেইনটেনেন্স ফি দেওয়া সত্ত্বেও সমস্যার সমাধান হয়নি। অবিলম্বে মেরামতের ব্যবস্থা করুন।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I reside in Flat No: ${f1.ifBlank { "Flat No" }}. We are facing persistent issues with (${f2.ifBlank { "Issue" }}). Despite paying maintenance dues, the fault is unresolved. Kindly deploy repair personnel."
                    }
                ),
                LetterTypeItem(
                    id = "house_renovation",
                    titleBn = "ফ্ল্যাট রেনোভেশন বা পার্কিং অনুমতি",
                    titleEn = "Flat Renovation / Parking Permission",
                    subjectBn = "ফ্ল্যাট অভ্যন্তরীণ সংস্কার / পার্কিং ব্যবহারের অনুমতির আবেদন",
                    subjectEn = "Application for permission for apartment interior renovation / parking",
                    field1LabelBn = "ফ্ল্যাট নম্বর ও গাড়ির নম্বর",
                    field1LabelEn = "Flat No & Vehicle Registration No",
                    field2LabelBn = "সংস্কারের সময়কাল ও কাজের প্রকৃতি",
                    field2LabelEn = "Renovation Duration & Nature of Work",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "মহাশয়, আমি ${name.ifBlank { "................" }}, ফ্ল্যাট নং: ${f1.ifBlank { "ফ্ল্যাট" }}। আমার ফ্ল্যাটে অভ্যন্তরীণ সংস্কারের কাজ (${f2.ifBlank { "তারিখ" }}) করতে ইচ্ছুক। সোসাইটির নিয়মাবলী মেনে কাজ সম্পন্ন করার নিশ্চয়তা দিয়ে অনুমতি প্রদানের আবেদন জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I am ${name.ifBlank { "................" }}, owner of Flat: ${f1.ifBlank { "Flat" }}. I plan interior renovation from ${f2.ifBlank { "Dates" }}. Society guidelines on noise and debris will be strictly respected. Kindly grant permission."
                    }
                ),
                LetterTypeItem(
                    id = "house_sale_noc",
                    titleBn = "ফ্ল্যাট বিক্রির জন্য NOC আবেদন",
                    titleEn = "NOC for Sale / Transfer of Flat",
                    subjectBn = "ফ্ল্যাট হস্তান্তর বা বিক্রির জন্য অনাপত্তি শংসাপত্র (NOC)",
                    subjectEn = "Application for No Objection Certificate (NOC) for sale / transfer of flat",
                    field1LabelBn = "ফ্ল্যাট নম্বর ও ক্রেতার নাম",
                    field1LabelEn = "Flat No & Prospective Purchaser",
                    field2LabelBn = "মেইনটেনেন্স নো-ডিউস স্ট্যাটাস",
                    field2LabelEn = "Society Dues Clearance Status",
                    bodyTemplateBn = { name, _, _, f1, f2 ->
                        "মহাশয়, আমি ${name.ifBlank { "................" }}। ফ্ল্যাট নং: ${f1.ifBlank { "ফ্ল্যাট নং" }}। আমি উক্ত ফ্ল্যাটটি বিক্রয় করতে ইচ্ছুক। আমার সমস্ত বকেয়া মেইনটেনেন্স ফি পরিশোধিত রয়েছে। হস্তান্তর প্রক্রিয়ার সুবিধার্থে সোসাইটির পক্ষ থেকে এনওসি (NOC) প্রদানের অনুরোধ জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, _, _, f1, f2 ->
                        "I own Flat No: ${f1.ifBlank { "Flat No" }} and intend to transfer it to ${f1.ifBlank { "Purchaser" }}. All society dues are cleared. I request you to issue the society No Objection Certificate (NOC)."
                    }
                )
            )
        ),

        // 21. অন্যান্য ও সাধারণ দরখাস্ত (Other / General)
        LetterCategory(
            id = "other_general",
            titleBn = "২১. অন্যান্য ও সাধারণ দরখাস্ত (Other / General)",
            titleEn = "21. Other / General",
            defaultRecipientBn = "যথাযোগ্য কর্তৃপক্ষ (The Competent Authority)",
            defaultRecipientEn = "The Competent Authority",
            letterTypes = listOf(
                LetterTypeItem(
                    id = "gen_institutional",
                    titleBn = "সাধারণ প্রাতিষ্ঠানিক দরখাস্ত",
                    titleEn = "General Institutional Application",
                    subjectBn = "প্রশাসনিক বিবেচনা ও প্রয়োজনীয় ব্যবস্থা গ্রহণের আবেদন",
                    subjectEn = "Application for administrative consideration and appropriate action",
                    field1LabelBn = "বিষয়বস্তুর মূল শিরোনাম / Ref Title",
                    field1LabelEn = "Core Subject / Ref Title",
                    field2LabelBn = "আবেদনের মূল বিবরণ বা নথি নম্বর",
                    field2LabelEn = "Main Context / Reference No",
                    bodyTemplateBn = { name, addr, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}। আপনার সদয় অবগতির জন্য জানাচ্ছি যে, ${f1.ifBlank { "বিষয়ের শিরোনাম" }} সংক্রান্ত বিষয়ে (${f2.ifBlank { "মূল বিবরণ" }}) আমি প্রয়োজনীয় সহায়তার জন্য আবেদন করছি। বিষয়টি বিবেচনা করে যথোপযুক্ত ব্যবস্থা গ্রহণে বাধিত করবেন।"
                    },
                    bodyTemplateEn = { name, addr, _, f1, f2 ->
                        "Most respectfully, I, ${name.ifBlank { "................" }}, residing at ${addr.ifBlank { "................" }}, submit this representation regarding: '${f1.ifBlank { "Subject" }}' (${f2.ifBlank { "Details" }}). I request you to kindly examine the matter and initiate appropriate action."
                    }
                ),
                LetterTypeItem(
                    id = "gen_community",
                    titleBn = "স্থানীয় সমস্যার প্রতিকার চেয়ে আবেদন",
                    titleEn = "Community / Civic Grievance",
                    subjectBn = "এলাকার সামাজিক ও নাগরিক সমস্যার প্রতিকার চেয়ে আবেদন",
                    subjectEn = "Representation seeking redressal of public and civic nuisance",
                    field1LabelBn = "এলাকা ও সমস্যার ধরন (যেমন: শব্দদূষণ / বাতি)",
                    field1LabelEn = "Locality & Nuisance Type (e.g. Streetlight)",
                    field2LabelBn = "কতদিন ধরে সমস্যা ও প্রভাব",
                    field2LabelEn = "Duration & Impact on Community",
                    bodyTemplateBn = { name, addr, _, f1, f2 ->
                        "মহাশয়, আমরা ${addr.ifBlank { "এলাকাবাসী" }} স্থানীয় বাসিন্দাগণ। আমাদের অঞ্চলে (${f1.ifBlank { "সমস্যার বিবরণ" }}) বিগত ${f2.ifBlank { "সময়কাল" }} ধরে চরম সংকট সৃষ্টি করছে। জনসাধারণের স্বার্থে উক্ত সমস্যার দ্রুত স্থায়ী প্রতিকার করার বিনীত প্রার্থনা জানাচ্ছি।"
                    },
                    bodyTemplateEn = { name, addr, _, f1, f2 ->
                        "On behalf of local residents of ${addr.ifBlank { "Locality" }}, I bring to your notice the civic grievance concerning (${f1.ifBlank { "Civic Problem" }}), prevailing since ${f2.ifBlank { "Duration" }}. Prompt intervention is earnestly solicited."
                    }
                ),
                LetterTypeItem(
                    id = "gen_custom",
                    titleBn = "কাস্টম আবেদনপত্র",
                    titleEn = "Custom / Free-form Application",
                    subjectBn = "বিশেষ বিষয়ের জন্য আনুষ্ঠানিক আবেদনপত্র",
                    subjectEn = "Formal application regarding specified subject matter",
                    field1LabelBn = "আবেদনের বিষয়বস্তু / Specific Subject",
                    field1LabelEn = "Specific Subject Matter",
                    field2LabelBn = "মূল বক্তব্যের সারাংশ / Key Body Points",
                    field2LabelEn = "Core Statement / Key Points",
                    bodyTemplateBn = { name, addr, _, f1, f2 ->
                        "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}। উক্ত বিষয়ের (${f1.ifBlank { "বিষয়বস্তু" }}) প্রেক্ষিতে জানাচ্ছি যে, ${f2.ifBlank { "বক্তব্য" }}। অতএব, প্রার্থনা এই যে, বিষয়টি মানবিকভাবে বিবেচনা করে প্রয়োজনীয় আদেশ প্রদানে বাধিত করবেন।"
                    },
                    bodyTemplateEn = { name, addr, _, f1, f2 ->
                        "I beg to state that I, ${name.ifBlank { "................" }}, resident of ${addr.ifBlank { "................" }}, represent the matter regarding (${f1.ifBlank { "Matter" }}). Context: ${f2.ifBlank { "Core Points" }}. I request you to kindly pass favorable orders."
                    }
                )
            )
        )
    )
}
