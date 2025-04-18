/*
 * Copyright 2016-2025 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.set

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
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
        val map1 = persistentHashMapOf(-1 to "minus-one", 0 to "zero", 65536 to "power-of-two") as PersistentHashMap<Int, String>
        val builder = map1.builder()
        val map11 = builder.build()

        assertEquals(map1, map11.toMap())
        assertEquals(map1, map11)

        val map2 = map1.remove(0)
        builder.remove(0)
        val map22 = builder.build()

        val a = arrayOf(map1.node, map11.node, map2.node, map22.node)

        println("map2.equals(map22): ${map2.equals(map22)}")

        assertEquals(map2, map22.toMap())
        assertEquals(map2, map22)
    }
}