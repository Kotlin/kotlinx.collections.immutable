/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.property

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.fail

/**
 * The fast property tier for [kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap].
 *
 * Runs on every target on every build. The exhaustive part is the point: only four cells exist at
 * shift 30, so the bottom of the trie is enumerated rather than sampled, and a failure there is
 * deterministic rather than "eventually, given enough draws".
 */
class PersistentHashMapPropertyTest {

    private fun check(cases: List<MapCase>) {
        for (case in cases) {
            val failures = runMapCase(case, checkCanonicalShape = true)
            if (failures.isEmpty()) continue
            fail(shrinkMapCase(case) { runMapCase(it, checkCanonicalShape = true) }.report())
        }
    }

    @Test
    fun exhaustiveAtTheDeepestLevel() {
        check(maxShiftCases())
    }

    @Test
    fun everyOperandRelation() {
        val cases = mutableListOf<MapCase>()
        for (seed in 0..<40) {
            val random = Random(seed)
            val universe = generateUniverse(random)
            for (relation in OperandRelation.entries) {
                cases += generateMapCase(random, relation, universe) ?: continue
            }
        }
        check(cases)
    }

    @Test
    fun singleProfileUniverses() {
        val cases = mutableListOf<MapCase>()
        for (profile in HashProfile.entries) {
            for (seed in 0..<10) {
                val random = Random(seed)
                val universe = generateUniverse(random, listOf(profile, profile), 2..4)
                for (relation in OperandRelation.entries) {
                    cases += generateMapCase(random, relation, universe) ?: continue
                }
            }
        }
        check(cases)
    }
}
