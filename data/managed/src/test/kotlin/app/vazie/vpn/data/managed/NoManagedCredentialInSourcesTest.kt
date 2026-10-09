package app.vazie.vpn.data.managed

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** The module that knows the wire format must not become the place a real credential is kept. */
class NoManagedCredentialInSourcesTest {

    @Test
    fun `no UUID-shaped literal appears outside a synthetic one`() {
        val offenders = sources()
            .flatMap { file ->
                UUID_SHAPE.findAll(file.readText())
                    .map { file.path to it.value }
                    .filterNot { (_, uuid) -> uuid.startsWith(SYNTHETIC_PREFIX) }
                    .toList()
            }
            .map { (path, uuid) -> "$path contains $uuid" }

        assertTrue(offenders.isEmpty(), offenders.joinToString(separator = "\n"))
    }

    @Test
    fun `no opaque credential-shaped run appears anywhere`() {
        // A REALITY key, a base64url session token and a short id are all long runs of one alphabet
        // with no punctuation. Real code does not produce them; a pasted credential does.
        val offenders = sources()
            .flatMap { file ->
                OPAQUE_RUN.findAll(file.readText())
                    .map { file.path to it.value }
                    .filterNot { (_, candidate) -> candidate.isObviouslySynthetic() }
                    .toList()
            }
            .map { (path, candidate) -> "$path contains a ${candidate.length}-character opaque run" }

        assertTrue(offenders.isEmpty(), offenders.joinToString(separator = "\n"))
    }

    @Test
    fun `no share link appears here either`() {
        // The managed path never produces or consumes `vless://` links.
        val offenders = sources()
            .filter { LINK.containsMatchIn(it.readText()) }
            .map { it.path }

        assertTrue(offenders.isEmpty(), "a share link reached the managed module:\n$offenders")
    }

    private fun sources(): List<File> = File("src")
        .walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .filterNot { it.name == "NoManagedCredentialInSourcesTest.kt" }
        .toList()

    private fun String.isObviouslySynthetic(): Boolean =
        toSet().size <= 2 ||
            contains("synthetic") ||
            contains("placeholder") ||
            contains("example") ||
            contains("not-a-real")

    private companion object {
        const val SYNTHETIC_PREFIX = "00000000-0000-"

        val UUID_SHAPE =
            Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

        val OPAQUE_RUN = Regex("[A-Za-z0-9_-]{40,}")

        val LINK = Regex("(vless|vmess|trojan|ss)://[A-Za-z0-9._%+-]{4,}", RegexOption.IGNORE_CASE)
    }
}
