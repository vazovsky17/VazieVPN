package app.vazie.vpn.feature.account

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmailFormatTest {

    @Test
    fun `ordinary addresses pass`() {
        listOf(
            "person@example.com",
            " person@example.com ",
            "first.last+tag@sub.example.co.uk",
            "имя@пример.рф",
        ).forEach { assertTrue(EmailFormat.isPlausible(it), "`$it` was refused") }
    }

    @Test
    fun `what cannot be an address is refused`() {
        listOf(
            "",
            "person",
            "person@",
            "@example.com",
            "person@example",
            "person@.com",
            "person@example.",
            "per son@example.com",
            "a@b@example.com",
            "person@example",
            "person@example.c",
            "pers on@example.com",
            ".person@example.com",
            "person.@example.com",
            "person@-example.com",
            "person@example..com",
            "p@" + "x".repeat(260) + ".com",
        ).forEach { assertFalse(EmailFormat.isPlausible(it), "`$it` was accepted") }
    }

    @Test
    fun `common domains are offered before the at sign is typed`() {
        assertEquals(listOf("ivan@gmail.com", "ivan@yandex.ru", "ivan@mail.ru"), EmailFormat.completions("ivan"))
        assertEquals(listOf("ivan@gmail.com", "ivan@yandex.ru", "ivan@mail.ru"), EmailFormat.completions("ivan@"))
    }

    @Test
    fun `completions narrow to what the domain starts with and stop once it is known`() {
        assertEquals(listOf("ivan@yandex.ru", "ivan@ya.ru", "ivan@yahoo.com"), EmailFormat.completions("ivan@ya"))
        assertEquals(listOf("ivan@gmail.com"), EmailFormat.completions("ivan@gm"))
        assertEquals(emptyList(), EmailFormat.completions("ivan@gmail.com"))
        assertEquals(emptyList(), EmailFormat.completions("ivan@company.example"))
    }

    @Test
    fun `nothing is offered for what cannot become an address`() {
        assertEquals(emptyList(), EmailFormat.completions(""))
        assertEquals(emptyList(), EmailFormat.completions("iv an"))
        assertEquals(emptyList(), EmailFormat.completions("a@b@"))
        assertEquals(emptyList(), EmailFormat.completions("ivan."))
    }
}
