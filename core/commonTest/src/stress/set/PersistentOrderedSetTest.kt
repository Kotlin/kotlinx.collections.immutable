/*
 * Copyright 2016-2025 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.set

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlin.test.Test
import kotlin.test.assertEquals

class PersistentOrderedSetTest {

    @Test
    fun equalsTestFromGitHubIssue() {
        val set1: PersistentSet<Int> = persistentSetOf(-1, 0, 65536)  // test passes with 65535 third parameter
        val builder = set1.builder()

        assertEquals(set1, builder.build().toSet())
        assertEquals(set1, builder.build())

        val set2 = set1.remove(0)
        builder.remove(0)

        println("set2.equals(builder.build()): ${set2.equals(builder.build())}")

        assertEquals(set2, builder.build().toSet())
        assertEquals(set2, builder.build())
    }

    @Test
    fun equalsTestPersistentMap() {
        val map1 = persistentHashMapOf(-1 to "1", 0 to "0", 65536 to "65536")
        val builder = map1.builder()

        assertEquals(map1, builder.build().toMap())
        assertEquals(map1, builder.build())

        val map2 = map1.remove(0)
        builder.remove(0)
        val map3 = builder.build()

        println("map2.equals(map3): ${map2.equals(map3)}")

        assertEquals(map2, builder.build().toMap())
        assertEquals(map2, builder.build())
    }
}