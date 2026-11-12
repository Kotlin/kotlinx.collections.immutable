/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.list

import kotlinx.collections.immutable.ImmutableList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ImmutableSubListEqualityTest {
    @Test
    fun `equal ranges of the same source require no element reads`() {
        val source = CountingList(List(100) { it })
        assertEquals(source.subList(10, 90), source.subList(10, 90))
        assertEquals(0, source.reads)
    }

    @Test
    fun `nested sublists of the same range require no element reads`() {
        val source = CountingList(List(100) { it })
        assertEquals(source.subList(10, 90).subList(10, 30), source.subList(20, 40))
        assertEquals(0, source.reads)
    }

    @Test
    fun `different ranges can contain equal elements`() {
        val source = CountingList(listOf(1, 2, 1, 2))
        assertEquals(source.subList(0, 2), source.subList(2, 4))
        assertNotEquals(source.subList(0, 2), source.subList(1, 3))
        assertNotEquals(source.subList(0, 2), source.subList(0, 3))
    }

    @Test
    fun `different sources compare by their elements`() {
        val first = CountingList(listOf(0, 1, 2, 3)).subList(1, 3)
        val second = CountingList(listOf(4, 1, 2, 5)).subList(1, 3)
        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertNotEquals(first, CountingList(listOf(0, 1, 3, 4)).subList(1, 3))
    }

    @Test
    fun `sublist equality remains symmetric with ordinary lists`() {
        val subList = CountingList(listOf(0, 1, 2, 3)).subList(1, 3)
        assertEquals<List<Int>>(listOf(1, 2), subList)
        assertEquals<List<Int>>(subList, listOf(1, 2))
        assertEquals(listOf(1, 2).hashCode(), subList.hashCode())
        assertNotEquals<Any?>(subList, null)
        assertNotEquals<Any>(subList, setOf(1, 2))
    }

    private class CountingList<E>(private val elements: List<E>) : AbstractList<E>(), ImmutableList<E> {
        var reads = 0
            private set

        override val size: Int get() = elements.size

        override fun get(index: Int): E {
            reads++
            return elements[index]
        }

        override fun subList(fromIndex: Int, toIndex: Int): ImmutableList<E> =
            super<ImmutableList>.subList(fromIndex, toIndex)
    }
}
