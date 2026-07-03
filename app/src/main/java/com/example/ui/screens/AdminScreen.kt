package com.example.ui.screens

import coil.compose.AsyncImage

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.shape.CircleShape
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.utils.NotificationHelper
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import android.graphics.BitmapFactory
import android.util.Base64

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit,
    onConfigUpdated: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Intercept system back press to go back to Home Screen
    BackHandler {
        onBack()
    }

    // Load active configurations
    var settings by remember { mutableStateOf(AppConfigManager.loadSettings(context)) }
    var buttons by remember { mutableStateOf(AppConfigManager.loadButtons(context)) }
    var premiumItemsList by remember { mutableStateOf(AppConfigManager.loadPremiumItems(context)) }
    var feedbacksList by remember { mutableStateOf(AppConfigManager.loadFeedbacks(context)) }
    var donationsList by remember { mutableStateOf(AppConfigManager.loadDonations(context)) }

    // Admin Navigation Tabs:
    // 0: General Settings, 1: Watch Buttons, 2: IPTV Config, 3: Donations, 4: Premium & Feedbacks
    var activeTab by remember { mutableStateOf(0) }

    // Sync state
    var isSyncing by remember { mutableStateOf(false) }
    var inputSheetId by remember { mutableStateOf(settings.sheetId) }
    var inputGithubConfigUrl by remember { mutableStateOf(settings.githubConfigUrl) }
    var isGithubRefreshing by remember { mutableStateOf(false) }

    // Broadcast messages inputs
    var notifyTitle by remember { mutableStateOf("") }
    var notifyBody by remember { mutableStateOf("") }

    // Dynamic Watch Button form inputs
    var btnNameInput by remember { mutableStateOf("") }
    var btnUrlInput by remember { mutableStateOf("") }
    var btnAdsEnabledInput by remember { mutableStateOf(true) }
    var btnImageBase64Input by remember { mutableStateOf<String?>(null) }
    var btnEmojiInput by remember { mutableStateOf("") }
    var editingButton by remember { mutableStateOf<WatchButton?>(null) }
    var editingPremiumItem by remember { mutableStateOf<PremiumItem?>(null) }

    val btnImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    if (bitmap.width > 512 || bitmap.height > 512) {
                        Toast.makeText(context, "বাটনের ছবির সাইজ সর্বোচ্চ ৫১২x৫১২ পিক্সেল হতে হবে!", Toast.LENGTH_LONG).show()
                    } else {
                        val outputStream = java.io.ByteArrayOutputStream()
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
                        val bytes = outputStream.toByteArray()
                        btnImageBase64Input = Base64.encodeToString(bytes, Base64.DEFAULT)
                        btnEmojiInput = ""
                    }
                } else {
                    Toast.makeText(context, "ছবি লোড করতে সমস্যা হয়েছে!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "ছবি প্রসেস করতে ব্যর্থ!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Premium link inputs
    var newPremName by remember { mutableStateOf("") }
    var newPremUrl by remember { mutableStateOf("") }
    var selectedPremCat by remember { mutableStateOf("Premium APK") }

    // Security & Socials inputs
    var pin1Input by remember { mutableStateOf(settings.pin1) }
    var pin2Input by remember { mutableStateOf(settings.pin2) }
    var pin3Input by remember { mutableStateOf(settings.pin3) }
    var seqQuestionInput by remember { mutableStateOf(settings.securityQuestion) }
    var seqAnswerInput by remember { mutableStateOf(settings.securityAnswer) }
    var contactNameInput by remember { mutableStateOf(settings.contactName) }
    var contactWhatsAppInput by remember { mutableStateOf(settings.contactWhatsApp) }
    var contactFacebookInput by remember { mutableStateOf(settings.contactFacebook) }
    var feedbackUrlInput by remember { mutableStateOf(settings.feedbackUrl) }

    // Watch/Visit texts inputs
    var watchVisitTitleInput by remember { mutableStateOf(settings.watchVisitText) }
    var watchVisitDescInput by remember { mutableStateOf(settings.watchVisitDesc) }

    // IPTV inputs
    var iptvEnabledInput by remember { mutableStateOf(settings.iptvEnabled) }
    var iptvPlaylistUrlInput by remember { mutableStateOf(settings.iptvPlaylistUrl) }
    var iptvSecondaryUrlInput by remember { mutableStateOf(settings.iptvSecondaryUrl) }
    var iptvFileContentInput by remember { mutableStateOf(settings.iptvFileContent) }

    // Donation texts inputs
    var donationNumberInput by remember { mutableStateOf(settings.donationNumber) }
    var donationInstructionsInput by remember { mutableStateOf(settings.donationInstructions) }
    var donationPackagesInput by remember { mutableStateOf(settings.donationPackages) }

    // Three-dot settings
    var threeDotAdDelayInput by remember { mutableStateOf(settings.showThreeDotAdDelaySeconds.toString()) }

    // Fast Browser & Sponsor text customization inputs
    var sponsorTextInput by remember { mutableStateOf(settings.sponsorText) }
    var browserEnabledInput by remember { mutableStateOf(settings.browserEnabled) }
    var browserHomeUrlInput by remember { mutableStateOf(settings.browserHomeUrl) }

    // Personal Ads & Networks inputs
    var personalAdEnabledInput by remember { mutableStateOf(settings.personalAdEnabled) }
    var personalAdImageUrlInput by remember { mutableStateOf(settings.personalAdImageUrl) }
    var personalAdClickUrlInput by remember { mutableStateOf(settings.personalAdClickUrl) }
    var personalAdVideoUrlInput by remember { mutableStateOf(settings.personalAdVideoUrl) }
    var monetagAdCodeInput by remember { mutableStateOf(settings.monetagAdCode) }
    var startappAppIdInput by remember { mutableStateOf(settings.startappAppId) }

    // Custom Shortcuts (Bookmarks) list state
    var browserShortcutsList by remember(settings.browserShortcutsJson) {
        mutableStateOf(AppConfigManager.parseShortcuts(settings.browserShortcutsJson))
    }

    // Default Search Engine input
    var searchEngineUrlInput by remember { mutableStateOf(settings.searchEngineUrl) }

    // Custom features/menu items list state
    var customMenuItemsList by remember(settings.customFeaturesJson) {
        mutableStateOf(AppConfigManager.parseCustomMenuItems(settings.customFeaturesJson))
    }

    var adsEnabledInput by remember { mutableStateOf(settings.adsEnabled) }
    var backAdEnabledInput by remember { mutableStateOf(settings.backAdEnabled) }
    var iptvServer1EnabledInput by remember { mutableStateOf(settings.iptvServer1Enabled) }
    var iptvServer2EnabledInput by remember { mutableStateOf(settings.iptvServer2Enabled) }
    var iptvPendingFileContentInput by remember { mutableStateOf(settings.iptvPendingFileContent) }

    fun refreshAll() {
        settings = AppConfigManager.loadSettings(context)
        buttons = AppConfigManager.loadButtons(context)
        premiumItemsList = AppConfigManager.loadPremiumItems(context)
        feedbacksList = AppConfigManager.loadFeedbacks(context)
        donationsList = AppConfigManager.loadDonations(context)
        sponsorTextInput = settings.sponsorText
        browserEnabledInput = settings.browserEnabled
        browserHomeUrlInput = settings.browserHomeUrl
        personalAdEnabledInput = settings.personalAdEnabled
        personalAdImageUrlInput = settings.personalAdImageUrl
        personalAdClickUrlInput = settings.personalAdClickUrl
        personalAdVideoUrlInput = settings.personalAdVideoUrl
        monetagAdCodeInput = settings.monetagAdCode
        startappAppIdInput = settings.startappAppId
        browserShortcutsList = AppConfigManager.parseShortcuts(settings.browserShortcutsJson)
        searchEngineUrlInput = settings.searchEngineUrl
        customMenuItemsList = AppConfigManager.parseCustomMenuItems(settings.customFeaturesJson)
        adsEnabledInput = settings.adsEnabled
        backAdEnabledInput = settings.backAdEnabled
        iptvServer1EnabledInput = settings.iptvServer1Enabled
        iptvServer2EnabledInput = settings.iptvServer2Enabled
        iptvPendingFileContentInput = settings.iptvPendingFileContent
        inputSheetId = settings.sheetId
        inputGithubConfigUrl = settings.githubConfigUrl
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Admin Panel",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("admin_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Scrollable Tab bar to prevent truncation in compact screens
            ScrollableTabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 8.dp
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("General", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Watch Buttons", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("IPTV Setup", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Donations", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            val pings = donationsList.filter { it.status == "Pending" }.size
                            if (pings > 0) {
                                Badge(containerColor = MaterialTheme.colorScheme.error) {
                                    Text(pings.toString(), color = MaterialTheme.colorScheme.onError, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                )
                Tab(
                    selected = activeTab == 4,
                    onClick = { activeTab = 4 },
                    text = { Text("Premium & Security", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 5,
                    onClick = { activeTab = 5 },
                    text = { Text("Browser & Bookmarks", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 6,
                    onClick = { activeTab = 6 },
                    text = { Text("Ads Control 📢", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (activeTab) {
                    0 -> {
                        // GENERAL SETTINGS & TEXT CUSTOMIZATIONS
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Section: Monetization Controls
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Monetization Controls",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Show Video Ads", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Enables full-screen reward ads on stream links globally", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Switch(
                                            checked = settings.adsEnabled,
                                            onCheckedChange = { checked ->
                                                val updated = settings.copy(adsEnabled = checked)
                                                AppConfigManager.saveSettings(context, updated)
                                                settings = updated
                                                onConfigUpdated()
                                            }
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Ad on Back Button Exit", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Plays video ad when going back from stream browser", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Switch(
                                            checked = settings.backAdEnabled,
                                            onCheckedChange = { checked ->
                                                val updated = settings.copy(backAdEnabled = checked)
                                                AppConfigManager.saveSettings(context, updated)
                                                settings = updated
                                                onConfigUpdated()
                                            }
                                        )
                                    }

                                    // Ad Network
                                    Text("Active Ad Network:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("monetag", "startapp").forEach { net ->
                                            val isSelected = settings.adNetwork == net
                                            Card(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        val updated = settings.copy(adNetwork = net)
                                                        AppConfigManager.saveSettings(context, updated)
                                                        settings = updated
                                                        onConfigUpdated()
                                                    },
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                                ),
                                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                                            ) {
                                                Text(
                                                    text = net.uppercase(),
                                                    textAlign = TextAlign.Center,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(10.dp),
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }

                                    // Three-dot delay
                                    OutlinedTextField(
                                        value = threeDotAdDelayInput,
                                        onValueChange = {
                                            threeDotAdDelayInput = it
                                            val num = it.toIntOrNull() ?: 5
                                            val updated = settings.copy(showThreeDotAdDelaySeconds = num)
                                            AppConfigManager.saveSettings(context, updated)
                                            settings = updated
                                        },
                                        label = { Text("Three-dot developer profile ad length (seconds)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // Section: Front App Texts
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Front App Customization",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    OutlinedTextField(
                                        value = watchVisitTitleInput,
                                        onValueChange = {
                                            watchVisitTitleInput = it
                                            val updated = settings.copy(watchVisitText = it)
                                            AppConfigManager.saveSettings(context, updated)
                                            settings = updated
                                            onConfigUpdated()
                                        },
                                        label = { Text("Watch & Visit Title Text") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = watchVisitDescInput,
                                        onValueChange = {
                                            watchVisitDescInput = it
                                            val updated = settings.copy(watchVisitDesc = it)
                                            AppConfigManager.saveSettings(context, updated)
                                            settings = updated
                                            onConfigUpdated()
                                        },
                                        label = { Text("Watch & Visit Description Text") },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = sponsorTextInput,
                                        onValueChange = {
                                            sponsorTextInput = it
                                            val updated = settings.copy(sponsorText = it)
                                            AppConfigManager.saveSettings(context, updated)
                                            settings = updated
                                            onConfigUpdated()
                                        },
                                        label = { Text("Sponsor Display Text (e.g. SPONSORED BY MONETAG)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // Custom Splash Screen Logo Configuration
                                    Text(
                                        text = "Custom Splash Screen Logo (অ্যাপ শুরুর লোগো)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )

                                    val logoPickerLauncher = rememberLauncherForActivityResult(
                                        contract = ActivityResultContracts.GetContent()
                                    ) { uri ->
                                        uri?.let {
                                            try {
                                                val inputStream = context.contentResolver.openInputStream(uri)
                                                val bitmap = BitmapFactory.decodeStream(inputStream)
                                                if (bitmap != null) {
                                                    val outputStream = java.io.ByteArrayOutputStream()
                                                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
                                                    val bytes = outputStream.toByteArray()
                                                    val base64Str = Base64.encodeToString(bytes, Base64.DEFAULT)
                                                    val updated = settings.copy(splashLogoBase64 = base64Str)
                                                    AppConfigManager.saveSettings(context, updated)
                                                    settings = updated
                                                    Toast.makeText(context, "লোগো সফলভাবে সেট করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                                    onConfigUpdated()
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "লোগো সেট করতে সমস্যা হয়েছে!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (!settings.splashLogoBase64.isNullOrBlank()) {
                                            val logoBitmap = remember(settings.splashLogoBase64) {
                                                try {
                                                    val decoded = Base64.decode(settings.splashLogoBase64, Base64.DEFAULT)
                                                    BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                                                } catch (e: Exception) {
                                                    null
                                                }
                                            }
                                            if (logoBitmap != null) {
                                                Image(
                                                    bitmap = logoBitmap.asImageBitmap(),
                                                    contentDescription = "Splash Logo Preview",
                                                    modifier = Modifier
                                                        .size(60.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color.LightGray),
                                                    contentScale = ContentScale.Fit
                                                 )
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .background(Color.LightGray, RoundedCornerShape(8.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = "Default Logo", tint = Color.Gray)
                                            }
                                        }

                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Button(
                                                onClick = { logoPickerLauncher.launch("image/*") },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("লোগো পরিবর্তন করুন", fontSize = 11.sp, color = Color.White)
                                            }
                                            if (!settings.splashLogoBase64.isNullOrBlank()) {
                                                Text(
                                                     text = "লোগো রিমুভ করুন",
                                                     fontSize = 11.sp,
                                                     color = Color.Red,
                                                     fontWeight = FontWeight.Bold,
                                                     modifier = Modifier.clickable {
                                                         val updated = settings.copy(splashLogoBase64 = null)
                                                         AppConfigManager.saveSettings(context, updated)
                                                         settings = updated
                                                         Toast.makeText(context, "লোগো রিসেট করা হয়েছে।", Toast.LENGTH_SHORT).show()
                                                         onConfigUpdated()
                                                     }
                                                )
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = Color(0xFFCAC4D0).copy(alpha = 0.3f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Enable Fast Browser (UC Style)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Displays high-speed web browser with automatic video play download button", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Switch(
                                            checked = browserEnabledInput,
                                            onCheckedChange = { checked ->
                                                browserEnabledInput = checked
                                                val updated = settings.copy(browserEnabled = checked)
                                                AppConfigManager.saveSettings(context, updated)
                                                settings = updated
                                                onConfigUpdated()
                                            }
                                        )
                                    }

                                    if (browserEnabledInput) {
                                        OutlinedTextField(
                                            value = browserHomeUrlInput,
                                            onValueChange = {
                                                browserHomeUrlInput = it
                                                val updated = settings.copy(browserHomeUrl = it)
                                                AppConfigManager.saveSettings(context, updated)
                                                settings = updated
                                                onConfigUpdated()
                                            },
                                            label = { Text("Browser Home URL") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            // Section: GitHub Remote Config Setup
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "GitHub Remote Config Setup",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    Text(
                                        text = "কোডে গুগল শিট আইডি সরাসরি হার্ডকোড না রেখে গিটহাব থেকে ডাইনামিকালি লোড করা হচ্ছে। নিচে আপনার গিটহাব config.json ফাইলের Raw URL প্রদান করুন।",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    OutlinedTextField(
                                        value = inputGithubConfigUrl,
                                        onValueChange = { inputGithubConfigUrl = it },
                                        label = { Text("GitHub Raw config.json URL") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                if (inputGithubConfigUrl.isBlank()) {
                                                    Toast.makeText(context, "GitHub URL is required", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                val updatedS = settings.copy(githubConfigUrl = inputGithubConfigUrl.trim())
                                                AppConfigManager.saveSettings(context, updatedS)
                                                refreshAll()
                                                onConfigUpdated()
                                                Toast.makeText(context, "গিটহাব লিঙ্ক সফলভাবে সেভ করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("লিঙ্ক সেভ করুন", fontSize = 12.sp)
                                        }

                                        Button(
                                            onClick = {
                                                if (inputGithubConfigUrl.isBlank()) {
                                                    Toast.makeText(context, "GitHub URL is required", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                isGithubRefreshing = true
                                                coroutineScope.launch {
                                                    // First, save the URL
                                                    val savedSettings = settings.copy(githubConfigUrl = inputGithubConfigUrl.trim())
                                                    AppConfigManager.saveSettings(context, savedSettings)
                                                    
                                                    // Then, force sync
                                                    val success = GitHubConfigManager.fetchAndSyncWithGitHub(context, inputGithubConfigUrl.trim())
                                                    isGithubRefreshing = false
                                                    if (success) {
                                                        Toast.makeText(context, "GitHub config লোড হয়েছে এবং গুগল শিট সফলভাবে সিঙ্ক হয়েছে!", Toast.LENGTH_LONG).show()
                                                        refreshAll()
                                                        onConfigUpdated()
                                                    } else {
                                                        Toast.makeText(context, "গিটহাব বা গুগল শিট সিঙ্ক ব্যর্থ হয়েছে। লিঙ্ক ও ইন্টারনেট চেক করুন।", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f),
                                            enabled = !isGithubRefreshing
                                        ) {
                                            if (isGithubRefreshing) {
                                                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                                            } else {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Force Refresh", fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    // Display current Spreadsheet ID info
                                    if (settings.sheetId.isNotBlank()) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = "সক্রিয় গুগল স্প্রেডশিট আইডি (Active Sheet ID):",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = settings.sheetId,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Section: Cloud Spreadsheet ID & Connect Guide
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Google Sheets Sync Setup",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    if (settings.sheetId.isNotBlank()) {
                                        Surface(
                                            color = Color(0xFFE8F5E9),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Connected",
                                                    tint = Color(0xFF2E7D32)
                                                )
                                                Text(
                                                    text = "গুগল শিট সফলভাবে কানেক্ট ও সিঙ্ক করা হয়েছে!",
                                                    color = Color(0xFF1B5E20),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = inputSheetId,
                                        onValueChange = { inputSheetId = it },
                                        label = { Text("Google Spreadsheet ID") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Button(
                                        onClick = {
                                            if (inputSheetId.isBlank()) {
                                                Toast.makeText(context, "Spreadsheet ID is required", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            isSyncing = true
                                            coroutineScope.launch {
                                                val success = GoogleSheetsManager.fetchAndSyncConfig(context, inputSheetId)
                                                isSyncing = false
                                                if (success) {
                                                    Toast.makeText(context, "গুগল শিট সফলভাবে সিঙ্ক ও কানেক্ট করা হয়েছে!", Toast.LENGTH_LONG).show()
                                                    val updatedS = settings.copy(sheetId = inputSheetId)
                                                    AppConfigManager.saveSettings(context, updatedS)
                                                    refreshAll()
                                                    onConfigUpdated()
                                                } else {
                                                    Toast.makeText(context, "সিঙ্ক ব্যর্থ হয়েছে। ইন্টারনেট সংযোগ বা আইডি পরীক্ষা করুন।", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !isSyncing
                                    ) {
                                        if (isSyncing) {
                                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                                        } else {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("সিঙ্ক ও কানেক্ট করুন (Sync Now)", color = MaterialTheme.colorScheme.onPrimary)
                                        }
                                    }

                                    var showSheetsGuide by remember { mutableStateOf(false) }
                                    OutlinedButton(
                                        onClick = { showSheetsGuide = !showSheetsGuide },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (showSheetsGuide) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("গুগল শিট তৈরি ও কানেক্ট করার গাইড")
                                    }

                                    AnimatedVisibility(visible = showSheetsGuide) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "গুগল শিট দিয়ে এডমিন প্যানেল কানেক্ট করার গাইড:",
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = "১. প্রথমে আপনার গুগল ড্রাইভ থেকে একটি নতুন Google Sheets স্প্রেডশিট তৈরি করুন।\n" +
                                                            "২. স্প্রেডশিটের প্রথম র-তে (Header) দুটি কলাম রাখুন: KEY এবং VALUE\n" +
                                                            "৩. KEY কলামে settings ফাইলের নামগুলো লিখুন (যেমন: ADS_ENABLED, SPONSOR_TEXT, DONATION_NUMBER) এবং VALUE কলামে তাদের মান দিন।\n" +
                                                            "৪. স্প্রেডশিটের ফাইল মেনু থেকে Share -> 'Anyone with the link' (Viewer) মুড চালু করুন।\n" +
                                                            "৫. স্প্রেডশিটের URL থেকে ID-টি কপি করুন (এটি /d/ এবং /edit এর মধ্যের দীর্ঘ কোডটি)।\n" +
                                                            "৬. আইডি-টি উপরে বসিয়ে 'সিঙ্ক ও কানেক্ট করুন' বাটনে চাপ দিন। আপনার শিট কানেক্ট হয়ে যাবে!",
                                                    fontSize = 11.sp,
                                                    lineHeight = 16.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Section: Personal Ads & Ad Networks Integration
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "বিজ্ঞাপন নেটওয়ার্ক ও পার্সোনাল বিজ্ঞাপন কন্ট্রোল",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    // Third Party Network Codes
                                    OutlinedTextField(
                                        value = monetagAdCodeInput,
                                        onValueChange = { monetagAdCodeInput = it },
                                        label = { Text("Monetag Ad Placement Code / Tag ID") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = startappAppIdInput,
                                        onValueChange = { startappAppIdInput = it },
                                        label = { Text("StartApp SDK App ID") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                    // Personal Ads Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("ব্যক্তিগত বিজ্ঞাপন দেখান (Personal Ads)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("অন করলে থার্ড পার্টি বিজ্ঞাপনের পরিবর্তে নিজের ইমেজ/লিংক বিজ্ঞাপন দেখাবে", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Switch(
                                            checked = personalAdEnabledInput,
                                            onCheckedChange = { personalAdEnabledInput = it }
                                        )
                                    }

                                    if (personalAdEnabledInput) {
                                        OutlinedTextField(
                                            value = personalAdImageUrlInput,
                                            onValueChange = { personalAdImageUrlInput = it },
                                            label = { Text("পার্সোনাল ব্যানার ইমেজ লিংক (Image URL)") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )

                                        OutlinedTextField(
                                            value = personalAdClickUrlInput,
                                            onValueChange = { personalAdClickUrlInput = it },
                                            label = { Text("ক্লিক রিডাইরেক্ট লিংক (Click URL)") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                    }

                                    

                                     val filePickerLauncher = rememberLauncherForActivityResult(
                                         contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                                     ) { uri ->
                                         uri?.let {
                                             try {
                                                 val inputStream = context.contentResolver.openInputStream(uri)
                                                 val text = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                                                 if (text.isNotBlank()) {
                                                     iptvFileContentInput = text
                                                     val updated = settings.copy(iptvFileContent = text)
                                                     AppConfigManager.saveSettings(context, updated)
                                                     settings = updated
                                                     Toast.makeText(context, "M3U ফাইল সফলভাবে ইম্পোর্ট করা হয়েছে!", Toast.LENGTH_LONG).show()
                                                 } else {
                                                     Toast.makeText(context, "ফাইলটি খালি বা পড়া যায়নি!", Toast.LENGTH_SHORT).show()
                                                 }
                                             } catch (e: Exception) {
                                                 Toast.makeText(context, "ফাইল ইম্পোর্ট করতে সমস্যা হয়েছে!", Toast.LENGTH_SHORT).show()
                                             }
                                         }
                                     }

                                     Row(
                                         modifier = Modifier.fillMaxWidth(),
                                         horizontalArrangement = Arrangement.spacedBy(8.dp)
                                     ) {
                                         Button(
                                             onClick = {
                                                 try {
                                                     filePickerLauncher.launch("*/*")
                                                 } catch (e: Exception) {
                                                     Toast.makeText(context, "ফাইল সিলেক্টর চালু করা যায়নি।", Toast.LENGTH_SHORT).show()
                                                 }
                                             },
                                             colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                             shape = RoundedCornerShape(12.dp),
                                             modifier = Modifier.weight(1f)
                                         ) {
                                             Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                             Spacer(modifier = Modifier.width(4.dp))
                                             Text("ফাইল ইম্পোর্ট (.m3u)", color = Color.White, fontSize = 11.sp)
                                         }

                                         Button(
                                             onClick = {
                                                 val updated = settings.copy(
                                                     iptvEnabled = iptvEnabledInput,
                                                     iptvPlaylistUrl = iptvPlaylistUrlInput,
                                                     iptvSecondaryUrl = iptvSecondaryUrlInput,
                                                     iptvFileContent = iptvFileContentInput
                                                 )
                                                 AppConfigManager.saveSettings(context, updated)
                                                 settings = updated
                                                 Toast.makeText(context, "IPTV সেটিংস সফলভাবে সেভ হয়েছে!", Toast.LENGTH_SHORT).show()
                                                 onConfigUpdated()
                                             },
                                             colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                             shape = RoundedCornerShape(12.dp),
                                             modifier = Modifier.weight(1f)
                                         ) {
                                             Text("সেটিংস সেভ করুন", color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp)
                                         }
                                     }

                                     Card(
                                         colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                         shape = RoundedCornerShape(12.dp),
                                         border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                         modifier = Modifier.fillMaxWidth()
                                     ) {
                                         Column(
                                             modifier = Modifier.padding(12.dp),
                                             verticalArrangement = Arrangement.spacedBy(8.dp)
                                         ) {
                                             Text(
                                                 text = "আইপি টিভি (IPTV) সেটআপ করার সম্পূর্ণ গাইড ও নিয়মাবলী:",
                                                 fontWeight = FontWeight.Bold,
                                                 color = MaterialTheme.colorScheme.primary,
                                                 fontSize = 13.sp
                                             )
                                             Text(
                                                 text = "১. কিভাবে লিংক দিয়ে সেটআপ করবেন: 'Primary M3U Playlist URL' বক্সে আপনার আইপি টিভি সার্ভার বা প্রোভাইডারের ডিরেক্ট .m3u এক্সটেনশন লিংকটি বসান এবং নিচে 'সেটিংস সেভ করুন' বাটনে চাপ দিন।\n২. কিভাবে গ্যালারি/স্টোরেজ থেকে ফাইল দিয়ে সেটআপ করবেন: সরাসরি ফাইল ইমপোর্ট করতে 'ফাইল ইম্পোর্ট (.m3u)' লাল বাটনে চাপ দিন এবং আপনার ডিভাইস থেকে .m3u ফাইলটি সিলেক্ট করুন। ফাইলটির সমস্ত চ্যানেল কোড সরাসরি বক্সে লোড হবে। এরপর সেভ করুন।\n৩. ব্যবহারকারীদের জন্য IPTV ট্যাব অন/অফ করতে চাইলে উপরে 'IPTV ফিচার চালু করুন' সুইচটি ব্যবহার করতে পারবেন।",
                                                 fontSize = 11.sp,
                                                 lineHeight = 16.sp,
                                                 color = MaterialTheme.colorScheme.onSurfaceVariant
                                             )
                                         }
                                     }
                                     if (false) {
                                         Text("Dummy")
                                     }
                                 }
                             }
                         }
                     }
                     1 -> {
                         // WATCH BUTTONS CONFIGURATION
                         val scrollState1 = rememberScrollState()
                         Column(
                             modifier = Modifier
                                 .fillMaxSize()
                                 .verticalScroll(scrollState1)
                                 .padding(16.dp),
                             verticalArrangement = Arrangement.spacedBy(16.dp)
                         ) {
                             Card(
                                 modifier = Modifier.fillMaxWidth(),
                                 colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                 shape = RoundedCornerShape(20.dp),
                                 border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                             ) {
                                 Column(
                                     modifier = Modifier.padding(16.dp),
                                     verticalArrangement = Arrangement.spacedBy(12.dp)
                                 ) {
                                     Text(
                                         text = "নতুন ওয়াচ বাটন যুক্ত করুন",
                                         color = MaterialTheme.colorScheme.primary,
                                         fontSize = 15.sp,
                                         fontWeight = FontWeight.Bold
                                     )
                                     HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                     OutlinedTextField(
                                         value = btnNameInput,
                                         onValueChange = { btnNameInput = it },
                                         label = { Text("বাটন নাম (যেমন: Watch Server 5)") },
                                         singleLine = true,
                                         modifier = Modifier.fillMaxWidth()
                                     )

                                     OutlinedTextField(
                                         value = btnUrlInput,
                                         onValueChange = { btnUrlInput = it },
                                         label = { Text("গন্তব্য URL লিংক (Destination URL)") },
                                         singleLine = true,
                                         modifier = Modifier.fillMaxWidth()
                                     )

                                     Row(
                                         modifier = Modifier.fillMaxWidth(),
                                         horizontalArrangement = Arrangement.SpaceBetween,
                                         verticalAlignment = Alignment.CenterVertically
                                     ) {
                                         Text("এই বাটনে বিজ্ঞাপন দেখাবে:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                         Switch(
                                             checked = btnAdsEnabledInput,
                                             onCheckedChange = { btnAdsEnabledInput = it }
                                         )
                                     }

                                     HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                     // Button Image/Emoji Option Header
                                     Text(
                                         text = "বাটন আইকন বা ইমোজি নির্বাচন করুন:",
                                         fontWeight = FontWeight.Bold,
                                         fontSize = 12.sp,
                                         color = MaterialTheme.colorScheme.onSurfaceVariant
                                     )

                                     Row(
                                         modifier = Modifier.fillMaxWidth(),
                                         horizontalArrangement = Arrangement.spacedBy(12.dp),
                                         verticalAlignment = Alignment.CenterVertically
                                     ) {
                                         // Preview
                                         Box(
                                             modifier = Modifier
                                                 .size(56.dp)
                                                 .clip(RoundedCornerShape(8.dp))
                                                 .background(Color.LightGray),
                                             contentAlignment = Alignment.Center
                                         ) {
                                             if (!btnImageBase64Input.isNullOrBlank()) {
                                                 val bitmap = remember(btnImageBase64Input) {
                                                     try {
                                                         val decoded = Base64.decode(btnImageBase64Input, Base64.DEFAULT)
                                                         BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                                                     } catch (e: Exception) {
                                                         null
                                                     }
                                                 }
                                                 if (bitmap != null) {
                                                     Image(
                                                         bitmap = bitmap.asImageBitmap(),
                                                         contentDescription = "Button Preview",
                                                         modifier = Modifier.fillMaxSize(),
                                                         contentScale = ContentScale.Crop
                                                     )
                                                 }
                                             } else if (btnEmojiInput.isNotBlank()) {
                                                 Text(text = btnEmojiInput, fontSize = 24.sp)
                                             } else {
                                                 Icon(Icons.Default.PlayArrow, contentDescription = "Default Preview", tint = Color.Gray)
                                             }
                                         }

                                         Column(
                                             verticalArrangement = Arrangement.spacedBy(4.dp)
                                         ) {
                                             Button(
                                                 onClick = { btnImagePickerLauncher.launch("image/*") },
                                                 shape = RoundedCornerShape(8.dp),
                                                 colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                 contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                 modifier = Modifier.height(32.dp)
                                             ) {
                                                 Text("গ্যালারি থেকে ছবি আপলোড", fontSize = 11.sp, color = Color.White)
                                             }

                                             if (!btnImageBase64Input.isNullOrBlank() || btnEmojiInput.isNotBlank()) {
                                                 Text(
                                                     text = "রিমুভ করুন",
                                                     fontSize = 11.sp,
                                                     color = Color.Red,
                                                     fontWeight = FontWeight.Bold,
                                                     modifier = Modifier.clickable {
                                                         btnImageBase64Input = null
                                                         btnEmojiInput = ""
                                                     }
                                                 )
                                             }
                                         }
                                     }

                                     // Emoji Quick Picker
                                     OutlinedTextField(
                                         value = btnEmojiInput,
                                         onValueChange = {
                                             btnEmojiInput = it
                                             if (it.isNotBlank()) btnImageBase64Input = null
                                         },
                                         label = { Text("অথবা ইমোজি দিন (যেমন: ⚽)") },
                                         singleLine = true,
                                         modifier = Modifier.fillMaxWidth()
                                     )

                                     Row(
                                         modifier = Modifier.fillMaxWidth(),
                                         horizontalArrangement = Arrangement.spacedBy(6.dp)
                                     ) {
                                         val quickEmojis = listOf("📺", "🎬", "⚽", "📡", "🔥", "🎥", "💻", "⭐")
                                         quickEmojis.forEach { em ->
                                             Card(
                                                 modifier = Modifier
                                                     .size(32.dp)
                                                     .clickable {
                                                         btnEmojiInput = em
                                                         btnImageBase64Input = null
                                                     },
                                                 shape = RoundedCornerShape(6.dp),
                                                 colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                             ) {
                                                 Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                     Text(text = em, fontSize = 14.sp)
                                                 }
                                             }
                                         }
                                     }

                                     Button(
                                         onClick = {
                                             if (btnNameInput.isBlank() || btnUrlInput.isBlank()) {
                                                 Toast.makeText(context, "নাম এবং ইউআরএল দেওয়া আবশ্যক!", Toast.LENGTH_SHORT).show()
                                                 return@Button
                                             }
                                             val list = buttons.toMutableList()
                                             val nextId = if (list.isEmpty()) 1 else (list.maxOfOrNull { it.id } ?: 0) + 1
                                             list.add(
                                                 WatchButton(
                                                     id = nextId,
                                                     name = btnNameInput,
                                                     url = btnUrlInput,
                                                     customImageBase64 = btnImageBase64Input,
                                                     emoji = btnEmojiInput.trim(),
                                                     adsEnabled = btnAdsEnabledInput,
                                                     isEnabled = true
                                                 )
                                             )
                                             AppConfigManager.saveButtons(context, list)
                                             buttons = list
                                             btnNameInput = ""
                                             btnUrlInput = ""
                                             btnImageBase64Input = null
                                             btnEmojiInput = ""
                                             Toast.makeText(context, "ওয়াচ বাটন যুক্ত করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                             onConfigUpdated()
                                         },
                                         colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                         shape = RoundedCornerShape(12.dp),
                                         modifier = Modifier.fillMaxWidth()
                                     ) {
                                         Text("বাটন যুক্ত করুন", color = MaterialTheme.colorScheme.onPrimary)
                                     }
                                 }
                             }

                             Card(
                                 modifier = Modifier.fillMaxWidth(),
                                 colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                 shape = RoundedCornerShape(20.dp),
                                 border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                             ) {
                                 Column(
                                     modifier = Modifier.padding(16.dp),
                                     verticalArrangement = Arrangement.spacedBy(12.dp)
                                 ) {
                                     Text(
                                         text = "বিদ্যমান ওয়াচ বাটন তালিকা:",
                                         fontWeight = FontWeight.Bold,
                                         fontSize = 15.sp,
                                         color = MaterialTheme.colorScheme.primary
                                     )
                                     HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                     if (buttons.isEmpty()) {
                                         Text(
                                             text = "কোন ওয়াচ বাটন পাওয়া যায়নি।",
                                             color = Color.Gray,
                                             fontSize = 12.sp,
                                             textAlign = TextAlign.Center,
                                             modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                         )
                                     } else {
                                         Column(
                                             verticalArrangement = Arrangement.spacedBy(8.dp)
                                         ) {
                                             buttons.forEach { btn ->
                                                 Card(
                                                     modifier = Modifier.fillMaxWidth(),
                                                     colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                     border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                                 ) {
                                                     Row(
                                                         modifier = Modifier.fillMaxWidth().padding(10.dp),
                                                         horizontalArrangement = Arrangement.SpaceBetween,
                                                         verticalAlignment = Alignment.CenterVertically
                                                     ) {
                                                         Row(
                                                             verticalAlignment = Alignment.CenterVertically,
                                                             modifier = Modifier.weight(1f)
                                                         ) {
                                                             // Thumbnail / Icon
                                                             Box(
                                                                 modifier = Modifier
                                                                     .size(40.dp)
                                                                     .clip(RoundedCornerShape(50.dp))
                                                                     .background(MaterialTheme.colorScheme.secondaryContainer),
                                                                 contentAlignment = Alignment.Center
                                                             ) {
                                                                 if (!btn.customImageBase64.isNullOrBlank()) {
                                                                     val listBitmap = remember(btn.customImageBase64) {
                                                                         try {
                                                                             val decoded = Base64.decode(btn.customImageBase64, Base64.DEFAULT)
                                                                             BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                                                                         } catch (e: Exception) {
                                                                             null
                                                                         }
                                                                     }
                                                                     if (listBitmap != null) {
                                                                         Image(
                                                                             bitmap = listBitmap.asImageBitmap(),
                                                                             contentDescription = null,
                                                                             modifier = Modifier.fillMaxSize(),
                                                                             contentScale = ContentScale.Crop
                                                                         )
                                                                     }
                                                                 } else if (!btn.emoji.isNullOrBlank()) {
                                                                     Text(text = btn.emoji, fontSize = 18.sp)
                                                                 } else {
                                                                     Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                                                                 }
                                                             }

                                                             Spacer(modifier = Modifier.width(10.dp))

                                                             Column {
                                                                 Text(text = btn.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                                 Text(text = btn.url, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                                                                 Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                     Text(
                                                                         text = if (btn.isEnabled) "সচল (Active)" else "নিষ্ক্রিয় (Disabled)",
                                                                         fontSize = 9.sp,
                                                                         fontWeight = FontWeight.Bold,
                                                                         color = if (btn.isEnabled) Color(0xFF4CAF50) else Color.Red
                                                                     )
                                                                     Text(
                                                                         text = if (btn.adsEnabled) "বিজ্ঞাপন চালু" else "বিজ্ঞাপন বন্ধ",
                                                                         fontSize = 9.sp,
                                                                         color = Color.Gray
                                                                     )
                                                                 }
                                                             }
                                                         }

                                                         Row(verticalAlignment = Alignment.CenterVertically) {
                                                             IconButton(
                                                                 onClick = {
                                                                     editingButton = btn
                                                                 },
                                                                 modifier = Modifier.size(32.dp)
                                                             ) {
                                                                 Icon(Icons.Default.Edit, contentDescription = "Edit Button", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                             }

                                                             IconButton(
                                                                 onClick = {
                                                                     val list = buttons.toMutableList()
                                                                     list.remove(btn)
                                                                     AppConfigManager.saveButtons(context, list)
                                                                     buttons = list
                                                                     Toast.makeText(context, "বাটনটি মুছে ফেলা হয়েছে!", Toast.LENGTH_SHORT).show()
                                                                     onConfigUpdated()
                                                                 },
                                                                 modifier = Modifier.size(32.dp)
                                                             ) {
                                                                 Icon(Icons.Default.Delete, contentDescription = "Delete Button", tint = Color.Red, modifier = Modifier.size(18.dp))
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
                      2 -> {
                          // IPTV CONFIGURATION
                          val scrollState2 = rememberScrollState()
                          Column(
                              modifier = Modifier
                                  .fillMaxSize()
                                  .verticalScroll(scrollState2)
                                  .padding(16.dp),
                              verticalArrangement = Arrangement.spacedBy(16.dp)
                          ) {
                              Card(
                                  modifier = Modifier.fillMaxWidth(),
                                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                  shape = RoundedCornerShape(20.dp),
                                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                              ) {
                                  Column(
                                      modifier = Modifier.padding(16.dp),
                                      verticalArrangement = Arrangement.spacedBy(12.dp)
                                  ) {
                                      Text(
                                          text = "IPTV প্লেলিস্ট ও চ্যানেল সেটিংস",
                                          color = MaterialTheme.colorScheme.primary,
                                          fontSize = 15.sp,
                                          fontWeight = FontWeight.Bold
                                      )
                                      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                      Row(
                                          modifier = Modifier.fillMaxWidth(),
                                          horizontalArrangement = Arrangement.SpaceBetween,
                                          verticalAlignment = Alignment.CenterVertically
                                      ) {
                                          Column(modifier = Modifier.weight(1f)) {
                                              Text("IPTV ফিচার চালু করুন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                              Text("অন করলে ব্যবহারকারীদের জন্য IPTV ট্যাবটি প্রদর্শিত হবে", fontSize = 11.sp, color = Color.Gray)
                                          }
                                          Switch(
                                              checked = iptvEnabledInput,
                                              onCheckedChange = { iptvEnabledInput = it }
                                          )
                                      }

                                      Row(
                                          modifier = Modifier.fillMaxWidth(),
                                          horizontalArrangement = Arrangement.SpaceBetween,
                                          verticalAlignment = Alignment.CenterVertically
                                      ) {
                                          Column(modifier = Modifier.weight(1f)) {
                                              Text("IPTV সার্ভার ১ (Server 1) চালু রাখুন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                              Text("সার্ভার ১ থেকে লোড হওয়া সোর্স চ্যানেলগুলো দেখাবে", fontSize = 11.sp, color = Color.Gray)
                                          }
                                          Switch(
                                              checked = iptvServer1EnabledInput,
                                              onCheckedChange = { iptvServer1EnabledInput = it }
                                          )
                                      }

                                      Row(
                                          modifier = Modifier.fillMaxWidth(),
                                          horizontalArrangement = Arrangement.SpaceBetween,
                                          verticalAlignment = Alignment.CenterVertically
                                      ) {
                                          Column(modifier = Modifier.weight(1f)) {
                                              Text("IPTV সার্ভার ২ (Server 2) চালু রাখুন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                              Text("সার্ভার ২ থেকে লোড হওয়া সোর্স চ্যানেলগুলো দেখাবে", fontSize = 11.sp, color = Color.Gray)
                                          }
                                          Switch(
                                              checked = iptvServer2EnabledInput,
                                              onCheckedChange = { iptvServer2EnabledInput = it }
                                          )
                                      }

                                      OutlinedTextField(
                                          value = iptvPlaylistUrlInput,
                                          onValueChange = { iptvPlaylistUrlInput = it },
                                          label = { Text("Primary M3U Playlist URL") },
                                          modifier = Modifier.fillMaxWidth(),
                                          singleLine = true
                                      )

                                      OutlinedTextField(
                                          value = iptvSecondaryUrlInput,
                                          onValueChange = { iptvSecondaryUrlInput = it },
                                          label = { Text("Backup M3U Playlist URL (Optional)") },
                                          modifier = Modifier.fillMaxWidth(),
                                          singleLine = true
                                      )

                                      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                      val filePickerLauncher = rememberLauncherForActivityResult(
                                          contract = ActivityResultContracts.GetContent()
                                      ) { uri ->
                                          uri?.let {
                                              try {
                                                  val inputStream = context.contentResolver.openInputStream(uri)
                                                  val text = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                                                  if (text.isNotBlank()) {
                                                      iptvPendingFileContentInput = text
                                                      val updated = settings.copy(iptvPendingFileContent = text)
                                                      AppConfigManager.saveSettings(context, updated)
                                                      settings = updated
                                                      Toast.makeText(context, "M3U ফাইলটি অপেক্ষমান তালিকায় লোড হয়েছে। নিচের এপ্রুভাল প্যানেল থেকে সিলেক্ট করুন!", Toast.LENGTH_LONG).show()
                                                  } else {
                                                      Toast.makeText(context, "ফাইলটি খালি বা পড়া যায়নি!", Toast.LENGTH_SHORT).show()
                                                  }
                                              } catch (e: Exception) {
                                                  Toast.makeText(context, "ফাইল ইম্পোর্ট করতে সমস্যা হয়েছে!", Toast.LENGTH_SHORT).show()
                                              }
                                          }
                                      }

                                      Row(
                                          modifier = Modifier.fillMaxWidth(),
                                          horizontalArrangement = Arrangement.spacedBy(8.dp)
                                      ) {
                                          Button(
                                              onClick = {
                                                  try {
                                                      filePickerLauncher.launch("*/*")
                                                  } catch (e: Exception) {
                                                      Toast.makeText(context, "ফাইল সিলেক্টর চালু করা যায়নি।", Toast.LENGTH_SHORT).show()
                                                  }
                                              },
                                              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                              shape = RoundedCornerShape(12.dp),
                                              modifier = Modifier.weight(1f)
                                          ) {
                                              Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                              Spacer(modifier = Modifier.width(4.dp))
                                              Text("ফাইল ইম্পোর্ট (.m3u)", color = Color.White, fontSize = 11.sp)
                                          }

                                          Button(
                                              onClick = {
                                                  val updated = settings.copy(
                                                      iptvEnabled = iptvEnabledInput,
                                                      iptvPlaylistUrl = iptvPlaylistUrlInput,
                                                      iptvSecondaryUrl = iptvSecondaryUrlInput,
                                                      iptvFileContent = iptvFileContentInput,
                                                      iptvServer1Enabled = iptvServer1EnabledInput,
                                                      iptvServer2Enabled = iptvServer2EnabledInput
                                                  )
                                                  AppConfigManager.saveSettings(context, updated)
                                                  settings = updated
                                                  Toast.makeText(context, "IPTV সেটিংস সফলভাবে সেভ হয়েছে!", Toast.LENGTH_SHORT).show()
                                                  onConfigUpdated()
                                              },
                                              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                              shape = RoundedCornerShape(12.dp),
                                              modifier = Modifier.weight(1f)
                                          ) {
                                              Text("সেটিংস সেভ করুন", color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp)
                                          }
                                      }

                                      Card(
                                          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                          shape = RoundedCornerShape(12.dp),
                                          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                          modifier = Modifier.fillMaxWidth()
                                      ) {
                                          Column(
                                              modifier = Modifier.padding(12.dp),
                                              verticalArrangement = Arrangement.spacedBy(8.dp)
                                          ) {
                                              Text(
                                                  text = "আইপি টিভি (IPTV) সেটআপ করার সম্পূর্ণ গাইড ও নিয়মাবলী:",
                                                  fontWeight = FontWeight.Bold,
                                                  color = MaterialTheme.colorScheme.primary,
                                                  fontSize = 13.sp
                                              )
                                              Text(
                                                  text = "১. কিভাবে লিংক দিয়ে সেটআপ করবেন: 'Primary M3U Playlist URL' বক্সে আপনার আইপি টিভি সার্ভার বা প্রোভাইডারের ডিরেক্ট .m3u এক্সটেনশন লিংকটি বসান এবং নিচে 'সেটিংস সেভ করুন' বাটনে চাপ দিন।\n২. কিভাবে ডিভাইস থেকে ফাইল দিয়ে সেটআপ করবেন: সরাসরি ফাইল ইমপোর্ট করতে 'ফাইল ইম্পোর্ট (.m3u)' লাল বাটনে চাপ দিন। ফাইলটির সমস্ত চ্যানেল অপেক্ষমান তালিকায় লোড হবে। ওখান থেকে আপনার পছন্দের চ্যানেল সিলেক্ট ও ক্যাটাগরি সেট করে 'অ্যাপে পাবলিশ' করুন।\n৩. সার্ভার বন্ধ বা চালু করা: প্লেয়ারের সার্ভার ১ ও সার্ভার ২ আলাদাভাবে সচল বা বন্ধ রাখতে পারবেন সুইচগুলোর মাধ্যমে।",
                                                  fontSize = 11.sp,
                                                  lineHeight = 16.sp,
                                                  color = MaterialTheme.colorScheme.onSurfaceVariant
                                              )
                                          }
                                      }
                                  }
                              }

                              // NEW CHANNEL APPROVAL & CATEGORY SELECTION CARD
                              Card(
                                  modifier = Modifier.fillMaxWidth(),
                                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                  shape = RoundedCornerShape(20.dp),
                                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                              ) {
                                  Column(
                                      modifier = Modifier.padding(16.dp),
                                      verticalArrangement = Arrangement.spacedBy(12.dp)
                                  ) {
                                      Text(
                                          text = "চ্যানেল এপ্রুভাল ও ক্যাটাগরি সিলেকশন প্যানেল",
                                          color = MaterialTheme.colorScheme.primary,
                                          fontSize = 15.sp,
                                          fontWeight = FontWeight.Bold
                                      )
                                      Text(
                                          text = "লোড করা M3U ফাইলের চ্যানেলগুলো প্লেয়ারে দেখানোর আগে এখান থেকে সিলেক্ট ও ক্যাটাগরি সেট করে পাবলিশ করতে হবে।",
                                          fontSize = 11.sp,
                                          color = Color.Gray
                                      )
                                      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                      // Managed channels list state
                                      val managedChannels = remember(iptvPendingFileContentInput, iptvFileContentInput) {
                                          val list = mutableStateListOf<IptvChannel>()
                                          if (iptvPendingFileContentInput.isNotBlank()) {
                                              list.addAll(IptvParser.parseM3u(iptvPendingFileContentInput))
                                          } else if (iptvFileContentInput.isNotBlank()) {
                                              list.addAll(IptvParser.parseM3u(iptvFileContentInput))
                                          }
                                          list
                                      }

                                      // Selected stream URLs for active publishing status
                                      val selectedUrls = remember(managedChannels) {
                                          val urls = mutableStateListOf<String>()
                                          urls.addAll(managedChannels.map { it.streamUrl })
                                          urls
                                      }

                                      var searchQuery by remember { mutableStateOf("") }
                                      var showAddChannelDialog by remember { mutableStateOf(false) }

                                      // Form states for manual channel addition
                                      var newChannelName by remember { mutableStateOf("") }
                                      var newChannelUrl by remember { mutableStateOf("") }
                                      var newChannelCat by remember { mutableStateOf("All") }
                                      var newChannelLogo by remember { mutableStateOf("") }

                                      if (managedChannels.isEmpty()) {
                                          Box(
                                              modifier = Modifier
                                                  .fillMaxWidth()
                                                  .padding(24.dp),
                                              contentAlignment = Alignment.Center
                                          ) {
                                              Column(
                                                  horizontalAlignment = Alignment.CenterHorizontally,
                                                  verticalArrangement = Arrangement.spacedBy(8.dp)
                                              ) {
                                                  Text(
                                                      "কোন অপেক্ষমান বা পাবলিশড চ্যানেল নেই। অনুগ্রহ করে একটি .m3u ফাইল ইম্পোর্ট করুন অথবা নিচে ম্যানুয়ালি চ্যানেল যোগ করুন!",
                                                      fontSize = 12.sp,
                                                      color = Color.Gray,
                                                      textAlign = TextAlign.Center
                                                  )
                                                  Button(
                                                      onClick = { showAddChannelDialog = true },
                                                      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                                  ) {
                                                      Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                      Spacer(modifier = Modifier.width(4.dp))
                                                      Text("ম্যানুয়ালি চ্যানেল যোগ করুন", fontSize = 11.sp)
                                                  }
                                              }
                                          }
                                      } else {
                                          OutlinedTextField(
                                              value = searchQuery,
                                              onValueChange = { searchQuery = it },
                                              label = { Text("চ্যানেল সার্চ করুন...") },
                                              leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                              singleLine = true,
                                              modifier = Modifier.fillMaxWidth()
                                          )

                                          Row(
                                              modifier = Modifier.fillMaxWidth(),
                                              horizontalArrangement = Arrangement.SpaceBetween,
                                              verticalAlignment = Alignment.CenterVertically
                                          ) {
                                              Text(
                                                  text = "চ্যানেল সংখ্যা: ${managedChannels.size} | সিলেক্টেড: ${selectedUrls.size}",
                                                  fontSize = 12.sp,
                                                  fontWeight = FontWeight.Bold,
                                                  color = MaterialTheme.colorScheme.onSurfaceVariant
                                              )
                                              Button(
                                                  onClick = { showAddChannelDialog = true },
                                                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                  shape = RoundedCornerShape(8.dp),
                                                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                  modifier = Modifier.height(32.dp)
                                              ) {
                                                  Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                                  Spacer(modifier = Modifier.width(2.dp))
                                                  Text("ম্যানুয়াল চ্যানেল যোগ", fontSize = 10.sp)
                                              }
                                          }

                                          Row(
                                              modifier = Modifier.fillMaxWidth(),
                                              horizontalArrangement = Arrangement.spacedBy(8.dp)
                                          ) {
                                              TextButton(
                                                  onClick = {
                                                      selectedUrls.clear()
                                                      selectedUrls.addAll(managedChannels.map { it.streamUrl })
                                                  },
                                                  modifier = Modifier.weight(1f)
                                              ) {
                                                  Text("সব সিলেক্ট করুন", fontSize = 11.sp)
                                              }
                                              TextButton(
                                                  onClick = {
                                                      selectedUrls.clear()
                                                  },
                                                  modifier = Modifier.weight(1f)
                                              ) {
                                                  Text("সব ডেসিলক্ট করুন", fontSize = 11.sp)
                                              }
                                              TextButton(
                                                  onClick = {
                                                      managedChannels.clear()
                                                      selectedUrls.clear()
                                                  },
                                                  modifier = Modifier.weight(1f),
                                                  colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                              ) {
                                                  Text("সব ডিলিট করুন", fontSize = 11.sp)
                                              }
                                          }

                                          val filteredChannels = remember(managedChannels, searchQuery) {
                                              managedChannels.filter {
                                                  it.name.contains(searchQuery, ignoreCase = true) ||
                                                  it.group.contains(searchQuery, ignoreCase = true)
                                              }
                                          }

                                          // We show channels using a vertical scroll Box to avoid scroll conflict inside parent
                                          Box(
                                              modifier = Modifier
                                                  .fillMaxWidth()
                                                  .heightIn(max = 280.dp)
                                                  .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                                  .background(MaterialTheme.colorScheme.surface)
                                          ) {
                                              val listScrollState = rememberScrollState()
                                              Column(
                                                  modifier = Modifier
                                                      .fillMaxSize()
                                                      .verticalScroll(listScrollState)
                                                      .padding(8.dp),
                                                  verticalArrangement = Arrangement.spacedBy(8.dp)
                                              ) {
                                                  filteredChannels.forEach { channel ->
                                                      val isChecked = selectedUrls.contains(channel.streamUrl)
                                                      Card(
                                                          modifier = Modifier.fillMaxWidth(),
                                                          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                      ) {
                                                          Column(
                                                              modifier = Modifier
                                                                  .fillMaxWidth()
                                                                  .padding(8.dp),
                                                              verticalArrangement = Arrangement.spacedBy(8.dp)
                                                          ) {
                                                              Row(
                                                                  modifier = Modifier.fillMaxWidth(),
                                                                  verticalAlignment = Alignment.CenterVertically,
                                                                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                              ) {
                                                                  Checkbox(
                                                                      checked = isChecked,
                                                                      onCheckedChange = { checked ->
                                                                          if (checked) {
                                                                              if (!selectedUrls.contains(channel.streamUrl)) {
                                                                                  selectedUrls.add(channel.streamUrl)
                                                                              }
                                                                          } else {
                                                                              selectedUrls.remove(channel.streamUrl)
                                                                          }
                                                                      }
                                                                  )

                                                                  Box(
                                                                      modifier = Modifier
                                                                          .size(36.dp)
                                                                          .clip(CircleShape)
                                                                          .background(MaterialTheme.colorScheme.secondaryContainer),
                                                                      contentAlignment = Alignment.Center
                                                                  ) {
                                                                      if (channel.logoUrl.isNotBlank()) {
                                                                          AsyncImage(
                                                                              model = channel.logoUrl,
                                                                              contentDescription = null,
                                                                              modifier = Modifier.fillMaxSize(),
                                                                              contentScale = ContentScale.Crop
                                                                          )
                                                                      } else {
                                                                          Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
                                                                      }
                                                                  }

                                                                  // Editable Name Field
                                                                  OutlinedTextField(
                                                                      value = channel.name,
                                                                      onValueChange = { newName ->
                                                                          val idx = managedChannels.indexOf(channel)
                                                                          if (idx != -1) {
                                                                              managedChannels[idx] = channel.copy(name = newName)
                                                                          }
                                                                      },
                                                                      label = { Text("চ্যানেলের নাম", fontSize = 9.sp) },
                                                                      singleLine = true,
                                                                      modifier = Modifier.weight(1f),
                                                                      textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                                  )

                                                                  // Delete Button
                                                                  IconButton(
                                                                      onClick = {
                                                                          managedChannels.remove(channel)
                                                                          selectedUrls.remove(channel.streamUrl)
                                                                      }
                                                                  ) {
                                                                      Icon(
                                                                          imageVector = Icons.Default.Delete,
                                                                          contentDescription = "Delete",
                                                                          tint = MaterialTheme.colorScheme.error
                                                                      )
                                                                  }
                                                              }

                                                              Row(
                                                                  modifier = Modifier.fillMaxWidth().padding(start = 44.dp),
                                                                  verticalAlignment = Alignment.CenterVertically,
                                                                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                              ) {
                                                                  // Editable Category Field
                                                                  OutlinedTextField(
                                                                      value = channel.group,
                                                                      onValueChange = { newGroup ->
                                                                          val idx = managedChannels.indexOf(channel)
                                                                          if (idx != -1) {
                                                                              managedChannels[idx] = channel.copy(group = newGroup)
                                                                          }
                                                                      },
                                                                      label = { Text("ক্যাটাগরি", fontSize = 9.sp) },
                                                                      singleLine = true,
                                                                      modifier = Modifier.weight(1f),
                                                                      textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                                                                  )

                                                                  // Read-only stream link display
                                                                  Text(
                                                                      text = channel.streamUrl,
                                                                      fontSize = 9.sp,
                                                                      color = Color.Gray,
                                                                      maxLines = 1,
                                                                      overflow = TextOverflow.Ellipsis,
                                                                      modifier = Modifier.weight(1f).padding(top = 8.dp)
                                                                  )
                                                              }
                                                          }
                                                      }
                                                  }
                                              }
                                          }

                                          Spacer(modifier = Modifier.height(8.dp))

                                          Button(
                                              onClick = {
                                                  val finalizedChannels = managedChannels.filter {
                                                      selectedUrls.contains(it.streamUrl)
                                                  }
                                                  if (finalizedChannels.isEmpty()) {
                                                      Toast.makeText(context, "কোন চ্যানেল সিলেক্ট করা হয়নি!", Toast.LENGTH_SHORT).show()
                                                      return@Button
                                                  }

                                                  // Serialize back to M3U format
                                                  val serializedM3u = serializeChannelsToM3u(finalizedChannels)
                                                  // Save into active M3U file settings
                                                  iptvFileContentInput = serializedM3u
                                                  iptvPendingFileContentInput = "" // Clear pending as it is published
                                                  val updated = settings.copy(
                                                      iptvFileContent = serializedM3u,
                                                      iptvPendingFileContent = ""
                                                  )
                                                  AppConfigManager.saveSettings(context, updated)
                                                  settings = updated
                                                  Toast.makeText(context, "${finalizedChannels.size} টি চ্যানেল সফলভাবে সেভ ও পাবলিশ হয়েছে!", Toast.LENGTH_LONG).show()
                                                  onConfigUpdated()
                                              },
                                              modifier = Modifier.fillMaxWidth(),
                                              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                          ) {
                                              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                  Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                                  Text("সিলেক্ট করা চ্যানেলগুলো অ্যাপে পাবলিশ করুন", color = Color.White)
                                              }
                                          }
                                      }

                                      // Add Manual Channel Dialog
                                      if (showAddChannelDialog) {
                                          AlertDialog(
                                              onDismissRequest = { showAddChannelDialog = false },
                                              title = { Text("নতুন চ্যানেল যোগ করুন") },
                                              text = {
                                                  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                      OutlinedTextField(
                                                          value = newChannelName,
                                                          onValueChange = { newChannelName = it },
                                                          label = { Text("চ্যানেলের নাম (আবশ্যক)") },
                                                          singleLine = true,
                                                          modifier = Modifier.fillMaxWidth()
                                                      )
                                                      OutlinedTextField(
                                                          value = newChannelUrl,
                                                          onValueChange = { newChannelUrl = it },
                                                          label = { Text("স্ট্রীম URL/M3U লিংক (আবশ্যক)") },
                                                          singleLine = true,
                                                          modifier = Modifier.fillMaxWidth()
                                                      )
                                                      OutlinedTextField(
                                                          value = newChannelCat,
                                                          onValueChange = { newChannelCat = it },
                                                          label = { Text("ক্যাটাগরি/গ্রুপ") },
                                                          singleLine = true,
                                                          modifier = Modifier.fillMaxWidth()
                                                      )
                                                      OutlinedTextField(
                                                          value = newChannelLogo,
                                                          onValueChange = { newChannelLogo = it },
                                                          label = { Text("লোগো URL (ঐচ্ছিক)") },
                                                          singleLine = true,
                                                          modifier = Modifier.fillMaxWidth()
                                                      )
                                                  }
                                              },
                                              confirmButton = {
                                                  Button(
                                                      onClick = {
                                                          if (newChannelName.isBlank() || newChannelUrl.isBlank()) {
                                                              Toast.makeText(context, "নাম এবং লিংক প্রদান করতে হবে!", Toast.LENGTH_SHORT).show()
                                                              return@Button
                                                          }
                                                          val newChan = IptvChannel(
                                                              name = newChannelName.trim(),
                                                              streamUrl = newChannelUrl.trim(),
                                                              group = newChannelCat.trim().ifBlank { "All" },
                                                              logoUrl = newChannelLogo.trim()
                                                          )
                                                          managedChannels.add(newChan)
                                                          selectedUrls.add(newChan.streamUrl)
                                                          // Reset inputs
                                                          newChannelName = ""
                                                          newChannelUrl = ""
                                                          newChannelCat = "All"
                                                          newChannelLogo = ""
                                                          showAddChannelDialog = false
                                                          Toast.makeText(context, "চ্যানেলটি তালিকায় যোগ করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                                      }
                                                  ) {
                                                      Text("যোগ করুন")
                                                  }
                                              },
                                              dismissButton = {
                                                  TextButton(onClick = { showAddChannelDialog = false }) {
                                                      Text("বাতিল")
                                                  }
                                              }
                                          )
                                      }
                                  }
                              }
                          }
                      }

                    3 -> {
                        // DONATION & ACTIVE SUBSCRIPTIONS MANAGEMENT
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "ডোনেশন নির্দেশিকা ও নাম্বার কনফিগার",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    OutlinedTextField(
                                        value = donationNumberInput,
                                        onValueChange = {
                                            donationNumberInput = it
                                            val updated = settings.copy(donationNumber = it)
                                            AppConfigManager.saveSettings(context, updated)
                                            settings = updated
                                        },
                                        label = { Text("টাকা রিসিভ করার নাম্বার (বিকাশ/নগদ)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = donationInstructionsInput,
                                        onValueChange = {
                                            donationInstructionsInput = it
                                            val updated = settings.copy(donationInstructions = it)
                                            AppConfigManager.saveSettings(context, updated)
                                            settings = updated
                                        },
                                        label = { Text("টাকা পাঠানোর নিয়ম / স্ট্রাকচার") },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = donationPackagesInput,
                                        onValueChange = {
                                            donationPackagesInput = it
                                            val updated = settings.copy(donationPackages = it)
                                            AppConfigManager.saveSettings(context, updated)
                                            settings = updated
                                        },
                                        label = { Text("উপলব্ধ প্যাকেজ ও চার্জ সমূহ") },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Button(
                                        onClick = {
                                            val updated = settings.copy(
                                                donationNumber = donationNumberInput,
                                                donationInstructions = donationInstructionsInput,
                                                donationPackages = donationPackagesInput
                                            )
                                            AppConfigManager.saveSettings(context, updated)
                                            settings = updated
                                            Toast.makeText(context, "ডোনেশন সেটিংস সফলভাবে আপডেট করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                            onConfigUpdated()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("ডোনেশন কনফিগার সেভ করুন", color = MaterialTheme.colorScheme.onPrimary)
                                    }
                                }
                            }

                            Text("ব্যবহারকারীদের সাবমিট করা ডোনেশন ও ট্রানজেকশন আইডি:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (donationsList.isEmpty()) {
                                    item {
                                        Text(
                                            "কোন ডোনেশন রেকর্ড পাওয়া যায়নি।",
                                            fontSize = 12.sp,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(16.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    items(donationsList) { don ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text("প্রেরক: ${don.senderName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                        Text("মোবাইল: ${don.senderPhone}", fontSize = 11.sp, color = Color.Gray)
                                                        Text("প্যাকেজ মেয়াদ: ${don.expirationDate}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6750A4))
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(
                                                                when (don.status) {
                                                                    "Approved" -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                                                    "Declined" -> Color(0xFFF44336).copy(alpha = 0.15f)
                                                                    else -> Color(0xFFFF9800).copy(alpha = 0.15f)
                                                                }
                                                            )
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = don.status,
                                                            color = when (don.status) {
                                                                "Approved" -> Color(0xFF4CAF50)
                                                                "Declined" -> Color(0xFFF44336)
                                                                else -> Color(0xFFFF9800)
                                                            },
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                }

                                                Text("TxID: ${don.transactionId} (${don.paymentMethod})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.DarkGray)
                                                Text("সাবমিট সময়: ${don.timestamp}", fontSize = 10.sp, color = Color.Gray)

                                                HorizontalDivider(color = Color(0xFFCAC4D0).copy(alpha = 0.3f))

                                                // Approve, Decline, Cancel buttons
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            val updatedList = donationsList.map {
                                                                if (it.id == don.id) it.copy(status = "Approved") else it
                                                            }
                                                            AppConfigManager.saveDonations(context, updatedList)
                                                            donationsList = updatedList
                                                            Toast.makeText(context, "সাবস্ক্রিপশন সচল (Approved) করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                                        contentPadding = PaddingValues(horizontal = 10.dp),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier
                                                            .weight(1.5f)
                                                            .height(32.dp)
                                                    ) {
                                                        Text("Approve", fontSize = 11.sp, color = Color.White)
                                                    }

                                                    Button(
                                                        onClick = {
                                                            val updatedList = donationsList.map {
                                                                if (it.id == don.id) it.copy(status = "Declined") else it
                                                            }
                                                            AppConfigManager.saveDonations(context, updatedList)
                                                            donationsList = updatedList
                                                            Toast.makeText(context, "সাবস্ক্রিপশন বাতিল (Declined) করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                                        contentPadding = PaddingValues(horizontal = 10.dp),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier
                                                            .weight(1.5f)
                                                            .height(32.dp)
                                                    ) {
                                                        Text("Decline", fontSize = 11.sp, color = Color.White)
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            val updatedList = donationsList.toMutableList()
                                                            updatedList.remove(don)
                                                            AppConfigManager.saveDonations(context, updatedList)
                                                            donationsList = updatedList
                                                            Toast.makeText(context, "রেকর্ড মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
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
                        // PREMIUM LINKS & SECURITY SETTINGS & USER FEEDBACKS
                        val scrollState4 = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState4)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Part A: Add new premium item Form
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "নতুন প্রিমিয়াম লিংক যুক্ত করুন",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    OutlinedTextField(
                                        value = newPremName,
                                        onValueChange = { newPremName = it },
                                        label = { Text("নাম (Inshot premium APK)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = newPremUrl,
                                        onValueChange = { newPremUrl = it },
                                        label = { Text("গন্তব্য URL ডাউনলোড লিংক") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // Category Select Pills
                                    Text("ক্যাটাগরি নির্বাচন করুন:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val cats = listOf("Premium APK", "Courses", "Books", "Movies")
                                        cats.forEach { cat ->
                                            val isSel = selectedPremCat == cat
                                            Card(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { selectedPremCat = cat },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                                ),
                                                border = BorderStroke(1.dp, if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                                            ) {
                                                Text(
                                                    text = cat.replace("Premium ", ""),
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 8.dp),
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (newPremName.isBlank() || newPremUrl.isBlank()) {
                                                Toast.makeText(context, "নাম এবং লিংক দুটোই আবশ্যক।", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            val list = premiumItemsList.toMutableList()
                                            list.add(
                                                PremiumItem(
                                                    id = UUID.randomUUID().toString(),
                                                    name = newPremName,
                                                    url = newPremUrl,
                                                    category = selectedPremCat
                                                )
                                            )
                                            AppConfigManager.savePremiumItems(context, list)
                                            premiumItemsList = list
                                            newPremName = ""
                                            newPremUrl = ""
                                            Toast.makeText(context, "প্রিমিয়াম লিংক যুক্ত করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                            onConfigUpdated()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("লিংক যুক্ত করুন", color = MaterialTheme.colorScheme.onPrimary)
                                    }
                                }
                            }

                            // Part A2: Existing Premium Items list (Admin Control Panel)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "বিদ্যমান প্রিমিয়াম আইটেম তালিকা",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    if (premiumItemsList.isEmpty()) {
                                        Text(
                                            text = "কোন প্রিমিয়াম আইটেম পাওয়া যায়নি।",
                                            fontSize = 12.sp,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                        )
                                    } else {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            premiumItemsList.forEach { item ->
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                            Text(
                                                                text = item.name,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 13.sp,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                text = item.url,
                                                                fontSize = 11.sp,
                                                                color = Color.Gray,
                                                                maxLines = 1
                                                            )
                                                            Box(
                                                                modifier = Modifier
                                                                    .padding(top = 4.dp)
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(
                                                                    text = item.category,
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                                )
                                                            }
                                                        }
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            IconButton(
                                                                onClick = {
                                                                    editingPremiumItem = item
                                                                },
                                                                modifier = Modifier.size(32.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Edit,
                                                                    contentDescription = "Edit Item",
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                            IconButton(
                                                                onClick = {
                                                                    val list = premiumItemsList.toMutableList()
                                                                    list.remove(item)
                                                                    AppConfigManager.savePremiumItems(context, list)
                                                                    premiumItemsList = list
                                                                    Toast.makeText(context, "আইটেমটি সফলভাবে মুছে ফেলা হয়েছে!", Toast.LENGTH_SHORT).show()
                                                                    onConfigUpdated()
                                                                },
                                                                modifier = Modifier.size(32.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Delete,
                                                                    contentDescription = "Delete Item",
                                                                    tint = Color.Red,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Part B: Multi-step PIN Security Settings
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "3-Tier Admin Security System",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    OutlinedTextField(
                                        value = pin1Input,
                                        onValueChange = { if (it.length <= 4) pin1Input = it },
                                        label = { Text("১ম সিকিউরিটি পিন (৪ ডিজিট)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = pin2Input,
                                        onValueChange = { if (it.length <= 3) pin2Input = it },
                                        label = { Text("২য় সিকিউরিটি পিন (৩ ডিজিট)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = pin3Input,
                                        onValueChange = { if (it.length <= 4) pin3Input = it },
                                        label = { Text("৩য় সিকিউরিটি পিন (৪ ডিজিট)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = seqQuestionInput,
                                        onValueChange = { seqQuestionInput = it },
                                        label = { Text("রিসেট প্রশ্ন (যেমন: What is my nickname?)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = seqAnswerInput,
                                        onValueChange = { seqAnswerInput = it },
                                        label = { Text("রিসেট প্রশ্নের উত্তর") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Button(
                                        onClick = {
                                            if (pin1Input.length != 4 || pin2Input.length != 3 || pin3Input.length != 4) {
                                                Toast.makeText(context, "পিনগুলোর সঠিক দৈর্ঘ্য ১নং: ৪, ২নং: ৩, ৩নং: ৪ ডিজিট হতে হবে।", Toast.LENGTH_LONG).show()
                                                return@Button
                                            }
                                            if (seqQuestionInput.isBlank() || seqAnswerInput.isBlank()) {
                                                Toast.makeText(context, "পিন রিসেট প্রশ্ন ও উত্তর দেওয়া বাধ্যতামূলক।", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            val updated = settings.copy(
                                                pin1 = pin1Input,
                                                pin2 = pin2Input,
                                                pin3 = pin3Input,
                                                securityQuestion = seqQuestionInput,
                                                securityAnswer = seqAnswerInput
                                            )
                                            AppConfigManager.saveSettings(context, updated)
                                            settings = updated
                                            Toast.makeText(context, "৩-স্তর বিশিষ্ট সিকিউরিটি সফলভাবে সেভ করা হয়েছে!", Toast.LENGTH_LONG).show()
                                            onConfigUpdated()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("সিকিউরিটি সেটিংস আপডেট করুন", color = Color.White)
                                    }
                                }
                            }

                            // Part C: Founder Social Connections Setup
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Founder Social Connections",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    OutlinedTextField(
                                        value = contactNameInput,
                                        onValueChange = { contactNameInput = it },
                                        label = { Text("প্রতিষ্ঠাতা নাম (Founder Name)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = contactWhatsAppInput,
                                        onValueChange = { contactWhatsAppInput = it },
                                        label = { Text("হোয়াটসঅ্যাপ নাম্বার (WhatsApp Link No)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = contactFacebookInput,
                                        onValueChange = { contactFacebookInput = it },
                                        label = { Text("ফেসবুক প্রোফাইল লিংক (Facebook Profile URL)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = feedbackUrlInput,
                                        onValueChange = { feedbackUrlInput = it },
                                        label = { Text("Google Web App Feedback Submit Link (CSV/Form Post)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Button(
                                        onClick = {
                                            val updated = settings.copy(
                                                contactName = contactNameInput,
                                                contactWhatsApp = contactWhatsAppInput,
                                                contactFacebook = contactFacebookInput,
                                                feedbackUrl = feedbackUrlInput
                                            )
                                            AppConfigManager.saveSettings(context, updated)
                                            settings = updated
                                            Toast.makeText(context, "সামাজিক যোগাযোগ সেটিংস সফলভাবে সেভ করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                            onConfigUpdated()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("সামাজিক সেটিংস সেভ করুন", color = MaterialTheme.colorScheme.onPrimary)
                                    }
                                }
                            }

                            // Part D: Feedbacks List Messages
                            Text("ব্যবহারকারীদের পাঠানো বার্তা ও পরামর্শ তালিকা:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            feedbacksList.forEach { msg ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("প্রেরক: ${msg.senderName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            IconButton(
                                                onClick = {
                                                    val list = feedbacksList.toMutableList()
                                                    list.remove(msg)
                                                    AppConfigManager.saveFeedbacks(context, list)
                                                    feedbacksList = list
                                                    Toast.makeText(context, "বার্তাটি ডিলিট করা হয়েছে", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Text("যোগাযোগ: ${msg.contact}", fontSize = 11.sp, color = Color.Gray)
                                        Text("ধরণ: ${msg.requestType}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                        Text("বার্তা: ${msg.message}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("সময়: ${msg.timestamp}", fontSize = 9.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                    5 -> {
                        // BROWSER & BOOKMARKS SETTINGS PANEL
                        val scrollState5 = rememberScrollState()
                        var newBookmarkName by remember { mutableStateOf("") }
                        var newBookmarkUrl by remember { mutableStateOf("") }
                        var newBookmarkType by remember { mutableStateOf("LETTER") }
                        var newBookmarkLabel by remember { mutableStateOf("") }
                        var newBookmarkColor by remember { mutableStateOf("#95D5B2") }

                        val shortcutsList = remember(settings.browserShortcutsJson) {
                            AppConfigManager.parseShortcuts(settings.browserShortcutsJson)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState5)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Section: Ad Connection Status & Simulation Feedback
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "📡 Ad Network Connection Panel (বিজ্ঞাপন সংযোগ প্যানেল)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "এখানে আপনার বিজ্ঞাপন নেটওয়ার্কের কানেকশন স্ট্যাটাস চেক করুন।",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                                    // StartApp Indicator
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("StartApp Ad Network", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("App ID: ${if(settings.startappAppId.isBlank()) "সেট করা নেই" else settings.startappAppId}", fontSize = 10.sp, color = Color.Gray)
                                        }
                                        Surface(
                                            color = if (settings.startappAppId.isNotBlank()) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .background(if (settings.startappAppId.isNotBlank()) Color(0xFF4CAF50) else Color(0xFFF44336), CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (settings.startappAppId.isNotBlank()) "কানেক্ট হয়েছে (Active)" else "কানেক্ট হয়নি (Inactive)",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (settings.startappAppId.isNotBlank()) Color(0xFF2E7D32) else Color(0xFFC62828)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Monetag Indicator
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Monetag Ad Network", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("Zone ID: ${if(settings.monetagAdCode.isBlank()) "সেট করা নেই" else settings.monetagAdCode}", fontSize = 10.sp, color = Color.Gray)
                                        }
                                        Surface(
                                            color = if (settings.monetagAdCode.isNotBlank()) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .background(if (settings.monetagAdCode.isNotBlank()) Color(0xFF4CAF50) else Color(0xFFF44336), CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (settings.monetagAdCode.isNotBlank()) "কানেক্ট হয়েছে (Active)" else "কানেক্ট হয়নি (Inactive)",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (settings.monetagAdCode.isNotBlank()) Color(0xFF2E7D32) else Color(0xFFC62828)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Part A: Browser Enable/Disable Switch
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "ব্রাউজার ও বুকমার্ক সেটিংস",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("ফাস্ট ব্রাউজার ট্যাব চালু করুন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("নিচের নেভিগেশন বারে 'Browser' ট্যাবটি দৃশ্যমান হবে।", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Switch(
                                            checked = settings.browserEnabled,
                                            onCheckedChange = { checked ->
                                                val updated = settings.copy(browserEnabled = checked)
                                                AppConfigManager.saveSettings(context, updated)
                                                settings = updated
                                                Toast.makeText(context, "ব্রাউজার স্ট্যাটাস সেভ হয়েছে!", Toast.LENGTH_SHORT).show()
                                                onConfigUpdated()
                                            }
                                        )
                                    }
                                }
                            }

                            // Part B: Add new bookmark shortcut Form
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "নতুন বুকমার্ক শর্টকাট যুক্ত করুন",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    OutlinedTextField(
                                        value = newBookmarkName,
                                        onValueChange = { newBookmarkName = it },
                                        label = { Text("বুকমার্কের নাম (যেমন: Facebook)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = newBookmarkUrl,
                                        onValueChange = { newBookmarkUrl = it },
                                        label = { Text("বুকমার্ক লিংক (যেমন: https://facebook.com)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = newBookmarkLabel,
                                        onValueChange = { newBookmarkLabel = it },
                                        label = { Text("আইকন অক্ষর (LETTER মোডের জন্য যেমন: F)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // Type select
                                    Text("আইকন ডিজাইন ধরণ নির্বাচন করুন:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    val shortcutTypes = listOf("LETTER", "SWIRL", "DOWNLOAD", "PLAY", "SPEECH")
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        shortcutTypes.forEach { type ->
                                            val isSelected = newBookmarkType == type
                                            ElevatedFilterChip(
                                                selected = isSelected,
                                                onClick = { newBookmarkType = type },
                                                label = { Text(type, fontSize = 10.sp) }
                                            )
                                        }
                                    }

                                    // Color selector
                                    Text("আইকনের ব্যাকগ্রাউন্ড কালার হেক্স কোড:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(
                                        value = newBookmarkColor,
                                        onValueChange = { newBookmarkColor = it },
                                        label = { Text("কালার হেক্স কোড (যেমন: #95D5B2)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Button(
                                        onClick = {
                                            if (newBookmarkName.isBlank() || newBookmarkUrl.isBlank()) {
                                                Toast.makeText(context, "বুকমার্কের নাম ও লিংক অবশ্যই দিতে হবে।", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            val currentList = shortcutsList.toMutableList()
                                            currentList.add(
                                                BrowserShortcut(
                                                    name = newBookmarkName,
                                                    url = newBookmarkUrl,
                                                    colorHex = newBookmarkColor.ifBlank { "#95D5B2" },
                                                    type = newBookmarkType,
                                                    label = newBookmarkLabel.ifBlank { newBookmarkName.take(1).uppercase() }
                                                )
                                            )
                                            val updatedJson = AppConfigManager.serializeShortcuts(currentList)
                                            val updated = settings.copy(browserShortcutsJson = updatedJson)
                                            AppConfigManager.saveSettings(context, updated)
                                            settings = updated
                                            newBookmarkName = ""
                                            newBookmarkUrl = ""
                                            newBookmarkLabel = ""
                                            Toast.makeText(context, "নতুন বুকমার্ক সফলভাবে সংরক্ষণ করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                            onConfigUpdated()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("বুকমার্ক যোগ করুন", color = Color.White)
                                    }
                                }
                            }

                            // Part C: List existing custom bookmarks
                            Text("বিদ্যমান বুকমার্ক তালিকা:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (shortcutsList.isEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text(
                                        text = "কোন কাস্টম বুকমার্ক যোগ করা হয়নি। ডিফল্ট ১২টি শর্টকাট ব্রাউজারে প্রদর্শিত হচ্ছে।",
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(16.dp),
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                shortcutsList.forEach { shortcut ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(try { Color(android.graphics.Color.parseColor(shortcut.colorHex)) } catch(e: Exception) { Color.Gray }),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(shortcut.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                }
                                                Column {
                                                    Text(shortcut.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text(shortcut.url, fontSize = 10.sp, color = Color.Gray)
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    val currentList = shortcutsList.toMutableList()
                                                    currentList.remove(shortcut)
                                                    val updatedJson = AppConfigManager.serializeShortcuts(currentList)
                                                    val updated = settings.copy(browserShortcutsJson = updatedJson)
                                                    AppConfigManager.saveSettings(context, updated)
                                                    settings = updated
                                                    Toast.makeText(context, "বুকমার্ক মুছে ফেলা হয়েছে।", Toast.LENGTH_SHORT).show()
                                                    onConfigUpdated()
                                                }
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    6 -> {
                        // NEW ADVANCED ADS MANAGER DASHBOARD
                        val scrollState = rememberScrollState()
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val isWide = maxWidth > 650.dp
                            if (isWide) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        AdPanelLeftContent(
                                            personalAdEnabled = personalAdEnabledInput,
                                            onPersonalAdEnabledChange = { personalAdEnabledInput = it },
                                            personalAdImageUrl = personalAdImageUrlInput,
                                            onPersonalAdImageUrlChange = { personalAdImageUrlInput = it },
                                            personalAdClickUrl = personalAdClickUrlInput,
                                            onPersonalAdClickChange = { personalAdClickUrlInput = it },
                                            personalAdVideoUrl = personalAdVideoUrlInput,
                                            onPersonalAdVideoChange = { personalAdVideoUrlInput = it }
                                        )
                                    }
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        AdPanelRightContent(
                                            context = context,
                                            settings = settings,
                                            adsEnabled = adsEnabledInput,
                                            onAdsEnabledChange = { adsEnabledInput = it },
                                            backAdEnabled = backAdEnabledInput,
                                            onBackAdEnabledChange = { backAdEnabledInput = it },
                                            activeNetwork = settings.adNetwork,
                                            onNetworkChange = { net ->
                                                val updated = settings.copy(adNetwork = net)
                                                AppConfigManager.saveSettings(context, updated)
                                                settings = updated
                                                onConfigUpdated()
                                            },
                                            monetagAdCode = monetagAdCodeInput,
                                            onMonetagAdCodeChange = { monetagAdCodeInput = it },
                                            startappAppId = startappAppIdInput,
                                            onStartappAppIdChange = { startappAppIdInput = it },
                                            onSave = {
                                                val updated = settings.copy(
                                                    adsEnabled = adsEnabledInput,
                                                    backAdEnabled = backAdEnabledInput,
                                                    personalAdEnabled = personalAdEnabledInput,
                                                    personalAdImageUrl = personalAdImageUrlInput,
                                                    personalAdClickUrl = personalAdClickUrlInput,
                                                    personalAdVideoUrl = personalAdVideoUrlInput,
                                                    monetagAdCode = monetagAdCodeInput,
                                                    startappAppId = startappAppIdInput
                                                )
                                                AppConfigManager.saveSettings(context, updated)
                                                settings = updated
                                                Toast.makeText(context, "বিজ্ঞাপন সেটিংস সংরক্ষণ ও কানেক্ট হয়েছে!", Toast.LENGTH_SHORT).show()
                                                onConfigUpdated()
                                            }
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(scrollState)
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    AdPanelLeftContent(
                                        personalAdEnabled = personalAdEnabledInput,
                                        onPersonalAdEnabledChange = { personalAdEnabledInput = it },
                                        personalAdImageUrl = personalAdImageUrlInput,
                                        onPersonalAdImageUrlChange = { personalAdImageUrlInput = it },
                                        personalAdClickUrl = personalAdClickUrlInput,
                                        onPersonalAdClickChange = { personalAdClickUrlInput = it },
                                        personalAdVideoUrl = personalAdVideoUrlInput,
                                        onPersonalAdVideoChange = { personalAdVideoUrlInput = it }
                                    )
                                    AdPanelRightContent(
                                        context = context,
                                        settings = settings,
                                        adsEnabled = adsEnabledInput,
                                        onAdsEnabledChange = { adsEnabledInput = it },
                                        backAdEnabled = backAdEnabledInput,
                                        onBackAdEnabledChange = { backAdEnabledInput = it },
                                        activeNetwork = settings.adNetwork,
                                        onNetworkChange = { net ->
                                            val updated = settings.copy(adNetwork = net)
                                            AppConfigManager.saveSettings(context, updated)
                                            settings = updated
                                            onConfigUpdated()
                                        },
                                        monetagAdCode = monetagAdCodeInput,
                                        onMonetagAdCodeChange = { monetagAdCodeInput = it },
                                        startappAppId = startappAppIdInput,
                                        onStartappAppIdChange = { startappAppIdInput = it },
                                        onSave = {
                                            val updated = settings.copy(
                                                adsEnabled = adsEnabledInput,
                                                backAdEnabled = backAdEnabledInput,
                                                personalAdEnabled = personalAdEnabledInput,
                                                personalAdImageUrl = personalAdImageUrlInput,
                                                personalAdClickUrl = personalAdClickUrlInput,
                                                personalAdVideoUrl = personalAdVideoUrlInput,
                                                monetagAdCode = monetagAdCodeInput,
                                                startappAppId = startappAppIdInput
                                            )
                                            AppConfigManager.saveSettings(context, updated)
                                            settings = updated
                                            Toast.makeText(context, "বিজ্ঞাপন সেটিংস সংরক্ষণ ও কানেক্ট হয়েছে!", Toast.LENGTH_SHORT).show()
                                            onConfigUpdated()
                                        }
                                    )
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    // Edit Watch Button Dialog
    editingButton?.let { btn ->
        var editName by remember(btn.id) { mutableStateOf(btn.name) }
        var editUrl by remember(btn.id) { mutableStateOf(btn.url) }
        var editAdsEnabled by remember(btn.id) { mutableStateOf(btn.adsEnabled) }
        var editIsEnabled by remember(btn.id) { mutableStateOf(btn.isEnabled) }

        AlertDialog(
            onDismissRequest = { editingButton = null },
            title = { Text("ওয়াচ বাটন এডিট করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("বাটন নাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editUrl,
                        onValueChange = { editUrl = it },
                        label = { Text("ভিডিও বা স্ট্রিম লিংক") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("বাটন चालू (On)", fontSize = 13.sp)
                        Switch(
                            checked = editIsEnabled,
                            onCheckedChange = { editIsEnabled = it }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("বিজ্ঞাপন দেখাবে (Ads)", fontSize = 13.sp)
                        Switch(
                            checked = editAdsEnabled,
                            onCheckedChange = { editAdsEnabled = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isBlank() || editUrl.isBlank()) {
                            Toast.makeText(context, "বাটন নাম ও লিংক দিন", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val list = buttons.map {
                            if (it.id == btn.id) {
                                it.copy(
                                    name = editName,
                                    url = editUrl,
                                    adsEnabled = editAdsEnabled,
                                    isEnabled = editIsEnabled
                                )
                            } else it
                        }
                        AppConfigManager.saveButtons(context, list)
                        buttons = list
                        editingButton = null
                        Toast.makeText(context, "বাটন সফলভাবে আপডেট হয়েছে!", Toast.LENGTH_SHORT).show()
                        onConfigUpdated()
                    }
                ) {
                    Text("সেভ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingButton = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Edit Premium Item Dialog
    editingPremiumItem?.let { item ->
        var editName by remember(item.id) { mutableStateOf(item.name) }
        var editUrl by remember(item.id) { mutableStateOf(item.url) }
        var editCategory by remember(item.id) { mutableStateOf(item.category) }

        val categoriesList = listOf("Premium APK", "Courses", "Books", "Movies")

        AlertDialog(
            onDismissRequest = { editingPremiumItem = null },
            title = { Text("প্রিমিয়াম লিংক এডিট করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("আইটেম নাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editUrl,
                        onValueChange = { editUrl = it },
                        label = { Text("ডাউনলোড বা ভিজিট লিংক") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("ক্যাটাগরি সিলেক্ট করুন:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Column {
                        categoriesList.forEach { cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { editCategory = cat }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = editCategory == cat,
                                    onClick = { editCategory = cat }
                                )
                                Text(cat, fontSize = 13.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isBlank() || editUrl.isBlank()) {
                            Toast.makeText(context, "নাম ও লিংক দিন", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val list = premiumItemsList.map {
                            if (it.id == item.id) {
                                it.copy(
                                    name = editName,
                                    url = editUrl,
                                    category = editCategory
                                )
                            } else it
                        }
                        AppConfigManager.savePremiumItems(context, list)
                        premiumItemsList = list
                        editingPremiumItem = null
                        Toast.makeText(context, "আইটেম সফলভাবে আপডেট হয়েছে!", Toast.LENGTH_SHORT).show()
                        onConfigUpdated()
                    }
                ) {
                    Text("সেভ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPremiumItem = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

// Helper function to serialize selected channels to M3U
fun serializeChannelsToM3u(channels: List<IptvChannel>): String {
    val sb = StringBuilder()
    sb.append("#EXTM3U\n")
    for (ch in channels) {
        sb.append("#EXTINF:-1 tvg-logo=\"${ch.logoUrl}\" group-title=\"${ch.group}\",${ch.name}\n")
        sb.append("${ch.streamUrl}\n")
    }
    return sb.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdPanelLeftContent(
    personalAdEnabled: Boolean,
    onPersonalAdEnabledChange: (Boolean) -> Unit,
    personalAdImageUrl: String,
    onPersonalAdImageUrlChange: (String) -> Unit,
    personalAdClickUrl: String,
    onPersonalAdClickChange: (String) -> Unit,
    personalAdVideoUrl: String,
    onPersonalAdVideoChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "ব্যক্তিগত বিজ্ঞাপন ও ফাইল সেটিংস (Personal Ads)",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("ব্যক্তিগত বিজ্ঞাপন দেখান (Personal Ads)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("নেটওয়ার্ক বিজ্ঞাপনের জায়গায় নিজের ব্যানার ও ভিডিও অ্যাড দেখান", fontSize = 11.sp, color = Color.Gray)
                }
                Switch(
                    checked = personalAdEnabled,
                    onCheckedChange = onPersonalAdEnabledChange
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = personalAdImageUrl,
                onValueChange = onPersonalAdImageUrlChange,
                label = { Text("ব্যক্তিগত বিজ্ঞাপন ব্যানার ইমেজ URL (পিকচার ফাইল)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://example.com/ad-image.png") }
            )

            OutlinedTextField(
                value = personalAdClickUrl,
                onValueChange = onPersonalAdClickChange,
                label = { Text("বিজ্ঞাপনে ক্লিক করলে যে লিংকে নিয়ে যাবে (Redirect URL)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://my-website.com") }
            )

            OutlinedTextField(
                value = personalAdVideoUrl,
                onValueChange = onPersonalAdVideoChange,
                label = { Text("ব্যক্তিগত ভিডিও বিজ্ঞাপনের MP4 URL (অপশনাল)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://example.com/video-ad.mp4") }
            )

            if (personalAdImageUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("বিজ্ঞাপন ব্যানার প্রিভিউ (Ad Preview):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = personalAdImageUrl,
                            contentDescription = "Ad Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdPanelRightContent(
    context: android.content.Context,
    settings: com.example.data.AppSettings,
    adsEnabled: Boolean,
    onAdsEnabledChange: (Boolean) -> Unit,
    backAdEnabled: Boolean,
    onBackAdEnabledChange: (Boolean) -> Unit,
    activeNetwork: String,
    onNetworkChange: (String) -> Unit,
    monetagAdCode: String,
    onMonetagAdCodeChange: (String) -> Unit,
    startappAppId: String,
    onStartappAppIdChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "নেটওয়ার্ক আইডি ও লাইভ কানেকশন স্ট্যাটাস",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("বিজ্ঞাপন দেখান (Enable Global Ads)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("অ্যাপের সকল জায়গায় বিজ্ঞাপন লোডিং সচল রাখুন", fontSize = 11.sp, color = Color.Gray)
                }
                Switch(
                    checked = adsEnabled,
                    onCheckedChange = onAdsEnabledChange
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("ব্যাক বাটনে বিজ্ঞাপন দেখান", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("ভিডিও প্লেয়ার থেকে ব্যাক করলে বিজ্ঞাপন দেখাবে", fontSize = 11.sp, color = Color.Gray)
                }
                Switch(
                    checked = backAdEnabled,
                    onCheckedChange = onBackAdEnabledChange
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text("অ্যাক্টিভ বিজ্ঞাপন নেটওয়ার্ক নির্বাচন করুন (Active Network):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("monetag", "startapp").forEach { net ->
                    val isSelected = activeNetwork == net
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNetworkChange(net) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = net.uppercase(),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = monetagAdCode,
                onValueChange = onMonetagAdCodeChange,
                label = { Text("Monetag Ad Placement Code / Tag ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. 1234567") }
            )

            OutlinedTextField(
                value = startappAppId,
                onValueChange = onStartappAppIdChange,
                label = { Text("StartApp SDK App ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. 204561722") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("লাইভ বিজ্ঞাপন কানেকশন ট্র্যাকার (Ad Sync Live Tracker):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isMonetagConnected = monetagAdCode.isNotBlank() && adsEnabled
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Monetag Connection Status", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (isMonetagConnected) Color(0xFF00E676) else Color(0xFFFF1744), CircleShape)
                            )
                            Text(
                                text = if (isMonetagConnected) "কানেক্ট হয়েছে (CONNECTED)" else "কানেক্ট হয়নি (OFF)",
                                fontSize = 11.sp,
                                color = if (isMonetagConnected) Color(0xFF2E7D32) else Color(0xFFC62828),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val isStartAppConnected = startappAppId.isNotBlank() && adsEnabled
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("StartApp Connection Status", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (isStartAppConnected) Color(0xFF00E676) else Color(0xFFFF1744), CircleShape)
                            )
                            Text(
                                text = if (isStartAppConnected) "কানেক্ট হয়েছে (CONNECTED)" else "কানেক্ট হয়নি (OFF)",
                                fontSize = 11.sp,
                                color = if (isStartAppConnected) Color(0xFF2E7D32) else Color(0xFFC62828),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val isPersonalConnected = settings.personalAdEnabled && settings.personalAdImageUrl.isNotBlank()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Personal Campaign Status", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (isPersonalConnected) Color(0xFF00E676) else Color(0xFF757575), CircleShape)
                            )
                            Text(
                                text = if (isPersonalConnected) "ক্যাম্পেইন সচল (ACTIVE)" else "বন্ধ রয়েছে (INACTIVE)",
                                fontSize = 11.sp,
                                color = if (isPersonalConnected) Color(0xFF2E7D32) else Color(0xFF757575),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = Color.White)
                    Text("সেভ এবং বিজ্ঞাপন কানেক্ট করুন", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}