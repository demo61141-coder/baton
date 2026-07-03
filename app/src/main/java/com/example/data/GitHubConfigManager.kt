package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GitHubConfigManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Fetches the config.json from GitHub and parses the Spreadsheet ID.
     * Returns the spreadsheetId if successful, null otherwise.
     */
    suspend fun fetchSpreadsheetIdFromGitHub(url: String): String? {
        if (url.isBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("GitHubConfigManager", "Failed to download config: HTTP ${response.code}")
                        return@withContext null
                    }
                    val bodyString = response.body?.string() ?: return@withContext null
                    val jsonObject = JSONObject(bodyString)
                    
                    var sheetId: String? = null
                    if (jsonObject.has("spreadsheetId")) {
                        sheetId = jsonObject.getString("spreadsheetId")
                    } else if (jsonObject.has("sheetId")) {
                        sheetId = jsonObject.getString("sheetId")
                    }
                    
                    if (!sheetId.isNullOrBlank()) {
                        sheetId.trim()
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("GitHubConfigManager", "Error fetching config.json from GitHub", e)
                null
            }
        }
    }

    /**
     * Fetches config.json from GitHub first to get the spreadsheet ID,
     * updates the local AppSettings, then performs Google Sheets sync.
     * If GitHub is unreachable, falls back to the last successfully saved spreadsheet ID in local storage.
     */
    suspend fun fetchAndSyncWithGitHub(context: Context, forceUrl: String? = null): Boolean {
        val settings = AppConfigManager.loadSettings(context)
        val urlToUse = forceUrl ?: settings.githubConfigUrl
        
        Log.d("GitHubConfigManager", "Fetching spreadsheet ID from: $urlToUse")
        val fetchedSheetId = fetchSpreadsheetIdFromGitHub(urlToUse)
        
        val finalSheetId = if (!fetchedSheetId.isNullOrBlank()) {
            Log.d("GitHubConfigManager", "Successfully fetched Sheet ID from GitHub: $fetchedSheetId")
            // Save the newly retrieved sheetId to local settings
            val updatedSettings = settings.copy(sheetId = fetchedSheetId)
            AppConfigManager.saveSettings(context, updatedSettings)
            fetchedSheetId
        } else {
            Log.w("GitHubConfigManager", "Failed to fetch from GitHub. Falling back to cached local Sheet ID.")
            settings.sheetId
        }

        if (finalSheetId.isBlank()) {
            Log.e("GitHubConfigManager", "No sheet ID available (both GitHub and local storage are blank)")
            return false
        }

        // Trigger Google Sheets Sync
        return GoogleSheetsManager.fetchAndSyncConfig(context, finalSheetId)
    }
}
