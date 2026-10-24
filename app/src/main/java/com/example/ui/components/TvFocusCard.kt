package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Channel
import com.example.data.model.ChannelType

/**
 * Converts direct Wikimedia .svg URLs (e.g. Love Nature) into raster .svg.png URLs
 * so Android decodes them natively without requiring an SVG library.
 */
private fun normalizePosterUrl(rawUrl: String?): String? {
    val trimmed = rawUrl?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
        ?: return null

    if (trimmed.contains("upload.wikimedia.org/wikipedia/commons/") &&
        !trimmed.contains("/thumb/") &&
        trimmed.substringBefore("?").endsWith(".svg", ignoreCase = true)
    ) {
        val clean = trimmed.substringBefore("?")
        val afterCommons = clean.substringAfter("upload.wikimedia.org/wikipedia/commons/")
        val fileName = clean.substringAfterLast("/")
        return "https://upload.wikimedia.org/wikipedia/commons/thumb/$afterCommons/500px-$fileName.png"
    }
    return trimmed
}

@Composable
fun TvFocusCard(
    channel: Channel,
    isSelected: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        label = "card_scale"
    )

    val borderColor = when {
        isFocused -> Color(0xFFFF9800) // Vibrant TV focus amber/brown
        isSelected -> Color(0xFF00E676) // Currently playing green
        else -> Color(0x33FFFFFF)
    }

    val borderWidth = if (isFocused) 3.dp else if (isSelected) 2.dp else 1.dp

    val primaryPoster = remember(channel.poster) { normalizePosterUrl(channel.poster) }
    val ytFallbackThumb = remember(channel.streamUrl, channel.type) {
        if (channel.type == ChannelType.YOUTUBE) {
            channel.getYouTubeVideoId()?.takeIf { it.isNotEmpty() }?.let { vid ->
                "https://i.ytimg.com/vi/$vid/hqdefault.jpg"
            }
        } else null
    }

    var primaryFailed by remember(channel.id, primaryPoster) { mutableStateOf(false) }
    var fallbackFailed by remember(channel.id, ytFallbackThumb) { mutableStateOf(false) }

    val showingPrimaryLogo = primaryPoster != null && !primaryFailed
    val showingYtFallback = !showingPrimaryLogo && ytFallbackThumb != null && !fallbackFailed

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF162E27) else Color(0xFF182220)
        ),
        border = BorderStroke(borderWidth, borderColor),
        modifier = modifier
            .scale(scale)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Poster / Logo container
            // Soft frosted slate-white canvas for logos ensures 100% visibility for dark/black transparent PNGs (e.g. Independent TV)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (showingPrimaryLogo) Color(0xFFEAEFED) else Color(0xFF0C1311)
                    )
            ) {
                when {
                    showingPrimaryLogo -> {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(primaryPoster)
                                .crossfade(true)
                                .build(),
                            contentDescription = channel.name,
                            contentScale = ContentScale.Fit,
                            onError = { primaryFailed = true },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp)
                        )
                    }
                    showingYtFallback -> {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(ytFallbackThumb)
                                .crossfade(true)
                                .build(),
                            contentDescription = channel.name,
                            contentScale = ContentScale.Crop,
                            onError = { fallbackFailed = true },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = "Channel Icon",
                            tint = if (channel.type == ChannelType.YOUTUBE) Color(0xFFFF5252) else Color(0xFF26A69A),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Channel badge (Live / IPTV)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (channel.type == ChannelType.YOUTUBE) Color(0xDDE53935) else Color(0xDD00897B),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                ) {
                    Text(
                        text = if (channel.type == ChannelType.YOUTUBE) "YT LIVE" else "IPTV",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // Favorite star button (touch clickable on mobile, non-focusable on TV D-pad so card focus stays 1-step)
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                        .focusProperties { canFocus = false }
                ) {
                    Icon(
                        imageVector = if (channel.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (channel.isFavorite) Color(0xFFFFD600) else if (showingPrimaryLogo) Color(0x9937474F) else Color(0x88FFFFFF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Channel Title
            Text(
                text = channel.name,
                color = if (isSelected) Color(0xFF00E676) else Color.White,
                fontSize = 13.sp,
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Group / Category subtext (shown for M3U playlists)
            if (channel.type == ChannelType.M3U && channel.groupTitle.isNotEmpty()) {
                Text(
                    text = channel.groupTitle,
                    color = Color(0xFF88A29B),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
