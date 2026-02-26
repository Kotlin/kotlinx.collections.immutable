/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.set

import kotlinx.collections.immutable.persistentHashSetOf
import tests.NForAlgorithmComplexity
import tests.distinctStringValues
import tests.stress.ExecutionTimeMeasuringTest
import tests.IntWrapper
import tests.stress.WrapperGenerator
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersistentHashSetBuilderTest : ExecutionTimeMeasuringTest() {
    @Test
    fun isEmptyTests() {
        val builder = persistentHashSetOf<Int>().builder()

        assertTrue(builder.isEmpty())

        val elementsToAdd = NForAlgorithmComplexity.O_NlogN

        repeat(times = elementsToAdd) { index ->
            builder.add(index)
            check(builder.isNotEmpty())
        }
        repeat(times = elementsToAdd - 1) { index ->
            builder.remove(index)
            check(builder.isNotEmpty())
        }
        builder.remove(elementsToAdd - 1)
        assertTrue(builder.isEmpty())
    }

    @Test
    fun sizeTests() {
        val builder = persistentHashSetOf<Int>().builder()

        assertTrue(builder.size == 0)

        val elementsToAdd = NForAlgorithmComplexity.O_NlogN

        repeat(times = elementsToAdd) { index ->
            builder.add(index)
            check(index + 1 == builder.size)

            builder.add(index)
            check(index + 1 == builder.size)
        }
        repeat(times = elementsToAdd) { index ->
            builder.remove(index)
            check(elementsToAdd - index - 1 == builder.size)

            builder.remove(index)
            check(elementsToAdd - index - 1 == builder.size)
        }
    }

    @Test
    fun storedElementsTests() {
        val builder = persistentHashSetOf<Int>().builder()
        assertTrue(builder.isEmpty())

        val mutableSet = mutableSetOf<Int>()

        val elementsToAdd = NForAlgorithmComplexity.O_NN

        repeat(times = elementsToAdd) {
            val element = Random.nextInt()
            mutableSet.add(element)
            builder.add(element)

            check(builder == mutableSet)
        }

        mutableSet.toMutableList().forEach { element ->
            mutableSet.remove(element)
            builder.remove(element)

            check(builder == mutableSet)
        }

        assertTrue(builder.isEmpty())
    }

    @Test
    fun iteratorTests() {
        val builder = persistentHashSetOf<Int>().builder()
        assertFalse(builder.iterator().hasNext())

        val mutableSet = mutableSetOf<Int>()

        val elementsToAdd = NForAlgorithmComplexity.O_NN

        repeat(times = elementsToAdd) {
            val element = Random.nextInt()
            mutableSet.add(element)
            builder.add(element)
        }

        var iterator = builder.iterator()
        mutableSet.toMutableList().forEach { element ->
            mutableSet.remove(element)

            var didRemove = false
            for (i in 0..1) {
                while (!didRemove && iterator.hasNext()) {
                    if (iterator.next() == element) {
                        iterator.remove()
                        didRemove = true
                        break
                    }
                }
                if (!didRemove) {
                    iterator = builder.iterator()
                }
            }
            check(didRemove)

            check(mutableSet.size == builder.size)
            check(mutableSet == builder)
        }

        assertTrue(builder.isEmpty())
    }

    @Test
    fun removeTests() {
        val builder = persistentHashSetOf<Int>().builder()

        val elementsToAdd = NForAlgorithmComplexity.O_NlogN

        repeat(times = elementsToAdd) { index ->
            builder.add(index)
        }
        repeat(times = elementsToAdd) { index ->
            check(elementsToAdd - index == builder.size)

            check(builder.contains(index))
            builder.remove(index)
            check(!builder.contains(index))
        }
    }

    @Test
    fun containsTests() {
        val builder = persistentHashSetOf<String>().builder()

        val elementsToAdd = NForAlgorithmComplexity.O_NNlogN

        val elements = distinctStringValues(elementsToAdd)
        repeat(times = elementsToAdd) { index ->
            builder.add(elements[index])

            for (i in 0..index) {
                check(builder.contains(elements[i]))
            }
        }
        repeat(times = elementsToAdd) { index ->
            for (i in elementsToAdd - 1 downTo index) {
                check(builder.contains(elements[i]))
            }

            builder.remove(elements[index])
        }
    }

    @Test
    fun addTests() {
        val builder = persistentHashSetOf<Int>().builder()

        val elementsToAdd = NForAlgorithmComplexity.O_NNlogN

        repeat(times = elementsToAdd) { index ->
            builder.add(index * 2)

            for (i in index downTo 0) {
                val element = i + index

                check(builder.contains(element))
                builder.remove(element)
                check(!builder.contains(element))
                check(!builder.contains(element + 1))
                builder.add(element + 1)
                check(builder.contains(element + 1))
            }
        }
        repeat(times = elementsToAdd) { index ->
            for (i in index until elementsToAdd ) {
                val element = elementsToAdd - index + i

                check(builder.contains(element))
                builder.remove(element)
                check(!builder.contains(element))
                check(!builder.contains(element - 1))
                builder.add(element - 1)
                check(builder.contains(element - 1))
            }

            builder.remove(elementsToAdd - 1)
        }
        assertTrue(builder.isEmpty())
    }

    @Test
    fun collisionTests() {
        val builder = persistentHashSetOf<IntWrapper>().builder()

        val elementsToAdd = NForAlgorithmComplexity.O_NlogN

        val numberOfDistinctHashCodes = elementsToAdd / 5   // should be less than `elementsToAdd`
        val eGen = WrapperGenerator<Int>(numberOfDistinctHashCodes)
        fun wrapper(element: Int): IntWrapper {
            return eGen.wrapper(element)
        }

        repeat(times = elementsToAdd) { index ->
            builder.add(wrapper(index))
            check(builder.contains(wrapper(index)))
            check(index + 1 == builder.size)

            builder.add(wrapper(index))
            check(index + 1 == builder.size)

            val collisions = eGen.wrappersByHashCode(wrapper(index).hashCode)
            check(collisions.contains(wrapper(index)))

            for (wrapper in collisions) {
                check(builder.contains(wrapper))
            }
        }
        repeat(times = elementsToAdd) { index ->
            val collisions = eGen.wrappersByHashCode(wrapper(index).hashCode)
            check(collisions.contains(wrapper(index)))

            if (!builder.contains(wrapper(index))) {
                for (wrapper in collisions) {
                    check(!builder.contains(wrapper))
                }
            } else {
                for (wrapper in collisions) {
                    check(builder.contains(wrapper))

                    val nonExistingElement = IntWrapper(wrapper.obj.inv(), wrapper.hashCode)
                    check(!builder.remove(nonExistingElement))
                    check(builder.contains(wrapper))
                    check(builder.remove(wrapper))
                    check(!builder.contains(wrapper))
                }
            }
        }
        assertTrue(builder.isEmpty())
    }

    @Test
    fun randomOperationsTests() {
        val setGen = mutableListOf(List(20) { persistentHashSetOf<IntWrapper>() })
        val expected = mutableListOf(List(20) { setOf<IntWrapper>() })

        repeat(times = 5) {

            val builders = setGen.last().map { it.builder() }
            val sets = builders.map { it.toMutableSet() }

            val operationCount = NForAlgorithmComplexity.O_NlogN

            val numberOfDistinctHashCodes = operationCount / 2  // less than `operationCount` to increase collision cases
            val hashCodes = List(numberOfDistinctHashCodes) { Random.nextInt() }

            repeat(times = operationCount) {
                val index = Random.nextInt(sets.size)
                val set = sets[index]
                val builder = builders[index]

                val shouldRemove = Random.nextDouble() < 0.3
                val shouldOperateOnExistingElement = set.isNotEmpty() && Random.nextDouble().let { if (shouldRemove) it < 0.8 else it < 0.001 }

                val element = if (shouldOperateOnExistingElement) set.first() else IntWrapper(Random.nextInt(), hashCodes.random())

                when {
                    shouldRemove -> {
                        check(set.remove(element) == builder.remove(element))
                    }
                    else -> {
                        check(set.add(element) == builder.add(element))
                    }
                }

                testAfterOperation(set, builder, element)
            }

            assertEquals(sets, builders)

            setGen.add( builders.map { it.build() } )
            expected.add(sets)

            val maxSize = builders.maxOf { it.size }
            println("Largest persistent set builder size: $maxSize")
        }

        setGen.forEachIndexed { genIndex, sets ->
            // assert that builders didn't modify persistent sets they were created from.
            sets.forEachIndexed { setIndex, set ->
                val expectedSet = expected[genIndex][setIndex]
                assertEquals(
                        expectedSet,
                        set,
                        message = "The persistent set of $genIndex generation was modified.\nExpected: $expectedSet\nActual: $set"
                )
            }
        }
    }

    private fun testAfterOperation(expected: Set<IntWrapper>, actual: Set<IntWrapper>, element: IntWrapper) {
        check(expected.size == actual.size)
        check(expected.contains(element) == actual.contains(element))
//        check(expected == actual)
    }
}