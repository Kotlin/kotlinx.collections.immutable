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

    @Test
    fun `flavor converters return the receiver`() {
        val ordered = persistentOrderedMapOf(1 to 10)
        assertSame(ordered, ordered.toPersistentOrderedMap())
        assertSame(ordered, ordered.builder().toPersistentOrderedMap())
        val unordered = persistentUnorderedMapOf(1 to 10)
        assertSame(unordered, unordered.toPersistentUnorderedMap())
        assertSame(unordered, unordered.builder().toPersistentUnorderedMap())
    }

    @Test
    fun `flavor converters convert between flavors`() {
        val source: Map<Int, Int> = linkedMapOf(3 to 30, 1 to 10)
        val ordered = source.toPersistentOrderedMap()
        for (map in listOf(ordered, ordered.builder())) {
            assertEquals(source, assertIs<PersistentUnorderedMap<Int, Int>>(map.toPersistentUnorderedMap()))
        }
        val unordered = source.toPersistentUnorderedMap()
        for (map in listOf(unordered, unordered.builder())) {
            assertEquals(map.toList(), assertIs<PersistentOrderedMap<Int, Int>>(map.toPersistentOrderedMap()).toList())
        }
    }

    @Test
    fun `operations keep the flavor of the receiver`() {
        val ordered: PersistentMap<Int, Int> = persistentMapOf(1 to 10, 2 to 20)
        for (map in listOf(ordered) + resultsOfOperations(ordered)) {
            val _ = assertIs<PersistentOrderedMap<Int, Int>>(map)
        }
        val unordered: PersistentMap<Int, Int> = persistentUnorderedMapOf(1 to 10, 2 to 20)
        for (map in listOf(unordered) + resultsOfOperations(unordered)) {
            val _ = assertIs<PersistentUnorderedMap<Int, Int>>(map)
        }
    }

    private fun resultsOfOperations(map: PersistentMap<Int, Int>): List<PersistentMap<Int, Int>> = listOf(
        map + (3 to 30), map + mapOf(3 to 30), map + listOf(3 to 30), map + arrayOf(3 to 30), map + sequenceOf(3 to 30),
        map.puttingAll(listOf(3 to 30)), map.puttingAll(arrayOf(3 to 30)), map.puttingAll(sequenceOf(3 to 30)),
        map - 1, map - listOf(1), map - arrayOf(1), map - sequenceOf(1), map.removing(1, 10),
        map.mutate { it[3] = 30 }, map.builder().build(), map.cleared()
    )

    @Test
    fun `flavor extensions declare the flavor of the receiver`() {
        val ordered = persistentOrderedMapOf(1 to 10, 2 to 20)
        val orderedResults: List<PersistentOrderedMap<Int, Int>> = listOf(
            ordered + (3 to 30),
            ordered + mapOf(3 to 30),
            ordered + listOf(3 to 30),
            ordered + arrayOf(3 to 30),
            ordered + sequenceOf(3 to 30),
            ordered.puttingAll(listOf(3 to 30)),
            ordered.puttingAll(arrayOf(3 to 30)),
            ordered.puttingAll(sequenceOf(3 to 30)),
            ordered - 1,
            ordered - listOf(1),
            ordered - arrayOf(1),
            ordered - sequenceOf(1),
            ordered.removing(1, 10),
            ordered.mutate { it[3] = 30 },
            ordered.builder().build()
        )
        val unordered = persistentUnorderedMapOf(1 to 10, 2 to 20)
        val unorderedResults: List<PersistentUnorderedMap<Int, Int>> = listOf(
            unordered + (3 to 30),
            unordered + mapOf(3 to 30),
            unordered + listOf(3 to 30),
            unordered + arrayOf(3 to 30),
            unordered + sequenceOf(3 to 30),
            unordered.puttingAll(listOf(3 to 30)),
            unordered.puttingAll(arrayOf(3 to 30)),
            unordered.puttingAll(sequenceOf(3 to 30)),
            unordered - 1,
            unordered - listOf(1),
            unordered - arrayOf(1),
            unordered - sequenceOf(1),
            unordered.removing(1, 10),
            unordered.mutate { it[3] = 30 },
            unordered.builder().build()
        )
        val with3 = mapOf(1 to 10, 2 to 20, 3 to 30)
        val without1 = mapOf(2 to 20)
        val expected = listOf(
            with3, with3, with3, with3, with3, with3, with3, with3,
            without1, without1, without1, without1, without1,
            with3, mapOf(1 to 10, 2 to 20)
        )
        assertEquals(expected.map { it.toList() }, orderedResults.map { it.toList() })
        assertEquals(expected, unorderedResults)
    }
}
