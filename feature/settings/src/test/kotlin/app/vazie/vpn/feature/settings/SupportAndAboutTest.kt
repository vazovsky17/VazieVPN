package app.vazie.vpn.feature.settings

import androidx.compose.ui.test.isHeading
import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import app.vazie.vpn.core.designsystem.preview.VaziePreviewTheme
import app.vazie.vpn.core.designsystem.theme.Appearance
import app.vazie.vpn.core.model.VazieAppIcon
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Support and About are two things, and the help text is readable. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h760dp-xhdpi")
class SupportAndAboutTest {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `Support and About are separate sections`() {
        settings()

        // Headers uppercase their text, so a case-sensitive match tells the header from the row.
        compose.onNode(hasText(string(R.string.settings_help_section)) and isHeading())
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNode(hasText(string(R.string.settings_about_section)) and isHeading())
            .performScrollTo()
            .assertIsDisplayed()
    }

    /** Support holds feedback; About holds privacy, the build and the developer. */
    @Test
    fun `the feedback row is under Support and the rest under About`() {
        settings()

        // Read without scrolling: a scrolling Column lays out every child anyway.
        val supportHeader = compose.onNode(hasText(string(R.string.settings_help_section)) and isHeading()).top()
        val feedbackRow = compose.onNodeWithText(string(R.string.settings_report_bug)).top()
        val aboutHeader = compose.onNode(hasText(string(R.string.settings_about_section)) and isHeading()).top()
        val privacyRow = compose.onNodeWithText(string(R.string.settings_privacy_row)).top()

        assertTrue(supportHeader < feedbackRow, "the feedback row is not under the Support header")
        assertTrue(feedbackRow < aboutHeader, "Support and About are interleaved")
        assertTrue(aboutHeader < privacyRow, "privacy is not under the About header")
    }

    /** The feedback subtitle is on screen in full, in both languages. */
    @Test
    fun `the feedback description is not truncated in English`() {
        settings()
        assertUntruncated(string(R.string.settings_report_bug_body))
    }

    /** Russian is the case that matters: it runs about a third longer than English. */
    @Test
    @Config(sdk = [34], qualifiers = "ru-rRU-w360dp-h760dp-xhdpi")
    fun `the feedback description is not truncated in Russian`() {
        settings()
        assertUntruncated(string(R.string.settings_report_bug_body))
    }

    /** The developer card carries the picture its text is about, read from the content description because
     * that is the part a screen reader gets. */
    @Test
    fun `About shows Vazovsky beside his own card`() {
        about()

        compose.onNodeWithText(string(R.string.settings_made_by_title))
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithContentDescription(
            context.getString(DesignSystemStrings.VAZOVSKY_STICKER),
        ).performScrollTo().assertIsDisplayed()
    }

    /** The feedback row is in one place, not two. */
    @Test
    fun `About does not carry the feedback row`() {
        about()
        assertEquals(
            0,
            compose.onAllNodes(hasText(string(R.string.settings_report_bug))).fetchSemanticsNodes().size,
            "the feedback row is on About as well as in Settings",
        )
    }

    @Test
    fun `pressing the feedback row asks to open the form`() {
        val actions = mutableListOf<SettingsAction>()
        settings { actions += it }

        compose.onNodeWithText(string(R.string.settings_report_bug)).performScrollTo().performClick()

        assertEquals<List<SettingsAction>>(listOf(SettingsAction.ReportBug), actions)
    }

    /** The subtitle node reports the whole string. */
    private fun assertUntruncated(expected: String) {
        val node = compose.onNodeWithText(expected, useUnmergedTree = true).performScrollTo()
        val shown = node.fetchSemanticsNode().config
            .getOrNull(SemanticsProperties.Text)
            .orEmpty()
            .joinToString("") { it.text }
        assertEquals(expected, shown, "the help description is cut off")
        node.assertIsDisplayed()
    }

    private fun SemanticsNodeInteraction.top(): Float =
        fetchSemanticsNode().positionInRoot.y

    private fun string(id: Int): String = context.getString(id)

    private fun settings(onAction: (SettingsAction) -> Unit = {}) {
        compose.setContent {
            VaziePreviewTheme(Appearance.NIGHT_INDIGO) {
                SettingsScreen(
                    state = SettingsUiState(
                        appearance = Appearance.NIGHT_INDIGO,
                        appIcon = VazieAppIcon.ORBIT,
                        systemIntegration = SystemIntegrationUi(),
                    ),
                    onAction = onAction,
                )
            }
        }
        compose.waitForIdle()
    }

    private fun about() {
        compose.setContent {
            VaziePreviewTheme(Appearance.NIGHT_INDIGO) {
                AboutScreen(
                    state = AboutUiState(about = AboutUi("1.0", 1, "play", "debug")),
                    onAction = {},
                )
            }
        }
        compose.waitForIdle()
    }

    /** The sticker's description lives in `:core:designsystem`, whose `R` this module cannot name directly
     * without an import that reads worse than the indirection. */
    private object DesignSystemStrings {
        val VAZOVSKY_STICKER =
            app.vazie.vpn.core.designsystem.R.string.vazie_a11y_vazovsky_sticker
    }
}
