package app.vazie.vpn.data.managed

import app.vazie.vpn.api.ServerEndpoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where each managed server listens, as the last catalogue fetch said — in memory only. */
internal class ServerEndpointMemory {

    private val endpoints = MutableStateFlow<Map<String, ServerEndpoint>>(emptyMap())

    val current: StateFlow<Map<String, ServerEndpoint>> = endpoints.asStateFlow()

    fun remember(servers: List<ServerDto>) {
        endpoints.value = servers.mapNotNull { server ->
            server.endpoint
                ?.takeIf { it.host.isNotBlank() && it.port in 1..MAX_PORT }
                ?.let { server.id to ServerEndpoint(it.host, it.port) }
        }.toMap()
    }

    private companion object {
        const val MAX_PORT = 65_535
    }
}
