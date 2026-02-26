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


class PersistentHashMapTest : ExecutionTimeMeasuringTest() {
    @Test
    fun isEmptyTests() {
        var map = persistentHashMapOf<Int, String>()

        assertTrue(map.isEmpty())
        assertFalse(map.put(0, "last").isEmpty())

        val elementsToAdd = NForAlgorithmComplexity.O_NlogN

        val values = distinctStringValues(elementsToAdd)
        repeat(times = elementsToAdd) { index ->
            map = map.put(index, values[index])
            check(map.isNotEmpty())
        }
        repeat(times = elementsToAdd - 1) { index ->
            map = map.remove(index)
            check(map.isNotEmpty())
        }
        map = map.remove(elementsToAdd - 1)
        assertTrue(map.isEmpty())
    }

    @Test
    fun sizeTests() {
        var map = persistentHashMapOf<Int, Int>()

        assertTrue(map.size == 0)
        assertEquals(1, map.put(1, 1).size)

        val elementsToAdd = NForAlgorithmComplexity.O_NlogN

        repeat(times = elementsToAdd) { index ->
            map = map.put(index, index)
            check(index + 1 == map.size)

            map = map.put(index, index)
            check(index + 1 == map.size)

            map = map.put(index, 7)
            check(index + 1 == map.size)
        }
        repeat(times = elementsToAdd) { index ->
            map = map.remove(index)
            check(elementsToAdd - index - 1 == map.size)

            map = map.remove(index)
            check(elementsToAdd - index - 1 == map.size)
        }
    }

    @Test
    fun keysValuesEntriesTests() {
        fun testProperties(expectedKeys: Set<Int>, actualMap: PersistentMap<Int, Int>) {
            val values = actualMap.values
            val keys = actualMap.keys
            val entries = actualMap.entries

            check(listOf(values.size, keys.size, entries.size).all { it == expectedKeys.size })

            check(expectedKeys == keys)
            check(keys.containsAll(values))

            entries.forEach { entry ->
                check(entry.key == entry.value)
                check(expectedKeys.contains(entry.key))
            }
        }

        var map = persistentHashMapOf<Int, Int>()
        assertTrue(map.keys.isEmpty())
        assertTrue(map.values.isEmpty())

        val elementsToAdd = NForAlgorithmComplexity.O_NNlogN

        val set = hashSetOf<Int>()
        repeat(times = elementsToAdd) {
            val key = Random.nextInt()
            set.add(key)
            map = map.put(key, key)

            testProperties(set, map)
        }

        set.toMutableSet().forEach { key ->
            set.remove(key)
            map = map.remove(key)

            testProperties(set, map)
        }
    }

    @Test
    fun removeTests() {
        var map = persistentHashMapOf<Int, String>()
        assertTrue(map.put(0, "0").remove(0).isEmpty())

        val elementsToAdd = NForAlgorithmComplexity.O_NlogN

        val values = distinctStringValues(elementsToAdd)
        repeat(times = elementsToAdd) { index ->
            map = map.put(index, values[index])
        }
        repeat(times = elementsToAdd) { index ->
            check(elementsToAdd - index == map.size)
            check(values[index] == map[index])
            map = map.remove(index)
            check(map[index] == null)
        }
        assertTrue(map.isEmpty())
    }

    @Test
    fun removeEntryTests() {
        var map = persistentHashMapOf<Int, String>()
        assertTrue(map.put(0, "0").remove(0, "0").isEmpty())
        assertFalse(map.put(0, "0").remove(0, "x").isEmpty())

        val elementsToAdd = NForAlgorithmComplexity.O_NlogN

        val values = distinctStringValues(elementsToAdd + 1)
        repeat(times = elementsToAdd) { index ->
            map = map.put(index, values[index])
        }
        repeat(times = elementsToAdd) { index ->
            check(elementsToAdd - index == map.size)
            check(values[index] == map[index])
            map = map.remove(index, values[index + 1])
            check(values[index] == map[index])
            map = map.remove(index, values[index])
            check(map[index] == null)
        }
        assertTrue(map.isEmpty())
    }

    @Test
    fun getTests() {
        var map = persistentHashMapOf<Int, String>()
        assertEquals("1", map.put(1, "1")[1])

        val elementsToAdd = NForAlgorithmComplexity.O_NNlogN

        val values = distinctStringValues(elementsToAdd)
        repeat(times = elementsToAdd) { index ->
            map = map.put(index, values[index])

            for (i in 0..index) {
                check(values[i] == map[i])
            }
        }
        repeat(times = elementsToAdd) { index ->
            for (i in elementsToAdd - 1 downTo index) {
                check(values[i] == map[i])
            }

            map = map.remove(index)
        }
    }

    @Test
    fun putTests() {
        var map = persistentHashMapOf<Int, String>()
        assertEquals("2", map.put(1, "1").put(1, "2")[1])

        val elementsToAdd = NForAlgorithmComplexity.O_NNlogN

        val values = distinctStringValues(2 * elementsToAdd)
        repeat(times = elementsToAdd) { index ->
            map = map.put(index, values[2 * index])

            for (i in 0..index) {
                val valueIndex = i + index

                check(values[valueIndex] == map[i])
                map = map.put(i, values[valueIndex + 1])
                check(values[valueIndex + 1] == map[i])
            }
        }
        repeat(times = elementsToAdd) { index ->
            for (i in index until elementsToAdd) {
                val valueIndex = elementsToAdd - index + i

                check(values[valueIndex] == map[i])
                map = map.put(i, values[valueIndex - 1])
                check(values[valueIndex - 1] == map[i])
            }

            map = map.remove(index)
        }
        assertTrue(map.isEmpty())
    }

    @Test
    fun collisionTests() {
        var map = persistentHashMapOf<IntWrapper, Int>()

        val oneWrapper = IntWrapper(1, 1)
        val twoWrapper = IntWrapper(2, 1)
        assertEquals(1, map.put(oneWrapper, 1).put(twoWrapper, 2)[oneWrapper])
        assertEquals(2, map.put(oneWrapper, 1).put(twoWrapper, 2)[twoWrapper])

        repeat(times = 2) { removeEntryPredicate ->

            val elementsToAdd = NForAlgorithmComplexity.O_NlogN

            val maxHashCode = elementsToAdd / 5     // should be less than `elementsToAdd`
            val keyGen = WrapperGenerator<Int>(maxHashCode)
            fun key(key: Int): IntWrapper {
                return keyGen.wrapper(key)
            }

            repeat(times = elementsToAdd) { index ->
                map = map.put(key(index), Int.MIN_VALUE)
                check(Int.MIN_VALUE == map[key(index)])
                check(index + 1 == map.size)

                map = map.put(key(index), index)
                check(index + 1 == map.size)

                val collisions = keyGen.wrappersByHashCode(key(index).hashCode)
                check(collisions.contains(key(index)))

                for (key in collisions) {
                    check(key.obj == map[key])
                }
            }
            repeat(times = elementsToAdd) { index ->
                val collisions = keyGen.wrappersByHashCode(key(index).hashCode)
                check(collisions.contains(key(index)))

                if (map[key(index)] == null) {
                    for (key in collisions) {
                        check(map[key] == null)
                    }
                } else {
                    for (key in collisions) {
                        check(key.obj == map[key])

                        map = if (removeEntryPredicate == 1) {
                            val nonExistingValue = Int.MIN_VALUE
                            val sameMap = map.remove(key, nonExistingValue)
                            check(map.size == sameMap.size)
                            check(key.obj == sameMap[key])

                            map.remove(key, key.obj)
                        } else {
                            val nonExistingKey = IntWrapper(Int.MIN_VALUE, key.hashCode)
                            val sameMap = map.remove(nonExistingKey)
                            check(map.size == sameMap.size)
                            check(key.obj == sameMap[key])

                            map.remove(key)
                        }

                        check(map[key] == null)
                    }
                }
            }
            assertTrue(map.isEmpty())
        }
    }

    @Test
    fun randomOperationsTests() {
        repeat(times = 1) {

            val mutableMaps = List(10) { hashMapOf<IntWrapper?, Int?>() }
            val immutableMaps = MutableList(10) { persistentHashMapOf<IntWrapper?, Int?>() }

            val operationCount = NForAlgorithmComplexity.O_NlogN

            val numberOfDistinctHashCodes = operationCount / 3  // less than `operationCount` to increase collision cases
            val hashCodes = List(numberOfDistinctHashCodes) { Random.nextInt() }

            repeat(times = operationCount) {
                val index = Random.nextInt(mutableMaps.size)
                val mutableMap = mutableMaps[index]
                val immutableMap = immutableMaps[index]

                val shouldRemove = Random.nextDouble() < 0.3
                val shouldOperateOnExistingKey = mutableMap.isNotEmpty() && Random.nextDouble().let { if (shouldRemove) it < 0.8 else it < 0.2 }

                val key = when {
                    shouldOperateOnExistingKey -> mutableMap.keys.first()
                    Random.nextDouble() < 0.001 -> null
                    else -> IntWrapper(Random.nextInt(), hashCodes.random())
                }

                val shouldRemoveByKey = shouldRemove && Random.nextBoolean()
                val shouldRemoveByKeyAndValue = shouldRemove && !shouldRemoveByKey

                val newImmutableMap = when {
                    shouldRemoveByKey -> {
                        mutableMap.remove(key)
                        immutableMap.remove(key)
                    }
                    shouldRemoveByKeyAndValue -> {
                        val shouldBeCurrentValue = Random.nextDouble() < 0.8
                        val value = if (shouldOperateOnExistingKey && shouldBeCurrentValue) mutableMap[key] else Random.nextInt()
                        val _ = mutableMap.remove(key, value)
                        immutableMap.remove(key, value)
                    }
                    else -> {
                        val shouldPutNullValue = Random.nextDouble() < 0.001
                        val value = if (shouldPutNullValue) null else Random.nextInt()
                        mutableMap[key] = value
                        immutableMap.put(key, value)
                    }
                }

                testAfterOperation(mutableMap, newImmutableMap, key)

                immutableMaps[index] = newImmutableMap
            }

            assertEquals<List<*>>(mutableMaps, immutableMaps)

            val maxSize = immutableMaps.maxOf { it.size }
            println("Largest persistent map size: $maxSize")

            mutableMaps.forEachIndexed { index, mutableMap ->
                var immutableMap = immutableMaps[index]

                val keys = mutableMap.keys.toMutableList()
                for (key in keys) {
                    mutableMap.remove(key)
                    immutableMap = immutableMap.remove(key)

                    testAfterOperation(mutableMap, immutableMap, key)
                }
            }
        }
    }

    private fun testAfterOperation(
            expected: Map<IntWrapper?, Int?>,
            actual: Map<IntWrapper?, Int?>,
            operationKey: IntWrapper?
    ) {
        check(expected.size == actual.size)
        check(expected[operationKey] == actual[operationKey])
        check(expected.containsKey(operationKey) == actual.containsKey(operationKey))

//        check(expected.containsValue(operationKey?.obj) == actual.containsValue(operationKey?.obj))
//        check(expected.keys == actual.keys)
//        check(expected.values.sortedBy { it } == actual.values.sortedBy { it })
    }
}
