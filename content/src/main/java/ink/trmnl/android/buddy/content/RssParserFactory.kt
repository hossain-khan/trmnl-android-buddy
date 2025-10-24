package ink.trmnl.android.buddy.content

import com.prof18.rssparser.RssParser

/**
 * Factory for creating RssParser instances.
 * This is a workaround for Metro compiler limitations with third-party types.
 */
object RssParserFactory {
    fun create(): RssParser = RssParser()
}
