/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.list

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.implementations.immutableList.MAX_BUFFER_SIZE
import kotlinx.collections.immutable.implementations.immutableList.PersistentVectorBuilder
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import tests.IntWrapper
import tests.contract.BuilderOperation
import tests.contract.IteratorOperation
import tests.contract.afterAMissingNext
import tests.contract.iteratorOperations
import tests.contract.testIterationContinues
import tests.contract.testIterator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** The list sizes at which the trie changes shape. */
internal const val TAIL_ONLY = 3
internal const val FULL_TAIL = MAX_BUFFER_SIZE
internal const val ONE_LEAF = MAX_BUFFER_SIZE + 8
internal const val LEAVES = 3 * MAX_BUFFER_SIZE + 4
internal const val NODES = MAX_BUFFER_SIZE * MAX_BUFFER_SIZE + 76

private val depths = mapOf(
    TAIL_ONLY to "the tail only",
    FULL_TAIL to "a full tail",
    ONE_LEAF to "a root that is one leaf",
    LEAVES to "a root over leaves",
    NODES to "a root over level-1 nodes",
)

/** The indices at the leaf and level boundaries of a list of [size]. */
internal fun boundaryIndices(size: Int): List<Int> =
    listOf(0, MAX_BUFFER_SIZE - 1, MAX_BUFFER_SIZE, MAX_BUFFER_SIZE * MAX_BUFFER_SIZE - 1, MAX_BUFFER_SIZE * MAX_BUFFER_SIZE, size - 1)
        .filter { it < size }
        .distinct()

/** Builders of [size] built from a list, which shares its leaves, and from adds, which own theirs. */
private fun builders(size: Int): List<Pair<String, PersistentList.Builder<Int>>> = listOf(
    "from a list" to List(size) { it }.toPersistentList().builder(),
    "from adds" to persistentListOf<Int>().builder().apply { addAll(List(size) { it }) },
)

@Suppress("UNCHECKED_CAST")
private fun wrapperBuilders(size: Int): List<Pair<String, PersistentVectorBuilder<IntWrapper>>> {
    val elements = List(size) { IntWrapper(it, it) }
    return listOf(
        "from a list" to elements.toPersistentList().builder() as PersistentVectorBuilder<IntWrapper>,
        "from adds" to persistentListOf<IntWrapper>().builder().apply { addAll(elements) } as PersistentVectorBuilder<IntWrapper>,
    )
}

/** Runs [test] on both builders of every size in [sizes], every depth by default; the message names the builder and the size. */
private fun forEachBuilder(sizes: Collection<Int> = depths.keys, test: (message: String, size: Int, builder: PersistentList.Builder<Int>) -> Unit) {
    for (size in sizes) {
        for ((origin, builder) in builders(size)) test("$origin at size $size, ${depths[size] ?: "a root over leaves"}", size, builder)
    }
}

private fun forEachWrapperBuilder(test: (message: String, size: Int, builder: PersistentVectorBuilder<IntWrapper>) -> Unit) {
    for ((size, depth) in depths) {
        for ((origin, builder) in wrapperBuilders(size)) test("$origin at size $size, $depth", size, builder)
    }
}

/** Builders the iterator sweep runs on: the tail alone and a leaf with a tail, from a list and from adds. */
private val listBuilders: List<Pair<String, () -> PersistentList.Builder<Any>>> = listOf(TAIL_ONLY, FULL_TAIL + 1).flatMap { size ->
    val elements = List<Any>(size) { it }
    listOf(
        "from a list of $size" to { elements.toPersistentList().builder() },
        "from adds of $size" to { persistentListOf<Any>().builder().apply { addAll(elements) } },
    )
}

/** The MutableListIterator surface, as stdlib's listIteratorOperations. */
private val listIteratorOperations: List<IteratorOperation<MutableListIterator<Any>>> = listOf(
    IteratorOperation("next()") { next() },
    IteratorOperation("previous()") { previous() },
    IteratorOperation("add()") { add("new") },
    IteratorOperation("add() then set()") { add("new"); set("new") },
    IteratorOperation("add() then remove()") { add("new"); remove() },
    IteratorOperation("set()", afterAMissingNext) { set("new") },
    IteratorOperation("set() twice", afterAMissingNext) { set("new"); set("newer") },
    IteratorOperation("remove()", afterAMissingNext) { remove() },
    IteratorOperation("remove() then set()", afterAMissingNext) { remove(); set("new") },
)

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

class PersistentListBuilderTest {

    @Test
    fun iterators() {
        val views = listOf<Pair<String, PersistentList.Builder<Any>.() -> MutableIterator<*>>>(
            "iterator" to { iterator() },
            "listIterator" to { listIterator() },
        )
        val listViews = listOf<Pair<String, PersistentList.Builder<Any>.() -> MutableListIterator<Any>>>(
            "listIterator" to { listIterator() },
        )
        val operations = listOf<BuilderOperation<PersistentList.Builder<Any>>>(
            BuilderOperation("no call", throwsCME = false) { },

            BuilderOperation("add()") { add("new") },
            BuilderOperation("add(0)") { add(0, "new") },
            BuilderOperation("removeAt(0)") { removeAt(0) },
            BuilderOperation("removeAt(lastIndex)") { removeAt(lastIndex) },
            BuilderOperation("removeAt(lastIndex) twice") { removeAt(lastIndex); removeAt(lastIndex) },
            BuilderOperation("clear()") { clear() },
            BuilderOperation("addAll()") { addAll(listOf("new", "newer")) },
            BuilderOperation("removeAll(the first element)") { removeAll(listOf(first())) },
            BuilderOperation("retainAll(the first element)") { retainAll(listOf(first())) },
            BuilderOperation("add() then removeAt(lastIndex)") { add("new"); removeAt(lastIndex) },

            BuilderOperation("set(1)", throwsCME = false) { this[1] = "changed" },
            BuilderOperation("set(lastIndex)", throwsCME = false) { this[lastIndex] = "changed" },
            BuilderOperation("set(the stored element)", throwsCME = false) { this[1] = this[1] },
            BuilderOperation("addAll(an empty list)", throwsCME = false) { addAll(emptyList()) },
            BuilderOperation("removeAll(missing elements)", throwsCME = false) { removeAll(listOf("missing")) },
            BuilderOperation("retainAll(a superset)", throwsCME = false) { retainAll(toList() + "missing") },
            BuilderOperation("build()", throwsCME = false) { val _ = build() },
        )

        for (operation in operations) {
            for (iteratorOp in iteratorOperations) testIterator(listBuilders, views, operation, iteratorOp, hasNextFollowsTheSize = true)
            for (iteratorOp in listIteratorOperations) testIterator(listBuilders, listViews, operation, iteratorOp, hasNextFollowsTheSize = true)
            if (!operation.throwsCME) testIterationContinues(listBuilders, views, operation)
        }
    }

    @Test
    fun `set and remove from a cursor at the end without a preceding move throw ISE`() = forEachBuilder(listOf(TAIL_ONLY)) { message, size, builder ->
        val iterator = builder.listIterator(size)

        builder.add(size)

        assertFailsWith<IllegalStateException>(message) { iterator.set(-1) }
        assertFailsWith<IllegalStateException>(message) { iterator.remove() }
        assertTrue(iterator.hasPrevious(), message)
        assertFailsWith<ConcurrentModificationException>(message) { iterator.previous() }
    }

    /** The sweep makes the iterator's own moves after the builder's call; this one makes them before it. */
    @Test
    fun `set and remove after the iterator's own remove and add and an external add throw ISE`() = forEachBuilder(listOf(TAIL_ONLY)) { message, _, builder ->
        val iterator = builder.listIterator()
        val _ = iterator.next()
        iterator.remove()
        iterator.add(-1)

        builder.add(9)

        assertFailsWith<IllegalStateException>(message) { iterator.set(-2) }
        assertFailsWith<IllegalStateException>(message) { iterator.remove() }
        assertFailsWith<ConcurrentModificationException>(message) { iterator.next() }
    }

    @Test
    fun `add to the iterator of an empty builder turns hasNext true and next throws`() {
        val builder = persistentListOf<Int>().builder()
        val iterator = builder.iterator()
        assertFalse(iterator.hasNext())

        builder.add(1)

        assertTrue(iterator.hasNext())
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }

    @Test
    fun `hasPrevious after a clear stays true and previous throws`() = forEachBuilder(listOf(TAIL_ONLY)) { message, _, builder ->
        val iterator = builder.listIterator()
        repeat(2) { val _ = iterator.next() }

        builder.clear()

        assertTrue(iterator.hasPrevious(), message)
        assertFailsWith<ConcurrentModificationException>(message) { iterator.previous() }
    }

    @Test
    fun `reverse reverses all the elements`() = forEachBuilder { message, size, builder ->
        builder.reverse()

        assertEquals((size - 1 downTo 0).toList(), builder.toList(), message)
    }

    @Test
    fun `next after a set of the upcoming index`() = forEachBuilder { message, _, builder ->
        val iterator = builder.iterator()
        assertEquals(0, iterator.next(), message)

        builder[1] = -1
        assertEquals(-1, iterator.next(), message)

        val _ = builder.build()
        builder[2] = -2
        assertEquals(-2, iterator.next(), "$message after build")
    }

    @Test
    fun `next after a set that copies a leaf under an owned root`() {
        @Suppress("UNCHECKED_CAST")
        val builder = List(LEAVES) { it }.toPersistentList().builder() as PersistentVectorBuilder<Int>
        builder[40] = -40 // copies the root and the leaf of indices 32 to 63, the leaf of indices 0 to 31 stays unowned
        val root = builder.root
        val iterator = builder.iterator()

        builder[0] = -1

        assertSame(root, builder.root)
        assertEquals(-1, iterator.next())
    }

    @Test
    fun `previous after a set of the first element from a cursor at the end`() = forEachBuilder { message, size, builder ->
        val iterator = builder.listIterator(size)

        builder[0] = -1

        assertEquals((size - 1 downTo 1) + listOf(-1), List(size) { iterator.previous() }, message)
    }

    @Test
    fun `previous after the iterator's set`() = forEachBuilder(listOf(ONE_LEAF)) { message, _, builder ->
        val iterator = builder.listIterator()
        repeat(5) { val _ = iterator.next() }
        assertEquals(5, iterator.next(), message)

        iterator.set(-1)

        assertEquals(-1, iterator.previous(), message)
    }

    @Test
    fun `set through two iterators in one leaf`() = forEachBuilder(listOf(LEAVES)) { message, _, builder ->
        val first = builder.listIterator(40)
        val second = builder.listIterator(41)
        assertEquals(40, first.next(), message)
        assertEquals(41, second.next(), message)

        first.set(-1) // copies the leaf of indices 32 to 63 when the builder came from a list
        second.set(-2)

        assertEquals(-2, first.next(), message)
        assertEquals(listOf(-2, -1), List(2) { second.previous() }, message)
    }

    @Test
    fun `add without a preceding next inserts the element`() = forEachBuilder(listOf(TAIL_ONLY)) { message, _, builder ->
        val iterator = builder.listIterator()

        iterator.add(-1)

        assertEquals(listOf(-1, 0, 1, 2), builder.toList(), message)
    }

    @Test
    fun `removeAll of an aligned suffix keeps the retained prefix and allows further additions`() {
        // The removed suffix starts at a leaf boundary: the retained size is a multiple of MAX_BUFFER_SIZE (32).
        val rows = listOf(100 to 64, 65 to 32, 1100 to 32, 1100 to 1024, 1100 to 1056, 100 to 0)
        for ((size, retainedSize) in rows) {
            val expected = List(retainedSize) { it }
            forEachBuilder(listOf(size)) { message, _, builder ->
                val row = "$message retaining $retainedSize"

                assertTrue(builder.removeAll((retainedSize..<size).toList()), row)

                assertEquals(expected, builder.toList(), row)
                val built = builder.build()
                builder.add(-1)
                assertEquals(expected, built, row)
                assertEquals(expected + (-1), builder.toList(), row)
            }
        }
    }

    @Test
    fun `removeAll of a middle leaf and the tail keeps the last retained leaf in the tail`() = forEachBuilder(listOf(LEAVES)) { message, _, builder ->
        val expected = (0..<32) + (64..<96)

        assertTrue(builder.removeAll((32..<64) + (96..<100)), message)

        assertEquals(expected, builder.toList(), message)
        val built = builder.build()
        builder.add(-1)
        assertEquals(expected, built, message)
        assertEquals(expected + (-1), builder.toList(), message)
    }

    @Test
    fun `removeAll of an argument whose contains throws removes the elements matched before the throw`() {
        val rows = listOf(
            Triple(TAIL_ONLY, listOf(0), 1),
            Triple(ONE_LEAF, listOf(0, 30), 20),
            Triple(LEAVES, listOf(0, 33, 70), 50),
            Triple(LEAVES, listOf(96), 98),
            Triple(NODES, listOf(0, 33, 1000), 50),
            Triple(NODES, listOf(0, 33, 1095), 1090),
        )
        for ((size, matching, thrower) in rows) {
            val expected = (0..<size).filter { it >= thrower || it !in matching }
            forEachBuilder(listOf(size)) { message, _, builder ->
                val row = "$message throwing at $thrower"
                val elements = ThrowingContains(matching, thrower)

                val caught = assertFailsWith<ContainsFailure>(row) { builder.removeAll(elements) }

                assertSame(elements.failure, caught, row)
                assertEquals((0..thrower).toList(), elements.checked, row)
                assertEquals(expected, builder.toList(), row)
                val built = builder.build()
                builder.add(-1)
                assertEquals(expected, built, row)
                assertEquals(expected + (-1), builder.toList(), row)
            }
        }
    }

    @Test
    fun `removeAll of an argument whose contains throws after a removal invalidates a live iterator`() = forEachBuilder(listOf(TAIL_ONLY)) { message, _, builder ->
        val iterator = builder.iterator()

        assertFailsWith<ContainsFailure>(message) { builder.removeAll(ThrowingContains(listOf(0), 1)) }

        assertFailsWith<ConcurrentModificationException>(message) { iterator.next() }
    }

    @Test
    fun `removeAll of an argument whose contains throws before any removal keeps a live iterator valid`() = forEachBuilder(listOf(TAIL_ONLY)) { message, _, builder ->
        val iterator = builder.iterator()

        assertFailsWith<ContainsFailure>(message) { builder.removeAll(ThrowingContains(listOf(2), 1)) }

        assertEquals(listOf(0, 1, 2), builder.toList(), message)
        assertEquals(0, iterator.next(), message)
    }

    @Test
    fun `removeAll of an argument whose contains throws leaves the list built earlier untouched`() {
        val builder = persistentListOf<Int>().builder().apply { addAll(List(LEAVES) { it }) }
        val built = builder.build()
        builder[0] = -1

        assertFailsWith<ContainsFailure> { builder.removeAll(ThrowingContains(listOf(-1, 33), 50)) }

        assertEquals(List(LEAVES) { it }, built)
        assertEquals((1..32) + (34..99), builder.toList())
    }

    @Test
    fun `set of the same element keeps the buffers and the built list`() = forEachWrapperBuilder { message, size, builder ->
        for (index in boundaryIndices(size)) {
            val root = builder.root
            val tail = builder.tail
            val trieModCount = builder.trieModCount
            val element = builder[index]

            assertSame(element, builder.set(index, element), "$message, index $index before the build")

            assertSame(root, builder.root, "$message, index $index before the build")
            assertSame(tail, builder.tail, "$message, index $index before the build")
            assertEquals(trieModCount, builder.trieModCount, "$message, index $index before the build")
        }

        val built = builder.build()
        for (index in boundaryIndices(size)) {
            val root = builder.root
            val tail = builder.tail
            val trieModCount = builder.trieModCount
            val element = builder[index]

            assertSame(element, builder.set(index, element), "$message, index $index")

            assertSame(root, builder.root, "$message, index $index")
            assertSame(tail, builder.tail, "$message, index $index")
            assertEquals(trieModCount, builder.trieModCount, "$message, index $index")
            assertSame(built, builder.build(), "$message, index $index")
        }
    }

    @Test
    fun `set of a different element returns the stored one`() = forEachWrapperBuilder { message, size, builder ->
        for (index in boundaryIndices(size)) {
            val stored = builder[index]
            val element = IntWrapper(-index - 1, -index - 1)

            assertSame(stored, builder.set(index, element), "$message, index $index")
            assertSame(element, builder[index], "$message, index $index")
        }
    }

    @Test
    fun `set of the same element in an unowned leaf under an owned root copies nothing`() {
        @Suppress("UNCHECKED_CAST")
        val builder = List(LEAVES) { IntWrapper(it, it) }.toPersistentList().builder() as PersistentVectorBuilder<IntWrapper>
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
    fun `set of the same element keeps a live iterator and the built list`() {
        for ((size, depth) in depths) {
            val list = List(size) { IntWrapper(it, it) }.toPersistentList()
            val builder = list.builder()
            val iterator = builder.iterator()
            assertSame(list[0], iterator.next(), depth)

            for (index in boundaryIndices(size)) {
                builder[index] = builder[index]
            }

            assertSame(list, builder.build(), depth)
            assertSame(list[1], iterator.next(), depth)
        }
    }
}
