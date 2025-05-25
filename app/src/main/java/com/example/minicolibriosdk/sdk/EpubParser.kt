package com.example.minicolibriosdk.sdk

import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

object EpubParser {
    data class EpubBook(
        val title: String,
        val chapters: List<Chapter>
    )

    data class Chapter(
        val id: String,
        val href: String,
        val content: String
    )

    fun parseEpub(file: File): EpubBook {
        val zip = ZipFile(file)
        val containerXml = zip.getInputStream(zip.getEntry("META-INF/container.xml"))
        val containerDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(containerXml)
        val opfPath = containerDoc.getElementsByTagName("rootfile")
            .item(0).attributes.getNamedItem("full-path").nodeValue

        val opfStream = zip.getInputStream(zip.getEntry(opfPath))
        val opfDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(opfStream)

        val title = opfDoc.getElementsByTagName("dc:title").item(0)?.textContent ?: "Untitled"
        val spine = opfDoc.getElementsByTagName("itemref")
        val manifest = opfDoc.getElementsByTagName("item")

        val idToHref = mutableMapOf<String, String>()
        for (i in 0 until manifest.length) {
            val item = manifest.item(i)
            val id = item.attributes.getNamedItem("id").nodeValue
            val href = item.attributes.getNamedItem("href").nodeValue
            idToHref[id] = href
        }

        val basePath = opfPath.substringBeforeLast('/') + "/"
        val chapters = mutableListOf<Chapter>()
        for (i in 0 until spine.length) {
            val itemref = spine.item(i)
            val idref = itemref.attributes.getNamedItem("idref").nodeValue
            val href = idToHref[idref] ?: continue
            val entry = zip.getEntry(basePath + href) ?: continue
            val html = zip.getInputStream(entry).bufferedReader().readText()
            chapters.add(Chapter(idref, href, html))
        }

        zip.close()
        return EpubBook(title, chapters)
    }
}
