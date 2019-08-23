/*
 * Copyright 2016-2018 JetBrains s.r.o.
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

package kotlinx.collections.immutable.stressTests.immutableSet

import kotlinx.collections.immutable.stressTests.ObjectWrapper
import kotlinx.collections.immutable.stressTests.WrapperGenerator
import kotlinx.collections.immutable.persistentHashSetOf
import kotlin.random.Random
import kotlin.test.*


class PersistentHashSetTest {
    @Test
    fun isEmptyTests() {
        var set = persistentHashSetOf<Int>()

        assertTrue(set.isEmpty())
        assertFalse(set.add(0).isEmpty())

        val elementsToAdd = 1000000
        repeat(times = elementsToAdd) { index ->
            set = set.add(index)
            assertFalse(set.isEmpty())
        }
        repeat(times = elementsToAdd - 1) { index ->
            set = set.remove(index)
            assertFalse(set.isEmpty())
        }
        set = set.remove(elementsToAdd - 1)
        assertTrue(set.isEmpty())
    }

    @Test
    fun sizeTests() {
        var set = persistentHashSetOf<Int>()

        assertTrue(set.size == 0)
        assertEquals(1, set.add(1).size)

        val elementsToAdd = 100000
        repeat(times = elementsToAdd) { index ->
            set = set.add(index)
            assertEquals(index + 1, set.size)

            set = set.add(index)
            assertEquals(index + 1, set.size)
        }
        repeat(times = elementsToAdd) { index ->
            set = set.remove(index)
            assertEquals(elementsToAdd - index - 1, set.size)

            set = set.remove(index)
            assertEquals(elementsToAdd - index - 1, set.size)
        }
    }

    @Test
    fun storedElementsTests() {
        var set = persistentHashSetOf<Int>()
        assertTrue(set.isEmpty())

        val mutableSet = mutableSetOf<Int>()

        val elementsToAdd = 2000
        repeat(times = elementsToAdd) {
            val element = Random.nextInt()
            mutableSet.add(element)
            set = set.add(element)

            assertEquals(set.sorted(), mutableSet.sorted())
        }

        mutableSet.toMutableSet().forEach { element ->
            mutableSet.remove(element)
            set = set.remove(element)

            assertEquals(set.sorted(), mutableSet.sorted())
        }

        assertTrue(set.isEmpty())
    }

    @Test
    fun removeTests() {
        var set = persistentHashSetOf<Int>()
        assertTrue(set.add(0).remove(0).isEmpty())

        val elementsToAdd = 1000000
        repeat(times = elementsToAdd) { index ->
            set = set.add(index)
        }
        repeat(times = elementsToAdd) { index ->
            assertEquals(elementsToAdd - index, set.size)

            assertTrue(set.contains(index))
            set = set.remove(index)
            assertFalse(set.contains(index))
        }
    }

    @Test
    fun containsTests() {
        var set = persistentHashSetOf<String>()
        assertTrue(set.add("1").contains("1"))

        val elementsToAdd = 10000
        repeat(times = elementsToAdd) { index ->
            set = set.add(index.toString())

            for (i in 0..index) {
                assertTrue(set.contains(i.toString()))
            }
        }
        repeat(times = elementsToAdd) { index ->
            for (i in elementsToAdd - 1 downTo index) {
                assertTrue(set.contains(i.toString()))
            }

            set = set.remove(index.toString())
        }
    }

    @Test
    fun addTests() {
        var set = persistentHashSetOf<Int>()
        assertTrue(set.add(1).add(1).contains(1))

        val elementsToAdd = 5000
        repeat(times = elementsToAdd) { index ->
            set = set.add(index * 2)

            for (i in index downTo 0) {
                assertTrue(set.contains(i + index))
                set = set.remove(i + index)
                assertFalse(set.contains(i + index))
                assertFalse(set.contains(i + index + 1))
                set = set.add(i + index + 1)
                assertTrue(set.contains(i + index + 1))
            }
        }
        repeat(times = elementsToAdd) { index ->
            for (i in index until elementsToAdd ) {
                val expected = elementsToAdd - index + i

                assertTrue(set.contains(expected))
                assertFalse(set.contains(expected - 1))
                set = set.remove(expected)
                set = set.add(expected - 1)
                assertTrue(set.contains(expected - 1))
            }

            set = set.remove(elementsToAdd - 1)
        }
    }

    @Test
    fun collisionTests() {
        var set = persistentHashSetOf<ObjectWrapper<Int>>()

        assertTrue(set.add(ObjectWrapper(1, 1)).contains(ObjectWrapper(1, 1)))

        val eGen = WrapperGenerator<Int>(20000)
        fun wrapper(element: Int): ObjectWrapper<Int> {
            return eGen.wrapper(element)
        }

        val elementsToAdd = 100000   /// should be more than eGen.hashCodeUpperBound
        repeat(times = elementsToAdd) { index ->
            set = set.add(wrapper(index))
            assertTrue(set.contains(wrapper(index)))
            assertEquals(index + 1, set.size)

            set = set.add(wrapper(index))
            assertEquals(index + 1, set.size)

            val collisions = eGen.wrappersByHashCode(wrapper(index).hashCode)
            assertTrue(collisions.contains(wrapper(index)))

            for (key in collisions) {
                assertTrue(set.contains(wrapper(index)))
            }
        }
        repeat(times = elementsToAdd) { index ->
            val collisions = eGen.wrappersByHashCode(wrapper(index).hashCode)
            assertTrue(collisions.contains(wrapper(index)))

            if (!set.contains(wrapper(index))) {
                for (wrapper in collisions) {
                    assertFalse(set.contains(wrapper))
                }
            } else {
                for (wrapper in collisions) {
                    assertTrue(set.contains(wrapper))

                    val sameSet = set.remove(ObjectWrapper(wrapper.obj.inv(), wrapper.hashCode))
                    assertEquals(set.size, sameSet.size)
                    assertTrue(sameSet.contains(wrapper))

                    set = set.remove(wrapper)
                    assertFalse(set.contains(wrapper))
                }
            }
        }
        assertTrue(set.isEmpty())
    }

    @Test
    fun randomOperationsTests() {
        repeat(times = 1) {

            val mutableSets = List(10) { mutableSetOf<ObjectWrapper<Int>?>() }
            val immutableSets = MutableList(10) { persistentHashSetOf<ObjectWrapper<Int>?>() }

            val operationCount = 2000000
            val hashCodes = List(operationCount / 3) { Random.nextInt() }
            repeat(times = operationCount) {
                val index = Random.nextInt(mutableSets.size)
                val mutableSet = mutableSets[index]
                val immutableSet = immutableSets[index]

                val element = if (Random.nextDouble() < 0.001) {
                    null
                } else {
                    val hashCodeIndex = Random.nextInt(hashCodes.size)
                    ObjectWrapper(Random.nextInt(), hashCodes[hashCodeIndex])
                }

                val operationType = Random.nextDouble()

                val shouldRemove = operationType < 0.3

                val newImmutableSet = when {
                    shouldRemove -> {
                        mutableSet.remove(element)
                        immutableSet.remove(element)
                    }
                    else -> {
                        mutableSet.add(element)
                        immutableSet.add(element)
                    }
                }

                assertEquals(mutableSet.size, newImmutableSet.size)
                assertEquals(mutableSet.contains(element), newImmutableSet.contains(element))
//                assertEquals(mutableSet.sorted(), newImmutableSet.sorted())

                immutableSets[index] = newImmutableSet
            }

            println(mutableSets.maxBy { it.size }?.size)
            println(immutableSets.maxBy { it.size }?.size)

            mutableSets.forEachIndexed { index, mutableSet ->
                var immutableSet = immutableSets[index]

                for (element in mutableSet.toMutableSet()) {
                    mutableSet.remove(element)
                    immutableSet = immutableSet.remove(element)

                    assertEquals(mutableSet.size, immutableSet.size)
                    assertEquals(mutableSet.contains(element), immutableSet.contains(element))
//                    assertEquals(mutableSet.sorted(), newImmutableSet.sorted())
                }
            }
        }
    }
}