package com.example.data.local

import com.example.data.model.AnimationStyle
import com.example.data.model.SceneItem
import com.example.data.model.SceneStatus
import com.example.data.model.ToonProject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class ProjectRepository(private val projectDao: ProjectDao) {

    val allProjects: Flow<List<ToonProject>> = projectDao.observeAllProjects().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun getProject(id: String): ToonProject? {
        return projectDao.getProjectById(id)?.toDomain()
    }

    suspend fun saveProject(project: ToonProject) {
        projectDao.upsertProject(project.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

    suspend fun deleteProject(id: String) {
        projectDao.deleteProjectById(id)
    }

    private fun ToonProject.toEntity(): ProjectEntity {
        val scenesArray = JSONArray()
        scenes.forEach { scene ->
            scenesArray.put(
                JSONObject().apply {
                    put("id", scene.id)
                    put("sceneNo", scene.sceneNo)
                    put("durationSec", scene.durationSec)
                    put("description", scene.description)
                    put("narration", scene.narration)
                    put("htmlCode", scene.htmlCode)
                    put("status", scene.status.name)
                    if (scene.errorMessage != null) {
                        put("errorMessage", scene.errorMessage)
                    }
                }
            )
        }
        return ProjectEntity(
            id = id,
            title = title,
            prompt = prompt,
            targetDurationSec = targetDurationSec,
            styleName = style.name,
            styleGuide = styleGuide,
            characterSheet = characterSheet,
            scenesJson = scenesArray.toString(),
            captionsEnabled = captionsEnabled,
            voiceoverEnabled = voiceoverEnabled,
            voiceSpeed = voiceSpeed,
            musicFilePath = musicFilePath,
            musicTitle = musicTitle,
            updatedAt = updatedAt
        )
    }

    private fun ProjectEntity.toDomain(): ToonProject {
        val parsedStyle = AnimationStyle.entries.firstOrNull { it.name == styleName }
            ?: AnimationStyle.CARTOON
        val scenesList = mutableListOf<SceneItem>()
        try {
            val array = JSONArray(scenesJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val statusName = obj.optString("status", SceneStatus.PENDING.name)
                val status = SceneStatus.entries.firstOrNull { it.name == statusName }
                    ?: SceneStatus.PENDING
                // Recover interrupted GENERATING states as PENDING on reload
                val safeStatus = if (status == SceneStatus.GENERATING) SceneStatus.PENDING else status
                scenesList.add(
                    SceneItem(
                        id = obj.optString("id", "${id}_$i"),
                        sceneNo = obj.optInt("sceneNo", i + 1),
                        durationSec = obj.optInt("durationSec", 6).coerceIn(5, 10),
                        description = obj.optString("description", ""),
                        narration = obj.optString("narration", ""),
                        htmlCode = obj.optString("htmlCode", ""),
                        status = safeStatus,
                        errorMessage = obj.optString("errorMessage").takeIf { it.isNotBlank() }
                    )
                )
            }
        } catch (_: Exception) {
            // Keep empty list if JSON malformed
        }

        return ToonProject(
            id = id,
            title = title,
            prompt = prompt,
            targetDurationSec = targetDurationSec,
            style = parsedStyle,
            styleGuide = styleGuide,
            characterSheet = characterSheet,
            scenes = scenesList,
            captionsEnabled = captionsEnabled,
            voiceoverEnabled = voiceoverEnabled,
            voiceSpeed = voiceSpeed,
            musicFilePath = musicFilePath,
            musicTitle = musicTitle,
            updatedAt = updatedAt
        )
    }
}
