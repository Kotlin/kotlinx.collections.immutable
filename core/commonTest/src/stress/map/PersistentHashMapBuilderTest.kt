/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.map

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.persistentHashMapOf
import tests.NForAlgorithmComplexity
import tests.distinctStringValues
import tests.remove
import tests.stress.ExecutionTimeMeasuringTest
import tests.stress.IntWrapper
import tests.stress.WrapperGenerator
import kotlin.collections.component1
import kotlin.collections.component2
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
            assertFalse(builder.isEmpty())
        }
        repeat(times = elementsToAdd - 1) {
            builder.remove(builder.size - 1)
            assertFalse(builder.isEmpty())
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
            assertNull(builder.put(index, index))
            assertEquals(index + 1, builder.size)

            assertEquals(index, builder.put(index, 7))
            assertEquals(index + 1, builder.size)

            assertEquals(7, builder.put(index, index))
            assertEquals(index + 1, builder.size)
        }
        repeat(times = elementsToAdd) { index ->
            assertEquals(index, builder.remove(index))
            assertEquals(elementsToAdd - index - 1, builder.size)

            assertNull(builder.remove(index))
            assertEquals(elementsToAdd - index - 1, builder.size)
        }
    }

    @Test
    fun keysValuesEntriesTests() {
        fun testProperties(expectedKeys: Set<Int>, actualBuilder: PersistentMap.Builder<Int, Int>) {
            val values = actualBuilder.values
            val keys = actualBuilder.keys
            val entries = actualBuilder.entries

            assertTrue(listOf(values.size, keys.size, entries.size).all { it == expectedKeys.size })

            assertEquals<Set<*>>(expectedKeys, keys)
            assertTrue(keys.containsAll(values))

            entries.forEach { entry ->
                assertEquals(entry.key, entry.value)
                assertTrue(expectedKeys.contains(entry.key))
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
                assertEquals(expectedSize, actual.size)

                val iterator = actual.keys.iterator()
                repeat(expectedSize) {
                    assertTrue(iterator.hasNext())

                    val nextKey = iterator.next()
                    assertEquals(expected[nextKey], actual[nextKey])

                    val shouldRemove = Random.nextDouble() < 0.2
                    if (shouldRemove) {
                        iterator.remove()
                        expectedSize--
                    }
                }
                assertFalse(iterator.hasNext())
            }

            assertTrue(actual.isEmpty())
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
                assertEquals(expectedSize, actual.size)

                val iterator = actual.values.iterator()
                repeat(expectedSize) {
                    assertTrue(iterator.hasNext())

                    iterator.next()

                    val shouldRemove = Random.nextDouble() < 0.2
                    if (shouldRemove) {
                        iterator.remove()
                        expectedSize--
                    }
                }
                assertFalse(iterator.hasNext())
            }

            assertTrue(actual.isEmpty())
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
                assertEquals(expectedSize, actual.size)

                val iterator = actual.entries.iterator()
                repeat(expectedSize) {
                    assertTrue(iterator.hasNext())

                    val nextEntry = iterator.next()
                    assertEquals(expected[nextEntry.key], actual[nextEntry.key])

                    val shouldUpdate = Random.nextDouble() < 0.1
                    if (shouldUpdate) {
                        val newValue = Random.nextInt()
                        assertEquals(expected.put(nextEntry.key, newValue), nextEntry.setValue(newValue))
                    }
                    val shouldRemove = Random.nextDouble() < 0.2
                    if (shouldRemove) {
                        iterator.remove()
                        expectedSize--
                    }
                }
                assertFalse(iterator.hasNext())
            }

            assertTrue(actual.isEmpty())
        }

        testAfterRandomPut { expected, map ->
            testEntriesIterator(expected.toMutableMap(), map.builder())
        }
    }

    @Test
    fun testReproduceOverIterationIssue() {
        val map: PersistentHashMap<Int, String> =
            persistentHashMapOf(1 to "a", 2  to "b", 3 to "c", 0 to "y", 32 to "z") as PersistentHashMap<Int, String>
        val builder = map.builder()
        val iterator = builder.entries.iterator()

        val expectedCount = map.size
        var actualCount = 0

        while (iterator.hasNext()) {
            val (key, _) = iterator.next()
            if (key == 0) iterator.remove()
            actualCount++
        }

        assertEquals(expectedCount, actualCount)
    }

    @Test
    fun `removing twice on iterators throws IllegalStateException`() {
        val map: PersistentHashMap<Int, String> =
            persistentHashMapOf(1 to "a", 2  to "b", 3 to "c", 0 to "y", 32 to "z") as PersistentHashMap<Int, String>
        val builder = map.builder()
        val iterator = builder.entries.iterator()

        assertFailsWith<IllegalStateException> {
            while (iterator.hasNext()) {
                val (key, _) = iterator.next()
                if (key == 0) iterator.remove()
                if (key == 0) {
                    iterator.remove()
                    iterator.remove()
                }
            }
        }
    }

    @Test
    fun `removing elements from different iterators throws ConcurrentModificationException`() {
        val map: PersistentHashMap<Int, String> =
            persistentHashMapOf(1 to "a", 2  to "b", 3 to "c", 0 to "y", 32 to "z") as PersistentHashMap<Int, String>
        val builder = map.builder()
        val iterator1 = builder.entries.iterator()
        val iterator2 = builder.entries.iterator()

        assertFailsWith<ConcurrentModificationException> {
            while (iterator1.hasNext()) {
                val (key, _) = iterator1.next()
                iterator2.next()
                if (key == 0) iterator1.remove()
                if (key == 2) iterator2.remove()
            }
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
            assertEquals(elementsToAdd - index, builder.size)
            assertEquals(values[index], builder[index])
            assertEquals(values[index], builder.remove(index))
            assertNull(builder[index])
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
            assertEquals(elementsToAdd - index, builder.size)
            assertEquals(values[index], builder[index])
            assertFalse(builder.remove(index, values[index + 1]))
            assertEquals(values[index], builder[index])
            assertTrue(builder.remove(index, values[index]))
            assertNull(builder[index])
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
                assertEquals(values[i], builder[i])
            }
        }
        repeat(times = elementsToAdd) { index ->
            for (i in elementsToAdd - 1 downTo index) {
                assertEquals(values[i], builder[i])
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
            assertNull(builder.put(index, values[2 * index]))

            for (i in 0..index) {
                val valueIndex = i + index

                assertEquals(values[valueIndex], builder[i])
                assertEquals(values[valueIndex], builder.put(i, values[valueIndex + 1]))
                assertEquals(values[valueIndex + 1], builder[i])
            }
        }
        repeat(times = elementsToAdd) { index ->
            for (i in index until elementsToAdd) {
                val valueIndex = elementsToAdd - index + i

                assertEquals(values[valueIndex], builder[i])
                assertEquals(values[valueIndex], builder.put(i, values[valueIndex - 1]))
                assertEquals(values[valueIndex - 1], builder[i])
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
                assertNull(builder.put(key(index), Int.MIN_VALUE))
                assertEquals(Int.MIN_VALUE, builder[key(index)])
                assertEquals(index + 1, builder.size)

                assertEquals(Int.MIN_VALUE, builder.put(key(index), index))
                assertEquals(index + 1, builder.size)

                val collisions = keyGen.wrappersByHashCode(key(index).hashCode)
                assertTrue(collisions.contains(key(index)))

                for (key in collisions) {
                    assertEquals(key.obj, builder[key])
                }
            }
            repeat(times = elementsToAdd) { index ->
                val collisions = keyGen.wrappersByHashCode(key(index).hashCode)
                assertTrue(collisions.contains(key(index)))

                if (builder[key(index)] == null) {
                    for (key in collisions) {
                        assertNull(builder[key])
                    }
                } else {
                    for (key in collisions) {
                        assertEquals(key.obj, builder[key])

                        if (removeEntryPredicate == 1) {
                            val nonExistingValue = Int.MIN_VALUE
                            assertFalse(builder.remove(key, nonExistingValue))
                            assertEquals(key.obj, builder[key])

                            assertTrue(builder.remove(key, key.obj))
                        } else {
                            val nonExistingKey = IntWrapper(Int.MIN_VALUE, key.hashCode)
                            assertNull(builder.remove(nonExistingKey))
                            assertEquals(key.obj, builder[key])

                            assertEquals(key.obj, builder.remove(key))
                        }

                        assertNull(builder[key])
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
                        assertEquals(map.remove(key), builder.remove(key))
                    }
                    shouldRemoveByKeyAndValue -> {
                        val shouldBeCurrentValue = Random.nextDouble() < 0.8
                        val value = if (shouldOperateOnExistingKey && shouldBeCurrentValue) map[key]!! else Random.nextInt()
                        assertEquals(map.remove(key, value), builder.remove(key, value))
                    }
                    else -> {
                        val value = Random.nextInt()
                        assertEquals(map.put(key, value), builder.put(key, value))
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
        assertEquals(expected.size, actual.size)
        assertEquals(expected[operationKey], actual[operationKey])
        assertEquals(expected.containsKey(operationKey), actual.containsKey(operationKey))

//        assertEquals(expected.containsValue(operationKey.obj), actual.containsValue(operationKey.obj))
//        assertEquals(expected.keys, actual.keys)
//        assertEquals(expected.values.sorted(), actual.values.sorted())
    }
}