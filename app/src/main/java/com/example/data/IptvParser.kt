package com.example.data

import java.io.BufferedReader
import java.io.StringReader
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class IptvChannel(
    val name: String,
    val logoUrl: String,
    val streamUrl: String,
    val group: String = "All"
)

object IptvParser {
    fun parseM3u(m3uContent: String): List<IptvChannel> {
        val channels = mutableListOf<IptvChannel>()
        try {
            val reader = BufferedReader(StringReader(m3uContent))
            var line = reader.readLine()
            var currentLogo = ""
            var currentName = ""
            var currentGroup = "All"
            
            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.startsWith("#EXTINF:")) {
                    // Extract name (last part after comma)
                    val commaIndex = trimmed.lastIndexOf(',')
                    currentName = if (commaIndex != -1 && commaIndex < trimmed.length - 1) {
                        trimmed.substring(commaIndex + 1).trim()
                    } else {
                        "Unknown Channel"
                    }
                    
                    // Extract tvg-logo
                    val logoRegex = """tvg-logo=["']([^"']+)["']""".toRegex()
                    val logoMatch = logoRegex.find(trimmed)
                    currentLogo = logoMatch?.groupValues?.get(1) ?: ""
                    
                    // Extract group-title
                    val groupRegex = """group-title=["']([^"']+)["']""".toRegex()
                    val groupMatch = groupRegex.find(trimmed)
                    currentGroup = groupMatch?.groupValues?.get(1) ?: "All"
                } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                    if (currentName.isNotEmpty()) {
                        channels.add(
                            IptvChannel(
                                name = currentName,
                                logoUrl = currentLogo,
                                streamUrl = trimmed,
                                group = currentGroup
                            )
                        )
                    }
                    // Reset
                    currentName = ""
                    currentLogo = ""
                    currentGroup = "All"
                }
                line = reader.readLine()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return channels
    }

    suspend fun fetchFromUrl(m3uUrl: String): List<IptvChannel> {
        return withContext(Dispatchers.IO) {
            try {
                val urlObj = URL(m3uUrl)
                val connection = urlObj.openConnection()
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                val stream = connection.getInputStream()
                val content = stream.bufferedReader().use { it.readText() }
                parseM3u(content)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
}
