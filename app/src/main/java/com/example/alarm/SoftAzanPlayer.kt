package com.example.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.example.model.SoftAzanTone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AzanPlaybackState(
    val isPlaying: Boolean = false,
    val label: String? = null,
    val tone: SoftAzanTone? = null
)

object SoftAzanPlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaPlayer: MediaPlayer? = null
    private var fadeJob: Job? = null

    private val _playbackState = MutableStateFlow(AzanPlaybackState())
    val playbackState: StateFlow<AzanPlaybackState> = _playbackState.asStateFlow()

    fun play(
        context: Context,
        tone: SoftAzanTone,
        targetVolume: Float,
        label: String,
        isAlarmStream: Boolean = true
    ) {
        stop()
        val appContext = context.applicationContext
        val clampedVolume = targetVolume.coerceIn(0.10f, 0.85f)

        runCatching {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(
                            if (isAlarmStream) AudioAttributes.USAGE_ALARM
                            else AudioAttributes.USAGE_MEDIA
                        )
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                val uri = Uri.parse("android.resource://${appContext.packageName}/${tone.rawResId}")
                setDataSource(appContext, uri)
                setVolume(0.05f, 0.05f)
                setOnCompletionListener {
                    stop()
                }
                setOnErrorListener { _, _, _ ->
                    stop()
                    true
                }
                prepare()
                start()
            }
            mediaPlayer = player
            _playbackState.value = AzanPlaybackState(
                isPlaying = true,
                label = label,
                tone = tone
            )

            // Smooth 2.5s gentle fade-in so the soft Azan never startles
            fadeJob = scope.launch {
                val steps = 20
                for (i in 1..steps) {
                    delay(125L)
                    val mp = mediaPlayer ?: break
                    val fraction = i.toFloat() / steps
                    val vol = 0.05f + (clampedVolume - 0.05f) * fraction
                    runCatching { mp.setVolume(vol, vol) }
                }
            }
        }.onFailure {
            stop()
        }
    }

    fun stop() {
        fadeJob?.cancel()
        fadeJob = null
        mediaPlayer?.let { mp ->
            runCatching {
                if (mp.isPlaying) mp.stop()
                mp.release()
            }
        }
        mediaPlayer = null
        _playbackState.value = AzanPlaybackState()
    }
}
