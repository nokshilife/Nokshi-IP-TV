package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Playlist

@Composable
fun PlaylistSettingsDialog(
    isBuiltInPinVerified: Boolean,
    playlists: List<Playlist>,
    isBackgroundAudioEnabled: Boolean,
    isBatteryUnrestricted: Boolean = false,
    isLoading: Boolean,
    statusMessage: String?,
    onLoadBuiltInM3u: () -> Unit,
    onLoadCustomM3u: (url: String, name: String) -> Unit,
    onLoadXtream: (server: String, user: String, pass: String) -> Unit,
    onDeletePlaylist: (id: Long) -> Unit,
    onToggleBackgroundAudio: (Boolean) -> Unit,
    onRequestBatteryUnrestricted: () -> Unit = {},
    onOpenSleepTimer: () -> Unit,
    onForceServerCheck: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("বিল্ট-ইন M3U", "কাস্টম M3U", "Xtream Codes", "অন্যান্য")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF141E1C),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFFB8CFC9),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "প্লেলিস্ট ও সেটিংস",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    TvNeutralButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("বন্ধ করুন")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs (Neutral when unfocused; subtle active indicator; amber ONLY when D-pad focused)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isActive = selectedTab == index
                        TvNeutralButton(
                            onClick = { selectedTab = index },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isActive) "• $title" else title,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!statusMessage.isNullOrEmpty()) {
                    Text(
                        text = statusMessage,
                        color = Color(0xFF76FF03),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Tab Content
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> BuiltInTab(
                            isVerified = isBuiltInPinVerified,
                            isLoading = isLoading,
                            onLoad = onLoadBuiltInM3u
                        )
                        1 -> CustomM3uTab(
                            playlists = playlists,
                            isLoading = isLoading,
                            onLoadCustom = onLoadCustomM3u,
                            onDelete = onDeletePlaylist
                        )
                        2 -> XtreamTab(
                            isLoading = isLoading,
                            onConnect = onLoadXtream
                        )
                        3 -> GeneralSettingsTab(
                            isBackgroundAudioEnabled = isBackgroundAudioEnabled,
                            isBatteryUnrestricted = isBatteryUnrestricted,
                            onToggleBackgroundAudio = onToggleBackgroundAudio,
                            onRequestBatteryUnrestricted = onRequestBatteryUnrestricted,
                            onOpenSleepTimer = onOpenSleepTimer,
                            onForceServerCheck = onForceServerCheck
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BuiltInTab(
    isVerified: Boolean,
    isLoading: Boolean,
    onLoad: () -> Unit
) {
    Column(modifier = Modifier.padding(8.dp)) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2E2A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isVerified) Icons.Default.CheckCircle else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isVerified) Color(0xFF76FF03) else Color(0xFFB8CFC9)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isVerified) "স্ট্যাটাস: আনলকড (ভেরিফাইড)" else "স্ট্যাটাস: ওয়ান-টাইম পিন সুরক্ষিত",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Nokshi IP TV-র অফিশিয়াল বিল্ট-ইন M3U প্লেলিস্ট (বাংলা, স্পোর্টস, ইসলামিক, মুভিজ, ইত্যাদিসহ)।",
                    color = Color(0xFFB0BEC5),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                TvNeutralButton(
                    onClick = onLoad,
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    } else {
                        Text(text = if (isVerified) "বিল্ট-ইন M3U লোড করুন" else "পিন ভেরিফাই করে লোড করুন")
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomM3uTab(
    playlists: List<Playlist>,
    isLoading: Boolean,
    onLoadCustom: (url: String, name: String) -> Unit,
    onDelete: (Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(8.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("প্লেলিস্টের নাম (যেমন: My IPTV)") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFFF9800),
                unfocusedBorderColor = Color(0xFF3E5751)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("M3U / M3U8 লিঙ্ক (URL)") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFFF9800),
                unfocusedBorderColor = Color(0xFF3E5751)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        TvNeutralButton(
            onClick = {
                if (url.isNotEmpty()) {
                    onLoadCustom(url, name.ifEmpty { "কাস্টম M3U" })
                    name = ""
                    url = ""
                }
            },
            enabled = !isLoading && url.isNotEmpty()
        ) {
            Text("প্লেলিস্ট যোগ ও লোড করুন")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "সংরক্ষিত কাস্টম প্লেলিস্ট:",
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(playlists) { pl ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2825)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = pl.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = pl.url.ifEmpty { pl.serverUrl ?: "" }, color = Color(0xFF80CBC4), fontSize = 11.sp, maxLines = 1)
                        }
                        TvNeutralButton(
                            onClick = { onDelete(pl.id) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun XtreamTab(
    isLoading: Boolean,
    onConnect: (server: String, user: String, pass: String) -> Unit
) {
    var server by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(8.dp)) {
        OutlinedTextField(
            value = server,
            onValueChange = { server = it },
            label = { Text("Server URL (যেমন: http://server.com:8080)") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFFF9800),
                unfocusedBorderColor = Color(0xFF3E5751)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFFF9800),
                unfocusedBorderColor = Color(0xFF3E5751)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFFF9800),
                unfocusedBorderColor = Color(0xFF3E5751)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        TvNeutralButton(
            onClick = {
                if (server.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
                    onConnect(server, username, password)
                }
            },
            enabled = !isLoading && server.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()
        ) {
            Text("Xtream সংযোগ করুন")
        }
    }
}

@Composable
private fun GeneralSettingsTab(
    isBackgroundAudioEnabled: Boolean,
    isBatteryUnrestricted: Boolean,
    onToggleBackgroundAudio: (Boolean) -> Unit,
    onRequestBatteryUnrestricted: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onForceServerCheck: () -> Unit
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(8.dp)
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2E2A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ব্যাটারি অপ্টিমাইজেশন (Unrestricted)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (isBatteryUnrestricted) {
                            "স্ট্যাটাস: Unrestricted সক্রিয় (ব্যাকগ্রাউন্ড প্লেব্যাক সুরক্ষিত)"
                        } else {
                            "স্ট্যাটাস: অপ্টিমাইজড (ব্যাকগ্রাউন্ডে বন্ধ হওয়া রোধে Unrestricted করুন)"
                        },
                        color = if (isBatteryUnrestricted) Color(0xFF76FF03) else Color(0xFF80CBC4),
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                TvNeutralButton(onClick = onRequestBatteryUnrestricted) {
                    Text(if (isBatteryUnrestricted) "যাচাই করুন" else "Unrestricted করুন")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2E2A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "মোবাইলে ব্যাকগ্রাউন্ড প্লেব্যাক (M3U)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "স্ক্রিন বন্ধ বা অ্যাপ ব্যাকগ্রাউন্ডে থাকলেও M3U স্ট্রিম সচল থাকবে।", color = Color(0xFF80CBC4), fontSize = 12.sp)
                }
                Switch(
                    checked = isBackgroundAudioEnabled,
                    onCheckedChange = onToggleBackgroundAudio,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFE4F0EC),
                        checkedTrackColor = Color(0xFF3E5E55)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2E2A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "সার্ভার চেক ও চ্যানেল আপডেট", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "সার্ভার তারিখ যাচাই ও check_youtube_live.php চালান।", color = Color(0xFF80CBC4), fontSize = 12.sp)
                }
                TvNeutralButton(onClick = onForceServerCheck) {
                    Text("রান করুন")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2E2A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "স্লিপ টাইমার", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "নির্দিষ্ট সময় পর অ্যাপ স্বয়ংক্রিয়ভাবে বন্ধ করার সময়সূচি।", color = Color(0xFF80CBC4), fontSize = 12.sp)
                }
                TvNeutralButton(onClick = onOpenSleepTimer) {
                    Text("সেট করুন")
                }
            }
        }
    }
}
