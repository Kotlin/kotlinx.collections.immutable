/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.set

import kotlinx.collections.immutable.*
import kotlin.test.*

class SetFlavorsTest {

    @Test
    fun `factories and converters return expected sets`() {
        val elements = listOf('c', 'a', 'b', 'c')
        for (set in listOf(
            persistentOrderedSetOf('c', 'a', 'b', 'c'),
            elements.toPersistentOrderedSet(),
            elements.toTypedArray().toPersistentOrderedSet(),
            elements.asSequence().toPersistentOrderedSet(),
            "cabc".toPersistentOrderedSet()
        )) {
            assertEquals(listOf('c', 'a', 'b'), assertIs<PersistentOrderedSet<Char>>(set).toList())
        }
        for (set in listOf(
            persistentUnorderedSetOf('c', 'a', 'b', 'c'),
            elements.toPersistentUnorderedSet(),
            elements.toTypedArray().toPersistentUnorderedSet(),
            elements.asSequence().toPersistentUnorderedSet(),
            "cabc".toPersistentUnorderedSet()
        )) {
            assertEquals(setOf('c', 'a', 'b'), assertIs<PersistentUnorderedSet<Char>>(set))
        }
        assertEquals(emptySet(), assertIs<PersistentOrderedSet<Char>>(persistentOrderedSetOf<Char>()))
        assertEquals(emptySet(), assertIs<PersistentUnorderedSet<Char>>(persistentUnorderedSetOf<Char>()))
    }

    @Test
    fun `existing converters return expected sets`() {
        val elements = listOf('c', 'a', 'b', 'c')
        val ordered = assertIs<PersistentOrderedSet<Char>>(elements.toPersistentSet())
        assertEquals(listOf('c', 'a', 'b'), ordered.toList())

        for (set in listOf(ordered, elements.toPersistentUnorderedSet())) {
            assertSame(set, set.toPersistentSet())
            assertSame(set, set.builder().toPersistentSet())
        }
    }

    @Test
    fun `flavor converters return the receiver`() {
        val ordered = persistentOrderedSetOf('a', 'b')
        assertSame(ordered, ordered.toPersistentOrderedSet())
        assertSame(ordered, ordered.builder().toPersistentOrderedSet())
        val unordered = persistentUnorderedSetOf('a', 'b')
        assertSame(unordered, unordered.toPersistentUnorderedSet())
        assertSame(unordered, unordered.builder().toPersistentUnorderedSet())
    }

    @Test
    fun `flavor converters convert between flavors`() {
        val ordered = persistentOrderedSetOf('c', 'a', 'b')
        for (set in listOf(ordered, ordered.builder())) {
            assertEquals(setOf('c', 'a', 'b'), assertIs<PersistentUnorderedSet<Char>>(set.toPersistentUnorderedSet()))
        }
        val unordered = persistentUnorderedSetOf('c', 'a', 'b')
        for (set in listOf(unordered, unordered.builder())) {
            assertEquals(set.toList(), assertIs<PersistentOrderedSet<Char>>(set.toPersistentOrderedSet()).toList())
        }
    }

    @Test
    fun `operations keep the flavor of the receiver`() {
        val ordered: PersistentSet<Int> = persistentSetOf(1, 2)
        for (set in listOf(ordered) + resultsOfOperations(ordered)) {
            val _ = assertIs<PersistentOrderedSet<Int>>(set)
        }
        val unordered: PersistentSet<Int> = persistentUnorderedSetOf(1, 2)
        for (set in listOf(unordered) + resultsOfOperations(unordered)) {
            val _ = assertIs<PersistentUnorderedSet<Int>>(set)
        }
    }

    private fun resultsOfOperations(set: PersistentSet<Int>): List<PersistentSet<Int>> {
        val collection: PersistentCollection<Int> = set
        return listOf(
            set + 3, set + listOf(3), set + arrayOf(3), set + sequenceOf(3),
            set - 1, set - listOf(1), set - arrayOf(1), set - sequenceOf(1), set.removingAll { it == 1 },
            set intersect listOf(1), collection intersect listOf(1),
            set.mutate { it.add(3) }, set.builder().build(), set.cleared()
        )
    }

    @Test
    fun `flavor extensions declare the flavor of the receiver`() {
        val ordered = persistentOrderedSetOf(1, 2)
        val orderedResults: List<PersistentOrderedSet<Int>> = listOf(
            ordered + 3,
            ordered + listOf(3),
            ordered + arrayOf(3),
            ordered + sequenceOf(3),
            ordered - 1,
            ordered - listOf(1),
            ordered - arrayOf(1),
            ordered - sequenceOf(1),
            ordered.removingAll { it == 1 },
            ordered intersect listOf(1),
            ordered.mutate { it.add(3) },
            ordered.builder().build()
        )
        val unordered = persistentUnorderedSetOf(1, 2)
        val unorderedResults: List<PersistentUnorderedSet<Int>> = listOf(
            unordered + 3,
            unordered + listOf(3),
            unordered + arrayOf(3),
            unordered + sequenceOf(3),
            unordered - 1,
            unordered - listOf(1),
            unordered - arrayOf(1),
            unordered - sequenceOf(1),
            unordered.removingAll { it == 1 },
            unordered intersect listOf(1),
            unordered.mutate { it.add(3) },
            unordered.builder().build()
        )
        val expected = listOf(
            setOf(1, 2, 3), setOf(1, 2, 3), setOf(1, 2, 3), setOf(1, 2, 3),
            setOf(2), setOf(2), setOf(2), setOf(2), setOf(2),
            setOf(1), setOf(1, 2, 3), setOf(1, 2)
        )
        assertEquals(expected.map { it.toList() }, orderedResults.map { it.toList() })
        assertEquals(expected, unorderedResults)
    }
}
