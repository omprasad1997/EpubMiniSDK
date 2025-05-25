package com.example.minicolibriosdk


import android.os.Bundle
import android.webkit.WebView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.minicolibriosdk.sdk.EpubParser
import com.example.minicolibriosdk.sdk.HtmlTextExtractor
import com.example.minicolibriosdk.sdk.TextToSpeechManager
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var tts: TextToSpeechManager
    private lateinit var sentences: List<String>
    private var currentSentenceIndex = 0
    private var isStopped = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        val playBtn = findViewById<Button>(R.id.playButton)
        val stopBtn = findViewById<Button>(R.id.stopButton)

        val file = copyEpubFromAssets()
        val book = EpubParser.parseEpub(file)
        sentences = HtmlTextExtractor.extractSentences(book.chapters[4].content)
        val fullHtml = HtmlTextExtractor.buildHtmlWithWordSpans(sentences, this)

        val html = HtmlTextExtractor.buildHtmlWithWordSpans(sentences, this)
        webView.settings.javaScriptEnabled = true
        webView.loadDataWithBaseURL(null, fullHtml, "text/html", "utf-8", null)

        tts = TextToSpeechManager(this)

        playBtn.setOnClickListener {
            if (!tts.isSpeaking()) {
                isStopped = false
                currentSentenceIndex = 0
                speakNextSentence()
            }
        }

        stopBtn.setOnClickListener {
            isStopped = true
            tts.shutdown()
        }
    }

    private fun speakNextSentence() {
        if (isStopped || currentSentenceIndex >= sentences.size) return

        val sentence = sentences[currentSentenceIndex]
        val wordOffset = sentences.take(currentSentenceIndex).sumOf { it.split(" ").size }

        tts.speakSentenceWithWordHighlight(
            sentence,
            wordOffset,
            onHighlight = { wordIndex ->
                runOnUiThread {
                    webView.evaluateJavascript("highlightWord($wordIndex);", null)
                }
            },
            onDone = {
                currentSentenceIndex++
                speakNextSentence()
            }
        )
    }

    private fun copyEpubFromAssets(): File {
        val file = File(filesDir, "sample.epub")
        if (!file.exists()) {
            assets.open("sample.epub").use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return file
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
    }
}
