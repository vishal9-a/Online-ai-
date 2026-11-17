package com.example.data.model

import java.util.UUID

enum class AnimationStyle(
    val label: String,
    val tagline: String,
    val promptGuidance: String
) {
    CARTOON(
        label = "Cartoon",
        tagline = "Bold outlines & bouncy squash-stretch",
        promptGuidance = "2D Saturday-morning cartoon style: expressive thick ink outlines, squash-and-stretch bouncy easing, vibrant cel-shaded colors, exaggerated poses, parallax sky/foreground."
    ),
    FLAT(
        label = "Flat",
        tagline = "Clean vector shapes & silky motion",
        promptGuidance = "Modern flat vector motion-graphics style: clean geometric shapes without heavy black outlines, layered gradients, crisp silhouettes, smooth cubic-bezier easing, tasteful particle accents."
    ),
    PIXEL(
        label = "Pixel",
        tagline = "Retro 8-bit grid sprites & arcade vibe",
        promptGuidance = "Retro 8-bit/16-bit pixel art style: crisp blocky pixel-grid sprites rendered on Canvas with imageSmoothingEnabled=false, stepped frame animations, dithered sky, arcade HUD sparkles."
    ),
    DOODLE(
        label = "Doodle",
        tagline = "Sketchbook ink lines & lively wobble",
        promptGuidance = "Hand-drawn sketchbook doodle style: textured dark-paper or warm-cream notebook background, jittery double-stroked ink lines (boil effect every ~120ms), marker highlighter fills, playful arrows and stars."
    )
}

enum class DurationPreset(val label: String, val seconds: Int) {
    SEC_15("15s", 15),
    SEC_30("30s", 30),
    MIN_1("1 min", 60),
    MIN_3("3 min", 180),
    MIN_5("5 min", 300),
    CUSTOM("Custom", 45)
}

enum class SceneStatus {
    PENDING,
    GENERATING,
    READY,
    ERROR
}

data class SceneItem(
    val id: String = UUID.randomUUID().toString(),
    val sceneNo: Int,
    val durationSec: Int,
    val description: String,
    val narration: String,
    val htmlCode: String = "",
    val status: SceneStatus = SceneStatus.PENDING,
    val errorMessage: String? = null
)

data class ToonProject(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Untitled Toon",
    val prompt: String = "",
    val targetDurationSec: Int = 30,
    val style: AnimationStyle = AnimationStyle.CARTOON,
    val styleGuide: String = "",
    val characterSheet: String = "",
    val scenes: List<SceneItem> = emptyList(),
    val captionsEnabled: Boolean = true,
    val voiceoverEnabled: Boolean = false,
    val voiceSpeed: Float = 1.0f,
    val musicFilePath: String? = null,
    val musicTitle: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val readyCount: Int get() = scenes.count { it.status == SceneStatus.READY }
    val totalDurationSec: Int get() = scenes.sumOf { it.durationSec }
}

data class StoryStarter(
    val chipLabel: String,
    val fullPrompt: String,
    val suggestedStyle: AnimationStyle,
    val suggestedSeconds: Int
)

val DEFAULT_STORY_STARTERS = listOf(
    StoryStarter(
        chipLabel = "Chaiwala on Mars",
        fullPrompt = "Ek desi chaiwala Raju Mars pe apni flying tapri kholta hai, aur alien astronauts cutting chai peene line laga dete hain.",
        suggestedStyle = AnimationStyle.CARTOON,
        suggestedSeconds = 30
    ),
    StoryStarter(
        chipLabel = "Barish & Paper Boat",
        fullPrompt = "Mumbai ki pehli barish mein ek choti ladki Ananya apni glowing paper boat chhodti hai jo secret underground river se hote hue samundar tak jaati hai.",
        suggestedStyle = AnimationStyle.FLAT,
        suggestedSeconds = 30
    ),
    StoryStarter(
        chipLabel = "Pixel Cricket Hero",
        fullPrompt = "Gully cricket ke final over mein chota pixel hero Bablu ko 6 balls pe 18 runs chahiye, aur uska lucky wooden bat jaadu dikhata hai.",
        suggestedStyle = AnimationStyle.PIXEL,
        suggestedSeconds = 15
    ),
    StoryStarter(
        chipLabel = "Hostel Ghost 404",
        fullPrompt = "Late night exam prep ke beech hostel room 404 mein ek friendly doodle bhoot aata hai aur chai aur notes banane mein help karta hai.",
        suggestedStyle = AnimationStyle.DOODLE,
        suggestedSeconds = 30
    )
)

sealed interface GenerationState {
    data object Idle : GenerationState
    data class SplittingStory(val message: String) : GenerationState
    data class GeneratingScenes(
        val currentScene: Int,
        val totalScenes: Int,
        val statusText: String
    ) : GenerationState
    data class Paused(
        val completedCount: Int,
        val totalScenes: Int,
        val reason: String,
        val isQuotaLimit: Boolean
    ) : GenerationState
    data class Completed(val totalScenes: Int) : GenerationState
    data class Error(val message: String) : GenerationState
}

sealed interface ExportState {
    data object Idle : ExportState
    data class Preparing(val message: String) : ExportState
    data class Recording(
        val sceneIndex: Int,
        val totalScenes: Int,
        val progressFraction: Float,
        val formatLabel: String
    ) : ExportState
    data class Completed(
        val filePath: String,
        val fileName: String,
        val formatLabel: String,
        val savedToGallery: Boolean
    ) : ExportState
    data class Error(val message: String) : ExportState
}
