/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.set

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import tests.IntWrapper
import tests.contract.BuilderOperation
import tests.contract.iteratorOperations
import tests.contract.testIterationContinues
import tests.contract.testIterator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Builders of three elements built from a set, which shares its trie, and from adds, which own theirs. */
private val orderedSetBuilders: List<Pair<String, () -> PersistentSet.Builder<Any>>> = listOf(
    "from a set" to { persistentSetOf<Any>(1, 2, 3).builder() },
    "from adds" to { persistentSetOf<Any>().builder().apply { addAll(listOf(1, 2, 3)) } },
)

/*
 * The keys of the cache test, a recorded reproduction: the hashes are part of the scenario. By the last digit of
 * the value, 0 to 2 share a collision node, 3 to 5 fan out at level 1 under root cell 13, 6 and 7 at level 2 under
 * cell 7 of that node, and the rest are spread over the root by a mixing hash.
 */
private fun key(value: Int): IntWrapper = IntWrapper(
    value,
    when (value.mod(10)) {
        0, 1, 2 -> 0
        3, 4, 5 -> 13 or ((value and 31) shl 5)
        6, 7 -> 13 or (7 shl 5) or ((value and 31) shl 10)
        else -> (value * 0x9E3779B9.toInt()).rotateLeft(7)
    },
)

private fun keys(vararg values: Int): List<IntWrapper> = values.map(::key)

class PersistentOrderedSetBuilderTest {

    @Test
    fun iterators() {
        val views = listOf<Pair<String, PersistentSet.Builder<Any>.() -> MutableIterator<*>>>("iterator" to { iterator() })
        val operations = listOf<BuilderOperation<PersistentSet.Builder<Any>>>(
            BuilderOperation("no call", throwsCME = false) { },

            BuilderOperation("add(a new element)") { add("new") },
            BuilderOperation("remove(the first element)") { remove(first()) },
            BuilderOperation("remove(the last element)") { remove(last()) },
            BuilderOperation("clear()") { clear() },
            BuilderOperation("addAll(a list with a new element)") { addAll(listOf(first(), "new")) },
            BuilderOperation("removeAll(a list holding the first element)") { removeAll(listOf(first())) },
            BuilderOperation("retainAll(a list holding only the first element)") { retainAll(listOf(first())) },
            BuilderOperation("add(a new element) then remove(it)") { add("new"); remove("new") },

            BuilderOperation("add(a stored element)", throwsCME = false) { add(first()) },
            BuilderOperation("remove(a missing element)", throwsCME = false) { remove("missing") },
            BuilderOperation("addAll(the stored elements)", throwsCME = false) { addAll(toList()) },
            BuilderOperation("removeAll(missing elements)", throwsCME = false) { removeAll(listOf("missing")) },
            BuilderOperation("retainAll(a superset)", throwsCME = false) { retainAll(toList() + "missing") },
        )

        for (operation in operations) {
            for (iteratorOp in iteratorOperations) testIterator(orderedSetBuilders, views, operation, iteratorOp)
            if (!operation.throwsCME) testIterationContinues(orderedSetBuilders, views, operation)
        }
    }

    @Test
    fun `builder cache remains consistent after repeated removals and rebuilds`() {
        var persistent = persistentSetOf<IntWrapper>()
        var builder = persistentSetOf<IntWrapper>().builder()

        var expectedPersistent = linkedSetOf<IntWrapper>()
        var expectedBuilder = linkedSetOf<IntWrapper>()

        fun builderAdd(value: Int) {
            builder.add(key(value))
            expectedBuilder.add(key(value))
        }

        fun builderAddAll(vararg values: Int) {
            val keys = keys(*values)
            builder.addAll(keys)
            expectedBuilder.addAll(keys)
        }

        fun builderRemove(value: Int) {
            builder.remove(key(value))
            expectedBuilder.remove(key(value))
        }

        fun builderRemoveAll(vararg values: Int) {
            val keys = keys(*values).toSet()
            builder.removeAll(keys)
            expectedBuilder.removeAll(keys)
        }

        fun persistentAdd(value: Int) {
            val key = key(value)
            persistent = persistent.adding(key)
            expectedPersistent.add(key)
        }

        fun persistentAddAll(vararg values: Int) {
            val keys = keys(*values)
            persistent = persistent.addingAll(keys)
            expectedPersistent.addAll(keys)
        }

        fun persistentRemove(value: Int) {
            val key = key(value)
            persistent = persistent.removing(key)
            expectedPersistent.remove(key)
        }

        fun persistentRemoveAll(vararg values: Int) {
            val keys = keys(*values)
            persistent = persistent.removingAll(keys)
            expectedPersistent.removeAll(keys.toSet())
        }

        fun rebuildBuilderFromPersistent() {
            builder = persistent.builder()
            expectedBuilder = LinkedHashSet(expectedPersistent)
        }

        fun rebuildPersistentFromBuilder() {
            persistent = builder.build()
            expectedPersistent = LinkedHashSet(expectedBuilder)
        }

        builderAdd(348)
        builderRemoveAll(348, 348, 64)
        persistentAddAll(368, 274, 483, 445)
        rebuildBuilderFromPersistent()
        rebuildPersistentFromBuilder()
        builderAddAll(368, 368, 368, 368)
        persistentAdd(457)
        builderRemove(368)
        builderRemoveAll(49, 274)
        builderAdd(302)
        persistentRemoveAll(346, 43, 169, 368)
        builderRemoveAll(483, 211, 348, 442, 211)
        persistentAddAll(400)
        builderAdd(158)
        persistentAdd(164)
        persistentAddAll(277, 90, 274)
        persistentAddAll(274, 27)
        rebuildPersistentFromBuilder()
        persistentRemoveAll(197, 342, 438, 287, 498)
        rebuildPersistentFromBuilder()
        builderRemoveAll(445, 445, 312)
        rebuildPersistentFromBuilder()
        builderAddAll(302)
        rebuildBuilderFromPersistent()
        persistentAddAll(302)
        persistentRemoveAll(155, 434, 206)
        persistentRemoveAll(15, 96, 22, 302)
        builderRemove(302)
        builderAdd(243)
        persistentAddAll(158, 286)
        builderRemoveAll(155, 74, 61, 158, 186)
        persistentAdd(298)
        persistentRemove(85)
        builderRemove(243)
        persistentAdd(44)
        persistentRemoveAll(406)

        assertEquals(expectedPersistent, LinkedHashSet(persistent.toList()))
        assertEquals(expectedBuilder, LinkedHashSet(builder.build().toList()))
        assertEquals(expectedBuilder.toList(), builder.build().toList())
    }

    @Test
    fun `removes of every remaining element keep hasNext true and next throws`() {
        for ((origin, newBuilder) in orderedSetBuilders) {
            val builder = newBuilder()
            val iterator = builder.iterator()
            assertEquals(1, iterator.next(), origin)
            assertEquals(2, iterator.next(), origin)

            for (element in listOf(1, 2, 3)) assertTrue(builder.remove(element), origin)

            assertTrue(iterator.hasNext(), origin)
            assertFailsWith<ConcurrentModificationException>(origin) { iterator.next() }
        }
    }

    @Test
    fun `add to the iterator of an empty builder keeps it false and next throws`() {
        val builder = persistentSetOf<Int>().builder()
        val iterator = builder.iterator()

        assertTrue(builder.add(1))

        assertFalse(iterator.hasNext())
        assertFailsWith<ConcurrentModificationException> { iterator.next() }
    }
}
