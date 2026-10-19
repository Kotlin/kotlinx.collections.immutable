/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.implementations.immutableList.LOG_MAX_BUFFER_SIZE
import kotlinx.collections.immutable.implementations.immutableList.MAX_BUFFER_SIZE
import kotlinx.collections.immutable.implementations.immutableList.PersistentVectorBuilder
import kotlinx.collections.immutable.implementations.immutableList.rootSize
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Every call leaves a persistent list and its builder with the content an ArrayList gets from it, read by index and by
 * list iterators from both ends, at the sizes where the trie changes shape and at the indices around its boundaries,
 * and with a trie that keeps nothing behind the size under a root no taller than its leaves need. The list a call
 * starts from never changes, nor does a list the builder built, while the builder goes on writing at both ends.
 *
 * A new call is a row of [calls]; a new size, an element of [sizes].
 */
class ListBoundaryTest {

    @Test
    fun persistentList() = runCells {
        for (size in sizes) for ((origin, newList) in listsOf(size)) for ((description, call) in calls(size)) cell("$origin, $description") {
            val list = newList()
            val model = MutableList(size) { it }
            val persistent = PersistentCalls(list)

            assertEquals(model.call(), persistent.call(), "the result")
            assertContent(model, persistent.list, "the list the call returned")
            assertShape(persistent.list.builder(), "the list the call returned")
            assertContent(model, list.mutate { val _ = it.call() }, "the list mutate returned")
            assertContent(List(size) { it }, list, "the list before the call")
        }
    }

    @Test
    fun builder() = runCells {
        for (size in sizes) {
            val list = persistentListOf<Int>().addingAll(List(size) { it })
            for ((origin, newBuilder) in listBuildersOf(list, NEW)) for ((description, call) in calls(size)) cell("$origin, $description") {
                val builder = newBuilder()
                val model = MutableList(size) { it }

                assertEquals(model.call(), builder.call(), "the result")
                assertContent(model, builder, "the builder")
                assertShape(builder, "the builder")

                builder.writeEnds(element = BEFORE_BUILD)
                model.writeEnds(element = BEFORE_BUILD)
                val built = builder.build()
                builder.writeEnds(element = AFTER_BUILD)
                assertContent(model, built, "the list built after the call")
                assertContent(List(size) { it }, list, "the list the builder was made from")
            }
        }
    }
}

private const val LEAF = MAX_BUFFER_SIZE
private const val LEVEL = LEAF * LEAF

/** A full two-level root, one more leaf and a tail of one: a root just promoted to three levels. */
private const val THREE_LEVELS = LEVEL * LEAF + LEAF + 1

/** Around a full tail, a full leaf, a full root and a promoted root. */
private val sizes = listOf(0, 1, 3, 31, 32, 33, 40, 63, 64, 65, 100, 1023, 1024, 1025, 1056, 1057, 1100, THREE_LEVELS)

private const val NEW = -1
private const val MISSING = -2
private const val BEFORE_BUILD = -3
private const val AFTER_BUILD = -4

private fun newElements(count: Int): List<Int> = List(count) { -10 - it }

private fun trieSize(size: Int): Int = if (size <= LEAF) 0 else rootSize(size)

/** One, as many as fill the tail, one more, and a leaf and one. */
private fun counts(size: Int): List<Int> {
    val room = LEAF - (size - trieSize(size))
    return listOf(1, room, room + 1, LEAF + 1).filter { it > 0 }.distinct()
}

/** Up to a leaf, addingAll and addings give the same buffers. */
private fun listsOf(size: Int): Origins<PersistentList<Int>> {
    val fromAddingAll = "a list of $size from addingAll" to { persistentListOf<Int>().addingAll(List(size) { it }) }
    val fromAddings = "a list of $size from addings" to { (0..<size).fold(persistentListOf<Int>()) { list, element -> list.adding(element) } }
    return if (size <= LEAF) listOf(fromAddingAll) else listOf(fromAddingAll, fromAddings)
}

/**
 * The builder makes retainAll, the calls with a predicate and the clears through subList one element at a time, so
 * their rows come last and skip three levels, where they would take longer than the rest of the table together.
 */
private fun calls(size: Int): List<Call<MutableList<Int>>> = buildList {
    val positions = boundaryIndices(size) + size
    call("add(x)") { add(NEW) }
    for (count in listOf(0, LEVEL + LEAF) + counts(size)) call("addAll($count elements)") { addAll(newElements(count)) }
    for (position in positions) {
        call("add($position, x)") { add(position, NEW) }
        for (count in counts(size)) call("addAll($position, $count elements)") { addAll(position, newElements(count)) }
    }
    for (index in boundaryIndices(size)) {
        call("set($index, x)") { set(index, NEW) }
        call("removeAt($index)") { removeAt(index) }
    }
    call("clear()") { clear() }
    call("remove(a missing element)") { remove(MISSING) }
    if (size > 0) {
        call("remove(the first element)") { remove(first()) }
        call("remove(the last element)") { remove(last()) }
    }
    call("removeAll(nothing)") { removeAll(emptySet()) }
    call("removeAll(a missing element)") { removeAll(setOf(MISSING)) }
    call("removeAll(every other element)") { removeAll((0..<size step 2).toSet()) }
    for (index in boundaryIndices(size)) {
        call("removeAll(the suffix from $index)") { removeAll((index..<size).toSet()) }
        call("removeAll(the prefix before $index)") { removeAll((0..<index).toSet()) }
        if (index % LEAF == 0 && index < trieSize(size)) call("removeAll(the leaf at $index)") { removeAll((index..<index + LEAF).toSet()) }
    }
    for (position in positions) {
        call("subList($position, size)") { subList(position, size) }
        call("subList(0, $position)") { subList(0, position) }
        call("subList(0, $position).add(x)") { subList(0, position).apply { add(NEW) } }
    }

    if (size == THREE_LEVELS) return@buildList
    call("retainAll(nothing)") { retainAll(emptySet()) }
    call("retainAll(every other element)") { retainAll((0..<size step 2).toSet()) }
    call("removeAll { every other element }") { removeAll { it % 2 == 0 } }
    for (index in boundaryIndices(size)) {
        call("retainAll(the suffix from $index)") { retainAll((index..<size).toSet()) }
        call("retainAll(the prefix before $index)") { retainAll((0..<index).toSet()) }
        call("removeAll { the suffix from $index }") { removeAll { it >= index } }
        if (index % LEAF == 0 && index < trieSize(size)) call("retainAll(the leaf at $index)") { retainAll((index..<index + LEAF).toSet()) }
    }
    for (position in positions) {
        call("subList($position, size).clear()") { subList(position, size).apply { clear() } }
        call("subList(0, $position).clear()") { subList(0, position).apply { clear() } }
    }
}

/** A list whose writes replace [list] with what the persistent call returns: add with adding, set with replacingAt. */
private class PersistentCalls(var list: PersistentList<Int>) : AbstractMutableList<Int>(), RandomAccess {
    override val size: Int get() = list.size
    override fun get(index: Int): Int = list[index]
    override fun set(index: Int, element: Int): Int = list[index].also { list = list.replacingAt(index, element) }
    override fun removeAt(index: Int): Int = list[index].also { list = list.removingAt(index) }
    override fun add(index: Int, element: Int) { list = list.addingAt(index, element) }
    override fun add(element: Int): Boolean = changes { adding(element) }
    override fun addAll(elements: Collection<Int>): Boolean = changes { addingAll(elements) }
    override fun addAll(index: Int, elements: Collection<Int>): Boolean = changes { addingAllAt(index, elements) }
    override fun remove(element: Int): Boolean = changes { removing(element) }
    override fun removeAll(elements: Collection<Int>): Boolean = changes { removingAll(elements) }
    override fun retainAll(elements: Collection<Int>): Boolean = changes { retainingAll(elements) }
    override fun clear() { list = list.cleared() }

    private fun changes(call: PersistentList<Int>.() -> PersistentList<Int>): Boolean {
        val size = list.size
        list = list.call()
        return list.size != size
    }
}

private fun MutableList<Int>.writeEnds(element: Int) {
    if (isEmpty()) return
    this[0] = element
    this[lastIndex] = element
}

/** Names the first index that differs, where assertEquals would print two long lists. */
private fun assertContent(expected: List<Int>, actual: List<Int>, message: String) {
    assertEquals(expected.size, actual.size, "$message: the size")
    val forward = actual.listIterator()
    for (index in expected.indices) {
        val element = expected[index]
        if (actual[index] != element) fail("$message: the element at $index is ${actual[index]}, not $element")
        if (forward.next() != element) fail("$message: next() at $index is not $element")
    }
    val backward = actual.listIterator(actual.size)
    for (index in expected.indices.reversed()) {
        if (backward.previous() != expected[index]) fail("$message: previous() at $index is not ${expected[index]}")
    }
}

/** Content checks cannot see an element left behind the size or a root taller than its leaves need. */
private fun assertShape(builder: PersistentList.Builder<Int>, message: String) {
    val vector = builder as PersistentVectorBuilder<Int>
    val trieSize = trieSize(vector.size)
    var shift = 0
    while (trieSize > LEAF shl shift) shift += LOG_MAX_BUFFER_SIZE
    assertEquals(shift, vector.rootShift, "$message: the height of the root")
    assertEquals(trieSize, vector.root?.let { elementsUnder(it, shift) } ?: 0, "$message: the elements in the trie")
    assertEquals(vector.size - trieSize, vector.tail.slots().count { it != null }, "$message: the elements in the tail")
}

private fun elementsUnder(node: Array<*>, shift: Int): Int =
    if (shift == 0) node.slots().count { it != null }
    else node.slots().sumOf { if (it == null) 0 else elementsUnder(it as Array<*>, shift - LOG_MAX_BUFFER_SIZE) }

/** The slots of a buffer that hold elements or nodes; a builder's buffer has one more, holding the ownership marker. */
private fun Array<*>.slots(): List<Any?> = take(LEAF)
