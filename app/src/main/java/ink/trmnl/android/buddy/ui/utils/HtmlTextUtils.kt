package ink.trmnl.android.buddy.ui.utils

import android.text.Spanned
import android.text.style.URLSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.text.HtmlCompat

/**
 * Convert HTML string to AnnotatedString for Compose Text.
 *
 * Supports basic HTML tags like <br/>, <a>, <b>, <i>, etc.
 *
 * @param html HTML string to convert
 * @param linkColor Color for hyperlinks
 * @return AnnotatedString with styled text and links
 */
fun htmlToAnnotatedString(
    html: String,
    linkColor: Color = Color.Blue,
): AnnotatedString {
    val spanned = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
    return buildAnnotatedString {
        append(spanned.toString())

        // Apply link styles
        spanned.getSpans(0, spanned.length, URLSpan::class.java).forEach { urlSpan ->
            val start = spanned.getSpanStart(urlSpan)
            val end = spanned.getSpanEnd(urlSpan)
            addStyle(
                style =
                    SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline,
                    ),
                start = start,
                end = end,
            )
            addStringAnnotation(
                tag = "URL",
                annotation = urlSpan.url,
                start = start,
                end = end,
            )
        }
    }
}

/**
 * Check if HTML content contains any HTML tags.
 *
 * @param text Text to check
 * @return true if text contains HTML tags
 */
fun isHtml(text: String): Boolean = text.contains("<") && text.contains(">")
