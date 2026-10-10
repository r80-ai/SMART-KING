package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AppScreen(
    modifier: Modifier = Modifier,
    isScrollable: Boolean = false,
    useSafeArea: Boolean = true,
    backgroundColor: Color = Color.Unspecified,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val bgColor = if (backgroundColor == Color.Unspecified) MaterialTheme.colorScheme.background else backgroundColor
    Surface(
        modifier = modifier.fillMaxSize(),
        color = bgColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (useSafeArea) Modifier.safeDrawingPadding().imePadding() else Modifier)
                .then(
                    if (isScrollable) Modifier.verticalScroll(rememberScrollState())
                    else Modifier
                )
                .padding(paddingValues),
            content = content
        )
    }
}
