package app.vazie.vpn.data.managed

import app.vazie.vpn.account.api.Account
import app.vazie.vpn.account.api.AccountRepository
import app.vazie.vpn.account.api.AccountResult
import app.vazie.vpn.account.api.AccountState
import app.vazie.vpn.account.api.EmailCodeRequested
import app.vazie.vpn.account.api.PlusAccess
import app.vazie.vpn.api.VazieAccessProblem
import app.vazie.vpn.api.VazieServerDirectory
import app.vazie.vpn.api.VazieServerResolution
import app.vazie.vpn.core.model.Secret
import app.vazie.vpn.managed.api.ManagedAccessFailure
import app.vazie.vpn.managed.api.ManagedAccessId
import app.vazie.vpn.managed.api.ManagedAccessRepository
import app.vazie.vpn.managed.api.ManagedConnectionMaterial
import app.vazie.vpn.managed.api.ManagedResult
import app.vazie.vpn.managed.api.ManagedServerAvailability
import app.vazie.vpn.managed.api.ManagedServerCatalogueStore
import app.vazie.vpn.managed.api.ManagedServerId
import app.vazie.vpn.managed.api.ManagedServerSummary
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

/** Vazie servers: listed only with VPN Plus, unavailable ones left out, access resolved to a profile that
 * carries the selection's id, and every refusal named. */
class ManagedServerDirectoryTest {

    private val account = StubAccount(AccountState.SignedIn(ACCOUNT.copy(plus = PlusAccess("2027-01-01T00:00:00Z"))))
    private val catalogue = StubCatalogue(
        listOf(
            summary(ManagedFixtures.SERVER_ID, "Netherlands", ManagedServerAvailability.AVAILABLE),
            summary(OTHER_SERVER, "Sweden", ManagedServerAvailability.DRAINING),
            summary(DOWN_SERVER, "Germany", ManagedServerAvailability.UNAVAILABLE),
        ),
    )
    private val access = StubAccess()
    private val directory = ManagedServerDirectory(access, catalogue, account)

    @Test
    fun `without VPN Plus there are no Vazie servers`() = runTest {
        account.state.value = AccountState.SignedIn(ACCOUNT)
        assertEquals(emptyList(), directory.observe().first())

        account.state.value = AccountState.SignedOut
        assertEquals(emptyList(), directory.observe().first())
    }

    @Test
    fun `with VPN Plus the usable servers are listed, the unavailable one is not`() = runTest {
        val servers = directory.observe().first()

        assertEquals(listOf("Netherlands", "Sweden"), servers.map { it.name })
        assertEquals(listOf(true, false), servers.map { it.available })
        assertEquals(VazieServerDirectory.idOf(ManagedFixtures.SERVER_ID), servers.first().id)
    }

    @Test
    fun `a used server says when, and the history outlives the process`() = runTest {
        val folder = File.createTempFile("vazie-managed", "").apply { delete(); mkdirs() }
        try {
            val file = File(folder, "server-usage")
            val clock = Clock.fixed(Instant.parse("2026-09-28T10:00:00Z"), ZoneOffset.UTC)
            val used = ManagedServerDirectory(access, catalogue, account, usage = FileServerUsage(file), clock = clock)
            assertEquals(listOf(null, null), used.observe().first().map { it.lastUsedAt })

            used.markUsed(VazieServerDirectory.idOf(OTHER_SERVER))
            used.markUsed(app.vazie.vpn.core.model.ProfileId("not-a-vazie-server"))

            val again = ManagedServerDirectory(access, catalogue, account, usage = FileServerUsage(file))
            assertEquals(listOf(null, clock.instant()), again.observe().first().map { it.lastUsedAt })
        } finally {
            folder.deleteRecursively()
        }
    }

    @Test
    fun `access resolves to a profile under the selection's id`() = runTest {
        val id = VazieServerDirectory.idOf(ManagedFixtures.SERVER_ID)
        access.answer = ManagedResult.Success(material())

        val resolved = assertIs<VazieServerResolution.Ready>(directory.resolve(id))

        assertEquals(id, resolved.profile.id)
        assertEquals("Netherlands", resolved.profile.name)
        assertEquals(listOf(ManagedServerId(ManagedFixtures.SERVER_ID)), access.ensured)
    }

    @Test
    fun `every refusal is named`() = runTest {
        val id = VazieServerDirectory.idOf(ManagedFixtures.SERVER_ID)
        mapOf(
            ManagedAccessFailure.SignInRequired to VazieAccessProblem.SIGN_IN_REQUIRED,
            ManagedAccessFailure.SessionExpired to VazieAccessProblem.SIGN_IN_REQUIRED,
            ManagedAccessFailure.EntitlementMissing to VazieAccessProblem.PLUS_REQUIRED,
            ManagedAccessFailure.ServerUnavailable to VazieAccessProblem.SERVER_UNAVAILABLE,
            ManagedAccessFailure.BackendUnreachable to VazieAccessProblem.UNREACHABLE,
            ManagedAccessFailure.ProvisioningFailed to VazieAccessProblem.OTHER,
        ).forEach { (failure, problem) ->
            access.answer = ManagedResult.Failure(failure)
            assertEquals(VazieServerResolution.Refused(problem), directory.resolve(id), "$failure")
        }
    }

    @Test
    fun `refresh asks the backend only with VPN Plus`() = runTest {
        directory.refresh()
        assertEquals(1, access.listed)

        account.state.value = AccountState.SignedOut
        directory.refresh()
        assertEquals(1, access.listed)
    }

    private fun material(): ManagedConnectionMaterial {
        val mapped = ManagedProfileMapper.map(ManagedFixtures.access())
        return assertIs<ManagedProfileMapping.Mapped>(mapped).material
    }

    private fun summary(id: String, name: String, availability: ManagedServerAvailability) = ManagedServerSummary(
        id = ManagedServerId(id),
        displayName = name,
        regionId = "region",
        countryCode = "NL",
        availability = availability,
    )

    private class StubCatalogue(servers: List<ManagedServerSummary>) : ManagedServerCatalogueStore {
        private val flow = MutableStateFlow(servers)
        override fun observe(): Flow<List<ManagedServerSummary>> = flow
        override suspend fun catalogue(): List<ManagedServerSummary> = flow.value
        override suspend fun replace(servers: List<ManagedServerSummary>) {
            flow.value = servers
        }
    }

    private class StubAccess : ManagedAccessRepository {
        var answer: ManagedResult<ManagedConnectionMaterial> = ManagedResult.Failure(ManagedAccessFailure.Unknown)
        val ensured = mutableListOf<ManagedServerId>()
        var listed = 0
        override suspend fun servers(): ManagedResult<List<ManagedServerSummary>> {
            listed++
            return ManagedResult.Success(emptyList())
        }
        override suspend fun ensureAccess(serverId: ManagedServerId): ManagedResult<ManagedConnectionMaterial> {
            ensured += serverId
            return answer
        }
        override suspend fun revokeAccess(id: ManagedAccessId): ManagedResult<Unit> = ManagedResult.Success(Unit)
    }

    private class StubAccount(initial: AccountState) : AccountRepository {
        override val state = MutableStateFlow(initial)
        override suspend fun refresh(): AccountState = state.value
        override suspend fun requestEmailCode(email: String): AccountResult<EmailCodeRequested> = error("unused")
        override suspend fun verifyEmailCode(email: String, code: Secret<String>): AccountResult<Account> = error("unused")
        override suspend fun signOut(): AccountResult<Unit> = error("unused")
        override suspend fun signOutLocally() = Unit
        override suspend fun deleteAccount(): AccountResult<Unit> = error("unused")
    }

    private companion object {
        const val OTHER_SERVER = "00000000-0000-4000-8000-0000000000b2"
        const val DOWN_SERVER = "00000000-0000-4000-8000-0000000000b3"
        val ACCOUNT = Account(id = "account-id", email = "person@example.com", status = "ACTIVE", entitlements = emptyList())
    }
}
