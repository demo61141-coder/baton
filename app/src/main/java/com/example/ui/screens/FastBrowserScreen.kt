package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.AppSettings
import kotlinx.coroutines.launch

// Dynamic model representing individual browser tabs with standard compose state delegation
class BrowserTabState(
    val id: String = java.util.UUID.randomUUID().toString(),
    initialUrl: String,
    val webView: WebView
) {
    var url by mutableStateOf(initialUrl)
    var title by mutableStateOf("New Tab")
    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)
    var progress by mutableStateOf(0)
    var isLoading by mutableStateOf(false)
}

enum class ShortcutType {
    LETTER,
    SWIRL,
    DOWNLOAD,
    PLAY,
    SPEECH
}

data class ShortcutItem(
    val name: String,
    val url: String,
    val color: Color,
    val type: ShortcutType,
    val label: String
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FastBrowserScreen(
    appSettings: AppSettings,
    onExitBrowser: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    // Manage a dynamic reactive list of browser tabs
    val tabs = remember { mutableStateListOf<BrowserTabState>() }
    var activeTabId by remember { mutableStateOf("") }
    var showTabManager by remember { mutableStateOf(false) }
    var showMenuDropdown by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf("") }

    // List of detected video streaming/file URLs
    val detectedVideos = remember { mutableStateListOf<Pair<String, String>>() }
    var showDownloadSheet by remember { mutableStateOf(false) }

    // JavaScript interface helper to detect videos across tabs
    val videoDetectorInterface = remember {
        object {
            @JavascriptInterface
            fun onVideoDetected(url: String, title: String) {
                if (url.isNotBlank() && !url.startsWith("blob:") && !url.startsWith("data:")) {
                    if (detectedVideos.none { it.first == url }) {
                        detectedVideos.add(Pair(url, title))
                    }
                }
            }
        }
    }

    // List of shortcuts exactly matching the screenshot
    val shortcuts = remember {
        listOf(
            ShortcutItem("Coding", "https://github.com", Color(0xFF95D5B2), ShortcutType.LETTER, "C"),
            ShortcutItem("VibeVoice", "https://websim.ai/c/vibevoice", Color(0xFF90CAF9), ShortcutType.LETTER, "V"),
            ShortcutItem("Websim", "https://websim.ai", Color(0xFF81D4FA), ShortcutType.LETTER, "W"),
            ShortcutItem("Youtube", "https://youtube.com", Color(0xFFCE93D8), ShortcutType.LETTER, "Y"),
            ShortcutItem("YouTube", "https://youtube.com", Color(0xFF29B6F6), ShortcutType.SWIRL, ""),
            ShortcutItem("Download", "https://websim.ai/c/downloader", Color(0xFF1E88E5), ShortcutType.DOWNLOAD, "N"),
            ShortcutItem("Generate", "https://websim.ai", Color(0xFFEF5350), ShortcutType.PLAY, ""),
            ShortcutItem("YouTube", "https://youtube.com", Color(0xFF4DB6AC), ShortcutType.LETTER, "Y"),
            ShortcutItem("Free", "https://websim.ai/c/free", Color(0xFF212121), ShortcutType.SPEECH, ""),
            ShortcutItem("YouTube", "https://youtube.com", Color(0xFF80CBC4), ShortcutType.LETTER, "Y"),
            ShortcutItem("YouTube", "https://youtube.com", Color(0xFF9E9E9E), ShortcutType.LETTER, "Y"),
            ShortcutItem("Free", "https://websim.ai/c/free-games", Color(0xFFF48FB1), ShortcutType.LETTER, "F")
        )
    }

    // Helper method to instantiate new WebViews for clean tab separation
    fun createNewTab(urlStr: String): BrowserTabState {
        val wv = WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                builtInZoomControls = true
                displayZoomControls = false
                userAgentString = "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                setSupportMultipleWindows(true)
            }
        }

        val tabState = BrowserTabState(initialUrl = urlStr, webView = wv)

        wv.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, startUrl: String?, favicon: Bitmap?) {
                super.onPageStarted(view, startUrl, favicon)
                tabState.isLoading = true
                startUrl?.let {
                    if (it != "about:blank") {
                        tabState.url = it
                    }
                }
            }

            override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                super.onPageFinished(view, finishedUrl)
                tabState.isLoading = false
                tabState.canGoBack = view?.canGoBack() ?: false
                tabState.canGoForward = view?.canGoForward() ?: false
                tabState.title = view?.title ?: "New Tab"
                finishedUrl?.let {
                    if (it != "about:blank") {
                        tabState.url = it
                    }
                }

                // Inject fast video scanner script
                view?.loadUrl(
                    "javascript:(function() { " +
                            "var checkVideos = function() { " +
                            "  var vids = document.getElementsByTagName('video'); " +
                            "  for (var i = 0; i < vids.length; i++) { " +
                            "    var v = vids[i]; " +
                            "    if (v && v.src && v.src.indexOf('blob:') !== 0) { " +
                            "      window.VideoDetector.onVideoDetected(v.src, document.title || 'Video File'); " +
                            "    } " +
                            "    if (v && v.currentSrc && v.currentSrc.indexOf('blob:') !== 0) { " +
                            "      window.VideoDetector.onVideoDetected(v.currentSrc, document.title || 'Video File'); " +
                            "    } " +
                            "    v.addEventListener('play', function() { " +
                            "      if (this.currentSrc && this.currentSrc.indexOf('blob:') !== 0) { " +
                            "        window.VideoDetector.onVideoDetected(this.currentSrc, document.title || 'Video File'); " +
                            "      } " +
                            "    }); " +
                            "  } " +
                            "  var srcs = document.getElementsByTagName('source'); " +
                            "  for (var j = 0; j < srcs.length; j++) { " +
                            "    var s = srcs[j]; " +
                            "    if (s && s.src && s.src.indexOf('blob:') !== 0) { " +
                            "      window.VideoDetector.onVideoDetected(s.src, document.title || 'Video Source'); " +
                            "    } " +
                            "  } " +
                            "}; " +
                            "checkVideos(); " +
                            "setInterval(checkVideos, 3000); " +
                            "})()"
                )
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val reqUrl = request?.url?.toString() ?: ""
                val cleanUrl = reqUrl.lowercase()

                if (cleanUrl.contains(".mp4") ||
                    cleanUrl.contains(".m3u8") ||
                    cleanUrl.contains(".mkv") ||
                    cleanUrl.contains(".webm") ||
                    cleanUrl.contains("/videoplayback") ||
                    cleanUrl.contains(".mov") ||
                    cleanUrl.contains(".mp3")
                ) {
                    if (!reqUrl.startsWith("data:") &&
                        !reqUrl.startsWith("blob:") &&
                        !reqUrl.contains("ads") &&
                        !reqUrl.contains("doubleclick") &&
                        !reqUrl.contains("google-analytics") &&
                        !reqUrl.contains("analytics")
                    ) {
                        view?.post {
                            if (detectedVideos.none { it.first == reqUrl }) {
                                detectedVideos.add(Pair(reqUrl, view.title ?: "Video stream"))
                            }
                        }
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }
        }

        wv.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                tabState.progress = newProgress
                if (newProgress >= 100) {
                    tabState.isLoading = false
                }
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message?
            ): Boolean {
                val transport = resultMsg?.obj as? WebView.WebViewTransport
                if (transport != null) {
                    val newTab = createNewTab("internal://homepage")
                    tabs.add(newTab)
                    activeTabId = newTab.id
                    transport.webView = newTab.webView
                    resultMsg.sendToTarget()
                    return true
                }
                return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
            }
        }

        wv.addJavascriptInterface(videoDetectorInterface, "VideoDetector")
        if (urlStr != "internal://homepage") {
            wv.loadUrl(urlStr)
        }
        return tabState
    }

    // Initialize with a default single tab if none exists
    if (tabs.isEmpty()) {
        val defaultTab = createNewTab("internal://homepage")
        tabs.add(defaultTab)
        activeTabId = defaultTab.id
    }

    // Reference to the currently active tab
    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.firstOrNull()
    val isLoading = activeTab?.isLoading ?: false
    val progressVal = activeTab?.progress ?: 0
    val canGoBackState = activeTab?.canGoBack ?: false
    val canGoForwardState = activeTab?.canGoForward ?: false
    val isHomepage = activeTab?.url == "internal://homepage"

    // Auto-sync search input with the current tab's active URL
    LaunchedEffect(activeTab?.url) {
        activeTab?.url?.let {
            searchInput = if (it == "internal://homepage" || it == "about:blank") "" else it
        }
    }

    // System Back Button handles back navigation beautifully
    BackHandler(enabled = true) {
        if (canGoBackState) {
            activeTab?.webView?.goBack()
        } else if (!isHomepage) {
            activeTab?.url = "internal://homepage"
            searchInput = ""
        } else {
            onExitBrowser()
        }
    }

    fun navigateTab(tab: BrowserTabState, query: String) {
        var targetUrl = query.trim()
        if (targetUrl.isNotBlank()) {
            if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
                if (targetUrl.contains(".") && !targetUrl.contains(" ")) {
                    targetUrl = "https://$targetUrl"
                } else {
                    targetUrl = "https://www.google.com/search?q=" + Uri.encode(targetUrl)
                }
            }
            tab.url = targetUrl
            tab.webView.loadUrl(targetUrl)
        }
    }

    Scaffold(
        topBar = {
            if (isHomepage) {
                // Customized Homepage TopBar matching the screenshot exactly
                Surface(
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search icon",
                            tint = Color.Black,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Homepage",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        ScanBracketIcon(
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                // WebView active TopBar supporting search/URL and navigation
                Surface(
                    tonalElevation = 3.dp,
                    color = Color(0xFFFEF7FF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    activeTab?.url = "internal://homepage"
                                    searchInput = ""
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Homepage"
                                )
                            }

                            OutlinedTextField(
                                value = searchInput,
                                onValueChange = { searchInput = it },
                                placeholder = { Text("Search or type URL", fontSize = 13.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Search
                                ),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        keyboardController?.hide()
                                        activeTab?.let { navigateTab(it, searchInput) }
                                    }
                                ),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        modifier = Modifier.size(18.dp),
                                        tint = Color.Gray
                                    )
                                },
                                trailingIcon = {
                                    if (searchInput.isNotEmpty()) {
                                        IconButton(
                                            onClick = { searchInput = "" },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    disabledContainerColor = Color.White,
                                    focusedIndicatorColor = Color(0xFF6750A4),
                                    unfocusedIndicatorColor = Color(0xFFCAC4D0)
                                ),
                                shape = RoundedCornerShape(24.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            )

                            IconButton(
                                onClick = {
                                    if (isLoading) {
                                        activeTab?.webView?.stopLoading()
                                    } else {
                                        activeTab?.webView?.reload()
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                                    contentDescription = if (isLoading) "Stop" else "Reload"
                                )
                            }
                        }

                        // Progress bar for loading
                        AnimatedVisibility(
                            visible = isLoading,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            LinearProgressIndicator(
                                progress = { progressVal / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp),
                                color = Color(0xFF6750A4),
                                trackColor = Color(0xFFEADDFF)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Elegant mobile browser Navigation BottomBar matching the screenshot exactly
            Surface(
                color = Color.White,
                tonalElevation = 6.dp,
                border = BorderStroke(0.5.dp, Color(0xFFE0E0E0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Back arrow icon
                    IconButton(
                        onClick = { activeTab?.webView?.goBack() },
                        enabled = canGoBackState
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (canGoBackState) Color.Black else Color.LightGray,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Forward arrow icon
                    IconButton(
                        onClick = { activeTab?.webView?.goForward() },
                        enabled = canGoForwardState
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Forward",
                            tint = if (canGoForwardState) Color.Black else Color.LightGray,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Home icon (line/outline style)
                    IconButton(
                        onClick = {
                            activeTab?.url = "internal://homepage"
                            searchInput = ""
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Browser Home",
                            tint = Color.Black,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Tab badge icon with current count inside a square box
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(2.dp, Color.Black, RoundedCornerShape(5.dp))
                            .clickable { showTabManager = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabs.size.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.Black
                        )
                    }

                    // Menu icon (three parallel lines)
                    Box {
                        IconButton(
                            onClick = { showMenuDropdown = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Browser Menu",
                                tint = Color.Black,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenuDropdown,
                            onDismissRequest = { showMenuDropdown = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text("নতুন ট্যাব (New Tab)", fontWeight = FontWeight.Bold, color = Color.Black) },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black) },
                                onClick = {
                                    showMenuDropdown = false
                                    val newTab = createNewTab("internal://homepage")
                                    tabs.add(newTab)
                                    activeTabId = newTab.id
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("ডাউনলোড ম্যানেজার", color = Color.Black) },
                                leadingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Black) },
                                onClick = {
                                    showMenuDropdown = false
                                    showDownloadSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("অ্যাপ স্ক্রিন (Streams)", color = Color.Red) },
                                leadingIcon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.Red) },
                                onClick = {
                                    showMenuDropdown = false
                                    onExitBrowser()
                                }
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = detectedVideos.isNotEmpty(),
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    ExtendedFloatingActionButton(
                        onClick = { showDownloadSheet = true },
                        containerColor = Color(0xFF34A853),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Download Video"
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Download (${detectedVideos.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .offset(x = (-4).dp, y = (-4).dp)
                            .size(18.dp)
                            .background(Color.Red, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = detectedVideos.size.toString(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            if (isHomepage) {
                // Custom high-fidelity Compose Homepage screen
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .background(Color.White)
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(72.dp))

                    // Stylized folded colorful logo centered
                    BrowserLogo(
                        modifier = Modifier.size(130.dp, 100.dp)
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    // Capsule-shaped empty search input field
                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = {
                            searchInput = it
                            if (it.isNotEmpty()) {
                                // If they start typing on the homepage, keep it in the search input
                            }
                        },
                        placeholder = { }, // Kept blank to match the screenshot perfectly
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboardController?.hide()
                                if (searchInput.isNotBlank()) {
                                    activeTab?.let { navigateTab(it, searchInput) }
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFFE0E0E0),
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            cursorColor = Color.Black
                        ),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(56.dp)
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    // Beautiful 3x4 grid of circular shortcut icons
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        val rows = shortcuts.chunked(4)
                        rows.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                rowItems.forEach { item ->
                                    ShortcutIcon(
                                        item = item,
                                        onClick = {
                                            activeTab?.let { navigateTab(it, item.url) }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Render WebView dynamically using key separation
                if (activeTab != null) {
                    key(activeTab.id) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { activeTab.webView },
                            update = { /* handled by factory key */ }
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "কোন সাইট খোলা নেই",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Text(
                                text = "উপরে সার্চ করুন বা ঠিকানা প্রবেশ করান",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog showing all active tabs
    if (showTabManager) {
        ModalBottomSheet(
            onDismissRequest = { showTabManager = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ট্যাব ম্যানেজার (${tabs.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20)
                    )

                    Button(
                        onClick = {
                            val newTab = createNewTab("internal://homepage")
                            tabs.add(newTab)
                            activeTabId = newTab.id
                            showTabManager = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("নতুন ট্যাব", fontSize = 12.sp)
                    }
                }

                HorizontalDivider(color = Color(0xFFCAC4D0).copy(alpha = 0.5f))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(tabs) { tab ->
                        val isCurrent = tab.id == activeTabId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    activeTabId = tab.id
                                    showTabManager = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) Color(0xFFEADDFF) else Color(0xFFF9F9FB)
                            ),
                            border = if (isCurrent) BorderStroke(1.5.dp, Color(0xFF6750A4)) else null,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = if (isCurrent) Color(0xFF21005D) else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (tab.url == "internal://homepage") "Homepage" else tab.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isCurrent) Color(0xFF21005D) else Color(0xFF1D1B20),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (tab.url == "internal://homepage") "internal://homepage" else tab.url,
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        if (tabs.size > 1) {
                                            val index = tabs.indexOf(tab)
                                            tabs.remove(tab)
                                            if (isCurrent) {
                                                val nextActive = if (index > 0) tabs[index - 1] else tabs[0]
                                                activeTabId = nextActive.id
                                            }
                                        } else {
                                            Toast.makeText(context, "কমপক্ষে একটি ট্যাব সচল রাখতে হবে!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close tab",
                                        tint = Color.Red,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // Downloads BottomSheet
    if (showDownloadSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDownloadSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ভিডিও ডাউনলোড ম্যানেজার",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20)
                    )
                    TextButton(
                        onClick = {
                            detectedVideos.clear()
                            showDownloadSheet = false
                        }
                    ) {
                        Text("সব মুছুন", color = Color.Red)
                    }
                }

                HorizontalDivider(color = Color(0xFFCAC4D0).copy(alpha = 0.5f))

                if (detectedVideos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("কোন ভিডিও সনাক্ত করা হয়নি", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(detectedVideos) { video ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9FB)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = video.second.ifBlank { "Video Stream Source" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF1D1B20),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = video.first,
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            try {
                                                val uri = Uri.parse(video.first)
                                                val request = DownloadManager.Request(uri).apply {
                                                    setTitle(video.second.ifBlank { "Downloaded Video" })
                                                    setDescription("ডাউনলোড হচ্ছে...")
                                                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                                    setDestinationInExternalPublicDir(
                                                        Environment.DIRECTORY_DOWNLOADS,
                                                        "all_live_video_" + System.currentTimeMillis() + when {
                                                            video.first.contains(".mp4", ignoreCase = true) -> ".mp4"
                                                            video.first.contains(".mkv", ignoreCase = true) -> ".mkv"
                                                            video.first.contains(".webm", ignoreCase = true) -> ".webm"
                                                            video.first.contains(".mp3", ignoreCase = true) -> ".mp3"
                                                            else -> ".mp4"
                                                        }
                                                    )
                                                    setAllowedOverMetered(true)
                                                    setAllowedOverRoaming(true)
                                                }

                                                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                                downloadManager.enqueue(request)

                                                Toast.makeText(
                                                    context,
                                                    "ডাউনলোড শুরু হয়েছে! নোটিফিকেশন বার চেক করুন।",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    "ডাউনলোড ব্যর্থ হয়েছে: ${e.localizedMessage}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                            showDownloadSheet = false
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(Color(0xFFEADDFF), shape = CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Download Item",
                                            tint = Color(0xFF21005D),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// Custom Microsoft/WebSim colorful ribbon logo Composable
@Composable
fun BrowserLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = 18.dp.toPx()

        // Draw blue ribbon slanting up-right
        drawLine(
            color = Color(0xFF0081FF),
            start = androidx.compose.ui.geometry.Offset(w * 0.28f, h * 0.72f),
            end = androidx.compose.ui.geometry.Offset(w * 0.44f, h * 0.28f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Draw red-orange ribbon slanting down-right
        drawLine(
            color = Color(0xFFFF3B30),
            start = androidx.compose.ui.geometry.Offset(w * 0.44f, h * 0.28f),
            end = androidx.compose.ui.geometry.Offset(w * 0.58f, h * 0.72f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Draw yellow-orange ribbon slanting up-right
        drawLine(
            color = Color(0xFFFFCC00),
            start = androidx.compose.ui.geometry.Offset(w * 0.58f, h * 0.72f),
            end = androidx.compose.ui.geometry.Offset(w * 0.72f, h * 0.28f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

// Custom ScanBracketIcon `[-]` matching screenshot exactly
@Composable
fun ScanBracketIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()
        val len = 5.dp.toPx()

        // Top-left bracket
        drawPath(
            path = Path().apply {
                moveTo(0f, len)
                lineTo(0f, 0f)
                lineTo(len, 0f)
            },
            color = Color.Black,
            style = Stroke(width = stroke)
        )
        // Top-right bracket
        drawPath(
            path = Path().apply {
                moveTo(w - len, 0f)
                lineTo(w, 0f)
                lineTo(w, len)
            },
            color = Color.Black,
            style = Stroke(width = stroke)
        )
        // Bottom-left bracket
        drawPath(
            path = Path().apply {
                moveTo(0f, h - len)
                lineTo(0f, h)
                lineTo(len, h)
            },
            color = Color.Black,
            style = Stroke(width = stroke)
        )
        // Bottom-right bracket
        drawPath(
            path = Path().apply {
                moveTo(w - len, h)
                lineTo(w, h)
                lineTo(w, h - len)
            },
            color = Color.Black,
            style = Stroke(width = stroke)
        )
        // Center minus line
        drawLine(
            color = Color.Black,
            start = androidx.compose.ui.geometry.Offset(w * 0.35f, h * 0.5f),
            end = androidx.compose.ui.geometry.Offset(w * 0.65f, h * 0.5f),
            strokeWidth = stroke
        )
    }
}

// Render dynamic custom shortcut icons matching each type
@Composable
fun ShortcutIcon(
    item: ShortcutItem,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .width(72.dp)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(item.color, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when (item.type) {
                ShortcutType.LETTER -> {
                    Text(
                        text = item.label,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                ShortcutType.SWIRL -> {
                    SwirlIcon()
                }
                ShortcutType.DOWNLOAD -> {
                    DownloadIcon()
                }
                ShortcutType.PLAY -> {
                    PlayIcon()
                }
                ShortcutType.SPEECH -> {
                    SpeechIcon()
                }
            }
        }
        Text(
            text = item.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SwirlIcon() {
    Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        val w = size.width
        val h = size.height
        val center = androidx.compose.ui.geometry.Offset(w / 2f, h / 2f)

        // Mathematical Archimedean Spiral representing swirl
        val path = Path()
        val steps = 360 * 2
        for (i in 0..steps step 5) {
            val angleRad = Math.toRadians(i.toDouble())
            val r = (w * 0.38f) * (i.toFloat() / steps)
            val x = center.x + r * Math.cos(angleRad).toFloat()
            val y = center.y + r * Math.sin(angleRad).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = Color.White,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun DownloadIcon() {
    Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 3.5.dp.toPx()

        // Stylized ribbon N shape
        drawLine(Color.White, androidx.compose.ui.geometry.Offset(w * 0.28f, h * 0.72f), androidx.compose.ui.geometry.Offset(w * 0.28f, h * 0.28f), strokeWidth, StrokeCap.Round)
        drawLine(Color.White, androidx.compose.ui.geometry.Offset(w * 0.28f, h * 0.28f), androidx.compose.ui.geometry.Offset(w * 0.72f, h * 0.72f), strokeWidth, StrokeCap.Round)
        drawLine(Color.White, androidx.compose.ui.geometry.Offset(w * 0.72f, h * 0.72f), androidx.compose.ui.geometry.Offset(w * 0.72f, h * 0.28f), strokeWidth, StrokeCap.Round)
    }
}

@Composable
fun PlayIcon() {
    Canvas(modifier = Modifier.fillMaxSize().padding(15.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.35f, h * 0.25f)
            lineTo(w * 0.75f, h * 0.5f)
            lineTo(w * 0.35f, h * 0.75f)
            close()
        }
        drawPath(path, color = Color.White)
    }
}

@Composable
fun SpeechIcon() {
    Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        val w = size.width
        val h = size.height

        // Rounded speech bubble path
        val path = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    rect = androidx.compose.ui.geometry.Rect(w * 0.15f, h * 0.15f, w * 0.85f, h * 0.68f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx(), 5.dp.toPx())
                )
            )
            // Speech tail
            moveTo(w * 0.35f, h * 0.68f)
            lineTo(w * 0.25f, h * 0.82f)
            lineTo(w * 0.45f, h * 0.68f)
        }
        drawPath(path, color = Color.White)

        // Three dots inside
        val dotRadius = 1.8.dp.toPx()
        drawCircle(Color.Black, radius = dotRadius, center = androidx.compose.ui.geometry.Offset(w * 0.35f, h * 0.42f))
        drawCircle(Color.Black, radius = dotRadius, center = androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.42f))
        drawCircle(Color.Black, radius = dotRadius, center = androidx.compose.ui.geometry.Offset(w * 0.65f, h * 0.42f))
    }
}
