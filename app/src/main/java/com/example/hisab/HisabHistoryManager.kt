package com.example.hisab

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object HisabHistoryManager {

    private val _history = MutableStateFlow<List<HisabHistoryItem>>(emptyList())
    val history: StateFlow<List<HisabHistoryItem>> = _history.asStateFlow()

    fun addHistory(
        type: String,
        titleBn: String,
        inputDescBn: String,
        resultBn: String
    ) {
        val newItem = HisabHistoryItem(
            type = type,
            titleBn = titleBn,
            inputDescriptionBn = inputDescBn,
            resultBn = resultBn
        )
        _history.value = listOf(newItem) + _history.value.take(49) // Keep last 50
    }

    fun clearHistory() {
        _history.value = emptyList()
    }
}
