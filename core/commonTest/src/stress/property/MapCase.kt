/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.property

import tests.IntWrapper
import kotlin.random.Random

/**
 * A map value that is cheap to compare structurally and to duplicate referentially.
 *
 * Two separately constructed `Value(3)` are `==` but not `===`, which the harness needs: `put`
 * detects a no-op referentially (`value === existing`) while `remove(key, value)` matches
 * structurally (`value == existing`), and `putAll` is specified to let the argument's value win.
 * A generator that reuses one instance per logical value can observe none of that.
 */
internal class Value(val id: Int) {
    override fun equals(other: Any?): Boolean = other is Value && other.id == id
    override fun hashCode(): Int = id
    override fun toString(): String = "Value($id)"
}

/**
 * One generated test case, held as data rather than as a random walk.
 *
 * Generation is the only randomised step. Running, shrinking and rendering are pure functions of a
 * case, which is what makes a failure reproducible without carrying a seed through the RNG, and what
 * lets the shrinker be plain deletion.
 */
internal class MapCase(
    val left: List<IntWrapper?>,
    val right: List<IntWrapper?>,
    val valueSalt: Int,
    val origin: String
) {
    val keys: List<IntWrapper?> get() = (left + right).distinct()

    /**
     * Values are derived from the key rather than stored, so that dropping a key during shrinking
     * cannot leave the case inconsistent. [valueSalt] is what makes the two sides disagree on a
     * shared key, which is the only way to observe `putAll`'s argument-wins contract.
     */
    fun valueFor(key: IntWrapper?, salt: Int): Value? =
        if (key.payload % 5 == 0) null else Value(key.payload + salt)

    fun leftValue(key: IntWrapper?): Value? = valueFor(key, 0)

    fun rightValue(key: IntWrapper?): Value? = valueFor(key, valueSalt)

    fun withLeft(keys: List<IntWrapper?>): MapCase = MapCase(keys, right, valueSalt, origin)

    fun withRight(keys: List<IntWrapper?>): MapCase = MapCase(left, keys, valueSalt, origin)

    fun withSalt(salt: Int): MapCase = MapCase(left, right, salt, origin)

    val size: Int get() = left.size + right.size

    override fun toString(): String = render()

    /**
     * Renders the case as a compilable test body. A random failure is only useful once it is a
     * committable regression, and this is what the reporter prints after shrinking.
     */
    fun render(): String = buildString {
        appendLine("    @Test")
        appendLine("    fun `regression from $origin`() {")
        for (key in keys) {
            if (key == null) continue
            appendLine("        val k${key.obj} = IntWrapper(${key.obj}, ${renderHash(key.hashCode())})")
        }
        appendLine()
        appendLine("        val left = persistentHashMapOf(${left.joinToString { entry(it, leftValue(it)) }})")
        appendLine("        val right = persistentHashMapOf(${right.joinToString { entry(it, rightValue(it)) }})")
        appendLine()
        appendLine("        val expected = left.toMap() + right.toMap()")
        appendLine("        assertEquals(expected, left.puttingAll(right))")
        appendLine("        assertEquals(expected, left.builder().apply { putAll(right) }.build())")
        appendLine("    }")
    }

    private fun entry(key: IntWrapper?, value: Value?): String {
        val name = if (key == null) "null" else "k${key.obj}"
        return "$name to ${if (value == null) "null" else "Value(${value.id})"}"
    }

    private fun renderHash(hash: Int): String = when {
        hash == Int.MIN_VALUE -> "Int.MIN_VALUE"
        hash == Int.MAX_VALUE -> "Int.MAX_VALUE"
        hash == 0 -> "0"
        hash and KeyUniverse.LOW_BITS == 0 -> "${hash ushr 30} shl 30"
        hash ushr 30 == 0 -> "$hash"
        else -> "${hash and KeyUniverse.LOW_BITS} or (${hash ushr 30} shl 30)"
    }
}

/** Generates a case whose two operands stand in [relation], or `null` if the universe cannot. */
internal fun generateMapCase(random: Random, relation: OperandRelation, universe: KeyUniverse): MapCase? {
    val (left, right) = universe.operandPair(random, relation) ?: return null
    // Salt 0 leaves the two sides agreeing on shared keys by value but not by identity, which is
    // what exposes referential no-op detection. A non-zero salt makes them genuinely disagree.
    val salt = if (random.nextBoolean()) 0 else 1000
    return MapCase(left, right, salt, "$relation")
}

/**
 * Enumerates the bottom of the trie instead of sampling it. Only four cells exist at shift 30, so
 * every subset of the exhaustive universe is affordable, and randomness is better spent elsewhere.
 */
internal fun maxShiftCases(keyCount: Int = 6): List<MapCase> {
    // The first six keys keep both collision groups and one lone key, which is everything the
    // node-versus-element shape needs. The full eight are for the long tier.
    val keys = maxShiftUniverse().keys.take(keyCount)
    val cases = mutableListOf<MapCase>()
    for (leftMask in 0..<(1 shl keys.size)) {
        val left = keys.filterIndexed { i, _ -> (leftMask shr i) and 1 == 1 }
        if (left.isEmpty()) continue
        for (rightMask in 0..<(1 shl keys.size)) {
            val right = keys.filterIndexed { i, _ -> (rightMask shr i) and 1 == 1 }
            if (right.isEmpty()) continue
            cases += MapCase(left, right, 1000, "maxShift[$leftMask,$rightMask]")
        }
    }
    return cases
}

/**
 * Enumerates subsets of [nullKeyUniverse], so that a null key is swept through the same operand
 * pairings as any other. It only differs where the trie cares: at hash zero, in a collision node.
 */
internal fun nullKeyCases(): List<MapCase> {
    val keys = nullKeyUniverse().keys
    val cases = mutableListOf<MapCase>()
    for (leftMask in 0..<(1 shl keys.size)) {
        val left = keys.filterIndexed { i, _ -> (leftMask shr i) and 1 == 1 }
        if (left.isEmpty()) continue
        for (rightMask in 0..<(1 shl keys.size)) {
            val right = keys.filterIndexed { i, _ -> (rightMask shr i) and 1 == 1 }
            if (right.isEmpty()) continue
            cases += MapCase(left, right, 1000, "nullKey[$leftMask,$rightMask]")
        }
    }
    return cases
}
