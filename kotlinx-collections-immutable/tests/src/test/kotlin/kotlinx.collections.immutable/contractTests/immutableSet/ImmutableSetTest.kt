/*
 * Copyright 2016-2019 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.collections.immutable.contractTests.immutableSet

import kotlinx.collections.immutable.*
import kotlinx.collections.immutable.contractTests.compare
import kotlinx.collections.immutable.contractTests.setBehavior
import org.junit.Test
import kotlin.test.*

class ImmutableHashSetTest : ImmutableSetTestBase() {
    override fun <T> immutableSetOf(vararg elements: T) = persistentHashSetOf(*elements)
}
class ImmutableOrderedSetTest : ImmutableSetTestBase() {
    override fun <T> immutableSetOf(vararg elements: T) = persistentSetOf(*elements)
    override fun <T> compareSets(expected: Set<T>, actual: Set<T>) = compare(expected, actual) { setBehavior(ordered = true) }
}

abstract class ImmutableSetTestBase {

    abstract fun <T> immutableSetOf(vararg elements: T): PersistentSet<T>
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
        val set3 = builder.toPersistentSet()
        assertTrue(set3 === set, "toPersistent calls build()")

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