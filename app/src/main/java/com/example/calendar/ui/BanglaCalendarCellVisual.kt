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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.calendar.model.CalendarEvent
import com.example.calendar.model.EventVisualType

/**
 * Renders authentic, high-resolution original-color photographs for deities,
 * historical personalities, real astronomical moon photos, or styled badges
 * for Hindu and Islamic festivals.
 */
@Composable
fun CalendarCellVisual(
    event: CalendarEvent,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    val drawableRes = when (event.visualType) {
        EventVisualType.KRISHNA -> R.drawable.photo_krishna
        EventVisualType.RADHAKRISHNAN -> R.drawable.photo_radhakrishnan
        EventVisualType.GANESHA -> R.drawable.photo_ganesha
        EventVisualType.VISHWAKARMA -> R.drawable.photo_vishwakarma
        EventVisualType.UTTAM_KUMAR -> R.drawable.photo_uttam_kumar
        EventVisualType.TITHI_PURNIMA -> R.drawable.photo_moon_full
        EventVisualType.TITHI_AMAVASYA -> R.drawable.photo_moon_dark
        EventVisualType.DURGA -> R.drawable.photo_durga
        EventVisualType.SARASWATI -> R.drawable.photo_saraswati
        EventVisualType.SHIVA -> R.drawable.photo_shiva
        EventVisualType.JAGANNATH -> R.drawable.photo_jagannath
        EventVisualType.VIVEKANANDA -> R.drawable.photo_vivekananda
        EventVisualType.NETAJI -> R.drawable.photo_netaji
        EventVisualType.RABINDRANATH -> R.drawable.photo_tagore
        EventVisualType.NAZRUL -> R.drawable.photo_nazrul
        EventVisualType.VIDYASAGAR -> R.drawable.photo_vidyasagar
        EventVisualType.GANDHI -> R.drawable.photo_gandhi
        EventVisualType.AMBEDKAR -> R.drawable.photo_ambedkar
        else -> null
    }

    if (!event.photoUrl.isNullOrEmpty()) {
        Box(
            modifier = modifier
                .size(28.dp)
                .clip(CircleShape)
                .border(
                    width = 1.dp,
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
                .size(28.dp)
                .clip(CircleShape)
                .border(
                    width = 1.dp,
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
    } else if (event.type == "ISLAMIC" || event.visualType == EventVisualType.ISLAMIC_CRESCENT || event.visualType == EventVisualType.EID) {
        // Emerald Green Islamic Crescent & Star Badge
        Box(
            modifier = modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF059669), Color(0xFF064E3B))
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFF34D399),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "☪",
                color = Color(0xFFECFDF5),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    } else if (event.type == "HINDU" || event.visualType == EventVisualType.HINDU_DIYA) {
        // Saffron / Gold Hindu Festival Badge
        Box(
            modifier = modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFEA580C), Color(0xFF7C2D12))
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFFFBBF24),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "ॐ",
                color = Color(0xFFFFFBEB),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    } else if (event.visualType == EventVisualType.BIRTHDAY_CAKE) {
        Box(
            modifier = modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF9333EA), Color(0xFF581C87))
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFFD8B4FE),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🎂",
                fontSize = 12.sp
            )
        }
    }
}
