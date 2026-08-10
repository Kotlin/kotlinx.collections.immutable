/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.property

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import tests.IntWrapper

/**
 * A property violation, tagged with the property that failed.
 *
 * The tag is what keeps shrinking honest: a smaller case only counts as the same failure if it
 * breaks the same property, otherwise the shrinker drifts onto an unrelated defect and reports a
 * repro for something the original case never hit.
 */
internal class PropertyFailure(val property: String, val operation: String, val detail: String) {
    val signature: String get() = "$property@$operation"
    override fun toString(): String = "[$property] $operation: $detail"
}

/**
 * Runs one case through the persistent API, the builder API and a stdlib model, and reports every
 * property that does not hold. Returns an empty list when the case is clean.
 *
 * Deliberately not asserted, because all three are legitimately history dependent: the order of
 * entries inside a collision node, which of two equal-but-distinct key objects survives an
 * operation, and whether draining a map to empty yields the canonical empty instance.
 */
internal fun runMapCase(
    case: MapCase,
    checkCanonicalShape: Boolean = false,
    checkNoOpIdentity: Boolean = false
): List<PropertyFailure> {
    val failures = mutableListOf<PropertyFailure>()
    val universe = case.keys

    fun fail(property: String, operation: String, detail: String) {
        failures += PropertyFailure(property, operation, detail)
    }

    /** Every check that must hold of any map the library hands out. */
    fun verify(operation: String, actual: PersistentMap<IntWrapper, Value?>, expected: Map<IntWrapper, Value?>) {
        val hashMap = actual.asHash()

        // The stdlib side of these two comparisons iterates the persistent map from outside, so it
        // is an oracle independent of the trie. Comparing two persistent maps is not — that runs
        // `equalsWith`, which is a structural comparison, and it is checked separately below.
        if (expected != actual) fail("model", operation, "expected $expected, got $actual")
        if (actual != expected) fail("model.reversed", operation, "expected $expected, got $actual")

        if (actual.size != expected.size) {
            fail("size", operation, "expected ${expected.size}, got ${actual.size}")
        }

        val shape = hashMap.shape()
        // Size is kept outside the trie, so a correct trie can be paired with a wrong count and a
        // wrong trie with a correct count. Count the entries independently, every time.
        val problems = shape.violations(expectedSize = actual.size)
        if (problems.isNotEmpty()) {
            fail("structure", operation, problems.joinToString("; "))
        }

        val entries = actual.entries.toList()
        if (entries.size != expected.size) {
            fail("iteration.count", operation, "iterated ${entries.size} of ${expected.size}")
        }
        if (entries.map { it.key }.toSet() != expected.keys) {
            fail("iteration.content", operation, "iterated ${entries.map { it.key }}, expected ${expected.keys}")
        }
        if (entries.map { it.key }.distinct().size != entries.size) {
            fail("iteration.duplicates", operation, "iteration yielded a key twice: ${entries.map { it.key }}")
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

        // A malformed trie can answer every direct question correctly and only misbehave one
        // operation later — that is how #294 stayed invisible on the set side.
        for (key in universe) {
            val probed = actual.removing(key)
            val probeExpected = expected - key
            if (probeExpected != probed) {
                fail("probe.remove", operation, "removing $key gave $probed, expected $probeExpected")
            }
            if (probed.size != probeExpected.size) {
                fail("probe.size", operation, "removing $key gave size ${probed.size}, expected ${probeExpected.size}")
            }
        }

        if (checkCanonicalShape) {
            // Shape must be a function of content: rebuilt in a fixed order, the same entries must
            // produce the same trie. Map equality already compares `dataMap` and `nodeMap` at every
            // level, so comparing against the rebuilt map is that check — and it is what makes
            // `assertEquals` between two persistent maps sound in the first place. The digest costs
            // two strings per node, so it is built only to explain a failure.
            val canonical = buildHashMap(expected.entries.sortedBy { it.key }.associate { it.key to it.value })
            if (canonical != hashMap || hashMap != canonical) {
                fail(
                    "canonical.shape", operation,
                    "rebuilt trie differs\n  rebuilt: ${canonical.shape().digest()}\n  actual:  ${shape.digest()}"
                )
            }
        }
    }

    val leftMap = buildHashMap(case.left.associateWith { case.leftValue(it) })
    val rightMap = buildHashMap(case.right.associateWith { case.rightValue(it) })
    val leftModel = case.left.associateWith { case.leftValue(it) }
    val rightModel = case.right.associateWith { case.rightValue(it) }

    verify("build left", leftMap, leftModel)
    verify("build right", rightMap, rightModel)

    // Once per case rather than per operation: this builds six representations of the content.
    differentContent(leftModel)?.let { failures += equalityFailures(leftModel, it) }
    failures += iteratorFailures("hash builder", { buildHashMap(it).builder() }, leftModel, checkNoOpIdentity)

    // putAll is the map's only two-tree operation: the trie merge that #294 mis-dispatched and #300
    // resolved in the wrong direction. Its contract is that the argument's value wins.
    verify("left.puttingAll(right)", leftMap.puttingAll(rightMap), leftModel + rightModel)
    verify("right.puttingAll(left)", rightMap.puttingAll(leftMap), rightModel + leftModel)
    verify(
        "left.builder().putAll(right)",
        leftMap.builder().apply { putAll(rightMap) }.build(),
        leftModel + rightModel
    )
    verify(
        "right.builder().putAll(left)",
        rightMap.builder().apply { putAll(leftMap) }.build(),
        rightModel + leftModel
    )

    // Every two-tree walker has a `this === otherNode` shortcut that an independently generated
    // pair of trees essentially never reaches.
    verify("left.puttingAll(left)", leftMap.puttingAll(leftMap), leftModel)
    val selfBuilder = leftMap.builder()
    selfBuilder.putAll(selfBuilder)
    verify("builder.putAll(itself)", selfBuilder.build(), leftModel)

    // Passing an unbuilt builder as the argument is a distinct path: it is `build()`-ed as a side
    // effect, which rotates the ownership token mid-operation.
    verify(
        "left.builder().putAll(right.builder())",
        leftMap.builder().apply { putAll(rightMap.builder()) }.build(),
        leftModel + rightModel
    )

    if (checkNoOpIdentity) {
        // Putting a value instance back where it already sits changes nothing, so the map must be
        // returned unchanged. Off by default: the builder path breaks this inside a collision node,
        // because `mutableCollisionPut` lacks the referential short circuit `collisionPut` has.
        for (key in case.left) {
            val stored = leftMap[key]
            if (leftMap.putting(key, stored) !== leftMap) {
                fail("noop.identity", "putting($key, its own value)", "returned a different map")
            }
            if (leftMap.builder().apply { put(key, stored) }.build() !== leftMap) {
                fail("noop.identity.builder", "builder.put($key, its own value)", "returned a different map")
            }
        }
    }

    var minus: PersistentMap<IntWrapper, Value?> = leftMap
    for (key in case.right) minus = minus.removing(key)
    verify("left minus right keys", minus, leftModel - case.right.toSet())

    for (key in case.right) {
        verify("left.putting($key)", leftMap.putting(key, case.rightValue(key)), leftModel + (key to case.rightValue(key)))
        verify("left.removing($key)", leftMap.removing(key), leftModel - key)
    }

    // The persistent path and the builder path must agree on the same sequence of single-key work.
    val stepwise = leftMap.builder()
    for (key in case.right) stepwise[key] = case.rightValue(key)
    verify("left.builder() stepwise", stepwise.build(), leftModel + rightModel)

    // Prior versions must be untouched by everything above.
    verify("left after all operations", leftMap, leftModel)
    verify("right after all operations", rightMap, rightModel)

    return failures
}

internal fun buildHashMap(entries: Map<IntWrapper, Value?>): PersistentHashMap<IntWrapper, Value?> {
    var map = PersistentHashMap.emptyOf<IntWrapper, Value?>()
    for ((key, value) in entries) map = map.putting(key, value)
    return map
}

@Suppress("UNCHECKED_CAST")
private fun PersistentMap<IntWrapper, Value?>.asHash(): PersistentHashMap<IntWrapper, Value?> =
    this as PersistentHashMap<IntWrapper, Value?>
