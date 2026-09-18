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
}
