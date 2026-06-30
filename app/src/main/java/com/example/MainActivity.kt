package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.data.AppConfigManager
import com.example.data.AppSettings
import com.example.data.WatchButton
import com.example.ui.components.LogoAnimationScreen
import com.example.ui.components.VideoAdOverlay
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.WebViewScreen
import com.example.ui.theme.MyApplicationTheme

sealed interface Screen {
    object Splash : Screen
    object Home : Screen
    data class Browser(val url: String, val title: String) : Screen
    object Admin : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
                var activeAdNetwork by remember { mutableStateOf<String?>(null) }
                var onAdFinishedCallback by remember { mutableStateOf<(() -> Unit)?>(null) }

                val context = LocalContext.current
                var settings by remember { mutableStateOf(AppConfigManager.loadSettings(context)) }

                // Trigger reload of configuration settings
                val updateSettings = {
                    settings = AppConfigManager.loadSettings(context)
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Custom routing flow using high-performance Compose transitions
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "MainScreenNavigation"
                        ) { screen ->
                            when (screen) {
                                is Screen.Splash -> {
                                    LogoAnimationScreen(
                                        onAnimationComplete = {
                                            currentScreen = Screen.Home
                                        }
                                    )
                                }
                                is Screen.Home -> {
                                    HomeScreen(
                                        onPortalSelected = { button ->
                                            if (settings.adsEnabled && button.adsEnabled) {
                                                // Pre-cache callback action
                                                onAdFinishedCallback = {
                                                    currentScreen = Screen.Browser(button.url, button.name)
                                                }
                                                activeAdNetwork = settings.adNetwork
                                            } else {
                                                currentScreen = Screen.Browser(button.url, button.name)
                                            }
                                        },
                                        onAdminClick = {
                                            currentScreen = Screen.Admin
                                        }
                                    )
                                }
                                is Screen.Browser -> {
                                    WebViewScreen(
                                        url = screen.url,
                                        title = screen.title,
                                        adsEnabled = settings.adsEnabled,
                                        adNetwork = settings.adNetwork,
                                        backAdEnabled = settings.backAdEnabled,
                                        onBack = {
                                            currentScreen = Screen.Home
                                        },
                                        triggerAd = { onAdFinished ->
                                            onAdFinishedCallback = onAdFinished
                                            activeAdNetwork = settings.adNetwork
                                        }
                                    )
                                }
                                is Screen.Admin -> {
                                    AdminScreen(
                                        onBack = {
                                            currentScreen = Screen.Home
                                        },
                                        onConfigUpdated = {
                                            updateSettings()
                                        }
                                    )
                                }
                            }
                        }

                        // Full-screen video advertisement overlay layer
                        activeAdNetwork?.let { adNetwork ->
                            VideoAdOverlay(
                                networkName = adNetwork,
                                appSettings = settings,
                                onAdDismissed = {
                                    activeAdNetwork = null
                                    onAdFinishedCallback?.invoke()
                                    onAdFinishedCallback = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
