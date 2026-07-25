package com.suvojeet.notenext.util

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

private const val TAG = "AudioRecorder"

/** Where recordings live. Mirrors the camera_photos / images layout used by other attachments. */
private const val AUDIO_DIR = "audio_notes"

/**
 * Thin MediaRecorder wrapper for voice notes.
 *
 * Records AAC in an MP4 container — playable by every Android version the app
 * supports and small enough that a long recording won't bloat the note.
 *
 * Not thread safe; drive it from a single (UI) caller.
 */
class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    val isRecording: Boolean get() = recorder != null

    /**
     * Starts a new recording.
     *
     * @return true if recording actually started. On false nothing is left running
     *   and no partial file survives, so the caller can just report the failure.
     */
    fun start(): Boolean {
        if (recorder != null) return false

        val dir = File(context.filesDir, AUDIO_DIR).apply { if (!exists()) mkdirs() }
        val file = File(dir, "AUD_${System.currentTimeMillis()}.m4a")

        return try {
            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = newRecorder
            outputFile = file
            true
        } catch (e: Exception) {
            // prepare()/start() throw on a busy mic, a revoked permission, or a
            // device that rejects the encoder config. Leave nothing behind.
            Log.w(TAG, "Failed to start recording", e)
            runCatching { recorder?.release() }
            recorder = null
            outputFile = null
            file.delete()
            false
        }
    }

    /**
     * Stops recording and returns a FileProvider uri for the finished clip, or null
     * if the recording failed or was too short to produce any audio.
     */
    fun stop(): Uri? {
        val activeRecorder = recorder ?: return null
        val file = outputFile
        recorder = null
        outputFile = null

        val stoppedCleanly = try {
            activeRecorder.stop()
            true
        } catch (e: RuntimeException) {
            // stop() throws when it's called before any frames were written — a tap
            // that starts and ends within a few hundred ms. The file is unusable.
            Log.w(TAG, "Recording stopped before any audio was captured", e)
            false
        } finally {
            runCatching { activeRecorder.release() }
        }

        if (!stoppedCleanly || file == null || !file.exists() || file.length() == 0L) {
            file?.delete()
            return null
        }

        return runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrElse {
            Log.w(TAG, "Could not expose recording via FileProvider", it)
            file.delete()
            null
        }
    }

    /** Aborts the current recording and deletes the partial file. */
    fun cancel() {
        val activeRecorder = recorder ?: return
        val file = outputFile
        recorder = null
        outputFile = null

        runCatching { activeRecorder.stop() }
        runCatching { activeRecorder.release() }
        file?.delete()
    }

    /**
     * Current input level, 0f..1f, for the waveform. Returns 0f when idle.
     * MediaRecorder reports a raw amplitude out of 32767.
     */
    fun currentAmplitude(): Float {
        val active = recorder ?: return 0f
        return runCatching {
            (active.maxAmplitude.coerceAtLeast(0) / 32767f).coerceIn(0f, 1f)
        }.getOrDefault(0f)
    }
}

/** Deletes a recording previously produced by [AudioRecorder]. No-op for other uris. */
fun deleteRecording(context: Context, uri: String) {
    val name = uri.substringAfterLast('/')
    if (!name.startsWith("AUD_")) return
    runCatching { File(File(context.filesDir, AUDIO_DIR), name).delete() }
}
