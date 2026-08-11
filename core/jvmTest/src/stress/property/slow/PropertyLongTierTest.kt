/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.property.slow

import tests.stress.property.HashProfile
import tests.stress.property.MapCase
import tests.stress.property.OperandRelation
import tests.stress.property.PropertyFailure
import tests.stress.property.SequenceCase
import tests.stress.property.generateMapCase
import tests.stress.property.generateSequenceCase
import tests.stress.property.generateUniverse
import tests.stress.property.maxShiftCases
import tests.stress.property.maxShiftUniverse
import tests.stress.property.runMapCase
import tests.stress.property.runOrderedCase
import tests.stress.property.runSequenceCase
import tests.stress.property.shrinkMapCase
import tests.stress.property.shrinkSequence
import tests.stress.property.wideUniverse
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.fail

/**
 * The long tier: many more seeds, bigger universes, and the full bottom-level sweep.
 *
 * It lives in `jvmTest` rather than in `commonTest` on purpose. Anything in `commonTest` is compiled
 * and linked into every test binary, including twenty Kotlin/Native targets, and a runtime filter
 * would not save that cost. The JVM is also the only place the library's own `assert` calls are
 * live — they are gated on `desiredAssertionStatus`, Gradle enables assertions by default, and on JS
 * and Wasm they compile to nothing — so this is where invariant-heavy work belongs.
 *
 * Exclude it with `-PjvmTestExcludes=tests.stress.property.slow.*`.
 */
class PropertyLongTierTest {

    private fun check(cases: List<MapCase>, run: (MapCase) -> List<PropertyFailure>) {
        for (case in cases) {
            if (run(case).isEmpty()) continue
            fail(shrinkMapCase(case) { run(it) }.report())
        }
    }

    private fun randomCases(seeds: Int, keysPerCluster: IntRange): List<MapCase> {
        val cases = mutableListOf<MapCase>()
        for (seed in 0..<seeds) {
            val random = Random(seed)
            val universe = generateUniverse(random, HashProfile.entries, keysPerCluster)
            for (relation in OperandRelation.entries) {
                cases += generateMapCase(random, relation, universe) ?: continue
            }
        }
        return cases
    }

    @Test
    fun hashMapOverTheWholeBottomLevel() {
        check(maxShiftCases(keyCount = 8)) { runMapCase(it, checkCanonicalShape = true) }
    }

    @Test
    fun hashMapOverManySeeds() {
        check(randomCases(seeds = 400, keysPerCluster = 2..7)) { runMapCase(it, checkCanonicalShape = true) }
    }

    @Test
    fun orderedMapOverManySeeds() {
        check(randomCases(seeds = 150, keysPerCluster = 2..7)) { runOrderedCase(it) }
    }

    @Test
    fun longMixedRunsOverAFullNode() {
        // The wide universe fills a root node to all 32 cells, which is the shape the other
        // universes never reach: they are at most fifteen keys and leave the node half empty.
        checkSequences(
            List(60) { seed ->
                generateSequenceCase(Random(seed), wideUniverse(), slots = 3, length = 80, origin = "wide/$seed")
            }
        )
    }

    @Test
    fun longMixedRunsOverTheDeepestLevel() {
        checkSequences(
            List(120) { seed ->
                generateSequenceCase(Random(seed), maxShiftUniverse(), slots = 4, length = 60, origin = "deep/$seed")
            }
        )
    }

    private fun checkSequences(cases: List<SequenceCase>) {
        for (case in cases) {
            if (runSequenceCase(case).isEmpty()) continue
            val (shrunk, failures) = shrinkSequence(case) { runSequenceCase(it) }
            fail("Property failed: ${failures.first()}\n\n${shrunk.render()}")
        }
    }
}
