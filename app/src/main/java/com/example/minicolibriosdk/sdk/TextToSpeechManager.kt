package com.example.minicolibriosdk.sdk


import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.*
import java.util.*

class TextToSpeechManager(private val context: Context) {
    private var tts: TextToSpeech? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var lastSentence: String = ""
    private var lastStartWordIndex: Int = 0
    private var pausedWordOffsetInSentence = 0
    private var isPaused = false
    private var isSpeaking = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(0.8f)
            }
        }
    }

    fun speakSentenceWithWordHighlight(
        sentence: String,
        startWordIndex: Int,
        onHighlight: (Int) -> Unit,
        onDone: () -> Unit
    ) {
        lastSentence = sentence
        lastStartWordIndex = startWordIndex
        pausedWordOffsetInSentence = 0
        isPaused = false
        isSpeaking = true

        val words = sentence.split(" ")

        scope.launch {
            for ((i, word) in words.withIndex()) {
                if (isPaused) {
                    pausedWordOffsetInSentence = i
                    break
                }
                onHighlight(startWordIndex + i)
                delay(400) // Estimated per-word delay
            }

            if (!isPaused) {
                pausedWordOffsetInSentence = 0
                onDone()
            }
        }

        tts?.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    fun pause() {
        isPaused = true
        isSpeaking = false
        tts?.stop()
    }

    fun resume(onHighlight: (Int) -> Unit, onDone: () -> Unit) {
        isPaused = false
        isSpeaking = true

        val allWords = lastSentence.split(" ")
        val remainingWords = allWords.drop(pausedWordOffsetInSentence)
        val resumedSentence = remainingWords.joinToString(" ")
        val resumedStartIndex = lastStartWordIndex + pausedWordOffsetInSentence

        speakSentenceWithWordHighlight(resumedSentence, resumedStartIndex, onHighlight, onDone)
    }

    fun isSpeaking(): Boolean {
        return isSpeaking
    }

    fun shutdown() {
        isPaused = false
        isSpeaking = false
        pausedWordOffsetInSentence = 0
        tts?.stop()
        tts?.shutdown()
        scope.cancel()
    }
}
