package ink.trmnl.android.buddy.content

import com.prof18.rssparser.RssParser

/**
 * Factory for creating RssParser instances.
 * This wrapper works around a Metro compiler issue with RssParser's constructor.
 */
object RssParserFactory {
    fun create(): RssParser = RssParser()
}
