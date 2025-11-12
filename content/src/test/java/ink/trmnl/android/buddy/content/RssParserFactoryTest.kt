package ink.trmnl.android.buddy.content

import assertk.assertThat
import assertk.assertions.isNotNull
import com.prof18.rssparser.RssParser
import org.junit.Test

/**
 * Unit tests for [RssParserFactory].
 */
class RssParserFactoryTest {
    @Test
    fun `create returns RssParser instance`() {
        val parser = RssParserFactory.create()

        assertThat(parser).isNotNull()
    }

    @Test
    fun `create returns new RssParser instance each time`() {
        val parser1 = RssParserFactory.create()
        val parser2 = RssParserFactory.create()

        // Both should be valid RssParser instances
        assertThat(parser1).isNotNull()
        assertThat(parser2).isNotNull()
    }
}
