package ink.trmnl.android.buddy.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ink.trmnl.android.buddy.ui.theme.ebGaramondFontFamily

/**
 * Composable for displaying app bar titles with the TRMNL brand font (EB Garamond).
 * This component ensures consistent typography across all navigation titles.
 *
 * @param text The title text to display
 * @param modifier Optional modifier for customization
 */
@Composable
fun TrmnlTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        fontFamily = ebGaramondFontFamily,
        modifier = modifier,
    )
}
