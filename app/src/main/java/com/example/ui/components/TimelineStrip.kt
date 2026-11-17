package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.SceneItem
import com.example.data.model.SceneStatus
import com.example.ui.theme.CyberMint
import com.example.ui.theme.ElectricCoral
import com.example.ui.theme.FriendlyRose
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.StudioGold
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun TimelineStrip(
    scenes: List<SceneItem>,
    selectedIndex: Int,
    isBusy: Boolean,
    onSelectScene: (Int) -> Unit,
    onRegenerateScene: (Int) -> Unit,
    onMoveScene: (Int, Int) -> Unit,
    onDeleteScene: (Int) -> Unit,
    onUpdateScene: (Int, String, String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Timeline Strip (${scenes.size} scenes)",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = "Tap any scene to preview",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        Spacer(Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("timeline_strip")
        ) {
            itemsIndexed(scenes, key = { _, item -> item.id }) { index, scene ->
                val isSelected = index == selectedIndex
                Surface(
                    color = if (isSelected) StudioCardElevated else StudioCard,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) ElectricCoral else StudioOutline
                    ),
                    modifier = Modifier
                        .width(168.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectScene(index) }
                        .testTag("scene_card_${scene.sceneNo}")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (scene.status == SceneStatus.GENERATING) {
                                    CircularProgressIndicator(
                                        color = ElectricCoral,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(12.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (scene.status) {
                                                    SceneStatus.READY -> CyberMint
                                                    SceneStatus.ERROR -> FriendlyRose
                                                    else -> TextSecondary
                                                }
                                            )
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "S${scene.sceneNo}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White
                                )
                            }

                            Surface(
                                color = StudioGold.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${scene.durationSec}s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StudioGold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = scene.narration.ifBlank { scene.description },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }
            }
        }

        // Action Bar for Selected Scene
        val selectedScene = scenes.getOrNull(selectedIndex)
        if (selectedScene != null) {
            Spacer(Modifier.height(10.dp))
            Surface(
                color = StudioCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, StudioOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Scene ${selectedScene.sceneNo}: ${selectedScene.description}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onMoveScene(selectedIndex, -1) },
                                enabled = !isBusy && selectedIndex > 0,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Move scene earlier"
                                )
                            }
                            IconButton(
                                onClick = { onMoveScene(selectedIndex, 1) },
                                enabled = !isBusy && selectedIndex < scenes.lastIndex,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = "Move scene later"
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { editingIndex = selectedIndex },
                                enabled = !isBusy,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Edit scene script"
                                )
                            }
                            IconButton(
                                onClick = { onRegenerateScene(selectedIndex) },
                                enabled = !isBusy,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("regenerate_scene_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Regenerate scene",
                                    tint = ElectricCoral
                                )
                            }
                            IconButton(
                                onClick = { onDeleteScene(selectedIndex) },
                                enabled = !isBusy && scenes.size > 1,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("delete_scene_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Delete scene",
                                    tint = FriendlyRose
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    editingIndex?.let { idx ->
        scenes.getOrNull(idx)?.let { scene ->
            EditSceneDialog(
                scene = scene,
                onDismiss = { editingIndex = null },
                onSave = { desc, narr, dur ->
                    onUpdateScene(idx, desc, narr, dur)
                    editingIndex = null
                }
            )
        }
    }
}

@Composable
private fun EditSceneDialog(
    scene: SceneItem,
    onDismiss: () -> Unit,
    onSave: (String, String, Int) -> Unit
) {
    var description by remember(scene.id) { mutableStateOf(scene.description) }
    var narration by remember(scene.id) { mutableStateOf(scene.narration) }
    var duration by remember(scene.id) { mutableFloatStateOf(scene.durationSec.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Scene ${scene.sceneNo}",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Visual Action") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = narration,
                    onValueChange = { narration = it },
                    label = { Text("Narration / Captions") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Column {
                    Text(
                        text = "Duration: ${duration.roundToInt()}s (5–10s)",
                        style = MaterialTheme.typography.labelMedium,
                        color = StudioGold
                    )
                    Slider(
                        value = duration,
                        onValueChange = { duration = it },
                        valueRange = 5f..10f,
                        steps = 4
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(description, narration, duration.roundToInt()) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
