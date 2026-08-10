/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.property

import kotlin.test.Test
import kotlin.test.fail

/**
 * The fast property tier for [kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap].
 *
 * Runs on every target on every build, and enumerates rather than samples: only four cells exist at
 * shift 30, so a failure here is deterministic rather than "eventually, given enough draws".
 *
 * The randomized sweeps live in the jvmTest long tier instead. Keeping a small copy of them here
 * would cost twenty Kotlin/Native links for a fraction of the seeds the long tier already runs.
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
}
