/*
 * Copyright 2016-2025 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.set

import kotlinx.collections.immutable.persistentSetOf
import kotlin.test.Test
import kotlin.test.assertEquals

class PersistentOrderedSetTest {

    @Test
    fun equalsTestFromGitHubIssue() {
        val set1 = persistentSetOf(-1, 0, 65536)  // test passes with 65535 third parameter
        val builder = set1.builder()

        assertEquals(set1, builder.build().toSet())
        assertEquals(set1, builder.build())

        val set2 = set1.remove(0)
        builder.remove(0)

        assertEquals(set2, builder.build().toSet())
        assertEquals(set2, builder.build())
    }
}