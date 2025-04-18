/*
 * Copyright 2016-2025 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.set

import kotlinx.collections.immutable.persistentSetOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PersistentOrderedSetTest {

    /**
     * Test from issue: https://github.com/Kotlin/kotlinx.collections.immutable/issues/204
     */
    @Test
    fun persistentSetAndBuilderEqualityBeforeAndAfterModificationTest() {
        val set1 = persistentSetOf(-486539264, 16777216, 0, 67108864)
        val builder = set1.builder()

        assertTrue(set1.equals(builder))
        assertEquals(set1, builder.build())
        assertEquals(set1, builder.build().toSet())

        val set2 = set1.remove(0)
        builder.remove(0)

        assertEquals(set2, builder.build().toSet())
        assertEquals(set2, builder.build())
    }
}