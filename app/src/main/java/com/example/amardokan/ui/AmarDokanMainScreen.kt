package com.example.amardokan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AmarDokanMainScreen(
    onExitApp: () -> Unit = {},
    onOpenPdfTools: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "আমার দোকান (Amar Dokan)",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Store Billing & Customer Management",
                color = Color(0xFF94A3B8),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onOpenPdfTools) {
                Text("Back to PDF Tools")
            }
        }
    }
}
