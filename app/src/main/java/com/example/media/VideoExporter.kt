package com.example.media

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import com.example.data.model.SceneItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.roundToInt

private const val TAG = "VideoExporter"

data class ExportResult(
    val file: File,
    val fileName: String,
    val mimeType: String,
    val formatLabel: String
)

class VideoExporter(private val context: Context) {

    private data class CodecSelection(
        val mimeType: String,
        val muxerFormat: Int,
        val extension: String,
        val formatLabel: String
    )

    private fun selectBestCodec(): CodecSelection {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        val hasVp8Encoder = codecList.codecInfos.any { info ->
            info.isEncoder && info.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_VIDEO_VP8, ignoreCase = true) }
        }
        return if (hasVp8Encoder) {
            CodecSelection(
                mimeType = MediaFormat.MIMETYPE_VIDEO_VP8,
                muxerFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM,
                extension = "webm",
                formatLabel = "WebM"
            )
        } else {
            CodecSelection(
                mimeType = MediaFormat.MIMETYPE_VIDEO_AVC,
                muxerFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4,
                extension = "mp4",
                formatLabel = "MP4"
            )
        }
    }

    suspend fun exportScenes(
        projectTitle: String,
        scenes: List<SceneItem>,
        captionsEnabled: Boolean,
        webView: WebView,
        onProgress: (sceneIndex: Int, totalScenes: Int, fraction: Float, formatLabel: String) -> Unit
    ): ExportResult {
        if (scenes.isEmpty()) {
            throw IllegalStateException("Export karne ke liye kam se kam 1 ready scene chahiye.")
        }

        val width = 854
        val height = 480
        val fps = 15
        val frameIntervalMs = 1000L / fps
        val selection = selectBestCodec()

        val exportsDir = File(context.filesDir, "exports").apply { mkdirs() }
        val safeTitle = projectTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(24).ifBlank { "PromptToon" }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "${safeTitle}_$timestamp.${selection.extension}"
        val outFile = File(exportsDir, fileName)

        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var inputSurface: android.view.Surface? = null
        var muxerStarted = false
        var trackIndex = -1
        val frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val frameCanvas = Canvas(frameBitmap)

        val totalDurationSec = scenes.sumOf { it.durationSec }.coerceAtLeast(1)
        val totalExpectedFrames = totalDurationSec * fps
        var globalFrameIndex = 0L

        try {
            val format = MediaFormat.createVideoFormat(selection.mimeType, width, height).apply {
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                )
                setInteger(MediaFormat.KEY_BIT_RATE, 2_000_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
            }

            codec = MediaCodec.createEncoderByType(selection.mimeType).apply {
                configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                inputSurface = createInputSurface()
                start()
            }
            muxer = MediaMuxer(outFile.absolutePath, selection.muxerFormat)

            val bufferInfo = MediaCodec.BufferInfo()
            val fadePaint = Paint()
            val captionBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(195, 11, 13, 19)
            }
            val captionTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 22f
                isFakeBoldText = true
            }

            for ((idx, scene) in scenes.withIndex()) {
                // Load scene HTML into the attached export WebView on Main thread
                withContext(Dispatchers.Main) {
                    loadHtmlAndAwait(webView, scene.htmlCode)
                }
                delay(280L)

                val sceneFrames = (scene.durationSec * fps).coerceAtLeast(fps)
                val fadeFrames = (fps * 0.35f).roundToInt().coerceAtLeast(1)

                for (f in 0 until sceneFrames) {
                    val frameStart = System.currentTimeMillis()

                    withContext(Dispatchers.Main) {
                        frameCanvas.drawColor(Color.rgb(11, 13, 19))
                        val vw = webView.width.coerceAtLeast(1)
                        val vh = webView.height.coerceAtLeast(1)
                        frameCanvas.save()
                        frameCanvas.scale(width.toFloat() / vw, height.toFloat() / vh)
                        webView.draw(frameCanvas)
                        frameCanvas.restore()

                        // Fade transition at start and end of each scene
                        val fadeAlpha = when {
                            f < fadeFrames -> ((1f - f.toFloat() / fadeFrames) * 230).roundToInt()
                            f > sceneFrames - fadeFrames -> (((f - (sceneFrames - fadeFrames)).toFloat() / fadeFrames) * 230).roundToInt()
                            else -> 0
                        }
                        if (fadeAlpha > 0) {
                            fadePaint.color = Color.argb(fadeAlpha.coerceIn(0, 255), 11, 13, 19)
                            frameCanvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fadePaint)
                        }

                        // Optional caption bar
                        if (captionsEnabled && scene.narration.isNotBlank()) {
                            drawCaptionOverlay(
                                canvas = frameCanvas,
                                width = width,
                                height = height,
                                text = scene.narration,
                                bgPaint = captionBgPaint,
                                textPaint = captionTextPaint
                            )
                        }
                    }

                    // Submit frame to MediaCodec input surface
                    val surface = inputSurface ?: throw IllegalStateException("Encoder surface missing")
                    val surfaceCanvas = surface.lockCanvas(null)
                    try {
                        surfaceCanvas.drawBitmap(frameBitmap, 0f, 0f, null)
                    } finally {
                        surface.unlockCanvasAndPost(surfaceCanvas)
                    }

                    val drainage = drainEncoder(codec, muxer, bufferInfo, trackIndex, muxerStarted, endOfStream = false)
                    trackIndex = drainage.first
                    muxerStarted = drainage.second

                    globalFrameIndex++
                    val progress = (globalFrameIndex.toFloat() / totalExpectedFrames).coerceIn(0f, 0.99f)
                    onProgress(idx + 1, scenes.size, progress, selection.formatLabel)

                    val elapsed = System.currentTimeMillis() - frameStart
                    val waitMs = (frameIntervalMs - elapsed).coerceAtLeast(12L)
                    delay(waitMs)
                }
            }

            codec.signalEndOfInputStream()
            val finalDrain = drainEncoder(codec, muxer, bufferInfo, trackIndex, muxerStarted, endOfStream = true)
            muxerStarted = finalDrain.second

            return ExportResult(
                file = outFile,
                fileName = fileName,
                mimeType = if (selection.extension == "webm") "video/webm" else "video/mp4",
                formatLabel = selection.formatLabel
            )
        } catch (e: CancellationException) {
            outFile.delete()
            throw e
        } catch (e: Exception) {
            outFile.delete()
            Log.e(TAG, "Video export failed", e)
            throw e
        } finally {
            frameBitmap.recycle()
            try {
                inputSurface?.release()
            } catch (_: Exception) {
            }
            try {
                codec?.stop()
                codec?.release()
            } catch (_: Exception) {
            }
            try {
                if (muxerStarted) {
                    muxer?.stop()
                }
                muxer?.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun drainEncoder(
        codec: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        currentTrackIndex: Int,
        currentMuxerStarted: Boolean,
        endOfStream: Boolean
    ): Pair<Int, Boolean> {
        var trackIdx = currentTrackIndex
        var started = currentMuxerStarted
        val timeoutUs = if (endOfStream) 10_000L else 0L

        while (true) {
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
            when {
                outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) break
                }
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (!started) {
                        trackIdx = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        started = true
                    }
                }
                outputIndex >= 0 -> {
                    val encodedData = codec.getOutputBuffer(outputIndex)
                    if (encodedData != null) {
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            bufferInfo.size = 0
                        }
                        if (bufferInfo.size > 0 && started && trackIdx >= 0) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(trackIdx, encodedData, bufferInfo)
                        }
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                }
            }
        }
        return trackIdx to started
    }

    private fun drawCaptionOverlay(
        canvas: Canvas,
        width: Int,
        height: Int,
        text: String,
        bgPaint: Paint,
        textPaint: TextPaint
    ) {
        val maxTextWidth = (width * 0.84f).roundToInt()
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, maxTextWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(2f, 1.08f)
            .setMaxLines(3)
            .build()

        val boxWidth = (layout.width + 40).coerceAtMost(width - 32)
        val boxHeight = layout.height + 24
        val left = (width - boxWidth) / 2f
        val top = (height - boxHeight - 22).toFloat()
        val rect = RectF(left, top, left + boxWidth, top + boxHeight)
        canvas.drawRoundRect(rect, 16f, 16f, bgPaint)

        canvas.save()
        canvas.translate((width - layout.width) / 2f, top + 12f)
        layout.draw(canvas)
        canvas.restore()
    }

    private suspend fun loadHtmlAndAwait(webView: WebView, html: String) {
        suspendCancellableCoroutine { cont ->
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (cont.isActive) {
                        cont.resume(Unit)
                    }
                }
            }
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        }
    }

    companion object {
        fun saveToDeviceGallery(context: Context, file: File, fileName: String, mimeType: String): Boolean {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Video.Media.MIME_TYPE, mimeType)
                        put(
                            MediaStore.Video.Media.RELATIVE_PATH,
                            "${Environment.DIRECTORY_MOVIES}/PromptToon"
                        )
                        put(MediaStore.Video.Media.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                        ?: return false
                    resolver.openOutputStream(uri)?.use { out ->
                        file.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    true
                } else {
                    val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                        ?: return false
                    moviesDir.mkdirs()
                    val dest = File(moviesDir, fileName)
                    file.copyTo(dest, overwrite = true)
                    true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed saving video to gallery", e)
                false
            }
        }

        fun shareVideo(context: Context, file: File, mimeType: String) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, "Share PromptToon Video").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }
}
