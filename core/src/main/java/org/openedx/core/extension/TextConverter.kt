package org.openedx.core.extension

import android.util.Patterns
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.openedx.core.config.Config
import org.openedx.core.utils.Logger

object TextConverter : KoinComponent {

    private const val TAG = "TextConverter"
    private val logger = Logger(TAG)
    private val config by inject<Config>()

    fun htmlTextToLinkedText(html: String): LinkedText {
        val doc: Document =
            Jsoup.parse(html)
        val links: Elements = doc.select("a[href]")
        val text = doc.text()
        val linksMap = mutableMapOf<String, String>()
        for (link in links) {
            var resultLink = if (link.attr("href").isNotEmpty() && link.attr("href")[0] == '/') {
                link.attr("href").substring(1)
            } else {
                link.attr("href")
            }
            if (!resultLink.startsWith("http")) {
                resultLink = config.getApiHostURL() + resultLink
            }
            if (resultLink.isNotEmpty() && isLinkValid(resultLink)) {
                linksMap[link.text()] = resultLink
            }
        }
        return LinkedText(text, linksMap.toMap())
    }

    fun isLinkValid(link: String) = Patterns.WEB_URL.matcher(link.lowercase()).matches()

    private fun getHeaders(document: Document): List<String> {
        val headersList = mutableListOf<String>()
        for (index in 1..6) {
            if (document.select("h$index").hasText()) {
                headersList.add(document.select("h$index").text())
            }
        }
        return headersList.toList()
    }

    private fun setSpacesForHeaders(text: String, headers: List<String>): String {
        var result = text
        try {
            headers.forEach {
                val startIndex = text.indexOf(it)
                val endIndex = startIndex + it.length + 1
                result = text.replaceRange(startIndex, endIndex, it + "\n")
            }
        } catch (e: Exception) {
            logger.e(throwable = e, metadata = mapOf("text" to text))
        }
        return result
    }

    private fun getImageLinks(document: Document): Map<String, String> {
        val imageLinks = mutableMapOf<String, String>()
        val elements = document.getElementsByTag("img")
        for (element in elements) {
            if (element.hasAttr("alt")) {
                imageLinks[element.attr("alt")] = element.attr("src")
            } else {
                imageLinks[element.attr("src")] = element.attr("src")
            }
        }
        return imageLinks.toMap()
    }
}

data class LinkedText(
    val text: String,
    val links: Map<String, String>
)
