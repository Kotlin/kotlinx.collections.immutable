/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.set

import kotlinx.collections.immutable.persistentHashSetOf
import tests.NForAlgorithmComplexity
import tests.distinctStringValues
import tests.stress.ExecutionTimeMeasuringTest
import tests.stress.IntWrapper
import tests.stress.WrapperGenerator
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class PersistentHashSetTest : ExecutionTimeMeasuringTest() {
    @Test
    fun isEmptyTests() {
        var set = persistentHashSetOf<Int>()

        assertTrue(set.isEmpty())
        assertFalse(set.add(0).isEmpty())

        val elementsToAdd = NForAlgorithmComplexity.O_NlogN

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

        val elementsToAdd = NForAlgorithmComplexity.O_NlogN

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

        val elementsToAdd = NForAlgorithmComplexity.O_NN

        val mutableSet = hashSetOf<Int>()
        repeat(times = elementsToAdd) {
            val element = Random.nextInt()
            mutableSet.add(element)
            set = set.add(element)

            assertEquals<Set<*>>(set, mutableSet)
        }

        mutableSet.toMutableList().forEach { element ->
            mutableSet.remove(element)
            set = set.remove(element)

            assertEquals<Set<*>>(set, mutableSet)
        }

        assertTrue(set.isEmpty())
    }

    @Test
    fun removeTests() {
        var set = persistentHashSetOf<Int>()
        assertTrue(set.add(0).remove(0).isEmpty())

        val elementsToAdd = NForAlgorithmComplexity.O_NlogN

        repeat(times = elementsToAdd) { index ->
            set = set.add(index)
        }
        repeat(times = elementsToAdd) { index ->
            assertEquals(elementsToAdd - index, set.size)

            assertTrue(set.contains(index))
            set = set.remove(index)
            assertFalse(set.contains(index))
        }
        assertTrue(set.isEmpty())
    }

    @Test
    fun containsTests() {
        var set = persistentHashSetOf<String>()
        assertTrue(set.add("1").contains("1"))

        val elementsToAdd = NForAlgorithmComplexity.O_NNlogN

        val elements = distinctStringValues(elementsToAdd)
        repeat(times = elementsToAdd) { index ->
            set = set.add(elements[index])

            for (i in 0..index) {
                assertTrue(set.contains(elements[i]))
            }
        }
        repeat(times = elementsToAdd) { index ->
            for (i in elementsToAdd - 1 downTo index) {
                assertTrue(set.contains(elements[i]))
            }

            set = set.remove(elements[index])
        }
    }

    @Test
    fun addTests() {
        var set = persistentHashSetOf<Int>()
        assertTrue(set.add(1).add(1).contains(1))

        val elementsToAdd = NForAlgorithmComplexity.O_NNlogN

        repeat(times = elementsToAdd) { index ->
            set = set.add(index * 2)

            for (i in index downTo 0) {
                val element = i + index

                assertTrue(set.contains(element))
                set = set.remove(element)
                assertFalse(set.contains(element))
                assertFalse(set.contains(element + 1))
                set = set.add(element + 1)
                assertTrue(set.contains(element + 1))
            }
        }
        repeat(times = elementsToAdd) { index ->
            for (i in index until elementsToAdd ) {
                val element = elementsToAdd - index + i

                assertTrue(set.contains(element))
                set = set.remove(element)
                assertFalse(set.contains(element))
                assertFalse(set.contains(element - 1))
                set = set.add(element - 1)
                assertTrue(set.contains(element - 1))
            }

            set = set.remove(elementsToAdd - 1)
        }
        assertTrue(set.isEmpty())
    }

    @Test
    fun collisionTests() {
        var set = persistentHashSetOf<IntWrapper>()

        val oneWrapper = IntWrapper(1, 1)
        val twoWrapper = IntWrapper(2, 1)
        assertTrue(set.add(oneWrapper).add(twoWrapper).run { contains(oneWrapper) && contains(twoWrapper) })

        val elementsToAdd = NForAlgorithmComplexity.O_NlogN

        val numberOfDistinctHashCodes = elementsToAdd / 5   // should be less than `elementsToAdd`
        val eGen = WrapperGenerator<Int>(numberOfDistinctHashCodes)
        fun wrapper(element: Int): IntWrapper {
            return eGen.wrapper(element)
        }

        repeat(times = elementsToAdd) { index ->
            set = set.add(wrapper(index))
            assertTrue(set.contains(wrapper(index)))
            assertEquals(index + 1, set.size)

            set = set.add(wrapper(index))
            assertEquals(index + 1, set.size)

            val collisions = eGen.wrappersByHashCode(wrapper(index).hashCode)
            assertTrue(collisions.contains(wrapper(index)))

            for (wrapper in collisions) {
                assertTrue(set.contains(wrapper))
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

                    val nonExistingElement = IntWrapper(wrapper.obj.inv(), wrapper.hashCode)
                    val sameSet = set.remove(nonExistingElement)
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

            val mutableSets = List(10) { hashSetOf<IntWrapper?>() }
            val immutableSets = MutableList(10) { persistentHashSetOf<IntWrapper?>() }

            val operationCount = NForAlgorithmComplexity.O_NlogN

            val numberOfDistinctHashCodes = operationCount / 3  // less than `operationCount` to increase collision cases
            val hashCodes = List(numberOfDistinctHashCodes) { Random.nextInt() }

            repeat(times = operationCount) {
                val index = Random.nextInt(mutableSets.size)
                val mutableSet = mutableSets[index]
                val immutableSet = immutableSets[index]

                val shouldRemove = Random.nextDouble() < 0.1
                val shouldOperateOnExistingElement = mutableSet.isNotEmpty() && Random.nextDouble().let { if (shouldRemove) it < 0.8 else it < 0.001 }

                val element = when {
                    shouldOperateOnExistingElement -> mutableSet.first()
                    Random.nextDouble() < 0.001 -> null
                    else -> IntWrapper(Random.nextInt(), hashCodes.random())
                }

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

                testAfterOperation(mutableSet, newImmutableSet, element)

                immutableSets[index] = newImmutableSet
            }

            assertEquals<List<*>>(mutableSets, immutableSets)

            val maxSize = immutableSets.maxBy { it.size }?.size
            println("Largest persistent set size: $maxSize")

            mutableSets.forEachIndexed { index, mutableSet ->
                var immutableSet = immutableSets[index]

                val elements = mutableSet.toMutableList()
                for (element in elements) {
                    mutableSet.remove(element)
                    immutableSet = immutableSet.remove(element)

                    testAfterOperation(mutableSet, immutableSet, element)
                }
            }
        }
    }

    private fun testAfterOperation(expected: Set<IntWrapper?>, actual: Set<IntWrapper?>, element: IntWrapper?) {
        assertEquals(expected.size, actual.size)
        assertEquals(expected.contains(element), actual.contains(element))
//        assertEquals(expected, actual)
    }
}