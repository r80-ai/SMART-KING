package com.example.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.print.PrintAttributes
import android.print.PrintManager
import android.speech.RecognizerIntent
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.graphics.asImageBitmap
import com.example.ui.components.SignatureUploadSection
import com.example.ui.components.toBase64Png
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data model for Application Categories
data class LetterCategory(
    val id: String,
    val titleBn: String,
    val titleEn: String,
    val defaultRecipientBn: String,
    val defaultRecipientEn: String,
    val letterTypes: List<LetterTypeItem>
)

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationMakerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var isBengali by remember { mutableStateOf(true) }

    // Categories and Letter Types Repository
    val categories = remember {
        listOf(
            LetterCategory(
                id = "bank",
                titleBn = "ব্যাংক (Bank)",
                titleEn = "Bank",
                defaultRecipientBn = "শাখা ব্যবস্থাপক (Branch Manager)",
                defaultRecipientEn = "The Branch Manager",
                letterTypes = listOf(
                    LetterTypeItem(
                        id = "bank_mobile",
                        titleBn = "মোবাইল নম্বর পরিবর্তন",
                        titleEn = "Mobile Number Update",
                        subjectBn = "মোবাইল নম্বর পরিবর্তনের আবেদন",
                        subjectEn = "Application for mobile number update",
                        field1LabelBn = "অ্যাকাউন্ট নম্বর / Account No",
                        field1LabelEn = "Account Number",
                        field2LabelBn = "নতুন মোবাইল নম্বর / New Mobile No",
                        field2LabelEn = "New Mobile Number",
                        bodyTemplateBn = { name, _, _, f1, f2 ->
                            "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। আপনার ব্যাংকে আমার একটি সেভিংস অ্যাকাউন্ট আছে, যার নম্বর ${f1.ifBlank { "................" }}। উক্ত অ্যাকাউন্টের সঙ্গে যুক্ত আমার পূর্ববর্তী মোবাইল নম্বরটি পরিবর্তন করে নতুন নম্বর ${f2.ifBlank { "................" }} নথিভুক্ত (Update) করার জন্য বিনীত অনুরোধ জানাচ্ছি।"
                        },
                        bodyTemplateEn = { name, _, _, f1, f2 ->
                            "I beg to state that I am ${name.ifBlank { "................" }}, holding a Savings Bank Account No. ${f1.ifBlank { "................" }} in your branch. I kindly request you to update my registered mobile number to ${f2.ifBlank { "................" }} in my account records."
                        }
                    ),
                    LetterTypeItem(
                        id = "bank_passbook",
                        titleBn = "নতুন পাসবই প্রাপ্তির আবেদন",
                        titleEn = "New Passbook Request",
                        subjectBn = "নতুন পাসবুক ইস্যু করার জন্য আবেদন",
                        subjectEn = "Application for issue of new passbook",
                        field1LabelBn = "অ্যাকাউন্ট নম্বর / Account No",
                        field1LabelEn = "Account Number",
                        field2LabelBn = "মোবাইল নম্বর / Mobile No",
                        field2LabelEn = "Mobile Number",
                        bodyTemplateBn = { name, _, _, f1, f2 ->
                            "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। আপনার শাখায় আমার সেভিংস অ্যাকাউন্ট নম্বর ${f1.ifBlank { "................" }}। আমার পূর্বের পাসবুকটির পৃষ্ঠা শেষ হয়ে যাওয়ায় / হারিয়ে যাওয়ায় লেনদেনের সুবিধার্থে একটি নতুন পাসবুক ইস্যু করার জন্য বিনীত প্রার্থনা জানাচ্ছি। আমার মোবাইল নম্বর: ${f2.ifBlank { "................" }}।"
                        },
                        bodyTemplateEn = { name, _, _, f1, f2 ->
                            "I beg to state that I am ${name.ifBlank { "................" }}, maintaining Savings Account No. ${f1.ifBlank { "................" }} with your branch. As my previous passbook is completely filled up / misplaced, I kindly request you to issue a new passbook for my account. My Contact No: ${f2.ifBlank { "................" }}."
                        }
                    ),
                    LetterTypeItem(
                        id = "bank_atm",
                        titleBn = "নতুন ATM / ডেবিট কার্ড আবেদন",
                        titleEn = "ATM / Debit Card Request",
                        subjectBn = "নতুন এটিএম কার্ড ইস্যু করার আবেদন",
                        subjectEn = "Application for issue of new ATM / Debit Card",
                        field1LabelBn = "অ্যাকাউন্ট নম্বর / Account No",
                        field1LabelEn = "Account Number",
                        field2LabelBn = "মোবাইল নম্বর / Mobile No",
                        field2LabelEn = "Mobile Number",
                        bodyTemplateBn = { name, _, _, f1, f2 ->
                            "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। আপনার শাখায় আমার অ্যাকাউন্ট নম্বর ${f1.ifBlank { "................" }}। আমার দৈনন্দিন লেনদেন সহজতর করার জন্য অ্যাকাউন্টের বিপরীতে একটি নতুন এটিএম/ডেবিট কার্ড ইস্যু করার অনুরোধ জানাচ্ছি। আমার রেজিস্টার্ড মোবাইল নম্বর: ${f2.ifBlank { "................" }}।"
                        },
                        bodyTemplateEn = { name, _, _, f1, f2 ->
                            "I beg to state that I am ${name.ifBlank { "................" }}, holding Account No. ${f1.ifBlank { "................" }} at your branch. I request you to kindly issue a new ATM/Debit Card linked to my savings account for convenient banking transactions. My Mobile No: ${f2.ifBlank { "................" }}."
                        }
                    ),
                    LetterTypeItem(
                        id = "bank_reactivate",
                        titleBn = "বন্ধ অ্যাকাউন্ট চালু করার আবেদন",
                        titleEn = "Reactivate Inactive Account",
                        subjectBn = "নিষ্ক্রিয় অ্যাকাউন্ট পুনরায় সক্রিয় করার আবেদন",
                        subjectEn = "Application to reactivate dormant / inactive account",
                        field1LabelBn = "অ্যাকাউন্ট নম্বর / Account No",
                        field1LabelEn = "Account Number",
                        field2LabelBn = "আধার / প্যান নম্বর / Aadhaar/PAN",
                        field2LabelEn = "Aadhaar / PAN Number",
                        bodyTemplateBn = { name, _, _, f1, f2 ->
                            "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। আপনার শাখায় আমার অ্যাকাউন্ট নম্বর ${f1.ifBlank { "................" }}। দীর্ঘদিন লেনদেন না করার কারণে অ্যাকাউন্টটি নিষ্ক্রিয় (Dormant/Inactive) হয়ে গেছে। আমি প্রয়োজনীয় কেওয়াইসি নথি (নম্বর: ${f2.ifBlank { "................" }}) জমা দিচ্ছি। দয়া করে অ্যাকাউন্টটি পুনরায় সক্রিয় করার ব্যবস্থা করুন।"
                        },
                        bodyTemplateEn = { name, _, _, f1, f2 ->
                            "I state that I am ${name.ifBlank { "................" }}, having Account No. ${f1.ifBlank { "................" }}. Due to lack of transactions for some time, my account has become dormant. I am submitting requisite KYC documents (ID No: ${f2.ifBlank { "................" }}). I request you to kindly reactivate my account."
                        }
                    ),
                    LetterTypeItem(
                        id = "bank_refund",
                        titleBn = "ব্যর্থ লেনদেনের টাকা রিফান্ড",
                        titleEn = "Failed Transaction Refund",
                        subjectBn = "ATM/অনলাইন ব্যর্থ লেনদেনের টাকা ফেরতের আবেদন",
                        subjectEn = "Application for refund of failed transaction amount",
                        field1LabelBn = "অ্যাকাউন্ট নম্বর / Account No",
                        field1LabelEn = "Account Number",
                        field2LabelBn = "লেনদেনের তারিখ ও টাকার পরিমাণ / Txn Info",
                        field2LabelEn = "Transaction Date & Amount",
                        bodyTemplateBn = { name, _, _, f1, f2 ->
                            "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। আমার অ্যাকাউন্ট নম্বর ${f1.ifBlank { "................" }}। গত ${f2.ifBlank { "তারিখ" }} লেনদেন করার সময় টাকা না পাওয়া সত্ত্বেও আমার অ্যাকাউন্ট থেকে টাকা কেটে নেওয়া হয়েছে। অনুগ্রহ করে উক্ত ব্যর্থ লেনদেনটি তদন্ত করে টাকা আমার অ্যাকাউন্টে ফেরত দেওয়ার ব্যবস্থা করুন।"
                        },
                        bodyTemplateEn = { name, _, _, f1, f2 ->
                            "I bring to your kind notice that I hold Account No. ${f1.ifBlank { "................" }}. On ${f2.ifBlank { "Date & Amount" }}, an ATM/Online transaction failed but the amount was deducted from my account. I request you to investigate and refund the deducted amount."
                        }
                    )
                )
            ),
            LetterCategory(
                id = "school",
                titleBn = "স্কুল ও কলেজ (School & College)",
                titleEn = "School & College",
                defaultRecipientBn = "প্রধান শিক্ষক / অধ্যক্ষ মহাশয় (Headmaster / Principal)",
                defaultRecipientEn = "The Headmaster / Principal",
                letterTypes = listOf(
                    LetterTypeItem(
                        id = "school_leave",
                        titleBn = "ছুটির জন্য আবেদন",
                        titleEn = "Leave of Absence",
                        subjectBn = "অনুপস্থিতির জন্য ছুটির আবেদন",
                        subjectEn = "Application for leave of absence",
                        field1LabelBn = "শ্রেণি ও বিভাগ / Class & Section",
                        field1LabelEn = "Class & Section",
                        field2LabelBn = "রোল নম্বর / Roll No",
                        field2LabelEn = "Roll Number",
                        bodyTemplateBn = { name, _, _, f1, f2 ->
                            "সবিনয় নিবেদন এই যে, আমি আপনার বিদ্যালয়ের ${f1.ifBlank { "........" }} শ্রেণির ছাত্র/ছাত্রী ${name.ifBlank { "................" }}, রোল নম্বর ${f2.ifBlank { "...." }}। অসুস্থতা / পারিবারিক অনিবার্য কারণে আমি বিদ্যালয়ে উপস্থিত হতে পারিনি। অনুগ্রহ করে উক্ত দিনগুলির ছুটি মঞ্জুর করে বাধিত করবেন।"
                        },
                        bodyTemplateEn = { name, _, _, f1, f2 ->
                            "Most respectfully, I state that I am ${name.ifBlank { "................" }}, a student of Class: ${f1.ifBlank { "........" }}, Roll No: ${f2.ifBlank { "...." }} in your esteemed institution. Due to sickness / urgent family reasons, I could not attend school. I kindly request you to grant me leave of absence."
                        }
                    ),
                    LetterTypeItem(
                        id = "school_tc",
                        titleBn = "ট্রান্সফার সার্টিফিকেট (TC) আবেদন",
                        titleEn = "Transfer Certificate (TC)",
                        subjectBn = "বিদ্যালয় ছাড়ার ছাড়পত্র (TC) প্রাপ্তির আবেদন",
                        subjectEn = "Application for issue of Transfer Certificate",
                        field1LabelBn = "শ্রেণি ও রোল / Class & Roll",
                        field1LabelEn = "Class & Roll No",
                        field2LabelBn = "টিসি নেওয়ার কারণ / Reason",
                        field2LabelEn = "Reason for Transfer",
                        bodyTemplateBn = { name, _, _, f1, f2 ->
                            "বিনীত নিবেদন এই যে, আমি আপনার বিদ্যালয়ের ${f1.ifBlank { "........" }} শ্রেণির ছাত্র/ছাত্রী ${name.ifBlank { "................" }}। আমার অভিভাবকের বদলি / বাসস্থান পরিবর্তনজনিত কারণ (${f2.ifBlank { "বাসস্থান পরিবর্তন" }})-এর জন্য অন্য বিদ্যালয়ে ভর্তি হতে হবে। অতএব, আমাকে একটি ট্রান্সফার সার্টিফিকেট প্রদান করার অনুরোধ জানাচ্ছি।"
                        },
                        bodyTemplateEn = { name, _, _, f1, f2 ->
                            "With due respect, I state that I am ${name.ifBlank { "................" }}, student of Class: ${f1.ifBlank { "........" }}. Owing to relocation / family transfer (${f2.ifBlank { "Family Relocation" }}), I am compelled to seek admission elsewhere. I request you to kindly issue my Transfer Certificate."
                        }
                    ),
                    LetterTypeItem(
                        id = "school_concession",
                        titleBn = "ফি মকুবের আবেদন",
                        titleEn = "Fee Concession",
                        subjectBn = "দরিদ্র তহবিল থেকে বিদ্যালয় ফি মকুবের আবেদন",
                        subjectEn = "Application for school fee concession",
                        field1LabelBn = "শ্রেণি ও বিভাগ / Class & Section",
                        field1LabelEn = "Class & Section",
                        field2LabelBn = "রোল নম্বর / Roll No",
                        field2LabelEn = "Roll Number",
                        bodyTemplateBn = { name, _, _, f1, f2 ->
                            "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, আপনার বিদ্যালয়ের ${f1.ifBlank { "........" }} শ্রেণির ছাত্র/ছাত্রী, রোল নম্বর ${f2.ifBlank { "...." }}। আমার পারিবারিক আর্থিক অবস্থা অত্যন্ত অসচ্ছল হওয়ায় নিয়মিত বেতন পরিশোধ করা অসম্ভব হয়ে পড়েছে। অতএব, মানবিক বিবেচনায় আমার ফি সম্পূর্ণ/অর্ধেক মকুব করার বিনীত প্রার্থনা জানাচ্ছি।"
                        },
                        bodyTemplateEn = { name, _, _, f1, f2 ->
                            "Respected Sir, I am ${name.ifBlank { "................" }}, studying in Class: ${f1.ifBlank { "........" }}, Roll No: ${f2.ifBlank { "...." }}. Due to acute financial hardship in my family, it is difficult to bear the school fees. I earnestly request you to grant me full / half concession in fees."
                        }
                    )
                )
            ),
            LetterCategory(
                id = "electricity",
                titleBn = "বিদ্যুৎ দপ্তর (Electricity / WBSEDCL)",
                titleEn = "Electricity / Power",
                defaultRecipientBn = "স্টেশন ম্যানেজার (Station Manager / AE)",
                defaultRecipientEn = "The Station Manager / Assistant Engineer",
                letterTypes = listOf(
                    LetterTypeItem(
                        id = "elec_meter",
                        titleBn = "নতুন বিদ্যুৎ মিটার সংযোগ",
                        titleEn = "New Meter Connection",
                        subjectBn = "নতুন বিদ্যুৎ মিটার সংযোগের জন্য আবেদন",
                        subjectEn = "Application for new domestic electric meter connection",
                        field1LabelBn = "আবেদন / রেফারেন্স নং (ঐচ্ছিক)",
                        field1LabelEn = "Application / Reference No",
                        field2LabelBn = "মোবাইল নম্বর / Mobile No",
                        field2LabelEn = "Mobile Number",
                        bodyTemplateBn = { name, addr, _, f1, f2 ->
                            "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}। আমার নতুন নির্মিত বাসস্থানে বিদ্যুৎ সংযোগের প্রয়োজন। আমি সমস্ত প্রয়োজনীয় শর্তাবলী ও নিয়ম মানতে প্রস্তুত (আবেদন নং: ${f1.ifBlank { "প্রযোজ্য নয়" }})। যোগাযোগ নম্বর: ${f2.ifBlank { "................" }}। অবিলম্বে মিটার সংযোগের ব্যবস্থা করার অনুরোধ জানাচ্ছি।"
                        },
                        bodyTemplateEn = { name, addr, _, f1, f2 ->
                            "I state that I am ${name.ifBlank { "................" }}, resident of ${addr.ifBlank { "................" }}. I require a new domestic electric meter connection at my premises (Ref No: ${f1.ifBlank { "N/A" }}). Contact: ${f2.ifBlank { "................" }}. I request you to kindly sanction and install the electric meter."
                        }
                    ),
                    LetterTypeItem(
                        id = "elec_bill",
                        titleBn = "অতিরিক্ত বিদ্যুৎ বিল সংশোধন",
                        titleEn = "Excess Bill Correction",
                        subjectBn = "অস্বাভাবিক অতিরিক্ত বিদ্যুৎ বিল সংশোধনের আবেদন",
                        subjectEn = "Application for correction of inflated electric bill",
                        field1LabelBn = "কনজিউমার আইডি / Consumer ID",
                        field1LabelEn = "Consumer ID",
                        field2LabelBn = "মিটার নম্বর / Meter No",
                        field2LabelEn = "Meter Number",
                        bodyTemplateBn = { name, _, _, f1, f2 ->
                            "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। আমার বিদ্যুৎ কনজিউমার আইডি নং ${f1.ifBlank { "................" }} এবং মিটার নং ${f2.ifBlank { "................" }}। চলতি মাসে আমার স্বাভাবিক ব্যবহারের তুলনায় অতিরিক্ত ও অস্বাভাবিক বিল পাঠানো হয়েছে। অনুগ্রহ করে মিটারটি পরীক্ষা করে সঠিক বিল প্রস্তুত করার অনুরোধ জানাচ্ছি।"
                        },
                        bodyTemplateEn = { name, _, _, f1, f2 ->
                            "I bring to your notice that I am ${name.ifBlank { "................" }}, holding Consumer ID: ${f1.ifBlank { "................" }} and Meter No: ${f2.ifBlank { "................" }}. For the current billing cycle, an abnormally inflated bill has been generated. I request you to inspect the meter and rectify the bill."
                        }
                    ),
                    LetterTypeItem(
                        id = "elec_faulty",
                        titleBn = "খারাপ / বিকল মিটার পরিবর্তন",
                        titleEn = "Defective Meter Replacement",
                        subjectBn = "বিকল বিদ্যুৎ মিটার পরিবর্তনের আবেদন",
                        subjectEn = "Application for replacement of defective electric meter",
                        field1LabelBn = "কনজিউমার আইডি / Consumer ID",
                        field1LabelEn = "Consumer ID",
                        field2LabelBn = "মিটার নম্বর / Meter No",
                        field2LabelEn = "Meter Number",
                        bodyTemplateBn = { name, _, _, f1, f2 ->
                            "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}। আমার কনজিউমার আইডি ${f1.ifBlank { "................" }} এবং মিটার নম্বর ${f2.ifBlank { "................" }}। বেশ কিছুদিন যাবৎ মিটারটি বিকল হয়ে বন্ধ রয়েছে এবং ডিসপ্লে প্রদর্শিত হচ্ছে না। অতএব, দ্রুত মিটারটি পরিবর্তন করে দেওয়ার অনুরোধ জানাচ্ছি।"
                        },
                        bodyTemplateEn = { name, _, _, f1, f2 ->
                            "I wish to state that I am ${name.ifBlank { "................" }}, Consumer ID: ${f1.ifBlank { "................" }} and Meter No: ${f2.ifBlank { "................" }}. My electric meter has stopped functioning and is completely defective. I request you to replace the faulty meter at the earliest."
                        }
                    )
                )
            ),
            LetterCategory(
                id = "panchayat",
                titleBn = "পঞ্চায়েত ও পৌরসভা (Panchayat / Municipality)",
                titleEn = "Panchayat / Municipality",
                defaultRecipientBn = "প্রধান / পৌরপিতা (Pradhan / Chairman)",
                defaultRecipientEn = "The Pradhan / Chairman / Municipal Commissioner",
                letterTypes = listOf(
                    LetterTypeItem(
                        id = "panch_income",
                        titleBn = "পারিবারিক বা আয়ের শংসাপত্র",
                        titleEn = "Income Certificate",
                        subjectBn = "বার্ষিক আয়ের শংসাপত্র (Income Certificate) প্রদানের আবেদন",
                        subjectEn = "Application for issuance of Income Certificate",
                        field1LabelBn = "পিতা / স্বামীর নাম / Guardian Name",
                        field1LabelEn = "Father / Husband Name",
                        field2LabelBn = "আধার নম্বর / Aadhaar No",
                        field2LabelEn = "Aadhaar Number",
                        bodyTemplateBn = { name, addr, _, f1, f2 ->
                            "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, পিতা/স্বামী- ${f1.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}, আধার নং: ${f2.ifBlank { "................" }}। আমার সরকারি স্কলারশিপ / উচ্চশিক্ষার দরকারে একটি পারিবারিক বার্ষিক আয়ের শংসাপত্র প্রয়োজন। দয়া করে আমাকে একটি আয়ের শংসাপত্র প্রদান করে উপকৃত করবেন।"
                        },
                        bodyTemplateEn = { name, addr, _, f1, f2 ->
                            "I beg to state that I am ${name.ifBlank { "................" }}, son/daughter of ${f1.ifBlank { "................" }}, resident of ${addr.ifBlank { "................" }}, Aadhaar No: ${f2.ifBlank { "................" }}. I require an official Annual Income Certificate for educational / official purposes. I request you to kindly issue the certificate."
                        }
                    ),
                    LetterTypeItem(
                        id = "panch_residence",
                        titleBn = "স্থায়ী বাসিন্দার শংসাপত্র",
                        titleEn = "Residential Certificate",
                        subjectBn = "স্থায়ী বাসিন্দার শংসাপত্র প্রাপ্তির আবেদন",
                        subjectEn = "Application for Residential Certificate",
                        field1LabelBn = "পিতা / স্বামীর নাম / Guardian Name",
                        field1LabelEn = "Father / Husband Name",
                        field2LabelBn = "ওয়ার্ড / গ্রাম নং / Ward or Village",
                        field2LabelEn = "Ward / Village No",
                        bodyTemplateBn = { name, addr, _, f1, f2 ->
                            "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, পিতা/স্বামী- ${f1.ifBlank { "................" }}। আমি আপনার এলাকার ${f2.ifBlank { "গ্রাম / ওয়ার্ড" }}-এর একজন স্থায়ী বাসিন্দা। সরকারি চাকরির আবেদন ও সনাক্তকরণের জন্য একটি আবাসিক শংসাপত্র প্রয়োজন। অতএব, আমাকে একটি স্থায়ী বাসিন্দার শংসাপত্র প্রদান করার অনুরোধ করছি।"
                        },
                        bodyTemplateEn = { name, addr, _, f1, f2 ->
                            "With respect, I state that I am ${name.ifBlank { "................" }}, S/D/W of ${f1.ifBlank { "................" }}. I am a permanent resident of ${f2.ifBlank { "Ward / Village" }}, ${addr.ifBlank { "Address" }}. I request you to issue a Residential Certificate for official documentation."
                        }
                    ),
                    LetterTypeItem(
                        id = "panch_repair",
                        titleBn = "রাস্তা ও নিকাশি নালা মেরামত",
                        titleEn = "Road & Drainage Repair",
                        subjectBn = "এলাকার রাস্তা ও নিকাশি নালা সংস্কারের আবেদন",
                        subjectEn = "Application for repair of road and drainage system",
                        field1LabelBn = "গ্রাম / পাড়া / ওয়ার্ডের নাম / Locality",
                        field1LabelEn = "Locality / Para / Ward",
                        field2LabelBn = "মোবাইল নম্বর / Contact No",
                        field2LabelEn = "Contact Number",
                        bodyTemplateBn = { name, _, _, f1, f2 ->
                            "বিনীত নিবেদন এই যে, আমি ${name.ifBlank { "................" }} এবং আমাদের এলাকার (${f1.ifBlank { "পাড়া/গ্রাম" }}) বাসিন্দাবৃন্দ। এলাকার প্রধান রাস্তা ও ড্রেন দীর্ঘদিন ধরে অত্যন্ত বেহাল ও কর্দমাক্ত অবস্থায় রয়েছে, যার ফলে সাধারণ মানুষের যাতায়াতে চরম ভোগান্তি হচ্ছে। অনুগ্রহ করে রাস্তা ও নিকাশি নালাটি সংস্কারের দ্রুত ব্যবস্থা গ্রহণ করুন।"
                        },
                        bodyTemplateEn = { name, _, _, f1, f2 ->
                            "We, the residents of ${f1.ifBlank { "Locality" }} through ${name.ifBlank { "................" }}, wish to highlight the dilapidated condition of the road and drainage system in our area. It causes acute inconvenience to commuters. We request you to initiate urgent repair works."
                        }
                    )
                )
            ),
            LetterCategory(
                id = "police",
                titleBn = "থানা ও পুলিশ (Police / GD / FIR)",
                titleEn = "Police Station (GD)",
                defaultRecipientBn = "ভারপ্রাপ্ত আধিকারিক (Officer-in-Charge / IC)",
                defaultRecipientEn = "The Officer-in-Charge / Inspector-in-Charge",
                letterTypes = listOf(
                    LetterTypeItem(
                        id = "police_mobile",
                        titleBn = "মোবাইল ফোন হারানোর জিডি",
                        titleEn = "Lost Mobile Phone GD",
                        subjectBn = "মোবাইল ফোন হারানোর সাধারণ ডায়েরি (GD) করার আবেদন",
                        subjectEn = "Application for General Diary (GD) regarding lost mobile phone",
                        field1LabelBn = "মোবাইল মডেল ও কোম্পানি / Mobile Model",
                        field1LabelEn = "Mobile Brand & Model",
                        field2LabelBn = "IMEI নম্বর ও সিম নম্বর / IMEI & SIM",
                        field2LabelEn = "IMEI & SIM Number",
                        bodyTemplateBn = { name, addr, _, f1, f2 ->
                            "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}। গত তারিখে যাতায়াতের সময় অসাবধানতাবশত আমার ব্যবহৃত মোবাইল ফোনটি (${f1.ifBlank { "হ্যান্ডসেটের বিবরণ" }}) হারিয়ে যায়। ফোনটির আইএমইআই ও সিম বিবরণ: ${f2.ifBlank { "IMEI / SIM" }}। চারিদিকে খোঁজাখুঁজি করেও পাওয়া যায়নি। ভবিষ্যতে কোনো অসদুপায় এড়াতে এই বিষয়ে একটি সাধারণ ডায়েরি (GD) নথিভুক্ত করার অনুরোধ জানাচ্ছি।"
                        },
                        bodyTemplateEn = { name, addr, _, f1, f2 ->
                            "I, ${name.ifBlank { "................" }}, resident of ${addr.ifBlank { "................" }}, beg to report that while traveling, I lost my mobile handset (${f1.ifBlank { "Device Details" }}) having IMEI/SIM: ${f2.ifBlank { "IMEI / SIM Details" }}. Despite thorough search, it could not be traced. Please record a General Diary (GD) entry for the same."
                        }
                    ),
                    LetterTypeItem(
                        id = "police_doc",
                        titleBn = "নথিপত্র / সার্টিফিকেট হারানোর জিডি",
                        titleEn = "Lost Document / Certificate GD",
                        subjectBn = "জরুরি নথিপত্র হারানোর জিডি করার আবেদন",
                        subjectEn = "Application for General Diary regarding lost documents",
                        field1LabelBn = "হারানো নথির বিবরণ (যেমন: মার্কশিট/ভোটার)",
                        field1LabelEn = "Document Details (Marksheet, ID, etc.)",
                        field2LabelBn = "হারানোর সম্ভাব্য স্থান ও সময় / Lost Place",
                        field2LabelEn = "Probable Location & Time",
                        bodyTemplateBn = { name, addr, _, f1, f2 ->
                            "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}। আমার অতি গুরুত্বপূর্ণ নথিপত্র (${f1.ifBlank { "নথির নাম ও নম্বর" }}) ${f2.ifBlank { "স্থানের নাম" }} এলাকায় যাতায়াতের সময় দুর্ভাগ্যবশত হারিয়ে গেছে। অনেক খোঁজাখুঁজি করেও তা উদ্ধার করা সম্ভব হয়নি। নতুন ডুপ্লিকেট কপি তোলার জন্য একটি জিডি (GD) গ্রহণ করার অনুরোধ জানাচ্ছি।"
                        },
                        bodyTemplateEn = { name, addr, _, f1, f2 ->
                            "I state that I am ${name.ifBlank { "................" }}, residing at ${addr.ifBlank { "................" }}. My vital document(s) (${f1.ifBlank { "Document particulars" }}) were lost near ${f2.ifBlank { "Location" }}. I request you to register a General Diary entry so I can apply for duplicate credentials."
                        }
                    )
                )
            ),
            LetterCategory(
                id = "bdo",
                titleBn = "সরকারি দপ্তর ও বিডিও (Govt Office / BDO)",
                titleEn = "Govt Office / BDO",
                defaultRecipientBn = "ব্লক উন্নয়ন আধিকারিক (Block Development Officer)",
                defaultRecipientEn = "The Block Development Officer",
                letterTypes = listOf(
                    LetterTypeItem(
                        id = "bdo_ration",
                        titleBn = "রেশন কার্ড সংশোধন / নাম তোলা",
                        titleEn = "Ration Card Correction / Addition",
                        subjectBn = "রেশন কার্ডে তথ্য সংশোধন / নতুন নাম তোলার আবেদন",
                        subjectEn = "Application for correction / addition in Ration Card",
                        field1LabelBn = "রেশন কার্ড নম্বর / Ration Card No",
                        field1LabelEn = "Ration Card Number",
                        field2LabelBn = "সংশোধনের বিবরণ / Correction Detail",
                        field2LabelEn = "Correction Details",
                        bodyTemplateBn = { name, addr, _, f1, f2 ->
                            "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}। আমার পরিবারের ডিজিটাল রেশন কার্ড নম্বর ${f1.ifBlank { "................" }}। উক্ত কার্ডে কিছু তথ্যগত ভুল সংশোধন (${f2.ifBlank { "সংশোধনের বিবরণ" }}) করা আবশ্যক। উপযুক্ত প্রমাণপত্রাদি সহ আবেদনটি জমা দিচ্ছি। অবিলম্বে সংশোধনের ব্যবস্থা করার বিনীত অনুরোধ জানাচ্ছি।"
                        },
                        bodyTemplateEn = { name, addr, _, f1, f2 ->
                            "I beg to submit that I am ${name.ifBlank { "................" }}, resident of ${addr.ifBlank { "................" }}. In our family Digital Ration Card No. ${f1.ifBlank { "................" }}, correction is required regarding ${f2.ifBlank { "Details" }}. I submit the requisite proofs and pray for speedy rectification."
                        }
                    ),
                    LetterTypeItem(
                        id = "bdo_pension",
                        titleBn = "বার্ধক্য / বিধবা ভাতা আবেদন",
                        titleEn = "Pension Application",
                        subjectBn = "বার্ধক্য / বিধবা ভাতা মঞ্জুরের আবেদন",
                        subjectEn = "Application for sanction of Old Age / Widow Pension",
                        field1LabelBn = "আধার নম্বর / Aadhaar No",
                        field1LabelEn = "Aadhaar Number",
                        field2LabelBn = "ব্যাংক অ্যাকাউন্ট নং / Bank Account No",
                        field2LabelEn = "Bank Account Number",
                        bodyTemplateBn = { name, addr, _, f1, f2 ->
                            "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}। আমার বয়স ও পারিবারিক আর্থিক অবস্থা বিবেচনায় জীবনধারণ অত্যন্ত কষ্টসাধ্য। আমার আধার নং: ${f1.ifBlank { "................" }} এবং ব্যাংক অ্যাকাউন্ট নং: ${f2.ifBlank { "................" }}। মানবিক কারণে আমাকে সরকারি মাসিক ভাতা মঞ্জুর করার বিনীত প্রার্থনা জানাচ্ছি।"
                        },
                        bodyTemplateEn = { name, addr, _, f1, f2 ->
                            "Most respectfully, I state that I am ${name.ifBlank { "................" }}, resident of ${addr.ifBlank { "................" }}. Considering my age and destitute economic condition, I request the sanction of monthly government pension. Aadhaar: ${f1.ifBlank { "................" }}, Bank A/C: ${f2.ifBlank { "................" }}."
                        }
                    )
                )
            ),
            LetterCategory(
                id = "land",
                titleBn = "ভূমি ও রেজিস্ট্রি (Land & Registry / BL&LRO)",
                titleEn = "Land & Registry / BL&LRO",
                defaultRecipientBn = "ব্লক ভূমি ও ভূমি সংস্কার আধিকারিক (BL&LRO)",
                defaultRecipientEn = "The Block Land & Land Reforms Officer (BL&LRO)",
                letterTypes = listOf(
                    LetterTypeItem(
                        id = "land_mutation",
                        titleBn = "জমির মিউটেশন আবেদন",
                        titleEn = "Land Mutation",
                        subjectBn = "জমির ওয়ারিশান / ক্রয়সূত্রে মিউটেশনের আবেদন",
                        subjectEn = "Application for Land Mutation",
                        field1LabelBn = "মৌজা ও খতিয়ান নং / Mouza & Khatian No",
                        field1LabelEn = "Mouza & Khatian No",
                        field2LabelBn = "দাগ নং ও জমির পরিমাণ / Plot No & Area",
                        field2LabelEn = "Plot No & Area",
                        bodyTemplateBn = { name, addr, _, f1, f2 ->
                            "সবিনয় নিবেদন এই যে, আমি ${name.ifBlank { "................" }}, সাং- ${addr.ifBlank { "................" }}। মৌজা ও খতিয়ান নং: ${f1.ifBlank { "মৌজা ও খতিয়ান" }}, দাগ নং ও জমির পরিমাণ: ${f2.ifBlank { "দাগ ও পরিমাণ" }} স্থিত জমি আমি বিধিসম্মতভাবে ক্রয়/উত্তরাধিকার সূত্রে অর্জন করেছি। প্রয়োজনীয় দলিল ও দাখিলা সহ জমিটির মিউটেশন মঞ্জুর করার আবেদন জানাচ্ছি।"
                        },
                        bodyTemplateEn = { name, addr, _, f1, f2 ->
                            "I beg to submit that I, ${name.ifBlank { "................" }}, resident of ${addr.ifBlank { "................" }}, have lawfully acquired land situated at Mouza & Khatian: ${f1.ifBlank { "Mouza/Khatian" }}, Plot & Area: ${f2.ifBlank { "Plot Details" }}. I request you to record the mutation in my name."
                        }
                    )
                )
            )
        )
    }

    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val currentCategory = categories[selectedCategoryIndex]

    var selectedLetterTypeIndex by remember { mutableIntStateOf(0) }
    val currentLetterType = currentCategory.letterTypes.getOrElse(selectedLetterTypeIndex) { currentCategory.letterTypes.first() }

    // Dynamic Form States
    var applicantName by remember { mutableStateOf("") }
    val currentDate = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date()) }
    var date by remember { mutableStateOf(currentDate) }
    var recipientTitle by remember(selectedCategoryIndex, isBengali) {
        mutableStateOf(if (isBengali) currentCategory.defaultRecipientBn else currentCategory.defaultRecipientEn)
    }
    var officeName by remember { mutableStateOf("") }
    var applicantAddress by remember { mutableStateOf("") }
    var place by remember { mutableStateOf("") }
    var dynamicField1 by remember { mutableStateOf("") }
    var dynamicField2 by remember { mutableStateOf("") }

    // Signature State
    var signatureBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Voice typing launcher
    var activeVoiceTarget by remember { mutableStateOf<String?>(null) }
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: ""
            if (spokenText.isNotBlank()) {
                when (activeVoiceTarget) {
                    "name" -> applicantName = spokenText
                    "office" -> officeName = spokenText
                    "address" -> applicantAddress = spokenText
                    "place" -> place = spokenText
                    "field1" -> dynamicField1 = spokenText
                    "field2" -> dynamicField2 = spokenText
                }
            }
        }
    }

    fun startVoiceInput(target: String) {
        activeVoiceTarget = target
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (isBengali) "bn-IN" else "en-US")
                putExtra(RecognizerIntent.EXTRA_PROMPT, if (isBengali) "বলুন, স্বয়ংক্রিয়ভাবে টাইপ হবে..." else "Speak now...")
            }
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, if (isBengali) "ভয়েস ইনপুট সমর্থিত নয়" else "Voice input not supported", Toast.LENGTH_SHORT).show()
        }
    }

    // Dynamic Live Preview Generator
    val generatedLetter = remember(
        isBengali, currentCategory, currentLetterType,
        applicantName, date, recipientTitle, officeName, applicantAddress, place, dynamicField1, dynamicField2, signatureBitmap
    ) {
        val sigPlaceholder = if (signatureBitmap != null) "[স্বাক্ষর সংযুক্ত]" else "................................"
        if (isBengali) {
            val body = currentLetterType.bodyTemplateBn(
                applicantName, applicantAddress, officeName, dynamicField1, dynamicField2
            )
            """
প্রতি,
${recipientTitle.ifBlank { currentCategory.defaultRecipientBn }}
${officeName.ifBlank { "...................................." }}

বিষয়: ${currentLetterType.subjectBn}

মহাশয়/মহাশয়া,
$body

অতএব, মহাশয়ের নিকট বিনীত প্রার্থনা, উক্ত বিষয়টি বিবেচনা করে প্রয়োজনীয় ব্যবস্থা গ্রহণ করলে আমি বিশেষভাবে উপকৃত হব।

তারিখ: $date
স্থান: ${place.ifBlank { "................" }}

ধন্যবাদান্তে,
${applicantName.ifBlank { "................" }}
${if (applicantAddress.isNotBlank()) "ঠিকানা: $applicantAddress\n" else ""}${if (signatureBitmap != null) "" else "স্বাক্ষর: ................................"}
            """.trimIndent()
        } else {
            val body = currentLetterType.bodyTemplateEn(
                applicantName, applicantAddress, officeName, dynamicField1, dynamicField2
            )
            """
To,
${recipientTitle.ifBlank { currentCategory.defaultRecipientEn }}
${officeName.ifBlank { "...................................." }}

Subject: ${currentLetterType.subjectEn}

Respected Sir/Madam,
$body

Therefore, I request you to kindly take necessary action in this regard at your earliest convenience.

Date: $date
Place: ${place.ifBlank { "................" }}

Yours faithfully,
${applicantName.ifBlank { "................" }}
${if (applicantAddress.isNotBlank()) "Address: $applicantAddress\n" else ""}${if (signatureBitmap != null) "" else "Signature: ................................"}
            """.trimIndent()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isBengali) "দরখাস্ত তৈরি" else "Application Maker",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (isBengali) "সহজেই তৈরি করুন অফিশিয়াল দরখাস্ত" else "Create formal applications instantly",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // 1. Language Switcher (বাংলা / English)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBengali) "ভাষা নির্বাচন করুন:" else "Select Language:",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { isBengali = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBengali) Color(0xFF2563EB) else Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("বাংলা", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { isBengali = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isBengali) Color(0xFF2563EB) else Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("English", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Category Selector (বিভাগ নির্বাচন)
            Text(
                text = if (isBengali) "১. বিভাগ বা দপ্তর নির্বাচন (Category):" else "1. Select Category:",
                color = Color(0xFF60A5FA),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            var categoryMenuExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                        .clickable { categoryMenuExpanded = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBengali) currentCategory.titleBn else currentCategory.titleEn,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Text("▼", color = Color.Gray, fontSize = 12.sp)
                    }
                }

                DropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(Color(0xFF1E293B))
                ) {
                    categories.forEachIndexed { index, cat ->
                        DropdownMenuItem(
                            text = { Text(if (isBengali) cat.titleBn else cat.titleEn, color = Color.White) },
                            onClick = {
                                selectedCategoryIndex = index
                                selectedLetterTypeIndex = 0
                                recipientTitle = if (isBengali) cat.defaultRecipientBn else cat.defaultRecipientEn
                                categoryMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Letter Type Selector (দরখাস্তের ধরন)
            Text(
                text = if (isBengali) "২. দরখাস্তের ধরন নির্বাচন (Letter Type):" else "2. Select Letter Type:",
                color = Color(0xFF60A5FA),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            var typeMenuExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                        .clickable { typeMenuExpanded = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBengali) currentLetterType.titleBn else currentLetterType.titleEn,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Text("▼", color = Color.Gray, fontSize = 12.sp)
                    }
                }

                DropdownMenu(
                    expanded = typeMenuExpanded,
                    onDismissRequest = { typeMenuExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(Color(0xFF1E293B))
                ) {
                    currentCategory.letterTypes.forEachIndexed { index, lType ->
                        DropdownMenuItem(
                            text = { Text(if (isBengali) lType.titleBn else lType.titleEn, color = Color.White) },
                            onClick = {
                                selectedLetterTypeIndex = index
                                typeMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Input Fields with Voice Typing
            Text(
                text = if (isBengali) "৩. প্রয়োজনীয় তথ্য পূরণ করুন:" else "3. Fill in Details:",
                color = Color(0xFF60A5FA),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Applicant Name
            OutlinedTextField(
                value = applicantName,
                onValueChange = { applicantName = it },
                label = { Text(if (isBengali) "আবেদনকারীর নাম (Applicant Name)" else "Applicant Name", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                trailingIcon = {
                    IconButton(onClick = { startVoiceInput("name") }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF38BDF8))
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFF334155)
                ),
                shape = RoundedCornerShape(8.dp)
            )

            // Date
            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text(if (isBengali) "তারিখ (Date)" else "Date", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFF334155)
                ),
                shape = RoundedCornerShape(8.dp)
            )

            // Recipient Title
            OutlinedTextField(
                value = recipientTitle,
                onValueChange = { recipientTitle = it },
                label = { Text(if (isBengali) "প্রাপকের পদ (Recipient Title)" else "Recipient Title", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFF334155)
                ),
                shape = RoundedCornerShape(8.dp)
            )

            // Office / Branch Name
            OutlinedTextField(
                value = officeName,
                onValueChange = { officeName = it },
                label = { Text(if (isBengali) "দপ্তর বা শাখার নাম ও ঠিকানা (Office Name & Branch)" else "Office Name & Address", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                trailingIcon = {
                    IconButton(onClick = { startVoiceInput("office") }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF38BDF8))
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFF334155)
                ),
                shape = RoundedCornerShape(8.dp)
            )

            // Applicant Address
            OutlinedTextField(
                value = applicantAddress,
                onValueChange = { applicantAddress = it },
                label = { Text(if (isBengali) "আবেদনকারীর ঠিকানা (Applicant Address)" else "Applicant Address", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                trailingIcon = {
                    IconButton(onClick = { startVoiceInput("address") }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF38BDF8))
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFF334155)
                ),
                shape = RoundedCornerShape(8.dp)
            )

            // Place
            OutlinedTextField(
                value = place,
                onValueChange = { place = it },
                label = { Text(if (isBengali) "স্থান (Place)" else "Place", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                trailingIcon = {
                    IconButton(onClick = { startVoiceInput("place") }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF38BDF8))
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFF334155)
                ),
                shape = RoundedCornerShape(8.dp)
            )

            // Dynamic Context Field 1
            if (currentLetterType.field1LabelBn.isNotBlank()) {
                OutlinedTextField(
                    value = dynamicField1,
                    onValueChange = { dynamicField1 = it },
                    label = { Text(if (isBengali) currentLetterType.field1LabelBn else currentLetterType.field1LabelEn, color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    trailingIcon = {
                        IconButton(onClick = { startVoiceInput("field1") }) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF38BDF8))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            // Dynamic Context Field 2
            if (currentLetterType.field2LabelBn.isNotBlank()) {
                OutlinedTextField(
                    value = dynamicField2,
                    onValueChange = { dynamicField2 = it },
                    label = { Text(if (isBengali) currentLetterType.field2LabelBn else currentLetterType.field2LabelEn, color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    trailingIcon = {
                        IconButton(onClick = { startVoiceInput("field2") }) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color(0xFF38BDF8))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Signature Upload Section (Optional with ONNX AI BG Remove & Original/Manual)
            SignatureUploadSection(
                signatureBitmap = signatureBitmap,
                onSignatureChanged = { signatureBitmap = it },
                title = if (isBengali) "স্বাক্ষর আপলোড (Upload Signature - ঐচ্ছিক)" else "Upload Signature (Optional)",
                subtitle = if (isBengali) "স্বাক্ষর আপলোড করলে প্রিন্ট কপিতে সই বসে যাবে। না দিলে ডট-লাইনে (................) হাতে সই করতে পারবেন।" else "Uploaded signature will be placed in the printout. If empty, a dot-line will appear for manual signing."
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons: Print A4 / Save PDF, Copy, Clear
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // A4 Print / PDF
                Button(
                    onClick = {
                        val webView = WebView(context)
                        val bodyText = if (isBengali) {
                            currentLetterType.bodyTemplateBn(applicantName, applicantAddress, officeName, dynamicField1, dynamicField2)
                        } else {
                            currentLetterType.bodyTemplateEn(applicantName, applicantAddress, officeName, dynamicField1, dynamicField2)
                        }

                        val signatureHtml = if (signatureBitmap != null) {
                            """<img src="data:image/png;base64,${signatureBitmap!!.toBase64Png()}" class="sig-img" alt="Signature" />"""
                        } else {
                            """................................"""
                        }

                        val htmlContent = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta charset="UTF-8">
                                <style>
                                    @page {
                                        size: A4 portrait;
                                        margin: 24mm 20mm 20mm 24mm;
                                    }
                                    body {
                                        font-family: 'Noto Sans Bengali', sans-serif, 'Segoe UI', Arial, sans-serif;
                                        font-size: 12.5pt;
                                        line-height: 1.7;
                                        color: #111;
                                        padding: 0;
                                        margin: 0;
                                    }
                                    .recipient {
                                        margin-bottom: 16px;
                                    }
                                    .subject {
                                        font-weight: bold;
                                        margin: 16px 0;
                                        text-decoration: underline;
                                    }
                                    .salutation {
                                        margin-bottom: 8px;
                                        font-weight: bold;
                                    }
                                    .body-text {
                                        text-align: justify;
                                        margin-bottom: 16px;
                                    }
                                    .closing-text {
                                        text-align: justify;
                                        margin-bottom: 24px;
                                    }
                                    .footer-table {
                                        width: 100%;
                                        margin-top: 24px;
                                        border-collapse: collapse;
                                    }
                                    .footer-left {
                                        width: 50%;
                                        vertical-align: bottom;
                                        font-size: 11pt;
                                        line-height: 1.6;
                                    }
                                    .footer-right {
                                        width: 50%;
                                        vertical-align: bottom;
                                        text-align: right;
                                        font-size: 11.5pt;
                                    }
                                    .sig-container {
                                        display: inline-block;
                                        text-align: center;
                                        min-width: 140px;
                                        margin-top: 8px;
                                    }
                                    .sig-img {
                                        max-height: 48px;
                                        max-width: 140px;
                                        object-fit: contain;
                                        display: block;
                                        margin: 0 auto;
                                    }
                                </style>
                            </head>
                            <body>
                                <div class="recipient">
                                    <div>${if (isBengali) "প্রতি," else "To,"}</div>
                                    <div>${recipientTitle.ifBlank { if (isBengali) currentCategory.defaultRecipientBn else currentCategory.defaultRecipientEn }}</div>
                                    <div>${officeName.ifBlank { "...................................." }}</div>
                                </div>

                                <div class="subject">
                                    ${if (isBengali) "বিষয়: " + currentLetterType.subjectBn else "Subject: " + currentLetterType.subjectEn}
                                </div>

                                <div class="salutation">${if (isBengali) "মহাশয়/মহাশয়া," else "Respected Sir/Madam,"}</div>
                                
                                <div class="body-text">$bodyText</div>

                                <div class="closing-text">
                                    ${if (isBengali) "অতএব, মহাশয়ের নিকট বিনীত প্রার্থনা, উক্ত বিষয়টি বিবেচনা করে প্রয়োজনীয় ব্যবস্থা গ্রহণ করলে আমি বিশেষভাবে উপকৃত হব।" else "Therefore, I request you to kindly take necessary action in this regard at your earliest convenience."}
                                </div>

                                <table class="footer-table">
                                    <tr>
                                        <td class="footer-left">
                                            <div>${if (isBengali) "তারিখ: " + date else "Date: " + date}</div>
                                            <div>${if (isBengali) "স্থান: " + place.ifBlank { "................" } else "Place: " + place.ifBlank { "................" }}</div>
                                        </td>
                                        <td class="footer-right">
                                            <div>${if (isBengali) "ধন্যবাদান্তে," else "Yours faithfully,"}</div>
                                            <div style="font-weight: bold; margin: 3px 0;">${applicantName.ifBlank { "................" }}</div>
                                            ${if (applicantAddress.isNotBlank()) "<div style='font-size: 10.5pt; color: #444;'>" + (if (isBengali) "ঠিকানা: " else "Address: ") + applicantAddress + "</div>" else ""}
                                            <div class="sig-container">
                                                $signatureHtml
                                            </div>
                                        </td>
                                    </tr>
                                </table>
                            </body>
                            </html>
                        """.trimIndent()

                        webView.webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                                val printAdapter = webView.createPrintDocumentAdapter("Application_Letter")
                                val printAttributes = PrintAttributes.Builder()
                                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                    .build()
                                printManager.print("Application_Letter", printAdapter, printAttributes)
                            }
                        }
                        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("A4 Print", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Copy Letter
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Application Letter", generatedLetter))
                        Toast.makeText(context, if (isBengali) "লেখাটি কপি করা হয়েছে" else "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Clear
                Button(
                    onClick = {
                        applicantName = ""
                        officeName = ""
                        applicantAddress = ""
                        place = ""
                        dynamicField1 = ""
                        dynamicField2 = ""
                        signatureBitmap = null
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // LIVE PREVIEW Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE PREVIEW (লাইভ প্রিভিউ)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = if (isBengali) "স্বয়ংক্রিয়ভাবে তৈরি দরখাস্ত" else "Real-time formatted letter",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    val bodyText = if (isBengali) {
                        currentLetterType.bodyTemplateBn(applicantName, applicantAddress, officeName, dynamicField1, dynamicField2)
                    } else {
                        currentLetterType.bodyTemplateEn(applicantName, applicantAddress, officeName, dynamicField1, dynamicField2)
                    }

                    Text(
                        text = if (isBengali) "প্রতি,\n${recipientTitle.ifBlank { currentCategory.defaultRecipientBn }}\n${officeName.ifBlank { "...................................." }}"
                               else "To,\n${recipientTitle.ifBlank { currentCategory.defaultRecipientEn }}\n${officeName.ifBlank { "...................................." }}",
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isBengali) "বিষয়: ${currentLetterType.subjectBn}" else "Subject: ${currentLetterType.subjectEn}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isBengali) "মহাশয়/মহাশয়া," else "Respected Sir/Madam,",
                        color = Color(0xFFE2E8F0),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = bodyText,
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isBengali) "অতএব, মহাশয়ের নিকট বিনীত প্রার্থনা, উক্ত বিষয়টি বিবেচনা করে প্রয়োজনীয় ব্যবস্থা গ্রহণ করলে আমি বিশেষভাবে উপকৃত হব।"
                               else "Therefore, I request you to kindly take necessary action in this regard at your earliest convenience.",
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text("তারিখ: $date", color = Color(0xFF94A3B8), fontSize = 11.5.sp)
                            Text("স্থান: ${place.ifBlank { "................" }}", color = Color(0xFF94A3B8), fontSize = 11.5.sp)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(if (isBengali) "ধন্যবাদান্তে," else "Yours faithfully,", color = Color(0xFFE2E8F0), fontSize = 12.sp)
                            Text(applicantName.ifBlank { "................" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            if (applicantAddress.isNotBlank()) {
                                Text(applicantAddress, color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            if (signatureBitmap != null) {
                                Image(
                                    bitmap = signatureBitmap!!.asImageBitmap(),
                                    contentDescription = "Signature",
                                    modifier = Modifier
                                        .height(40.dp)
                                        .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                                        .padding(2.dp)
                                )
                            } else {
                                Text("................................", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
