/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlin.test.assertEquals

/**
 * A call through a builder. [movesTheSize] is true when the call moves the size at least once or is a clear(), which
 * invalidates every live iterator even of an empty builder; [operations] sets it by the section the call is written in.
 */
class BuilderOperation<in B>(val description: String, val movesTheSize: Boolean, val function: B.() -> Unit)

/**
 * A table of calls through a builder in two sections, the calls that keep the size and the calls that move it. The
 * keep section starts with "no call", so that the iterator calls also run on an untouched builder.
 */
fun <B> operations(
    thatKeepTheSize: Rows<B.() -> Unit>.() -> Unit = {},
    thatMoveTheSize: Rows<B.() -> Unit>.() -> Unit = {},
): List<BuilderOperation<B>> =
    rows(thatKeepTheSize).map { (description, call) -> BuilderOperation(description, movesTheSize = false, call) } +
        rows(thatMoveTheSize).map { (description, call) -> BuilderOperation(description, movesTheSize = true, call) }

/** The calls on a live iterator, which [testIterator] makes alone and in pairs. */
val iteratorCalls: List<Call<MutableIterator<*>>> = rows {
    "next()" { next() }
    "remove()" { remove() }
}

val listIteratorCalls: List<Call<MutableListIterator<Any>>> = iteratorCalls + rows {
    "previous()" { previous() }
    "add()" { add("new") }
    "set()" { set("new") }
}

/** The elements an iterator still has. A live map entry is read as its key and value the moment it is drained. */
fun Iterator<*>.drain(): List<Any?> = asSequence().map(::read).toList()

private fun read(element: Any?): Any? = if (element is Map.Entry<*, *>) element.key to element.value else element

/**
 * Tests the fail-fast contract of the builder iterators as stdlib's ConcurrentModificationTest and guava-testlib's
 * IteratorTester do. Opens every iterator of [views] on a fresh builder of every origin in [builders], calls next 0 to
 * size times, makes a call of [operations] and then one call of [iteratorCalls] or two in a row: one cell per origin,
 * view, builder call and call sequence, with every position inside it.
 *
 * After a call that keeps the size, the iterator of a stdlib list of what the builder holds, advanced as far, takes the
 * same calls and judges them: each returns what the list's iterator returns or throws the exception it throws, the
 * two then have the same rest, and the builder ends up holding what the list holds, in that order when it is a list.
 *
 * After a call that moves the size, the iterator is frozen where the call found it: each call throws
 * ConcurrentModificationException, except a remove or a set with no element to act on, which throws
 * IllegalStateException; a fresh stdlib list iterator at the same position, over what the builder held when the
 * iterator was opened, tells the two apart. A next afterwards throws ConcurrentModificationException, and the builder
 * still holds what the call left in it.
 */
fun <B, I : MutableIterator<*>> Cells.testIterator(
    builders: Origins<B>,
    views: Views<B, I>,
    operations: List<BuilderOperation<B>>,
    iteratorCalls: List<Call<I>>,
) {
    val alone = iteratorCalls.map { call -> call.first to listOf(call) }
    val pairs = iteratorCalls.flatMap { (first, firstCall) ->
        iteratorCalls.map { (second, secondCall) ->
            "$first then $second" to listOf("the first $first" to firstCall, "the second $second" to secondCall)
        }
    }
    for ((origin, newBuilder) in builders) for ((view, open) in views) for (operation in operations) for ((calls, steps) in alone + pairs) {
        cell("$origin, $view, $calls after ${operation.description}") {
            val opened = newBuilder().open().drain()
            for (returned in 0..opened.size) at("after $returned next") {
                val builder = newBuilder()
                val iterator = builder.open()
                repeat(returned) { val _ = iterator.next() }
                operation.function(builder)
                val content = builder.open().drain()

                if (operation.movesTheSize) at("frozen where the call found it") {
                    for ((description, call) in steps) {
                        val fresh = outcomeOf { modelIteratorOf<I>(opened.toMutableList(), returned).call() }
                        val expected = if (fresh == "throws IllegalStateException") fresh else "throws ConcurrentModificationException"
                        assertEquals(expected, outcomeOf { iterator.call() }, description)
                    }
                    assertEquals("throws ConcurrentModificationException", outcomeOf { iterator.next() }, "next() afterwards")
                    assertEquals(content, builder.open().drain(), "the builder")
                } else at("as the iterator of a stdlib list of the content") {
                    assertEquals(opened.size, content.size, "the size after ${operation.description}, which its section says it keeps")
                    val model = content.toMutableList()
                    val modelIterator = modelIteratorOf<I>(model, returned)
                    for ((description, call) in steps) assertEquals(outcomeOf { modelIterator.call() }, outcomeOf { iterator.call() }, description)
                    assertEquals(modelIterator.drain(), outcomeOf { iterator.drain() }, "the rest")
                    if (builder is List<*>) assertEquals(model, builder.open().drain(), "the builder")
                    else assertEquals(model.unordered(), builder.open().drain().unordered(), "the builder")
                }
            }
        }
    }
}

/** A stdlib list iterator over [elements], advanced by [returned] calls of next, which judges the one under test. */
@Suppress("UNCHECKED_CAST")
private fun <I : MutableIterator<*>> modelIteratorOf(elements: MutableList<Any?>, returned: Int): I =
    (elements.listIterator() as I).also { model -> repeat(returned) { val _ = model.next() } }

/** The elements as a multiset, since a remove from a hash map may promote an entry ahead of the visited ones. */
private fun List<Any?>.unordered(): List<Any?> = sortedBy { it.toString() }

/**
 * What [call] returns, read as [drain] reads it, "returns" when that is Unit, or "throws X" when it throws X. A failed
 * assertion fails the cell.
 */
private fun outcomeOf(call: () -> Any?): Any? =
    try {
        val result = call()
        if (result is Unit) "returns" else read(result)
    } catch (e: AssertionError) {
        throw e
    } catch (e: Throwable) {
        "throws ${e::class.simpleName}"
    }
