/*
 * Copyright 2016-2025 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.set

import kotlinx.collections.immutable.implementations.immutableSet.LOG_MAX_BRANCHING_FACTOR
import kotlinx.collections.immutable.implementations.immutableSet.MAX_SHIFT
import kotlinx.collections.immutable.implementations.immutableSet.PersistentHashSet
import kotlinx.collections.immutable.implementations.immutableSet.PersistentHashSetBuilder
import kotlinx.collections.immutable.implementations.immutableSet.TrieNode
import kotlinx.collections.immutable.persistentHashSetOf
import tests.IntWrapper
import tests.trie.*
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PersistentHashSetBuilderTest {

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
        val expected = persistentHashSetOf(collidingKey1, collidingKey2, sibling)

        val builder = persistentHashSetOf(collidingKey1, collidingKey2).builder()
        assertTrue(builder.addAll(persistentHashSetOf(collidingKey1, sibling)))
        assertEquals(3, builder.size)
        assertEquals(expected, builder.build())

        val reversedBuilder = persistentHashSetOf(collidingKey1, sibling).builder()
        assertTrue(reversedBuilder.addAll(persistentHashSetOf(collidingKey1, collidingKey2)))
        assertEquals(3, reversedBuilder.size)
        assertEquals(expected, reversedBuilder.build())
    }

    @Test
    fun `addAll should insert a new element into a bottom-level collision node`() {
        val expected = persistentHashSetOf(collidingKey1, collidingKey2, collidingKey3, sibling)

        val builder = persistentHashSetOf(collidingKey1, collidingKey2).builder()
        assertTrue(builder.addAll(persistentHashSetOf(collidingKey3, sibling)))
        assertEquals(4, builder.size)
        assertEquals(expected, builder.build())

        val reversedBuilder = persistentHashSetOf(collidingKey3, sibling).builder()
        assertTrue(reversedBuilder.addAll(persistentHashSetOf(collidingKey1, collidingKey2)))
        assertEquals(4, reversedBuilder.size)
        assertEquals(expected, reversedBuilder.build())
    }

    @Test
    fun `retainAll should find elements inside a bottom-level collision node`() {
        val expected = persistentHashSetOf(collidingKey1)

        val builder = persistentHashSetOf(collidingKey1, collidingKey2).builder()
        assertTrue(builder.retainAll(persistentHashSetOf(collidingKey1, sibling)))
        assertEquals(1, builder.size)
        assertEquals(expected, builder.build())

        val reversedBuilder = persistentHashSetOf(collidingKey1, sibling).builder()
        assertTrue(reversedBuilder.retainAll(persistentHashSetOf(collidingKey1, collidingKey2)))
        assertEquals(1, reversedBuilder.size)
        assertEquals(expected, reversedBuilder.build())
    }

    @Test
    fun `removeAll should remove elements stored in a bottom-level collision node`() {
        val builder = persistentHashSetOf(collidingKey1, collidingKey2).builder()
        assertTrue(builder.removeAll(persistentHashSetOf(collidingKey1, sibling)))
        assertEquals(1, builder.size)
        assertEquals(persistentHashSetOf(collidingKey2), builder.build())

        val reversedBuilder = persistentHashSetOf(collidingKey1, sibling).builder()
        assertTrue(reversedBuilder.removeAll(persistentHashSetOf(collidingKey1, collidingKey2)))
        assertEquals(1, reversedBuilder.size)
        assertEquals(persistentHashSetOf(sibling), reversedBuilder.build())
    }

    @Test
    fun `addAll should keep the stored element instance when the collision node is reached at the last level`() {
        val builder = persistentHashSetOf(collidingKey1, sibling).builder()

        builder.addAll(persistentHashSetOf(collidingKey1.copy(), collidingKey2))

        assertEquals(3, builder.size)
        assertSame(collidingKey1, builder.single { it == collidingKey1 })
        assertSame(sibling, builder.single { it == sibling })
    }

    @Test
    fun `addAll should keep every stored instance when the receiver's collision node is an equality-subset of the argument's`() {
        val builder = persistentHashSetOf(collidingKey1, collidingKey2).builder()

        builder.addAll(persistentHashSetOf(collidingKey1.copy(), collidingKey2.copy(), collidingKey3))

        assertEquals(3, builder.size)
        assertSame(collidingKey1, builder.single { it == collidingKey1 })
        assertSame(collidingKey2, builder.single { it == collidingKey2 })
    }

    @Test
    fun `addAll should insert the element when the argument's subtree lacks it`() {
        val builder = persistentHashSetOf(collidingKey1).builder()

        builder.addAll(persistentHashSetOf(levelOneSibling, otherLevelOneSibling))

        assertEquals(3, builder.size)
        assertTrue(builder.contains(levelOneSibling))
        assertTrue(builder.contains(otherLevelOneSibling))
        assertSame(collidingKey1, builder.single { it == collidingKey1 })
    }

    @Test
    fun `addAll should push the element deeper when it collides with an argument element inside the subtree`() {
        val builder = persistentHashSetOf(collidingKey1).builder()

        builder.addAll(persistentHashSetOf(levelTwoSibling, levelOneSibling))

        assertEquals(3, builder.size)
        assertTrue(builder.contains(levelTwoSibling))
        assertTrue(builder.contains(levelOneSibling))
        assertSame(collidingKey1, builder.single { it == collidingKey1 })
    }

    @Test
    fun `addAll should reuse the argument's subtree when it holds the stored element instance`() {
        val argument = persistentHashSetOf(collidingKey1, levelOneSibling) as PersistentHashSet<IntWrapper>
        val builder = (persistentHashSetOf(collidingKey1) as PersistentHashSet<IntWrapper>).builder() as PersistentHashSetBuilder<IntWrapper>

        builder.addAll(argument)

        assertEquals(2, builder.size)
        assertSame(argument.node, builder.node)
    }

    @Test
    fun `addAll should reuse the argument's collision node when its element is already the receiver's instance`() {
        val argument = persistentHashSetOf(collidingKey1, collidingKey2) as PersistentHashSet<IntWrapper>
        val builder = (persistentHashSetOf(collidingKey1) as PersistentHashSet<IntWrapper>).builder() as PersistentHashSetBuilder<IntWrapper>

        builder.addAll(argument)

        assertEquals(2, builder.size)
        assertSame(argument.node, builder.node)
    }

    @Test
    fun `addAll should reuse the argument's collision node when the receiver's elements are already its instances`() {
        val argument = persistentHashSetOf(collidingKey1, collidingKey2, collidingKey3) as PersistentHashSet<IntWrapper>
        val builder = (persistentHashSetOf(collidingKey1, collidingKey2) as PersistentHashSet<IntWrapper>).builder() as PersistentHashSetBuilder<IntWrapper>

        builder.addAll(argument)

        assertEquals(3, builder.size)
        assertSame(argument.node, builder.node)
    }

    @Test
    fun `addAll should not write the receiver's element into the argument's set`() {
        val argumentElement = collidingKey1.copy()
        val argument = persistentHashSetOf(argumentElement, levelOneSibling)
        val builder = persistentHashSetOf(collidingKey1).builder()

        builder.addAll(argument)

        assertSame(collidingKey1, builder.single { it == collidingKey1 })
        assertSame(argumentElement, argument.single { it == argumentElement })
    }

    @Test
    fun `addAll should invalidate a live iterator when the argument holds the element in a subtree`() {
        val builder = persistentHashSetOf(collidingKey1).builder()

        val iterator = builder.iterator()
        builder.addAll(persistentHashSetOf(collidingKey1.copy(), levelOneSibling))

        assertTrue(builder.contains(collidingKey1))
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `addAll of the stored elements should not invalidate an iterator`() {
        val builder = persistentHashSetOf(collidingKey1, collidingKey2, sibling).builder()

        val iterator = builder.iterator()
        val visited = mutableListOf(iterator.next())
        builder.addAll(persistentHashSetOf(collidingKey1, collidingKey2, sibling))
        while (iterator.hasNext()) {
            visited.add(iterator.next())
        }

        assertEquals(listOf(collidingKey1, collidingKey2, sibling), visited.sorted())
    }

    @Test
    fun `addAll that merges an element into the argument's subtree should count one size change`() {
        val overlapping = persistentHashSetOf(collidingKey1).builder() as PersistentHashSetBuilder<IntWrapper>
        val modCount = overlapping.modCount
        overlapping.addAll(persistentHashSetOf(collidingKey1.copy(), levelOneSibling))
        assertEquals(modCount + 1, overlapping.modCount)

        val disjoint = persistentHashSetOf(collidingKey1).builder() as PersistentHashSetBuilder<IntWrapper>
        val disjointModCount = disjoint.modCount
        disjoint.addAll(persistentHashSetOf(levelOneSibling, otherLevelOneSibling))
        assertEquals(disjointModCount + 1, disjoint.modCount)

        val collision =
            persistentHashSetOf(collidingKey1, sibling).builder() as PersistentHashSetBuilder<IntWrapper>
        val collisionModCount = collision.modCount
        collision.addAll(persistentHashSetOf(collidingKey2, collidingKey3))
        assertEquals(4, collision.size)
        assertEquals(collisionModCount + 1, collision.modCount)
    }

    @Test
    fun `addAll of random colliding sets should keep the stored element instances`() {
        val hashes = intArrayOf(
            0, 1,
            1 shl LOG_MAX_BRANCHING_FACTOR, (1 shl LOG_MAX_BRANCHING_FACTOR) or 1,
            1 shl (2 * LOG_MAX_BRANCHING_FACTOR),
            1 shl MAX_SHIFT, (1 shl MAX_SHIFT) or (1 shl LOG_MAX_BRANCHING_FACTOR)
        )
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

    @Test
    fun `retainAll should keep the stored element instance when the collision node is reached at the last level`() {
        val builder = persistentHashSetOf(collidingKey1, collidingKey2, sibling).builder()

        assertTrue(builder.retainAll(persistentHashSetOf(collidingKey1.copy(), sibling.copy())))

        assertEquals(2, builder.size)
        assertSame(collidingKey1, builder.single { it == collidingKey1 })
        assertSame(sibling, builder.single { it == sibling })
    }

    @Test
    fun `retainAll should drop the argument's element when the receiver's subtree does not hold it`() {

        val leafMiss = persistentHashSetOf(collidingKey1, levelOneSibling, rootSibling).builder()
        assertTrue(leafMiss.retainAll(persistentHashSetOf(collidingKey3, rootSibling.copy())))
        assertEquals(1, leafMiss.size)
        assertSame(rootSibling, leafMiss.single())

        val absentCell = persistentHashSetOf(collidingKey1, levelOneSibling, rootSibling).builder()
        assertTrue(absentCell.retainAll(persistentHashSetOf(otherLevelOneSibling, rootSibling.copy())))
        assertEquals(1, absentCell.size)
        assertSame(rootSibling, absentCell.single())

        val collisionMiss = persistentHashSetOf(collidingKey1, collidingKey2, sibling).builder()
        assertTrue(collisionMiss.retainAll(persistentHashSetOf(collidingKey3, sibling)))
        assertEquals(1, collisionMiss.size)
        assertSame(sibling, collisionMiss.single())
    }

    @Test
    fun `retainAll should keep every stored instance when compacting a collision node the builder owns`() {
        val builder = persistentHashSetOf<IntWrapper>().builder()
        builder.add(collidingKey1)
        builder.add(collidingKey2)
        builder.add(collidingKey3)

        assertTrue(builder.retainAll(persistentHashSetOf(collidingKey1.copy(), collidingKey2.copy())))

        assertEquals(2, builder.size)
        assertSame(collidingKey1, builder.single { it == collidingKey1 })
        assertSame(collidingKey2, builder.single { it == collidingKey2 })
    }

    @Test
    fun `retainAll should reuse the argument's node when it already holds the receiver's element instance`() {
        val argument = persistentHashSetOf(collidingKey1) as PersistentHashSet<IntWrapper>
        val builder = (persistentHashSetOf(collidingKey1, levelOneSibling) as PersistentHashSet<IntWrapper>)
                .builder() as PersistentHashSetBuilder<IntWrapper>

        builder.retainAll(argument)

        assertEquals(1, builder.size)
        assertSame(argument.node, builder.node)
    }

    @Test
    fun `retainAll should reuse the argument's collision node when its elements are the receiver's instances`() {
        val argument = persistentHashSetOf(collidingKey1, collidingKey2) as PersistentHashSet<IntWrapper>
        val builder = (persistentHashSetOf(collidingKey1, collidingKey2, collidingKey3) as PersistentHashSet<IntWrapper>)
                .builder() as PersistentHashSetBuilder<IntWrapper>

        builder.retainAll(argument)

        assertEquals(2, builder.size)
        assertSame(collisionNodeOf(argument.node), collisionNodeOf(builder.node))
    }

    @Suppress("UNCHECKED_CAST")
    private fun collisionNodeOf(node: TrieNode<IntWrapper>): TrieNode<IntWrapper> {
        var current = node
        while (current.bitmap != 0) {
            current = current.buffer[0] as TrieNode<IntWrapper>
        }
        return current
    }

    @Test
    fun `retainAll of random colliding sets should keep the stored element instances`() {
        val hashes = intArrayOf(
            0, 1,
            1 shl LOG_MAX_BRANCHING_FACTOR, (1 shl LOG_MAX_BRANCHING_FACTOR) or 1,
            1 shl (2 * LOG_MAX_BRANCHING_FACTOR),
            1 shl MAX_SHIFT, (1 shl MAX_SHIFT) or (1 shl LOG_MAX_BRANCHING_FACTOR)
        )
        val random = Random(322)
        repeat(200) { iteration ->
            val receiverElements = mutableMapOf<Int, IntWrapper>()
            val builder = persistentHashSetOf<IntWrapper>().builder()
            for (id in 0..<21) {
                if (random.nextBoolean()) {
                    val element = IntWrapper(id, hashes[id % hashes.size])
                    receiverElements[id] = element
                    builder.add(element)
                }
            }
            val argumentElements = mutableMapOf<Int, IntWrapper>()
            val argumentBuilder = persistentHashSetOf<IntWrapper>().builder()
            for (id in 0..<21) {
                if (random.nextBoolean()) {
                    val element = IntWrapper(id, hashes[id % hashes.size])
                    argumentElements[id] = element
                    argumentBuilder.add(element)
                }
            }

            builder.retainAll(argumentBuilder.build())

            val shape = "iteration $iteration, receiver ${receiverElements.keys}, argument ${argumentElements.keys}"
            val retainedIds = receiverElements.keys intersect argumentElements.keys
            assertEquals(retainedIds.size, builder.size, shape)
            for (id in retainedIds) {
                val element = receiverElements.getValue(id)
                assertSame(element, builder.singleOrNull { it == element }, "$shape, id $id")
            }
        }
    }
}
