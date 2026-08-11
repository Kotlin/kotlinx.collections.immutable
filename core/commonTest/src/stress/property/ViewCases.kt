/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.property

import kotlinx.collections.immutable.PersistentMap
import tests.IntWrapper

/**
 * Drives the bulk operations of the `keys`, `values` and `entries` views of a builder.
 *
 * None of them is overridden, so each one runs the generic `AbstractMutableCollection` loop, which
 * removes through the view's own iterator. That means a whole `removeAll` is a run of
 * iterator removals with a path reset after each, on a trie that is being rewritten underneath - the
 * same machinery a single removal uses, but repeated, which is where it stops being obvious.
 */
internal fun viewFailures(
    label: String,
    builderOf: (Map<IntWrapper?, Value?>) -> PersistentMap.Builder<IntWrapper?, Value?>,
    validate: (PersistentMap<IntWrapper?, Value?>) -> List<String>,
    model: Map<IntWrapper?, Value?>,
    otherKeys: List<IntWrapper?>
): List<PropertyFailure> {
    val failures = mutableListOf<PropertyFailure>()
    if (model.isEmpty()) return failures

    fun check(
        operation: String,
        expected: Map<IntWrapper?, Value?>,
        mutate: (PersistentMap.Builder<IntWrapper?, Value?>) -> Unit
    ) {
        val builder = builderOf(model)
        mutate(builder)

        if (builder != expected || expected != builder) {
            failures += PropertyFailure("view", "$label $operation", "left $builder, expected $expected")
        }
        if (builder.size != expected.size) {
            failures += PropertyFailure("view.size", "$label $operation", "${builder.size}, expected ${expected.size}")
        }
        if (builder.keys.toList().size != expected.size) {
            failures += PropertyFailure("view.iteration", "$label $operation", "iterated ${builder.keys.toList()}")
        }
        val built = builder.build()
        if (built != expected) {
            failures += PropertyFailure("view.build", "$label $operation", "built $built, expected $expected")
        }
        val problems = validate(built)
        if (problems.isNotEmpty()) {
            failures += PropertyFailure("view.structure", "$label $operation", problems.joinToString("; "))
        }
    }

    val others = otherKeys.toSet()
    check("keys.removeAll", model.filterKeys { it !in others }) { it.keys.removeAll(others) }
    check("keys.retainAll", model.filterKeys { it in others }) { it.keys.retainAll(others) }
    check("keys.clear", emptyMap()) { it.keys.clear() }

    val firstKey = model.keys.first()
    check("keys.remove", model - firstKey) { val _ = it.keys.remove(firstKey) }

    // A value removal is only predictable when exactly one key carries that value. Values here are
    // derived from the key, so nulls repeat and nothing else does.
    val doomedValues = model.values.filter { value -> model.values.count { it == value } == 1 }.toSet()
    if (doomedValues.isNotEmpty()) {
        check("values.removeAll", model.filterValues { it !in doomedValues }) { it.values.removeAll(doomedValues) }
        check("values.retainAll", model.filterValues { it in doomedValues }) { it.values.retainAll(doomedValues) }
    }

    // Entry equality is key and value together, so an entry whose value has changed is a different
    // entry and must not be removed.
    val doomedEntries = LinkedHashMap(model.filterKeys { it in others }).entries
    check("entries.removeAll", model.filterKeys { it !in others }) { it.entries.removeAll(doomedEntries) }

    val restamped = LinkedHashMap<IntWrapper?, Value?>(model.mapValues { Value(-99) }).entries
    check("entries.removeAll of restamped entries", model) { it.entries.removeAll(restamped) }

    return failures
}
