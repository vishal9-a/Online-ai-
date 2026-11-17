package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.example.data.model.ExportState
import com.example.ui.theme.CyberMint
import com.example.ui.theme.ElectricCoral
import com.example.ui.theme.FriendlyRose
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioSlate
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ExportVideoDialog(
    exportState: ExportState,
    captionsEnabled: Boolean,
    onStartExport: (WebView) -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onClose: () -> Unit
) {
    var captureWebView by remember { mutableStateOf<WebView?>(null) }

    Dialog(onDismissRequest = {
        if (exportState !is ExportState.Recording && exportState !is ExportState.Preparing) {
            onClose()
        }
    }) {
        Surface(
            color = StudioSlate,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, StudioOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Movie,
                        contentDescription = "Export Video",
                        tint = ElectricCoral,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Export Animated Video",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }

                // Live 16:9 Capture Stage attached to the Dialog window
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(StudioObsidian)
                ) {
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
                                }
                                captureWebView = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (exportState is ExportState.Idle) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(StudioObsidian.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (captionsEnabled) {
                                    "WebM/MP4 video captions ke saath record hoga"
                                } else {
                                    "Clean 2D animation video record hoga (no captions)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                when (exportState) {
                    is ExportState.Idle -> {
                        Text(
                            text = "Har ready scene smooth fade transitions ke saath record kiya jayega. Recording mein lagbhag video jitna time lagega.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onClose) {
                                Text("Cancel")
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { captureWebView?.let(onStartExport) },
                                enabled = captureWebView != null,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCoral,
                                    contentColor = StudioObsidian
                                ),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("start_recording_button")
                            ) {
                                Text("Start Recording")
                            }
                        }
                    }

                    is ExportState.Preparing -> {
                        Text(
                            text = exportState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        LinearProgressIndicator(
                            color = ElectricCoral,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    is ExportState.Recording -> {
                        val pct = (exportState.progressFraction * 100f).roundToInt()
                        Text(
                            text = "Recording Scene ${exportState.sceneIndex}/${exportState.totalScenes} (${exportState.formatLabel}) • $pct%",
                            style = MaterialTheme.typography.labelLarge,
                            color = CyberMint
                        )
                        LinearProgressIndicator(
                            progress = { exportState.progressFraction },
                            color = CyberMint,
                            trackColor = StudioObsidian,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50))
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onClose) {
                                Text("Stop & Cancel", color = FriendlyRose)
                            }
                        }
                    }

                    is ExportState.Completed -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Export ready",
                                tint = CyberMint,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${exportState.formatLabel} ready: ${exportState.fileName}",
                                style = MaterialTheme.typography.titleSmall,
                                color = CyberMint
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onDownload,
                                enabled = !exportState.savedToGallery,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberMint,
                                    contentColor = StudioObsidian
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("download_video_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = "Download video",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (exportState.savedToGallery) "Saved!" else "Download")
                            }

                            OutlinedButton(
                                onClick = onShare,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = "Share video",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Share")
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onClose) {
                                Text("Done")
                            }
                        }
                    }

                    is ExportState.Error -> {
                        Text(
                            text = exportState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = FriendlyRose
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onClose) {
                                Text("Close")
                            }
                        }
                    }
                }
            }
        }
    }
}
