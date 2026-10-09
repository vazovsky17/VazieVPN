package app.vazie.vpn.data.account

import app.vazie.vpn.core.crypto.ByteCipher
import app.vazie.vpn.core.model.Secret
import java.io.File
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

/** The encrypted session file, with a per-test AES-GCM key instead of the Keystore. */
class FileSessionTokenStoreTest {

    private val directory = File.createTempFile("vazie-session", "").let { file ->
        file.delete()
        file.mkdirs()
        file
    }
    private val file = File(directory, "vazie-account/session")
    private val cipher = TestCipher()

    @AfterTest
    fun cleanUp() {
        directory.deleteRecursively()
    }

    @Test
    fun `a new installation has no session`() = runTest {
        assertNull(store().token())
        assertFalse(file.exists())
    }

    @Test
    fun `a stored token comes back, and survives a new store on the same file`() = runTest {
        store().store(Secret.of(TOKEN))

        assertEquals(TOKEN, store().token()?.expose())
    }

    @Test
    fun `nothing is written in the clear`() = runTest {
        store().store(Secret.of(TOKEN))

        val onDisk = String(file.readBytes(), Charsets.ISO_8859_1)
        assertFalse(onDisk.contains(TOKEN), "the token is on disk in the clear")
    }

    @Test
    fun `clearing forgets the token for good`() = runTest {
        val store = store()
        store.store(Secret.of(TOKEN))

        store.clear()

        assertNull(store.token())
        assertNull(store().token(), "a fresh store found a cleared session")
    }

    @Test
    fun `a token stored after a clear is returned again`() = runTest {
        val store = store()
        store.store(Secret.of(TOKEN))
        store.clear()

        store.store(Secret.of("a-second-session-token"))

        assertEquals("a-second-session-token", store.token()?.expose())
    }

    @Test
    fun `a record this device cannot decrypt reads as no session`() = runTest {
        file.parentFile?.mkdirs()
        file.writeBytes(byteArrayOf(9, 9, 9, 9))

        assertNull(store().token())
    }

    @Test
    fun `the store never renders the token`() = runTest {
        val store = store()
        store.store(Secret.of(TOKEN))

        assertFalse(store.toString().contains(TOKEN))
        assertFalse(store.token().toString().contains(TOKEN))
    }

    private fun store() = FileSessionTokenStore(file = file, cipher = cipher)

    private class TestCipher : ByteCipher {
        private val key: SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

        override fun encrypt(plaintext: ByteArray): ByteArray {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            return cipher.iv + cipher.doFinal(plaintext)
        }

        override fun decrypt(ciphertext: ByteArray): ByteArray {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, ciphertext, 0, IV_BYTES))
            return cipher.doFinal(ciphertext, IV_BYTES, ciphertext.size - IV_BYTES)
        }

        private companion object {
            const val TRANSFORMATION = "AES/GCM/NoPadding"
            const val TAG_BITS = 128
            const val IV_BYTES = 12
        }
    }

    private companion object {
        const val TOKEN = "a-synthetic-session-token-that-opens-nothing"
    }
}
