/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlin.test.fail

/**
 * The cells of one table. Each cell runs on its own, and [runCells] then fails once, listing the first hundred failed
 * ones, each cut to 500 characters, with the first failure as the cause.
 */
class Cells {
    private var count = 0
    private val failures = mutableListOf<String>()
    private var firstFailure: Throwable? = null

    fun cell(name: String, body: () -> Unit) {
        count++
        try {
            body()
        } catch (e: Throwable) {
            failures += "$name: ${e.describe().take(500)}"
            if (firstFailure == null) firstFailure = e
        }
    }

    internal fun report() {
        if (count == 0) fail("The table ran no cell")
        if (failures.isEmpty()) return
        fail("${failures.size} of $count cells failed:\n" + failures.joinToString("\n", limit = 100), firstFailure)
    }
}

fun runCells(table: Cells.() -> Unit): Unit = Cells().apply(table).report()

/** Runs [body] with [position] in front of whatever it throws, so that the assertions inside need no message. */
fun at(position: String, body: () -> Unit) {
    try {
        body()
    } catch (e: Throwable) {
        throw AssertionError("$position: ${e.describe()}", e)
    }
}

/** The message of a failed assertion, and any other exception with its class. */
private fun Throwable.describe(): String = (this as? AssertionError)?.message ?: toString()

/** A row of a table of calls: a call on a receiver, under the name its cell takes. */
typealias Call<R> = Pair<String, R.() -> Any?>

/** A table of rows written as `"name" { ... }`, one per line; a row under an `if` or a `for` is added when reached. */
fun <T> rows(table: Rows<T>.() -> Unit): List<Pair<String, T>> = Rows<T>().apply(table).added

class Rows<T> {
    internal val added = mutableListOf<Pair<String, T>>()

    operator fun String.invoke(row: T) {
        added += this to row
    }
}
