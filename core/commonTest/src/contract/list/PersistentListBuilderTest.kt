/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.list

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.implementations.immutableList.PersistentVectorBuilder
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import tests.IntWrapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PersistentListBuilderTest {

    // 3: tail only, 40: the root is the single leaf, 100: a root over leaves, 1100: a root over level-1 nodes over leaves
    private fun builders(size: Int): List<Pair<String, PersistentList.Builder<Int>>> {
        val fromList = List(size) { it }.toPersistentList().builder()
        val fromAdds = persistentListOf<Int>().builder().apply { addAll(List(size) { it }) }
        return listOf("from a list" to fromList, "from adds" to fromAdds)
    }

    private fun wrapperBuilders(size: Int): List<Pair<String, PersistentVectorBuilder<IntWrapper>>> {
        val elements = List(size) { IntWrapper(it, it) }
        val fromList = elements.toPersistentList().builder() as PersistentVectorBuilder<IntWrapper>
        val fromAdds = persistentListOf<IntWrapper>().builder().apply { addAll(elements) } as PersistentVectorBuilder<IntWrapper>
        return listOf("from a list" to fromList, "from adds" to fromAdds)
    }

    private fun indices(size: Int) = listOf(0, 31, 32, 1023, 1024, size - 1).filter { it < size }.distinct()

    @Test
    fun `next after a set of the upcoming index returns the new element at every depth whether the builder came from a list or from adds`() {
        for (size in listOf(3, 40, 100, 1100)) {
            for ((flavour, builder) in builders(size)) {
                val iterator = builder.iterator()
                assertEquals(0, iterator.next(), "$flavour at size $size")

                builder[1] = -1
                assertEquals(-1, iterator.next(), "$flavour at size $size")

                val _ = builder.build()
                builder[2] = -2
                assertEquals(-2, iterator.next(), "$flavour at size $size after build")
            }
        }
    }

    @Test
    fun `next after a set that copies a leaf under an owned root returns the new element`() {
        val builder = List(100) { it }.toPersistentList().builder() as PersistentVectorBuilder<Int>
        builder[40] = -40 // copies the root and the leaf of indices 32 to 63, the leaf of indices 0 to 31 stays unowned
        val root = builder.root
        val iterator = builder.iterator()

        builder[0] = -1

        assertSame(root, builder.root)
        assertEquals(-1, iterator.next())
    }

    @Test
    fun `previous after a set of the first element returns the new element from a cursor at the end at every depth`() {
        for (size in listOf(40, 100, 1100)) {
            for ((flavour, builder) in builders(size)) {
                val iterator = builder.listIterator(size)

                builder[0] = -1

                assertEquals((size - 1 downTo 1) + listOf(-1), List(size) { iterator.previous() }, "$flavour at size $size")
            }
        }
    }

    @Test
    fun `previous after the iterator set of the visited index returns the new element`() {
        for ((flavour, builder) in builders(40)) {
            val iterator = builder.listIterator()
            repeat(5) { val _ = iterator.next() }
            assertEquals(5, iterator.next(), flavour)

            iterator.set(-1)

            assertEquals(-1, iterator.previous(), flavour)
        }
    }

    @Test
    fun `next and previous of two iterators in one leaf return the elements set through the other`() {
        for ((flavour, builder) in builders(100)) {
            val first = builder.listIterator(40)
            val second = builder.listIterator(41)
            assertEquals(40, first.next(), flavour)
            assertEquals(41, second.next(), flavour)

            first.set(-1) // copies the leaf of indices 32 to 63 when the builder came from a list
            second.set(-2)

            assertEquals(-2, first.next(), flavour)
            assertEquals(listOf(-2, -1), List(2) { second.previous() }, flavour)
        }
    }

    @Test
    fun `reverse of the builder reverses all the elements whether the builder came from a list or from adds`() {
        for ((flavour, builder) in builders(40)) {
            builder.reverse()

            assertEquals((39 downTo 0).toList(), builder.toList(), flavour)
        }
    }

    @Test
    fun `hasNext after an external add to an exhausted iterator turns true and next throws`() {
        for (size in listOf(3, 32, 40)) {
            for ((flavour, builder) in builders(size)) {
                val iterator = builder.iterator()
                repeat(size) { val _ = iterator.next() }
                assertFalse(iterator.hasNext(), "$flavour at size $size")

                builder.add(size)

                assertTrue(iterator.hasNext(), "$flavour at size $size")
                assertFailsWith<ConcurrentModificationException>("$flavour at size $size") { iterator.next() }
            }
        }
    }

    @Test
    fun `hasNext after an external removeAt that shrinks the builder to the cursor turns false`() {
        for (size in listOf(3, 40)) {
            for ((flavour, builder) in builders(size)) {
                val iterator = builder.iterator()
                repeat(size - 1) { val _ = iterator.next() }

                builder.removeAt(size - 1)

                assertFalse(iterator.hasNext(), "$flavour at size $size")
            }
        }
    }

    @Test
    fun `hasNext after external removals that strand the cursor beyond the size stays true and next throws`() {
        for ((flavour, builder) in builders(3)) {
            val iterator = builder.iterator()
            repeat(3) { val _ = iterator.next() }

            builder.removeAt(2)
            builder.removeAt(1)

            assertTrue(iterator.hasNext(), flavour)
            assertFailsWith<ConcurrentModificationException>(flavour) { iterator.next() }
        }
    }

    @Test
    fun `hasNext on an iterator of an empty builder after an external add turns true and next throws`() {
        val builder = persistentListOf<Int>().builder()
        val iterator = builder.iterator()
        assertFalse(iterator.hasNext())

        builder.add(1)

        assertTrue(iterator.hasNext())
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `hasNext on an exhausted iterator after an external set that copies a leaf stays false`() {
        val builder = List(40) { it }.toPersistentList().builder()
        val iterator = builder.iterator()
        repeat(40) { val _ = iterator.next() }

        builder[0] = -1 // copies the root and the leaf of indices 0 to 31, the size stays the same

        assertFalse(iterator.hasNext())
        assertFailsWith<NoSuchElementException> { iterator.next() }
    }

    @Test
    fun `hasPrevious after an external clear stays true and previous throws`() {
        for ((flavour, builder) in builders(3)) {
            val iterator = builder.listIterator()
            repeat(2) { val _ = iterator.next() }

            builder.clear()

            assertTrue(iterator.hasPrevious(), flavour)
            assertFailsWith<ConcurrentModificationException>(flavour) { iterator.previous() }
        }
    }

    @Test
    fun `an add and a removeAt that cancel out in size still invalidate a live iterator`() {
        for ((flavour, builder) in builders(3)) {
            val iterator = builder.iterator()
            val _ = iterator.next()

            builder.add(-3)
            builder.removeAt(3)

            assertFailsWith<ConcurrentModificationException>(flavour) { iterator.next() }
        }
    }

    @Test
    fun `iterator remove and set without a preceding next after an external add throw IllegalStateException`() {
        for ((flavour, builder) in builders(3)) {
            val iterator = builder.listIterator()

            builder.add(3)

            assertFailsWith<IllegalStateException>(flavour) { iterator.remove() }
            assertFailsWith<IllegalStateException>(flavour) { iterator.set(-1) }
            assertEquals(listOf(0, 1, 2, 3), builder.toList(), flavour)
        }
    }

    @Test
    fun `next after a remove and a set that threw IllegalStateException still throws ConcurrentModificationException`() {
        for ((flavour, builder) in builders(3)) {
            val iterator = builder.listIterator()

            builder.add(3)

            assertFailsWith<IllegalStateException>(flavour) { iterator.remove() }
            assertFailsWith<IllegalStateException>(flavour) { iterator.set(-1) }
            assertFailsWith<ConcurrentModificationException>(flavour) { iterator.next() }
        }
    }

    @Test
    fun `iterator remove and set on an untouched builder throw IllegalStateException`() {
        for ((flavour, builder) in builders(3)) {
            val iterator = builder.listIterator()

            assertFailsWith<IllegalStateException>(flavour) { iterator.remove() }
            assertFailsWith<IllegalStateException>(flavour) { iterator.set(-1) }
            assertEquals(listOf(0, 1, 2), builder.toList(), flavour)
        }
    }

    @Test
    fun `iterator remove after a next and an external add throws ConcurrentModificationException`() {
        for ((flavour, builder) in builders(3)) {
            val iterator = builder.listIterator()
            assertEquals(0, iterator.next(), flavour)

            builder.add(3)

            assertFailsWith<ConcurrentModificationException>(flavour) { iterator.remove() }
            assertEquals(listOf(0, 1, 2, 3), builder.toList(), flavour)
        }
    }

    @Test
    fun `iterator set twice after a next and an external add throws ConcurrentModificationException`() {
        for ((flavour, builder) in builders(3)) {
            val iterator = builder.listIterator()
            assertEquals(0, iterator.next(), flavour)
            iterator.set(-1)

            builder.add(3)

            assertFailsWith<ConcurrentModificationException>(flavour) { iterator.set(-2) }
            assertEquals(listOf(-1, 1, 2, 3), builder.toList(), flavour)
        }
    }

    @Test
    fun `iterator remove and set after the iterator's own remove and add followed by an external add throw IllegalStateException`() {
        for ((flavour, builder) in builders(3)) {
            val iterator = builder.listIterator()
            assertEquals(0, iterator.next(), flavour)
            iterator.remove()

            builder.add(3)

            assertFailsWith<IllegalStateException>(flavour) { iterator.remove() }
            assertEquals(listOf(1, 2, 3), builder.toList(), flavour)
        }

        for ((flavour, builder) in builders(3)) {
            val iterator = builder.listIterator()
            assertEquals(0, iterator.next(), flavour)
            iterator.add(-1)

            builder.add(3)

            assertFailsWith<IllegalStateException>(flavour) { iterator.set(-2) }
            assertEquals(listOf(0, -1, 1, 2, 3), builder.toList(), flavour)
        }
    }

    @Test
    fun `iterator add without a preceding next inserts the element and still reports an external add`() {
        for ((flavour, builder) in builders(3)) {
            val iterator = builder.listIterator()

            iterator.add(-1)

            assertEquals(listOf(-1, 0, 1, 2), builder.toList(), flavour)
        }

        for ((flavour, builder) in builders(3)) {
            val iterator = builder.listIterator()

            builder.add(3)

            assertFailsWith<ConcurrentModificationException>(flavour) { iterator.add(-1) }
            assertEquals(listOf(0, 1, 2, 3), builder.toList(), flavour)
        }
    }

    @Test
    fun `iterator set without a preceding previous from a cursor at the end throws IllegalStateException`() {
        for ((flavour, builder) in builders(3)) {
            val iterator = builder.listIterator(3)

            builder.add(3)

            assertFailsWith<IllegalStateException>(flavour) { iterator.set(-1) }
            assertEquals(listOf(0, 1, 2, 3), builder.toList(), flavour)
        }
    }

    @Test
    fun `iterator remove and set after an external remove past the returned index throw ConcurrentModificationException`() {
        for ((flavour, builder) in builders(3)) {
            val iterator = builder.listIterator(3)
            assertEquals(2, iterator.previous(), flavour)

            builder.removeAt(0)
            builder.removeAt(0)

            assertFailsWith<ConcurrentModificationException>(flavour) { iterator.remove() }
            assertFailsWith<ConcurrentModificationException>(flavour) { iterator.set(-1) }
            assertEquals(listOf(2), builder.toList(), flavour)
        }
    }

    @Test
    fun `next and previous throw ConcurrentModificationException rather than NoSuchElementException when the iteration cannot continue`() {
        for ((flavour, builder) in builders(3)) {
            val iterator = builder.listIterator()
            repeat(2) { val _ = iterator.next() }

            builder.removeAt(2)

            assertFailsWith<ConcurrentModificationException>(flavour) { iterator.next() }
        }

        for ((flavour, builder) in builders(3)) {
            val iterator = builder.listIterator()

            builder.add(3)

            assertFailsWith<ConcurrentModificationException>(flavour) { iterator.previous() }
        }
    }

    @Test
    fun `removeAll of an aligned suffix preserves the retained prefix and allows further additions`() {
        val rows = listOf(100 to 64, 65 to 32, 1100 to 32, 1100 to 1024, 1100 to 1056, 100 to 0)
        for ((size, retainedSize) in rows) {
            val expected = List(retainedSize) { it }
            for ((flavour, builder) in builders(size)) {
                val message = "$flavour at size $size retaining $retainedSize"

                assertTrue(builder.removeAll((retainedSize..<size).toList()), message)

                assertEquals(expected, builder.toList(), message)
                val built = builder.build()
                builder.add(-1)
                assertEquals(expected, built, message)
                assertEquals(expected + (-1), builder.toList(), message)
            }
        }
    }

    @Test
    fun `removeAll whose contains throws removes the elements matched before the throw and keeps the rest at every depth`() {
        val rows = listOf(
            Triple(3, listOf(0), 1),
            Triple(40, listOf(0, 30), 20),
            Triple(100, listOf(0, 33, 70), 50),
            Triple(100, listOf(96), 98),
            Triple(1100, listOf(0, 33, 1000), 50),
            Triple(1100, listOf(0, 33, 1095), 1090),
        )
        for ((size, matching, thrower) in rows) {
            val expected = (0..<size).filter { it >= thrower || it !in matching }
            for ((flavour, builder) in builders(size)) {
                val elements = ThrowingContains(matching, thrower)

                val caught = assertFailsWith<ContainsFailure>("$flavour at size $size throwing at $thrower") {
                    builder.removeAll(elements)
                }

                assertSame(elements.failure, caught, "$flavour at size $size throwing at $thrower")
                assertEquals((0..thrower).toList(), elements.checked, "$flavour at size $size throwing at $thrower")
                assertEquals(expected, builder.toList(), "$flavour at size $size throwing at $thrower")
                val built = builder.build()
                builder.add(-1)
                assertEquals(expected, built, "$flavour at size $size throwing at $thrower")
                assertEquals(expected + (-1), builder.toList(), "$flavour at size $size throwing at $thrower")
            }
        }
    }

    @Test
    fun `removeAll whose contains throws invalidates a live iterator when an element was removed before the throw`() {
        for ((flavour, builder) in builders(3)) {
            val iterator = builder.iterator()

            assertFailsWith<ContainsFailure>(flavour) { builder.removeAll(ThrowingContains(listOf(0), 1)) }

            assertFailsWith<ConcurrentModificationException>(flavour) { iterator.next() }
        }
    }

    @Test
    fun `removeAll whose contains throws keeps a live iterator valid when nothing was removed before the throw`() {
        for ((flavour, builder) in builders(3)) {
            val iterator = builder.iterator()

            assertFailsWith<ContainsFailure>(flavour) { builder.removeAll(ThrowingContains(listOf(2), 1)) }

            assertEquals(listOf(0, 1, 2), builder.toList(), flavour)
            assertEquals(0, iterator.next(), flavour)
        }
    }

    @Test
    fun `removeAll whose contains throws leaves the list built earlier untouched`() {
        val builder = persistentListOf<Int>().builder().apply { addAll(List(100) { it }) }
        val built = builder.build()
        builder[0] = -1

        assertFailsWith<ContainsFailure> { builder.removeAll(ThrowingContains(listOf(-1, 33), 50)) }

        assertEquals(List(100) { it }, built)
        assertEquals((1..32) + (34..99), builder.toList())
    }

    @Test
    fun `set of the element stored at the index keeps the buffers and the built list at every depth whether the builder came from a list or from adds`() {
        for (size in listOf(3, 40, 100, 1100)) {
            for ((flavour, builder) in wrapperBuilders(size)) {
                for (index in indices(size)) {
                    val root = builder.root
                    val tail = builder.tail
                    val trieModCount = builder.trieModCount
                    val element = builder[index]

                    assertSame(element, builder.set(index, element), "$flavour at size $size index $index before the build")

                    assertSame(root, builder.root, "$flavour at size $size index $index before the build")
                    assertSame(tail, builder.tail, "$flavour at size $size index $index before the build")
                    assertEquals(trieModCount, builder.trieModCount, "$flavour at size $size index $index before the build")
                }

                val built = builder.build()
                for (index in indices(size)) {
                    val root = builder.root
                    val tail = builder.tail
                    val trieModCount = builder.trieModCount
                    val element = builder[index]

                    assertSame(element, builder.set(index, element), "$flavour at size $size index $index")

                    assertSame(root, builder.root, "$flavour at size $size index $index")
                    assertSame(tail, builder.tail, "$flavour at size $size index $index")
                    assertEquals(trieModCount, builder.trieModCount, "$flavour at size $size index $index")
                    assertSame(built, builder.build(), "$flavour at size $size index $index")
                }
            }
        }
    }

    @Test
    fun `set of a different element returns the element that was stored at every depth whether the builder came from a list or from adds`() {
        for (size in listOf(3, 40, 100, 1100)) {
            for ((flavour, builder) in wrapperBuilders(size)) {
                for (index in indices(size)) {
                    val stored = builder[index]
                    val element = IntWrapper(-index - 1, -index - 1)

                    assertSame(stored, builder.set(index, element), "$flavour at size $size index $index")
                    assertSame(element, builder[index], "$flavour at size $size index $index")
                }
            }
        }
    }

    @Test
    fun `set of the element stored in an unowned leaf under an owned root copies nothing`() {
        val builder = List(100) { IntWrapper(it, it) }.toPersistentList().builder() as PersistentVectorBuilder<IntWrapper>
        builder[40] = IntWrapper(-40, -40)
        val root = builder.root
        val leaf = root!![0]
        val trieModCount = builder.trieModCount

        builder[0] = builder[0]

        assertSame(root, builder.root)
        assertSame(leaf, builder.root!![0])
        assertEquals(trieModCount, builder.trieModCount)
    }

    @Test
    fun `set of the element stored at the index leaves a live iterator and the built list untouched at every depth`() {
        for (size in listOf(3, 40, 100, 1100)) {
            val list = List(size) { IntWrapper(it, it) }.toPersistentList()
            val builder = list.builder()
            val iterator = builder.iterator()
            assertSame(list[0], iterator.next(), "size $size")

            for (index in indices(size)) {
                builder[index] = builder[index]
            }

            assertSame(list, builder.build(), "size $size")
            assertSame(list[1], iterator.next(), "size $size")
        }
    }

    private class ContainsFailure : Error()

    private class ThrowingContains(private val matching: List<Int>, private val thrower: Int) : Collection<Int> by matching {
        val failure = ContainsFailure()
        val checked = mutableListOf<Int>()

        override fun contains(element: Int): Boolean {
            checked.add(element)
            if (element == thrower) throw failure
            return element in matching
        }
    }
}
