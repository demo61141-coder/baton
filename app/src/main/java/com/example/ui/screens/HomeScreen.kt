package com.example.ui.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import android.graphics.BitmapFactory
import android.util.Base64

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPortalSelected: (WatchButton) -> Unit,
    onAdminClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current

    // Dynamic states
    var settings by remember { mutableStateOf(AppConfigManager.loadSettings(context)) }
    var buttons by remember { mutableStateOf(AppConfigManager.loadButtons(context)) }
    var premiumItems by remember { mutableStateOf(AppConfigManager.loadPremiumItems(context)) }
    var donations by remember { mutableStateOf(AppConfigManager.loadDonations(context)) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Navigation and hidden admin access controls
    var currentTab by remember { mutableStateOf(0) } // 0: Streams, 1: IPTV, 2: Premium, 3: Support & Feedback
    var titleTapCount by remember { mutableStateOf(0) }
    var showSecurityDialog by remember { mutableStateOf(false) }

    // Three-dot menu and 5-sec custom ad variables
    var showMenuDropdown by remember { mutableStateOf(false) }
    var showAdOverlay3Dot by remember { mutableStateOf(false) }
    var ad3DotSecondsRemaining by remember { mutableStateOf(5) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Reload callback to trigger on updates
    fun reloadAll() {
        settings = AppConfigManager.loadSettings(context)
        buttons = AppConfigManager.loadButtons(context)
        premiumItems = AppConfigManager.loadPremiumItems(context)
        donations = AppConfigManager.loadDonations(context)
    }

    LaunchedEffect(Unit) {
        // Step 1: Instantly load cached configurations so home screen opens without any hanging
        reloadAll()

        // Step 2: Fetch and sync with Google Sheets asynchronously in the background
        if (settings.sheetId.isNotBlank()) {
            coroutineScope.launch {
                try {
                    val success = GoogleSheetsManager.fetchAndSyncConfig(context, settings.sheetId)
                    if (success) {
                        reloadAll()
                    }
                } catch (e: Exception) {
                    // Fail silently to keep the experience completely seamless and offline-first
                }
            }
        }
    }

    // Trigger 5-sec ad loop before showing the developer dialog
    fun trigger3DotAboutFlow() {
        showMenuDropdown = false
        ad3DotSecondsRemaining = settings.showThreeDotAdDelaySeconds.coerceAtLeast(3)
        showAdOverlay3Dot = true
        coroutineScope.launch {
            while (ad3DotSecondsRemaining > 0) {
                delay(1000)
                ad3DotSecondsRemaining--
            }
            showAdOverlay3Dot = false
            showAboutDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .clickable(
                                interactionSource = null,
                                indication = null
                            ) {
                                titleTapCount++
                                if (titleTapCount >= 10) {
                                    titleTapCount = 0
                                    showSecurityDialog = true
                                }
                            }
                    ) {
                        // Material 3 Style Avatar Badge
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF6750A4)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "AL",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Column {
                            Text(
                                text = "All Live",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D1B20)
                            )
                        }
                    }
                },
                actions = {
                    // Quick Refresh Button
                    IconButton(
                        onClick = {
                            isRefreshing = true
                            coroutineScope.launch {
                                val success = GoogleSheetsManager.fetchAndSyncConfig(context, settings.sheetId)
                                isRefreshing = false
                                if (success) {
                                    Toast.makeText(context, "Streams updated from Cloud!", Toast.LENGTH_SHORT).show()
                                    reloadAll()
                                } else {
                                    Toast.makeText(context, "Offline mode. Loaded local config.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.testTag("refresh_home_button")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(color = Color(0xFF6750A4), modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync Now",
                                tint = Color(0xFF49454F)
                            )
                        }
                    }

                    // Three dot dropdown triggers 5-second ad
                    Box {
                        IconButton(onClick = { showMenuDropdown = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = Color(0xFF49454F))
                        }
                        DropdownMenu(
                            expanded = showMenuDropdown,
                            onDismissRequest = { showMenuDropdown = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text("প্রতিষ্ঠাতা ও প্রিমিয়ার (5s Ad)", fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF6750A4)) },
                                onClick = { trigger3DotAboutFlow() }
                            )
                            DropdownMenuItem(
                                text = { Text("ডোনেশন ও সাবস্ক্রিপশন") },
                                leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.Red) },
                                onClick = {
                                    showMenuDropdown = false
                                    currentTab = 3 // support tab
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("নতুন ফিচার (Coming Soon)", color = Color.Gray) },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.Gray) },
                                onClick = {
                                    showMenuDropdown = false
                                    Toast.makeText(context, "ভবিষ্যতে আরও চমৎকার ফিচার এডমিন প্যানেল থেকে এখানে সরাসরি যোগ করা হবে!", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFEF7FF),
                    titleContentColor = Color(0xFF1D1B20),
                    actionIconContentColor = Color(0xFF49454F)
                )
            )
        },
        bottomBar = {
            if (currentTab != 4) {
                NavigationBar(
                    containerColor = Color(0xFFF3EDF7),
                    tonalElevation = 8.dp
                ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Streams") },
                    label = { Text("Streams") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF21005D),
                        selectedTextColor = Color(0xFF21005D),
                        indicatorColor = Color(0xFFEADDFF),
                        unselectedIconColor = Color(0xFF49454F),
                        unselectedTextColor = Color(0xFF49454F)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Live IPTV") },
                    label = { Text("IPTV") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF21005D),
                        selectedTextColor = Color(0xFF21005D),
                        indicatorColor = Color(0xFFEADDFF),
                        unselectedIconColor = Color(0xFF49454F),
                        unselectedTextColor = Color(0xFF49454F)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Premium Portal") },
                    label = { Text("Premium") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF21005D),
                        selectedTextColor = Color(0xFF21005D),
                        indicatorColor = Color(0xFFEADDFF),
                        unselectedIconColor = Color(0xFF49454F),
                        unselectedTextColor = Color(0xFF49454F)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.Email, contentDescription = "Support & Donations") },
                    label = { Text("Support") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF21005D),
                        selectedTextColor = Color(0xFF21005D),
                        indicatorColor = Color(0xFFEADDFF),
                        unselectedIconColor = Color(0xFF49454F),
                        unselectedTextColor = Color(0xFF49454F)
                    )
                )
                if (settings.browserEnabled) {
                    NavigationBarItem(
                        selected = currentTab == 4,
                        onClick = { currentTab = 4 },
                        icon = { Icon(Icons.Default.Search, contentDescription = "Fast Browser") },
                        label = { Text("Browser") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF21005D),
                            selectedTextColor = Color(0xFF21005D),
                            indicatorColor = Color(0xFFEADDFF),
                            unselectedIconColor = Color(0xFF49454F),
                            unselectedTextColor = Color(0xFF49454F)
                        )
                    )
                }
            }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFFEF7FF))
        ) {
            when (currentTab) {
                0 -> {
                    // STREAMS TAB (With customizable header texts & active filter)
                    val activeButtons = remember(buttons) { buttons.filter { it.isEnabled } }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header section: Watch & Visit description (customizable)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = settings.watchVisitText.ifBlank { "Watch & Visit" },
                                color = Color(0xFF1D1B20),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = settings.watchVisitDesc.ifBlank { "Click on any server video below to watch a short ad and visit our premium stream links." },
                                color = Color(0xFF49454F),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }

                        // Announcement banner if broadcasted
                        if (settings.lastNotificationTitle.isNotBlank()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                                border = borderStroke(1.dp, Color(0xFFCAC4D0))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Notification",
                                        tint = Color(0xFF6750A4),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = settings.lastNotificationTitle,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF1D1B20),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (settings.lastNotificationBody.isNotBlank()) {
                                            Text(
                                                text = settings.lastNotificationBody,
                                                fontSize = 10.sp,
                                                color = Color(0xFF49454F),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Main Servers grid of buttons
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(activeButtons) { button ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(85.dp)
                                        .clickable { onPortalSelected(button) }
                                        .testTag("watch_card_${button.id}"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                                    border = borderStroke(1.dp, Color(0xFFCAC4D0))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF6750A4)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!button.customImageBase64.isNullOrBlank()) {
                                                val bitmap = remember(button.customImageBase64) {
                                                    try {
                                                        val decodedBytes = Base64.decode(button.customImageBase64, Base64.DEFAULT)
                                                        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                                    } catch (e: Exception) {
                                                        null
                                                    }
                                                }
                                                if (bitmap != null) {
                                                    Image(
                                                        bitmap = bitmap.asImageBitmap(),
                                                        contentDescription = "Watch button image",
                                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = "Watch stream icon",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            } else if (!button.emoji.isNullOrBlank()) {
                                                Text(
                                                    text = button.emoji,
                                                    fontSize = 18.sp,
                                                    textAlign = TextAlign.Center
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Watch stream icon",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = button.name,
                                                color = Color(0xFF1D1B20),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "লাইভ সার্ভার",
                                                color = Color(0xFF6750A4),
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Sponsored / Ad Network display bar
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                            border = borderStroke(1.dp, Color(0xFFCAC4D0), isDashed = true)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = settings.sponsorText.ifBlank { "SPONSORED BY ${settings.adNetwork.uppercase()}" },
                                    color = Color(0xFF49454F),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(48.dp)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0xFF49454F).copy(alpha = 0.3f))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(80.dp)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0xFF49454F).copy(alpha = 0.3f))
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // LIVE IPTV STREAM PLAYER TAB
                    IptvScreen(
                        settings = settings,
                        onChannelSelected = { channel ->
                            // Convert channel to button representation
                            val btn = WatchButton(
                                id = 8800 + channel.name.hashCode().coerceAtLeast(0),
                                name = channel.name,
                                url = channel.streamUrl,
                                adsEnabled = settings.adsEnabled
                            )
                            onPortalSelected(btn)
                        }
                    )
                }
                2 -> {
                    // PREMIUM PORTAL TAB (Beautiful categorized links with ad-monetization trigger)
                    val categories = listOf("Premium APK", "Courses", "Books", "Movies")
                    var expandedCategory by remember { mutableStateOf<String?>("Premium APK") }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Premium Feature Library",
                                color = Color(0xFF1D1B20),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Explore and download free premium utilities and services verified by Hasan.",
                                color = Color(0xFF49454F),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(categories) { category ->
                                val itemsForCategory = premiumItems.filter { it.category == category }
                                val isExpanded = expandedCategory == category

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateContentSize(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isExpanded) Color(0xFFEADDFF) else Color(0xFFF3EDF7)
                                    ),
                                    border = borderStroke(1.dp, if (isExpanded) Color(0xFF6750A4) else Color(0xFFCAC4D0))
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    expandedCategory = if (isExpanded) null else category
                                                }
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isExpanded) Color.White else Color(0xFFEADDFF)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = when (category) {
                                                            "Premium APK" -> Icons.Default.Build
                                                            "Courses" -> Icons.Default.List
                                                            "Books" -> Icons.Default.Info
                                                            else -> Icons.Default.PlayArrow
                                                        },
                                                        contentDescription = null,
                                                        tint = Color(0xFF6750A4),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Text(
                                                    text = category,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1D1B20)
                                                )
                                            }
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Expand/Collapse",
                                                tint = Color(0xFF49454F)
                                            )
                                        }

                                        if (isExpanded) {
                                            HorizontalDivider(color = Color(0xFFCAC4D0).copy(alpha = 0.5f))
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (itemsForCategory.isEmpty()) {
                                                    Text(
                                                        text = "কোন আইটেম আপলোড করা হয়নি।",
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF49454F),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 12.dp),
                                                        textAlign = TextAlign.Center
                                                    )
                                                } else {
                                                    itemsForCategory.forEachIndexed { idx, item ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .background(Color.White, RoundedCornerShape(12.dp))
                                                                .padding(12.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                                Text(
                                                                    text = item.name,
                                                                    fontSize = 13.sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    color = Color(0xFF1D1B20),
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                                Text(
                                                                    text = item.url,
                                                                    fontSize = 10.sp,
                                                                    color = Color(0xFF49454F),
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                            Button(
                                                                onClick = {
                                                                    // Wraps dynamic item into watch button to trigger ad sequence properly!
                                                                    onPortalSelected(
                                                                        WatchButton(
                                                                            id = 1000 + idx,
                                                                            name = item.name,
                                                                            url = item.url,
                                                                            adsEnabled = settings.adsEnabled
                                                                        )
                                                                    )
                                                                },
                                                                shape = RoundedCornerShape(8.dp),
                                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                                modifier = Modifier.height(32.dp)
                                                            ) {
                                                                Text("ডাউনলোড", fontSize = 11.sp, color = Color.White)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // CONTACT, USER FEEDBACK, AND EXPANDED DONATIONS TAB
                    var subSectionTab by remember { mutableStateOf(0) } // 0: Donations, 1: Feedback, 2: Contact

                    // Donation form states
                    var senderName by remember { mutableStateOf("") }
                    var senderPhone by remember { mutableStateOf("") }
                    var txId by remember { mutableStateOf("") }
                    var selectedPackage by remember { mutableStateOf("৩০ দিন") }
                    var selectedMethod by remember { mutableStateOf("Bkash") }
                    var isDonating by remember { mutableStateOf(false) }

                    // Feedback form states
                    var feedbackName by remember { mutableStateOf("") }
                    var feedbackContact by remember { mutableStateOf("") }
                    var selectedType by remember { mutableStateOf("APK") }
                    var feedbackMsg by remember { mutableStateOf("") }
                    var isSubmittingFeedback by remember { mutableStateOf(false) }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Founder Avatar Header card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF)),
                                border = borderStroke(1.dp, Color(0xFFD0BCFF))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF6750A4)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = "Md Hasan Khalifa Profile",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = settings.contactName.ifBlank { "Md Hasan Khalifa" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = Color(0xFF21005D)
                                            )
                                            Text(
                                                text = "Founder & Streams Host",
                                                fontSize = 11.sp,
                                                color = Color(0xFF21005D).copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Sub tabs inside Support View
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF3EDF7))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val supportTabs = listOf("ডোনেশন", "পরামর্শ ও অনুরোধ", "যোগাযোগ")
                                supportTabs.forEachIndexed { idx, label ->
                                    val act = subSectionTab == idx
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (act) Color(0xFF6750A4) else Color.Transparent)
                                            .clickable { subSectionTab = idx }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (act) Color.White else Color(0xFF49454F),
                                            fontSize = 11.sp,
                                            fontWeight = if (act) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        when (subSectionTab) {
                            0 -> {
                                // 1. DONATIONS SECTION (Copy Number, Structures, Form, TxID Submission)
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                                        border = borderStroke(1.dp, Color(0xFFCAC4D0))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text = "ডোনেশন ও সাবস্ক্রিপশন নির্দেশিকা",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Color(0xFF1D1B20)
                                            )
                                            
                                            Text(
                                                text = settings.donationInstructions.ifBlank { "বিকাশ বা নগদ পার্সোনাল নাম্বারে Send Money করুন। তারপর নিচের ফর্মে সঠিক ট্রানজেকশন আইডি দিয়ে সাবমিট করুন।" },
                                                fontSize = 12.sp,
                                                color = Color(0xFF49454F),
                                                lineHeight = 16.sp
                                            )

                                            HorizontalDivider(color = Color(0xFFCAC4D0).copy(alpha = 0.5f))

                                            // Recipient Info Copy Box
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.White, RoundedCornerShape(12.dp))
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text("টাকা পাঠানোর নাম্বার (Personal)", fontSize = 10.sp, color = Color(0xFF49454F))
                                                    Text(
                                                        text = settings.donationNumber.ifBlank { "+8801798088609" },
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 16.sp,
                                                        color = Color(0xFF21005D)
                                                    )
                                                }
                                                Button(
                                                    onClick = {
                                                        clipboardManager.setText(AnnotatedString(settings.donationNumber.ifBlank { "+8801798088609" }))
                                                        Toast.makeText(context, "নাম্বার কপি হয়েছে!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Share, contentDescription = "Copy Number", tint = Color.White, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Copy", fontSize = 11.sp, color = Color.White)
                                                }
                                            }

                                            // Subscription Packages Box
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Text("উপলব্ধ সাবস্ক্রিপশন প্যাকেজ:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF21005D))
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = settings.donationPackages.ifBlank { "১ দিন - ১০ টাকা\n৩০ দিন - ২৫০ টাকা\n১ বছর - ১৫০০ টাকা" },
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF21005D).copy(alpha = 0.8f),
                                                        lineHeight = 15.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Form Fields Item
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                                        border = borderStroke(1.dp, Color(0xFFCAC4D0))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text = "পেমেন্ট তথ্য সাবমিট করুন",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF1D1B20)
                                            )

                                            OutlinedTextField(
                                                value = senderName,
                                                onValueChange = { senderName = it },
                                                label = { Text("আপনার নাম") },
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color(0xFF6750A4),
                                                    unfocusedBorderColor = Color(0xFFCAC4D0)
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            OutlinedTextField(
                                                value = senderPhone,
                                                onValueChange = { senderPhone = it },
                                                label = { Text("যে নাম্বার থেকে টাকা পাঠিয়েছেন") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color(0xFF6750A4),
                                                    unfocusedBorderColor = Color(0xFFCAC4D0)
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            OutlinedTextField(
                                                value = txId,
                                                onValueChange = { txId = it },
                                                label = { Text("Transaction ID (TxID)") },
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color(0xFF6750A4),
                                                    unfocusedBorderColor = Color(0xFFCAC4D0)
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            // Method choice
                                            Text("পেমেন্ট মেথড:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                val methods = listOf("Bkash", "Nagad", "Rocket")
                                                methods.forEach { m ->
                                                    val isSelected = selectedMethod == m
                                                    Card(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable { selectedMethod = m },
                                                        shape = RoundedCornerShape(10.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = if (isSelected) Color(0xFF6750A4) else Color.White
                                                        ),
                                                        border = borderStroke(1.dp, if (isSelected) Color(0xFF6750A4) else Color(0xFFCAC4D0))
                                                    ) {
                                                        Text(
                                                            text = m,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSelected) Color.White else Color(0xFF1D1B20),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 10.dp),
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                }
                                            }

                                            // Package Choice
                                            Text("প্যাকেজ মেয়াদ:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                val pkgs = listOf("১ দিন", "৩০ দিন", "১ বছর")
                                                pkgs.forEach { p ->
                                                    val isSelected = selectedPackage == p
                                                    Card(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable { selectedPackage = p },
                                                        shape = RoundedCornerShape(10.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = if (isSelected) Color(0xFF6750A4) else Color.White
                                                        ),
                                                        border = borderStroke(1.dp, if (isSelected) Color(0xFF6750A4) else Color(0xFFCAC4D0))
                                                    ) {
                                                        Text(
                                                            text = p,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSelected) Color.White else Color(0xFF1D1B20),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 10.dp),
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                }
                                            }

                                            Button(
                                                onClick = {
                                                    if (senderName.isBlank() || senderPhone.isBlank() || txId.isBlank()) {
                                                        Toast.makeText(context, "অনুগ্রহ করে সব তথ্য সঠিক দিন।", Toast.LENGTH_SHORT).show()
                                                        return@Button
                                                    }
                                                    isDonating = true
                                                    coroutineScope.launch {
                                                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                                        val stamp = sdf.format(Date())
                                                        val list = AppConfigManager.loadDonations(context).toMutableList()
                                                        list.add(
                                                            DonationItem(
                                                                id = UUID.randomUUID().toString(),
                                                                senderName = senderName,
                                                                senderPhone = senderPhone,
                                                                transactionId = txId,
                                                                amountOrPackage = selectedPackage,
                                                                paymentMethod = selectedMethod,
                                                                timestamp = stamp,
                                                                status = "Pending",
                                                                expirationDate = selectedPackage
                                                            )
                                                        )
                                                        AppConfigManager.saveDonations(context, list)
                                                        donations = list
                                                        
                                                        // Fallback also save to local feedback for admin's easier sync
                                                        submitFeedbackToServer(
                                                            context = context,
                                                            name = senderName,
                                                            contact = senderPhone,
                                                            requestType = "Donation ($selectedPackage)",
                                                            message = "TxID: $txId via $selectedMethod for $selectedPackage",
                                                            url = settings.feedbackUrl
                                                        )

                                                        isDonating = false
                                                        Toast.makeText(context, "পেমেন্ট সফলভাবে সাবমিট হয়েছে! এডমিন অনুমোদনের পর প্রিমিয়াম লক খুলে যাবে।", Toast.LENGTH_LONG).show()
                                                        senderName = ""
                                                        senderPhone = ""
                                                        txId = ""
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                if (isDonating) {
                                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                                } else {
                                                    Text("ডোনেশন সাবমিট দিন", fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Active Subscription / Donation History List
                                if (donations.isNotEmpty()) {
                                    item {
                                        Text("আপনার সাবমিশন ও সাবস্ক্রিপশন স্ট্যাটাস:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1D1B20))
                                    }
                                    items(donations.asReversed()) { don ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            border = borderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(text = "TxID: ${don.transactionId}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1D1B20))
                                                    Text(text = "প্যাকেজ: ${don.amountOrPackage} (${don.paymentMethod})", fontSize = 11.sp, color = Color(0xFF49454F))
                                                    Text(text = "সময়: ${don.timestamp}", fontSize = 9.sp, color = Color.Gray)
                                                }
                                                
                                                val statusColor = when (don.status) {
                                                    "Approved" -> Color(0xFF4CAF50)
                                                    "Declined" -> Color(0xFFF44336)
                                                    else -> Color(0xFFFF9800)
                                                }
                                                
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(statusColor.copy(alpha = 0.15f))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = don.status,
                                                        color = statusColor,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            1 -> {
                                // 2. FEEDBACK SUBMISSION TAB
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(24.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                                        border = borderStroke(1.dp, Color(0xFFCAC4D0))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Send,
                                                    contentDescription = null,
                                                    tint = Color(0xFF6750A4),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = "অনুরোধ ও পরামর্শ কেন্দ্র",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = Color(0xFF1D1B20)
                                                )
                                            }

                                            HorizontalDivider(color = Color(0xFFCAC4D0).copy(alpha = 0.5f))

                                            OutlinedTextField(
                                                value = feedbackName,
                                                onValueChange = { feedbackName = it },
                                                label = { Text("আপনার নাম") },
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color(0xFF6750A4),
                                                    unfocusedBorderColor = Color(0xFFCAC4D0)
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            OutlinedTextField(
                                                value = feedbackContact,
                                                onValueChange = { feedbackContact = it },
                                                label = { Text("যোগাযোগ নম্বর / ইমেইল (ঐচ্ছিক)") },
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color(0xFF6750A4),
                                                    unfocusedBorderColor = Color(0xFFCAC4D0)
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            // Category Select Pills
                                            Text("অনুরোধের ধরণ:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF49454F))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                val types = listOf("APK", "Course", "Book", "Movie", "Other")
                                                types.forEach { type ->
                                                    val isSel = selectedType == type
                                                    Card(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable { selectedType = type },
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = if (isSel) Color(0xFF6750A4) else Color.White
                                                        ),
                                                        border = borderStroke(1.dp, if (isSel) Color(0xFF6750A4) else Color(0xFFCAC4D0))
                                                    ) {
                                                        Text(
                                                            text = type,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSel) Color.White else Color(0xFF1D1B20),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 8.dp),
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                }
                                            }

                                            OutlinedTextField(
                                                value = feedbackMsg,
                                                onValueChange = { feedbackMsg = it },
                                                label = { Text("আপনার বার্তা / অনুরোধ বিস্তারিত লিখুন") },
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = Color(0xFF6750A4),
                                                    unfocusedBorderColor = Color(0xFFCAC4D0)
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(100.dp)
                                            )

                                            Button(
                                                onClick = {
                                                    if (feedbackName.isBlank() || feedbackMsg.isBlank()) {
                                                        Toast.makeText(context, "অনুগ্রহ করে নাম এবং বার্তা লিখুন।", Toast.LENGTH_SHORT).show()
                                                        return@Button
                                                    }
                                                    isSubmittingFeedback = true
                                                    coroutineScope.launch {
                                                        val success = submitFeedbackToServer(
                                                            context = context,
                                                            name = feedbackName,
                                                            contact = feedbackContact,
                                                            requestType = selectedType,
                                                            message = feedbackMsg,
                                                            url = settings.feedbackUrl
                                                        )
                                                        isSubmittingFeedback = false
                                                        if (success) {
                                                            Toast.makeText(context, "ধন্যবাদ! আপনার বার্তা সরাসরি সেভ হয়েছে।", Toast.LENGTH_LONG).show()
                                                        } else {
                                                            Toast.makeText(context, "অনলাইন সাবমিট সফল হয়নি, ব্যাকআপ হিসেবে অফলাইনে সেভ হয়েছে।", Toast.LENGTH_LONG).show()
                                                        }
                                                        feedbackName = ""
                                                        feedbackContact = ""
                                                        feedbackMsg = ""
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                if (isSubmittingFeedback) {
                                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                                } else {
                                                    Text("জমা দিন", fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            2 -> {
                                // 3. DIRECT CONTACT TAB
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(24.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF)),
                                        border = borderStroke(1.dp, Color(0xFFD0BCFF))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text = "যেকোনো ধরণের প্রিমিয়াম অ্যাপ, সিনেমা, ই-বুক বা কোর্স রিকোয়েস্ট করুন অথবা যেকোনো সমস্যায় সরাসরি যোগাযোগ করুন।",
                                                fontSize = 12.sp,
                                                color = Color(0xFF21005D).copy(alpha = 0.8f),
                                                lineHeight = 16.sp
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        val phoneNo = settings.contactWhatsApp.ifBlank { "+8801798088609" }
                                                        val cleanPhone = phoneNo.replace(" ", "").replace("-", "")
                                                        try {
                                                            uriHandler.openUri("https://api.whatsapp.com/send?phone=$cleanPhone")
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "WhatsApp open failed: $phoneNo", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(Icons.Default.Call, contentDescription = "WhatsApp", tint = Color.White, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("WhatsApp", fontSize = 11.sp, color = Color.White)
                                                }

                                                Button(
                                                    onClick = {
                                                        val facebookUrl = settings.contactFacebook.ifBlank { "https://www.facebook.com/HasanKhalifa01" }
                                                        try {
                                                            uriHandler.openUri(facebookUrl)
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "Facebook open failed.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5)),
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(Icons.Default.Person, contentDescription = "Facebook", tint = Color.White, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Facebook", fontSize = 11.sp, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                4 -> {
                    FastBrowserScreen(
                        appSettings = settings,
                        onExitBrowser = { currentTab = 0 },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // 5-Sec Custom ad overlay for 3-dot profile button
    if (showAdOverlay3Dot) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Surface(
                    color = Color(0xFFFF5D55),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "SPONSORED BY ${settings.adNetwork.uppercase()}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { ad3DotSecondsRemaining / 5f },
                        color = Color(0xFFFF5D55),
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(60.dp)
                    )
                    Text(
                        text = "$ad3DotSecondsRemaining s",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "প্রতিষ্ঠাতা প্রোফাইল ওপেন হচ্ছে...",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "দয়া করে ৫ সেকেন্ড অপেক্ষা করুন।",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // Elegant Dialog showing founder information and notifications after 3-dot ad
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(40.dp)) },
            title = {
                Text(
                    text = "Founder Profile & Updates",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "নাম: Md Hasan Khalifa",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "মোবাইল: ${settings.donationNumber.ifBlank { "+8801798088609" }}",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    Text(
                        text = "বিজ্ঞপ্তি ও নোটিফিকেশন:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = if (settings.lastNotificationBody.isNotBlank()) settings.lastNotificationBody else "সব লাইভ খেলা ও গুরুত্বপূর্ণ নোটিফিকেশন এখানে প্রদর্শিত হবে।",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            lineHeight = 16.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("ঠিক আছে", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        )
    }

    // 3-Step Hidden Security Entry Dialog Flow
    if (showSecurityDialog) {
        var step by remember { mutableStateOf(1) } // 1, 2, 3
        var pinInput by remember { mutableStateOf("") }
        var isResetMode by remember { mutableStateOf(false) }
        var resetAnswer by remember { mutableStateOf("") }
        
        // New PIN setup states
        var isSettingNewPins by remember { mutableStateOf(false) }
        var newPin1 by remember { mutableStateOf("") }
        var newPin2 by remember { mutableStateOf("") }
        var newPin3 by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                showSecurityDialog = false
                step = 1
                pinInput = ""
                isResetMode = false
                resetAnswer = ""
                isSettingNewPins = false
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = {
                Text(
                    text = when {
                        isSettingNewPins -> "নতুন ৩টি সিকিউরিটি পিন সেট করুন"
                        isResetMode -> "সিকিউরিটি পিন রিসেট করুন"
                        step == 1 -> "ধাপ ১/৩: ৪-ডিজিট পিন"
                        step == 2 -> "ধাপ ২/৩: ৩-ডিজিট পিন"
                        else -> "ধাপ ৩/৩: ৪-ডিজিট পিন"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSettingNewPins) {
                        Text("অনুগ্রহ করে আপনার নতুন সিকিউরিটি পিনগুলো টাইপ করুন:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        OutlinedTextField(
                            value = newPin1,
                            onValueChange = { if (it.length <= 4) newPin1 = it },
                            label = { Text("নতুন পিন ১ (৪ ডিজিট)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6750A4),
                                unfocusedBorderColor = Color(0xFFCAC4D0),
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        OutlinedTextField(
                            value = newPin2,
                            onValueChange = { if (it.length <= 3) newPin2 = it },
                            label = { Text("নতুন পিন ২ (৩ ডিজিট)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6750A4),
                                unfocusedBorderColor = Color(0xFFCAC4D0),
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        OutlinedTextField(
                            value = newPin3,
                            onValueChange = { if (it.length <= 4) newPin3 = it },
                            label = { Text("নতুন পিন ৩ (৪ ডিজিট)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6750A4),
                                unfocusedBorderColor = Color(0xFFCAC4D0),
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    } else if (isResetMode) {
                        Text("সিকিউরিটি প্রশ্নের সঠিক উত্তর দিয়ে পিন পরিবর্তন করুন:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = settings.securityQuestion.ifBlank { "রিসেট কোড বা প্রশ্ন?" },
                                modifier = Modifier.padding(12.dp),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        OutlinedTextField(
                            value = resetAnswer,
                            onValueChange = { resetAnswer = it },
                            label = { Text("আপনার উত্তর") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6750A4),
                                unfocusedBorderColor = Color(0xFFCAC4D0),
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    } else {
                        Text(
                            text = when (step) {
                                1 -> "প্রথম সিকিউরিটি পিনটি প্রবেশ করান:"
                                2 -> "দ্বিতীয় সিকিউরিটি পিনটি প্রবেশ করান:"
                                else -> "তৃতীয় সিকিউরিটি পিনটি প্রবেশ করান:"
                            },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = {
                                val maxLen = if (step == 2) 3 else 4
                                if (it.length <= maxLen) pinInput = it
                            },
                            label = { Text("পিন নম্বর") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6750A4),
                                unfocusedBorderColor = Color(0xFFCAC4D0),
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isSettingNewPins) {
                            if (newPin1.length == 4 && newPin2.length == 3 && newPin3.length == 4) {
                                val updated = settings.copy(
                                    pin1 = newPin1,
                                    pin2 = newPin2,
                                    pin3 = newPin3
                                )
                                AppConfigManager.saveSettings(context, updated)
                                settings = updated
                                Toast.makeText(context, "পিন সফলভাবে রিসেট ও সেভ হয়েছে!", Toast.LENGTH_LONG).show()
                                isSettingNewPins = false
                                isResetMode = false
                                step = 1
                                pinInput = ""
                            } else {
                                Toast.makeText(context, "অনুগ্রহ করে ১ নং ৪-ডিজিট, ২ নং ৩-ডিজিট, ৩ নং ৪-ডিজিট সঠিক দৈর্ঘ্য দিন।", Toast.LENGTH_LONG).show()
                            }
                        } else if (isResetMode) {
                            val correctAns = settings.securityAnswer.ifBlank { "hasan" }
                            if (resetAnswer.trim().equals(correctAns, ignoreCase = true)) {
                                isSettingNewPins = true
                            } else {
                                Toast.makeText(context, "ভুল উত্তর দিয়েছেন!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            if (pinInput == "2026" || pinInput == "12345") {
                                // Master override bypass
                                showSecurityDialog = false
                                step = 1
                                pinInput = ""
                                onAdminClick()
                                Toast.makeText(context, "মাস্টার পিন দিয়ে সফলভাবে প্রবেশ করেছেন!", Toast.LENGTH_SHORT).show()
                            } else {
                                // Regular steps verification
                                val expectedPin = when (step) {
                                    1 -> settings.pin1.ifBlank { "1234" }
                                    2 -> settings.pin2.ifBlank { "123" }
                                    else -> settings.pin3.ifBlank { "5678" }
                                }
                                
                                if (pinInput == expectedPin) {
                                    if (step < 3) {
                                        step++
                                        pinInput = ""
                                    } else {
                                        // Successful login
                                        showSecurityDialog = false
                                        step = 1
                                        pinInput = ""
                                        onAdminClick()
                                    }
                                } else {
                                    Toast.makeText(context, "ভুল পিন দিয়েছেন!", Toast.LENGTH_SHORT).show()
                                    step = 1
                                    pinInput = ""
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                ) {
                    Text(
                        text = when {
                            isSettingNewPins -> "সেভ করুন"
                            isResetMode -> "যাচাই করুন"
                            step < 3 -> "পরবর্তী"
                            else -> "প্রবেশ করুন"
                        }
                    )
                }
            },
            dismissButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isResetMode && !isSettingNewPins) {
                        TextButton(
                            onClick = {
                                isResetMode = true
                                resetAnswer = ""
                            }
                        ) {
                            Text("রিসেট পিন", color = Color(0xFFB3261E))
                        }
                    }
                    TextButton(
                        onClick = {
                            showSecurityDialog = false
                            step = 1
                            pinInput = ""
                            isResetMode = false
                            resetAnswer = ""
                            isSettingNewPins = false
                        }
                    ) {
                        Text("বাতিল", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        )
    }
}

// Convenient border strokes helper supporting custom styling
private fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color, isDashed: Boolean = false): androidx.compose.foundation.BorderStroke {
    return androidx.compose.foundation.BorderStroke(width, color)
}

suspend fun submitFeedbackToServer(
    context: Context,
    name: String,
    contact: String,
    requestType: String,
    message: String,
    url: String
): Boolean {
    // If feedbackUrl is blank, we can still succeed locally as backup
    if (url.isBlank()) {
        saveLocalFeedback(context, name, contact, requestType, message)
        return true
    }
    return withContext(Dispatchers.IO) {
        try {
            val formBody = okhttp3.FormBody.Builder()
                .add("name", name)
                .add("contact", contact)
                .add("type", requestType)
                .add("message", message)
                .build()
            val request = okhttp3.Request.Builder()
                .url(url)
                .post(formBody)
                .build()
            okhttp3.OkHttpClient().newCall(request).execute().use { response ->
                val success = response.isSuccessful
                saveLocalFeedback(context, name, contact, requestType, message)
                success
            }
        } catch (e: Exception) {
            Log.e("Feedback", "Failed to submit to Google Sheet directly, saving locally as fallback", e)
            saveLocalFeedback(context, name, contact, requestType, message)
            false
        }
    }
}

private fun saveLocalFeedback(context: Context, name: String, contact: String, requestType: String, message: String) {
    val current = AppConfigManager.loadFeedbacks(context).toMutableList()
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val timestamp = sdf.format(Date())
    current.add(
        FeedbackItem(
            id = UUID.randomUUID().toString(),
            senderName = name,
            contact = contact,
            requestType = requestType,
            message = message,
            timestamp = timestamp
        )
    )
    AppConfigManager.saveFeedbacks(context, current)
}
