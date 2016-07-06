package kotlinx.collections.immutable

import org.junit.Test
import kotlin.test.*

class ImmutableListTest {

    @Test fun empty() {
        val empty1 = immutableListOf<Int>()
        val empty2 = immutableListOf<String>()
        assertEquals<ImmutableList<Any>>(empty1, empty2)
        assertEquals<List<Any>>(listOf(), empty1)
        assertTrue(empty1 === empty2)
    }

    @Test fun ofElements() {
        val list0 = listOf("a", "d", 1, null)
        val list1 = immutableListOf("a", "d", 1, null)
        val list2 = immutableListOf("a", "d", 1, null)

        assertEquals(list0, list1)
        assertEquals(list1, list2)
    }

    @Test fun toImmutable() {
        val original = listOf("a", "bar", "cat", null)

        val list = original.toMutableList() // copy
        var immList = list.toImmutable()
        val immList2 = immList.toImmutable()
        assertTrue(immList2 === immList)

        assertEquals<List<*>>(list, immList) // problem
        assertEquals(list.toString(), immList.toString())
        assertEquals(list.hashCode(), immList.hashCode())

        list.removeAt(0)
        assertNotEquals<List<*>>(list, immList)

        immList = immList.removeAt(0)
        assertEquals<List<*>>(list, immList) // problem
    }

    @Test fun addElements() {
        var list = immutableListOf<String>()
        list = list.add("x")
        list = list.add(0, "a")
        list = list.addAll(list)
        list = list.addAll(1, listOf("b", "c"))
        list = list + "y"
        list += "z"
        list += arrayOf("1", "2").asIterable()
        assertEquals("abcxaxyz12".map { it.toString() }, list)
    }

    @Test fun replaceElements() {
        var list = "abcxaxab12".toList().toImmutable()

        for (i in list.indices) {
            list = list.set(i, list[i] as Char + i)
        }

        assertEquals("ace{e}gi9;", list.joinToString(""))
        assertFailsWith<IndexOutOfBoundsException> { list.set(-1, '0') }
        assertFailsWith<IndexOutOfBoundsException> { list.set(list.size + 1, '0') }
    }

    @Test fun removeElements() {
        val list = "abcxaxyz12".toList().toImmutable()
        fun expectList(content: String, list: ImmutableList<Char>) {
            assertEquals(content, list.joinToString(""))
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

    @Test fun subList() {
        val list = "abcxaxyz12".toList().toImmutable()
        val subList = list.subList(2, 5) // 2, 3, 4
        assertTrue(subList is ImmutableList)
        assertEquals(listOf('c', 'x', 'a'), subList)

        assertFailsWith<IndexOutOfBoundsException> { list.subList(-1, 2) }
        assertFailsWith<IndexOutOfBoundsException> { list.subList(0, list.size + 1) }
    }

    @Test fun builder() {
        val builder = immutableListOf<Char>().builder()
        "abcxaxyz12".toCollection(builder)
        val list = builder.build()
        assertEquals<List<*>>(list, builder)
        assertTrue(list === builder.build(), "Building the same list without modifications")

        val list2 = builder.toImmutable()
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

    fun <T> ImmutableList<T>.testMutation(operation: MutableList<T>.() -> Unit) {
        val mutable = this.toMutableList()
        val builder = this.builder()

        operation(mutable)
        operation(builder)

        assertEquals(mutable, builder)
        assertEquals<List<*>>(mutable, builder.build())
    }



}