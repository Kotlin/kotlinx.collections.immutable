/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlinx.collections.immutable.*
import tests.assertTypeEquals
import kotlin.test.*

class ConversionsTest {

    /** An [Iterable] that is not a [Collection], to exercise the non-collection branches of `plus`/`minus`. */
    private fun <T> plainIterableOf(vararg values: T): Iterable<T> = object : Iterable<T> {
        override fun iterator(): Iterator<T> = values.iterator()
    }

    @Test
    fun `plus on a collection-typed reference appends an element and minus removes a single occurrence`() {
        val collection: PersistentCollection<Int> = persistentListOf(1, 2, 3, 2)

        assertEquals(listOf(1, 2, 3, 2, 4), (collection + 4).toList())
        assertEquals(listOf(1, 3, 2), (collection - 2).toList(), "only the first occurrence is removed")
        assertEquals(listOf(1, 2, 3, 2), collection.toList(), "original collection is unchanged")
    }

    @Test
    fun `plus on a collection-typed reference appends elements of a collection iterable array and sequence`() {
        val collection: PersistentCollection<Int> = persistentListOf(1, 2)

        assertEquals(listOf(1, 2, 3, 4), (collection + listOf(3, 4)).toList())
        assertEquals(listOf(1, 2, 3, 4), (collection + plainIterableOf(3, 4)).toList())
        assertEquals(listOf(1, 2, 3, 4), (collection + arrayOf(3, 4)).toList())
        assertEquals(listOf(1, 2, 3, 3), (collection + sequenceOf(3, 3)).toList(), "list-backed collection keeps duplicates")
    }

    @Test
    fun `minus on a collection-typed reference removes all occurrences of a collection iterable array and sequence`() {
        val collection: PersistentCollection<Int> = persistentListOf(1, 2, 3, 2)

        assertEquals(listOf(1, 3), (collection - listOf(2)).toList())
        assertEquals(listOf(1, 3), (collection - plainIterableOf(2)).toList())
        assertEquals(listOf(1, 2, 2), (collection - arrayOf(3)).toList())
        assertEquals(listOf(1), (collection - sequenceOf(2, 3)).toList())
    }

    @Test
    fun `plus and minus on a persistent list accept array and sequence arguments`() {
        val list = persistentListOf(1, 2, 3, 2)

        assertEquals(listOf(1, 2, 3, 2, 4, 5), list + arrayOf(4, 5))
        assertEquals(listOf(1, 2, 3, 2, 4, 5), list + sequenceOf(4, 5))
        assertEquals(listOf(1, 3), list - arrayOf(2))
        assertEquals(listOf(1), list - sequenceOf(2, 3))
        assertEquals(listOf(1, 2, 3, 2), list, "original list is unchanged")
    }

    @Test
    fun `plus and minus on a persistent set accept array and sequence arguments`() {
        val set = persistentSetOf(1, 2, 3)

        assertEquals(listOf(1, 2, 3, 4), (set + arrayOf(2, 4)).toList(), "existing elements keep their position")
        assertEquals(listOf(1, 2, 3, 4), (set + sequenceOf(4, 1)).toList())
        assertEquals(listOf(1, 3), (set - arrayOf(2, 5)).toList())
        assertEquals(listOf(3), (set - sequenceOf(1, 2)).toList())
        assertEquals(listOf(1, 2, 3), set.toList(), "original set is unchanged")
    }

    @Test
    fun `intersect of a persistent collection with an iterable returns a persistent set of common elements`() {
        val collection: PersistentCollection<Int> = persistentListOf(1, 2, 2, 3, 4)

        val intersection = collection intersect listOf(2, 4, 5)

        assertEquals(setOf(2, 4), intersection)
        assertEquals(listOf(2, 4), intersection.toList(), "encounter order is preserved")
    }

    @Test
    fun `plus on a persistent map accepts an array and a sequence of pairs and minus removes a key`() {
        val map: PersistentMap<String, Int> = persistentMapOf("a" to 1, "b" to 2)

        assertEquals(mapOf("a" to 1, "b" to 2, "c" to 3), map + arrayOf("c" to 3))
        assertEquals(mapOf("a" to 5, "b" to 2, "c" to 3), map + sequenceOf("c" to 3, "a" to 5), "the later pair wins")
        assertEquals(mapOf("b" to 2), map - "a")
        assertSame(map, map - "missing", "removing an absent key returns the same instance")
    }

    @Test
    fun `array and sequence convert to immutable and persistent lists preserving order`() {
        val expected = listOf(3, 1, 2, 1)

        assertEquals(expected, arrayOf(3, 1, 2, 1).toImmutableList())
        assertEquals(expected, sequenceOf(3, 1, 2, 1).toImmutableList())
        assertEquals(expected, arrayOf(3, 1, 2, 1).toPersistentList())
        assertEquals(expected, sequenceOf(3, 1, 2, 1).toPersistentList())
    }

    @Test
    fun `array and sequence convert to ordered sets deduplicating and preserving encounter order`() {
        assertEquals(listOf(3, 1, 2), arrayOf(3, 1, 3, 2, 1).toImmutableSet().toList())
        assertEquals(listOf(3, 1, 2), sequenceOf(3, 1, 3, 2, 1).toImmutableSet().toList())
        assertEquals(listOf(3, 1, 2), arrayOf(3, 1, 3, 2, 1).toPersistentSet().toList())
        assertEquals(listOf(3, 1, 2), sequenceOf(3, 1, 3, 2, 1).toPersistentSet().toList())
    }

    @Test
    fun `array and sequence convert to persistent hash sets dropping duplicates`() {
        val fromArray = arrayOf(3, 1, 3, 2, 1).toPersistentHashSet()
        val fromSequence = sequenceOf(3, 1, 3, 2, 1).toPersistentHashSet()

        assertEquals(setOf(1, 2, 3), fromArray)
        assertEquals(setOf(1, 2, 3), fromSequence)
        assertTypeEquals(persistentHashSetOf(1), fromArray)
        assertTypeEquals(persistentHashSetOf(1), fromSequence)
    }

    @Test
    fun `char sequence converts to sets of its characters`() {
        assertEquals(listOf('m', 'i', 's', 'p'), "mississippi".toImmutableSet().toList())
        assertEquals(listOf('m', 'i', 's', 'p'), "mississippi".toPersistentSet().toList())
        assertEquals(setOf('m', 'i', 's', 'p'), "mississippi".toPersistentHashSet())
    }

    @Test
    fun `minus and intersect on a persistent list and set accept plain iterables`() {
        assertEquals(listOf(1, 3), persistentListOf(1, 2, 3, 2) - plainIterableOf(2), "all occurrences are removed")
        assertEquals(listOf(1, 3), (persistentSetOf(1, 2, 3) - plainIterableOf(2, 5)).toList())
        assertEquals(setOf(2), persistentSetOf(1, 2, 3) intersect plainIterableOf(2, 5))
    }

    @Test
    fun `set converters return sets as is build builders and copy plain iterables`() {
        assertEquals(listOf(3, 1, 2), listOf(3, 1, 3, 2, 1).toImmutableSet().toList(), "plain iterable is copied")

        val ordered = persistentSetOf(1, 2)
        assertSame(ordered, ordered.toPersistentSet())

        val hashSet = persistentHashSetOf(1, 2)
        assertSame(hashSet, hashSet.toPersistentHashSet())

        val builder = hashSet.builder()
        builder.add(3)
        val built = builder.toPersistentHashSet()
        assertEquals(setOf(1, 2, 3), built)
        builder.add(4)
        assertEquals(setOf(1, 2, 3), built, "built set is not affected by further builder mutations")
    }

    @Test
    fun `toPersistentHashMap returns hash maps as is builds builders and copies ordinary maps`() {
        val hashMap = persistentHashMapOf("a" to 1, "b" to 2)
        assertSame(hashMap, hashMap.toPersistentHashMap())

        val builder = hashMap.builder()
        builder["c"] = 3
        val built = builder.toPersistentHashMap()
        assertEquals(mapOf("a" to 1, "b" to 2, "c" to 3), built)
        builder["d"] = 4
        assertEquals(mapOf("a" to 1, "b" to 2, "c" to 3), built, "built map is not affected by further builder mutations")

        val ordinary = mapOf("x" to 1, "y" to 2)
        val converted = ordinary.toPersistentHashMap()
        assertEquals(ordinary, converted)
        assertTypeEquals(hashMap, converted)
    }
}
