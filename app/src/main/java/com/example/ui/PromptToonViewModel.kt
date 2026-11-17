package com.example.ui

import android.app.Application
import android.net.Uri
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiStoryboardService
import com.example.data.local.ProjectRepository
import com.example.data.local.PromptToonDatabase
import com.example.data.model.AnimationStyle
import com.example.data.model.DurationPreset
import com.example.data.model.ExportState
import com.example.data.model.GenerationState
import com.example.data.model.SceneItem
import com.example.data.model.SceneStatus
import com.example.data.model.StoryStarter
import com.example.data.model.ToonProject
import com.example.media.ExportResult
import com.example.media.VideoExporter
import com.example.media.VoiceAndMusicController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class AppTab {
    STUDIO,
    PROJECTS
}

class PromptToonViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(
        PromptToonDatabase.getInstance(application).projectDao()
    )
    private val aiService = GeminiStoryboardService()
    private val voiceAndMusic = VoiceAndMusicController(application)
    private val videoExporter = VideoExporter(application)

    val savedProjects: StateFlow<List<ToonProject>> = repository.allProjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList()
        )

    private val _currentTab = MutableStateFlow(AppTab.STUDIO)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _project = MutableStateFlow(ToonProject())
    val project: StateFlow<ToonProject> = _project.asStateFlow()

    private val _selectedPreset = MutableStateFlow(DurationPreset.SEC_30)
    val selectedPreset: StateFlow<DurationPreset> = _selectedPreset.asStateFlow()

    private val _selectedSceneIndex = MutableStateFlow(0)
    val selectedSceneIndex: StateFlow<Int> = _selectedSceneIndex.asStateFlow()

    private val _generationState = MutableStateFlow<GenerationState>(GenerationState.Idle)
    val generationState: StateFlow<GenerationState> = _generationState.asStateFlow()

    private val _isComposerExpanded = MutableStateFlow(true)
    val isComposerExpanded: StateFlow<Boolean> = _isComposerExpanded.asStateFlow()

    private val _isPlayingAll = MutableStateFlow(false)
    val isPlayingAll: StateFlow<Boolean> = _isPlayingAll.asStateFlow()

    private val _playbackFadeAlpha = MutableStateFlow(0f)
    val playbackFadeAlpha: StateFlow<Float> = _playbackFadeAlpha.asStateFlow()

    private val _playbackElapsedSec = MutableStateFlow(0)
    val playbackElapsedSec: StateFlow<Int> = _playbackElapsedSec.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private var generationJob: Job? = null
    private var playbackJob: Job? = null
    private var exportJob: Job? = null
    private var lastExportResult: ExportResult? = null

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun dismissToast() {
        _toastMessage.value = null
    }

    fun setComposerExpanded(expanded: Boolean) {
        _isComposerExpanded.value = expanded
    }

    fun updatePrompt(prompt: String) {
        _project.value = _project.value.copy(prompt = prompt)
    }

    fun updateStyle(style: AnimationStyle) {
        _project.value = _project.value.copy(style = style)
        persistIfHasScenes()
    }

    fun selectDurationPreset(preset: DurationPreset) {
        _selectedPreset.value = preset
        if (preset != DurationPreset.CUSTOM) {
            _project.value = _project.value.copy(targetDurationSec = preset.seconds)
        }
    }

    fun updateCustomDuration(seconds: Int) {
        _selectedPreset.value = DurationPreset.CUSTOM
        _project.value = _project.value.copy(targetDurationSec = seconds.coerceIn(10, 600))
    }

    fun applyStarter(starter: StoryStarter) {
        val matchingPreset = DurationPreset.entries.firstOrNull {
            it != DurationPreset.CUSTOM && it.seconds == starter.suggestedSeconds
        } ?: DurationPreset.CUSTOM
        _selectedPreset.value = matchingPreset
        _project.value = _project.value.copy(
            prompt = starter.fullPrompt,
            style = starter.suggestedStyle,
            targetDurationSec = starter.suggestedSeconds
        )
    }

    fun startNewProject() {
        stopPlayback()
        generationJob?.cancel()
        _generationState.value = GenerationState.Idle
        _selectedSceneIndex.value = 0
        _selectedPreset.value = DurationPreset.SEC_30
        _project.value = ToonProject()
        _isComposerExpanded.value = true
        _currentTab.value = AppTab.STUDIO
    }

    fun loadProject(proj: ToonProject) {
        stopPlayback()
        generationJob?.cancel()
        _project.value = proj
        val preset = DurationPreset.entries.firstOrNull {
            it != DurationPreset.CUSTOM && it.seconds == proj.targetDurationSec
        } ?: DurationPreset.CUSTOM
        _selectedPreset.value = preset
        _selectedSceneIndex.value = 0
        _isComposerExpanded.value = proj.scenes.isEmpty()

        val pendingCount = proj.scenes.count { it.status != SceneStatus.READY }
        _generationState.value = when {
            proj.scenes.isEmpty() -> GenerationState.Idle
            pendingCount > 0 -> GenerationState.Paused(
                completedCount = proj.readyCount,
                totalScenes = proj.scenes.size,
                reason = "Pichle session se ${proj.readyCount}/${proj.scenes.size} scenes ready hain. Resume dabake baaqi banao!",
                isQuotaLimit = false
            )
            else -> GenerationState.Completed(proj.scenes.size)
        }
        _currentTab.value = AppTab.STUDIO
    }

    fun deleteSavedProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            if (_project.value.id == projectId) {
                startNewProject()
            }
            _toastMessage.value = "Project delete ho gaya."
        }
    }

    fun selectScene(index: Int) {
        val scenes = _project.value.scenes
        if (scenes.isEmpty()) return
        _selectedSceneIndex.value = index.coerceIn(0, scenes.lastIndex)
        if (_project.value.voiceoverEnabled && !_isPlayingAll.value) {
            val scene = scenes[_selectedSceneIndex.value]
            if (scene.status == SceneStatus.READY && scene.narration.isNotBlank()) {
                voiceAndMusic.speak(scene.narration, _project.value.voiceSpeed)
            }
        }
    }

    fun generateStoryboardAndScenes() {
        val current = _project.value
        val prompt = current.prompt.trim()
        if (prompt.isEmpty()) {
            _generationState.value = GenerationState.Error(
                "Pehle apni story likho ya upar se ek starter choose karo!"
            )
            return
        }

        stopPlayback()
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            try {
                _generationState.value = GenerationState.SplittingStory(
                    "Gemini story ko scenes mein split kar raha hai..."
                )
                val plan = aiService.planStory(
                    prompt = prompt,
                    targetDurationSec = current.targetDurationSec,
                    style = current.style
                )

                val newProject = current.copy(
                    id = if (current.scenes.isEmpty()) current.id else UUID.randomUUID().toString(),
                    title = plan.title,
                    styleGuide = plan.styleGuide,
                    characterSheet = plan.characterSheet,
                    scenes = plan.scenes,
                    updatedAt = System.currentTimeMillis()
                )
                _project.value = newProject
                _selectedSceneIndex.value = 0
                _isComposerExpanded.value = false
                repository.saveProject(newProject)

                runSceneGenerationLoop()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _generationState.value = GenerationState.Error(
                    GeminiStoryboardService.toFriendlyHinglishError(e)
                )
            }
        }
    }

    fun resumeGeneration() {
        val current = _project.value
        if (current.scenes.isEmpty()) {
            generateStoryboardAndScenes()
            return
        }
        stopPlayback()
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            runSceneGenerationLoop()
        }
    }

    fun pauseGeneration() {
        generationJob?.cancel()
        generationJob = null
        val cur = _project.value
        val cleanScenes = cur.scenes.map {
            if (it.status == SceneStatus.GENERATING) it.copy(status = SceneStatus.PENDING) else it
        }
        val updated = cur.copy(scenes = cleanScenes)
        _project.value = updated
        viewModelScope.launch { repository.saveProject(updated) }
        _generationState.value = GenerationState.Paused(
            completedCount = updated.readyCount,
            totalScenes = updated.scenes.size,
            reason = "Generation pause kar diya gaya hai. Jab chaho Resume dabao!",
            isQuotaLimit = false
        )
    }

    private suspend fun runSceneGenerationLoop() {
        val total = _project.value.scenes.size
        for (i in 0 until total) {
            val scene = _project.value.scenes.getOrNull(i) ?: continue
            if (scene.status == SceneStatus.READY && scene.htmlCode.isNotBlank()) {
                continue
            }

            updateSceneAt(i) {
                it.copy(status = SceneStatus.GENERATING, errorMessage = null)
            }
            _generationState.value = GenerationState.GeneratingScenes(
                currentScene = i + 1,
                totalScenes = total,
                statusText = "Scene ${i + 1}/$total animate ho raha hai..."
            )

            var success = false
            var lastError: Throwable? = null
            val backoffDelays = listOf(2500L, 5000L)

            for (attempt in 0..backoffDelays.size) {
                try {
                    val proj = _project.value
                    val html = aiService.generateSceneHtml(
                        style = proj.style,
                        styleGuide = proj.styleGuide,
                        characterSheet = proj.characterSheet,
                        scene = scene,
                        totalScenes = total
                    )
                    updateSceneAt(i) {
                        it.copy(
                            htmlCode = html,
                            status = SceneStatus.READY,
                            errorMessage = null
                        )
                    }
                    repository.saveProject(_project.value)
                    if (_selectedSceneIndex.value == 0 || i == 0) {
                        _selectedSceneIndex.value = i
                    }
                    success = true
                    break
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastError = e
                    if (attempt < backoffDelays.size) {
                        val waitMs = backoffDelays[attempt]
                        val secLabel = waitMs / 1000.0
                        _generationState.value = GenerationState.GeneratingScenes(
                            currentScene = i + 1,
                            totalScenes = total,
                            statusText = if (GeminiStoryboardService.isQuotaOrRateLimitError(e)) {
                                "Rate limit — ${secLabel}s wait karke Scene ${i + 1} retry kar rahe hain..."
                            } else {
                                "Scene ${i + 1} thoda atka — ${secLabel}s baad retry..."
                            }
                        )
                        delay(waitMs)
                    }
                }
            }

            if (!success) {
                val err = lastError ?: IllegalStateException("Scene generation failed")
                val friendly = GeminiStoryboardService.toFriendlyHinglishError(err)
                val isQuota = GeminiStoryboardService.isQuotaOrRateLimitError(err)
                updateSceneAt(i) {
                    it.copy(status = SceneStatus.ERROR, errorMessage = friendly)
                }
                repository.saveProject(_project.value)
                _generationState.value = GenerationState.Paused(
                    completedCount = _project.value.readyCount,
                    totalScenes = total,
                    reason = friendly,
                    isQuotaLimit = isQuota
                )
                return
            }

            // Respect free-tier rate limits with a polite gap between scenes
            if (i < total - 1) {
                delay(1200L)
            }
        }

        val readyCount = _project.value.readyCount
        _generationState.value = GenerationState.Completed(readyCount)
        _toastMessage.value = "Sab $readyCount scenes ready hain! Play All dabake dekho."
    }

    fun regenerateSingleScene(index: Int) {
        val current = _project.value
        val scene = current.scenes.getOrNull(index) ?: return
        stopPlayback()
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            updateSceneAt(index) {
                it.copy(status = SceneStatus.GENERATING, errorMessage = null)
            }
            _generationState.value = GenerationState.GeneratingScenes(
                currentScene = index + 1,
                totalScenes = current.scenes.size,
                statusText = "Scene ${index + 1} naye tareeke se bana rahe hain..."
            )
            try {
                val html = aiService.generateSceneHtml(
                    style = current.style,
                    styleGuide = current.styleGuide,
                    characterSheet = current.characterSheet,
                    scene = scene,
                    totalScenes = current.scenes.size
                )
                updateSceneAt(index) {
                    it.copy(
                        htmlCode = html,
                        status = SceneStatus.READY,
                        errorMessage = null
                    )
                }
                repository.saveProject(_project.value)
                _selectedSceneIndex.value = index
                _generationState.value = GenerationState.Completed(_project.value.readyCount)
                _toastMessage.value = "Scene ${index + 1} regenerate ho gaya!"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = GeminiStoryboardService.toFriendlyHinglishError(e)
                updateSceneAt(index) {
                    it.copy(status = SceneStatus.ERROR, errorMessage = msg)
                }
                _generationState.value = GenerationState.Error(msg)
            }
        }
    }

    fun moveScene(index: Int, delta: Int) {
        val scenes = _project.value.scenes.toMutableList()
        val targetIndex = index + delta
        if (index !in scenes.indices || targetIndex !in scenes.indices) return
        stopPlayback()
        val item = scenes.removeAt(index)
        scenes.add(targetIndex, item)
        val renumbered = scenes.mapIndexed { idx, s -> s.copy(sceneNo = idx + 1) }
        val updated = _project.value.copy(scenes = renumbered)
        _project.value = updated
        _selectedSceneIndex.value = targetIndex
        viewModelScope.launch { repository.saveProject(updated) }
    }

    fun deleteScene(index: Int) {
        val scenes = _project.value.scenes.toMutableList()
        if (index !in scenes.indices) return
        if (scenes.size <= 1) {
            _toastMessage.value = "Kam se kam 1 scene timeline mein rehna chahiye!"
            return
        }
        stopPlayback()
        scenes.removeAt(index)
        val renumbered = scenes.mapIndexed { idx, s -> s.copy(sceneNo = idx + 1) }
        val updated = _project.value.copy(scenes = renumbered)
        _project.value = updated
        _selectedSceneIndex.value = _selectedSceneIndex.value.coerceIn(0, renumbered.lastIndex)
        viewModelScope.launch { repository.saveProject(updated) }
        _toastMessage.value = "Scene hata diya gaya."
    }

    fun updateSceneDetails(index: Int, description: String, narration: String, durationSec: Int) {
        updateSceneAt(index) {
            it.copy(
                description = description.trim(),
                narration = narration.trim(),
                durationSec = durationSec.coerceIn(5, 10)
            )
        }
        persistIfHasScenes()
        _toastMessage.value = "Scene update save ho gaya."
    }

    fun toggleCaptions() {
        _project.value = _project.value.copy(captionsEnabled = !_project.value.captionsEnabled)
        persistIfHasScenes()
    }

    fun toggleVoiceover() {
        val next = !_project.value.voiceoverEnabled
        _project.value = _project.value.copy(voiceoverEnabled = next)
        if (!next) {
            voiceAndMusic.stopSpeech()
        }
        if (_isPlayingAll.value) {
            voiceAndMusic.updateMusicVolume(duckForVoice = next)
        }
        persistIfHasScenes()
    }

    fun cycleVoiceSpeed() {
        val speeds = listOf(0.85f, 1.0f, 1.2f)
        val cur = _project.value.voiceSpeed
        val nextIdx = (speeds.indexOfFirst { kotlin.math.abs(it - cur) < 0.05f } + 1) % speeds.size
        _project.value = _project.value.copy(voiceSpeed = speeds[nextIdx])
        persistIfHasScenes()
    }

    fun attachMusic(uri: Uri) {
        viewModelScope.launch {
            val result = VoiceAndMusicController.copyAudioUriToStorage(
                getApplication(),
                uri,
                _project.value.id
            )
            if (result != null) {
                val (path, title) = result
                _project.value = _project.value.copy(
                    musicFilePath = path,
                    musicTitle = title
                )
                persistIfHasScenes()
                _toastMessage.value = "Music add ho gaya: $title"
            } else {
                _toastMessage.value = "Audio file load nahi ho paayi. Koi aur MP3/WAV try karo."
            }
        }
    }

    fun removeMusic() {
        voiceAndMusic.stopMusic()
        _project.value = _project.value.copy(musicFilePath = null, musicTitle = null)
        persistIfHasScenes()
    }

    fun togglePlayAll() {
        if (_isPlayingAll.value) {
            stopPlayback()
            return
        }
        val readyIndices = _project.value.scenes.mapIndexedNotNull { idx, scene ->
            if (scene.status == SceneStatus.READY && scene.htmlCode.isNotBlank()) idx else null
        }
        if (readyIndices.isEmpty()) {
            _toastMessage.value = "Abhi koi ready scene nahi hai — pehle Generate dabao."
            return
        }

        _isPlayingAll.value = true
        _playbackElapsedSec.value = 0

        val proj = _project.value
        if (!proj.musicFilePath.isNullOrBlank()) {
            voiceAndMusic.startMusic(proj.musicFilePath, duckForVoice = proj.voiceoverEnabled)
        }

        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            try {
                var elapsed = 0
                for ((pos, sceneIndex) in readyIndices.withIndex()) {
                    val scene = _project.value.scenes.getOrNull(sceneIndex) ?: continue
                    // Smooth fade transition between scenes
                    if (pos > 0) {
                        _playbackFadeAlpha.value = 1f
                        delay(180L)
                    }
                    _selectedSceneIndex.value = sceneIndex
                    _playbackFadeAlpha.value = 0f

                    if (_project.value.voiceoverEnabled && scene.narration.isNotBlank()) {
                        voiceAndMusic.speak(scene.narration, _project.value.voiceSpeed)
                    }

                    val sec = scene.durationSec.coerceIn(5, 10)
                    repeat(sec) {
                        delay(1000L)
                        elapsed += 1
                        _playbackElapsedSec.value = elapsed
                    }
                }
            } finally {
                _playbackFadeAlpha.value = 0f
                _isPlayingAll.value = false
                voiceAndMusic.stopSpeech()
                voiceAndMusic.stopMusic()
            }
        }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        _isPlayingAll.value = false
        _playbackFadeAlpha.value = 0f
        voiceAndMusic.stopSpeech()
        voiceAndMusic.stopMusic()
    }

    fun startVideoExport(exportWebView: WebView) {
        val readyScenes = _project.value.scenes.filter {
            it.status == SceneStatus.READY && it.htmlCode.isNotBlank()
        }
        if (readyScenes.isEmpty()) {
            _exportState.value = ExportState.Error("Export ke liye kam se kam 1 ready scene chahiye.")
            return
        }

        stopPlayback()
        exportJob?.cancel()
        exportJob = viewModelScope.launch {
            _exportState.value = ExportState.Preparing("Video recorder ready ho raha hai...")
            try {
                val result = videoExporter.exportScenes(
                    projectTitle = _project.value.title,
                    scenes = readyScenes,
                    captionsEnabled = _project.value.captionsEnabled,
                    webView = exportWebView,
                    onProgress = { idx, total, fraction, formatLabel ->
                        _exportState.value = ExportState.Recording(
                            sceneIndex = idx,
                            totalScenes = total,
                            progressFraction = fraction,
                            formatLabel = formatLabel
                        )
                    }
                )
                lastExportResult = result
                _exportState.value = ExportState.Completed(
                    filePath = result.file.absolutePath,
                    fileName = result.fileName,
                    formatLabel = result.formatLabel,
                    savedToGallery = false
                )
            } catch (e: CancellationException) {
                _exportState.value = ExportState.Idle
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(
                    "Export mein dikkat aayi: ${e.localizedMessage ?: "Dobara try karo."}"
                )
            }
        }
    }

    fun downloadExportedVideo() {
        val result = lastExportResult ?: return
        viewModelScope.launch {
            val saved = VideoExporter.saveToDeviceGallery(
                context = getApplication(),
                file = result.file,
                fileName = result.fileName,
                mimeType = result.mimeType
            )
            val current = _exportState.value
            if (current is ExportState.Completed) {
                _exportState.value = current.copy(savedToGallery = saved)
            }
            _toastMessage.value = if (saved) {
                "Video Movies/PromptToon mein download ho gaya!"
            } else {
                "Gallery save fail hua, par Share button se video bhej sakte ho."
            }
        }
    }

    fun shareExportedVideo() {
        val result = lastExportResult ?: return
        if (!result.file.exists()) return
        VideoExporter.shareVideo(getApplication(), result.file, result.mimeType)
    }

    fun cancelOrResetExport() {
        exportJob?.cancel()
        exportJob = null
        _exportState.value = ExportState.Idle
    }

    private fun updateSceneAt(index: Int, transform: (SceneItem) -> SceneItem) {
        val scenes = _project.value.scenes.toMutableList()
        if (index !in scenes.indices) return
        scenes[index] = transform(scenes[index])
        _project.value = _project.value.copy(scenes = scenes)
    }

    private fun persistIfHasScenes() {
        val proj = _project.value
        if (proj.scenes.isNotEmpty()) {
            viewModelScope.launch { repository.saveProject(proj) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceAndMusic.release()
        exportJob?.cancel()
        playbackJob?.cancel()
        generationJob?.cancel()
    }
}
