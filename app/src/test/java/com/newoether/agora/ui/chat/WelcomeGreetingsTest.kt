package com.newoether.agora.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WelcomeGreetingsTest {

    @Test
    fun bucketBoundaries() {
        assertEquals(WelcomeGreetings.Bucket.LATE_NIGHT, WelcomeGreetings.bucketFor(0))
        assertEquals(WelcomeGreetings.Bucket.LATE_NIGHT, WelcomeGreetings.bucketFor(4))
        assertEquals(WelcomeGreetings.Bucket.MORNING, WelcomeGreetings.bucketFor(5))
        assertEquals(WelcomeGreetings.Bucket.MORNING, WelcomeGreetings.bucketFor(11))
        assertEquals(WelcomeGreetings.Bucket.AFTERNOON, WelcomeGreetings.bucketFor(12))
        assertEquals(WelcomeGreetings.Bucket.AFTERNOON, WelcomeGreetings.bucketFor(16))
        assertEquals(WelcomeGreetings.Bucket.EVENING, WelcomeGreetings.bucketFor(17))
        assertEquals(WelcomeGreetings.Bucket.EVENING, WelcomeGreetings.bucketFor(21))
        assertEquals(WelcomeGreetings.Bucket.LATE_NIGHT, WelcomeGreetings.bucketFor(22))
        assertEquals(WelcomeGreetings.Bucket.LATE_NIGHT, WelcomeGreetings.bucketFor(23))
    }

    @Test
    fun everyBucketHasSeveralVariants() {
        for (bucket in WelcomeGreetings.Bucket.entries) {
            assertTrue(
                "bucket $bucket should offer rotating variants",
                WelcomeGreetings.variants(bucket).size >= 3,
            )
        }
    }

    @Test
    fun nameSlotExpandsOnlyWhenANameIsSaved() {
        assertEquals("Good morning, Sneaks", WelcomeGreetings.greeting(8, "Sneaks", 0))
        assertEquals("Good morning", WelcomeGreetings.greeting(8, "", 0))
        assertEquals("Good morning", WelcomeGreetings.greeting(8, "   ", 0))
    }

    @Test
    fun variantIndexWrapsAndNegativesAreSafe() {
        val morning = WelcomeGreetings.MORNING
        assertEquals(
            WelcomeGreetings.greeting(8, "", 0),
            WelcomeGreetings.greeting(8, "", morning.size),
        )
        assertEquals(
            WelcomeGreetings.greeting(8, "", morning.size - 1),
            WelcomeGreetings.greeting(8, "", -1),
        )
    }

    @Test
    fun greetingNeverLeaksThePlaceholder() {
        for (hour in 0..23) {
            for (index in -2..8) {
                val text = WelcomeGreetings.greeting(hour, "Sneaks", index)
                assertTrue(text.isNotBlank())
                assertTrue("placeholder leaked at hour=$hour index=$index: $text", !text.contains("%1"))
            }
        }
    }
}
