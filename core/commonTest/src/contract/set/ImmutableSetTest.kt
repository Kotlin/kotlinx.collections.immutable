/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.set

import kotlinx.collections.immutable.*
import tests.contract.compare
import tests.contract.setBehavior
import tests.isDigit
import tests.isUpperCase
import tests.stress.IntWrapper
import kotlin.test.*

class ImmutableHashSetTest : ImmutableSetTestBase() {
    override fun <T> immutableSetOf(vararg elements: T) = persistentHashSetOf(*elements)
    override fun <T> testBuilderToPersistentSet(builder: PersistentSet.Builder<T>) {
        assertNotSame(builder.build(), builder.toPersistentSet(), "toPersistent shouldn't call build()")
    }

    @Test fun addAllElements() {
        run {
            val left = immutableSetOf<Int>() + (1..2000)
            assertSame(left, left.addAll(immutableSetOf()))
            compareSets(left, immutableSetOf<Int>().addAll(left))
        }

        run {
            val left = immutableSetOf<Int>() + (1..2000)
            val right = immutableSetOf<Int>() + (200..3000)
            compareSets(left.toSet() + right.toSet(), left.addAll(right))
        }

        run {
            val left = immutableSetOf<IntWrapper>() + (1..2000).map { IntWrapper(it, it % 200) }
            val right = immutableSetOf<IntWrapper>() + (200..3000).map { IntWrapper(it, it % 200) }
            compareSets(left.toSet() + right.toSet(), left.addAll(right))
        }

        run {
            val left = immutableSetOf<String>() + (1..2000).map { "$it" }
            val right = immutableSetOf<String>() + (200..3000).map { "$it" }
            compareSets(left.toSet() + right.toSet(), left.addAll(right))
        }

        run {
            val left = immutableSetOf<Int>() + (1..2000)
            assertSame(left, left + left)
            assertSame(left, left + immutableSetOf())
            val right = immutableSetOf<Int>() + (1..2000)
            assertSame(right, right + left)
            assertSame(left, left + right)
        }
    }

    @Test fun addAllElementsFromBuilder() {
        val builder1 = immutableSetOf<String>().builder()
        val builder2 = immutableSetOf<String>().builder()
        val expected = mutableSetOf<String>()
        for(i in 300..400) {
            builder1.add("$i")
            expected.add("$i")
        }
        for(i in 0..200) {
            builder2.add("$i")
            expected.add("$i")
        }
        builder1.addAll(builder2)

        // make sure we work with current state of builder2, not previous
        compareSets(expected, builder1)
        builder2.add("200")
        // make sure we can't modify builder1 through builder2
        compareSets(expected, builder1)
        // make sure builder builds correct map
        compareSets(expected, builder1.build())
    }

}
class ImmutableOrderedSetTest : ImmutableSetTestBase() {
    override fun <T> immutableSetOf(vararg elements: T) = persistentSetOf(*elements)
    override fun <T> compareSets(expected: Set<T>, actual: Set<T>) = compare(expected, actual) { setBehavior(ordered = true) }
    override fun <T> testBuilderToPersistentSet(builder: PersistentSet.Builder<T>) {
        assertSame(builder.build(), builder.toPersistentSet(), "toPersistent should call build()")
    }

    @Test fun elementHashCodeChanged() {
        val changing = mutableSetOf("ok")
        val persistent: PersistentSet<Any> = immutableSetOf("constant", changing, "fix")
        assertEquals(1, persistent.filter { it === changing }.size)
        changing.add("break iteration")
        assertFailsWith<ConcurrentModificationException> { persistent.filter { it === changing } }
    }

    @Test fun builderElementHashCodeChanged() {
        val changing = mutableSetOf("ok")
        val builder: PersistentSet.Builder<Any> = immutableSetOf<Any>().builder().apply {
            addAll(listOf("constant", changing, "fix"))
        }
        assertEquals(1, builder.filter { it === changing }.size)
        changing.add("break iteration")
        assertFailsWith<ConcurrentModificationException> { builder.filter { it === changing } }
    }
}

abstract class ImmutableSetTestBase {
    abstract fun <T> immutableSetOf(vararg elements: T): PersistentSet<T>
    abstract fun <T> testBuilderToPersistentSet(builder: PersistentSet.Builder<T>)

    fun <T> immutableSetOf(elements: Collection<T>) = immutableSetOf<T>() + elements

    open fun <T> compareSets(expected: Set<T>, actual: Set<T>) = compareSetsUnordered(expected, actual)
    fun <T> compareSetsUnordered(expected: Set<T>, actual: Set<T>) = compare(expected, actual) { setBehavior(ordered = false) }

    @Test open fun empty() {
        val empty1 = immutableSetOf<Int>()
        val empty2 = immutableSetOf<String>()
        assertEquals<ImmutableSet<Any>>(empty1, empty2)
        assertEquals<Set<Any>>(setOf(), empty1)
        assertTrue(empty1 === empty2)

        compareSets(emptySet(), empty1)
    }

    @Test fun ofElements() {
        val set0 = setOf("a", "d", 1, null)
        val set1 = immutableSetOf("a", "d", 1, null)
        val set2 = immutableSetOf("a", "d", 1, null)

        compareSets(set0, set1)
        compareSets(set1, set2)
    }

    @Test fun toImmutable() {
        val original = setOf("a", "bar", "cat", null)
        val immOriginal = immutableSetOf(original)
        compareSets(original, immOriginal)

        val hashSet = HashSet(original) // copy
        var immSet = immutableSetOf(hashSet)
        val immSet2 = immSet.toImmutableSet()
        assertTrue(immSet2 === immSet)

        compareSetsUnordered(original, immSet)
        compareSetsUnordered(hashSet, immSet)

        hashSet.remove("a")
        assertNotEquals<Set<*>>(hashSet, immSet)

        immSet = immSet.remove("a")
        compareSetsUnordered(hashSet, immSet)
    }

    @Test fun addElements() {
        var set = immutableSetOf<String>()
        set = set.add("x")
        set = set.addAll(set)
        set = set + "y"
        set += "z"
        set += arrayOf("1", "2").asIterable()
        compareSets("xyz12".map { it.toString() }.toSet(), set)
    }


    @Test fun removeElements() {
        val set = immutableSetOf("abcxyz12".toList())
        fun expectSet(content: String, set: ImmutableSet<Char>) {
            compareSets(content.toSet(), set)
        }

        expectSet("abcyz12", set.remove('x'))
        expectSet("abcyz12", set - 'x')
        expectSet("abcy12", set.removeAll(setOf('x', 'z')))
        expectSet("abcy12", set - setOf('x', 'z'))
        expectSet("abcxyz", set.removeAll { it.isDigit() })

        compareSets(emptySet(), set - set)
        compareSets(emptySet(), set.clear())
    }

    @Test fun builder() {
        val builder = immutableSetOf<Char>().builder()
        "abcxaxyz12".toCollection(builder)
        val set = builder.build()
        compareSets(set, builder)
        assertTrue(set === builder.build(), "Building the same set without modifications")

        val set2 = builder.toImmutableSet()
        assertTrue(set2 === set, "toImmutable calls build()")

        testBuilderToPersistentSet(builder)

        with(set) {
            testMutation { add('K') }
            testMutation { addAll("kotlin".toSet()) }
            testMutation { remove('x') }
            testMutation { removeAll(setOf('x', 'z')) }
            testMutation { removeAll { it.isDigit() } }
            testMutation { clear() }
            testMutation { retainAll("xyz".toSet()) }
            testMutation { retainAll { it.isDigit() } }
        }
    }

    fun <T> PersistentSet<T>.testMutation(operation: MutableSet<T>.() -> Unit) {
        val mutable = HashSet(this) as MutableSet<T>
        val builder = this.builder()

        operation(mutable)
        operation(builder)

        compareSetsUnordered(mutable, builder)
        compareSetsUnordered(mutable, builder.build())
    }

    @Test open fun noOperation() {
        immutableSetOf<Int>().testNoOperation({ clear() }, { clear() })

        val set = immutableSetOf("abcxyz12".toList())
        with(set) {
            testNoOperation({ add('a') }, { add('a') })
            testNoOperation({ addAll(listOf('a', 'b')) }, { addAll(listOf('a', 'b')) })
            testNoOperation({ remove('d') }, { remove('d') })
            testNoOperation({ removeAll(listOf('d', 'e')) }, { removeAll(listOf('d', 'e')) })
            testNoOperation({ removeAll { it.isUpperCase() } }, { removeAll { it.isUpperCase() } })
        }
    }

    fun <T> PersistentSet<T>.testNoOperation(persistent: PersistentSet<T>.() -> PersistentSet<T>, mutating: MutableSet<T>.() -> Unit) {
        val result = this.persistent()
        val buildResult = this.mutate(mutating)
        // Ensure non-mutating operations return the same instance
        assertTrue(this === result)
        assertTrue(this === buildResult)
    }

}