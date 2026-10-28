package com.example.calendar.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.calendar.model.CalendarEvent
import com.example.calendar.util.EventDrawableMapper
import com.example.calendar.util.EventVisualFallbackEngine

/**
 * Renders authentic, high-resolution original-color photographs for deities,
 * historical personalities, real astronomical moon photos, or styled vector graphic
 * badges for Hindu, Islamic, Sikh, Buddhist, Jain, Christian festivals, and National Days.
 */
@Composable
fun CalendarCellVisual(
    event: CalendarEvent,
    isToday: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp
) {
    val context = LocalContext.current
    val drawableRes = event.resolveDrawableRes(context)
        ?: EventDrawableMapper.getDrawableResForEvent(event, context)

    val borderWidth = if (size >= 40.dp) 1.5.dp else 1.dp
    val textFontSize = if (size >= 40.dp) 24.sp else 13.sp

    if (!event.photoUrl.isNullOrEmpty()) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .border(
                    width = borderWidth,
                    color = if (isToday) Color(0xFFD4A017) else Color(0x66FFFFFF),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = event.photoUrl,
                contentDescription = event.titleBn,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else if (drawableRes != null) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .border(
                    width = borderWidth,
                    color = if (isToday) Color(0xFFD4A017) else Color(0x55E2E8F0),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = event.titleBn,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        // Styled Vector Fallback with festival tint background and authentic iconography
        val fallback = EventVisualFallbackEngine.getFallback(event)
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Brush.radialGradient(fallback.gradientColors))
                .border(
                    width = borderWidth,
                    color = fallback.borderColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fallback.symbol,
                color = fallback.textColor,
                fontSize = textFontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
