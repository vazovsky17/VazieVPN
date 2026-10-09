package app.vazie.vpn.data.managed

/** The Amsterdam managed server, shaped exactly as the backend describes it and built entirely from values
 * that open nothing. */
internal object ManagedFixtures {

    const val ACCESS_ID = "00000000-0000-4000-8000-0000000000a1"
    const val SERVER_ID = "00000000-0000-4000-8000-0000000000a2"
    const val CREDENTIAL = "00000000-0000-4000-8000-0000000000c1"
    const val PUBLIC_KEY = "synthetic-reality-public-key-not-a-real-one"
    const val SHORT_ID = "0011223344556677"
    const val HOST = "203.0.113.10"
    const val SERVER_NAME = "www.example.com"
    const val CREATED_AT = "2026-08-31T10:00:00Z"

    fun server(
        status: String = "AVAILABLE",
        protocols: List<String> = listOf("VLESS"),
    ): ServerDto = ServerDto(
        id = SERVER_ID,
        displayName = "Amsterdam",
        regionId = "nl-ams",
        countryCode = "NL",
        city = "Amsterdam",
        status = status,
        protocols = protocols,
    )

    fun realitySecurity(
        alpn: List<String> = emptyList(),
        shortId: String? = SHORT_ID,
        spiderX: String? = null,
        publicKey: String? = PUBLIC_KEY,
    ): SecurityDto = SecurityDto(
        kind = "REALITY",
        serverName = SERVER_NAME,
        fingerprint = "chrome",
        alpn = alpn,
        publicKey = publicKey,
        shortId = shortId,
        spiderX = spiderX,
    )

    fun profile(
        protocol: String = "VLESS",
        credential: String = CREDENTIAL,
        flow: String? = "xtls-rprx-vision",
        security: SecurityDto = realitySecurity(),
        transport: TransportDto = TransportDto(kind = "TCP"),
        port: Int = 2053,
    ): ManagedProfileDto = ManagedProfileDto(
        protocol = protocol,
        endpoint = EndpointDto(host = HOST, port = port),
        credential = credential,
        flow = flow,
        security = security,
        transport = transport,
    )

    fun access(
        state: String = "ACTIVE",
        expiresAt: String? = null,
        profile: ManagedProfileDto = profile(),
        server: ServerDto = server(),
    ): AccessResponseDto = AccessResponseDto(
        id = ACCESS_ID,
        state = state,
        server = server,
        createdAt = CREATED_AT,
        expiresAt = expiresAt,
        profile = profile,
    )
}
