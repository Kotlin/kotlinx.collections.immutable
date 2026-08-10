/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.property

import tests.IntWrapper

/**
 * Compares every representation of the same content against every other, in both directions.
 *
 * All four persistent map classes override `equals` with a `when` over the concrete types of the
 * other operand, and each branch reaches into that operand's trie directly. A branch that is
 * missing, or that unwraps the wrong side, is silent: the comparison quietly falls through to the
 * content-based `AbstractMap.equals` and still answers correctly most of the time. That is the shape
 * of #218, and half of these branches take a live *builder* as the operand, which the rest of the
 * harness never passes to anything.
 *
 * The unequal half of the matrix matters as much as the equal half. A branch that always returned
 * true would sail through a test that only ever compares equal maps.
 */
internal fun equalityFailures(
    entries: Map<IntWrapper, Value?>,
    different: Map<IntWrapper, Value?>
): List<PropertyFailure> {
    val failures = mutableListOf<PropertyFailure>()

    fun fail(property: String, operation: String, detail: String) {
        failures += PropertyFailure(property, operation, detail)
    }

    val same = representationsOf(entries)
    val other = representationsOf(different)

    for ((leftName, left) in same) {
        for ((rightName, right) in same) {
            if (left != right) {
                fail("equality.same", "$leftName == $rightName", "reported unequal for equal content")
            }
            if (left.hashCode() != right.hashCode()) {
                fail("equality.hashCode", "$leftName vs $rightName", "${left.hashCode()} against ${right.hashCode()}")
            }
        }
        for ((rightName, right) in other) {
            if (left == right) {
                fail("equality.different", "$leftName == $rightName", "reported equal for different content")
            }
            if (right == left) {
                fail("equality.different", "$rightName == $leftName", "reported equal for different content")
            }
        }
    }
    return failures
}

/**
 * The same content as every implementation that can hold it, including the two builders. A builder
 * is a `MutableMap`, so it is a legal operand of `equals` and the persistent classes dispatch on it
 * explicitly.
 */
private fun representationsOf(entries: Map<IntWrapper, Value?>): List<Pair<String, Map<IntWrapper, Value?>>> {
    val ordered = entries.map { it.key to it.value }
    return listOf(
        "hash" to buildHashMap(entries),
        "hashBuilder" to buildHashMap(entries).builder(),
        "ordered" to buildOrderedMap(ordered),
        "orderedBuilder" to buildOrderedMap(ordered).builder(),
        "linkedHashMap" to LinkedHashMap(entries),
        "hashMap" to HashMap(entries),
    )
}

/**
 * Content that differs from [entries] in a way no implementation may miss: one entry's value is
 * replaced. Returns `null` when [entries] is too small to change.
 */
internal fun differentContent(entries: Map<IntWrapper, Value?>): Map<IntWrapper, Value?>? {
    val key = entries.keys.firstOrNull() ?: return null
    val changed = LinkedHashMap(entries)
    changed[key] = Value(-7)
    return if (changed == entries) null else changed
}
