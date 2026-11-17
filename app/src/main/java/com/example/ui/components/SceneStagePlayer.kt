package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.ClosedCaptionOff
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MovieCreation
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VideoCall
import androidx.compose.material.icons.rounded.VoiceOverOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.SceneItem
import com.example.data.model.SceneStatus
import com.example.ui.theme.CyberMint
import com.example.ui.theme.ElectricCoral
import com.example.ui.theme.FriendlyRose
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioSlate
import com.example.ui.theme.TextSecondary
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SceneStagePlayer(
    scene: SceneItem?,
    totalScenes: Int,
    totalDurationSec: Int,
    isPlayingAll: Boolean,
    fadeAlpha: Float,
    playbackElapsedSec: Int,
    captionsEnabled: Boolean,
    voiceoverEnabled: Boolean,
    voiceSpeed: Float,
    musicTitle: String?,
    hasReadyScenes: Boolean,
    onTogglePlayAll: () -> Unit,
    onToggleCaptions: () -> Unit,
    onToggleVoiceover: () -> Unit,
    onCycleVoiceSpeed: () -> Unit,
    onPickMusic: () -> Unit,
    onRemoveMusic: () -> Unit,
    onOpenExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedFade by animateFloatAsState(
        targetValue = fadeAlpha,
        animationSpec = tween(durationMillis = 180),
        label = "scene_fade"
    )
    var loadedSignature by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth()) {
        // 16:9 Sandboxed Stage Frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(20.dp))
                .background(StudioObsidian)
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            if (isPlayingAll) CyberMint else ElectricCoral.copy(alpha = 0.7f),
                            StudioOutline
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            if (scene != null && scene.status == SceneStatus.READY && scene.htmlCode.isNotBlank()) {
                val signature = "${scene.id}_${scene.htmlCode.hashCode()}"
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            setBackgroundColor(android.graphics.Color.parseColor("#0B0D13"))
                            webViewClient = WebViewClient()
                            settings.apply {
                                javaScriptEnabled = true
                                allowFileAccess = false
                                allowContentAccess = false
                                blockNetworkLoads = true
                                domStorageEnabled = false
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                cacheMode = WebSettings.LOAD_NO_CACHE
                            }
                            isVerticalScrollBarEnabled = false
                            isHorizontalScrollBarEnabled = false
                        }
                    },
                    update = { webView ->
                        if (loadedSignature != signature) {
                            loadedSignature = signature
                            webView.loadDataWithBaseURL(
                                null,
                                scene.htmlCode,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                StagePlaceholder(scene = scene)
            }

            // Scene badge top-left
            if (scene != null) {
                Surface(
                    color = StudioObsidian.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, StudioOutline),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    when (scene.status) {
                                        SceneStatus.READY -> CyberMint
                                        SceneStatus.GENERATING -> ElectricCoral
                                        SceneStatus.ERROR -> FriendlyRose
                                        SceneStatus.PENDING -> TextSecondary
                                    }
                                )
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "SCENE ${scene.sceneNo}/$totalScenes • ${scene.durationSec}s",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }

            // Optional Narration Captions Overlay
            androidx.compose.animation.AnimatedVisibility(
                visible = captionsEnabled && scene != null && scene.narration.isNotBlank() && scene.status == SceneStatus.READY,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Surface(
                    color = StudioObsidian.copy(alpha = 0.82f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, StudioOutline.copy(alpha = 0.7f))
                ) {
                    Text(
                        text = scene?.narration.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Fade transition veil during Play All
            if (animatedFade > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(StudioObsidian.copy(alpha = animatedFade))
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Primary Transport Row: Play All + Timecode + Export
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onTogglePlayAll,
                enabled = hasReadyScenes,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPlayingAll) FriendlyRose else CyberMint,
                    contentColor = StudioObsidian
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("play_all_button")
            ) {
                Icon(
                    imageVector = if (isPlayingAll) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlayingAll) "Stop playback" else "Play all scenes",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isPlayingAll) "Stop" else "Play All",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Surface(
                color = StudioCard,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, StudioOutline),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "${formatSeconds(playbackElapsedSec)} / ${formatSeconds(totalDurationSec)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isPlayingAll) CyberMint else TextSecondary
                    )
                }
            }

            OutlinedButton(
                onClick = onOpenExport,
                enabled = hasReadyScenes,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (hasReadyScenes) ElectricCoral else StudioOutline),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ElectricCoral
                ),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("export_video_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.VideoCall,
                    contentDescription = "Export video",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Export",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Audio & Caption Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = captionsEnabled,
                onClick = onToggleCaptions,
                label = { Text("Captions") },
                leadingIcon = {
                    Icon(
                        imageVector = if (captionsEnabled) Icons.Rounded.ClosedCaption else Icons.Rounded.ClosedCaptionOff,
                        contentDescription = "Toggle captions",
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ElectricCoral.copy(alpha = 0.2f),
                    selectedLabelColor = ElectricCoral,
                    selectedLeadingIconColor = ElectricCoral
                ),
                modifier = Modifier.testTag("toggle_captions_chip")
            )

            FilterChip(
                selected = voiceoverEnabled,
                onClick = onToggleVoiceover,
                label = { Text("Voiceover") },
                leadingIcon = {
                    Icon(
                        imageVector = if (voiceoverEnabled) Icons.Rounded.RecordVoiceOver else Icons.Rounded.VoiceOverOff,
                        contentDescription = "Toggle voiceover",
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyberMint.copy(alpha = 0.18f),
                    selectedLabelColor = CyberMint,
                    selectedLeadingIconColor = CyberMint
                ),
                modifier = Modifier.testTag("toggle_voiceover_chip")
            )

            if (voiceoverEnabled) {
                FilterChip(
                    selected = true,
                    onClick = onCycleVoiceSpeed,
                    label = { Text("${voiceSpeed}x") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Speed,
                            contentDescription = "Voice speed",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            if (musicTitle == null) {
                FilterChip(
                    selected = false,
                    onClick = onPickMusic,
                    label = { Text("Add Music") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = "Upload background music",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("upload_music_chip")
                )
            } else {
                Surface(
                    color = StudioCard,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CyberMint.copy(alpha = 0.5f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 10.dp, end = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = "Background music",
                            tint = CyberMint,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = musicTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(100.dp)
                        )
                        IconButton(
                            onClick = onRemoveMusic,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Remove music",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StagePlaceholder(scene: SceneItem?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(StudioSlate, StudioObsidian)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(20.dp)
        ) {
            when (scene?.status) {
                SceneStatus.GENERATING -> {
                    CircularProgressIndicator(
                        color = ElectricCoral,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Scene ${scene.sceneNo} animate ho raha hai...",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                }
                SceneStatus.ERROR -> {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = "Scene error",
                        tint = FriendlyRose,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = scene.errorMessage ?: "Scene generate nahi ho paaya. Regenerate dabao!",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 3
                    )
                }
                SceneStatus.PENDING -> {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "Waiting to animate",
                        tint = ElectricCoral,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Scene ${scene.sceneNo} queue mein hai...",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Rounded.MovieCreation,
                        contentDescription = "Empty stage",
                        tint = TextSecondary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Storyboard ready hone par yahan 2D animation chalega",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

fun formatSeconds(totalSec: Int): String {
    val safe = totalSec.coerceAtLeast(0)
    val mins = safe / 60
    val secs = safe % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
