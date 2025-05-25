package com.example.minicolibriosdk.sdk

import android.content.Context

object HtmlTextExtractor {

    // Common abbreviations that shouldn't be split
    private val abbreviations = listOf(
        "Mr.", "Mrs.", "Ms.", "Dr.", "Prof.", "e.g.", "i.e.", "etc.", "vs.", "Sr.", "Jr."
    )

    fun extractSentences(html: String): List<String> {
        val rawText = html
            .replace(Regex("<(script|style)[^>]*>.*?</\\1>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("&[a-z]+;"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        // Break into sentence candidates
        val candidates = rawText.split(Regex("(?<=[.?!])\\s+"))
        val result = mutableListOf<String>()

        var buffer = ""
        for (candidate in candidates) {
            val trimmed = candidate.trim()
            if (trimmed.isEmpty()) continue

            // Add to buffer
            buffer = if (buffer.isEmpty()) {
                trimmed
            } else {
                "$buffer $trimmed"
            }

            val lastWord = trimmed.split(" ").lastOrNull() ?: ""
            val endsWithAbbrev = abbreviations.any { trimmed.endsWith(it) }

            if (!endsWithAbbrev && trimmed.length > 20) {
                result.add(buffer.trim())
                buffer = ""
            }
        }

        if (buffer.isNotBlank()) result.add(buffer.trim())

        // Optional: merge very short sentences
        val final = mutableListOf<String>()
        var temp = ""
        for (sentence in result) {
            if (sentence.length < 25) {
                temp += " $sentence"
            } else {
                if (temp.isNotBlank()) {
                    final.add(temp.trim())
                    temp = ""
                }
                final.add(sentence)
            }
        }
        if (temp.isNotBlank()) final.add(temp.trim())

        return final
    }

    fun buildHtmlWithWordSpans(sentences: List<String>, context: Context): String {
        val sb = StringBuilder()
        var wordIndex = 0

        for (sentence in sentences) {
            val words = sentence.trim().split(" ")
            for (word in words) {
                sb.append("<span class='word' id='w$wordIndex'>$word </span>")
                wordIndex++
            }
        }

        val htmlTemplate = context.assets.open("highlight_template_word.html").bufferedReader().use {
            it.readText()
        }

        return htmlTemplate.replace("{{content}}", sb.toString())
    }


}

