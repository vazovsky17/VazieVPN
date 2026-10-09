package app.vazie.vpn.data.managed

import app.vazie.vpn.api.VpnProfile
import app.vazie.vpn.api.XrayFlow
import app.vazie.vpn.api.XrayOutbound
import app.vazie.vpn.api.XraySecurity
import app.vazie.vpn.api.XrayTransport
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The mapping the whole managed path rests on, field by field. */
class ManagedProfileMapperTest {

    @Test
    fun `the Amsterdam profile maps field for field`() {
        val mapped = assertIs<ManagedProfileMapping.Mapped>(
            ManagedProfileMapper.map(ManagedFixtures.access()),
        )
        val profile = assertIs<VpnProfile.Xray>(mapped.material.profile)
        val outbound = assertIs<XrayOutbound.Vless>(profile.outbound)

        assertEquals(ManagedFixtures.CREDENTIAL, outbound.userId.expose())
        assertEquals(ManagedFixtures.HOST, outbound.endpoint.host)
        assertEquals(2053, outbound.endpoint.port)
        assertEquals(XrayFlow.XTLS_RPRX_VISION, outbound.flow)

        val security = assertIs<XraySecurity.Reality>(outbound.security)
        assertEquals(ManagedFixtures.SERVER_NAME, security.serverName)
        assertEquals("chrome", security.fingerprint)
        assertEquals(ManagedFixtures.PUBLIC_KEY, security.publicKey.expose())
        assertEquals(ManagedFixtures.SHORT_ID, security.shortId?.expose())
        // Absent rather than invented: the engine substitutes its own default, and a second default
        // here could disagree with it.
        assertNull(security.spiderX)

        val transport = assertIs<XrayTransport.Tcp>(outbound.transport)
        assertNull(transport.headerType)

        assertTrue(outbound.unknownParameters.isEmpty)
    }

    @Test
    fun `a managed profile is not stored and says so`() {
        val mapped = assertIs<ManagedProfileMapping.Mapped>(
            ManagedProfileMapper.map(ManagedFixtures.access()),
        )
        val profile = mapped.material.profile

        // No origin, because it was never stored - and the profile store refuses to write one.
        assertNull(profile.origin)
        assertEquals("vazie-managed-${ManagedFixtures.ACCESS_ID}", profile.id.value)
        assertEquals("Amsterdam", profile.name)
        assertEquals(Instant.parse(ManagedFixtures.CREATED_AT), profile.createdAt)
    }

    @Test
    fun `an entitlement window survives when the backend states one`() {
        val mapped = assertIs<ManagedProfileMapping.Mapped>(
            ManagedProfileMapper.map(ManagedFixtures.access(expiresAt = "2026-12-31T23:59:59Z")),
        )

        assertEquals(Instant.parse("2026-12-31T23:59:59Z"), mapped.material.access.expiresAt)
    }

    @Test
    fun `an absent short id and an absent spiderX are absent, not empty strings`() {
        val mapped = assertIs<ManagedProfileMapping.Mapped>(
            ManagedProfileMapper.map(
                ManagedFixtures.access(
                    profile = ManagedFixtures.profile(
                        security = ManagedFixtures.realitySecurity(shortId = null),
                    ),
                ),
            ),
        )
        val outbound = assertIs<XrayOutbound.Vless>(
            assertIs<VpnProfile.Xray>(mapped.material.profile).outbound,
        )

        assertNull(assertIs<XraySecurity.Reality>(outbound.security).shortId)
    }

    @Test
    fun `a blank short id reads as absent, because the engine writes the same thing for both`() {
        val mapped = assertIs<ManagedProfileMapping.Mapped>(
            ManagedProfileMapper.map(
                ManagedFixtures.access(
                    profile = ManagedFixtures.profile(
                        security = ManagedFixtures.realitySecurity(shortId = "   "),
                    ),
                ),
            ),
        )
        val outbound = assertIs<XrayOutbound.Vless>(
            assertIs<VpnProfile.Xray>(mapped.material.profile).outbound,
        )

        assertNull(assertIs<XraySecurity.Reality>(outbound.security).shortId)
    }

    @Test
    fun `a spiderX the operator chose is carried rather than replaced`() {
        val mapped = assertIs<ManagedProfileMapping.Mapped>(
            ManagedProfileMapper.map(
                ManagedFixtures.access(
                    profile = ManagedFixtures.profile(
                        security = ManagedFixtures.realitySecurity(spiderX = "/probe"),
                    ),
                ),
            ),
        )
        val outbound = assertIs<XrayOutbound.Vless>(
            assertIs<VpnProfile.Xray>(mapped.material.profile).outbound,
        )

        assertEquals("/probe", assertIs<XraySecurity.Reality>(outbound.security).spiderX)
    }

    @Test
    fun `an absent flow is absent, and a blank one means the same`() {
        listOf(null, "", "   ").forEach { raw ->
            val mapped = assertIs<ManagedProfileMapping.Mapped>(
                ManagedProfileMapper.map(
                    ManagedFixtures.access(profile = ManagedFixtures.profile(flow = raw)),
                ),
                "flow $raw should have mapped",
            )
            val outbound = assertIs<XrayOutbound.Vless>(
                assertIs<VpnProfile.Xray>(mapped.material.profile).outbound,
            )
            assertNull(outbound.flow)
        }
    }

    @Test
    fun `a TLS security keeps its ALPN list`() {
        // The model has an ALPN field for TLS and not for REALITY, and this is the half that works.
        val mapped = assertIs<ManagedProfileMapping.Mapped>(
            ManagedProfileMapper.map(
                ManagedFixtures.access(
                    profile = ManagedFixtures.profile(
                        security = SecurityDto(
                            kind = "TLS",
                            serverName = ManagedFixtures.SERVER_NAME,
                            fingerprint = "chrome",
                            alpn = listOf("h2", "http/1.1"),
                        ),
                    ),
                ),
            ),
        )
        val outbound = assertIs<XrayOutbound.Vless>(
            assertIs<VpnProfile.Xray>(mapped.material.profile).outbound,
        )

        assertEquals(listOf("h2", "http/1.1"), assertIs<XraySecurity.Tls>(outbound.security).alpn)
    }

    @Test
    fun `a websocket transport keeps its path and host`() {
        val mapped = assertIs<ManagedProfileMapping.Mapped>(
            ManagedProfileMapper.map(
                ManagedFixtures.access(
                    profile = ManagedFixtures.profile(
                        flow = null,
                        security = SecurityDto(kind = "TLS", serverName = ManagedFixtures.SERVER_NAME),
                        transport = TransportDto(kind = "WEBSOCKET", path = "/vazie", host = "cdn.example.net"),
                    ),
                ),
            ),
        )
        val outbound = assertIs<XrayOutbound.Vless>(
            assertIs<VpnProfile.Xray>(mapped.material.profile).outbound,
        )

        val transport = assertIs<XrayTransport.WebSocket>(outbound.transport)
        assertEquals("/vazie", transport.path)
        assertEquals("cdn.example.net", transport.host)
    }

    @Test
    fun `a grpc transport keeps its service name and mode`() {
        val mapped = assertIs<ManagedProfileMapping.Mapped>(
            ManagedProfileMapper.map(
                ManagedFixtures.access(
                    profile = ManagedFixtures.profile(
                        flow = null,
                        security = SecurityDto(kind = "TLS"),
                        transport = TransportDto(kind = "GRPC", serviceName = "vazie", multiMode = true),
                    ),
                ),
            ),
        )
        val outbound = assertIs<XrayOutbound.Vless>(
            assertIs<VpnProfile.Xray>(mapped.material.profile).outbound,
        )

        val transport = assertIs<XrayTransport.Grpc>(outbound.transport)
        assertEquals("vazie", transport.serviceName)
        assertTrue(transport.multiMode)
    }

    @Test
    fun `a tcp header type is carried`() {
        val mapped = assertIs<ManagedProfileMapping.Mapped>(
            ManagedProfileMapper.map(
                ManagedFixtures.access(
                    profile = ManagedFixtures.profile(transport = TransportDto(kind = "TCP", headerType = "http")),
                ),
            ),
        )
        val outbound = assertIs<XrayOutbound.Vless>(
            assertIs<VpnProfile.Xray>(mapped.material.profile).outbound,
        )

        assertEquals("http", assertIs<XrayTransport.Tcp>(outbound.transport).headerType)
    }
}
