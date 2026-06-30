package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppConfigManager
import kotlinx.coroutines.delay

@Composable
fun LogoAnimationScreen(onAnimationComplete: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { AppConfigManager.loadSettings(context) }
    val splashLogoBase64 = settings.splashLogoBase64

    // Animation states
    var startAnimations by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (startAnimations) 1.2f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "LogoScale"
    )

    val rotateAngle by animateFloatAsState(
        targetValue = if (startAnimations) 360f else 0f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "LogoRotation"
    )

    val alphaProgress by animateFloatAsState(
        targetValue = if (startAnimations) 1.0f else 0.0f,
        animationSpec = tween(1200, easing = LinearEasing),
        label = "TextAlpha"
    )

    val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    LaunchedEffect(Unit) {
        startAnimations = true
        // Keep splash active for 1.0 second for snappy loading, then transition
        delay(1000)
        onAnimationComplete()
    }

    // Sleek Interface Light Theme Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFEF7FF)),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background elements
        Box(
            modifier = Modifier
                .size(300.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFD0BCFF).copy(alpha = 0.25f),
                                Color.Transparent
                             )
                        ),
                        radius = size.width / 2
                    )
                }
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Main glowing logo icon container
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(scale * if (startAnimations) pulseScale else 1.0f)
                    .rotate(rotateAngle)
                    .drawBehind {
                        // Procedural Sleek ring gradient
                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFF6750A4), // M3 Primary Purple
                                    Color(0xFFD0BCFF), // Light purple
                                    Color(0xFF6750A4)
                                )
                            ),
                            style = Stroke(width = 6.dp.toPx())
                        )
                        // Second glowing outer ring
                        drawCircle(
                            color = Color(0xFF6750A4).copy(alpha = 0.3f),
                            radius = (size.width / 2) + 12.dp.toPx(),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                    .clip(CircleShape)
                    .background(Color(0xFF6750A4)),
                contentAlignment = Alignment.Center
            ) {
                if (!splashLogoBase64.isNullOrBlank()) {
                    val bitmap = remember(splashLogoBase64) {
                        try {
                            val decodedString = Base64.decode(splashLogoBase64, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Custom Splash Logo",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "All Live Play Stream Logo",
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "All Live Play Stream Logo",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Brand Text
            Text(
                text = "All Live",
                color = Color(0xFF1D1B20),
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.5).sp,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.drawBehind {
                    // Accent underline
                    val strokeWidth = 3.dp.toPx()
                    val y = size.height + 8.dp.toPx()
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF6750A4),
                                Color(0xFFD0BCFF),
                                Color(0xFFEADDFF)
                            )
                        ),
                        start = androidx.compose.ui.geometry.Offset(size.width * 0.15f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width * 0.85f, y),
                        strokeWidth = strokeWidth
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "DYNAMIC STREAM ENGINE",
                color = Color(0xFF49454F).copy(alpha = alphaProgress * 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
    }
}
