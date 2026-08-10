/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.property

import kotlinx.collections.immutable.implementations.immutableMap.MAX_SHIFT
import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.implementations.immutableMap.TrieNode
import tests.IntWrapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Pins the structural walker against shapes whose form is known independently, and — more
 * importantly — proves the validator has teeth by handing it deliberately malformed tries built
 * straight through `TrieNode`'s internal constructor. A validator that never fires is worse than
 * no validator, because it reads as coverage.
 */
class MapTrieShapeTest {

    private fun mapOf(vararg entries: Pair<IntWrapper, Int>): PersistentHashMap<IntWrapper, Int> {
        var map = PersistentHashMap.emptyOf<IntWrapper, Int>()
        for ((key, value) in entries) map = map.putting(key, value)
        return map
    }

    @Test
    fun emptyRootIsTheOnlyLegalEmptyNode() {
        val shape = PersistentHashMap.emptyOf<IntWrapper, Int>().shape()

        assertEquals(1, shape.nodes.size)
        assertEquals(0, shape.entryCount)
        assertEquals(emptyList(), shape.violations(expectedSize = 0))
    }

    @Test
    fun fullHashCollisionChainsDownToACollisionNode() {
        // Equal hash codes in full, so the two keys can only be separated below MAX_SHIFT.
        val shape = mapOf(IntWrapper(1, 0b1) to 1, IntWrapper(2, 0b1) to 2).shape()

        // Six intermediate levels plus the shift-30 parent, then the collision node itself.
        assertEquals(listOf(0, 5, 10, 15, 20, 25, 30, 35), shape.nodes.map { it.shift })
        assertEquals(1, shape.collisionNodes.size)

        val collision = shape.collisionNodes.single()
        assertTrue(collision.shift > MAX_SHIFT)
        assertEquals(0b1, collision.hashPrefix)
        assertEquals(2, collision.entryCount)
        assertEquals(2, shape.entryCount)
        assertEquals(emptyList(), shape.violations(expectedSize = 2))
    }

    @Test
    fun topBitsAloneReachTheDeepestLevel() {
        // Agreeing on bits 0..29 and differing only in bits 30..31 is the one way to force a
        // shift-30 branch node without a full hash collision.
        val shape = mapOf(IntWrapper(1, 0) to 1, IntWrapper(2, 1 shl 30) to 2).shape()

        assertEquals(listOf(0, 5, 10, 15, 20, 25, 30), shape.nodes.map { it.shift })
        assertEquals(0, shape.collisionNodes.size)

        val deepest = shape.nodes.last()
        assertEquals(MAX_SHIFT, deepest.shift)
        assertEquals(2, deepest.entryCount)
        assertEquals(emptyList(), shape.violations(expectedSize = 2))
    }

    @Test
    fun shallowMapsHaveNoViolations() {
        val shape = mapOf(
            IntWrapper(1, 1) to 1,
            IntWrapper(2, 2) to 2,
            IntWrapper(3, 33) to 3,
            IntWrapper(4, 65) to 4,
            IntWrapper(5, -1) to 5,
            IntWrapper(6, Int.MIN_VALUE) to 6,
            IntWrapper(7, Int.MAX_VALUE) to 7,
        ).shape()

        assertEquals(emptyList(), shape.violations(expectedSize = 7))
    }

    @Test
    fun digestIgnoresCollisionEntryOrder() {
        val a = IntWrapper(1, 7)
        val b = IntWrapper(2, 7)

        // Single put prepends into a collision node, so these two build orders leave the same
        // entries in opposite buffer positions.
        val forward = mapOf(a to 1, b to 2)
        val backward = mapOf(b to 2, a to 1)

        assertNotEquals(
            forward.node.buffer.toList(),
            backward.node.buffer.toList(),
            "the build orders were expected to differ somewhere in the trie"
        )
        assertEquals(forward.shape().digest(), backward.shape().digest())
        assertEquals(forward, backward)
    }

    @Test
    fun digestSeparatesDifferentContents() {
        val one = mapOf(IntWrapper(1, 1) to 1).shape().digest()
        val other = mapOf(IntWrapper(1, 1) to 2).shape().digest()

        assertNotEquals(one, other)
    }

    @Test
    fun validatorCatchesANonRootNodeHoldingASingleEntry() {
        // What PR #218 restored on the mutable path: such a node must be promoted into its parent.
        val child = TrieNode<IntWrapper, Int>(0b1, 0, arrayOf(IntWrapper(1, 0), 1))
        val root = TrieNode<IntWrapper, Int>(0, 0b1, arrayOf(child))

        val problems = root.shape().violations()

        assertEquals(1, problems.size, "expected exactly one violation, got $problems")
        assertTrue("must be promoted" in problems.single(), problems.single())
    }

    @Test
    fun validatorCatchesAKeyInTheWrongCell() {
        // Hash 0 belongs in cell 0. The bitmap claims cell 1, which is the shape a garbage hash
        // segment leaves behind, and how issue #294 corrupted a trie.
        val root = TrieNode<IntWrapper, Int>(0b10, 0, arrayOf(IntWrapper(1, 0), 1))

        val problems = root.shape().violations()

        assertEquals(1, problems.size, "expected exactly one violation, got $problems")
        assertTrue("hashes to segment 0" in problems.single(), problems.single())
    }

    @Test
    fun validatorCatchesABufferThatDisagreesWithTheBitmaps() {
        val root = TrieNode<IntWrapper, Int>(0b11, 0, arrayOf(IntWrapper(1, 0), 1))

        val problems = root.shape().violations()

        assertEquals(1, problems.size, "expected exactly one violation, got $problems")
        assertTrue("does not match the bitmaps" in problems.single(), problems.single())
    }

    @Test
    fun validatorCatchesASingleEntryCollisionNode() {
        val key = IntWrapper(1, 0)
        var root: TrieNode<IntWrapper, Int> = TrieNode(0, 0, arrayOf(key, 1))
        // Bury it below MAX_SHIFT so the walk classifies it as a collision node.
        repeat(times = 7) { root = TrieNode(0, 0b1, arrayOf(root)) }

        val problems = root.shape().violations()

        assertEquals(1, problems.size, "expected exactly one violation, got $problems")
        assertTrue("at least two entries" in problems.single(), problems.single())
    }

    @Test
    fun validatorCatchesSizeDrift() {
        val problems = mapOf(IntWrapper(1, 1) to 1).shape().violations(expectedSize = 2)

        assertEquals(1, problems.size, "expected exactly one violation, got $problems")
        assertTrue("size reports 2" in problems.single(), problems.single())
    }
}
