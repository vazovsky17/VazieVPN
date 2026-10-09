package app.vazie.vpn.data.account

import app.vazie.vpn.core.crypto.ByteCipher
import app.vazie.vpn.core.model.Secret
import app.vazie.vpn.core.network.SessionTokenStore
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** A session token in a file, encrypted with a key that never leaves the device's hardware-backed key
 * store. */
internal class FileSessionTokenStore(
    private val file: File,
    private val cipher: ByteCipher,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SessionTokenStore {

    private val mutex = Mutex()

    override suspend fun token(): Secret<String>? = mutex.withLock {
        withContext(dispatcher) {
            if (!file.exists()) return@withContext null
            runCatching { cipher.decrypt(file.readBytes()) }
                .map { String(it, Charsets.UTF_8) }
                .getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?.let { Secret.of(it) }
        }
    }

    override suspend fun store(token: Secret<String>) = mutex.withLock {
        withContext(dispatcher) {
            file.parentFile?.mkdirs()
            val payload = cipher.encrypt(token.expose().toByteArray(Charsets.UTF_8))
            val temporary = File(file.parentFile, file.name + TEMPORARY_SUFFIX)
            temporary.writeBytes(payload)
            if (!temporary.renameTo(file)) {
                file.writeBytes(payload)
                temporary.delete()
            }
            Unit
        }
    }

    override suspend fun clear() = mutex.withLock {
        withContext(dispatcher) {
            // Deleting is enough: an absent file is "no session". If the file cannot be deleted it
            // is overwritten with an encrypted empty record, which reads the same way.
            if (!file.delete() && file.exists()) {
                file.writeBytes(cipher.encrypt(ByteArray(0)))
            }
            Unit
        }
    }

    private companion object {
        const val TEMPORARY_SUFFIX = ".tmp"
    }
}
