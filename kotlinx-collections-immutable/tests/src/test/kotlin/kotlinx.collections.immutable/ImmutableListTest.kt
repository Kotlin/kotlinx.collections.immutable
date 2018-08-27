package kotlinx.collections.immutable

import org.junit.Test
import test.collections.behaviors.listBehavior
import test.collections.compare
import kotlin.test.*

import kotlinx.collections.immutable.toPersistentList as toPersistentVectorList
import kotlinx.collections.immutable.toImmutableList as toImmutableVectorList


open class ImmutableListTest {

    private fun <T> compareLists(expected: List<T>, actual: List<T>) = compare(expected, actual) { listBehavior() }

    open fun <T> persistentListOf() = kotlinx.collections.immutable.persistentListOf<T>()
    open fun <T> persistentListOf(vararg elements: T) = kotlinx.collections.immutable.persistentListOf<T>(*elements)
    open fun <T> Iterable<T>.toPersistentList(): PersistentList<T> = toPersistentVectorList()
    open fun <T> Iterable<T>.toImmutableList(): ImmutableList<T> = toImmutableVectorList()


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
        var list = "abcxaxab12".toList().toImmutableList().toPersistentList()

        for (i in list.indices) {
            list = list.set(i, list[i] as Char + i)
        }

        assertEquals("ace{e}gi9;", list.joinToString(""))
        assertFailsWith<IndexOutOfBoundsException> { list.set(-1, '0') }
        assertFailsWith<IndexOutOfBoundsException> { list.set(list.size + 1, '0') }
    }

    @Test fun removeElements() {
        val list = "abcxaxyz12".toList().toImmutableList().toPersistentList()
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

    @Test fun subList() {
        val list = "abcxaxyz12".toList().toImmutableList()
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
        val list = "abcxaxyz12".toList().toImmutableList().toPersistentList()
        val builder = list.builder()
        val subList = builder.subList(2, 5)
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

        val list = "abcxaxyz12".toList().toPersistentList()
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

        assertEquals(listOf("x", null, 1), listAny)
    }
}

class ImmutableArrayListTest : ImmutableListTest() {
    override fun <T> persistentListOf(): PersistentList<T> = immutableArrayListOf()
    override fun <T> persistentListOf(vararg elements: T): PersistentList<T> = immutableArrayListOf(*elements)
    override fun <T> Iterable<T>.toPersistentList(): PersistentList<T> = toImmutableArrayList()
    override fun <T> Iterable<T>.toImmutableList(): ImmutableList<T> = toImmutableArrayList()
}