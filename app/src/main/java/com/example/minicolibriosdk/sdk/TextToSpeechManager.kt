package com.example.minicolibriosdk.sdk


import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import java.io.File
import java.util.*
import kotlin.concurrent.thread
import android.speech.tts.UtteranceProgressListener


class TextToSpeechManager(private val context: Context) {
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(0.8f) // Slower and clearer speech
            }
        }
    }

    // New: Estimate real TTS audio duration
    private fun estimateAudioDuration(text: String, onDuration: (Long) -> Unit) {
        val file = File(context.cacheDir, "tts_sample.wav")
        val utteranceId = UUID.randomUUID().toString()

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(id: String?) {
                if (id == utteranceId) {
                    val mediaPlayer = MediaPlayer()
                    try {
                        mediaPlayer.setDataSource(file.absolutePath)
                        mediaPlayer.prepare()
                        val duration = mediaPlayer.duration.toLong()
                        mediaPlayer.release()
                        onDuration(duration)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        onDuration(2000L) // fallback estimate
                    }
                }
            }

            override fun onError(utteranceId: String?) {
                onDuration(2000L) // fallback on error
            }
        })

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        tts?.synthesizeToFile(text, params, file, utteranceId)
    }

    // Updated: uses real audio duration to sync word highlight
    fun speakSentenceWithWordHighlight(
        sentence: String,
        startWordIndex: Int,
        onHighlight: (Int) -> Unit,
        onDone: () -> Unit
    ) {
        val words = sentence.split(" ")

        estimateAudioDuration(sentence) { duration ->
            val delayPerWord = duration / words.size

            tts?.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())

            thread {
                for ((i, _) in words.withIndex()) {
                    Thread.sleep(delayPerWord)
                    onHighlight(startWordIndex + i)
                }
                onDone()
            }
        }
    }

    fun isSpeaking(): Boolean {
        return tts?.isSpeaking == true
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
