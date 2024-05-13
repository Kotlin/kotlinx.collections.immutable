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

class PersistentHashSetBuilderTest : ExecutionTimeMeasuringTest() {
    @Test
    fun isEmptyTests() {
        val builder = persistentHashSetOf<Int>().builder()

        assertTrue(builder.isEmpty())

        val elementsToAdd = NForAlgorithmComplexity.O_NlogN

        repeat(times = elementsToAdd) { index ->
            builder.add(index)
            assertFalse(builder.isEmpty())
        }
        repeat(times = elementsToAdd - 1) { index ->
            builder.remove(index)
            assertFalse(builder.isEmpty())
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
            assertEquals(index + 1, builder.size)

            builder.add(index)
            assertEquals(index + 1, builder.size)
        }
        repeat(times = elementsToAdd) { index ->
            builder.remove(index)
            assertEquals(elementsToAdd - index - 1, builder.size)

            builder.remove(index)
            assertEquals(elementsToAdd - index - 1, builder.size)
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

            assertEquals(builder, mutableSet)
        }

        mutableSet.toMutableList().forEach { element ->
            mutableSet.remove(element)
            builder.remove(element)

            assertEquals(builder, mutableSet)
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
            assertTrue(didRemove)

            assertEquals(mutableSet.size, builder.size)
            assertEquals(mutableSet, builder)
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
            assertEquals(elementsToAdd - index, builder.size)

            assertTrue(builder.contains(index))
            builder.remove(index)
            assertFalse(builder.contains(index))
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
                assertTrue(builder.contains(elements[i]))
            }
        }
        repeat(times = elementsToAdd) { index ->
            for (i in elementsToAdd - 1 downTo index) {
                assertTrue(builder.contains(elements[i]))
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

                assertTrue(builder.contains(element))
                builder.remove(element)
                assertFalse(builder.contains(element))
                assertFalse(builder.contains(element + 1))
                builder.add(element + 1)
                assertTrue(builder.contains(element + 1))
            }
        }
        repeat(times = elementsToAdd) { index ->
            for (i in index until elementsToAdd ) {
                val element = elementsToAdd - index + i

                assertTrue(builder.contains(element))
                builder.remove(element)
                assertFalse(builder.contains(element))
                assertFalse(builder.contains(element - 1))
                builder.add(element - 1)
                assertTrue(builder.contains(element - 1))
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
            assertTrue(builder.contains(wrapper(index)))
            assertEquals(index + 1, builder.size)

            builder.add(wrapper(index))
            assertEquals(index + 1, builder.size)

            val collisions = eGen.wrappersByHashCode(wrapper(index).hashCode)
            assertTrue(collisions.contains(wrapper(index)))

            for (wrapper in collisions) {
                assertTrue(builder.contains(wrapper))
            }
        }
        repeat(times = elementsToAdd) { index ->
            val collisions = eGen.wrappersByHashCode(wrapper(index).hashCode)
            assertTrue(collisions.contains(wrapper(index)))

            if (!builder.contains(wrapper(index))) {
                for (wrapper in collisions) {
                    assertFalse(builder.contains(wrapper))
                }
            } else {
                for (wrapper in collisions) {
                    assertTrue(builder.contains(wrapper))

                    val nonExistingElement = IntWrapper(wrapper.obj.inv(), wrapper.hashCode)
                    assertFalse(builder.remove(nonExistingElement))
                    assertTrue(builder.contains(wrapper))
                    assertTrue(builder.remove(wrapper))
                    assertFalse(builder.contains(wrapper))
                }
            }
        }
        assertTrue(builder.isEmpty())
    }

    // This test is flaky and fails here: https://github.com/JetBrains/kotlin/blob/600e306d80382d9eaacbccbb28225778474a8741/libraries/stdlib/js/src/kotlin/collections/InternalHashMap.kt#L274
    // The reason of the stdlib exception is unclear. To determine what operations lead to the exception, we log them before test fails.
    @Test
    fun randomOperationsTests() = repeat(10) {
        val setGen = mutableListOf(List(20) { persistentHashSetOf<IntWrapper>() })
        val expected = mutableListOf(List(20) { setOf<IntWrapper>() })

        repeat(times = 5) {

            val builders = setGen.last().map { it.builder() }
            val sets = builders.map { it.toMutableSet() }
            val lastGenOperations = builders.map { builder ->
                StringBuilder(builder.size * 20).apply {
                    append("initial:")
                    builder.forEach { append("(${it.obj},${it.hashCode})") }
                    appendLine()
                }
            }

            val operationCount = NForAlgorithmComplexity.O_NlogN

            val numberOfDistinctHashCodes = operationCount / 2  // less than `operationCount` to increase collision cases
            val hashCodes = List(numberOfDistinctHashCodes) { Random.nextInt() }

            repeat(times = operationCount) {
                val index = Random.nextInt(sets.size)
                val set = sets[index]
                val builder = builders[index]
                val operations = lastGenOperations[index]

                val shouldRemove = Random.nextDouble() < 0.3
                val shouldOperateOnExistingElement = set.isNotEmpty() && Random.nextDouble().let { if (shouldRemove) it < 0.8 else it < 0.001 }

                val element = if (shouldOperateOnExistingElement) set.first() else IntWrapper(Random.nextInt(), hashCodes.random())

                try {
                    when {
                        shouldRemove -> {
                            operations.appendLine("r(${element.obj},${element.hashCode})")
                            assertEquals(set.remove(element), builder.remove(element))
                        }
                        else -> {
                            operations.appendLine("a(${element.obj},${element.hashCode})")
                            assertEquals(set.add(element), builder.add(element))
                        }
                    }

                    testAfterOperation(set, builder, element)
                } catch (e: Throwable) {
                    println(operations)
                    throw e
                }
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
        assertEquals(expected.size, actual.size)
        assertEquals(expected.contains(element), actual.contains(element))
//        assertEquals(expected, actual)
    }
}