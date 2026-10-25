package com.newoether.agora.viewmodel

/**
 * The " #12" suffix at the end of a conversation title — a hashtag plus a number,
 * e.g. "Welcome Messages #12" — so a conversation can be referred to by a short,
 * stable number in the drawer, in chat, and from memory.
 *
 * Rules (all covered by ConversationTitleTagTest):
 * - [apply] appends " #<number>" unless the title already ends with a tag, in which
 *   case the existing number wins and the title is returned unchanged. A numbered
 *   conversation therefore keeps its number across re-titles.
 * - [baseTitle] strips the tag, for callers that need the bare title.
 * - [numberOf] reads the tag back, or returns null for an untagged title.
 *
 * Numbers are assigned by [ConversationTitleGenerator] the first time a conversation
 * gets an AI-generated title: the count of ordinary conversations at that moment.
 * Existing conversations are never renumbered and their stored titles are untouched
 * until a title is (re)generated, so nothing about existing data changes on upgrade.
 * Known limitation, by design for now: deleting conversations can let a later
 * conversation reuse a freed number. A never-reused sequence would need a dedicated
 * counter column (Room migration) — proposed separately, not required for this tag.
 */
object ConversationTitleTag {

    private val TAG_SUFFIX = Regex("""\s+#(\d+)$""")

    /** The trailing tag's number, or null when [title] has no " #<number>" suffix. */
    fun numberOf(title: String): Int? =
        TAG_SUFFIX.find(title.trimEnd())?.groupValues?.get(1)?.toIntOrNull()

    /** [title] with any trailing " #<number>" tag removed. */
    fun baseTitle(title: String): String =
        TAG_SUFFIX.replace(title.trimEnd(), "").trimEnd()

    /**
     * [title] carrying number [number]. Returns [title] unchanged when it already has
     * a tag (the existing number is stable and wins) or when [number] is not positive.
     */
    fun apply(title: String, number: Int): String {
        val base = title.trim()
        if (base.isEmpty() || number < 1) return title
        if (numberOf(base) != null) return base
        return "$base #$number"
    }
}
