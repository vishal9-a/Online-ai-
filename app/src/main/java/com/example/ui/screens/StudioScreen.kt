package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AnimationStyle
import com.example.data.model.DEFAULT_STORY_STARTERS
import com.example.data.model.DurationPreset
import com.example.data.model.GenerationState
import com.example.data.model.StoryStarter
import com.example.data.model.ToonProject
import com.example.ui.components.SceneStagePlayer
import com.example.ui.components.TimelineStrip
import com.example.ui.components.formatSeconds
import com.example.ui.theme.CyberMint
import com.example.ui.theme.ElectricCoral
import com.example.ui.theme.FriendlyRose
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.StudioGold
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioSlate
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun StudioScreen(
    project: ToonProject,
    selectedPreset: DurationPreset,
    selectedSceneIndex: Int,
    generationState: GenerationState,
    isComposerExpanded: Boolean,
    isPlayingAll: Boolean,
    fadeAlpha: Float,
    playbackElapsedSec: Int,
    onToggleComposer: (Boolean) -> Unit,
    onPromptChange: (String) -> Unit,
    onSelectDurationPreset: (DurationPreset) -> Unit,
    onCustomDurationChange: (Int) -> Unit,
    onSelectStyle: (AnimationStyle) -> Unit,
    onApplyStarter: (StoryStarter) -> Unit,
    onGenerateClick: () -> Unit,
    onPauseGeneration: () -> Unit,
    onResumeGeneration: () -> Unit,
    onNewProjectClick: () -> Unit,
    onSelectScene: (Int) -> Unit,
    onRegenerateScene: (Int) -> Unit,
    onMoveScene: (Int, Int) -> Unit,
    onDeleteScene: (Int) -> Unit,
    onUpdateScene: (Int, String, String, Int) -> Unit,
    onTogglePlayAll: () -> Unit,
    onToggleCaptions: () -> Unit,
    onToggleVoiceover: () -> Unit,
    onCycleVoiceSpeed: () -> Unit,
    onPickMusic: () -> Unit,
    onRemoveMusic: () -> Unit,
    onOpenExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isGenerating = generationState is GenerationState.SplittingStory ||
        generationState is GenerationState.GeneratingScenes
    var showBible by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Studio Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (project.scenes.isEmpty()) "PromptToon Studio" else project.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (project.scenes.isEmpty()) {
                            "Apni kahani likho, AI 2D cartoon banayega"
                        } else {
                            "${project.style.label} • ${project.readyCount}/${project.scenes.size} scenes • ${formatSeconds(project.totalDurationSec)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                if (project.scenes.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onNewProjectClick,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, StudioOutline),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "New story",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("New", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // Hero Artwork when starting fresh
            if (project.scenes.isEmpty() && isComposerExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(152.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(StudioSlate)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_studio_hero),
                        contentDescription = "PromptToon 2D animation studio banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        StudioObsidian.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )
                    Surface(
                        color = ElectricCoral.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "2D STORY-TO-VIDEO ENGINE",
                            style = MaterialTheme.typography.labelSmall,
                            color = StudioObsidian,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Collapsible Story Composer Card
            Surface(
                color = StudioSlate,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, StudioOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onToggleComposer(!isComposerExpanded) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "Story composer",
                                tint = ElectricCoral,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Story Prompt & Settings",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                        Icon(
                            imageVector = if (isComposerExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (isComposerExpanded) "Collapse composer" else "Expand composer",
                            tint = TextSecondary
                        )
                    }

                    AnimatedVisibility(visible = isComposerExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Hinglish Quick Starters
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DEFAULT_STORY_STARTERS.forEach { starter ->
                                    AssistChip(
                                        onClick = { onApplyStarter(starter) },
                                        label = {
                                            Text(
                                                text = starter.chipLabel,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = StudioCard,
                                            labelColor = StudioGold
                                        ),
                                        border = BorderStroke(1.dp, StudioOutline)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = project.prompt,
                                onValueChange = onPromptChange,
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.prompt_hint),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextMuted
                                    )
                                },
                                minLines = 3,
                                maxLines = 5,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricCoral,
                                    unfocusedBorderColor = StudioOutline,
                                    focusedContainerColor = StudioObsidian,
                                    unfocusedContainerColor = StudioObsidian
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("story_prompt_input")
                            )

                            // Duration Picker
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Target Video Length (${formatSeconds(project.targetDurationSec)})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DurationPreset.entries.forEach { preset ->
                                        val isSelected = selectedPreset == preset
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { onSelectDurationPreset(preset) },
                                            label = { Text(preset.label) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = ElectricCoral,
                                                selectedLabelColor = StudioObsidian,
                                                containerColor = StudioCard,
                                                labelColor = TextSecondary
                                            )
                                        )
                                    }
                                }
                                if (selectedPreset == DurationPreset.CUSTOM) {
                                    Slider(
                                        value = project.targetDurationSec.toFloat(),
                                        onValueChange = { onCustomDurationChange(it.roundToInt()) },
                                        valueRange = 10f..600f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = ElectricCoral,
                                            activeTrackColor = ElectricCoral
                                        )
                                    )
                                }
                            }

                            // Style Picker
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Animation Style",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AnimationStyle.entries.forEach { style ->
                                        val selected = project.style == style
                                        Surface(
                                            color = if (selected) ElectricCoral.copy(alpha = 0.16f) else StudioCard,
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(
                                                width = if (selected) 1.5.dp else 1.dp,
                                                color = if (selected) ElectricCoral else StudioOutline
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable { onSelectStyle(style) }
                                                .testTag("style_chip_${style.name.lowercase()}")
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = styleIcon(style),
                                                    contentDescription = style.label,
                                                    tint = if (selected) ElectricCoral else TextSecondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    text = style.label,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = if (selected) Color.White else TextSecondary,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = onGenerateClick,
                                enabled = !isGenerating,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCoral,
                                    contentColor = StudioObsidian
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("generate_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = "Generate animation",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (project.scenes.isEmpty()) {
                                        "Generate Storyboard & Animate"
                                    } else {
                                        "Regenerate Full Story"
                                    },
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }

            // Generation Progress / Pause-Resume Banner
            GenerationStatusBanner(
                state = generationState,
                onPause = onPauseGeneration,
                onResume = onResumeGeneration
            )

            // Main Stage + Timeline once scenes exist
            if (project.scenes.isNotEmpty()) {
                val activeScene = project.scenes.getOrNull(selectedSceneIndex)

                SceneStagePlayer(
                    scene = activeScene,
                    totalScenes = project.scenes.size,
                    totalDurationSec = project.totalDurationSec,
                    isPlayingAll = isPlayingAll,
                    fadeAlpha = fadeAlpha,
                    playbackElapsedSec = playbackElapsedSec,
                    captionsEnabled = project.captionsEnabled,
                    voiceoverEnabled = project.voiceoverEnabled,
                    voiceSpeed = project.voiceSpeed,
                    musicTitle = project.musicTitle,
                    hasReadyScenes = project.readyCount > 0,
                    onTogglePlayAll = onTogglePlayAll,
                    onToggleCaptions = onToggleCaptions,
                    onToggleVoiceover = onToggleVoiceover,
                    onCycleVoiceSpeed = onCycleVoiceSpeed,
                    onPickMusic = onPickMusic,
                    onRemoveMusic = onRemoveMusic,
                    onOpenExport = onOpenExport
                )

                TimelineStrip(
                    scenes = project.scenes,
                    selectedIndex = selectedSceneIndex,
                    isBusy = isGenerating || isPlayingAll,
                    onSelectScene = onSelectScene,
                    onRegenerateScene = onRegenerateScene,
                    onMoveScene = onMoveScene,
                    onDeleteScene = onDeleteScene,
                    onUpdateScene = onUpdateScene
                )

                // Style Guide & Character Sheet Consistency Card
                Surface(
                    color = StudioCard,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, StudioOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showBible = !showBible },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Palette,
                                    contentDescription = "Style guide",
                                    tint = StudioGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Style Guide & Character Sheet",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White
                                )
                            }
                            Icon(
                                imageVector = if (showBible) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = "Toggle style guide",
                                tint = TextSecondary
                            )
                        }
                        AnimatedVisibility(visible = showBible) {
                            Column(
                                modifier = Modifier.padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "STYLE GUIDE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StudioGold
                                )
                                Text(
                                    text = project.styleGuide,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "CHARACTER SHEET",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberMint
                                )
                                Text(
                                    text = project.characterSheet,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GenerationStatusBanner(
    state: GenerationState,
    onPause: () -> Unit,
    onResume: () -> Unit
) {
    when (state) {
        is GenerationState.Idle,
        is GenerationState.Completed -> Unit

        is GenerationState.SplittingStory -> {
            Surface(
                color = StudioCardElevated,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ElectricCoral.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                    LinearProgressIndicator(
                        color = ElectricCoral,
                        trackColor = StudioObsidian,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                    )
                }
            }
        }

        is GenerationState.GeneratingScenes -> {
            val fraction = if (state.totalScenes > 0) {
                state.currentScene.toFloat() / state.totalScenes.toFloat()
            } else 0f

            Surface(
                color = StudioCardElevated,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ElectricCoral.copy(alpha = 0.7f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("generation_progress_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Scene ${state.currentScene}/${state.totalScenes}",
                                style = MaterialTheme.typography.titleMedium,
                                color = ElectricCoral
                            )
                            Text(
                                text = state.statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        TextButton(
                            onClick = onPause,
                            modifier = Modifier.testTag("pause_generation_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Pause,
                                contentDescription = "Pause generation",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Pause")
                        }
                    }
                    LinearProgressIndicator(
                        progress = { fraction },
                        color = ElectricCoral,
                        trackColor = StudioObsidian,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                    )
                }
            }
        }

        is GenerationState.Paused -> {
            Surface(
                color = StudioCardElevated,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, StudioGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = "Generation paused",
                            tint = StudioGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Paused (${state.completedCount}/${state.totalScenes} ready)",
                            style = MaterialTheme.typography.titleSmall,
                            color = StudioGold
                        )
                    }
                    Text(
                        text = state.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                    Button(
                        onClick = onResume,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberMint,
                            contentColor = StudioObsidian
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("resume_generation_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Resume generation",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Resume Remaining Scenes")
                    }
                }
            }
        }

        is GenerationState.Error -> {
            Surface(
                color = FriendlyRose.copy(alpha = 0.14f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, FriendlyRose),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WarningAmber,
                        contentDescription = "Error",
                        tint = FriendlyRose,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun styleIcon(style: AnimationStyle): ImageVector = when (style) {
    AnimationStyle.CARTOON -> Icons.Rounded.SmartToy
    AnimationStyle.FLAT -> Icons.Rounded.Layers
    AnimationStyle.PIXEL -> Icons.Rounded.GridView
    AnimationStyle.DOODLE -> Icons.Rounded.Brush
}
