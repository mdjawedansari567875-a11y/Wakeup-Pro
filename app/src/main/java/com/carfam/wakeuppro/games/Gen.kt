package com.carfam.wakeuppro.games

import kotlin.random.Random

/**
 * Algorithmic puzzle generators. Every generator builds a brand new puzzle from random
 * parameters (so there are thousands of variations), and fingerprints it for the history.
 *
 * Difficulty d: 0 = Easy, 1 = Medium, 2 = Hard.
 */
object Gen {

    fun make(type: GameType, d: Int, r: Random): Puzzle = when (type) {
        GameType.NUMBER_MEMORY -> digitMemory(false, d, r)
        GameType.REVERSE_MEMORY -> digitMemory(true, d, r)
        GameType.MISSING_NUMBER -> missingNumber(d, r)
        GameType.SHAPE_RECOGNITION -> shapeRecognition(d, r)
        GameType.PATTERN_COMPLETION -> patternCompletion(d, r)
        GameType.COLOR_RECOGNITION -> colorRecognition(r)
        GameType.STROOP -> stroop(d, r)
        GameType.ODD_ONE_OUT -> oddOneOut(d, r)
        GameType.LOGIC -> logic(d, r)
        GameType.SEQUENCE -> sequenceNext(d, r)
        GameType.WORD_MEMORY -> wordMemory(d, r)
        GameType.QUICK_DECISION -> quickDecision(d, r)
        GameType.SYMBOL_MATCH -> symbolMatch(d, r)
        GameType.SHORT_TERM_MEMORY -> memoryGrid(d, r)
        GameType.MENTAL_MATH -> mentalMath(d, r)
        GameType.DIRECTION -> direction(d, r)
        GameType.IMAGE_MATCH -> imageMatch(d, r)
        GameType.DIFFERENT_SHAPE -> differentShape(d, r)
        GameType.BRAIN_TEASER -> brainTeaser(d, r)
        GameType.REACTION -> reaction(d, r)
        GameType.TAP_COUNTER -> tapCounter(d, r)
        GameType.SHAKE -> shake(d, r)
        GameType.CONTROL_PANEL -> controlPanel(d, r)
        GameType.BALANCE -> balance(d, r)
        GameType.SIMON -> simon(d, r)
        GameType.CATCH_DOT -> catchDot(d, r)
        GameType.SWIPE_ARROWS -> swipeArrows(d, r)
        GameType.SLIDER_LOCK -> sliderLock(d, r)
    }

    // ---------------- helpers ----------------

    private fun mk(
        type: GameType, key: String, instruction: String, prompt: String = "",
        options: List<String> = emptyList(), answer: Int = -1, items: List<String> = emptyList(),
        ints: List<Int> = emptyList(), answers: Set<Int> = emptySet(),
        promptColor: Long = 0L, optionColors: List<Long> = emptyList()
    ) = Puzzle(
        type, type.name + "-" + Integer.toHexString(key.hashCode()), instruction, prompt,
        options, answer, items, ints, answers, promptColor, optionColors
    )

    private fun <T> Random.pick(l: List<T>): T = l[nextInt(l.size)]

    private fun <T> shuffleWithAnswer(opts: List<T>, correct: Int, r: Random): Pair<List<T>, Int> {
        val order = opts.indices.shuffled(r)
        return order.map { opts[it] } to order.indexOf(correct)
    }

    private fun ipow(b: Int, e: Int): Int {
        var x = 1
        repeat(e) { x *= b }
        return x
    }

    /** 4 numeric options (one correct), shuffled. */
    private fun numChoices(ans: Int, r: Random, spread: Int): Pair<List<String>, Int> {
        val set = linkedSetOf(ans)
        var guard = 0
        while (set.size < 4 && guard++ < 300) {
            val delta = r.nextInt(1, spread + 1) * (if (r.nextBoolean()) 1 else -1)
            val v = ans + delta
            if (v >= 0 || ans < 0) set.add(v)
        }
        var extra = 1
        while (set.size < 4) set.add(ans + spread + extra++)
        val (opts, idx) = shuffleWithAnswer(set.toList(), 0, r)
        return opts.map { it.toString() } to idx
    }

    // ---------------- 1/2 Number memory (forward / reverse) ----------------

    private fun digitMemory(reverse: Boolean, d: Int, r: Random): Puzzle {
        val len = (if (reverse) 4 else 5) + d
        val digits = (1..len).joinToString("") { r.nextInt(10).toString() }
        val showMs = 1500 + len * 600
        return mk(
            if (reverse) GameType.REVERSE_MEMORY else GameType.NUMBER_MEMORY,
            "dm|$reverse|$digits",
            if (reverse) "Memorize the number, then type it BACKWARDS" else "Memorize the number, then type it",
            items = listOf(digits), ints = listOf(showMs)
        )
    }

    // ---------------- 3 Missing number ----------------

    private fun missingNumber(d: Int, r: Random): Puzzle {
        val kind = r.nextInt(4)
        val seq: List<Int> = when (kind) {
            0 -> {
                val s = r.nextInt(1, 10 + d * 15)
                val st = r.nextInt(2, 5 + d * 3)
                (0 until 6).map { s + it * st }
            }
            1 -> {
                val s = r.nextInt(1, 4)
                val m = r.nextInt(2, 4)
                (0 until 5).map { s * ipow(m, it) }
            }
            2 -> {
                val k = r.nextInt(0, 10 + d * 10)
                (1..6).map { it * it + k }
            }
            else -> {
                val a = r.nextInt(1, 6)
                val b = r.nextInt(2, 9)
                val s1 = r.nextInt(1, 20)
                val s2 = r.nextInt(1, 30)
                (0 until 7).map { if (it % 2 == 0) s1 + (it / 2) * a else s2 + (it / 2) * b }
            }
        }
        val gap = r.nextInt(1, seq.size - 1)
        val prompt = seq.mapIndexed { i, v -> if (i == gap) "?" else v.toString() }.joinToString("   ")
        val (opts, idx) = numChoices(seq[gap], r, 3 + d * 3)
        return mk(GameType.MISSING_NUMBER, "mn|$kind|${seq.joinToString(",")}|$gap",
            "Find the missing number", prompt, opts, idx)
    }

    // ---------------- 10 Sequence (what comes next) ----------------

    private fun sequenceNext(d: Int, r: Random): Puzzle {
        val kind = r.nextInt(4)
        val seq: List<Int> = when (kind) {
            0 -> { // alternate +a, -b
                val a = r.nextInt(4, 10 + d * 4)
                val b = r.nextInt(1, a - 1)
                val out = mutableListOf(r.nextInt(5, 30))
                for (i in 1 until 7) out.add(out.last() + if (i % 2 == 1) a else -b)
                out
            }
            1 -> { // fibonacci-like
                val out = mutableListOf(r.nextInt(1, 5), r.nextInt(1, 6))
                while (out.size < 7) out.add(out[out.size - 1] + out[out.size - 2])
                out
            }
            2 -> { // growing differences
                val inc = r.nextInt(1, 3 + d)
                var diff = r.nextInt(1, 4)
                val out = mutableListOf(r.nextInt(1, 20))
                for (i in 1 until 7) { out.add(out.last() + diff); diff += inc }
                out
            }
            else -> { // x*m + c
                val m = if (d == 2) r.nextInt(2, 4) else 2
                val c = r.nextInt(0, 4)
                val out = mutableListOf(r.nextInt(1, 4))
                while (out.size < 6) out.add(out.last() * m + c)
                out
            }
        }
        val shown = seq.dropLast(1)
        val ans = seq.last()
        val (opts, idx) = numChoices(ans, r, 3 + d * 4)
        return mk(GameType.SEQUENCE, "sq|$kind|${seq.joinToString(",")}",
            "What number comes next?", shown.joinToString("   ") + "   ?", opts, idx)
    }

    // ---------------- 4 Shape recognition ----------------

    private val SHAPES = listOf(
        "Circle" to "●", "Square" to "■", "Triangle" to "▲", "Diamond" to "◆", "Star" to "★",
        "Pentagon" to "⬟", "Hexagon" to "⬢", "Cross" to "✚", "Heart" to "♥"
    )
    private val SIDES = listOf("Triangle" to 3, "Square" to 4, "Pentagon" to 5, "Hexagon" to 6)

    private fun shapeRecognition(d: Int, r: Random): Puzzle {
        return if (d == 0 || r.nextBoolean()) {
            val four = SHAPES.shuffled(r).take(4)
            val target = r.nextInt(4)
            mk(GameType.SHAPE_RECOGNITION, "sr0|${four.joinToString { it.first }}|$target",
                "Tap the correct shape", "Find the ${four[target].first.uppercase()}",
                four.map { it.second }, target)
        } else {
            val (name, sides) = r.pick(SIDES)
            val correct = SHAPES.first { it.first == name }
            val others = SHAPES.filter { it.first != name && it.first != "Diamond" && it.first != "Square" }
                .shuffled(r).take(3)
            val all = listOf(correct) + others
            val (opts, idx) = shuffleWithAnswer(all.map { it.second }, 0, r)
            mk(GameType.SHAPE_RECOGNITION, "sr1|$name|${opts.joinToString("")}",
                "Tap the correct shape", "Which shape has $sides sides?", opts, idx)
        }
    }

    // ---------------- 5 Pattern completion ----------------

    private val PAT = listOf("▲", "●", "■", "◆", "★", "✚", "♥", "⬢")

    private fun patternCompletion(d: Int, r: Random): Puzzle {
        val unitLen = 2 + d + (if (r.nextBoolean()) 0 else 1).coerceAtMost(1)
        val unit = mutableListOf<String>()
        val pool = PAT.shuffled(r)
        for (i in 0 until unitLen) unit.add(pool[i % 4])
        if (unit.toSet().size < 2) unit[0] = pool[1]
        val k = r.nextInt(1, unitLen)
        val seq = (0 until unitLen * 2 + k).map { unit[it % unitLen] }
        val ans = unit[k]
        val distract = PAT.filter { it != ans }.shuffled(r).take(3)
        val (opts, idx) = shuffleWithAnswer(listOf(ans) + distract, 0, r)
        return mk(GameType.PATTERN_COMPLETION, "pc|${seq.joinToString("")}|$ans",
            "Which shape continues the pattern?", seq.joinToString(" ") + "  ?", opts, idx)
    }

    // ---------------- 6 Color recognition ----------------

    private val COLORS = listOf(
        "Red" to 0xFFE53935L, "Blue" to 0xFF1E88E5L, "Green" to 0xFF43A047L, "Yellow" to 0xFFFDD835L,
        "Purple" to 0xFF8E24AAL, "Orange" to 0xFFFB8C00L, "Pink" to 0xFFEC407AL, "Brown" to 0xFF795548L,
        "Cyan" to 0xFF00ACC1L, "Black" to 0xFF111111L
    )

    private fun colorRecognition(r: Random): Puzzle {
        val four = COLORS.shuffled(r).take(4)
        val t = r.nextInt(4)
        return if (r.nextBoolean()) {
            mk(GameType.COLOR_RECOGNITION, "cr0|${four.joinToString { it.first }}|$t",
                "Which colour is this?", "", four.map { it.first }, t, promptColor = four[t].second)
        } else {
            mk(GameType.COLOR_RECOGNITION, "cr1|${four.joinToString { it.first }}|$t",
                "Tap the matching colour", "Find ${four[t].first.uppercase()}",
                four.map { "" }, t, optionColors = four.map { it.second })
        }
    }

    // ---------------- 7 Stroop ----------------

    private fun stroop(d: Int, r: Random): Puzzle {
        val rounds = 3 + d
        val items = (0 until rounds).map {
            val ink = r.nextInt(6)
            var word = r.nextInt(6)
            while (word == ink) word = r.nextInt(6)
            val others = (0 until 6).filter { it != ink && it != word }.shuffled(r).take(2)
            val opts = (listOf(ink, word) + others).shuffled(r)
            "$word|$ink|${opts.joinToString(",")}"
        }
        return mk(GameType.STROOP, "st|${items.joinToString(";")}",
            "Tap the COLOUR OF THE INK, not the word", items = items)
    }

    // ---------------- 8 Odd one out ----------------

    private val CATS = mapOf(
        "fruit" to listOf("🍎", "🍌", "🍇", "🍓", "🍊", "🍉", "🍍", "🥝", "🍒", "🍑"),
        "veg" to listOf("🥕", "🌽", "🥦", "🍆", "🥔", "🧅", "🥒", "🥬"),
        "land" to listOf("🐶", "🐱", "🐭", "🐰", "🦊", "🐻", "🐼", "🐯", "🦁", "🐮", "🐷", "🐴"),
        "sea" to listOf("🐟", "🐠", "🐡", "🦈", "🐙", "🐬", "🐳", "🦀"),
        "birds" to listOf("🐔", "🐧", "🐦", "🦆", "🦅", "🦉", "🦜", "🦢"),
        "vehicles" to listOf("🚗", "🚕", "🚌", "🚓", "🚑", "🚒", "🚜", "🚲"),
        "sports" to listOf("⚽", "🏀", "🏈", "⚾", "🎾", "🏐", "🏉", "🎱"),
        "weather" to listOf("☀️", "🌧️", "⛈️", "❄️", "🌈", "☁️", "🌪️"),
        "music" to listOf("🎸", "🎹", "🎺", "🎻", "🥁", "🎷"),
        "clothes" to listOf("👕", "👖", "👗", "🧥", "👟", "🧢", "👒")
    )
    private val RELATED = listOf("fruit" to "veg", "land" to "sea", "land" to "birds", "sea" to "birds", "veg" to "fruit")

    private fun oddOneOut(d: Int, r: Random): Puzzle {
        val (a, b) = if (d == 2) r.pick(RELATED) else CATS.keys.shuffled(r).take(2).let { it[0] to it[1] }
        val majority = if (d == 0) 3 else 4
        val main = CATS.getValue(a).shuffled(r).take(majority)
        val odd = r.pick(CATS.getValue(b))
        val (opts, idx) = shuffleWithAnswer(main + odd, majority, r)
        return mk(GameType.ODD_ONE_OUT, "oo|$a|$b|${opts.joinToString("")}",
            "Tap the one that does NOT belong", "", opts, idx)
    }

    // ---------------- 9 Logic ----------------

    private val NAMES = listOf("Aman", "Riya", "Karan", "Neha", "Raj", "Simran", "Arjun", "Pooja", "Vikas", "Meera", "Rohan", "Anita")
    private val RELS = listOf(
        listOf("taller", "shorter", "tallest", "shortest"),
        listOf("older", "younger", "oldest", "youngest"),
        listOf("faster", "slower", "fastest", "slowest"),
        listOf("heavier", "lighter", "heaviest", "lightest")
    )
    private val DAYS = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    private val CRITTERS = listOf("Zorp" to "Zorps", "Blip" to "Blips", "Taff" to "Taffs", "Quib" to "Quibs", "Mivo" to "Mivos", "Drell" to "Drells")

    private fun logic(d: Int, r: Random): Puzzle = when (r.nextInt(3)) {
        0 -> {
            val n = if (d == 2) 4 else 3
            val ranked = NAMES.shuffled(r).take(n)
            val extra = NAMES.first { it !in ranked }
            val rel = r.pick(RELS)
            val statements = (0 until n - 1).map { i ->
                if (r.nextBoolean()) "${ranked[i]} is ${rel[0]} than ${ranked[i + 1]}"
                else "${ranked[i + 1]} is ${rel[1]} than ${ranked[i]}"
            }.shuffled(r)
            val askMax = r.nextBoolean()
            val correct = if (askMax) ranked.first() else ranked.last()
            val pool = if (n == 3) ranked + extra else ranked
            val (opts, idx) = shuffleWithAnswer(pool, pool.indexOf(correct), r)
            mk(GameType.LOGIC, "lg0|${statements.joinToString(";")}|$askMax",
                "Read the clues and think",
                statements.joinToString(".\n") + ".\n\nWho is the ${if (askMax) rel[2] else rel[3]}?", opts, idx)
        }
        1 -> {
            val (s1, p1) = r.pick(CRITTERS)
            val others = CRITTERS.filter { it.first != s1 }
            val (s2, p2) = r.pick(others)
            val who = r.pick(NAMES)
            val kind = r.nextInt(4)
            val (text, ans) = when (kind) {
                0 -> "All $p1 are $p2. $who is a $s1.\n\nIs $who a $s2?" to 0
                1 -> "All $p1 are $p2. $who is a $s2.\n\nIs $who a $s1?" to 2
                2 -> "No $p1 is a $s2. $who is a $s1.\n\nIs $who a $s2?" to 1
                else -> "Some $p1 are $p2. $who is a $s1.\n\nIs $who a $s2?" to 2
            }
            mk(GameType.LOGIC, "lg1|$s1|$s2|$who|$kind", "Pick the answer that must be true",
                text, listOf("Yes", "No", "Can't say"), ans)
        }
        else -> {
            val today = r.nextInt(7)
            if (r.nextBoolean()) {
                val n = r.nextInt(2, 8 + d * 20)
                val ans = (today + n) % 7
                val opts = (listOf(ans) + (0 until 7).filter { it != ans }.shuffled(r).take(3))
                val (sh, idx) = shuffleWithAnswer(opts.map { DAYS[it] }, 0, r)
                mk(GameType.LOGIC, "lg2a|$today|$n", "Count carefully",
                    "If today is ${DAYS[today]}, what day will it be after $n days?", sh, idx)
            } else {
                val ans = (today + 3) % 7
                val opts = (listOf(ans) + (0 until 7).filter { it != ans }.shuffled(r).take(3))
                val (sh, idx) = shuffleWithAnswer(opts.map { DAYS[it] }, 0, r)
                mk(GameType.LOGIC, "lg2b|$today", "Count carefully",
                    "The day before yesterday was ${DAYS[today]}.\n\nWhat day is tomorrow?", sh, idx)
            }
        }
    }

    // ---------------- 11 Word memory ----------------

    private val WORDS = listOf(
        "apple", "river", "chair", "cloud", "tiger", "mango", "pencil", "garden", "window", "silver",
        "planet", "market", "bridge", "candle", "forest", "island", "jacket", "lemon", "mirror", "orange",
        "rocket", "spoon", "table", "violin", "wallet", "yellow", "zebra", "anchor", "basket", "camera",
        "dragon", "engine", "flower", "guitar", "hammer", "insect", "jungle", "kettle", "ladder", "magnet",
        "needle", "ocean", "parrot", "queen", "rabbit", "sunset", "temple", "umbrella", "village", "whistle"
    )

    private fun wordMemory(d: Int, r: Random): Puzzle {
        val n = 4 + d
        val all = WORDS.shuffled(r).take(n * 2)
        val shown = all.take(n)
        val opts = all.shuffled(r)
        val answers = opts.indices.filter { opts[it] in shown }.toSet()
        return mk(GameType.WORD_MEMORY, "wm|${shown.sorted().joinToString(",")}|${opts.joinToString(",")}",
            "Remember the words, then pick them all", items = shown, options = opts,
            ints = listOf(4 + n), answers = answers)
    }

    // ---------------- 12 Quick decision ----------------

    private val WORDPAIRS = listOf("CAT" to "ELEPHANT", "SUN" to "MOUNTAIN", "PEN" to "NOTEBOOK", "BEE" to "BUTTERFLY", "SKY" to "UMBRELLA")

    private fun quickDecision(d: Int, r: Random): Puzzle {
        val rounds = (0 until 5).map {
            when (r.nextInt(4)) {
                0 -> { val n = r.nextInt(10, 99); "Is $n even?|Yes|No|${if (n % 2 == 0) 0 else 1}" }
                1 -> {
                    val a = r.nextInt(10, 99); var b = r.nextInt(10, 99); while (b == a) b = r.nextInt(10, 99)
                    "Which is BIGGER?|$a|$b|${if (a > b) 0 else 1}"
                }
                2 -> {
                    val a = r.nextInt(3, 10 + d * 3); val b = r.nextInt(3, 10)
                    val right = r.nextBoolean()
                    val shown = if (right) a * b else a * b + r.pick(listOf(-2, -1, 1, 2, 10))
                    "$a × $b = $shown|True|False|${if (right) 0 else 1}"
                }
                else -> {
                    val (w1, w2) = r.pick(WORDPAIRS)
                    if (r.nextBoolean()) "Which word is LONGER?|$w1|$w2|1" else "Which word is LONGER?|$w2|$w1|0"
                }
            }
        }
        return mk(GameType.QUICK_DECISION, "qd|${rounds.joinToString(";")}",
            "Decide fast! Answer before the time runs out", items = rounds, ints = listOf(6 - d))
    }

    // ---------------- 13 Symbol matching ----------------

    private val SYMBOL_POOLS = listOf("◐◑◒◓", "▲△▼▽", "★☆✦✧", "■□▣◼", "◆◇◈❖", "♠♣♥♦", "⊕⊗⊙⊘")

    private fun symbolMatch(d: Int, r: Random): Puzzle {
        val pool = r.pick(SYMBOL_POOLS).map { it.toString() }
        val target = r.pick(pool)
        val others = pool.filter { it != target }
        val matches = 3 + d
        val cells = MutableList(12) { "" }
        val spots = (0 until 12).shuffled(r)
        val answers = spots.take(matches).toSet()
        for (i in 0 until 12) cells[i] = if (i in answers) target else r.pick(others)
        return mk(GameType.SYMBOL_MATCH, "sm|$target|${cells.joinToString("")}",
            "Tap EVERY symbol that matches the target", target, items = cells, answers = answers)
    }

    // ---------------- 14 Short-term memory grid ----------------

    private fun memoryGrid(d: Int, r: Random): Puzzle {
        val n = if (d == 2) 4 else 3
        val lit = when (d) { 0 -> 3; 1 -> 4; else -> 6 }
        val cells = (0 until n * n).shuffled(r).take(lit).toSet()
        return mk(GameType.SHORT_TERM_MEMORY, "mg|$n|${cells.sorted().joinToString(",")}",
            "Remember the lit squares, then tap them", ints = listOf(n, 2200 + d * 400), answers = cells)
    }

    // ---------------- 15 Mental math ----------------

    private fun mentalMath(d: Int, r: Random): Puzzle {
        val kind = r.nextInt(6)
        val (expr, ans) = when (kind) {
            0 -> { val a = r.nextInt(10, 40 + d * 80); val b = r.nextInt(10, 40 + d * 60); "$a + $b" to a + b }
            1 -> { val a = r.nextInt(40, 90 + d * 100); val b = r.nextInt(10, a - 5); "$a − $b" to a - b }
            2 -> { val a = r.nextInt(3, 10 + d * 4); val b = r.nextInt(3, 10 + d * 3); "$a × $b" to a * b }
            3 -> { val a = r.nextInt(2, 9 + d * 5); val b = r.nextInt(2, 9 + d * 5); val c = r.nextInt(2, 6 + d * 2); "($a + $b) × $c" to (a + b) * c }
            4 -> { val a = r.nextInt(3, 10 + d * 3); val b = r.nextInt(3, 10 + d * 3); val c = r.nextInt(1, 20 + d * 10); "$a × $b + $c" to a * b + c }
            else -> { val b = r.nextInt(2, 9 + d * 3); val q = r.nextInt(3, 12 + d * 6); "${b * q} ÷ $b" to q }
        }
        val (opts, idx) = numChoices(ans, r, 4 + d * 4)
        return mk(GameType.MENTAL_MATH, "mm|$expr", "Solve it in your head", "$expr = ?", opts, idx)
    }

    // ---------------- 16 Direction ----------------

    private val COMPASS = listOf("North", "East", "South", "West")

    private fun direction(d: Int, r: Random): Puzzle {
        val mode = if (d == 0) r.nextInt(2) else r.nextInt(3)
        val i = r.nextInt(4)
        return when (mode) {
            0 -> {
                val (opts, idx) = shuffleWithAnswer(ARROWS, (i + 2) % 4, r)
                mk(GameType.DIRECTION, "dr0|$i|${opts.joinToString("")}",
                    "Tap the arrow pointing the OPPOSITE way", ARROWS[i], opts, idx)
            }
            1 -> {
                val cw = r.nextBoolean()
                val ans = if (cw) (i + 1) % 4 else (i + 3) % 4
                val (opts, idx) = shuffleWithAnswer(ARROWS, ans, r)
                mk(GameType.DIRECTION, "dr1|$i|$cw|${opts.joinToString("")}",
                    "Rotate this arrow 90° ${if (cw) "CLOCKWISE" else "COUNTER-CLOCKWISE"}", ARROWS[i], opts, idx)
            }
            else -> {
                val turns = (0 until 3 + d).map { r.nextInt(3) } // 0 right, 1 left, 2 u-turn
                var face = i
                for (t in turns) face = (face + when (t) { 0 -> 1; 1 -> 3; else -> 2 }) % 4
                val words = turns.joinToString(", then ") { when (it) { 0 -> "turn right"; 1 -> "turn left"; else -> "U-turn" } }
                val (opts, idx) = shuffleWithAnswer(ARROWS, face, r)
                mk(GameType.DIRECTION, "dr2|$i|${turns.joinToString("")}",
                    "⬆ North   ➡ East   ⬇ South   ⬅ West",
                    "You face ${COMPASS[i]}. You $words.\n\nWhere are you facing now?", opts, idx)
            }
        }
    }

    // ---------------- 17 Image matching (card pairs) ----------------

    private val CARD_EMOJI = listOf("🐶", "🐱", "🍎", "🚗", "⚽", "🌙", "⭐", "🎸", "🍕", "🦋", "🌵", "🎈", "🍉", "🐢", "🚀")

    private fun imageMatch(d: Int, r: Random): Puzzle {
        val pairs = when (d) { 0 -> 3; 1 -> 4; else -> 6 }
        val picks = CARD_EMOJI.shuffled(r).take(pairs)
        val cards = (picks + picks).shuffled(r)
        return mk(GameType.IMAGE_MATCH, "im|${cards.joinToString("")}",
            "Flip the cards and find all the matching pairs", items = cards)
    }

    // ---------------- 18 Find the different one ----------------

    private val LOOKALIKES = listOf("O" to "Q", "5" to "S", "M" to "N", "E" to "F", "b" to "d", "p" to "q", "8" to "B", "2" to "Z", "0" to "O", "W" to "V")

    private fun differentShape(d: Int, r: Random): Puzzle {
        val n = 3 + d
        val (x, y) = r.pick(LOOKALIKES)
        val swap = r.nextBoolean()
        val common = if (swap) x else y
        val odd = if (swap) y else x
        val pos = r.nextInt(n * n)
        val cells = (0 until n * n).map { if (it == pos) odd else common }
        return mk(GameType.DIFFERENT_SHAPE, "ds|$common$odd|$n|$pos", "Tap the one that is different",
            items = cells, answer = pos, ints = listOf(n))
    }

    // ---------------- 19 Brain teasers ----------------

    private class Tease(val q: String, val a: String, val wrong: List<String>)

    private val TEASERS = listOf(
        Tease("What has keys but can't open locks?", "A piano", listOf("A map", "A window", "A door")),
        Tease("What gets wetter the more it dries?", "A towel", listOf("A sponge", "Rain", "Soap")),
        Tease("What can you catch but not throw?", "A cold", listOf("A ball", "A fish", "Light")),
        Tease("What goes up but never comes down?", "Your age", listOf("A balloon", "Smoke", "Rain")),
        Tease("How many months have 28 days?", "All 12", listOf("1", "2", "6")),
        Tease("What has hands but can't clap?", "A clock", listOf("Gloves", "A statue", "A robot")),
        Tease("What begins with T, ends with T and has T inside it?", "A teapot", listOf("A tent", "A tablet", "A tight")),
        Tease("Which is heavier: 1 kg of cotton or 1 kg of iron?", "They weigh the same", listOf("Cotton", "Iron", "Depends")),
        Tease("What has a head and a tail but no body?", "A coin", listOf("A snake", "A comet", "An arrow")),
        Tease("I speak without a mouth and hear without ears. What am I?", "An echo", listOf("The wind", "A ghost", "A radio")),
        Tease("What has a neck but no head?", "A bottle", listOf("A shirt", "A giraffe", "A road")),
        Tease("What is full of holes but still holds water?", "A sponge", listOf("A bucket", "A net", "A cloud")),
        Tease("What is always in front of you but can't be seen?", "The future", listOf("Air", "Your shadow", "A wall")),
        Tease("If a red house is made of red bricks, what is a greenhouse made of?", "Glass", listOf("Green bricks", "Wood", "Plants")),
        Tease("What has one eye but cannot see?", "A needle", listOf("A coin", "A cup", "A book")),
        Tease("A rooster lays an egg on top of a roof. Which way does it roll?", "Roosters don't lay eggs", listOf("Left", "Right", "Straight down")),
        Tease("How many letters are in the phrase 'THE ALPHABET'?", "11", listOf("10", "12", "26")),
        Tease("What comes down but never goes up?", "Rain", listOf("A kite", "Smoke", "A lift")),
        Tease("The more you take, the more you leave behind. What are they?", "Footsteps", listOf("Money", "Photos", "Time")),
        Tease("What word is spelled wrong in every dictionary?", "Wrong", listOf("Misspell", "Error", "Spelling"))
    )

    private fun brainTeaser(d: Int, r: Random): Puzzle {
        val kind = r.nextInt(4)
        if (kind == 0 || d == 0 && kind == 1) {
            val t = r.pick(TEASERS)
            val (opts, idx) = shuffleWithAnswer(listOf(t.a) + t.wrong, 0, r)
            return mk(GameType.BRAIN_TEASER, "bt0|${t.q}", "Think carefully - it's a trick!", t.q, opts, idx)
        }
        return when (kind) {
            1 -> {
                val total = r.nextInt(8, 30)
                val left = r.nextInt(2, 7)
                val (opts, idx) = numChoices(left, r, 4)
                mk(GameType.BRAIN_TEASER, "bt1|$total|$left", "Think carefully - it's a trick!",
                    "A farmer has $total sheep. All but $left of them run away. How many are left?", opts, idx)
            }
            2 -> {
                val n1 = r.nextInt(3, 7)
                val gap = r.nextInt(2, 6)
                val t = (n1 - 1) * gap
                val n2 = r.nextInt(n1 + 1, 13)
                val ans = (n2 - 1) * gap
                val (opts, idx) = numChoices(ans, r, 4)
                mk(GameType.BRAIN_TEASER, "bt2|$n1|$gap|$n2", "Count the gaps, not the strikes",
                    "A clock takes $t seconds to strike $n1 o'clock. How many seconds does it take to strike $n2 o'clock?", opts, idx)
            }
            else -> {
                val n = r.nextInt(3, 6)
                val gap = r.nextInt(2, 5) * 10
                val ans = (n - 1) * gap
                val (opts, idx) = numChoices(ans, r, 20)
                mk(GameType.BRAIN_TEASER, "bt3|$n|$gap", "Count the gaps, not the pills",
                    "You take the first of $n pills now, then one more every $gap minutes. How many minutes until you have taken all $n?",
                    opts, idx)
            }
        }
    }

    // ---------------- 20 Reaction ----------------

    private fun reaction(d: Int, r: Random): Puzzle {
        val delays = (0 until 3).map { r.nextInt(1200, 3600) }
        return mk(GameType.REACTION, "re|${delays.joinToString(",")}",
            "Wait for GREEN, then tap as fast as you can. Don't tap on red!",
            items = delays.map { it.toString() }, ints = listOf(1500 - d * 350))
    }

    // ---------------- Reel games ----------------

    private fun tapCounter(d: Int, r: Random): Puzzle {
        val need = 14 + d * 5 + r.nextInt(0, 6)
        val secs = 10 - d
        val variant = r.nextInt(1000)
        return mk(GameType.TAP_COUNTER, "tc|$need|$secs|$variant", "Tap the button $need times before the time runs out!",
            ints = listOf(need, secs, variant))
    }

    private fun shake(d: Int, r: Random): Puzzle {
        val need = 12 + d * 8 + r.nextInt(0, 5)
        val variant = r.nextInt(1000)
        return mk(GameType.SHAKE, "sh|$need|$variant", "Shake your phone vigorously until the ring is full", ints = listOf(need, variant))
    }

    private val PANEL_NAMES = listOf("LIGHT", "SHIELD", "TEMP", "STABILITY", "REACTOR", "POWER", "COOLANT", "SIGNAL", "ENGINE", "RADAR", "PRESSURE", "OXYGEN")
    private val CHOICES = listOf("ALPHA", "BETA", "GAMMA", "DELTA")

    private fun controlPanel(d: Int, r: Random): Puzzle {
        val names = PANEL_NAMES.shuffled(r).take(3 + d)
        val tasks = names.map { name ->
            when (r.nextInt(3)) {
                0 -> "T|$name|${r.nextBoolean()}"
                1 -> "V|$name|${r.nextInt(1, 10) * 10}"
                else -> "C|$name|${r.pick(CHOICES)}|${CHOICES.joinToString(",")}"
            }
        }
        return mk(GameType.CONTROL_PANEL, "cp|${tasks.joinToString(";")}",
            "Match the target configuration. Complete the tasks in order", items = tasks)
    }

    private fun balance(d: Int, r: Random): Puzzle {
        val hold = listOf(10, 12, 15)[d]
        val inner = listOf(45, 40, 36)[d]
        val drift = listOf(22, 32, 42)[d]
        val angle = r.nextInt(360)
        return mk(GameType.BALANCE, "bl|$hold|$inner|$drift|$angle",
            "Use the D-pad to guide the ball. Hold it inside the ring for $hold seconds",
            ints = listOf(hold, inner, drift, angle))
    }

    // ---------------- Original CARFAM games ----------------

    private fun simon(d: Int, r: Random): Puzzle {
        val seq = (0 until 3 + d).map { r.nextInt(4) }
        return mk(GameType.SIMON, "sim|${seq.joinToString("")}", "Watch the pattern, then repeat it",
            items = seq.map { it.toString() })
    }

    private fun catchDot(d: Int, r: Random): Puzzle {
        val need = 6 + d * 2
        val maxMiss = 5 - d
        val life = 1600 - d * 300
        return mk(GameType.CATCH_DOT, "cd|$need|$maxMiss|$life|${r.nextInt(10000)}",
            "Tap $need dots before you miss ${maxMiss + 1}!", ints = listOf(need, maxMiss, life))
    }

    private fun swipeArrows(d: Int, r: Random): Puzzle {
        val rounds = (0 until 5 + d).map { "${r.nextInt(4)}|${if (r.nextInt(3) < 1 + d) 1 else 0}" }
        return mk(GameType.SWIPE_ARROWS, "sw|${rounds.joinToString(";")}",
            "Swipe the SAME way as the arrow - or OPPOSITE when told", items = rounds)
    }

    private fun sliderLock(d: Int, r: Random): Puzzle {
        val target = r.nextInt(5, 96)
        val tol = listOf(6, 4, 3)[d]
        return mk(GameType.SLIDER_LOCK, "sl|$target|$tol", "Set the slider to $target, then press LOCK",
            ints = listOf(target, tol))
    }
}
