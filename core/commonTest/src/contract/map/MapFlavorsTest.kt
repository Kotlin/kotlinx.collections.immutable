/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.map

import kotlinx.collections.immutable.*
import kotlin.test.*

class MapFlavorsTest {

    @Test
    fun `factories and converters return expected maps`() {
        val source: Map<Int, Int> = linkedMapOf(3 to 30, 1 to 10)
        for (map in listOf(persistentOrderedMapOf(3 to 30, 1 to 10), source.toPersistentOrderedMap())) {
            assertEquals(source.toList(), assertIs<PersistentOrderedMap<Int, Int>>(map).toList())
        }
        for (map in listOf(persistentUnorderedMapOf(3 to 30, 1 to 10), source.toPersistentUnorderedMap())) {
            assertEquals(source, assertIs<PersistentUnorderedMap<Int, Int>>(map))
        }
        assertEquals(emptyMap(), assertIs<PersistentOrderedMap<Int, Int>>(persistentOrderedMapOf<Int, Int>()))
        assertEquals(emptyMap(), assertIs<PersistentUnorderedMap<Int, Int>>(persistentUnorderedMapOf<Int, Int>()))
    }

    @Test
    fun `existing converters return expected maps`() {
        val source: Map<Int, Int> = linkedMapOf(3 to 30, 1 to 10)
        val ordered = assertIs<PersistentOrderedMap<Int, Int>>(source.toPersistentMap())
        assertEquals(source.toList(), ordered.toList())

        for (map in listOf(ordered, source.toPersistentUnorderedMap())) {
            assertSame(map, map.toPersistentMap())
            assertSame(map, map.builder().toPersistentMap())
        }
    }
}
