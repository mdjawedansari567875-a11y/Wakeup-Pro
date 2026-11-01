package com.carfam.wakeuppro.games

import kotlin.random.Random

enum class GameType(val title: String, val emoji: String) {
    // 20 core categories from the spec
    NUMBER_MEMORY("Number Memory", "🔢"),
    REVERSE_MEMORY("Reverse Number Memory", "🔁"),
    MISSING_NUMBER("Missing Number", "❓"),
    SHAPE_RECOGNITION("Shape Recognition", "🔷"),
    PATTERN_COMPLETION("Pattern Completion", "🧩"),
    COLOR_RECOGNITION("Color Recognition", "🎨"),
    STROOP("Stroop Color Test", "🌈"),
    ODD_ONE_OUT("Odd One Out", "🎯"),
    LOGIC("Logic Puzzle", "🧠"),
    SEQUENCE("Sequence Recognition", "📈"),
    WORD_MEMORY("Word Memory", "📝"),
    QUICK_DECISION("Quick Decision", "⚡"),
    SYMBOL_MATCH("Symbol Matching", "🔣"),
    SHORT_TERM_MEMORY("Memory Grid", "🟦"),
    MENTAL_MATH("Mental Math", "➗"),
    DIRECTION("Direction Test", "🧭"),
    IMAGE_MATCH("Image Matching", "🃏"),
    DIFFERENT_SHAPE("Find the Different One", "🔍"),
    BRAIN_TEASER("Brain Teaser", "💡"),
    REACTION("Reaction Test", "⏱"),

    // From the reference reels
    TAP_COUNTER("Tap Challenge", "👆"),
    SHAKE("Shake To Dismiss", "📳"),
    CONTROL_PANEL("Control Panel", "🎛"),
    BALANCE("Balance Test", "⚖"),

    // Original CARFAM games
    SIMON("Simon Sequence", "🎵"),
    CATCH_DOT("Catch The Dots", "🔴"),
    SWIPE_ARROWS("Swipe Rules", "👉"),
    SLIDER_LOCK("Slider Lock", "🔒")
}

/**
 * A fully generated puzzle. Which fields are used depends on [type]; see Gen.kt.
 * [id] is a content fingerprint used by the anti-repetition history.
 */
data class Puzzle(
    val type: GameType,
    val id: String,
    val instruction: String,
    val prompt: String = "",
    val options: List<String> = emptyList(),
    val answer: Int = -1,
    val items: List<String> = emptyList(),
    val ints: List<Int> = emptyList(),
    val answers: Set<Int> = emptySet(),
    val promptColor: Long = 0L,
    val optionColors: List<Long> = emptyList()
)

val STROOP_NAMES = listOf("RED", "BLUE", "GREEN", "YELLOW", "PURPLE", "ORANGE")
val STROOP_COLORS = listOf(0xFFE53935L, 0xFF1E88E5L, 0xFF43A047L, 0xFFFDD835L, 0xFF8E24AAL, 0xFFFB8C00L)
val ARROWS = listOf("⬆", "➡", "⬇", "⬅")

/** Storage the engine needs; implemented by data.Store (and by a fake in tests). */
interface History {
    fun recentIds(): List<String>
    fun addId(id: String)
    fun lastCombo(): String
    fun setLastCombo(v: String)
}

object Engine {

    /** Picks [count] distinct game types; none of them were used by the previous alarm. */
    fun pickGames(h: History, count: Int, rng: Random = Random.Default): List<GameType> {
        val last = h.lastCombo().split(",").filter { it.isNotBlank() }
        val all = GameType.values().toList()
        var pool = all.filter { it.name !in last }
        if (pool.size < count) pool = all
        var pick = pool.shuffled(rng).take(count)
        var tries = 0
        while (pick.map { it.name }.sorted() == last.sorted() && tries < 20) {
            pick = pool.shuffled(rng).take(count)
            tries++
        }
        h.setLastCombo(pick.joinToString(",") { it.name })
        return pick
    }

    /** Generates a fresh puzzle whose id is not in the recent history. */
    fun puzzle(type: GameType, diff: Int, h: History, rng: Random = Random.Default): Puzzle {
        val recent = h.recentIds().toHashSet()
        var p = Gen.make(type, diff, rng)
        var tries = 0
        while (p.id in recent && tries < 40) {
            p = Gen.make(type, diff, rng)
            tries++
        }
        h.addId(p.id)
        return p
    }
}
