/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.list

import kotlinx.collections.immutable.*
import kotlinx.collections.immutable.implementations.immutableList.PersistentVectorBuilder
import tests.contract.*
import tests.*
import kotlin.test.*

class ImmutableListTest {

    private fun <T> compareLists(expected: List<T>, actual: List<T>) = compare(expected, actual) { listBehavior() }


    @Test fun empty() {
        val empty1 = persistentListOf<Int>()
        val empty2 = persistentListOf<String>()
        assertEquals<ImmutableList<Any>>(empty1, empty2)
        assertEquals<List<Any>>(listOf(), empty1)
        assertTrue(empty1 === empty2)

        assertFailsWith<NoSuchElementException> { empty1.iterator().next() }

        compareLists(emptyList<Int>(), empty1)

    }

    @Test fun ofElements() {
        val list0 = listOf("a", "d", 1, null)
        val list1 = persistentListOf("a", "d", 1, null)
        val list2 = persistentListOf("a", "d", 1, null)

        compareLists(list0, list1)
        assertEquals(list1, list2)
    }

    @Test fun toImmutable() {
        val original = listOf("a", "bar", "cat", null)

        val list = original.toMutableList() // copy
        var immList = list.toImmutableList()
        val immList2 = immList.toImmutableList()
        assertTrue(immList2 === immList)

        compareLists(original, immList)

        list.removeAt(0)
        assertNotEquals<List<*>>(list, immList)

        immList = immList.toPersistentList().removingAt(0)
        compareLists(list, immList)
    }

    @Test fun emptyListToPersistentList() {
        val empty = emptyList<Int>()
        val emptyPersistent = empty.toPersistentList()

        assertSame(emptyPersistent, empty.toPersistentList())
    }

    @Test fun addElements() {
        var list = persistentListOf<String>()
        list = list.adding("x")
        list = list.addingAt(0, "a")
        list = list.addingAll(list)
        list = list.addingAllAt(1, listOf("b", "c"))
        list = list + "y"
        list += "z"
        list += arrayOf("1", "2").asIterable()
        compareLists("abcxaxyz12".map { it.toString() }, list)
    }

    @Test fun replaceElements() {
        var list = "abcxaxab12".toImmutableList().toPersistentList()

        for (i in list.indices) {
            list = list.replacingAt(i, list[i] + i)
        }

        assertEquals("ace{e}gi9;", list.joinToString(""))
        assertFailsWith<IndexOutOfBoundsException> { list.replacingAt(-1, '0') }
        assertFailsWith<IndexOutOfBoundsException> { list.replacingAt(list.size + 1, '0') }
    }

    @Test fun removeElements() {
        val list = "abcxaxyz12".toImmutableList().toPersistentList()
        fun expectList(content: String, list: ImmutableList<Char>) {
            compareLists(content.toList(), list)
        }

        expectList("bcxaxyz12", list.removingAt(0))
        expectList("abcaxyz12", list.removing('x'))
        expectList("abcaxyz12", list - 'x')
        expectList("abcayz12", list.removingAll(listOf('x')))
        expectList("abcayz12", list - listOf('x'))
        expectList("abcxaxyz", list.removingAll { it.isDigit() })

        assertEquals(emptyList<Char>(), list - list)
        assertEquals(emptyList<Char>(), list.cleared())
    }

    @Test fun subList() {
        val list = "abcxaxyz12".toImmutableList()
        val subList = list.subList(2, 5) // 2, 3, 4
        compareLists(listOf('c', 'x', 'a'), subList)

        assertFailsWith<IndexOutOfBoundsException> { list.subList(-1, 2) }
        assertFailsWith<IndexOutOfBoundsException> { list.subList(0, list.size + 1) }
    }

    @Test fun builder() {
        val builder = persistentListOf<Char>().builder()
        "abcxaxyz12".toCollection(builder)
        val list = builder.build()
        assertEquals<List<*>>(list, builder)
        assertTrue(list === builder.build(), "Building the same list without modifications")

        val list2 = builder.toImmutableList()
        assertTrue(list2 === list, "toImmutable calls build()")

        with(list) {
            testMutation { add('K') }
            testMutation { add(0, 'K') }
            testMutation { addAll("kotlin".toList()) }
            testMutation { addAll(0, "kotlin".toList()) }
            testMutation { this[1] = this[1] + 2 }
            testMutation { removeAt(lastIndex) }
            testMutation { remove('x') }
            testMutation { removeAll(listOf('x')) }
            testMutation { removeAll { it.isDigit() } }
            testMutation { clear() }
            testMutation { retainAll("xyz".toList()) }
            testMutation { retainAll { it.isDigit() } }
        }
    }

    @Test fun subListOfBuilder() {
        val list = "abcxaxyz12".toImmutableList().toPersistentList()
        val builder = list.builder()

        val subList = builder.subList(2, 5)
        builder[4] = 'x'
        assertEquals("cxx", subList.joinToString(""))
        builder[4] = 'b'
        assertEquals("cxb", subList.joinToString(""))
        subList.removeAt(0)
        assertEquals("xb", subList.joinToString(""))
        assertEquals("abxbxyz12", builder.joinToString(""))
    }

    private val PersistentList.Builder<*>.modCount: Int get() = (this as PersistentVectorBuilder<*>).getModCount()

    /**
     * The builder's clear drops the root and the tail in one step: one modification, and the builder goes on from the
     * empty list.
     */
    @Test fun clearOfBuilder() = forEachBuilder(listBuilders) { builder ->
        val elements = builder.toList()
        val built = builder.build()
        val modCount = builder.modCount

        builder.clear()

        assertEquals(1, builder.modCount - modCount, "modifications counted by clear()")
        assertSame(persistentListOf(), builder.build(), "built after the clear")
        val newElements = List<Any>(elements.size) { "new $it" }
        for (element in newElements) builder.add(element)
        assertEquals(newElements, builder.build(), "built after as many adds")
        assertEquals(elements, built, "built before the clear")
    }

    fun <T> PersistentList<T>.testMutation(operation: MutableList<T>.() -> Unit) {
        val mutable = this.toMutableList()
        val builder = this.builder()

        operation(mutable)
        operation(builder)

        compareLists(mutable, builder)
        compareLists(mutable, builder.build())
    }

    @Test fun covariantTyping() {
        val listNothing = persistentListOf<Nothing>()

        val listS: PersistentList<String> = listNothing + "x"
        val listSN: PersistentList<String?> = listS + (null as String?)
        val listAny: PersistentList<Any?> = listSN + 1

        assertEquals<List<*>>(listOf("x", null, 1), listAny)
    }
}
