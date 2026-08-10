/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.property

import tests.IntWrapper

/** A key no generated universe contains. Hash 0 puts it in the collision group null would join. */
private val ABSENT = IntWrapper(-1, 0)

/**
 * Exercises the builder iterators, which nothing else in the harness reaches.
 *
 * The op alphabet elsewhere cannot express "hold an iterator across a mutation", so the whole
 * `modCount` surface goes untested — and that surface already has one known defect, #304, where an
 * operation that changes nothing bumps the counter and invalidates a live iterator. The iterators
 * check the counter in `next()` only, so a spurious bump surfaces one step later.
 */
internal fun iteratorFailures(
    label: String,
    build: (Map<IntWrapper, Value?>) -> MutableMap<IntWrapper, Value?>,
    model: Map<IntWrapper, Value?>,
    checkNoOpKeepsIteratorValid: Boolean = false,
    checkSetValueKeepsIteratorValid: Boolean = true
): List<PropertyFailure> {
    val failures = mutableListOf<PropertyFailure>()

    fun fail(property: String, operation: String, detail: String) {
        failures += PropertyFailure(property, operation, detail)
    }

    if (model.size < 2) return failures

    // Removing every other entry through the iterator must leave exactly the others, and must not
    // invalidate the iterator that did the removing.
    run {
        val builder = build(model)
        val expected = LinkedHashMap(model)
        val iterator = builder.entries.iterator()
        var index = 0
        try {
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (index % 2 == 0) {
                    expected.remove(entry.key)
                    iterator.remove()
                }
                index++
            }
        } catch (e: ConcurrentModificationException) {
            fail("iterator.remove", "$label removing through the iterator", "invalidated itself: $e")
            return@run
        }
        if (builder != expected) {
            fail("iterator.remove", "$label removing through the iterator", "left $builder, expected $expected")
        }
    }

    // setValue is a value replacement, not a structural change, so it must not invalidate either.
    // The ordered builder breaks this — see #307 — so its caller turns the check off.
    if (checkSetValueKeepsIteratorValid) run {
        val builder = build(model)
        val expected = LinkedHashMap(model)
        try {
            for (entry in builder.entries) {
                val replacement = Value(entry.key.obj + 500)
                expected[entry.key] = replacement
                entry.setValue(replacement)
            }
        } catch (e: ConcurrentModificationException) {
            fail("iterator.setValue", "$label setValue during iteration", "invalidated the iterator: $e")
            return@run
        }
        if (builder != expected) {
            fail("iterator.setValue", "$label setValue during iteration", "left $builder, expected $expected")
        }
    }

    // A mutation that really changes the map must invalidate: that is what the counter is for.
    run {
        val builder = build(model)
        val iterator = builder.keys.iterator()
        val _ = iterator.next()
        builder[ABSENT] = Value(0)
        val invalidated = try {
            val _ = iterator.next()
            false
        } catch (e: ConcurrentModificationException) {
            true
        }
        if (!invalidated) {
            fail("iterator.comodification", "$label put a new key during iteration", "the iterator kept going")
        }
    }

    // The mirror of it: an operation that changes nothing must leave the iterator alone.
    for ((operation, mutate) in noOps(model, checkNoOpKeepsIteratorValid)) {
        val builder = build(model)
        val iterator = builder.keys.iterator()
        val _ = iterator.next()
        mutate(builder)
        try {
            while (iterator.hasNext()) { val _ = iterator.next() }
        } catch (e: ConcurrentModificationException) {
            fail("iterator.noOp", "$label $operation", "a no-op invalidated the iterator: $e")
        }
    }

    return failures
}

private fun noOps(
    model: Map<IntWrapper, Value?>,
    includeKnownBroken: Boolean
): List<Pair<String, (MutableMap<IntWrapper, Value?>) -> Unit>> {
    val ops = mutableListOf<Pair<String, (MutableMap<IntWrapper, Value?>) -> Unit>>(
        "remove of an absent key" to { it.remove(ABSENT) },
        "putAll of an empty map" to { it.putAll(emptyMap()) },
    )
    if (includeKnownBroken) {
        // Off by default: this is #304. `mutableCollisionPut` lacks the referential short circuit
        // `collisionPut` has, so re-putting a value instance bumps modCount inside a collision node.
        val key = model.keys.first()
        val value = model[key]
        ops += "put of a value already stored" to { it[key] = value }
    }
    return ops
}
