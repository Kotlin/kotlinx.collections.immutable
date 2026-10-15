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
import kotlinx.collections.immutable.toPersistentList
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * A persistent list and its builder hold the same content as an ArrayList after every operation, at every size where
 * the trie changes shape and at the boundary indices of the first leaf, the first level-1 node, the last leaf and the
 * tail. A list built before the operation never changes, nor does one built after it, when the builder goes on
 * writing at both ends, where the leaf an operation pulled into the tail and the path it kept are. The trie behind
 * them stays well-formed: every leaf is full, nothing is left behind the last leaf, the root has the least height for
 * its leaves, and the tail is never empty while the root is not.
 */
class ListBoundaryTest {

    @Test
    fun persistentList() = runCells {
        val checked = mutableSetOf<Pair<Int, KClass<*>>>()
        for (size in sizes) {
            val elements = List(size) { it }
            val rows = rowsOf(size)
            for ((origin, newList) in listsOf(size)) for (operation in rows.both + rows.persistentOnly) {
                cell("$origin, ${operation.description}") {
                    val list = newList()
                    val model = list.toMutableList()

                    val _ = operation.mutable(model)
                    val actual = operation.persistent(list)

                    compare<List<Int>>(model, actual) { contentBehavior(checked) }
                    assertShape(actual.builder(), "the result")
                    assertContent(elements, list, "the list before the operation")
                }
            }
        }
    }

    @Test
    fun builder() = runCells {
        val checked = mutableSetOf<Pair<Int, KClass<*>>>()
        for (size in sizes) {
            val elements = List(size) { it }
            val rows = rowsOf(size)
            for ((origin, newSubject) in buildersOf(size)) {
                for (operation in rows.both) {
                    builderCell(elements, origin, newSubject, operation.description, operation.mutable, checked)
                }
                for ((description, operation) in rows.builderOnly) {
                    builderCell(elements, origin, newSubject, description, operation, checked)
                }
            }
        }
    }
}

private fun Cells.builderCell(
    elements: List<Int>,
    origin: String,
    newSubject: () -> Subject,
    description: String,
    operation: MutableList<Int>.() -> Any?,
    checked: MutableSet<Pair<Int, KClass<*>>>,
) = cell("$origin, $description") {
    val (builder, earlier) = newSubject()
    val model = builder.toMutableList()

    val expected = model.operation()
    val actual = builder.operation()

    if (expected is List<*> && actual is List<*>) compare(expected, actual) { contentBehavior(checked) } else assertEquals(expected, actual)
    compare<List<Int>>(model, builder) { contentBehavior(checked) }
    assertShape(builder, "the builder")
    setEnds(builder, model, CHANGED)
    if (earlier != null) assertContent(elements, earlier, "the list built before the operation")

    val built = builder.build()
    val builtModel = model.toList()
    setEnds(builder, model, SPARE)
    builder.add(SPARE)
    assertShape(builder, "the builder after the build")
    assertContent(builtModel, built, "the list built after the operation")
    assertContent(model + SPARE, builder, "the builder after the build")
}

private const val LEAF = MAX_BUFFER_SIZE
private const val LEVEL = MAX_BUFFER_SIZE * MAX_BUFFER_SIZE

/** A root just promoted to three levels: a full two-level root, one more leaf and a tail of one. */
private const val THREE_LEVELS = LEVEL * LEAF + LEAF + 1

private const val WRITTEN = -1
private const val NEW = -2
private const val MISSING = -3

/** The element written at both ends and added after the build, to see the built list keep its content. */
private const val SPARE = -4

/** The element written at both ends after the operation, to see the list built before keep its content. */
private const val CHANGED = -5

/** [count] elements no list holds. */
private fun added(count: Int): List<Int> = List(count) { -6 - it }

/** The sizes at which the trie changes shape: around a full tail, a full leaf, a full root and a promoted root. */
private val sizes: List<Int> = (listOf(0, 1, LEAF - 1, LEAF, LEAF + 1, 2 * LEAF - 1, 2 * LEAF, 2 * LEAF + 1, LEVEL - 1, LEVEL, LEVEL + 1, LEVEL + LEAF, LEVEL + LEAF + 1) + listDepths.keys).distinct().sorted() + THREE_LEVELS

private fun shapeOf(size: Int): String = listDepths[size] ?: when {
    size == 0 -> "empty"
    size <= LEAF -> "the tail only"
    size <= 2 * LEAF -> "a root that is one leaf"
    size <= LEVEL + LEAF -> "a root over leaves"
    size <= LEVEL * LEAF + LEAF -> "a root over level-1 nodes"
    else -> "a root over level-2 nodes"
}

/** The elements of the trie of a list of [size]; the rest are the tail. */
private fun trieSize(size: Int): Int = if (size <= LEAF) 0 else rootSize(size)

/** [boundaryIndices] with the ends of the last leaf and the start of the tail. */
private fun indices(size: Int): List<Int> =
    (boundaryIndices(size) + listOf(trieSize(size) - LEAF, trieSize(size) - 1, trieSize(size))).filter { it in 0..<size }.distinct().sorted()

private fun positions(size: Int): List<Int> = indices(size) + size

private fun leafStarts(size: Int): List<Int> =
    indices(size).map { it shr LOG_MAX_BUFFER_SIZE shl LOG_MAX_BUFFER_SIZE }.filter { it < trieSize(size) }.distinct()

/** The counts to add to a list of [size]: one, as many as fill the tail, one more than that, and a leaf and one. */
private fun counts(size: Int): List<Int> {
    val room = LEAF - (size - trieSize(size))
    return listOf(1, room, room + 1, LEAF + 1).filter { it > 0 }.distinct()
}

/**
 * From addingAll, which above a leaf goes through a builder and keeps its buffers, and from addings one at a time; up
 * to a leaf the two give the same buffers.
 */
private fun listsOf(size: Int): List<Pair<String, () -> PersistentList<Int>>> {
    val fromAddingAll = "a list of $size from addingAll, ${shapeOf(size)}" to { List(size) { it }.toPersistentList() }
    val fromAddings = "a list of $size from addings, ${shapeOf(size)}" to { (0..<size).fold(persistentListOf<Int>()) { list, element -> list.adding(element) } }
    return if (size > LEAF) listOf(fromAddingAll, fromAddings) else listOf(fromAddingAll)
}

/** [earlier] is null when the builder shares nothing. */
private data class Subject(val builder: PersistentList.Builder<Int>, val earlier: PersistentList<Int>?)

/**
 * The two origins with a write own different paths, and the second writes after a build, which the list built before
 * must not see. They run above a leaf, where a write owns less than adds do, up to [LEAVES], and at [NODES] for a
 * taller trie.
 */
private fun buildersOf(size: Int): List<Pair<String, () -> Subject>> {
    val elements = List(size) { it }
    val shape = shapeOf(size)
    val builders = mutableListOf(
        "from a list of $size, $shape" to { val list = elements.toPersistentList(); Subject(list.builder(), list) },
        "from adds of $size, $shape" to { Subject(persistentListOf<Int>().builder().apply { addAll(elements) }, null) },
    )
    if (size in LEAF + 1..LEAVES || size == NODES) {
        builders += "from a list of $size with a set of the first element, $shape" to { val list = elements.toPersistentList(); Subject(list.builder().apply { this[0] = WRITTEN }, list) }
        builders += "from adds of $size after a build and a set of the last element, $shape" to {
            val builder = persistentListOf<Int>().builder().apply { addAll(elements) }
            val list = builder.build()
            builder[builder.lastIndex] = WRITTEN
            Subject(builder, list)
        }
    }
    return builders
}

private class ListOperation(
    val description: String,
    val persistent: PersistentList<Int>.() -> PersistentList<Int>,
    val mutable: MutableList<Int>.() -> Any?,
)

/** Gives the lambda its receiver type, which a bare pair cannot infer. */
private fun operation(description: String, operation: MutableList<Int>.() -> Any?): Pair<String, MutableList<Int>.() -> Any?> = description to operation

private class Rows(
    val both: List<ListOperation>,
    val persistentOnly: List<ListOperation>,
    val builderOnly: List<Pair<String, MutableList<Int>.() -> Any?>>,
)

private fun rowsOf(size: Int): Rows =
    if (size == THREE_LEVELS) deepRows() else Rows(operations(size), mutations(size), views(size))

private fun List<Int>.suffixFrom(index: Int): Set<Int> = subList(index, size).toHashSet()
private fun List<Int>.prefixBefore(index: Int): Set<Int> = subList(0, index).toHashSet()
private fun List<Int>.tail(): Set<Int> = subList(trieSize(size), size).toHashSet()
private fun List<Int>.leafAt(start: Int): Set<Int> = subList(start, start + LEAF).toHashSet()
private fun List<Int>.everyOther(): Set<Int> = filterIndexed { index, _ -> index % 2 == 0 }.toHashSet()

/** The view writes only up to [LEAVES], where a cell is cheap. */
private fun operations(size: Int): List<ListOperation> = buildList {
    add(ListOperation("add(x)", { adding(NEW) }) { add(NEW) })
    add(ListOperation("addAll(nothing)", { addingAll(emptyList()) }) { addAll(emptyList()) })
    for (count in counts(size) + (LEVEL + LEAF)) {
        add(ListOperation("addAll($count elements)", { addingAll(added(count)) }) { addAll(added(count)) })
    }
    add(ListOperation("remove(a missing element)", { removing(MISSING) }) { remove(MISSING) })
    if (size > 0) {
        add(ListOperation("remove(the first element)", { removing(first()) }) { remove(first()) })
        add(ListOperation("remove(the last element)", { removing(last()) }) { remove(last()) })
    }
    add(ListOperation("removeAll(nothing)", { removingAll(emptySet()) }) { removeAll(emptySet()) })
    add(ListOperation("removeAll(a missing element)", { removingAll(setOf(MISSING)) }) { removeAll(setOf(MISSING)) })
    add(ListOperation("removeAll(every other element)", { removingAll(everyOther()) }) { removeAll(everyOther()) })
    add(ListOperation("removeAll { every other element }", { val chosen = everyOther(); removingAll { it in chosen } }) { val chosen = everyOther(); removeAll { it in chosen } })
    add(ListOperation("retainAll(nothing)", { retainingAll(emptySet()) }) { retainAll(emptySet()) })
    add(ListOperation("retainAll(every other element)", { retainingAll(everyOther()) }) { retainAll(everyOther()) })
    add(ListOperation("clear()", { cleared() }) { clear() })
    for (index in indices(size)) {
        add(ListOperation("set($index, x)", { replacingAt(index, NEW) }) { set(index, NEW) })
        add(ListOperation("removeAt($index)", { removingAt(index) }) { removeAt(index) })
        add(ListOperation("removeAll(the suffix from $index)", { removingAll(suffixFrom(index)) }) { removeAll(suffixFrom(index)) })
        add(ListOperation("retainAll(the suffix from $index)", { retainingAll(suffixFrom(index)) }) { retainAll(suffixFrom(index)) })
        add(ListOperation("removeAll(the prefix before $index)", { removingAll(prefixBefore(index)) }) { removeAll(prefixBefore(index)) })
        add(ListOperation("retainAll(the prefix before $index)", { retainingAll(prefixBefore(index)) }) { retainAll(prefixBefore(index)) })
    }
    for (start in leafStarts(size)) {
        add(ListOperation("removeAll(the leaf at $start)", { removingAll(leafAt(start)) }) { removeAll(leafAt(start)) })
        if (start + LEAF < trieSize(size)) {
            add(ListOperation("removeAll(the leaf at $start and the tail)", { removingAll(leafAt(start) + tail()) }) { removeAll(leafAt(start) + tail()) })
        }
    }
    for (position in positions(size)) {
        add(ListOperation("add($position, x)", { addingAt(position, NEW) }) { add(position, NEW) })
        for (count in counts(size)) {
            add(ListOperation("addAll($position, $count elements)", { addingAllAt(position, added(count)) }) { addAll(position, added(count)) })
        }
    }
    if (size <= LEAVES) addAll(viewOperations(size))
}

/** The writes through subList views, whose index arithmetic is the stdlib's, and the one-by-one retainAll of a leaf. */
private fun viewOperations(size: Int): List<ListOperation> = buildList {
    for (start in leafStarts(size)) {
        add(ListOperation("retainAll(the leaf at $start)", { retainingAll(leafAt(start)) }) { retainAll(leafAt(start)) })
    }
    for (index in indices(size)) {
        add(ListOperation("subList($index, size).removeAt(0)", { mutate { it.subList(index, it.size).removeAt(0) } }) { subList(index, size).apply { removeAt(0) } })
    }
    for (position in positions(size)) {
        add(ListOperation("subList($position, size).clear()", { mutate { it.subList(position, it.size).clear() } }) { subList(position, size).apply { clear() } })
        add(ListOperation("subList(0, $position).clear()", { mutate { it.subList(0, position).clear() } }) { subList(0, position).apply { clear() } })
        add(ListOperation("subList(0, $position).add(x)", { mutate { it.subList(0, position).add(NEW) } }) { subList(0, position).apply { add(NEW) } })
    }
}

/**
 * Persistent-only rows: the predicate removal of a suffix, which on the builder would take stdlib's iterator like
 * retainAll of the prefix, and mutate at indices in the trie, whose build right after the write would return a stale
 * cached list.
 */
private fun mutations(size: Int): List<ListOperation> = buildList {
    for (index in indices(size)) {
        add(ListOperation("removeAll { the suffix from $index }", { val chosen = suffixFrom(index); removingAll { it in chosen } }) { val chosen = suffixFrom(index); removeAll { it in chosen } })
    }
    for (index in indices(size).filter { it < trieSize(size) }) {
        add(ListOperation("mutate { set($index, x) }", { mutate { it[index] = NEW } }) { set(index, NEW) })
        add(ListOperation("mutate { removeAt($index) }", { mutate { it.removeAt(index) } }) { removeAt(index) })
        add(ListOperation("mutate { add($index, x) }", { mutate { it.add(index, NEW) } }) { add(index, NEW) })
    }
}

/** Builder-only rows: a JDK subList reads through the builder's list iterator at an offset. */
private fun views(size: Int): List<Pair<String, MutableList<Int>.() -> Any?>> = buildList {
    for (position in positions(size)) {
        add(operation("subList($position, size)") { subList(position, size) })
        add(operation("subList(0, $position)") { subList(0, position) })
    }
}

/** Operations that move leaves across the levels of a three-level root, and the one removeAt chain through them. */
private fun deepRows(): Rows {
    val lastLeaf = LEVEL * LEAF - LEAF
    val both = listOf(
        ListOperation("add(x)", { adding(NEW) }) { add(NEW) },
        ListOperation("addAll(${LEAF + 1} elements)", { addingAll(added(LEAF + 1)) }) { addAll(added(LEAF + 1)) },
        ListOperation("add(0, x)", { addingAt(0, NEW) }) { add(0, NEW) },
        ListOperation("set(${LEVEL * LEAF - 1}, x)", { replacingAt(LEVEL * LEAF - 1, NEW) }) { set(LEVEL * LEAF - 1, NEW) },
        ListOperation("removeAt(0)", { removingAt(0) }) { removeAt(0) },
        ListOperation("removeAt(${THREE_LEVELS - 1})", { removingAt(THREE_LEVELS - 1) }) { removeAt(THREE_LEVELS - 1) },
        ListOperation("removeAll(the tail)", { removingAll(tail()) }) { removeAll(tail()) },
        ListOperation("removeAll(the suffix from $LEAF)", { removingAll(suffixFrom(LEAF)) }) { removeAll(suffixFrom(LEAF)) },
        ListOperation("removeAll(the suffix from $lastLeaf)", { removingAll(suffixFrom(lastLeaf)) }) { removeAll(suffixFrom(lastLeaf)) },
        ListOperation("removeAll(the leaf at $lastLeaf)", { removingAll(leafAt(lastLeaf)) }) { removeAll(leafAt(lastLeaf)) },
    )
    val persistentOnly = listOf(
        ListOperation("removeAll { the suffix from $lastLeaf }", { val chosen = suffixFrom(lastLeaf); removingAll { it in chosen } }) { val chosen = suffixFrom(lastLeaf); removeAll { it in chosen } },
        ListOperation("mutate { set(${LEVEL * LEAF - 1}, x) }", { mutate { it[LEVEL * LEAF - 1] = NEW } }) { set(LEVEL * LEAF - 1, NEW) },
    )
    val builderOnly = listOf(
        operation("removeAll { the suffix from $lastLeaf } one by one through the iterator") { val chosen = suffixFrom(lastLeaf); removeAll { it in chosen } },
    )
    return Rows(both, persistentOnly, builderOnly)
}

/**
 * The index check comes first, so that a mismatch names an index rather than two long lists. The walks from the middle
 * and the end are there because the trie iterator's start depends on the height. stdlib's listBehavior, whose checks
 * do not depend on the content and include a quadratic containsAll, runs once per size and class, up to two leaves.
 */
private fun <T> CompareContext<List<T>>.contentBehavior(checked: MutableSet<Pair<Int, KClass<*>>>) {
    propertyEquals("the size") { size }
    for (index in expected.indices) {
        val element = actual[index]
        if (element != expected[index]) fail("the element at $index is $element, not ${expected[index]}")
    }
    equals()
    propertyEquals("the hash code") { hashCode() }
    compareProperty({ listIterator() }) { forward(); backward() }
    compareProperty({ listIterator(size / 2) }) { forward() }
    compareProperty({ listIterator(size) }) { backward() }
    if (expected.size <= 2 * LEAF && checked.add(expected.size to actual::class)) listBehavior()
}

private fun <T> CompareContext<ListIterator<T>>.forward() {
    while (expected.hasNext()) {
        val element = actual.next()
        val expectedElement = expected.next()
        if (element != expectedElement) fail("the iterator's next at ${expected.previousIndex()} is $element, not $expectedElement")
    }
    assertFalse(actual.hasNext(), "the iterator has a next past the end")
}

private fun <T> CompareContext<ListIterator<T>>.backward() {
    while (expected.hasPrevious()) {
        val element = actual.previous()
        val expectedElement = expected.previous()
        if (element != expectedElement) fail("the iterator's previous at ${expected.nextIndex()} is $element, not $expectedElement")
    }
    assertFalse(actual.hasPrevious(), "the iterator has a previous before the start")
}

private fun setEnds(builder: MutableList<Int>, model: MutableList<Int>, element: Int) {
    if (model.isEmpty()) return
    builder[0] = element
    model[0] = element
    builder[builder.lastIndex] = element
    model[model.lastIndex] = element
}

/** assertEquals on two lists would print both whole; this names the first index that differs. */
private fun assertContent(expected: List<Int>, actual: List<Int>, message: String) {
    assertEquals(expected.size, actual.size, "$message: the size")
    for (index in expected.indices) {
        val element = actual[index]
        if (element != expected[index]) fail("$message: the element at $index is $element, not ${expected[index]}")
    }
}

private fun assertShape(builder: PersistentList.Builder<*>, message: String) {
    val vector = builder as PersistentVectorBuilder<*>
    val size = vector.size
    val root = vector.root
    val tail = vector.tail
    val trieSize = trieSize(size)
    if (root == null) {
        assertTrue(size <= LEAF, "$message: no root at size $size")
        assertEquals(0, vector.rootShift, "$message: the shift without a root")
    } else {
        assertTrue(size > LEAF, "$message: an empty tail beside the root at size $size")
        assertTrue(vector.rootShift == 0 || root[1] != null, "$message: a root of one child at shift ${vector.rootShift}")
        assertEquals(trieSize shr LOG_MAX_BUFFER_SIZE, leavesUnder(root, vector.rootShift, message), "$message: the leaves of the trie")
    }
    for (cell in 0..<(size - trieSize)) if (tail[cell] == null) fail("$message: the tail cell $cell is empty")
    for (cell in (size - trieSize)..<minOf(tail.size, LEAF)) if (tail[cell] != null) fail("$message: the tail cell $cell is behind the size")
}

private fun leavesUnder(node: Array<Any?>, shift: Int, message: String): Int {
    if (shift == 0) {
        for (cell in 0..<LEAF) if (node[cell] == null) fail("$message: cell $cell of a leaf is empty")
        return 1
    }
    val children = (0..<LEAF).takeWhile { node[it] != null }
    if (children.isEmpty()) fail("$message: a node at shift $shift has no children")
    for (cell in children.size..<LEAF) if (node[cell] != null) fail("$message: cell $cell of a node at shift $shift is behind its last child")
    var leaves = 0
    for (cell in children) {
        @Suppress("UNCHECKED_CAST")
        val childLeaves = leavesUnder(node[cell] as Array<Any?>, shift - LOG_MAX_BUFFER_SIZE, message)
        if (cell != children.last() && childLeaves != 1 shl (shift - LOG_MAX_BUFFER_SIZE)) fail("$message: child $cell of a node at shift $shift is not full")
        leaves += childLeaves
    }
    return leaves
}
