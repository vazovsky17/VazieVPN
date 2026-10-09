package app.vazie.vpn.data.managed

import app.vazie.vpn.api.UnsupportedRuntimeFeature
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Everything the mapper refuses, and the reason each refusal is better than the alternative. */
class ManagedProfileRejectionTest {

    @Test
    fun `REALITY with an ALPN list is unsupported rather than silently stripped`() {
        // The model has no ALPN for REALITY. Today's Amsterdam server sends none; the day one does,
        // Vazie must say so rather than negotiate a handshake nobody configured.
        val result = ManagedProfileMapper.map(
            ManagedFixtures.access(
                profile = ManagedFixtures.profile(
                    security = ManagedFixtures.realitySecurity(alpn = listOf("h2")),
                ),
            ),
        )

        assertEquals(UnsupportedRuntimeFeature.SECURITY, result.unsupportedFeature())
    }

    @Test
    fun `a flow this build does not implement is unsupported, never dropped`() {
        val result = ManagedProfileMapper.map(
            ManagedFixtures.access(profile = ManagedFixtures.profile(flow = "xtls-rprx-direct")),
        )

        assertEquals(UnsupportedRuntimeFeature.FLOW, result.unsupportedFeature())
    }

    @Test
    fun `a security kind this build does not know is unsupported`() {
        val result = ManagedProfileMapper.map(
            ManagedFixtures.access(profile = ManagedFixtures.profile(security = SecurityDto(kind = "SOMETHING"))),
        )

        assertEquals(UnsupportedRuntimeFeature.SECURITY, result.unsupportedFeature())
    }

    @Test
    fun `a transport kind this build does not know is unsupported`() {
        val result = ManagedProfileMapper.map(
            ManagedFixtures.access(
                profile = ManagedFixtures.profile(transport = TransportDto(kind = "HTTPUPGRADE")),
            ),
        )

        assertEquals(UnsupportedRuntimeFeature.TRANSPORT, result.unsupportedFeature())
    }

    @Test
    fun `a protocol other than VLESS is unsupported`() {
        val result = ManagedProfileMapper.map(
            ManagedFixtures.access(profile = ManagedFixtures.profile(protocol = "TROJAN")),
        )

        assertEquals(UnsupportedRuntimeFeature.PROTOCOL, result.unsupportedFeature())
    }

    @Test
    fun `a NONE security carrying anything at all is unsupported`() {
        // `XraySecurity.None` has no fields, so every one of these would be dropped on the way in.
        listOf(
            SecurityDto(kind = "NONE", serverName = "x"),
            SecurityDto(kind = "NONE", fingerprint = "chrome"),
            SecurityDto(kind = "NONE", alpn = listOf("h2")),
            SecurityDto(kind = "NONE", publicKey = "x"),
            SecurityDto(kind = "NONE", shortId = "x"),
            SecurityDto(kind = "NONE", spiderX = "/"),
        ).forEach { security ->
            val result = ManagedProfileMapper.map(
                ManagedFixtures.access(profile = ManagedFixtures.profile(flow = null, security = security)),
            )
            assertEquals(
                UnsupportedRuntimeFeature.SECURITY,
                result.unsupportedFeature(),
                "a NONE security carrying $security should have been refused",
            )
        }
    }

    @Test
    fun `REALITY fields on a TLS security are unsupported`() {
        listOf(
            SecurityDto(kind = "TLS", publicKey = "x"),
            SecurityDto(kind = "TLS", shortId = "x"),
            SecurityDto(kind = "TLS", spiderX = "/"),
        ).forEach { security ->
            val result = ManagedProfileMapper.map(
                ManagedFixtures.access(profile = ManagedFixtures.profile(flow = null, security = security)),
            )
            assertEquals(UnsupportedRuntimeFeature.SECURITY, result.unsupportedFeature())
        }
    }

    @Test
    fun `a field belonging to another transport is unsupported`() {
        listOf(
            TransportDto(kind = "TCP", path = "/vazie"),
            TransportDto(kind = "TCP", host = "cdn.example.net"),
            TransportDto(kind = "TCP", serviceName = "vazie"),
            TransportDto(kind = "TCP", multiMode = true),
            TransportDto(kind = "WEBSOCKET", path = "/vazie", headerType = "http"),
            TransportDto(kind = "WEBSOCKET", path = "/vazie", serviceName = "vazie"),
            TransportDto(kind = "GRPC", serviceName = "vazie", path = "/vazie"),
            TransportDto(kind = "GRPC", serviceName = "vazie", headerType = "http"),
        ).forEach { transport ->
            val result = ManagedProfileMapper.map(
                ManagedFixtures.access(profile = ManagedFixtures.profile(transport = transport)),
            )
            assertEquals(
                UnsupportedRuntimeFeature.TRANSPORT,
                result.unsupportedFeature(),
                "$transport should have been refused",
            )
        }
    }

    @Test
    fun `a response that is not the contract is malformed, not unsupported`() {
        // The distinction matters to the user: unsupported is something Vazie can explain, malformed
        // is a backend fault with nothing truthful to say beyond that it happened.
        listOf(
            ManagedFixtures.access(profile = ManagedFixtures.profile(port = 0)),
            ManagedFixtures.access(profile = ManagedFixtures.profile(port = 70000)),
            ManagedFixtures.access(profile = ManagedFixtures.profile(credential = "   ")),
            ManagedFixtures.access(
                profile = ManagedFixtures.profile(
                    security = ManagedFixtures.realitySecurity(publicKey = null),
                ),
            ),
            ManagedFixtures.access(
                profile = ManagedFixtures.profile(
                    flow = null,
                    security = SecurityDto(kind = "TLS"),
                    transport = TransportDto(kind = "WEBSOCKET"),
                ),
            ),
            ManagedFixtures.access(
                profile = ManagedFixtures.profile(
                    flow = null,
                    security = SecurityDto(kind = "TLS"),
                    transport = TransportDto(kind = "GRPC"),
                ),
            ),
        ).forEach { response ->
            assertIs<ManagedProfileMapping.Malformed>(
                ManagedProfileMapper.map(response),
                "expected a malformed response",
            )
        }
    }

    @Test
    fun `a state this build cannot represent refuses the material`() {
        // REVOKED is the interesting one: material for a revoked access is a contradiction, and connecting
        // with a credential the backend just said is gone produces a tunnel that dies without explanation.
        listOf("REVOKED", "SOMETHING_NEW", "").forEach { state ->
            assertIs<ManagedProfileMapping.Malformed>(
                ManagedProfileMapper.map(ManagedFixtures.access(state = state)),
                "state $state should have been refused",
            )
        }
    }

    @Test
    fun `an unreadable timestamp is malformed`() {
        val response = ManagedFixtures.access().copy(createdAt = "the day before yesterday")

        assertIs<ManagedProfileMapping.Malformed>(ManagedProfileMapper.map(response))
    }

    private fun ManagedProfileMapping.unsupportedFeature(): UnsupportedRuntimeFeature =
        assertIs<ManagedProfileMapping.Unsupported>(this).feature
}
