/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.property

/** The outcome of shrinking: the smallest case still reproducing the failure, and that failure. */
internal class Shrunk(val case: MapCase, val failures: List<PropertyFailure>) {

    /** A ready-to-paste regression test, which is the only form in which a random failure is useful. */
    fun report(): String = buildString {
        appendLine("Property failed: ${failures.first()}")
        if (failures.size > 1) {
            appendLine("Also failing: ${failures.drop(1).map { it.signature }.distinct().joinToString()}")
        }
        appendLine()
        appendLine("Shrunk to ${case.left.size} + ${case.right.size} keys, salt ${case.valueSalt}:")
        appendLine(case.render())
    }
}

/**
 * Shrinks by deletion only, keeping a candidate when it still fails *the same property*.
 *
 * Tagging matters: without it the shrinker happily drifts onto an unrelated defect and prints a
 * repro for something the original case never hit. The step cap keeps a pathological case from
 * turning a test run into a search.
 */
internal fun shrinkMapCase(
    original: MapCase,
    maxSteps: Int = 400,
    run: (MapCase) -> List<PropertyFailure>
): Shrunk {
    val originalFailures = run(original)
    if (originalFailures.isEmpty()) return Shrunk(original, originalFailures)

    val target = originalFailures.first().signature
    var best = original
    var bestFailures = originalFailures
    var steps = 0

    fun accept(candidate: MapCase): Boolean {
        if (steps++ >= maxSteps) return false
        val failures = run(candidate)
        if (failures.none { it.signature == target }) return false
        best = candidate
        bestFailures = failures
        return true
    }

    var progress = true
    while (progress && steps < maxSteps) {
        progress = false

        for (i in best.left.indices) {
            if (accept(best.withLeft(best.left.without(i)))) {
                progress = true
                break
            }
        }
        if (progress) continue

        for (i in best.right.indices) {
            if (accept(best.withRight(best.right.without(i)))) {
                progress = true
                break
            }
        }
        if (progress) continue

        if (best.valueSalt != 0 && accept(best.withSalt(0))) progress = true
    }

    return Shrunk(best, bestFailures)
}

private fun <T> List<T>.without(index: Int): List<T> = filterIndexed { i, _ -> i != index }
