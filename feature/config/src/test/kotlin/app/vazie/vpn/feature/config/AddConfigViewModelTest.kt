package app.vazie.vpn.feature.config

import app.vazie.vpn.core.model.Secret
import app.vazie.vpn.api.ProfileOrigin
import app.vazie.vpn.api.VpnProfile
import app.vazie.vpn.api.XrayOutbound
import app.vazie.vpn.config.ConfigParserRegistry
import app.vazie.vpn.config.InvalidReason
import app.vazie.vpn.config.UnsupportedFeature
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/** The wizard over the real parser and a repository double. */
@OptIn(ExperimentalCoroutinesApi::class)
class AddConfigViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val profiles = FakeProfileRepository()

    private val selected = FakeSelectedProfileStore()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `choosing a method opens the next step and waits for a link`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddConfigAction.SelectMethod(ImportMethodUi.PASTE))

        assertEquals(AddConfigEffect.OpenPreview, viewModel.effects.first())
        assertEquals(DetectionUiState.AwaitingInput, viewModel.state.value.detection)
    }

    /** Every method the type can name is one that works. */
    @Test
    fun `every import method there is opens the paste step`() = runTest(dispatcher) {
        ImportMethodUi.entries.forEach { method ->
            val viewModel = viewModel()

            viewModel.onAction(AddConfigAction.SelectMethod(method))

            assertEquals(AddConfigEffect.OpenPreview, viewModel.effects.first())
        }
    }

    /** The format phrase comes from the registry and survives entering the flow. */
    @Test
    fun `the advertised formats come from the parsers and survive a reset`() = runTest(dispatcher) {
        val viewModel = viewModel()
        assertEquals("VLESS", viewModel.state.value.supportedFormats)

        viewModel.onAction(AddConfigAction.SelectMethod(ImportMethodUi.PASTE))
        advanceUntilIdle()

        assertEquals("VLESS", viewModel.state.value.supportedFormats)
    }

    @Test
    fun `asking to paste asks the route for the clipboard`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(AddConfigAction.PasteRequested)

        assertEquals(AddConfigEffect.ReadClipboard, viewModel.effects.first())
    }

    @Test
    fun `a REALITY link becomes a preview of what it is`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.paste(ImportTestLinks.reality())

        val preview = assertIs<DetectionUiState.Recognized>(viewModel.state.value.detection).preview
        assertEquals("VLESS", preview.protocolLabel)
        assertEquals("REALITY", preview.securityLabel)
        assertEquals("TCP", preview.transportLabel)
        assertEquals("${ImportTestLinks.HOST}:443", preview.server)
        assertEquals("chrome", preview.fingerprint)
        assertEquals("xtls-rprx-vision", preview.flow)
        assertEquals(listOf("packetEncoding"), preview.keptParameters)
        assertEquals("Home relay", viewModel.state.value.name)
        assertTrue(viewModel.state.value.canSave)
    }

    @Test
    fun `a link with no name of its own is named after its host`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.paste(ImportTestLinks.reality(fragment = null))

        assertEquals(ImportTestLinks.HOST, viewModel.state.value.name)
    }

    @Test
    fun `a transport Vazie cannot run is unsupported, not invalid`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.paste(ImportTestLinks.link(query = "type=kcp"))

        val detection = assertIs<DetectionUiState.Unsupported>(viewModel.state.value.detection)
        assertEquals(UnsupportedFeature.Transport("kcp"), detection.feature)
        assertEquals("VLESS", detection.protocolLabel)
        assertFalse(viewModel.state.value.canSave)
    }

    @Test
    fun `a link Vazie cannot read is invalid, with the reason it failed`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.paste(ImportTestLinks.link(userId = "not-a-uuid"))

        val detection = assertIs<DetectionUiState.Invalid>(viewModel.state.value.detection)
        assertEquals(InvalidReason.MalformedUserId, detection.reason)
        assertFalse(viewModel.state.value.canSave)
    }

    @Test
    fun `an empty clipboard says so rather than nothing`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.paste("")

        assertEquals(
            InvalidReason.Empty,
            assertIs<DetectionUiState.Invalid>(viewModel.state.value.detection).reason,
        )
    }

    @Test
    fun `a blank name blocks saving`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.paste(ImportTestLinks.reality())

        viewModel.onAction(AddConfigAction.NameChanged("   "))

        assertFalse(viewModel.state.value.canSave)
    }

    @Test
    fun `an outcome that is not recognised cannot continue`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.paste(ImportTestLinks.link(query = "type=kcp"))

        viewModel.onAction(AddConfigAction.Continue)
        advanceUntilIdle()

        assertTrue(profiles.profiles.isEmpty())
    }

    @Test
    fun `saving stores the profile and moves to the last step`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.paste(ImportTestLinks.reality())
        viewModel.onAction(AddConfigAction.NameChanged("Home relay"))

        viewModel.onAction(AddConfigAction.Save)
        advanceUntilIdle()

        val profile = assertIs<VpnProfile.Xray>(profiles.profiles.single())
        assertEquals("Home relay", profile.name)
        assertEquals(ProfileOrigin.IMPORTED_LINK, profile.origin)
        val outbound = assertIs<XrayOutbound.Vless>(profile.outbound)
        assertEquals(ImportTestLinks.USER_ID, outbound.userId.expose())
        assertEquals(listOf("packetEncoding"), outbound.unknownParameters.names)
        assertEquals(AddConfigEffect.OpenDone, viewModel.effects.first())
    }

    @Test
    fun `the stored profile id is Vazie's own and not the one from the link`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            viewModel.paste(ImportTestLinks.reality())

            viewModel.onAction(AddConfigAction.Save)
            advanceUntilIdle()

            assertTrue(profiles.profiles.single().id.value != ImportTestLinks.USER_ID)
        }

    @Test
    fun `a link Vazie could not read is never stored`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.paste(ImportTestLinks.link(userId = "not-a-uuid"))

        viewModel.onAction(AddConfigAction.Save)
        advanceUntilIdle()

        assertTrue(profiles.profiles.isEmpty())
    }

    @Test
    fun `saving selects what was just imported`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.paste(ImportTestLinks.reality())
        viewModel.onAction(AddConfigAction.NameChanged("Home relay"))

        viewModel.onAction(AddConfigAction.Save)
        advanceUntilIdle()

        val stored = profiles.profiles.single()
        assertEquals(
            stored.id,
            selected.selected(),
            "the wizard added a configuration and armed a different one",
        )
    }

    @Test
    fun `the wizard's state never holds the link it read`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.paste(ImportTestLinks.reality())

        val rendered = viewModel.state.value.toString()
        assertTrue(ImportTestLinks.USER_ID !in rendered, "the user id reached the state")
        assertTrue(ImportTestLinks.PUBLIC_KEY !in rendered, "the REALITY key reached the state")
        assertTrue("packetEncoding=xudp" !in rendered, "a preserved value reached the state")
    }

    private fun viewModel() = AddConfigViewModel(
        parsers = ConfigParserRegistry.default(),
        profiles = profiles,
        selected = selected,
    )

    private fun AddConfigViewModel.paste(raw: String) = onPasted(Secret.of(raw))
}
