package app.vazie.vpn.feature.account

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Judging the address while it is typed, and finding the code on the clipboard. */
class EmailCheckTest {

    @Test
    fun `nothing is said while the address is still being typed`() {
        assertEquals(EmailCheck.Empty, EmailFormat.check(""))
        assertEquals(EmailCheck.Typing, EmailFormat.check("person"))
        assertEquals(EmailCheck.Typing, EmailFormat.check("person@"))
        assertEquals(EmailCheck.Typing, EmailFormat.check("person@ex"))
    }

    @Test
    fun `a complete address is fine`() {
        assertEquals(EmailCheck.Ok, EmailFormat.check("person@example.com"))
        assertEquals(EmailCheck.Ok, EmailFormat.check("  person@gmail.com "))
    }

    @Test
    fun `broken addresses are called out`() {
        assertEquals(EmailCheck.Invalid, EmailFormat.check("per son@example.com"))
        assertEquals(EmailCheck.Invalid, EmailFormat.check("a@b@example.com"))
        assertEquals(EmailCheck.Invalid, EmailFormat.check("@example.com"))
        assertEquals(EmailCheck.Invalid, EmailFormat.check("person@example."))
    }

    @Test
    fun `a typo in a common domain gets a one-tap fix`() {
        assertEquals(EmailCheck.Suggestion("person@gmail.com"), EmailFormat.check("person@gmial.com"))
        assertEquals(EmailCheck.Suggestion("person@gmail.com"), EmailFormat.check("person@gmail"))
        assertEquals(EmailCheck.Suggestion("person@yandex.ru"), EmailFormat.check("person@yandx.ru"))
        assertEquals(EmailCheck.Suggestion("person@mail.ru"), EmailFormat.check("person@mail.ri"))
    }

    @Test
    fun `the button waits for an address that could be real`() {
        assertEquals(false, SignInUiState(email = "person@ex").canRequestCode)
        assertEquals(true, SignInUiState(email = "person@example.com").canRequestCode)
    }

    @Test
    fun `the code is taken from the clipboard only when it is unambiguous`() {
        assertEquals("123456", codeIn("Ваш код для входа в Vazie: 123456", 6))
        assertEquals("123456", codeIn("123456", 6))
        assertNull(codeIn("Код 123456, запасной 654321", 6))
        assertNull(codeIn("Позвоните +7 000 1234567", 6))
        assertNull(codeIn("1234567", 6))
        assertNull(codeIn(null, 6))
    }
}
