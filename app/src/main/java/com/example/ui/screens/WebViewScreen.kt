package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    url: String,
    title: String,
    adsEnabled: Boolean,
    adNetwork: String,
    backAdEnabled: Boolean,
    onBack: () -> Unit,
    triggerAd: (onAdFinished: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentTitle by remember { mutableStateOf(title) }
    var currentUrl by remember { mutableStateOf(url) }
    var isLoading by remember { mutableStateOf(true) }
    var progressVal by remember { mutableStateOf(0) }

    // Intercept hardware/system back button
    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            // Trigger Back Ad before exit if enabled
            if (adsEnabled && backAdEnabled) {
                triggerAd {
                    onBack()
                }
            } else {
                onBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentUrl,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (webViewInstance?.canGoBack() == true) {
                                webViewInstance?.goBack()
                            } else {
                                if (adsEnabled && backAdEnabled) {
                                    triggerAd { onBack() }
                                } else {
                                    onBack()
                                }
                            }
                        },
                        modifier = Modifier.testTag("webview_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Reload button
                    IconButton(
                        onClick = { webViewInstance?.reload() },
                        modifier = Modifier.testTag("webview_reload_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload Page",
                            tint = Color.White
                        )
                    }

                    // Share button
                    IconButton(
                        onClick = {
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, currentUrl)
                            }
                            context.startActivity(
                                android.content.Intent.createChooser(shareIntent, "Share Stream URL")
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Stream",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F0C1B),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF05050A))
        ) {
            // Main Android WebView integration
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("main_web_view"),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, urlStr: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, urlStr, favicon)
                                isLoading = true
                                urlStr?.let { currentUrl = it }
                            }

                            override fun onPageFinished(view: WebView?, urlStr: String?) {
                                super.onPageFinished(view, urlStr)
                                isLoading = false
                                view?.title?.let { currentTitle = it }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                progressVal = newProgress
                                if (newProgress >= 100) {
                                    isLoading = false
                                }
                            }
                        }

                        loadUrl(url)
                        webViewInstance = this
                    }
                },
                update = { webView ->
                    // Handle potential state updates safely
                }
            )

            // Web Loading Linear Progress bar
            AnimatedVisibility(
                visible = isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                LinearProgressIndicator(
                    progress = { progressVal / 100f },
                    color = Color(0xFF00F2FE),
                    trackColor = Color(0xFF161233),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                )
            }
        }
    }
}
