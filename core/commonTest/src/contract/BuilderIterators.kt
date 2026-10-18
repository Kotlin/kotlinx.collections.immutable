/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/*
 * The fail-fast contract of the builder iterators, tested as stdlib's ConcurrentModificationTest tests it. A table of
 * calls through the builder, each saying whether it invalidates live iterators, is crossed with a table of calls on
 * a live iterator.
 */

class BuilderOperation<in B>(
    val description: String,
    /** true when the call moves the size at least once, which invalidates every live iterator. */
    val throwsCME: Boolean = true,
    val function: B.() -> Unit,
)

class IteratorOperation<in I>(
    val description: String,
    /**
     * true when the call needs an element returned by next, as remove and set do. Without one it throws
     * IllegalStateException first, even after a structural call, as in java.util.
     */
    val needsAnElement: Boolean = false,
    val function: I.() -> Any?,
)

val iteratorOperations: List<IteratorOperation<MutableIterator<*>>> = listOf(
    IteratorOperation("next()") { next() },
    IteratorOperation("remove()", needsAnElement = true) { remove() },
    IteratorOperation("remove() twice", needsAnElement = true) { remove(); val _ = assertFailsWith<IllegalStateException> { remove() } },
)

val listIteratorOperations: List<IteratorOperation<MutableListIterator<Any>>> = listOf(
    IteratorOperation("previous()") { previous() },
    IteratorOperation("add()") { add("new") },
    IteratorOperation("add() then set()") { add("new"); val _ = assertFailsWith<IllegalStateException> { set("new") } },
    IteratorOperation("add() then remove()") { add("new"); val _ = assertFailsWith<IllegalStateException> { remove() } },
    IteratorOperation("set()", needsAnElement = true) { set("new") },
    IteratorOperation("set() twice", needsAnElement = true) { set("new"); set("newer") },
    IteratorOperation("set() then previous()", needsAnElement = true) { set("new"); previous() },
    IteratorOperation("remove() then set()", needsAnElement = true) { remove(); val _ = assertFailsWith<IllegalStateException> { set("new") } },
)

/** The elements an iterator still has. A live map entry is read as its key and value the moment it is drained. */
fun Iterator<*>.drain(): List<Any?> = asSequence().map(::read).toList()

private fun read(element: Any?): Any? = if (element is Map.Entry<*, *>) element.key to element.value else element

/**
 * Opens every iterator of [views] on a fresh builder of every origin in [builders], calls next 0 to size times, makes
 * [builderOp] and then [iteratorOp]; one cell holds every position of an origin and a view. After a structural call,
 * [iteratorOp] throws ConcurrentModificationException, IllegalStateException first when it needs an element and next
 * returned none, so does every next after it, and the builder keeps what it held. After a non-structural call, the
 * iterator does what the iterator of a stdlib list of what the builder holds does, and the builder ends up holding
 * what that list holds, in that order when it is a list.
 */
fun <B, I : MutableIterator<*>> Cells.testIterator(
    builders: Origins<B>,
    views: Views<B, I>,
    builderOp: BuilderOperation<B>,
    iteratorOp: IteratorOperation<I>,
) {
    for ((origin, newBuilder) in builders) for ((view, open) in views) {
        cell("$origin, $view, ${iteratorOp.description} after ${builderOp.description}") {
            for (returned in 0..newBuilder().open().drain().size) at("after $returned next") {
                val builder = newBuilder()
                val iterator = builder.open()
                repeat(returned) { val _ = iterator.next() }
                builderOp.function(builder)
                val content = builder.open().drain()

                if (builderOp.throwsCME) {
                    val failure = if (iteratorOp.needsAnElement && returned == 0) IllegalStateException::class
                                  else ConcurrentModificationException::class
                    assertFailsWith(failure) { iteratorOp.function(iterator) }
                    assertFailsWith<ConcurrentModificationException> { iterator.next() }
                    assertEquals(content, builder.open().drain())
                } else {
                    val model = content.toMutableList()
                    @Suppress("UNCHECKED_CAST")
                    val modelIterator = model.listIterator() as I
                    repeat(returned) { val _ = modelIterator.next() }
                    assertEquals(outcomeOf { iteratorOp.function(modelIterator) }, outcomeOf { iteratorOp.function(iterator) })
                    assertEquals(modelIterator.drain(), iterator.drain())
                    if (builder is List<*>) assertEquals(model, builder.open().drain())
                    else assertEquals(model.unordered(), builder.open().drain().unordered())
                }
            }
        }
    }
}

/** The elements as a multiset, since a remove from a hash map may promote an entry ahead of the visited ones. */
private fun List<Any?>.unordered(): List<Any?> = sortedBy { it.toString() }

/** What [call] returns, read as [drain] reads it, or the class of what it throws. A failed assertion fails the cell. */
private fun outcomeOf(call: () -> Any?): Any? =
    try {
        read(call())
    } catch (e: AssertionError) {
        throw e
    } catch (e: Throwable) {
        e::class
    }
