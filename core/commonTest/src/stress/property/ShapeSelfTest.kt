/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.property

import kotlinx.collections.immutable.implementations.immutableMap.MAX_SHIFT
import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import tests.IntWrapper
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Proves that each generator profile really produces the trie shape it claims, by looking at the
 * shape rather than at the hashes.
 *
 * Without this the harness can be silently vacuous — which is exactly how the existing stress suite
 * failed. Every one of its collision tests draws hashes from `[0, elementCount)`, so bits above the
 * twelfth are always clear and the bottom of the trie was never reached, for years, while the tests
 * read as thorough collision coverage.
 */
class ShapeSelfTest {

    private fun mapOf(keys: List<IntWrapper?>): PersistentHashMap<IntWrapper?, Int> {
        var map = PersistentHashMap.emptyOf<IntWrapper?, Int>()
        for (key in keys) map = map.putting(key, key.payload)
        return map
    }

    private fun clusterOf(profile: HashProfile, seed: Int, count: Int): List<IntWrapper?> {
        val universe = generateUniverse(Random(seed), listOf(profile), count..count)
        return universe.keys
    }

    private fun segmentAt(hash: Int, shift: Int): Int = (hash shr shift) and 0b11111

    @Test
    fun oneBucketPutsEveryKeyIntoASingleCollisionNode() {
        val keys = clusterOf(HashProfile.OneBucket, seed = 1, count = 4)
        val shape = mapOf(keys).shape()

        assertEquals(listOf(0, 5, 10, 15, 20, 25, 30, 35), shape.nodes.map { it.shift })
        assertEquals(8, shape.depth)
        assertEquals(1, shape.collisionNodes.size)
        assertEquals(keys.size, shape.collisionNodes.single().entryCount)
        assertEquals(emptyList(), shape.violations(expectedSize = keys.size))
    }

    @Test
    fun topBitsReachesTheShift30Level() {
        val keys = clusterOf(HashProfile.TopBits, seed = 2, count = 5)
        val shape = mapOf(keys).shape()

        val deepest = shape.nodes.filter { it.shift == MAX_SHIFT }
        assertEquals(1, deepest.size, "expected exactly one shift-30 node in $shape")
        assertTrue(
            deepest.single().entryCount + deepest.single().childCount >= 2,
            "the shift-30 node should branch, got ${deepest.single()}"
        )
        assertEquals(emptyList(), shape.violations(expectedSize = keys.size))
    }

    @Test
    fun deepPrefixChainsForExactlyAsLongAsTheSegmentsAgree() {
        val keys = clusterOf(HashProfile.DeepPrefix, seed = 3, count = 4)
        val hashes = keys.map { it.hashCode() }

        var shared = 0
        while (shared < 6 && hashes.map { segmentAt(it, shared * 5) }.distinct().size == 1) shared++
        assertTrue(shared >= 1, "the profile should share at least one segment, hashes were $hashes")

        val shape = mapOf(keys).shape()
        for (level in 0..<shared) {
            val node = shape.nodes.single { it.shift == level * 5 }
            assertEquals(
                1, node.entryCount + node.childCount,
                "node at shift ${level * 5} should have a single child while segments agree"
            )
        }
        val divergence = shape.nodes.single { it.shift == shared * 5 }
        assertTrue(
            divergence.entryCount + divergence.childCount >= 2,
            "the keys should split at shift ${shared * 5}, got $divergence"
        )
        assertEquals(emptyList(), shape.violations(expectedSize = keys.size))
    }

    @Test
    fun fullRangeStaysShallowAndAvoidsCollisions() {
        val keys = clusterOf(HashProfile.FullRange, seed = 4, count = 5)
        val shape = mapOf(keys).shape()

        assertEquals(emptyList(), shape.collisionNodes)
        assertTrue(shape.depth <= 3, "full-range hashes should stay shallow, got depth ${shape.depth}")
        assertEquals(emptyList(), shape.violations(expectedSize = keys.size))
    }

    @Test
    fun extremesCoverEveryCellOfTheDeepestLevel() {
        // Only four cells exist at shift 30, and the sign boundaries hit all of them: an arithmetic
        // shift makes 0 and MAX_VALUE land in cells 0 and 1, MIN_VALUE and -1 in cells 30 and 31.
        val cells = intArrayOf(0, Int.MAX_VALUE, Int.MIN_VALUE, -1).map { segmentAt(it, MAX_SHIFT) }

        assertEquals(MAX_SHIFT_SEGMENTS.toList(), cells.sorted().distinct().sorted())
    }

    @Test
    fun theExhaustiveUniverseFillsAllFourBottomCells() {
        val universe = maxShiftUniverse()
        val shape = mapOf(universe.keys).shape()

        val bottom = shape.nodes.single { it.shift == MAX_SHIFT }
        // Two collision groups in cells 0 and 1, two lone keys in cells 30 and 31.
        assertEquals(0b11, bottom.nodeMap)
        assertEquals((1 shl 30) or (1 shl 31), bottom.dataMap)
        assertEquals(2, shape.collisionNodes.size)
        assertEquals(emptyList(), shape.violations(expectedSize = universe.keys.size))
    }

    @Test
    fun collisionVsLoneOperandsPlaceANodeOppositeAnElement() {
        // The load-bearing self-test. If this stops holding, the harness no longer reaches the shape
        // that produced #294 and #300, and everything downstream is testing shallow tries.
        val universe = maxShiftUniverse()
        val (left, right) = assertNotNull(
            universe.operandPair(Random(5), OperandRelation.CollisionVsLone),
            "the exhaustive universe must be able to express CollisionVsLone"
        )

        val leftShape = mapOf(left).shape()
        val rightShape = mapOf(right).shape()

        val leftBottom = leftShape.nodes.single { it.shift == MAX_SHIFT }
        val rightBottom = rightShape.nodes.single { it.shift == MAX_SHIFT }
        assertEquals(
            leftBottom.hashPrefix, rightBottom.hashPrefix,
            "both trees must reach the same shift-30 cell block"
        )

        val sharedCells = leftBottom.nodeMap and rightBottom.dataMap
        assertTrue(
            sharedCells != 0,
            "expected a cell holding a collision node on the left and a lone element on the right, " +
                "left=$leftBottom right=$rightBottom"
        )
        assertEquals(1, leftShape.collisionNodes.size)
        assertEquals(emptyList(), rightShape.collisionNodes)
    }

    @Test
    fun aNullKeySitsInsideTheHashZeroCollisionNode() {
        // The one place a null key is not just another key: `null.hashCode()` is 0, so it shares a
        // collision node with every key of hash zero instead of sitting in its own cell.
        val group = nullKeyUniverse().keys.filter { it.hashCode() == 0 }
        assertTrue(null in group, "null must belong to the hash-zero group")

        val shape = mapOf(group).shape()
        val collision = shape.collisionNodes.single()
        assertEquals(group.size, collision.entryCount)
        assertTrue(
            List(collision.entryCount) { collision.keyAt(it) }.contains(null),
            "the collision node should hold the null key, holds ${List(collision.entryCount) { collision.keyAt(it) }}"
        )
        assertEquals(emptyList(), shape.violations(expectedSize = group.size))
    }

    @Test
    fun theWideUniverseFillsANodeCompletely() {
        val universe = wideUniverse()
        val shape = mapOf(universe.keys).shape()
        val root = shape.nodes.first()

        assertEquals(0, root.shift)
        assertEquals(32, root.entryCount + root.childCount, "the root should use every cell, got $root")
        assertEquals(8, root.childCount, "expected eight cells to hold a sub-node, got $root")
        assertEquals(24 * 2 + 8, root.buffer.size)
        assertEquals(emptyList(), shape.violations(expectedSize = universe.keys.size))
    }

    @Test
    fun everyRelationTheExhaustiveUniverseClaimsIsBuildable() {
        val universe = maxShiftUniverse()
        val unbuildable = OperandRelation.entries.filter { universe.operandPair(Random(6), it) == null }

        assertEquals(emptyList(), unbuildable)
    }

    @Test
    fun aMixedUniverseStillProducesCollisionGroupsAndAPusher() {
        val universe = generateUniverse(Random(7))

        assertTrue(universe.collisionGroups.isNotEmpty(), "no collision group in $universe")
        assertTrue(
            universe.collisionGroups.any { universe.pushersFor(it).isNotEmpty() },
            "no collision group has a key that pushes it down to shift 30, in $universe"
        )
    }
}
