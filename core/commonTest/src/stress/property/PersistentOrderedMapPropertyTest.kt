/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.property

import kotlin.test.Test
import kotlin.test.fail

/**
 * The fast property tier for
 * [kotlinx.collections.immutable.implementations.persistentOrderedMap.PersistentOrderedMap].
 *
 * This is what `persistentMapOf` returns, and until now it had no randomized coverage at all: the
 * whole `stress` package tests the hash family. It also never reaches the two-tree trie merge —
 * the ordered builder has no bulk override — so what is under test here is the chain, one insertion
 * at a time.
 */
class PersistentOrderedMapPropertyTest {

    private fun check(cases: List<MapCase>) {
        for (case in cases) {
            val failures = runOrderedCase(case)
            if (failures.isEmpty()) continue
            fail(shrinkMapCase(case) { runOrderedCase(it) }.report())
        }
    }

    @Test
    fun exhaustiveAtTheDeepestLevel() {
        // The ordered map wraps values in LinkedValue, so the trie underneath still has to survive
        // the collision shapes the hash map is swept over. The randomized sweeps are in the jvmTest
        // long tier.
        check(maxShiftCases(keyCount = 5))
    }
}
