package app.vazie.vpn.managed.api

import app.vazie.vpn.api.UnsupportedRuntimeFeature

/** Why managed access could not be obtained, from a closed taxonomy. */
sealed interface ManagedAccessFailure {

    /** No session on this device. Nothing has been tried, and signing in is what would change that. */
    data object SignInRequired : ManagedAccessFailure

    /** The session existed and no longer works. The stored token is discarded on this one, and only this one. */
    data object SessionExpired : ManagedAccessFailure

    /** Authenticated, and not entitled to managed access (`MANAGED_SERVER_ACCESS`). */
    data object EntitlementMissing : ManagedAccessFailure

    /** No Managed Server will take this account right now, or the one asked for will not. */
    data object ServerUnavailable : ManagedAccessFailure

    /** The access referred to is not there. From a revocation this is success in disguise, and from a read it
     * means there is no usable managed access — the two readings are the caller's to make. */
    data object AccessNotFound : ManagedAccessFailure

    /** The server was chosen and the client could not be created on it. Retryable. */
    data object ProvisioningFailed : ManagedAccessFailure

    /** Asked for too often. `retryAfterSeconds` is the server's own answer, absent if it gave none. */
    data class RateLimited(val retryAfterSeconds: Long?) : ManagedAccessFailure

    /** Nothing answered: no network, no route, or nothing within the timeout. */
    data object BackendUnreachable : ManagedAccessFailure

    /** The backend described a profile this build cannot run. */
    data class UnsupportedProfile(val feature: UnsupportedRuntimeFeature) : ManagedAccessFailure

    /** Something else. Kept rather than mapped optimistically onto a neighbour. */
    data object Unknown : ManagedAccessFailure
}
