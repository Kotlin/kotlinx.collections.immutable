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
    fun `builder bulk operations accept another builder as the argument`() {
        val retaining = persistentHashSetOf(1, 2, 3).builder()
        assertTrue(retaining.retainAll(persistentHashSetOf(2, 3, 4).builder()))
        assertEquals(setOf(2, 3), retaining.build())

        val removing = persistentHashSetOf(1, 2, 3).builder()
        assertTrue(removing.removeAll(persistentHashSetOf(2, 3, 4).builder()))
        assertEquals(setOf(1), removing.build())
    }

    @Test
    fun `bulk operations on an already-mutated builder work on its owned nodes in place`() {
        val adding = persistentHashSetOf(1, 2).builder()
        adding.add(3) // the builder owns the root from here on
        assertFalse(adding.addAll(persistentHashSetOf(1, 2)), "nothing new to add")
        assertEquals(setOf(1, 2, 3), adding.build())

        val retaining = persistentHashSetOf(1, 2, 3).builder()
        retaining.add(4)
        assertTrue(retaining.retainAll(persistentHashSetOf(1, 2)))
        assertEquals(setOf(1, 2), retaining.build())
        assertFalse(retaining.retainAll(persistentHashSetOf(1, 2, 7)), "everything already retained")
        assertEquals(setOf(1, 2), retaining.build())

        val removing = persistentHashSetOf(1, 2, 3).builder()
        removing.add(4)
        assertTrue(removing.removeAll(persistentHashSetOf(1, 4)))
        assertEquals(setOf(2, 3), removing.build())

        // the same retain and remove shapes against an owned collision node
        val collisionRetaining = persistentHashSetOf(a, b).builder()
        collisionRetaining.add(c)
        assertTrue(collisionRetaining.retainAll(persistentHashSetOf(a, c)))
        assertEquals(setOf(a, c), collisionRetaining.build())

        val collisionRemoving = persistentHashSetOf(a, b).builder()
        collisionRemoving.add(c)
        assertTrue(collisionRemoving.removeAll(persistentHashSetOf(a, b)))
        assertEquals(setOf(c), collisionRemoving.build())
    }

    @Test
    fun `bulk operations merge collision nodes of every relative shape`() {
        val left = persistentHashSetOf(a, b)

        // all elements already present: the same instance comes back
        assertSame(left, left.addingAll(persistentHashSetOf(a, b)))

        // fully disjoint collision nodes merge into one
        assertEquals(setOf(a, b, c, d), left.addingAll(persistentHashSetOf(c, d)))

        // retainAll leaving exactly one survivor lifts it out of the collision node
        val single = persistentHashSetOf(a, b).builder()
        assertTrue(single.retainAll(persistentHashSetOf(a, c)))
        assertEquals(setOf(a), single.build())

        // retainAll whose intersection is the whole argument
        val superset = persistentHashSetOf(a, b, c).builder()
        assertTrue(superset.retainAll(persistentHashSetOf(a, b)))
        assertEquals(setOf(a, b), superset.build())

        // a bulk identity operation on a set containing a collision node
        val withCollision = persistentHashSetOf(a, b, IntWrapper(9, 9))
        val builder = withCollision.builder()
        assertFalse(builder.addAll(withCollision), "adding a set to itself changes nothing")
        assertEquals(withCollision, builder.build())
    }

    @Test
    fun `bulk operations keep deep trie structures working`() {
        // 0, 32, 64 and 1024, 2048 share lower hash segments, forming multi-level chains
        val removeDeep = persistentHashSetOf(32, 1024, 2048).builder()
        assertTrue(removeDeep.removeAll(persistentHashSetOf(32)))
        assertEquals(setOf(1024, 2048), removeDeep.build())

        val retainDeep = persistentHashSetOf(32, 1024, 2048).builder()
        assertTrue(retainDeep.retainAll(persistentHashSetOf(33, 1024, 2048)))
        assertEquals(setOf(1024, 2048), retainDeep.build())

        val addInto = persistentHashSetOf(0, 32).builder()
        assertTrue(addInto.addAll(persistentHashSetOf(64)))
        assertEquals(setOf(0, 32, 64), addInto.build())

        // retainAll producing a subtree identical to the argument's shares it
        val one = persistentHashSetOf(0, 32, 64)
        val two = one.removing(64)
        val sharing = one.builder()
        assertTrue(sharing.retainAll(two))
        assertEquals(setOf(0, 32), sharing.build())

        // removing an element absent from an existing sub-node of a fresh builder
        val absentDeep = persistentHashSetOf(0, 32).builder()
        assertFalse(absentDeep.remove(64))
        assertEquals(setOf(0, 32), absentDeep.build())

        // containsAll saying no at both trie shapes
        assertFalse(persistentHashSetOf(1, 2).containsAll(persistentHashSetOf(1, 64)), "absent slot")
        assertFalse(persistentHashSetOf(1, 2).containsAll(persistentHashSetOf(34)), "occupied slot, different element")
        assertTrue(persistentHashSetOf(1, 2, 3).containsAll(persistentHashSetOf(1, 3)))
    }

    @Test
    fun `null elements flow through set operations`() {
        // null splits a cell into a sub-node and back
        val withNull = persistentHashSetOf<Int?>(null)
        assertEquals(setOf<Int?>(null, 32), withNull.adding(32))
        assertEquals(setOf<Int?>(1), persistentHashSetOf<Int?>(1, null) - null)

        // bulk operations carrying null through node merges, in both directions
        val nullIntoNodes = persistentHashSetOf<Int?>(0, 32).builder()
        assertTrue(nullIntoNodes.addAll(persistentHashSetOf<Int?>(null)))
        assertEquals(setOf<Int?>(0, 32, null), nullIntoNodes.build())
        val nodesIntoNull = persistentHashSetOf<Int?>(null).builder()
        assertTrue(nodesIntoNull.addAll(persistentHashSetOf<Int?>(0, 32)))
        assertEquals(setOf<Int?>(0, 32, null), nodesIntoNull.build())

        // two singleton entries sharing a position merge into a node, null on either side
        val nullMeets32 = persistentHashSetOf<Int?>(null).builder()
        assertTrue(nullMeets32.addAll(persistentHashSetOf<Int?>(32)))
        assertEquals(setOf<Int?>(null, 32), nullMeets32.build())
        val meets32Null = persistentHashSetOf<Int?>(32).builder()
        assertTrue(meets32Null.addAll(persistentHashSetOf<Int?>(null)))
        assertEquals(setOf<Int?>(null, 32), meets32Null.build())

        val retaining = persistentHashSetOf<Int?>(null, 1, 32).builder()
        assertTrue(retaining.retainAll(persistentHashSetOf<Int?>(null, 32)))
        assertEquals(setOf<Int?>(null, 32), retaining.build())

        val removing = persistentHashSetOf<Int?>(null, 0, 32).builder()
        assertTrue(removing.removeAll(persistentHashSetOf<Int?>(null, 32)))
        assertEquals(setOf<Int?>(0), removing.build())

        // a null entry meeting a node cell in retainAll and removeAll
        val retainNullVsNode = persistentHashSetOf<Int?>(null, 1).builder()
        assertTrue(retainNullVsNode.retainAll(persistentHashSetOf<Int?>(null, 32)))
        assertEquals(setOf<Int?>(null), retainNullVsNode.build())
        val removeNullVsNode = persistentHashSetOf<Int?>(null, 1).builder()
        assertTrue(removeNullVsNode.removeAll(persistentHashSetOf<Int?>(null, 32)))
        assertEquals(setOf<Int?>(1), removeNullVsNode.build())

        assertTrue(persistentHashSetOf<Int?>(null, 1).containsAll(persistentHashSetOf<Int?>(null)))
        assertTrue(persistentHashSetOf<Int?>(null, 32).containsAll(persistentHashSetOf<Int?>(null)))

        // iterator removal around a null that fully collides with a present hash-zero element
        val builder = persistentHashSetOf<Any?>(0, null).builder()
        val iterator = builder.iterator()
        val removed = mutableSetOf<Any?>()
        while (iterator.hasNext()) {
            removed.add(iterator.next())
            iterator.remove()
        }
        assertEquals(setOf<Any?>(0, null), removed)
        assertTrue(builder.isEmpty())
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
