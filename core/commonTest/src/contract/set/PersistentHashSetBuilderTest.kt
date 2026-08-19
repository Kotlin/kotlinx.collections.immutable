/*
 * Copyright 2016-2025 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.set

import kotlinx.collections.immutable.implementations.immutableSet.PersistentHashSet
import kotlinx.collections.immutable.implementations.immutableSet.PersistentHashSetBuilder
import kotlinx.collections.immutable.persistentHashSetOf
import tests.IntWrapper
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PersistentHashSetBuilderTest {

    private val a1 = IntWrapper(1, 0)
    private val a2 = IntWrapper(2, 0)
    private val a3 = IntWrapper(4, 0)
    private val sibling = IntWrapper(3, 1 shl 30)

    @Test
    fun `should correctly iterate after removing integer element`() {
        val removedElement = 0
        val set: PersistentHashSet<Int> =
            persistentHashSetOf(1, 2, 3, removedElement, 32)
                    as PersistentHashSet<Int>

        validate(set, removedElement)
    }

    @Test
    fun `should correctly iterate after removing IntWrapper element`() {
        val removedElement = IntWrapper(0, 0)
        val set: PersistentHashSet<IntWrapper> = persistentHashSetOf(
            removedElement,
            IntWrapper(1, 0),
            IntWrapper(2, 32),
            IntWrapper(3, 32)
        ) as PersistentHashSet<IntWrapper>

        validate(set, removedElement)
    }

    private fun <E> validate(set: PersistentHashSet<E>, removedElement: E) {
        val builder = set.builder()
        val iterator = builder.iterator()

        val expectedCount = set.size
        var actualCount = 0

        while (iterator.hasNext()) {
            val element = iterator.next()
            if (element == removedElement) {
                iterator.remove()
            }
            actualCount++
        }

        val resultSet = builder.build()
        for (element in set) {
            if (element != removedElement) {
                assertTrue(element in resultSet)
            } else {
                assertFalse(element in resultSet)
            }
        }

        assertEquals(expectedCount, actualCount)
    }

    @Test
    fun `removing twice on iterators throws IllegalStateException`() {
        val set: PersistentHashSet<Int> =
            persistentHashSetOf(1, 2, 3, 0, 32) as PersistentHashSet<Int>
        val builder = set.builder()
        val iterator = builder.iterator()

        assertFailsWith<IllegalStateException> {
            while (iterator.hasNext()) {
                val element = iterator.next()
                if (element == 0) iterator.remove()
                if (element == 0) {
                    iterator.remove()
                    iterator.remove()
                }
            }
        }
    }

    @Test
    fun `removing elements from different iterators throws ConcurrentModificationException`() {
        val set: PersistentHashSet<Int> =
            persistentHashSetOf(1, 2, 3, 0, 32) as PersistentHashSet<Int>
        val builder = set.builder()
        val iterator1 = builder.iterator()
        val iterator2 = builder.iterator()

        assertFailsWith<ConcurrentModificationException> {
            while (iterator1.hasNext()) {
                val element1 = iterator1.next()
                val _ = iterator2.next()
                if (element1 == 0) iterator1.remove()
                if (element1 == 2) iterator2.remove()
            }
        }
    }

    @Test
    fun `removing element from one iterator and accessing another throws ConcurrentModificationException`() {
        val set = persistentHashSetOf(1, 2, 3)
        val builder = set.builder()
        val iterator1 = builder.iterator()
        val iterator2 = builder.iterator()

        assertFailsWith<ConcurrentModificationException> {
            val _ = iterator1.next()
            iterator1.remove()
            iterator2.next()
        }
    }

    @Test
    fun `retainAll should promote the only remaining element to the root`() {
        val builder = persistentHashSetOf(1, 33).builder()
        builder.retainAll(persistentHashSetOf(1, 65))
        val expected = persistentHashSetOf(1)

        assertTrue(expected.equals(builder))
        assertEquals(expected, builder.build())
        assertEquals(builder.build(), expected)
    }

    @Test
    fun `addAll should not duplicate an element shared with a bottom-level collision node`() {
        val expected = persistentHashSetOf(a1, a2, sibling)

        val builder = persistentHashSetOf(a1, a2).builder()
        assertTrue(builder.addAll(persistentHashSetOf(a1, sibling)))
        assertEquals(3, builder.size)
        assertEquals(expected, builder.build())

        val reversedBuilder = persistentHashSetOf(a1, sibling).builder()
        assertTrue(reversedBuilder.addAll(persistentHashSetOf(a1, a2)))
        assertEquals(3, reversedBuilder.size)
        assertEquals(expected, reversedBuilder.build())
    }

    @Test
    fun `addAll should insert a new element into a bottom-level collision node`() {
        val expected = persistentHashSetOf(a1, a2, a3, sibling)

        val builder = persistentHashSetOf(a1, a2).builder()
        assertTrue(builder.addAll(persistentHashSetOf(a3, sibling)))
        assertEquals(4, builder.size)
        assertEquals(expected, builder.build())

        val reversedBuilder = persistentHashSetOf(a3, sibling).builder()
        assertTrue(reversedBuilder.addAll(persistentHashSetOf(a1, a2)))
        assertEquals(4, reversedBuilder.size)
        assertEquals(expected, reversedBuilder.build())
    }

    @Test
    fun `retainAll should find elements inside a bottom-level collision node`() {
        val expected = persistentHashSetOf(a1)

        val builder = persistentHashSetOf(a1, a2).builder()
        assertTrue(builder.retainAll(persistentHashSetOf(a1, sibling)))
        assertEquals(1, builder.size)
        assertEquals(expected, builder.build())

        val reversedBuilder = persistentHashSetOf(a1, sibling).builder()
        assertTrue(reversedBuilder.retainAll(persistentHashSetOf(a1, a2)))
        assertEquals(1, reversedBuilder.size)
        assertEquals(expected, reversedBuilder.build())
    }

    @Test
    fun `removeAll should remove elements stored in a bottom-level collision node`() {
        val builder = persistentHashSetOf(a1, a2).builder()
        assertTrue(builder.removeAll(persistentHashSetOf(a1, sibling)))
        assertEquals(1, builder.size)
        assertEquals(persistentHashSetOf(a2), builder.build())

        val reversedBuilder = persistentHashSetOf(a1, sibling).builder()
        assertTrue(reversedBuilder.removeAll(persistentHashSetOf(a1, a2)))
        assertEquals(1, reversedBuilder.size)
        assertEquals(persistentHashSetOf(sibling), reversedBuilder.build())
    }

    @Test
    fun `addAll should keep the stored element instance when the collision node is reached at the last level`() {
        val storedElement = IntWrapper(1, 0)
        val builder = persistentHashSetOf(storedElement, sibling).builder()

        builder.addAll(persistentHashSetOf(IntWrapper(1, 0), IntWrapper(2, 0)))

        assertEquals(3, builder.size)
        assertSame(storedElement, builder.single { it == storedElement })
        assertSame(sibling, builder.single { it == sibling })
    }

    @Test
    fun `addAll should keep every stored instance when the receiver's collision node is an equality-subset of the argument's`() {
        val storedElement1 = IntWrapper(1, 0)
        val storedElement2 = IntWrapper(2, 0)
        val builder = persistentHashSetOf(storedElement1, storedElement2).builder()

        builder.addAll(persistentHashSetOf(IntWrapper(1, 0), IntWrapper(2, 0), IntWrapper(3, 0)))

        assertEquals(3, builder.size)
        assertSame(storedElement1, builder.single { it == storedElement1 })
        assertSame(storedElement2, builder.single { it == storedElement2 })
    }

    @Test
    fun `addAll should insert the element when the argument's subtree lacks it`() {
        val storedElement = IntWrapper(1, 0)
        val builder = persistentHashSetOf(storedElement).builder()

        builder.addAll(persistentHashSetOf(IntWrapper(2, 32), IntWrapper(3, 64)))

        assertEquals(3, builder.size)
        assertTrue(builder.contains(IntWrapper(2, 32)))
        assertTrue(builder.contains(IntWrapper(3, 64)))
        assertSame(storedElement, builder.single { it == storedElement })
    }

    @Test
    fun `addAll should push the element deeper when it collides with an argument element inside the subtree`() {
        val storedElement = IntWrapper(1, 0)
        val builder = persistentHashSetOf(storedElement).builder()

        builder.addAll(persistentHashSetOf(IntWrapper(2, 1 shl 10), IntWrapper(3, 32)))

        assertEquals(3, builder.size)
        assertTrue(builder.contains(IntWrapper(2, 1 shl 10)))
        assertTrue(builder.contains(IntWrapper(3, 32)))
        assertSame(storedElement, builder.single { it == storedElement })
    }

    @Test
    fun `addAll should reuse the argument's subtree when it holds the stored element instance`() {
        val argument = persistentHashSetOf(a1, IntWrapper(2, 32)) as PersistentHashSet<IntWrapper>
        val builder = (persistentHashSetOf(a1) as PersistentHashSet<IntWrapper>).builder() as PersistentHashSetBuilder<IntWrapper>

        builder.addAll(argument)

        assertEquals(2, builder.size)
        assertSame(argument.node, builder.node)
    }

    @Test
    fun `addAll should reuse the argument's collision node when its element is already the receiver's instance`() {
        val argument = persistentHashSetOf(a1, a2) as PersistentHashSet<IntWrapper>
        val builder = (persistentHashSetOf(a1) as PersistentHashSet<IntWrapper>).builder() as PersistentHashSetBuilder<IntWrapper>

        builder.addAll(argument)

        assertEquals(2, builder.size)
        assertSame(argument.node, builder.node)
    }

    @Test
    fun `addAll should reuse the argument's collision node when the receiver's elements are already its instances`() {
        val argument = persistentHashSetOf(a1, a2, a3) as PersistentHashSet<IntWrapper>
        val builder = (persistentHashSetOf(a1, a2) as PersistentHashSet<IntWrapper>).builder() as PersistentHashSetBuilder<IntWrapper>

        builder.addAll(argument)

        assertEquals(3, builder.size)
        assertSame(argument.node, builder.node)
    }

    @Test
    fun `addAll should not write the receiver's element into the argument's set`() {
        val storedElement = IntWrapper(1, 0)
        val argumentElement = IntWrapper(1, 0)
        val argument = persistentHashSetOf(argumentElement, IntWrapper(2, 32))
        val builder = persistentHashSetOf(storedElement).builder()

        builder.addAll(argument)

        assertSame(storedElement, builder.single { it == storedElement })
        assertSame(argumentElement, argument.single { it == argumentElement })
    }

    @Test
    fun `addAll should invalidate a live iterator when the argument holds the element in a subtree`() {
        val builder = persistentHashSetOf(a1).builder()

        val iterator = builder.iterator()
        builder.addAll(persistentHashSetOf(IntWrapper(1, 0), IntWrapper(2, 32)))

        assertTrue(builder.contains(a1))
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `addAll of the stored elements should not invalidate an iterator`() {
        val builder = persistentHashSetOf(a1, a2, sibling).builder()

        val iterator = builder.iterator()
        val visited = mutableListOf(iterator.next())
        builder.addAll(persistentHashSetOf(a1, a2, sibling))
        while (iterator.hasNext()) {
            visited.add(iterator.next())
        }

        assertEquals(listOf(a1, a2, sibling), visited.sorted())
    }

    @Test
    fun `addAll that merges an element into the argument's subtree should count one size change`() {
        val overlapping = persistentHashSetOf(IntWrapper(1, 0)).builder() as PersistentHashSetBuilder<IntWrapper>
        val modCount = overlapping.modCount
        overlapping.addAll(persistentHashSetOf(IntWrapper(1, 0), IntWrapper(2, 32)))
        assertEquals(modCount + 1, overlapping.modCount)

        val disjoint = persistentHashSetOf(IntWrapper(1, 0)).builder() as PersistentHashSetBuilder<IntWrapper>
        val disjointModCount = disjoint.modCount
        disjoint.addAll(persistentHashSetOf(IntWrapper(2, 32), IntWrapper(3, 64)))
        assertEquals(disjointModCount + 1, disjoint.modCount)

        val collision =
            persistentHashSetOf(IntWrapper(1, 0), sibling).builder() as PersistentHashSetBuilder<IntWrapper>
        val collisionModCount = collision.modCount
        collision.addAll(persistentHashSetOf(IntWrapper(2, 0), IntWrapper(4, 0)))
        assertEquals(4, collision.size)
        assertEquals(collisionModCount + 1, collision.modCount)
    }

    @Test
    fun `addAll of random colliding sets should keep the stored element instances`() {
        val hashes = intArrayOf(0, 1, 32, 33, 1 shl 10, 1 shl 30, (1 shl 30) or 32)
        val random = Random(316)
        repeat(200) { iteration ->
            val receiverElements = mutableMapOf<Int, IntWrapper>()
            val builder = persistentHashSetOf<IntWrapper>().builder()
            for (id in 0..<14) {
                if (random.nextBoolean()) {
                    val element = IntWrapper(id, hashes[id % hashes.size])
                    receiverElements[id] = element
                    builder.add(element)
                }
            }
            val argumentElements = mutableMapOf<Int, IntWrapper>()
            val argumentBuilder = persistentHashSetOf<IntWrapper>().builder()
            for (id in 0..<14) {
                if (random.nextBoolean()) {
                    val element = IntWrapper(id, hashes[id % hashes.size])
                    argumentElements[id] = element
                    argumentBuilder.add(element)
                }
            }

            builder.addAll(argumentBuilder.build())

            val shape = "iteration $iteration, receiver ${receiverElements.keys}, argument ${argumentElements.keys}"
            assertEquals((receiverElements.keys + argumentElements.keys).size, builder.size, shape)
            for ((id, element) in receiverElements) {
                assertSame(element, builder.singleOrNull { it == element }, "$shape, id $id")
            }
            for ((id, element) in argumentElements) {
                if (id in receiverElements) continue
                assertSame(element, builder.singleOrNull { it == element }, "$shape, id $id")
            }
        }
    }
}
