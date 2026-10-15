/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail

/*
 * The MutableIterator contract of the builders, tested the way the standard library tests it in
 * libraries/stdlib/test/collections/ConcurrentModificationTest.kt: a table of named calls made through the builder
 * (Builders.kt), each saying whether it invalidates a live iterator, crossed with a table of calls made on the
 * iterator.
 */

/**
 * The cells of one table. Each cell runs on its own; the table then fails once, listing the first 100 failing cells
 * with the count of the rest, and keeps the first failure as the cause.
 */
class Cells {
    private var count = 0
    private val failures = mutableListOf<String>()
    private var firstFailure: Throwable? = null

    /** Runs [body] as one cell; [message] names the cell in the report. */
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

/** Runs the cells of [table], then fails once if any of them failed or if there were none. */
fun runCells(table: Cells.() -> Unit): Unit = Cells().apply(table).report()

/** A call made through the builder behind a live iterator, as stdlib's CollectionOperation. */
class BuilderOperation<in C>(
    val description: String,
    /** true when the call moves the size at least once, which invalidates every live iterator. */
    val throwsCME: Boolean = true,
    val function: C.() -> Unit,
)

/** A call made on the live iterator, as stdlib's IteratorOperation. */
class IteratorOperation<in I>(
    val description: String,
    /** true when the call needs an element returned by next, as remove and set do: with none, it throws IllegalStateException first, as in java.util. */
    val needsAnElement: Boolean = false,
    val function: I.() -> Any?,
)

/** The calls made on a live iterator, as stdlib's iteratorOperations. */
val iteratorOperations: List<IteratorOperation<MutableIterator<*>>> = listOf(
    IteratorOperation("next()") { next() },
)

/** The elements an iterator still has; a live map entry is read as its key and value the moment it is drained. */
internal fun Iterator<*>.drain(): List<Any?> = asSequence().map { if (it is Map.Entry<*, *>) it.key to it.value else it }.toList()

/** The elements as a multiset: a remove from a hash trie may promote an element ahead of the ones already visited. */
private fun List<Any?>.unordered(): List<Any?> = sortedBy { it.toString() }

/** The number of elements [open] gives on a fresh builder; if that throws, a failed cell named [message] says so and the result is null. */
private fun <C> Cells.sizeOf(message: String, newBuilder: () -> C, open: C.() -> Iterator<*>): Int? =
    try {
        newBuilder().open().drain().size
    } catch (e: Throwable) {
        cell(message) { throw e }
        null
    }

/**
 * Makes [builderOp] on a fresh builder of every origin in [builders], behind every iterator of [views], with 0 to
 * size elements returned, and checks what [iteratorOp] then does. The contract is java.util's: a structural call
 * makes next and remove throw ConcurrentModificationException; with nothing returned, remove reports
 * IllegalStateException first; a failed call leaves the iterator failed and the builder as the call left it; a
 * non-structural call changes nothing, so the iterator does what it does on an untouched builder (the control run
 * says what that is).
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
            val onUntouched = runCatching { iteratorOp.function(control) }

            val builder = newBuilder()
            val iterator = builder.open()
            repeat(returned) { val _ = iterator.next() }
            builderOp.function.invoke(builder)
            val live = builder.open().drain()

            val failure = when {
                iteratorOp.needsAnElement && returned == 0 -> IllegalStateException::class
                builderOp.throwsCME -> ConcurrentModificationException::class
                else -> onUntouched.exceptionOrNull()?.let { it::class }
            }
            if (failure == null) {
                val _ = iteratorOp.function(iterator)
            } else {
                @Suppress("UNCHECKED_CAST")
                assertFailsWith(failure as KClass<Throwable>) { iteratorOp.function(iterator) }
                if (builderOp.throwsCME) {
                    assertFailsWith<ConcurrentModificationException> { iterator.next() }
                    assertEquals(live.unordered(), builder.open().drain().unordered())
                }
            }
        }
    }
}

/**
 * Makes the non-structural [builderOp] on a fresh builder of every origin in [builders], behind every iterator of
 * [views], with 0 to size elements returned, and checks that the rest of the iteration is the builder's live
 * content from the cursor on, before and after the iterator's own remove, that the remove takes the returned
 * element out of the builder, and that it fails every other live iterator.
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
                val expected = live.toMutableList().apply { if (removeAtTheCursor) removeAt(returned - 1) }
                assertEquals(expected.unordered(), builder.open().drain().unordered())
            }
        }
    }
}
