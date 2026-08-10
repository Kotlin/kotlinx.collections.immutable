/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.property

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.implementations.persistentOrderedMap.PersistentOrderedMap
import kotlinx.collections.immutable.implementations.persistentOrderedMap.PersistentOrderedMapBuilder
import tests.IntWrapper

/**
 * Runs one case through [PersistentOrderedMap], which is what `persistentMapOf` returns and
 * therefore what most callers get.
 *
 * It is a `PersistentHashMap<K, LinkedValue<V>>` with a doubly-linked chain laid over it, so it
 * inherits every trie invariant and adds one of its own that nothing else here can see. The model is
 * a `LinkedHashMap`, which pins the order as well as the contents: both keep an existing key in
 * place when its value is replaced, and append a new one at the end.
 */
internal fun runOrderedCase(case: MapCase): List<PropertyFailure> {
    val failures = mutableListOf<PropertyFailure>()
    val universe = case.keys

    fun fail(property: String, operation: String, detail: String) {
        failures += PropertyFailure(property, operation, detail)
    }

    fun verify(operation: String, actual: PersistentMap<IntWrapper, Value?>, expected: Map<IntWrapper, Value?>) {
        val ordered = actual.asOrdered()

        if (expected != actual) fail("model", operation, "expected $expected, got $actual")
        if (actual != expected) fail("model.reversed", operation, "expected $expected, got $actual")
        if (actual.size != expected.size) {
            fail("size", operation, "expected ${expected.size}, got ${actual.size}")
        }

        // The order is the whole reason this class exists, and it is invisible to every check above:
        // `Map.equals` is order-agnostic.
        val actualOrder = actual.keys.toList()
        val expectedOrder = expected.keys.toList()
        if (actualOrder != expectedOrder) {
            fail("order", operation, "expected $expectedOrder, got $actualOrder")
        }

        val problems = ordered.orderedViolations()
        if (problems.isNotEmpty()) {
            fail("chain", operation, problems.joinToString("; "))
        }

        for (key in universe) {
            if (actual.containsKey(key) != expected.containsKey(key)) {
                fail("containsKey", operation, "disagree on $key")
            }
            if (actual[key] != expected[key]) {
                fail("get", operation, "expected ${expected[key]} for $key, got ${actual[key]}")
            }
        }
        if (actual.hashCode() != expected.hashCode()) {
            fail("hashCode", operation, "expected ${expected.hashCode()}, got ${actual.hashCode()}")
        }

        // The same content as a hash map must compare equal both ways. `PersistentOrderedMap.equals`
        // dispatches over a `when` of concrete types, and a missing branch there is silent.
        val asHash = expected.entries.fold(PersistentHashMap.emptyOf<IntWrapper, Value?>()) { m, e ->
            m.putting(e.key, e.value)
        }
        if (ordered != asHash || asHash != ordered) {
            fail("crossFamily.equals", operation, "ordered and hash maps of the same content disagree")
        }
        if (ordered.hashCode() != asHash.hashCode()) {
            fail("crossFamily.hashCode", operation, "${ordered.hashCode()} against ${asHash.hashCode()}")
        }

        // A broken chain can answer everything above correctly and only show up a step later.
        for (key in universe) {
            val probed = actual.removing(key)
            val probeExpected = expected - key
            if (probeExpected != probed) {
                fail("probe.remove", operation, "removing $key gave $probed, expected $probeExpected")
            }
            if (probed.keys.toList() != probeExpected.keys.toList()) {
                fail("probe.order", operation, "removing $key gave ${probed.keys.toList()}")
            }
            val probeProblems = probed.asOrdered().orderedViolations()
            if (probeProblems.isNotEmpty()) {
                fail("probe.chain", operation, "after removing $key: ${probeProblems.joinToString("; ")}")
            }
        }
    }

    val leftMap = buildOrderedMap(case.left.map { it to case.leftValue(it) })
    val rightMap = buildOrderedMap(case.right.map { it to case.rightValue(it) })
    val leftModel = linkedModel(case.left.map { it to case.leftValue(it) })
    val rightModel = linkedModel(case.right.map { it to case.rightValue(it) })

    verify("build left", leftMap, leftModel)
    verify("build right", rightMap, rightModel)

    failures += iteratorFailures(
        "ordered builder",
        { entries -> buildOrderedMap(entries.map { it.key to it.value }).builder() },
        leftModel,
        // https://github.com/Kotlin/kotlinx.collections.immutable/issues/307
        checkSetValueKeepsIteratorValid = false
    )

    verify("left.puttingAll(right)", leftMap.puttingAll(rightMap), leftModel + rightModel)
    verify("right.puttingAll(left)", rightMap.puttingAll(leftMap), rightModel + leftModel)
    verify(
        "left.builder().putAll(right)",
        leftMap.builder().apply { putAll(rightMap) }.build(),
        leftModel + rightModel
    )
    verify("left.puttingAll(left)", leftMap.puttingAll(leftMap), leftModel)

    // A hash map as the argument: the ordered builder has no bulk override, so this goes per entry
    // and has to keep the chain right one insertion at a time. `puttingAll` is specified as one
    // `put` per mapping "in the specified map", so new keys land in the argument's iteration order,
    // and a hash map's order is unspecified — the expectation has to be read off the argument.
    val rightAsHash = case.right.fold(PersistentHashMap.emptyOf<IntWrapper, Value?>()) { m, k ->
        m.putting(k, case.rightValue(k))
    }
    verify(
        "left.puttingAll(hashMap)",
        leftMap.puttingAll(rightAsHash),
        linkedModel(leftModel.map { it.key to it.value } + rightAsHash.map { it.key to it.value })
    )

    for (key in case.right) {
        verify("left.putting($key)", leftMap.putting(key, case.rightValue(key)), leftModel + (key to case.rightValue(key)))
        verify("left.removing($key)", leftMap.removing(key), leftModel - key)

        // Two-arg remove matches the value structurally, unlike put's referential no-op check.
        // Nothing else in the harness touches it.
        val stored = leftModel[key]
        verify(
            "left.removing($key, stored)",
            leftMap.removing(key, stored),
            if (leftModel.containsKey(key)) leftModel - key else leftModel
        )
        verify("left.removing($key, other)", leftMap.removing(key, Value(-1)), leftModel)
    }

    // Removing a key and putting it back must move it to the end, not restore its old place.
    for (key in case.left) {
        val expected = linkedModel((leftModel - key).map { it.key to it.value } + listOf(key to case.leftValue(key)))
        verify("left.removing($key).putting($key)", leftMap.removing(key).putting(key, case.leftValue(key)), expected)
    }

    var minus: PersistentMap<IntWrapper, Value?> = leftMap
    for (key in case.right) minus = minus.removing(key)
    verify("left minus right keys", minus, leftModel - case.right.toSet())

    // The builder is checked between steps, not only through what `build()` returns: the chain has
    // to be sound while it is being mutated, and a builder holds it in its own fields.
    val stepwise = PersistentOrderedMapBuilder(leftMap)
    val stepModel = LinkedHashMap(leftModel)
    for (key in case.right) {
        stepwise[key] = case.rightValue(key)
        stepModel[key] = case.rightValue(key)
        val problems = stepwise.chain().problems
        if (problems.isNotEmpty()) {
            fail("builder.chain", "builder put $key", problems.joinToString("; "))
        }
        if (stepwise.keys.toList() != stepModel.keys.toList()) {
            fail("builder.order", "builder put $key", "got ${stepwise.keys.toList()}")
        }
    }
    verify("left.builder() stepwise", stepwise.build(), leftModel + rightModel)

    val removing = PersistentOrderedMapBuilder(leftMap)
    val removeModel = LinkedHashMap(leftModel)
    for (key in case.left) {
        removing.remove(key)
        removeModel.remove(key)
        val problems = removing.chain().problems
        if (problems.isNotEmpty()) {
            fail("builder.chain", "builder remove $key", problems.joinToString("; "))
        }
        if (removing.keys.toList() != removeModel.keys.toList()) {
            fail("builder.order", "builder remove $key", "got ${removing.keys.toList()}")
        }
    }

    verify("left after all operations", leftMap, leftModel)
    verify("right after all operations", rightMap, rightModel)

    return failures
}

internal fun buildOrderedMap(entries: List<Pair<IntWrapper, Value?>>): PersistentOrderedMap<IntWrapper, Value?> {
    var map = PersistentOrderedMap.emptyOf<IntWrapper, Value?>()
    for ((key, value) in entries) map = map.putting(key, value)
    return map
}

internal fun linkedModel(entries: List<Pair<IntWrapper, Value?>>): Map<IntWrapper, Value?> {
    val model = LinkedHashMap<IntWrapper, Value?>()
    for ((key, value) in entries) model[key] = value
    return model
}

@Suppress("UNCHECKED_CAST")
private fun PersistentMap<IntWrapper, Value?>.asOrdered(): PersistentOrderedMap<IntWrapper, Value?> =
    this as PersistentOrderedMap<IntWrapper, Value?>
