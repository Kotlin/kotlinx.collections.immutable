/*
 * Copyright 2016-2026 JetBrains s.r.o.
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

        compareLists(emptyList<Int>(), empty1)

    }

    @Test
    fun persistentListFails() {
        var xs = persistentListOf(
                *(1..1885).map { it }.toTypedArray()
        )

        xs = xs.removingAll(
                (1..1837).map { it }
        )

        assertEquals((1838..1885).toList(), xs)
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

        for (size in listOf(40, 100, 1100)) {
            val nulls = List<Any?>(size) { null }.toPersistentList()
            for (index in listOf(-1, size, size + 31)) {
                assertFailsWith<IndexOutOfBoundsException>("size $size index $index") { nulls.replacingAt(index, null) }
                assertFailsWith<IndexOutOfBoundsException>("size $size index $index") { nulls.mutate { it[index] = null } }
            }
        }
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

    @Test
    fun `removing an aligned suffix preserves the retained prefix`() {
        for ((size, retainedSize) in listOf(65 to 32, 100 to 64)) {
            val elements = List(size) { it }
            val list = elements.toPersistentList()
            val expected = elements.take(retainedSize)
            val message = "size $size retaining $retainedSize"

            assertEquals(expected, list.removingAll(elements.drop(retainedSize)), message)
            assertEquals(expected, list.removingAll { it >= retainedSize }, message)
            assertEquals(expected, list.retainingAll(expected), message)
            assertEquals(elements, list, message)
        }
    }

    @Test
    fun smallPersistentListFromMutableBuffer() {
        val list = List(33) { it }
        var vector = persistentListOf<Int>().mutate { it.addAll(list) }
        vector = vector.removingAt(vector.lastIndex)
        assertEquals(list.dropLast(1), vector)
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

        // the first `set` copies the tail the builder does not own, the second one writes in place; neither is a structural change
        val subList = builder.subList(2, 5)
        builder[4] = 'x'
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

    private fun indices(size: Int) = listOf(0, 31, 32, 1023, 1024, size - 1).filter { it < size }.distinct()

    @Test fun noOperation() {
        persistentListOf<Int>().testNoOperation({ cleared() }, { clear() })

        val list = "abcxaxyz12".toPersistentList()
        with(list) {
            testNoOperation({ removing('d') }, { remove('d') })
            testNoOperation({ removingAll(listOf('d', 'e')) }, { removeAll(listOf('d', 'e')) })
            testNoOperation({ removingAll { it.isUpperCase() } }, { removeAll { it.isUpperCase() } })
            testNoOperation({ removingAll(emptyList()) }, { removeAll(emptyList())})
            testNoOperation({ addingAll(emptyList()) }, { addAll(emptyList())})
            testNoOperation({ addingAllAt(2, emptyList()) }, { addAll(2, emptyList())})
        }

        for (size in listOf(3, 40, 100, 1100)) {
            val wrappers = List(size) { IntWrapper(it, it) }.toPersistentList()
            for (index in indices(size)) {
                val element = wrappers[index]
                val equalElement = IntWrapper(index, index)
                val elementAtNextIndex = wrappers[(index + 1) % size]
                val message = "size $size index $index"

                wrappers.testNoOperation({ replacingAt(index, element) }, { this[index] = element }, message)
                wrappers.testNotNoOperation({ replacingAt(index, equalElement) }, { this[index] = equalElement }, message)
                wrappers.testNotNoOperation({ replacingAt(index, elementAtNextIndex) }, { this[index] = elementAtNextIndex }, message)
            }
        }

        val sameSlot = List(40) { IntWrapper(it, it) }.toPersistentList()
        sameSlot.testNotNoOperation({ replacingAt(39, this[7]) }, { this[39] = this[7] })
        sameSlot.testNotNoOperation({ replacingAt(7, this[39]) }, { this[7] = this[39] })
    }

    fun <T> PersistentList<T>.testNoOperation(persistent: PersistentList<T>.() -> PersistentList<T>, mutating: MutableList<T>.() -> Unit, message: String? = null) {
        val result = this.persistent()
        val buildResult = this.mutate(mutating)
        // Ensure non-mutating operations return the same instance
        assertSame(this, result, message)
        assertSame(this, buildResult, message)
    }

    fun <T> PersistentList<T>.testNotNoOperation(persistent: PersistentList<T>.() -> PersistentList<T>, mutating: MutableList<T>.() -> Unit, message: String? = null) {
        val result = this.persistent()
        val buildResult = this.mutate(mutating)
        // Ensure mutating operations do not return the same instance
        assertNotSame(this, result, message)
        assertNotSame(this, buildResult, message)
    }

    @Test fun replacingAtEqualButNotSameElement() {
        for (size in listOf(3, 40, 100, 1100)) {
            val list = List(size) { IntWrapper(it, it) }.toPersistentList()
            for (index in indices(size)) {
                val newElement = IntWrapper(index, index)
                val newList = list.replacingAt(index, newElement)
                assertNotSame(list, newList, "size $size index $index")
                assertEquals(list, newList, "size $size index $index")
                assertSame(newElement, newList[index], "size $size index $index")

                assertSame(newList, newList.replacingAt(index, newElement), "size $size index $index")
                assertSame(newList, newList.mutate { it[index] = newElement }, "size $size index $index")
            }
        }
    }

    @Test fun replacingAtNullElements() {
        val element = Any()
        for (size in listOf(3, 40, 100, 1100)) {
            val nulls = List<Any?>(size) { null }.toPersistentList()
            for (index in indices(size)) {
                val message = "size $size index $index"
                nulls.testNoOperation({ replacingAt(index, null) }, { this[index] = null }, message)

                val withElement = nulls.replacingAt(index, element)
                assertNotSame(nulls, withElement, message)
                assertSame(element, withElement[index], message)

                val builder = withElement.builder()
                assertSame(element, builder.set(index, null), message)
                assertNull(builder[index], message)
                assertNull(builder.set(index, null), message)

                val nullBuilder = nulls.builder()
                assertNull(nullBuilder.set(index, element), message)
                assertSame(element, nullBuilder[index], message)
            }
        }
    }

    @Test fun covariantTyping() {
        val listNothing = persistentListOf<Nothing>()

        val listS: PersistentList<String> = listNothing + "x"
        val listSN: PersistentList<String?> = listS + (null as String?)
        val listAny: PersistentList<Any?> = listSN + 1

        assertEquals<List<*>>(listOf("x", null, 1), listAny)
    }
}
