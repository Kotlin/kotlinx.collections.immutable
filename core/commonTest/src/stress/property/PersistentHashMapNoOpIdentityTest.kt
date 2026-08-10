/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.property

import kotlinx.collections.immutable.persistentHashMapOf
import tests.IntWrapper
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.fail

/**
 * Putting a value back where it already sits by identity changes nothing, so the map must come back
 * unchanged.
 *
 * The builder breaks this inside a collision node — see
 * [#304](https://github.com/Kotlin/kotlinx.collections.immutable/issues/304) — so the property is
 * off by default in [runMapCase] and the sweep below is ignored. Turn both on with the fix.
 */
class PersistentHashMapNoOpIdentityTest {

    @Test
    fun theMapItselfHoldsTheContract() {
        val colliding = IntWrapper(1, 7)
        val map = persistentHashMapOf(colliding to "A", IntWrapper(2, 7) to "B")

        assertSame(map, map.putting(colliding, map[colliding]!!))
    }

    @Test
    fun theBuilderHoldsItOutsideACollisionNode() {
        val key = IntWrapper(3, 3)
        val map = persistentHashMapOf(key to "C")

        assertSame(map, map.builder().apply { put(key, map[key]!!) }.build())
    }

    @Test
    @Ignore // https://github.com/Kotlin/kotlinx.collections.immutable/issues/304
    fun everyShapeHoldsIt() {
        for (case in maxShiftCases()) {
            val failures = runMapCase(case, checkNoOpIdentity = true)
            if (failures.isEmpty()) continue
            fail(shrinkMapCase(case) { runMapCase(it, checkNoOpIdentity = true) }.report())
        }
    }
}
