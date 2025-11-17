package ink.trmnl.android.buddy.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme
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

// Preview Composables
@PreviewLightDark
@Preview(name = "TRMNL Title - Short Text", showBackground = true)
@Composable
private fun TrmnlTitleShortPreview() {
    TrmnlBuddyAppTheme {
        TrmnlTitle(text = "TRMNL")
    }
}

@PreviewLightDark
@Preview(name = "TRMNL Title - Long Text", showBackground = true)
@Composable
private fun TrmnlTitleLongPreview() {
    TrmnlBuddyAppTheme {
        TrmnlTitle(text = "Device Details - Battery History")
    }
}
