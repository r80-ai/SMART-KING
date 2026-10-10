package com.example.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.PdfMasterViewModel

object LanguageHelper {
    fun get(lang: String, en: String, bn: String, hi: String): String {
        return when (lang) {
            "bn" -> bn
            "hi" -> hi
            else -> en
        }
    }
}

@Composable
fun stringResource(viewModel: PdfMasterViewModel, en: String, bn: String, hi: String = ""): String {
    val lang by viewModel.appLanguage.collectAsState()
    return LanguageHelper.get(lang, en, bn, if (hi.isEmpty()) en else hi)
}
