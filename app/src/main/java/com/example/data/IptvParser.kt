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
                if (trimmed.startsWith("#EXTINF")) {
                    // Extract name (last part after comma)
                    val commaIndex = trimmed.lastIndexOf(',')
                    currentName = if (commaIndex != -1 && commaIndex < trimmed.length - 1) {
                        trimmed.substring(commaIndex + 1).trim()
                    } else {
                        "Unknown Channel"
                    }
                    
                    // Extract tvg-logo (quoted or unquoted)
                    val logoRegexQuoted = """tvg-logo=["']([^"']+)["']""".toRegex()
                    val logoRegexUnquoted = """tvg-logo=([^"\s,]+)""".toRegex()
                    val logoMatch = logoRegexQuoted.find(trimmed) ?: logoRegexUnquoted.find(trimmed)
                    currentLogo = logoMatch?.groupValues?.get(1) ?: ""
                    
                    // Extract group-title (quoted or unquoted)
                    val groupRegexQuoted = """group-title=["']([^"']+)["']""".toRegex()
                    val groupRegexUnquoted = """group-title=([^"\s,]+)""".toRegex()
                    val groupMatch = groupRegexQuoted.find(trimmed) ?: groupRegexUnquoted.find(trimmed)
                    currentGroup = groupMatch?.groupValues?.get(1) ?: "All"
                } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                    if (currentName.isNotEmpty()) {
                        // Split group by semicolons or commas to support multiple categories nicely
                        val groups = currentGroup.split(';', ',').map { it.trim() }.filter { it.isNotBlank() }
                        if (groups.isNotEmpty()) {
                            for (g in groups) {
                                channels.add(
                                    IptvChannel(
                                        name = currentName,
                                        logoUrl = currentLogo,
                                        streamUrl = trimmed,
                                        group = g
                                    )
                                )
                            }
                        } else {
                            channels.add(
                                IptvChannel(
                                    name = currentName,
                                    logoUrl = currentLogo,
                                    streamUrl = trimmed,
                                    group = "All"
                                )
                            )
                        }
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
