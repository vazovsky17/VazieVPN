package app.vazie.vpn.data.managed

import app.vazie.vpn.core.network.VazieApiClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The rendered form of a response is part of its contract. */
class ManagedDtoRedactionTest {

    @Test
    fun `no rendering of an access response can print credential material`() {
        val response = ManagedFixtures.access(expiresAt = "2026-12-31T23:59:59Z")

        val rendered = response.toString()

        assertFalse(rendered.contains(ManagedFixtures.CREDENTIAL), "the credential was printed")
        assertFalse(rendered.contains(ManagedFixtures.PUBLIC_KEY), "the REALITY public key was printed")
        assertFalse(rendered.contains(ManagedFixtures.SHORT_ID), "the REALITY short id was printed")
    }

    @Test
    fun `the profile and the security render nothing at all`() {
        // Nothing rather than "the safe fields": which fields are safe is a judgement that would have
        // to be made again every time somebody adds one, and that is the judgement that fails.
        assertEquals(ManagedProfileDto.REDACTED, ManagedFixtures.profile().toString())
        assertEquals(SecurityDto.REDACTED, ManagedFixtures.realitySecurity().toString())
    }

    @Test
    fun `redaction does not cost serialization`() {
        // The override changes how the object prints, not what it is. If this ever stopped holding,
        // the app would connect to nothing.
        val json = VazieApiClient.json
        val encoded = json.encodeToString(AccessResponseDto.serializer(), ManagedFixtures.access())
        val decoded = json.decodeFromString(AccessResponseDto.serializer(), encoded)

        assertTrue(encoded.contains(ManagedFixtures.CREDENTIAL), "the credential must survive the wire")
        assertEquals(ManagedFixtures.CREDENTIAL, decoded.profile.credential)
        assertEquals(ManagedFixtures.PUBLIC_KEY, decoded.profile.security.publicKey)
    }

    @Test
    fun `a field the backend adds later does not stop the response decoding`() {
        val json = VazieApiClient.json
        val withExtra = """
            {"id":"${ManagedFixtures.ACCESS_ID}","state":"ACTIVE","createdAt":"${ManagedFixtures.CREATED_AT}",
             "somethingAddedLater":true,
             "server":{"id":"${ManagedFixtures.SERVER_ID}","displayName":"Amsterdam","regionId":"nl-ams",
                       "countryCode":"NL","status":"AVAILABLE","protocols":["VLESS"]},
             "profile":{"protocol":"VLESS","endpoint":{"host":"${ManagedFixtures.HOST}","port":2053},
                        "credential":"${ManagedFixtures.CREDENTIAL}","flow":"xtls-rprx-vision",
                        "security":{"kind":"REALITY","publicKey":"${ManagedFixtures.PUBLIC_KEY}"},
                        "transport":{"kind":"TCP"}}}
        """.trimIndent()

        val decoded = json.decodeFromString(AccessResponseDto.serializer(), withExtra)

        assertEquals(ManagedFixtures.ACCESS_ID, decoded.id)
        assertTrue(decoded.profile.security.alpn.isEmpty())
    }
}
