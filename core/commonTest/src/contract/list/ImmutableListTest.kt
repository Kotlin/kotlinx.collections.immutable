/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.list

import kotlinx.collections.immutable.*
import tests.contract.compare
import tests.contract.listBehavior
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

        compareLists(emptyList(), empty1)

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

        immList = immList.toPersistentList().removeAt(0)
        compareLists(list, immList)
    }

    @Test fun addElements() {
        var list = persistentListOf<String>()
        list = list.add("x")
        list = list.add(0, "a")
        list = list.addAll(list)
        list = list.addAll(1, listOf("b", "c"))
        list = list + "y"
        list += "z"
        list += arrayOf("1", "2").asIterable()
        compareLists("abcxaxyz12".map { it.toString() }, list)
    }

    @Test fun replaceElements() {
        var list = "abcxaxab12".toImmutableList().toPersistentList()

        for (i in list.indices) {
            list = list.set(i, list[i] as Char + i)
        }

        assertEquals("ace{e}gi9;", list.joinToString(""))
        assertFailsWith<IndexOutOfBoundsException> { list.set(-1, '0') }
        assertFailsWith<IndexOutOfBoundsException> { list.set(list.size + 1, '0') }
    }

    @Test fun removeElements() {
        val list = "abcxaxyz12".toImmutableList().toPersistentList()
        fun expectList(content: String, list: ImmutableList<Char>) {
            compareLists(content.toList(), list)
        }

        expectList("bcxaxyz12", list.removeAt(0))
        expectList("abcaxyz12", list.remove('x'))
        expectList("abcaxyz12", list - 'x')
        expectList("abcayz12", list.removeAll(listOf('x')))
        expectList("abcayz12", list - listOf('x'))
        expectList("abcxaxyz", list.removeAll { it.isDigit() })

        assertEquals(emptyList<Char>(), list - list)
        assertEquals(emptyList<Char>(), list.clear())
    }

    @Test
    fun smallPersistentListFromMutableBuffer() {
        val list = List(33) { it }
        var vector = persistentListOf<Int>().mutate { it.addAll(list) }
        vector = vector.removeAt(vector.lastIndex)
        assertEquals(list.dropLast(1), vector)
    }

    @Test fun subList() {
        val list = "abcxaxyz12".toImmutableList()
        val subList = list.subList(2, 5) // 2, 3, 4
        assertTrue(subList is ImmutableList)
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
            testMutation { this[1] = this[1] as Char + 2 }
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

        // builder needs to recreate the inner trie to apply the modification.
        // So, structural changes in builder causes CME on subList iteration.
        var subList = builder.subList(2, 5)
        builder[4] = 'x'
        testOn(TestPlatform.JVM) {
            assertFailsWith<ConcurrentModificationException> { subList.joinToString("") }
        }

        // builder is the exclusive owner of the inner trie.
        // So, `set(index, value)` doesn't lead to structural changes.
        subList = builder.subList(2, 5)
        assertEquals("cxx", subList.joinToString(""))
        builder[4] = 'b'
        assertEquals("cxb", subList.joinToString(""))
        subList.removeAt(0)
        assertEquals("xb", subList.joinToString(""))
        assertEquals("abxbxyz12", builder.joinToString(""))
    }

    fun <T> PersistentList<T>.testMutation(operation: MutableList<T>.() -> Unit) {
        val mutable = this.toMutableList()
        val builder = this.builder()

        operation(mutable)
        operation(builder)

        compareLists(mutable, builder)
        compareLists(mutable, builder.build())
    }

    @Test fun noOperation() {
        persistentListOf<Int>().testNoOperation({ clear() }, { clear() })

        val list = "abcxaxyz12".toPersistentList()
        with(list) {
            testNoOperation({ remove('d') }, { remove('d') })
            testNoOperation({ removeAll(listOf('d', 'e')) }, { removeAll(listOf('d', 'e')) })
            testNoOperation({ removeAll { it.isUpperCase() } }, { removeAll { it.isUpperCase() } })
        }
    }

    fun <T> PersistentList<T>.testNoOperation(persistent: PersistentList<T>.() -> PersistentList<T>, mutating: MutableList<T>.() -> Unit) {
        val result = this.persistent()
        val buildResult = this.mutate(mutating)
        // Ensure non-mutating operations return the same instance
        assertTrue(this === result)
        assertTrue(this === buildResult)
    }

    @Test fun covariantTyping() {
        val listNothing = persistentListOf<Nothing>()

        val listS: PersistentList<String> = listNothing + "x"
        val listSN: PersistentList<String?> = listS + (null as String?)
        val listAny: PersistentList<Any?> = listSN + 1

        assertEquals<List<*>>(listOf("x", null, 1), listAny)
    }
}