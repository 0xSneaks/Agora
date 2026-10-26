package com.newoether.agora.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConversationTitleTagTest {

    @Test
    fun applyAppendsTag() {
        assertEquals("Welcome Messages #12", ConversationTitleTag.apply("Welcome Messages", 12))
    }

    @Test
    fun applyIsIdempotentAndKeepsTheExistingNumber() {
        val once = ConversationTitleTag.apply("Welcome Messages", 12)
        assertEquals(once, ConversationTitleTag.apply(once, 12))
        // A later re-title must not renumber the conversation.
        assertEquals(once, ConversationTitleTag.apply(once, 99))
    }

    @Test
    fun numberOfReadsTheTag() {
        assertEquals(12, ConversationTitleTag.numberOf("Welcome Messages #12"))
        assertNull(ConversationTitleTag.numberOf("Welcome Messages"))
        assertNull(ConversationTitleTag.numberOf("C# notes"))
        assertNull(ConversationTitleTag.numberOf("Issue #12 is about titles"))
    }

    @Test
    fun baseTitleStripsOnlyTheTrailingTag() {
        assertEquals("Welcome Messages", ConversationTitleTag.baseTitle("Welcome Messages #12"))
        assertEquals("Issue #12 is about titles", ConversationTitleTag.baseTitle("Issue #12 is about titles"))
        assertEquals("Welcome Messages", ConversationTitleTag.baseTitle("Welcome Messages"))
    }

    @Test
    fun applyRejectsNonPositiveNumbersAndBlankTitles() {
        assertEquals("Welcome Messages", ConversationTitleTag.apply("Welcome Messages", 0))
        assertEquals("Welcome Messages", ConversationTitleTag.apply("Welcome Messages", -3))
        assertEquals("", ConversationTitleTag.apply("", 4))
    }
}
