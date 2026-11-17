package com.example.media

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import android.speech.tts.TextToSpeech
import android.util.Log
import java.io.File
import java.util.Locale

private const val TAG = "VoiceAndMusic"

class VoiceAndMusicController(context: Context) {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var mediaPlayer: MediaPlayer? = null
    private var currentMusicPath: String? = null

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val indianEnglish = Locale("en", "IN")
                val result = tts?.setLanguage(indianEnglish)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.US)
                }
                ttsReady = true
            } else {
                Log.w(TAG, "TextToSpeech init failed with status=$status")
            }
        }
    }

    fun speak(text: String, speed: Float = 1.0f) {
        if (!ttsReady || text.isBlank()) return
        try {
            tts?.setSpeechRate(speed.coerceIn(0.6f, 1.8f))
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "scene_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.e(TAG, "TTS speak failed", e)
        }
    }

    fun stopSpeech() {
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
    }

    fun startMusic(filePath: String?, duckForVoice: Boolean) {
        if (filePath.isNullOrBlank()) return
        val file = File(filePath)
        if (!file.exists()) return
        try {
            if (currentMusicPath != filePath || mediaPlayer == null) {
                stopMusic()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    isLooping = true
                    prepare()
                }
                currentMusicPath = filePath
            }
            val volume = if (duckForVoice) 0.20f else 0.65f
            mediaPlayer?.setVolume(volume, volume)
            if (mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start background music", e)
            stopMusic()
        }
    }

    fun updateMusicVolume(duckForVoice: Boolean) {
        val volume = if (duckForVoice) 0.20f else 0.65f
        try {
            mediaPlayer?.setVolume(volume, volume)
        } catch (_: Exception) {
        }
    }

    fun stopMusic() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
            currentMusicPath = null
        }
    }

    fun release() {
        stopSpeech()
        stopMusic()
        try {
            tts?.shutdown()
        } catch (_: Exception) {
        } finally {
            tts = null
            ttsReady = false
        }
    }

    companion object {
        fun copyAudioUriToStorage(context: Context, uri: Uri, projectId: String): Pair<String, String>? {
            return try {
                val resolver = context.contentResolver
                var displayName = "background_music.mp3"
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            displayName = cursor.getString(idx) ?: displayName
                        }
                    }
                }
                val musicDir = File(context.filesDir, "music").apply { mkdirs() }
                val safeExt = displayName.substringAfterLast('.', "mp3")
                val destFile = File(musicDir, "bgm_${projectId}.$safeExt")
                resolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: return null
                destFile.absolutePath to displayName
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy selected audio file", e)
                null
            }
        }
    }
}
