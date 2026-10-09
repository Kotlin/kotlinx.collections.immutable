/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/*
 * The MutableIterator contract of the builders, tested the way the standard library tests it in
 * libraries/stdlib/test/collections/ConcurrentModificationTest.kt: each test file writes a table of named calls it
 * makes through the builder, says of each whether it invalidates a live iterator, and hands the table to the two
 * functions below.
 */

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
    /**
     * The failure the call reports, given whether next returned an element and whether the builder changed.
     * null leaves it to the call, which must then do what it does on a builder nothing was called on.
     */
    val failure: (returned: Boolean, changed: Boolean) -> KClass<out Throwable>? =
        { _, changed -> ConcurrentModificationException::class.takeIf { changed } },
    val function: I.() -> Any?,
)

/** The failure of a call that needs an element returned by next: as in java.util, the state check comes first. */
val afterAMissingNext: (returned: Boolean, changed: Boolean) -> KClass<out Throwable>? = { returned, changed ->
    if (!returned) IllegalStateException::class else ConcurrentModificationException::class.takeIf { changed }
}

/** The whole MutableIterator surface, as stdlib's iteratorOperations. */
val iteratorOperations: List<IteratorOperation<MutableIterator<*>>> = listOf(
    IteratorOperation("next()") { next() },
    IteratorOperation("remove()", afterAMissingNext) { remove() },
    IteratorOperation("remove() twice", afterAMissingNext) { remove(); remove() },
)

/** The elements an iterator still has; a live map entry is read as its key and value the moment it is drained. */
private fun Iterator<*>.drain(): List<Any?> = asSequence().map { if (it is Map.Entry<*, *>) it.key to it.value else it }.toList()

/** The elements as a multiset: a remove from a hash trie may promote an element ahead of the ones already visited. */
private fun List<Any?>.unordered(): List<Any?> = sortedBy { it.toString() }

/**
 * Makes [builderOp] on a fresh builder of every origin in [builders], behind every iterator of [views], with 0 to
 * size elements returned, and checks what [iteratorOp] then does. The contract is java.util's: a structural call
 * makes next and remove throw ConcurrentModificationException; with nothing returned, remove reports
 * IllegalStateException first; a failed call leaves the iterator failed and the builder as the call left it; a
 * non-structural call changes nothing, so the iterator does what it does on an untouched builder (the control run
 * says what that is); hasNext never throws and answers from the iterator's own cursor, or from the builder's live
 * size when [hasNextFollowsTheSize]. A row costs builders x views x (size + 1) fresh builders per iterator operation.
 */
fun <C : Any, I : MutableIterator<*>> testIterator(
    builders: List<Pair<String, () -> C>>,
    views: List<Pair<String, C.() -> I>>,
    builderOp: BuilderOperation<C>,
    iteratorOp: IteratorOperation<I>,
    hasNextFollowsTheSize: Boolean = false,
) {
    for ((origin, newBuilder) in builders) for ((viewName, open) in views) {
        val size = newBuilder().open().drain().size
        for (returned in 0..size) {
            val message = "$origin, $viewName, ${iteratorOp.description} after $returned next, ${builderOp.description}"

            // what the call does when the builder operation was not made at all
            val control = newBuilder().open()
            repeat(returned) { val _ = control.next() }
            val onUntouched = runCatching { iteratorOp.function(control) }

            val builder = newBuilder()
            val iterator = builder.open()
            repeat(returned) { val _ = iterator.next() }
            builderOp.function.invoke(builder)

            // a cursor left beyond the live size still answers true, leaving the comodification for next to report
            val live = builder.open().drain()
            assertEquals(returned != (if (hasNextFollowsTheSize) live.size else size), iterator.hasNext(), message)

            val failure = iteratorOp.failure(returned > 0, builderOp.throwsCME) ?: onUntouched.exceptionOrNull()?.let { it::class }
            if (failure == null) {
                val _ = iteratorOp.function(iterator)
            } else {
                @Suppress("UNCHECKED_CAST")
                val _ = assertFailsWith(failure as KClass<Throwable>, message) { iteratorOp.function(iterator) }
                if (builderOp.throwsCME) {
                    // the failed call leaves the iterator failed and the builder as the operation left it
                    val _ = assertFailsWith<ConcurrentModificationException>(message) { iterator.next() }
                    assertEquals(live.unordered(), builder.open().drain().unordered(), message)
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
fun <C : Any> testIterationContinues(
    builders: List<Pair<String, () -> C>>,
    views: List<Pair<String, C.() -> MutableIterator<*>>>,
    builderOp: BuilderOperation<C>,
) {
    require(!builderOp.throwsCME) { "${builderOp.description} is structural" }
    for ((origin, newBuilder) in builders) for ((viewName, open) in views) {
        val size = newBuilder().open().drain().size
        for (returned in 0..size) for (removeAtTheCursor in listOf(false, true)) {
            if (removeAtTheCursor && returned == 0) continue
            val message = "$origin, $viewName after $returned next, ${builderOp.description}" +
                    if (removeAtTheCursor) " and the iterator's remove" else ""

            val builder = newBuilder()
            val iterator = builder.open()
            repeat(returned) { val _ = iterator.next() }
            builderOp.function.invoke(builder)
            val live = builder.open().drain()

            if (removeAtTheCursor) {
                val others = views.map { (_, other) -> builder.other() }
                iterator.remove()
                for (other in others) {
                    val _ = assertFailsWith<ConcurrentModificationException>(message) { other.next() }
                }
            }

            assertEquals(live.drop(returned), iterator.drain(), message)
            val expected = live.toMutableList().apply { if (removeAtTheCursor) removeAt(returned - 1) }
            assertEquals(expected.unordered(), builder.open().drain().unordered(), message)
        }
    }
}
