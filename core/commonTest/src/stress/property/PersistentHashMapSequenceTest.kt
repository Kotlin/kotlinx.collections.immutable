/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.property

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.fail

/**
 * Long mixed runs over a register of maps, as opposed to the fixed battery the other tiers apply to
 * a pair of them.
 *
 * What only shows up here is history. After a dozen mixed operations a map shares nodes with several
 * earlier versions and with the other slots, and every one of those has to still be exactly what it
 * was. A few seeds run everywhere; the wide universes and the long runs are in the jvmTest tier.
 */
class PersistentHashMapSequenceTest {

    private fun check(cases: List<SequenceCase>) {
        for (case in cases) {
            if (runSequenceCase(case).isEmpty()) continue
            val (shrunk, failures) = shrinkSequence(case) { runSequenceCase(it) }
            fail("Property failed: ${failures.first()}\n\n${shrunk.render()}")
        }
    }

    @Test
    fun mixedRunsOverTheDeepestLevel() {
        check(List(8) { seed -> generateSequenceCase(Random(seed), maxShiftUniverse(), origin = "maxShift/$seed") })
    }

    @Test
    fun mixedRunsOverANullKey() {
        check(List(8) { seed -> generateSequenceCase(Random(seed), nullKeyUniverse(), origin = "nullKey/$seed") })
    }
}
