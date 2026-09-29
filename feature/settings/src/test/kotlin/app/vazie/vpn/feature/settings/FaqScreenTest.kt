package app.vazie.vpn.feature.settings

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import app.vazie.vpn.core.designsystem.preview.VaziePreviewTheme
import app.vazie.vpn.core.designsystem.theme.Appearance
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Questions and answers: every question is on the screen, and the refund answer opens the site's refunds page. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FaqScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val opened = mutableListOf<String>()

    private fun show() {
        compose.setContent {
            val handler = object : UriHandler {
                override fun openUri(uri: String) {
                    opened += uri
                }
            }
            CompositionLocalProvider(LocalUriHandler provides handler) {
                VaziePreviewTheme(Appearance.NIGHT_INDIGO) {
                    FaqScreen(onBack = {})
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `every question is shown`() {
        show()
        FaqItems.forEach { item ->
            compose.onNodeWithText(context.getString(item.question)).performScrollTo()
        }
    }

    @Test
    fun `the refund answer opens the refunds page on the site`() {
        show()
        compose.onNodeWithText(context.getString(R.string.faq_refund_link)).performScrollTo().performClick()
        assertEquals(listOf("https://site.example/legal/refunds"), opened)
    }
}
