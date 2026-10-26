package com.newoether.agora.ui.chat

/**
 * Every greeting the empty new-chat screen can show — all of them live in this one
 * object so the wording is easy to find and edit. No string resources, no other files.
 *
 * How it works:
 * - The local hour picks a [Bucket] (see [bucketFor]).
 * - One variant from that bucket is chosen when the new-chat screen is shown.
 * - Each variant contains `%1$s` where the name slot goes. The slot expands to
 *   `", Name"` when a display name is saved and to `""` when it isn't, so the same
 *   list serves both cases: "Good morning%1$s" renders as "Good morning, Sneaks"
 *   or plain "Good morning".
 *
 * Add, remove, or reword variants freely — just keep at least one per bucket and
 * keep the `%1$s` slot in each one.
 */
object WelcomeGreetings {

    enum class Bucket { MORNING, AFTERNOON, EVENING, LATE_NIGHT }

    /** Which bucket fits the local hour (0-23). Late night is 22:00-04:59. */
    fun bucketFor(hour: Int): Bucket = when (hour) {
        in 5..11 -> Bucket.MORNING
        in 12..16 -> Bucket.AFTERNOON
        in 17..21 -> Bucket.EVENING
        else -> Bucket.LATE_NIGHT
    }

    val MORNING: List<String> = listOf(
        "Good morning%1\$s",
        "Morning%1\$s — what are we building today?",
        "Rise and shine%1\$s",
        "Good morning%1\$s. Ready when you are.",
        "Back at it%1\$s — good morning.",
    )

    val AFTERNOON: List<String> = listOf(
        "Good afternoon%1\$s",
        "Afternoon%1\$s — what are we working on?",
        "Good afternoon%1\$s. Ready when you are.",
        "Hey%1\$s, hope the day's treating you well.",
        "Afternoon%1\$s — let's get into it.",
    )

    val EVENING: List<String> = listOf(
        "Good evening%1\$s",
        "Evening%1\$s — what are we working on tonight?",
        "Good evening%1\$s. Ready when you are.",
        "Evening%1\$s — winding down, or just getting started?",
        "Hey%1\$s, good evening.",
    )

    val LATE_NIGHT: List<String> = listOf(
        "Hey%1\$s",
        "Still up%1\$s? Let's make it count.",
        "Burning the midnight oil%1\$s?",
        "Late night%1\$s — I'm here.",
        "Hey%1\$s, night owl mode.",
    )

    fun variants(bucket: Bucket): List<String> = when (bucket) {
        Bucket.MORNING -> MORNING
        Bucket.AFTERNOON -> AFTERNOON
        Bucket.EVENING -> EVENING
        Bucket.LATE_NIGHT -> LATE_NIGHT
    }

    /**
     * The greeting for [hour] and [name]. [variantIndex] picks the variant and may be
     * any integer (it wraps), which lets the caller pass a random or rotating index
     * without caring how many variants a bucket has.
     */
    fun greeting(hour: Int, name: String, variantIndex: Int): String {
        val bucketVariants = variants(bucketFor(hour))
        val template = bucketVariants[Math.floorMod(variantIndex, bucketVariants.size)]
        val trimmedName = name.trim()
        val nameSlot = if (trimmedName.isEmpty()) "" else ", $trimmedName"
        return template.replace("%1\$s", nameSlot)
    }
}
