package com.example.data

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class WatchButton(
    val id: Int,
    val name: String,
    val url: String,
    val iconName: String = "play_circle",
    val isEnabled: Boolean = true,
    val adsEnabled: Boolean = true,
    val customImageBase64: String? = null,
    val emoji: String? = null
)

data class BrowserShortcut(
    val name: String,
    val url: String,
    val colorHex: String = "#FF95D5B2",
    val type: String = "LETTER", // LETTER, SWIRL, DOWNLOAD, PLAY, SPEECH
    val label: String = ""
)

data class CustomMenuItem(
    val name: String,
    val url: String,
    val iconName: String = "star"
)

data class AppBackup(
    val settings: AppSettings,
    val buttons: List<WatchButton>,
    val premiumItems: List<PremiumItem>
)

data class AppSettings(
    val adsEnabled: Boolean = true,
    val adNetwork: String = "monetag", // monetag or startapp
    val backAdEnabled: Boolean = true,
    val sheetId: String = "", // Retrieve dynamically from GitHub config.json; no longer hardcoded
    val githubConfigUrl: String = "https://raw.githubusercontent.com/demo61141-coder/baton/main/config.json", // Raw URL to the GitHub config.json
    val lastNotificationTitle: String = "Welcome!",
    val lastNotificationBody: String = "Thanks for joining All Live streams.",
    val sponsorText: String = "SPONSORED BY MONETAG",
    val browserEnabled: Boolean = true,
    val browserHomeUrl: String = "https://www.google.com",
    
    // Security PINs
    val pin1: String = "1234",
    val pin2: String = "123",
    val pin3: String = "5678",
    val securityQuestion: String = "Secret Keyword?",
    val securityAnswer: String = "hasan",
    
    // Contact Settings
    val contactName: String = "Md Hasan Khalifa",
    val contactWhatsApp: String = "+8801798088609",
    val contactFacebook: String = "https://www.facebook.com/HasanKhalifa01",
    
    // Feedback endpoint URL
    val feedbackUrl: String = "",

    // Donation Settings
    val donationEnabled: Boolean = true,
    val donationNumber: String = "+8801798088609",
    val donationInstructions: String = "বিকাশ বা নগদ পার্সোনাল নাম্বারে Send Money করুন। তারপর নিচের ফর্মে সঠিক ট্রানজেকশন আইডি দিয়ে সাবমিট করুন।",
    val donationPackages: String = "১ দিন - ১০ টাকা\n৩০ দিন - ২৫০ টাকা\n১ বছর - ১৫০০ টাকা",

    // Customizable Watch Stream title/description
    val watchVisitText: String = "Watch Live Streams",
    val watchVisitDesc: String = "Click on any server video below to watch a short ad and visit our premium stream links.",

    // IPTV player controls
    val iptvEnabled: Boolean = true,
    val iptvPlaylistUrl: String = "https://raw.githubusercontent.com/Fribb/iptv-channels/master/iptv/playlists/playlist_singapore.m3u",
    val iptvSecondaryUrl: String = "https://iptv-org.github.io/iptv/categories/news.m3u",
    val iptvFileContent: String = "", // raw M3U channels string or pasted channels
    val iptvServer1Enabled: Boolean = true,
    val iptvServer2Enabled: Boolean = true,
    val iptvPendingFileContent: String = "", // M3U content loaded for admin preview before active

    // Three dot config
    val showThreeDotAdDelaySeconds: Int = 5,

    // Personal Ad Settings
    val personalAdEnabled: Boolean = false,
    val personalAdImageUrl: String = "https://images.unsplash.com/photo-1611162617213-7d7a39e9b1d7?w=500",
    val personalAdClickUrl: String = "https://www.google.com",
    val personalAdVideoUrl: String = "",
    val monetagAdCode: String = "",
    val startappAppId: String = "",

    // Browser Custom Shortcuts & Engines
    val browserShortcutsJson: String = "", // List<BrowserShortcut> as JSON
    val searchEngineUrl: String = "https://www.google.com/search?q=", // browser active search engine URL
    val customFeaturesJson: String = "", // List<CustomMenuItem> as JSON
    val splashLogoBase64: String? = null // custom Base64 string for splash screen logo
)

data class PremiumItem(
    val id: String,
    val name: String,
    val url: String,
    val category: String // "Premium APK", "Premium Course", "Premium Book", "New Premium Movie"
)

data class FeedbackItem(
    val id: String,
    val senderName: String,
    val contact: String,
    val requestType: String,
    val message: String,
    val timestamp: String
)

data class DonationItem(
    val id: String,
    val senderName: String,
    val senderPhone: String,
    val transactionId: String,
    val amountOrPackage: String,
    val paymentMethod: String, // "Bkash" / "Nagad" / "Rocket"
    val timestamp: String,
    var status: String, // "Pending", "Approved", "Declined"
    var expirationDate: String // e.g. "Expired", "1 Day", "1 Month", "1 Year", "Permanent"
)

object AppConfigManager {
    private const val PREFS_NAME = "all_live_prefs"
    private const val KEY_SETTINGS = "app_settings"
    private const val KEY_BUTTONS = "watch_buttons"
    private const val KEY_PREMIUM_ITEMS = "premium_items"
    private const val KEY_FEEDBACKS = "feedbacks"
    private const val KEY_DONATIONS = "donations"

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    // Default buttons fallback in case Google Sheets is empty or offline
    val defaultButtons = listOf(
        WatchButton(1, "Watch 1 - Sports Hub", "https://news.google.com", "sports"),
        WatchButton(2, "Watch 2 - Global Stream", "https://m.youtube.com", "tv"),
        WatchButton(3, "Watch 3 - Movies & Shows", "https://www.imdb.com", "movie"),
        WatchButton(4, "Watch 4 - Live News Tracker", "https://m.wikipedia.org", "news")
    )

    // Default premium items
    val defaultPremiumItems = listOf(
        PremiumItem("p1", "Inshot Pro MOD APK (Unlocked)", "https://inshot.com", "Premium APK"),
        PremiumItem("p2", "CapCut Pro Video Editor", "https://capcut.com", "Premium APK"),
        PremiumItem("p3", "Full Android App Development Course", "https://coursera.org", "Courses"),
        PremiumItem("p4", "Rich Dad Poor Dad PDF", "https://books.google.com", "Books"),
        PremiumItem("p5", "Awesome Action Movie Stream", "https://netflix.com", "Movies")
    )

    fun saveSettings(context: Context, settings: AppSettings) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val adapter = moshi.adapter(AppSettings::class.java)
            val json = adapter.toJson(settings)
            prefs.edit().putString(KEY_SETTINGS, json).apply()
        } catch (e: Exception) {
            Log.e("AppConfigManager", "Failed to save settings", e)
        }
    }

    fun loadSettings(context: Context): AppSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_SETTINGS, null) ?: return AppSettings()
        return try {
            val adapter = moshi.adapter(AppSettings::class.java)
            adapter.fromJson(json) ?: AppSettings()
        } catch (e: Exception) {
            AppSettings()
        }
    }

    fun saveButtons(context: Context, buttons: List<WatchButton>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val type = Types.newParameterizedType(List::class.java, WatchButton::class.java)
            val adapter = moshi.adapter<List<WatchButton>>(type)
            val json = adapter.toJson(buttons)
            prefs.edit().putString(KEY_BUTTONS, json).apply()
        } catch (e: Exception) {
            Log.e("AppConfigManager", "Failed to save buttons", e)
        }
    }

    fun loadButtons(context: Context): List<WatchButton> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_BUTTONS, null) ?: return defaultButtons
        return try {
            val type = Types.newParameterizedType(List::class.java, WatchButton::class.java)
            val adapter = moshi.adapter<List<WatchButton>>(type)
            val list = adapter.fromJson(json)
            if (list.isNullOrEmpty()) defaultButtons else list
        } catch (e: Exception) {
            defaultButtons
        }
    }

    fun savePremiumItems(context: Context, items: List<PremiumItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val type = Types.newParameterizedType(List::class.java, PremiumItem::class.java)
            val adapter = moshi.adapter<List<PremiumItem>>(type)
            val json = adapter.toJson(items)
            prefs.edit().putString(KEY_PREMIUM_ITEMS, json).apply()
        } catch (e: Exception) {
            Log.e("AppConfigManager", "Failed to save premium items", e)
        }
    }

    fun loadPremiumItems(context: Context): List<PremiumItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PREMIUM_ITEMS, null) ?: return defaultPremiumItems
        return try {
            val type = Types.newParameterizedType(List::class.java, PremiumItem::class.java)
            val adapter = moshi.adapter<List<PremiumItem>>(type)
            val list = adapter.fromJson(json)
            if (list == null) defaultPremiumItems else list
        } catch (e: Exception) {
            defaultPremiumItems
        }
    }

    fun saveFeedbacks(context: Context, feedbacks: List<FeedbackItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val type = Types.newParameterizedType(List::class.java, FeedbackItem::class.java)
            val adapter = moshi.adapter<List<FeedbackItem>>(type)
            val json = adapter.toJson(feedbacks)
            prefs.edit().putString(KEY_FEEDBACKS, json).apply()
        } catch (e: Exception) {
            Log.e("AppConfigManager", "Failed to save feedbacks", e)
        }
    }

    fun loadFeedbacks(context: Context): List<FeedbackItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_FEEDBACKS, null) ?: return emptyList()
        return try {
            val type = Types.newParameterizedType(List::class.java, FeedbackItem::class.java)
            val adapter = moshi.adapter<List<FeedbackItem>>(type)
            val list = adapter.fromJson(json)
            if (list == null) emptyList() else list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveDonations(context: Context, donations: List<DonationItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val type = Types.newParameterizedType(List::class.java, DonationItem::class.java)
            val adapter = moshi.adapter<List<DonationItem>>(type)
            val json = adapter.toJson(donations)
            prefs.edit().putString(KEY_DONATIONS, json).apply()
        } catch (e: Exception) {
            Log.e("AppConfigManager", "Failed to save donations", e)
        }
    }

    fun loadDonations(context: Context): List<DonationItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_DONATIONS, null) ?: return emptyList()
        return try {
            val type = Types.newParameterizedType(List::class.java, DonationItem::class.java)
            val adapter = moshi.adapter<List<DonationItem>>(type)
            val list = adapter.fromJson(json)
            if (list == null) emptyList() else list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun exportBackup(context: Context): String {
        return try {
            val backup = AppBackup(
                settings = loadSettings(context),
                buttons = loadButtons(context),
                premiumItems = loadPremiumItems(context)
            )
            val adapter = moshi.adapter(AppBackup::class.java)
            adapter.toJson(backup)
        } catch (e: Exception) {
            Log.e("AppConfigManager", "Failed to export backup", e)
            ""
        }
    }

    fun importBackup(context: Context, jsonStr: String): Boolean {
        return try {
            val adapter = moshi.adapter(AppBackup::class.java)
            val backup = adapter.fromJson(jsonStr) ?: return false
            saveSettings(context, backup.settings)
            saveButtons(context, backup.buttons)
            savePremiumItems(context, backup.premiumItems)
            true
        } catch (e: Exception) {
            Log.e("AppConfigManager", "Failed to import backup", e)
            false
        }
    }

    fun parseShortcuts(json: String): List<BrowserShortcut> {
        if (json.isBlank()) return emptyList()
        return try {
            val type = Types.newParameterizedType(List::class.java, BrowserShortcut::class.java)
            val adapter = moshi.adapter<List<BrowserShortcut>>(type)
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializeShortcuts(shortcuts: List<BrowserShortcut>): String {
        return try {
            val type = Types.newParameterizedType(List::class.java, BrowserShortcut::class.java)
            val adapter = moshi.adapter<List<BrowserShortcut>>(type)
            adapter.toJson(shortcuts)
        } catch (e: Exception) {
            ""
        }
    }

    fun parseCustomMenuItems(json: String): List<CustomMenuItem> {
        if (json.isBlank()) return emptyList()
        return try {
            val type = Types.newParameterizedType(List::class.java, CustomMenuItem::class.java)
            val adapter = moshi.adapter<List<CustomMenuItem>>(type)
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializeCustomMenuItems(items: List<CustomMenuItem>): String {
        return try {
            val type = Types.newParameterizedType(List::class.java, CustomMenuItem::class.java)
            val adapter = moshi.adapter<List<CustomMenuItem>>(type)
            adapter.toJson(items)
        } catch (e: Exception) {
            ""
        }
    }
}
