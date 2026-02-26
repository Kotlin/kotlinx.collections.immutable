/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.map

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf
import tests.NForAlgorithmComplexity
import tests.distinctStringValues
import tests.remove
import tests.stress.ExecutionTimeMeasuringTest
import tests.IntWrapper
import tests.stress.WrapperGenerator
import kotlin.random.Random
import kotlin.test.*

class PersistentHashMapBuilderTest : ExecutionTimeMeasuringTest() {

    @Test
    fun isEmptyTests() {
        val builder = persistentHashMapOf<Int, String>().builder()

        assertTrue(builder.isEmpty())

        val elementsToAdd = NForAlgorithmComplexity.O_NlogN

        val values = distinctStringValues(elementsToAdd)
        repeat(times = elementsToAdd) { index ->
            builder[index] = values[index]
            check(builder.isNotEmpty())
        }
        repeat(times = elementsToAdd - 1) {
            builder.remove(builder.size - 1)
            check(builder.isNotEmpty())
        }
        builder.remove(builder.size - 1)
        assertTrue(builder.isEmpty())
    }

    @Test
    fun sizeTests() {
        val builder = persistentHashMapOf<Int, Int>().builder()

        assertTrue(builder.size == 0)

        val elementsToAdd = NForAlgorithmComplexity.O_NlogN

        repeat(times = elementsToAdd) { index ->
            check(builder.put(index, index) == null)
            check(index + 1 == builder.size)

            check(index == builder.put(index, 7))
            check(index + 1 == builder.size)

            check(7 == builder.put(index, index))
            check(index + 1 == builder.size)
        }
        repeat(times = elementsToAdd) { index ->
            check(index == builder.remove(index))
            check(elementsToAdd - index - 1 == builder.size)

            check(builder.remove(index) == null)
            check(elementsToAdd - index - 1 == builder.size)
        }
    }

    @Test
    fun keysValuesEntriesTests() {
        fun testProperties(expectedKeys: Set<Int>, actualBuilder: PersistentMap.Builder<Int, Int>) {
            val values = actualBuilder.values
            val keys = actualBuilder.keys
            val entries = actualBuilder.entries

            check(listOf(values.size, keys.size, entries.size).all { it == expectedKeys.size })

            check(expectedKeys == keys)
            check(keys.containsAll(values))

            entries.forEach { entry ->
                check(entry.key == entry.value)
                check(expectedKeys.contains(entry.key))
            }
        }

        val builder = persistentHashMapOf<Int, Int>().builder()
        assertTrue(builder.keys.isEmpty())
        assertTrue(builder.values.isEmpty())

        val elementsToAdd = NForAlgorithmComplexity.O_NNlogN

        val set = hashSetOf<Int>()
        repeat(times = elementsToAdd) {
            val key = Random.nextInt()
            set.add(key)
            builder[key] = key

            testProperties(set, builder)
        }

        set.toMutableSet().forEach { key ->
            set.remove(key)
            builder.remove(key)

            testProperties(set, builder)
        }
    }

    private fun testAfterRandomPut(block: (MutableMap<IntWrapper, Int>, PersistentMap<IntWrapper, Int>) -> Unit) {
        val elementsToAdd = NForAlgorithmComplexity.O_NNlogN

        var map = persistentHashMapOf<IntWrapper, Int>()
        val expected = hashMapOf<IntWrapper, Int>()

        repeat(times = elementsToAdd) {
            val keyValue = Random.nextInt()
            val keyHash = Random.nextInt(elementsToAdd) // to have collisions
            val key = IntWrapper(keyValue, keyHash)

            expected[key] = keyValue
            map = map.put(key, keyValue)

            val shouldTest = Random.nextDouble() < 0.1
            if (shouldTest) {
                block(expected, map)
            }
        }
    }

    @Test
    fun keysIteratorTests() {
        fun testKeysIterator(expected: MutableMap<IntWrapper, Int>, actual: PersistentMap.Builder<IntWrapper, Int>) {
            var expectedSize = actual.size
            while (expectedSize > 0) {
                check(expectedSize == actual.size)

                val iterator = actual.keys.iterator()
                repeat(expectedSize) {
                    check(iterator.hasNext())

                    val nextKey = iterator.next()
                    check(expected[nextKey] == actual[nextKey])

                    val shouldRemove = Random.nextDouble() < 0.2
                    if (shouldRemove) {
                        iterator.remove()
                        expectedSize--
                    }
                }
                check(!iterator.hasNext())
            }

            check(actual.isEmpty())
        }

        testAfterRandomPut { expected, map ->
            testKeysIterator(expected, map.builder())
        }
    }

    @Test
    fun valuesIteratorTests() {
        fun testValuesIterator(actual: PersistentMap.Builder<IntWrapper, Int>) {
            var expectedSize = actual.size
            while (expectedSize > 0) {
                check(expectedSize == actual.size)

                val iterator = actual.values.iterator()
                repeat(expectedSize) {
                    check(iterator.hasNext())

                    val _ = iterator.next()

                    val shouldRemove = Random.nextDouble() < 0.2
                    if (shouldRemove) {
                        iterator.remove()
                        expectedSize--
                    }
                }
                check(!iterator.hasNext())
            }

            check(actual.isEmpty())
        }

        testAfterRandomPut { _, map ->
            testValuesIterator(map.builder())
        }
    }

    @Test
    fun entriesIteratorTests() {
        fun testEntriesIterator(expected: MutableMap<IntWrapper, Int>, actual: PersistentMap.Builder<IntWrapper, Int>) {
            var expectedSize = actual.size
            while (expectedSize > 0) {
                check(expectedSize == actual.size)

                val iterator = actual.entries.iterator()
                repeat(expectedSize) {
                    check(iterator.hasNext())

                    val nextEntry = iterator.next()
                    check(expected[nextEntry.key] == actual[nextEntry.key])

                    val shouldUpdate = Random.nextDouble() < 0.1
                    if (shouldUpdate) {
                        val newValue = Random.nextInt()
                        check(expected.put(nextEntry.key, newValue) == nextEntry.setValue(newValue))
                    }
                    val shouldRemove = Random.nextDouble() < 0.2
                    if (shouldRemove) {
                        iterator.remove()
                        expectedSize--
                    }
                }
                check(!iterator.hasNext())
            }

            check(actual.isEmpty())
        }

        testAfterRandomPut { expected, map ->
            testEntriesIterator(expected.toMutableMap(), map.builder())
        }
    }

    @Test
    fun removeTests() {
        val builder = persistentHashMapOf<Int, String>().builder()

        val elementsToAdd = NForAlgorithmComplexity.O_NlogN

        val values = distinctStringValues(elementsToAdd)
        repeat(times = elementsToAdd) { index ->
            builder[index] = values[index]
        }
        repeat(times = elementsToAdd) { index ->
            check(elementsToAdd - index == builder.size)
            check(values[index] == builder[index])
            check(values[index] == builder.remove(index))
            check(builder[index] == null)
        }
    }

    @Test
    fun removeBuildTests() {
        val builder = persistentHashMapOf<IntWrapper, Int>().builder()

        val elementsToAddToBuilder = NForAlgorithmComplexity.O_NlogN

        val keyGen = WrapperGenerator<Int>(elementsToAddToBuilder)
        val expectedKeys = hashSetOf<IntWrapper>()

        repeat(times = elementsToAddToBuilder) {
            val keyValue = Random.nextInt()
            val key = keyGen.wrapper(keyValue)
            expectedKeys.add(key)
            builder[key] = keyValue
        }

        val elementsToRemoveFromBuilder = expectedKeys.size / 2
        expectedKeys.take(elementsToRemoveFromBuilder).forEach { key ->
            expectedKeys.remove(key)
            builder.remove(key)
        }

        var map = builder.build()

        val elementsToRemoveFromMap = expectedKeys.size / 2
        expectedKeys.take(elementsToRemoveFromMap).forEach { key ->
            expectedKeys.remove(key)
            map = map.remove(key)
        }
        assertEquals<Set<IntWrapper>>(expectedKeys, map.keys)

        val elementsToAddToMap = elementsToAddToBuilder - expectedKeys.size
        repeat(elementsToAddToMap) {
            val keyValue = Random.nextInt()
            val key = keyGen.wrapper(keyValue)
            expectedKeys.add(key)
            map = map.put(key, keyValue)
        }
        assertEquals<Set<IntWrapper>>(expectedKeys, map.keys)

        expectedKeys.toHashSet().forEach { key ->
            expectedKeys.remove(key)
            map = map.remove(key)
        }
        assertEquals<Set<IntWrapper>>(expectedKeys, map.keys)
    }

    @Test
    fun removeEntryTests() {
        val builder = persistentHashMapOf<Int, String>().builder()

        val elementsToAdd = NForAlgorithmComplexity.O_NlogN

        val values = distinctStringValues(elementsToAdd + 1)
        repeat(times = elementsToAdd) { index ->
            builder[index] = values[index]
        }
        repeat(times = elementsToAdd) { index ->
            check(elementsToAdd - index == builder.size)
            check(values[index] == builder[index])
            check(!builder.remove(index, values[index + 1]))
            check(values[index] == builder[index])
            check(builder.remove(index, values[index]))
            check(builder[index] == null)
        }
    }

    @Test
    fun getTests() {
        val builder = persistentHashMapOf<Int, String>().builder()

        val elementsToAdd = NForAlgorithmComplexity.O_NNlogN

        val values = distinctStringValues(elementsToAdd)
        repeat(times = elementsToAdd) { index ->
            builder[index] = values[index]

            for (i in 0..index) {
                check(values[i] == builder[i])
            }
        }
        repeat(times = elementsToAdd) { index ->
            for (i in elementsToAdd - 1 downTo index) {
                check(values[i] == builder[i])
            }

            builder.remove(index)
        }
    }

    @Test
    fun putTests() {
        val builder = persistentHashMapOf<Int, String>().builder()

        val elementsToAdd = NForAlgorithmComplexity.O_NNlogN

        val values = distinctStringValues(2 * elementsToAdd)
        repeat(times = elementsToAdd) { index ->
            check(builder.put(index, values[2 * index]) == null)

            for (i in 0..index) {
                val valueIndex = i + index

                check(values[valueIndex] == builder[i])
                check(values[valueIndex] == builder.put(i, values[valueIndex + 1]))
                check(values[valueIndex + 1] == builder[i])
            }
        }
        repeat(times = elementsToAdd) { index ->
            for (i in index until elementsToAdd) {
                val valueIndex = elementsToAdd - index + i

                check(values[valueIndex] == builder[i])
                check(values[valueIndex] == builder.put(i, values[valueIndex - 1]))
                check(values[valueIndex - 1] == builder[i])
            }

            builder.remove(index)
        }
        assertTrue(builder.isEmpty())
    }

    @Test
    fun collisionTests() {
        val builder = persistentHashMapOf<IntWrapper, Int>().builder()

        repeat(times = 2) { removeEntryPredicate ->

            val elementsToAdd = NForAlgorithmComplexity.O_NlogN

            val maxHashCode = elementsToAdd / 5    // should be less than `elementsToAdd`
            val keyGen = WrapperGenerator<Int>(maxHashCode)
            fun key(key: Int): IntWrapper {
                return keyGen.wrapper(key)
            }

            repeat(times = elementsToAdd) { index ->
                check(builder.put(key(index), Int.MIN_VALUE) == null)
                check(Int.MIN_VALUE == builder[key(index)])
                check(index + 1 == builder.size)

                check(Int.MIN_VALUE == builder.put(key(index), index))
                check(index + 1 == builder.size)

                val collisions = keyGen.wrappersByHashCode(key(index).hashCode)
                check(collisions.contains(key(index)))

                for (key in collisions) {
                    check(key.obj == builder[key])
                }
            }
            repeat(times = elementsToAdd) { index ->
                val collisions = keyGen.wrappersByHashCode(key(index).hashCode)
                check(collisions.contains(key(index)))

                if (builder[key(index)] == null) {
                    for (key in collisions) {
                        check(builder[key] == null)
                    }
                } else {
                    for (key in collisions) {
                        check(key.obj == builder[key])

                        if (removeEntryPredicate == 1) {
                            val nonExistingValue = Int.MIN_VALUE
                            check(!builder.remove(key, nonExistingValue))
                            check(key.obj == builder[key])

                            check(builder.remove(key, key.obj))
                        } else {
                            val nonExistingKey = IntWrapper(Int.MIN_VALUE, key.hashCode)
                            check(builder.remove(nonExistingKey) == null)
                            check(key.obj == builder[key])

                            check(key.obj == builder.remove(key))
                        }

                        check(builder[key] == null)
                    }
                }
            }
            assertTrue(builder.isEmpty())
        }
    }

    @Test
    fun randomOperationsTests() {
        val mapGen = mutableListOf(List(20) { persistentHashMapOf<IntWrapper, Int>() })
        val expected = mutableListOf(List(20) { mapOf<IntWrapper, Int>() })

        repeat(times = 5) {

            val builders = mapGen.last().map { it.builder() }
            val maps = builders.map { it.toMutableMap() }

            val operationCount = NForAlgorithmComplexity.O_NlogN

            val numberOfDistinctHashCodes = operationCount / 2  // less than `operationCount` to increase collision cases
            val hashCodes = List(numberOfDistinctHashCodes) { Random.nextInt() }

            repeat(times = operationCount) {
                val index = Random.nextInt(maps.size)
                val map = maps[index]
                val builder = builders[index]

                val shouldRemove = Random.nextDouble() < 0.3
                val shouldOperateOnExistingKey = map.isNotEmpty() && Random.nextDouble().let { if (shouldRemove) it < 0.8 else it < 0.2 }

                val key = if (shouldOperateOnExistingKey) map.keys.first() else IntWrapper(Random.nextInt(), hashCodes.random())

                val shouldRemoveByKey = shouldRemove && Random.nextBoolean()
                val shouldRemoveByKeyAndValue = shouldRemove && !shouldRemoveByKey

                when {
                    shouldRemoveByKey -> {
                        check(map.remove(key) == builder.remove(key))
                    }
                    shouldRemoveByKeyAndValue -> {
                        val shouldBeCurrentValue = Random.nextDouble() < 0.8
                        val value = if (shouldOperateOnExistingKey && shouldBeCurrentValue) map[key]!! else Random.nextInt()
                        check(map.remove(key, value) == builder.remove(key, value))
                    }
                    else -> {
                        val value = Random.nextInt()
                        check(map.put(key, value) == builder.put(key, value))
                    }
                }

                testAfterOperation(map, builder, key)
            }

            assertEquals(maps, builders)

            mapGen.add( builders.map { it.build() } )
            expected.add(maps)

            val maxSize = builders.maxOf { it.size }
            println("Largest persistent map builder size: $maxSize")
        }

        mapGen.forEachIndexed { genIndex, maps ->
            // assert that builders didn't modify persistent maps they were created from.
            maps.forEachIndexed { mapIndex, map ->
                val expectedMap = expected[genIndex][mapIndex]
                assertEquals(
                        expectedMap,
                        map,
                        message = "The persistent map of $genIndex generation was modified.\nExpected: $expectedMap\nActual: $map"
                )
            }
        }
    }

    private fun testAfterOperation(
            expected: Map<IntWrapper, Int>,
            actual: Map<IntWrapper, Int>,
            operationKey: IntWrapper
    ) {
        check(expected.size == actual.size)
        check(expected[operationKey] == actual[operationKey])
        check(expected.containsKey(operationKey) == actual.containsKey(operationKey))

//        check(expected.containsValue(operationKey.obj) == actual.containsValue(operationKey.obj))
//        check(expected.keys == actual.keys)
//        check(expected.values.sorted() == actual.values.sorted())
    }
}
