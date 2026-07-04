/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.set

import kotlinx.collections.immutable.persistentHashSetOf
import tests.IntWrapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PersistentHashSetExtraTest {

    // All four elements share hash code 0. Adding any two of them to a hash set creates
    // a chain of single-cell nodes down to the maximal shift ending in a collision node,
    // and every subsequent operation on them has to descend that chain.
    private val a = IntWrapper(1, 0)
    private val b = IntWrapper(2, 0)
    private val c = IntWrapper(3, 0)
    private val d = IntWrapper(4, 0)

    @Test
    fun `adding elements with equal hash codes to a persistent set builds a working collision chain`() {
        val twoElements = persistentHashSetOf<IntWrapper>().adding(a).adding(b)
        assertEquals(setOf(a, b), twoElements)

        val threeElements = twoElements.adding(c)
        assertEquals(setOf(a, b, c), threeElements)
        assertTrue(threeElements.containsAll(listOf(a, b, c)))
        assertFalse(threeElements.contains(d))

        // adding a colliding element that is already present must return the same instance
        assertSame(threeElements, threeElements.adding(b))
        // the original two-element set is not affected by the third addition
        assertEquals(setOf(a, b), twoElements)
    }

    @Test
    fun `removing elements with equal hash codes from a persistent set unwinds the collision chain`() {
        val set = persistentHashSetOf(a, b, c)

        val withoutB = set.removing(b)
        assertEquals(setOf(a, c), withoutB)
        assertFalse(withoutB.contains(b))

        // removing an absent element whose hash code leads to the collision node is a no-op
        assertSame(set, set.removing(d))

        // removing down to a single element lifts it out of the collision chain to the root
        val single = withoutB.removing(a)
        assertEquals(setOf(c), single)
        assertTrue(single.contains(c))
        // now the cell for this hash code holds a plain element that does not match
        assertSame(single, single.removing(d))

        assertEquals(setOf(a, b, c), set)
    }

    @Test
    fun `builder add and remove operate on a collision chain shared with the persistent set`() {
        val set = persistentHashSetOf(a, b)
        val builder = set.builder()

        assertTrue(builder.add(c))       // has to copy the chain nodes shared with the set
        assertFalse(builder.add(a))      // already present in the collision node
        assertFalse(builder.remove(d))   // absent element with the colliding hash code
        assertTrue(builder.remove(b))

        assertEquals(2, builder.size)
        assertEquals(setOf(a, c), builder.build())
        assertEquals(setOf(a, b), set)   // the source set is not affected

        val flatBuilder = persistentHashSetOf(a).builder()
        assertFalse(flatBuilder.remove(d)) // the cell for this hash code holds a different element
        assertEquals(1, flatBuilder.size)
        assertEquals(setOf(a), flatBuilder.build())
    }

    @Test
    fun `builder iterator remove keeps iterating the remaining collision node elements`() {
        val set = persistentHashSetOf(a, b, c)
        val builder = set.builder()
        val iterator = builder.iterator()

        val removed = iterator.next()
        iterator.remove() // two colliding elements remain, so the iterator re-enters the collision node

        val rest = mutableListOf<IntWrapper>()
        while (iterator.hasNext()) {
            rest += iterator.next()
        }

        assertEquals(2, rest.size)
        assertEquals(setOf(a, b, c) - removed, rest.toSet())
        assertFalse(removed in builder)
        assertEquals(setOf(a, b, c) - removed, builder.build())
        assertEquals(setOf(a, b, c), set)
    }
}
