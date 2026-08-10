/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.property

import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.implementations.persistentOrderedMap.LinkedValue
import kotlinx.collections.immutable.implementations.persistentOrderedMap.PersistentOrderedMap
import kotlinx.collections.immutable.implementations.persistentOrderedMap.PersistentOrderedMapBuilder
import kotlinx.collections.immutable.internal.EndOfChain
import tests.IntWrapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins the chain walker against orders that are known independently, and proves it catches the
 * malformations that content checks cannot see. The chains here are built straight through the
 * internal constructors, because no sequence of public calls is supposed to produce them.
 */
class OrderedChainTest {

    private val a = IntWrapper(1, 1)
    private val b = IntWrapper(2, 2)
    private val c = IntWrapper(3, 3)

    private fun orderedOf(vararg entries: Pair<IntWrapper, String>): PersistentOrderedMap<IntWrapper, String> {
        var map = PersistentOrderedMap.emptyOf<IntWrapper, String>()
        for ((key, value) in entries) map = map.putting(key, value)
        return map
    }

    /** Hand-builds a map whose chain is whatever the caller says, without going through the API. */
    private fun handBuilt(
        firstKey: Any?,
        lastKey: Any?,
        links: List<Triple<IntWrapper, Any?, Any?>>
    ): PersistentOrderedMap<IntWrapper, String> {
        var hashMap = PersistentHashMap.emptyOf<IntWrapper, LinkedValue<String>>()
        for ((key, previous, next) in links) {
            hashMap = hashMap.putting(key, LinkedValue("v${key.obj}", previous, next))
        }
        return PersistentOrderedMap(firstKey, lastKey, hashMap)
    }

    @Test
    fun anEmptyMapHasAnEmptyChain() {
        val map = PersistentOrderedMap.emptyOf<IntWrapper, String>()

        assertEquals(emptyList(), map.chain().forward)
        assertEquals(emptyList(), map.orderedViolations())
    }

    @Test
    fun theChainFollowsInsertionOrder() {
        val map = orderedOf(a to "A", b to "B", c to "C")

        assertEquals(listOf(a, b, c), map.chain().forward)
        assertEquals(emptyList(), map.orderedViolations())
    }

    @Test
    fun replacingAValueKeepsThePosition() {
        val map = orderedOf(a to "A", b to "B", c to "C").putting(a, "A2")

        assertEquals(listOf(a, b, c), map.chain().forward)
        assertEquals(emptyList(), map.orderedViolations())
    }

    @Test
    fun removingFromEachEndAndFromTheMiddleKeepsTheChainWhole() {
        val map = orderedOf(a to "A", b to "B", c to "C")

        assertEquals(listOf(b, c), map.removing(a).chain().forward)
        assertEquals(listOf(a, c), map.removing(b).chain().forward)
        assertEquals(listOf(a, b), map.removing(c).chain().forward)
        for (key in listOf(a, b, c)) {
            assertEquals(emptyList(), map.removing(key).orderedViolations(), "after removing $key")
        }
    }

    @Test
    fun reinsertingAKeyMovesItToTheEnd() {
        val map = orderedOf(a to "A", b to "B", c to "C").removing(a).putting(a, "A")

        assertEquals(listOf(b, c, a), map.chain().forward)
        assertEquals(emptyList(), map.orderedViolations())
    }

    @Test
    fun theBuilderKeepsTheSameChain() {
        val builder = PersistentOrderedMapBuilder(orderedOf(a to "A", b to "B"))
        builder[c] = "C"
        builder.remove(a)

        assertEquals(listOf(b, c), builder.chain().forward)
        assertEquals(emptyList(), builder.chain().problems)
    }

    @Test
    fun theWalkerCatchesACyclicChain() {
        // Every content oracle passes here: the keys are right, the size is right, and the
        // iterators stop after `size` steps, so they never notice the chain does not end.
        val map = handBuilt(
            firstKey = a, lastKey = b,
            links = listOf(Triple(a, EndOfChain, b), Triple(b, a, a))
        )

        assertEquals(2, map.size)
        assertEquals(setOf(a, b), map.keys.toSet())

        val problems = map.chain().problems
        assertEquals(1, problems.size, "expected one problem, got $problems")
        assertTrue("cyclic" in problems.single(), problems.single())
    }

    @Test
    fun theWalkerCatchesADanglingLink() {
        val map = handBuilt(
            firstKey = a, lastKey = a,
            links = listOf(Triple(a, EndOfChain, c))
        )

        val problems = map.chain().problems
        assertEquals(1, problems.size, "expected one problem, got $problems")
        assertTrue("does not hold" in problems.single(), problems.single())
    }

    @Test
    fun theWalkerCatchesAKeyTheChainNeverReaches() {
        val map = handBuilt(
            firstKey = a, lastKey = b,
            links = listOf(Triple(a, EndOfChain, b), Triple(b, a, EndOfChain), Triple(c, EndOfChain, EndOfChain))
        )

        val problems = map.chain().problems
        assertEquals(1, problems.size, "expected one problem, got $problems")
        assertTrue("size reports 3" in problems.single(), problems.single())
    }

    @Test
    fun theWalkerCatchesABackPointerThatDisagrees() {
        val map = handBuilt(
            firstKey = a, lastKey = b,
            links = listOf(Triple(a, EndOfChain, b), Triple(b, EndOfChain, EndOfChain))
        )

        val problems = map.chain().problems
        assertEquals(1, problems.size, "expected one problem, got $problems")
        assertTrue("points back at" in problems.single(), problems.single())
    }
}
