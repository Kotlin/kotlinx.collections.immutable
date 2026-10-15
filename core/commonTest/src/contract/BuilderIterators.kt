/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail

/*
 * The fail-fast contract of the builder iterators, tested as stdlib's ConcurrentModificationTest tests it. A table of
 * calls through the builder, each saying whether it invalidates live iterators, is crossed with a table of calls on
 * a live iterator.
 */

/** The cells of one table. Each cell runs on its own, and [runCells] then fails once, listing the failed ones. */
class Cells {
    private var count = 0
    private val failures = mutableListOf<String>()
    private var firstFailure: Throwable? = null

    fun cell(message: String, body: () -> Unit) {
        count++
        try {
            body()
        } catch (e: Throwable) {
            failures += "$message: $e"
            if (firstFailure == null) firstFailure = e
        }
    }

    internal fun report() {
        if (count == 0) fail("The table ran no cell")
        if (failures.isEmpty()) return
        val shown = failures.take(100)
        val more = if (failures.size > shown.size) "\n... and ${failures.size - shown.size} more" else ""
        fail("${failures.size} of $count cells failed:\n" + shown.joinToString("\n") + more, firstFailure)
    }
}

fun runCells(table: Cells.() -> Unit): Unit = Cells().run {
    try {
        table()
    } catch (e: Throwable) {
        cell("between the cells") { throw e }
    }
    report()
}

class BuilderOperation<in C>(
    val description: String,
    /** true when the call moves the size at least once, which invalidates every live iterator. */
    val throwsCME: Boolean = true,
    val function: C.() -> Unit,
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
)

/** The elements an iterator still has. A live map entry is read as its key and value the moment it is drained. */
internal fun Iterator<*>.drain(): List<Any?> = asSequence().map { if (it is Map.Entry<*, *>) it.key to it.value else it }.toList()

private val Any.reportedSize: Int get() = if (this is Map<*, *>) size else (this as Collection<*>).size

/** The elements as a multiset, since a remove from a hash map may promote an entry ahead of the visited ones. */
private fun List<Any?>.unordered(): List<Any?> = sortedBy { it.toString() }

/** The size [open] gives on a fresh builder, or null when that throws and a failed cell named [message] reports it. */
private fun <C> Cells.sizeOf(message: String, newBuilder: () -> C, open: C.() -> Iterator<*>): Int? =
    try {
        newBuilder().open().drain().size
    } catch (e: Throwable) {
        cell(message) { throw e }
        null
    }

/**
 * Opens every iterator of [views] on a fresh builder of every origin in [builders], calls next 0 to size times, makes
 * [builderOp] and checks what [iteratorOp] does. After a structural call it throws ConcurrentModificationException,
 * and the iterator stays failed. After a non-structural call it throws what it throws on an untouched builder, or
 * succeeds. A call that needs an element throws IllegalStateException first when next returned nothing. An assertion
 * of [iteratorOp] that fails on the untouched builder fails the cell, rather than becoming the outcome to expect.
 */
fun <C : Any, I : MutableIterator<*>> Cells.testIterator(
    builders: List<Pair<String, () -> C>>,
    views: List<Pair<String, C.() -> I>>,
    builderOp: BuilderOperation<C>,
    iteratorOp: IteratorOperation<I>,
) {
    for ((origin, newBuilder) in builders) for ((viewName, open) in views) {
        val size = sizeOf("$origin, $viewName", newBuilder, open) ?: continue
        for (returned in 0..size) cell("$origin, $viewName, ${iteratorOp.description} after $returned next, ${builderOp.description}") {
            val control = newBuilder().open()
            repeat(returned) { val _ = control.next() }
            val onUntouched = runCatching { iteratorOp.function(control) }.onFailure { if (it is AssertionError) throw it }

            val builder = newBuilder()
            val iterator = builder.open()
            repeat(returned) { val _ = iterator.next() }
            builderOp.function.invoke(builder)
            val live = builder.open().drain()
            assertEquals(live.size, builder.reportedSize)

            val failure = when {
                iteratorOp.needsAnElement && returned == 0 -> IllegalStateException::class
                builderOp.throwsCME -> ConcurrentModificationException::class
                else -> onUntouched.exceptionOrNull()?.let { it::class }
            }
            if (failure == null) {
                val _ = iteratorOp.function(iterator)
            } else {
                assertFailsWith(failure) { iteratorOp.function(iterator) }
                if (builderOp.throwsCME) {
                    assertFailsWith<ConcurrentModificationException> { iterator.next() }
                    assertEquals(live, builder.open().drain())
                } else if (iteratorOp.needsAnElement && returned == 0) {
                    assertEquals(live, iterator.drain())
                    assertEquals(live, builder.open().drain())
                }
            }
        }
    }
}

/**
 * Opens every iterator of [views] on a fresh builder of every origin in [builders], calls next 0 to size times and
 * makes the non-structural [builderOp], with and without the iterator's own remove after it. Checks that the rest of
 * the iteration is the live content from the cursor on, that the remove takes the element out of the builder, and that
 * it fails the other live iterators.
 */
fun <C : Any> Cells.testIterationContinues(
    builders: List<Pair<String, () -> C>>,
    views: List<Pair<String, C.() -> MutableIterator<*>>>,
    builderOp: BuilderOperation<C>,
) {
    require(!builderOp.throwsCME) { "${builderOp.description} is structural" }
    for ((origin, newBuilder) in builders) for ((viewName, open) in views) {
        val size = sizeOf("$origin, $viewName", newBuilder, open) ?: continue
        for (returned in 0..size) for (removeAtTheCursor in listOf(false, true)) {
            if (removeAtTheCursor && returned == 0) continue
            val message = "$origin, $viewName after $returned next, ${builderOp.description}" +
                    if (removeAtTheCursor) " and the iterator's remove" else ""
            cell(message) {
                val builder = newBuilder()
                val iterator = builder.open()
                repeat(returned) { val _ = iterator.next() }
                builderOp.function.invoke(builder)
                val live = builder.open().drain()

                if (removeAtTheCursor) {
                    val others = views.map { (_, other) -> builder.other() }
                    iterator.remove()
                    for (other in others) {
                        assertFailsWith<ConcurrentModificationException> { other.next() }
                    }
                }

                assertEquals(live.drop(returned), iterator.drain())
                assertFailsWith<NoSuchElementException> { iterator.next() }
                val expected = live.toMutableList().apply { if (removeAtTheCursor) removeAt(returned - 1) }
                assertEquals(expected.unordered(), builder.open().drain().unordered())
                assertEquals(expected.size, builder.reportedSize)
            }
        }
    }
}

val listIteratorOperations: List<IteratorOperation<MutableListIterator<Any>>> = listOf(
    IteratorOperation("previous()") { previous() },
    IteratorOperation("add()") { add("new") },
    IteratorOperation("add() then set()") { add("new"); assertFailsWith<IllegalStateException> { set("new") } },
    IteratorOperation("add() then remove()") { add("new"); assertFailsWith<IllegalStateException> { remove() } },
    IteratorOperation("set()", needsAnElement = true) { set("new") },
    IteratorOperation("set() twice", needsAnElement = true) { set("new"); set("newer") },
    IteratorOperation("remove()", needsAnElement = true) { remove() },
    IteratorOperation("remove() then set()", needsAnElement = true) { remove(); assertFailsWith<IllegalStateException> { set("new") } },
)
