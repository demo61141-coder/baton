package com.example.ui.screens

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
