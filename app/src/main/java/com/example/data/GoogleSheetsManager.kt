package com.example.data

import android.content.Context
import android.util.Log
import com.example.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.StringReader
import java.util.concurrent.TimeUnit

object GoogleSheetsManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Helper to parse a single CSV line while respecting quoted commas
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (ch == '\"') {
                inQuotes = !inQuotes
            } else if (ch == ',' && !inQuotes) {
                result.add(current.toString().trim())
                current.setLength(0)
            } else {
                current.append(ch)
            }
            i++
        }
        result.add(current.toString().trim())
        return result
    }

    /**
     * Fetches configuration from a Google Sheet (using the Spreadsheet ID).
     * Returns true if fetch and sync succeeded, false otherwise.
     */
    suspend fun fetchAndSyncConfig(context: Context, sheetId: String): Boolean {
        if (sheetId.isBlank()) return false
        val url = "https://docs.google.com/spreadsheets/d/$sheetId/export?format=csv"

        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("GoogleSheetsManager", "Failed to download CSV: HTTP ${response.code}")
                        return@withContext false
                    }

                    val csvData = response.body?.string() ?: return@withContext false
                    if (csvData.contains("<!DOCTYPE html>") || csvData.contains("login.google.com")) {
                        Log.e("GoogleSheetsManager", "Downloaded HTML instead of CSV. Ensure spreadsheet sharing is set to 'Anyone with link can view'.")
                        return@withContext false
                    }

                    parseAndApplyCsv(context, csvData, sheetId)
                    true
                }
            } catch (e: Exception) {
                Log.e("GoogleSheetsManager", "Error fetching Google Sheets config", e)
                false
            }
        }
    }

    private fun parseAndApplyCsv(context: Context, csvData: String, sheetId: String) {
        val reader = BufferedReader(StringReader(csvData))
        var line: String?

        val currentSettings = AppConfigManager.loadSettings(context)
        val newButtons = mutableListOf<WatchButton>()
        val newPremiumItems = mutableListOf<PremiumItem>()
        var adsEnabled = currentSettings.adsEnabled
        var adNetwork = currentSettings.adNetwork
        var backAdEnabled = currentSettings.backAdEnabled
        var sponsorText = currentSettings.sponsorText
        var browserEnabled = currentSettings.browserEnabled
        var browserHomeUrl = currentSettings.browserHomeUrl
        var notificationTitle = ""
        var notificationBody = ""

        var buttonIndex = 1

        // Skip header if first line is header
        var isFirstLine = true

        while (reader.readLine().also { line = it } != null) {
            val currentLine = line ?: continue
            if (currentLine.isBlank()) continue

            val row = parseCsvLine(currentLine)
            if (row.isEmpty()) continue

            // If it is the first line, check if it is a header row
            if (isFirstLine) {
                isFirstLine = false
                if (row[0].equals("Type", ignoreCase = true) || row[0].equals("Id", ignoreCase = true)) {
                    continue // Skip header row
                }
            }

            val type = row.getOrNull(0)?.uppercase() ?: continue
            val key = row.getOrNull(1) ?: ""
            val value = row.getOrNull(2) ?: ""
            val extra = row.getOrNull(3) ?: ""

            when (type) {
                "BUTTON" -> {
                    if (key.isNotBlank() && value.isNotBlank()) {
                        val icon = if (extra.isNotBlank()) extra else "play_circle"
                        newButtons.add(
                            WatchButton(
                                id = buttonIndex++,
                                name = key,
                                url = value,
                                iconName = icon
                            )
                        )
                    }
                }
                "PREMIUM" -> {
                    if (key.isNotBlank() && value.isNotBlank()) {
                        val rawCat = if (extra.isNotBlank()) extra else "Premium APK"
                        val mappedCat = when {
                            rawCat.contains("APK", ignoreCase = true) -> "Premium APK"
                            rawCat.contains("Course", ignoreCase = true) || rawCat.contains("Class", ignoreCase = true) -> "Courses"
                            rawCat.contains("Book", ignoreCase = true) || rawCat.contains("PDF", ignoreCase = true) -> "Books"
                            rawCat.contains("Movie", ignoreCase = true) || rawCat.contains("Film", ignoreCase = true) || rawCat.contains("Video", ignoreCase = true) -> "Movies"
                            else -> {
                                val clean = rawCat.trim()
                                if (clean.equals("Premium APK", ignoreCase = true)) "Premium APK"
                                else if (clean.equals("Courses", ignoreCase = true)) "Courses"
                                else if (clean.equals("Books", ignoreCase = true)) "Books"
                                else if (clean.equals("Movies", ignoreCase = true)) "Movies"
                                else clean
                            }
                        }
                        newPremiumItems.add(
                            PremiumItem(
                                id = java.util.UUID.randomUUID().toString(),
                                name = key,
                                url = value,
                                category = mappedCat
                            )
                        )
                    }
                }
                "SETTING" -> {
                    when (key.uppercase()) {
                        "ADSENABLED" -> adsEnabled = value.lowercase() == "true" || value == "1"
                        "ADNETWORK" -> adNetwork = if (value.lowercase() == "startapp") "startapp" else "monetag"
                        "BACKADENABLED" -> backAdEnabled = value.lowercase() == "true" || value == "1"
                        "SPONSORTEXT" -> sponsorText = value
                        "BROWSERENABLED" -> browserEnabled = value.lowercase() == "true" || value == "1"
                        "BROWSERHOMEURL" -> browserHomeUrl = value
                    }
                }
                "NOTIFICATION" -> {
                    if (key.isNotBlank()) {
                        notificationTitle = key
                        notificationBody = value
                    }
                }
            }
        }

        // Apply buttons
        if (newButtons.isNotEmpty()) {
            AppConfigManager.saveButtons(context, newButtons)
        }

        // Apply premium items if found in Sheet
        if (newPremiumItems.isNotEmpty()) {
            AppConfigManager.savePremiumItems(context, newPremiumItems)
        }

        // Apply settings
        val updatedSettings = currentSettings.copy(
            adsEnabled = adsEnabled,
            adNetwork = adNetwork,
            backAdEnabled = backAdEnabled,
            sheetId = sheetId,
            sponsorText = sponsorText,
            browserEnabled = browserEnabled,
            browserHomeUrl = browserHomeUrl,
            lastNotificationTitle = if (notificationTitle.isNotBlank()) notificationTitle else currentSettings.lastNotificationTitle,
            lastNotificationBody = if (notificationBody.isNotBlank()) notificationBody else currentSettings.lastNotificationBody
        )
        AppConfigManager.saveSettings(context, updatedSettings)

        // Show a native notification if there is a new announcement
        if (notificationTitle.isNotBlank()) {
            NotificationHelper.sendNotification(context, notificationTitle, notificationBody)
        }
    }
}
