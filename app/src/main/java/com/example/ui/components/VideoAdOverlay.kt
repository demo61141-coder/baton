package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AppSettings
import kotlinx.coroutines.delay

@Composable
fun VideoAdOverlay(
    networkName: String, // "monetag" or "startapp"
    appSettings: AppSettings? = null,
    onAdDismissed: () -> Unit
) {
    val context = LocalContext.current
    var secondsRemaining by remember { mutableStateOf(5) }
    var isSkipEnabled by remember { mutableStateOf(false) }
    var videoProgress by remember { mutableStateOf(0f) }

    val isPersonalAd = appSettings?.personalAdEnabled == true

    // Countdown and video progress updates
    LaunchedEffect(Unit) {
        val totalDuration = 5000L // 5 seconds
        val step = 100L
        var elapsed = 0L

        while (elapsed < totalDuration) {
            delay(step)
            elapsed += step
            videoProgress = elapsed.toFloat() / totalDuration
            val remaining = ((totalDuration - elapsed) / 1000).toInt() + 1
            if (remaining != secondsRemaining) {
                secondsRemaining = remaining
            }
        }
        secondsRemaining = 0
        isSkipEnabled = true
        videoProgress = 1.0f
    }

    // Colors according to ad network branding
    val networkThemeColor = if (isPersonalAd) {
        Color(0xFF34A853) // Green for Personal/Direct Ads
    } else if (networkName.equals("monetag", ignoreCase = true)) {
        Color(0xFFFF5D55) // Monetag Red-Coral
    } else {
        Color(0xFF3F51B5) // StartApp Indigo
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .clickable(enabled = false) {}, // Intercept touch events
        contentAlignment = Alignment.Center
    ) {
        // Neon tech grid backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                networkThemeColor.copy(alpha = 0.12f),
                                Color.Transparent,
                                Color.Black
                            )
                        )
                    )
                }
        )

        // Main Ad Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .systemBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Ad network label + Skip timer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ad Badge
                Surface(
                    color = networkThemeColor,
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 4.dp
                ) {
                    Text(
                        text = if (isPersonalAd) "SPONSORED DIRECT AD" else "${networkName.uppercase()} AD",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                // Skip/Close Button containing countdown
                Surface(
                    color = if (isSkipEnabled) Color.White else Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.4f), Color.Transparent))
                    ),
                    modifier = Modifier
                        .clickable(enabled = isSkipEnabled) { onAdDismissed() }
                        .testTag("ad_close_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (!isSkipEnabled) {
                            Text(
                                text = "Close in $secondsRemaining s",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "Skip",
                                color = Color.Black,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Ad",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Central Area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 24.dp)
                    .clickable {
                        if (isPersonalAd && appSettings?.personalAdClickUrl?.isNotBlank() == true) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(appSettings.personalAdClickUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback
                            }
                        }
                    },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14141E)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.sweepGradient(listOf(networkThemeColor.copy(alpha = 0.8f), Color.Transparent))
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPersonalAd && appSettings != null) {
                        AsyncImage(
                            model = appSettings.personalAdImageUrl,
                            contentDescription = "Personal Ad Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Visual overlay for progress
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "ট্যাপ করে স্পনসর ভিজিট করুন",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    } else {
                        // Simulated visual equalizer / playback pulse
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Simulated Video Ad Stream Playing",
                                tint = networkThemeColor.copy(alpha = 0.8f),
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Streaming Video Ad Showcase",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Loading target premium portal in background...",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Top-left watermark of ad playing
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            progress = { videoProgress },
                            color = networkThemeColor,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPersonalAd) "Direct Ad" else "Live Stream Ad",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }

                    // Bottom progress track
                    LinearProgressIndicator(
                        progress = { videoProgress },
                        color = networkThemeColor,
                        trackColor = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .height(6.dp)
                    )
                }
            }

            // Bottom CTA Box: Install / Unlock Target Link
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isPersonalAd && appSettings?.personalAdClickUrl?.isNotBlank() == true) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(appSettings.personalAdClickUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback
                            }
                        }
                    },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B2B)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.1f), Color.Transparent))
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPersonalAd) "Sponsored Offer" else "Cyberpunk Arena 3D",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isPersonalAd) "নিয়মিত অফার দেখতে ট্যাপ করুন" else "4.9 ★ • Interactive RPG",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (isPersonalAd && appSettings?.personalAdClickUrl?.isNotBlank() == true) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(appSettings.personalAdClickUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    onAdDismissed()
                                }
                            } else {
                                onAdDismissed()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = networkThemeColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isPersonalAd) "VISIT NOW" else "INSTALL NOW",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
