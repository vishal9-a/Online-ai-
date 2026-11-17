package com.example.data.ai

import android.util.Log
import com.example.data.model.AnimationStyle
import com.example.data.model.SceneItem
import com.example.data.model.SceneStatus
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.math.roundToInt

private const val TAG = "GeminiStoryboard"

data class StoryPlanResult(
    val title: String,
    val styleGuide: String,
    val characterSheet: String,
    val scenes: List<SceneItem>
)

class GeminiStoryboardService {

    private val storyPlanSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Short catchy title for the animated story (2-5 words)"),
            "style_guide" to Schema.string(description = "Consistent visual style guide: exact hex color palette (sky, ground, accents), line thickness, lighting, shading rules."),
            "character_sheet" to Schema.string(description = "Detailed visual specs for every recurring character: name, proportions, hair/hat, outfit colors with hex codes, signature prop."),
            "scenes" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "scene_no" to Schema.integer(description = "1-based sequential scene index"),
                        "duration_sec" to Schema.integer(description = "Scene duration in seconds between 5 and 10 inclusive"),
                        "description" to Schema.string(description = "Detailed visual choreography: camera framing, background, character action, and motion from start to end of the scene."),
                        "narration" to Schema.string(description = "Short expressive narration or dialogue line for voiceover/captions (1-2 sentences, matching story language).")
                    )
                ),
                description = "Ordered list of scenes, each 5 to 10 seconds, summing close to the requested target duration."
            )
        )
    )

    private val storyPlannerModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = storyPlanSchema
                temperature = 0.85f
            }
        )
    }

    private val sceneAnimatorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.7f
            }
        )
    }

    suspend fun planStory(
        prompt: String,
        targetDurationSec: Int,
        style: AnimationStyle
    ): StoryPlanResult {
        val avgSceneSec = 7
        val targetSceneCount = (targetDurationSec.toFloat() / avgSceneSec).roundToInt().coerceIn(2, 45)
        val systemPrompt = buildString {
            appendLine("You are an award-winning 2D animation director and storyboard artist.")
            appendLine("Split the user's story prompt into an animated storyboard.")
            appendLine("Story prompt: \"$prompt\"")
            appendLine("Target total duration: $targetDurationSec seconds (aim for about $targetSceneCount scenes).")
            appendLine("Visual Style: ${style.label} — ${style.promptGuidance}")
            appendLine("Rules:")
            appendLine("1. Every scene MUST have duration_sec between 5 and 10 seconds.")
            appendLine("2. Sum of all scene durations should be close to $targetDurationSec seconds.")
            appendLine("3. Create a super concrete style_guide (exact hex colors, sky/ground mood, line weight) and character_sheet (exact colors, outfits, shapes) so every scene draws the exact same characters.")
            appendLine("4. If the story prompt uses Hinglish or Hindi-English mix, keep narration fun and natural in matching Hinglish/English.")
            appendLine("5. Make each scene's description vivid with clear continuous 2D movement (walking, flying, bouncing, weather, camera pan) easy to animate on HTML5 Canvas.")
        }

        val response = withTimeout(60_000L) {
            storyPlannerModel.generateContent(systemPrompt)
        }
        val jsonText = response.text
            ?: throw IllegalStateException("Gemini ne empty storyboard return kiya. Dobara try karo!")

        val root = JSONObject(jsonText)
        val title = root.optString("title", "PromptToon Story").ifBlank { "PromptToon Story" }
        val styleGuide = root.optString("style_guide", style.promptGuidance)
        val characterSheet = root.optString("character_sheet", "Expressive stylized lead character.")
        val scenesJson = root.getJSONArray("scenes")

        val scenes = mutableListOf<SceneItem>()
        for (i in 0 until scenesJson.length()) {
            val item = scenesJson.getJSONObject(i)
            scenes.add(
                SceneItem(
                    sceneNo = i + 1,
                    durationSec = item.optInt("duration_sec", 6).coerceIn(5, 10),
                    description = item.optString("description", "Animated scene ${i + 1}"),
                    narration = item.optString("narration", ""),
                    status = SceneStatus.PENDING
                )
            )
        }
        if (scenes.isEmpty()) {
            throw IllegalStateException("Koi scenes generate nahi ho paaye. Prompt thoda detail mein likho!")
        }
        return StoryPlanResult(
            title = title,
            styleGuide = styleGuide,
            characterSheet = characterSheet,
            scenes = scenes
        )
    }

    suspend fun generateSceneHtml(
        style: AnimationStyle,
        styleGuide: String,
        characterSheet: String,
        scene: SceneItem,
        totalScenes: Int
    ): String {
        val animationPrompt = buildString {
            appendLine("Return ONLY a complete, self-contained animated HTML5 document (<!DOCTYPE html>...) using HTML5 <canvas> (preferred) or animated <svg>.")
            appendLine("DO NOT include markdown fences (```html), DO NOT include explanations, DO NOT load any external fonts, scripts, or images.")
            appendLine("")
            appendLine("PROJECT STYLE: ${style.label} — ${style.promptGuidance}")
            appendLine("STYLE GUIDE (MUST FOLLOW STRICTLY FOR COLORS & MOOD): $styleGuide")
            appendLine("CHARACTER SHEET (MUST KEEP CHARACTERS IDENTICAL): $characterSheet")
            appendLine("")
            appendLine("SCENE ${scene.sceneNo} OF $totalScenes (Duration: ${scene.durationSec}s):")
            appendLine("Action & Visuals: ${scene.description}")
            appendLine("Story context / mood: ${scene.narration}")
            appendLine("")
            appendLine("TECHNICAL REQUIREMENTS:")
            appendLine("1. Fill 100vw x 100vh cleanly with zero scrollbars: html, body { margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden; background: #0B0D13; }")
            appendLine("2. Render at crisp 16:9 aspect ratio (e.g., internal canvas 960x540 scaled via CSS width:100%; height:100%; object-fit:contain or resize listener).")
            appendLine("3. Animate smoothly at 60fps using requestAnimationFrame(loop). Use elapsed time t = ((performance.now() - start) / 1000) so the choreography unfolds naturally over ${scene.durationSec} seconds and loops gracefully.")
            appendLine("4. Draw rich multi-layered scenery (sky gradient, parallax clouds/buildings/stars, foreground props) AND expressive articulated characters (head, eyes blinking, limbs swinging, squash/stretch, particles/sparkles).")
            appendLine("5. Do NOT render large subtitle text inside the canvas (captions are overlaid separately by the app).")
            appendLine("6. Ensure zero JavaScript errors even on older mobile WebViews.")
        }

        val response = withTimeout(60_000L) {
            sceneAnimatorModel.generateContent(animationPrompt)
        }
        val raw = response.text?.trim().orEmpty()
        val cleaned = extractCleanHtml(raw)
        if (!cleaned.contains("<canvas", ignoreCase = true) && !cleaned.contains("<svg", ignoreCase = true)) {
            throw IllegalStateException("Scene ${scene.sceneNo} ka animation code incomplete tha. Retry kar rahe hain.")
        }
        return cleaned
    }

    private fun extractCleanHtml(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```")) {
            val firstNewline = text.indexOf('\n')
            if (firstNewline != -1) {
                text = text.substring(firstNewline + 1)
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length - 3)
            }
        }
        val docIdx = text.indexOf("<!DOCTYPE", ignoreCase = true)
        val htmlIdx = text.indexOf("<html", ignoreCase = true)
        val startIdx = when {
            docIdx >= 0 -> docIdx
            htmlIdx >= 0 -> htmlIdx
            else -> 0
        }
        val endHtmlIdx = text.lastIndexOf("</html>", ignoreCase = true)
        return if (endHtmlIdx > startIdx) {
            text.substring(startIdx, endHtmlIdx + 7).trim()
        } else {
            text.substring(startIdx).trim()
        }
    }

    companion object {
        fun isQuotaOrRateLimitError(error: Throwable): Boolean {
            val msg = buildString {
                append(error.message.orEmpty())
                append(" ")
                append(error.cause?.message.orEmpty())
                append(" ")
                append(error::class.java.simpleName)
            }.lowercase()
            return msg.contains("429") ||
                msg.contains("quota") ||
                msg.contains("resource_exhausted") ||
                msg.contains("rate limit") ||
                msg.contains("too many requests") ||
                msg.contains("503") ||
                msg.contains("overloaded")
        }

        fun toFriendlyHinglishError(error: Throwable): String {
            if (error is CancellationException && error !is TimeoutCancellationException) {
                throw error
            }
            val raw = error.message.orEmpty()
            Log.e(TAG, "Gemini error: $raw", error)
            return when {
                error is TimeoutCancellationException ->
                    "Gemini thoda zyada time le raha hai (60s timeout). Dobara try karo!"
                isQuotaOrRateLimitError(error) ->
                    "Free-tier API quota ya rate limit hit ho gaya! Tension mat lo — bane hue scenes safe hain. Thodi der baad Resume dabao."
                raw.contains("403") || raw.contains("AppCheck", ignoreCase = true) ->
                    "App Check verify nahi ho paaya (403). Network check karke dobara try karo."
                raw.contains("network", ignoreCase = true) ||
                    raw.contains("Unable to resolve host", ignoreCase = true) ||
                    raw.contains("timeout", ignoreCase = true) ->
                    "Internet connection weak lag raha hai. Connection check karke Resume karo!"
                else ->
                    "Kuch gadbad ho gayi: ${raw.ifBlank { "Please dobara try karo." }}"
            }
        }
    }
}
