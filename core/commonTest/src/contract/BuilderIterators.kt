/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlin.test.assertEquals

/*
 * The fail-fast contract of the builder iterators, tested as stdlib's ConcurrentModificationTest tests it. A table of
 * calls through the builder, each saying whether it invalidates live iterators, is crossed with the calls on a live
 * iterator, alone and in pairs, as guava-testlib's IteratorTester sends sequences of calls to the iterator under test
 * and to a reference iterator.
 */

class BuilderOperation<in B>(
    val description: String,
    /** true when the call moves the size at least once or is a clear(), which invalidates every live iterator. */
    val throwsCME: Boolean = true,
    val function: B.() -> Unit,
)

val next: Call<MutableIterator<*>> = "next()" to { next() }

/** The calls on a live iterator, which [testIterator] makes alone and in pairs. */
val iteratorCalls: List<Call<MutableIterator<*>>> = listOf(next, "remove()" to { remove() })

val listIteratorCalls: List<Call<MutableListIterator<Any>>> =
    iteratorCalls + listOf("previous()" to { previous() }, "add()" to { add("new") }, "set()" to { set("new") })

/** The elements an iterator still has. A live map entry is read as its key and value the moment it is drained. */
fun Iterator<*>.drain(): List<Any?> = asSequence().map(::read).toList()

private fun read(element: Any?): Any? = if (element is Map.Entry<*, *>) element.key to element.value else element

/**
 * Opens every iterator of [views] on a fresh builder of every origin in [builders], calls next 0 to size times, makes
 * a call of [builderOps] and then one call of [iteratorCalls] or two in a row: one cell per origin, view, builder call
 * and call sequence, with every position inside it.
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
    builderOps: List<BuilderOperation<B>>,
    iteratorCalls: List<Call<I>>,
) {
    val alone = iteratorCalls.map { listOf(it) }
    val pairs = iteratorCalls.flatMap { first -> iteratorCalls.map { second -> listOf(first, second) } }
    for (builderOp in builderOps) for (calls in alone + pairs) for ((origin, newBuilder) in builders) for ((view, open) in views) {
        cell("$origin, $view, ${calls.joinToString(" then ") { it.first }} after ${builderOp.description}") {
            val opened = newBuilder().open().drain()
            for (returned in 0..opened.size) at("after $returned next") {
                val builder = newBuilder()
                val iterator = builder.open()
                repeat(returned) { val _ = iterator.next() }
                builderOp.function(builder)
                val content = builder.open().drain()

                if (builderOp.throwsCME) {
                    for ((name, call) in calls) {
                        val fresh = modelIteratorOf<I>(opened.toMutableList(), returned)
                        val noElement = outcomeOf { fresh.call() } == "throws IllegalStateException"
                        val expected = if (noElement) "throws IllegalStateException" else "throws ConcurrentModificationException"
                        assertEquals(expected, outcomeOf { iterator.call() }, name)
                    }
                    assertEquals("throws ConcurrentModificationException", outcomeOf { iterator.next() }, "next() afterwards")
                    assertEquals(content, builder.open().drain(), "the builder")
                } else {
                    val model = content.toMutableList()
                    val modelIterator = modelIteratorOf<I>(model, returned)
                    for ((name, call) in calls) assertEquals(outcomeOf { modelIterator.call() }, outcomeOf { iterator.call() }, name)
                    assertEquals(modelIterator.drain(), iterator.drain(), "the rest")
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

/** What [call] returns, read as [drain] reads it, or "throws X" when it throws X. A failed assertion fails the cell. */
private fun outcomeOf(call: () -> Any?): Any? =
    try {
        read(call())
    } catch (e: AssertionError) {
        throw e
    } catch (e: Throwable) {
        "throws ${e::class.simpleName}"
    }
