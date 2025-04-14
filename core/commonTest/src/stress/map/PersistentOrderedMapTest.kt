/*
 * Copyright 2016-2025 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.map

import kotlinx.collections.immutable.persistentMapOf
import kotlin.test.Test
import kotlin.test.assertEquals

class PersistentOrderedMapTest {

    @Test
    fun equalsTest() {
        val expected = persistentMapOf("a" to 1, "b" to 2, "c" to 3)
        val actual = persistentMapOf<String, Int>().put("a", 1).put("b", 2).put("c", 3)
        assertEquals(expected, actual)
    }
}