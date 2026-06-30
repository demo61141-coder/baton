package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AppSettings
import com.example.data.IptvChannel
import com.example.data.IptvParser
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IptvScreen(
    settings: AppSettings,
    onChannelSelected: (IptvChannel) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var isLoading by remember { mutableStateOf(false) }
    var channels by remember { mutableStateOf<List<IptvChannel>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf("All") }
    
    // Choose which source to load: 1 = Primary URL, 2 = Secondary URL, 3 = Local File Content
    var activeSourceIndex by remember { mutableStateOf(1) }

    val loadChannels = {
        isLoading = true
        coroutineScope.launch {
            try {
                val list = when (activeSourceIndex) {
                    1 -> {
                        val url = settings.iptvPlaylistUrl.ifBlank { "https://raw.githubusercontent.com/Fribb/iptv-channels/master/iptv/playlists/playlist_singapore.m3u" }
                        IptvParser.fetchFromUrl(url)
                    }
                    2 -> {
                        val url = settings.iptvSecondaryUrl.ifBlank { "https://iptv-org.github.io/iptv/categories/news.m3u" }
                        IptvParser.fetchFromUrl(url)
                    }
                    else -> {
                        if (settings.iptvFileContent.isNotBlank()) {
                            IptvParser.parseM3u(settings.iptvFileContent)
                        } else {
                            emptyList()
                        }
                    }
                }
                channels = list
                if (list.isEmpty()) {
                    Toast.makeText(context, "কোন চ্যানেল খুঁজে পাওয়া যায়নি!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "${list.size} টি চ্যানেল লোড হয়েছে!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "চ্যানেল লোড করতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(activeSourceIndex, settings) {
        loadChannels()
    }

    val groups = remember(channels) {
        listOf("All") + channels.map { it.group }.distinct().filter { it.isNotBlank() }.sorted()
    }

    val filteredChannels = remember(channels, searchQuery, selectedGroup) {
        channels.filter { channel ->
            val matchQuery = channel.name.contains(searchQuery, ignoreCase = true)
            val matchGroup = selectedGroup == "All" || channel.group == selectedGroup
            matchQuery && matchGroup
        }
    }

    // Sleek and modern neon gradients
    val mainGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F0C1B), Color(0xFF05050A))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(mainGradient)
            .padding(16.dp)
            .systemBarsPadding()
    ) {
        // App Title & Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Live IPTV Player",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "সারাদিন সরাসরি লাইভ স্ট্রিম ও স্যাটেলাইট টিভি দেখুন",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            IconButton(
                onClick = { loadChannels() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh channels", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live source tabs (Primary, Secondary, Local/Custom)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf("Primary Server", "Secondary Server", "Imported M3U")
            tabs.forEachIndexed { index, title ->
                val active = activeSourceIndex == index + 1
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) Color(0xFF6750A4) else Color.Transparent)
                        .clickable { activeSourceIndex = index + 1 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (active) Color.White else Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar with Sleek Neon glow borders
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("চ্যানেল সার্চ করুন...", color = Color.White.copy(alpha = 0.4f)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon", tint = Color.White.copy(alpha = 0.6f)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search", tint = Color.White.copy(alpha = 0.6f))
                    }
                }
            },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00F2FE),
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                focusedContainerColor = Color.White.copy(alpha = 0.04f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.04f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("iptv_search_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Group selection horizontal scroll list
        if (groups.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("ক্যাটাগরি:", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                }
                
                // Show a couple of categories or a simplified selector to keep page clean
                val displayGroups = groups.take(4)
                displayGroups.forEach { grp ->
                    val isSel = selectedGroup == grp
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) Color(0xFF00F2FE).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f))
                            .border(1.dp, if (isSel) Color(0xFF00F2FE) else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { selectedGroup = grp }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = grp,
                            color = if (isSel) Color(0xFF00F2FE) else Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Channels layout
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (isLoading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF00F2FE))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("সার্ভার থেকে লাইভ টিভি লোড হচ্ছে...", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            } else if (filteredChannels.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "No channels icon",
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "কোন লাইভ চ্যানেল পাওয়া যায়নি। অনুগ্রহ করে এডমিন প্যানেল থেকে IPTV URL বা ফাইল সঠিক দিন।",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredChannels) { channel ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clickable { onChannelSelected(channel) }
                                .testTag("iptv_channel_card_${channel.name}"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.1f), Color.Transparent))
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Logo holder
                                if (channel.logoUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = channel.logoUrl,
                                        contentDescription = channel.name,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.1f))
                                            .padding(4.dp)
                                    )
                                } else {
                                    // Elegant fallback icon
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(Color(0xFF6750A4), Color(0xFF3F51B5))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = channel.name.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 20.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = channel.name,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    if (channel.group.isNotBlank()) {
                                        Text(
                                            text = channel.group,
                                            color = Color(0xFF00F2FE),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center
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
}
