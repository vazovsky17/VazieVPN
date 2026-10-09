package app.vazie.vpn.feature.config

import app.vazie.vpn.config.InvalidReason
import app.vazie.vpn.config.UnsupportedFeature
import kotlinx.collections.immutable.persistentListOf

/** Synthetic wizard states, for previews and the debug outcome switcher. */
internal object AddConfigFixtures {

    val reality = ConfigPreviewUi(
        protocolLabel = "VLESS",
        securityLabel = "REALITY",
        transportLabel = "TCP",
        server = "relay.example.net:443",
        endpointHost = "relay.example.net",
        endpointPort = 443,
        serverName = "relay.example.net",
        fingerprint = "chrome",
        flow = "xtls-rprx-vision",
        suggestedName = "Home relay",
        mark = "VL",
        keptParameters = persistentListOf("packetEncoding"),
    )

    val webSocket = ConfigPreviewUi(
        protocolLabel = "VLESS",
        securityLabel = "TLS",
        transportLabel = "WebSocket",
        server = "edge.example.net:8443",
        endpointHost = "edge.example.net",
        endpointPort = 8443,
        serverName = "edge.example.net",
        fingerprint = null,
        flow = null,
        suggestedName = "Travel relay",
        mark = "VL",
    )

    val plain = ConfigPreviewUi(
        protocolLabel = "VLESS",
        securityLabel = null,
        transportLabel = "TCP",
        server = "192.0.2.10:443",
        endpointHost = "192.0.2.10",
        endpointPort = 443,
        serverName = null,
        fingerprint = null,
        flow = null,
        suggestedName = "192.0.2.10",
        mark = "VL",
    )

    /** The four outcomes step 2 can reach, in the order a reviewer wants to compare them. */
    val allOutcomes: List<DetectionUiState> = listOf(
        DetectionUiState.AwaitingInput,
        DetectionUiState.Recognized(reality),
        DetectionUiState.Recognized(webSocket),
        DetectionUiState.Recognized(plain),
        DetectionUiState.Unsupported(
            protocolLabel = "VLESS",
            feature = UnsupportedFeature.Transport("kcp"),
        ),
        DetectionUiState.Invalid(InvalidReason.MalformedUserId),
        DetectionUiState.Invalid(InvalidReason.MissingParameter("pbk")),
    )
}
