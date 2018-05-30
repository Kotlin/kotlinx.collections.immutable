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

package kotlinx.collections.immutable.implementations.immutableMap

import kotlinx.collections.immutable.implementations.ObjectWrapper
import kotlinx.collections.immutable.implementations.WrapperGenerator
import org.junit.Test
import org.junit.Assert.*
import java.util.*


class PersistentHashMapTest {
    @Test
    fun isEmptyTests() {
        var map = persistentHashMapOf<Int, String>()

        assertTrue(map.isEmpty())
        assertFalse(map.put(0, "last").isEmpty())

        val elementsToAdd = 1000000
        repeat(times = elementsToAdd) { index ->
            map = map.put(index, index.toString())
            assertFalse(map.isEmpty())
        }
        repeat(times = elementsToAdd - 1) { index ->
            map = map.remove(index)
            assertFalse(map.isEmpty())
        }
        map = map.remove(elementsToAdd - 1)
        assertTrue(map.isEmpty())
    }

    @Test
    fun sizeTests() {
        var map = persistentHashMapOf<Int, Int>()

        assertTrue(map.size == 0)
        assertEquals(1, map.put(1, 1).size)

        val elementsToAdd = 100000
        repeat(times = elementsToAdd) { index ->
            map = map.put(index, index)
            assertEquals(index + 1, map.size)

            map = map.put(index, index)
            assertEquals(index + 1, map.size)

            map = map.put(index, 7)
            assertEquals(index + 1, map.size)
        }
        repeat(times = elementsToAdd) { index ->
            map = map.remove(index)
            assertEquals(elementsToAdd - index - 1, map.size)

            map = map.remove(index)
            assertEquals(elementsToAdd - index - 1, map.size)
        }
    }

    @Test
    fun keysValuesEntriesTests() {
        var map = persistentHashMapOf<Int, Int>()
        assertTrue(map.keys.isEmpty())
        assertTrue(map.values.isEmpty())

        val set = mutableSetOf<Int>()
        val random = Random()

        val elementsToAdd = 2000
        repeat(times = elementsToAdd) {
            val key = random.nextInt()
            set.add(key)
            map = map.put(key, key)

            assertEquals(set.sorted(), map.keys.sorted())
            assertEquals(set.sorted(), map.values.sorted())

            map.entries.forEach { entry ->
                assertEquals(entry.key, entry.value)
                assertTrue(set.contains(entry.key))
            }
        }

        set.toMutableSet().forEach { key ->
            set.remove(key)
            map = map.remove(key)

            assertEquals(set.sorted(), map.keys.sorted())
            assertEquals(set.sorted(), map.values.sorted())

            map.entries.forEach { entry ->
                assertEquals(entry.key, entry.value)
                assertTrue(set.contains(entry.key))
            }
        }
    }

    @Test
    fun removeTests() {
        var map = persistentHashMapOf<Int, String>()
        assertTrue(map.put(0, "0").remove(0).isEmpty())

        val elementsToAdd = 1000000
        repeat(times = elementsToAdd) { index ->
            map = map.put(index, index.toString())
        }
        repeat(times = elementsToAdd) { index ->
            assertEquals(elementsToAdd - index, map.size)
            assertEquals(index.toString(), map[index])
            map = map.remove(index)
            assertNull(map[index])
        }
    }

    @Test
    fun removeEntryTests() {
        var map = persistentHashMapOf<Int, String>()
        assertTrue(map.put(0, "0").remove(0, "0").isEmpty())
        assertFalse(map.put(0, "0").remove(0, "x").isEmpty())

        val elementsToAdd = 1000000
        repeat(times = elementsToAdd) { index ->
            map = map.put(index, index.toString())
        }
        repeat(times = elementsToAdd) { index ->
            assertEquals(elementsToAdd - index, map.size)
            assertEquals(index.toString(), map[index])
            map = map.remove(index, (index + 1).toString())
            assertEquals(index.toString(), map[index])
            map = map.remove(index, index.toString())
            assertNull(map[index])
        }
    }

    @Test
    fun getTests() {
        var map = persistentHashMapOf<Int, String>()
        assertEquals("1", map.put(1, "1")[1])

        val elementsToAdd = 10000
        repeat(times = elementsToAdd) { index ->
            map = map.put(index, index.toString())

            for (i in 0..index) {
                assertEquals(i.toString(), map[i])
            }
        }
        repeat(times = elementsToAdd) { index ->
            for (i in elementsToAdd - 1 downTo index) {
                assertEquals(i.toString(), map[i])
            }

            map = map.remove(index)
        }
    }

    @Test
    fun putTests() {
        var map = persistentHashMapOf<Int, String>()
        assertEquals("2", map.put(1, "1").put(1, "2")[1])

        val elementsToAdd = 5000
        repeat(times = elementsToAdd) { index ->
            map = map.put(index, (index * 2).toString())

            for (i in 0..index) {
                assertEquals((i + index).toString(), map[i])
                map = map.put(i, (i + index + 1).toString())
                assertEquals((i + index + 1).toString(), map[i])
            }
        }
        repeat(times = elementsToAdd) { index ->
            for (i in index until elementsToAdd) {
                val expected = elementsToAdd - index + i

                assertEquals(expected.toString(), map[i])
                map = map.put(i, (expected - 1).toString())
                assertEquals((expected - 1).toString(), map[i])
            }

            map = map.remove(index)
        }
    }

    @Test
    fun collisionTests() {
        var map = persistentHashMapOf<ObjectWrapper<Int>, Int>()

        assertEquals(2, map.put(ObjectWrapper(1, 1), 1).put(ObjectWrapper(1, 1), 2)[ObjectWrapper(1, 1)])

        repeat(times = 2) { removeEntryPredicate ->
            val keyGen = WrapperGenerator<Int>(20000)
            fun key(key: Int): ObjectWrapper<Int> {
                return keyGen.wrapper(key)
            }

            val elementsToAdd = 100000   /// should be more than keyGen.hashCodeUpperBound
            repeat(times = elementsToAdd) { index ->
                map = map.put(key(index), Int.MIN_VALUE)
                assertEquals(Int.MIN_VALUE, map[key(index)])
                assertEquals(index + 1, map.size)

                map = map.put(key(index), index)
                assertEquals(index + 1, map.size)

                val collisions = keyGen.wrappersByHashCode(key(index).hashCode)
                assertTrue(collisions.contains(key(index)))

                for (key in collisions) {
                    assertEquals(key.obj, map[key])
                }
            }
            repeat(times = elementsToAdd) { index ->
                val collisions = keyGen.wrappersByHashCode(key(index).hashCode)
                assertTrue(collisions.contains(key(index)))

                if (map[key(index)] == null) {
                    for (key in collisions) {
                        assertNull(map[key])
                    }
                } else {
                    for (key in collisions) {
                        assertEquals(key.obj, map[key])
                        map = if (removeEntryPredicate == 1) {
                            val sameMap = map.remove(key, Int.MIN_VALUE)
                            assertEquals(map.size, sameMap.size)
                            assertEquals(key.obj, sameMap[key])

                            map.remove(key, key.obj)
                        } else {
                            val sameMap = map.remove(ObjectWrapper(Int.MIN_VALUE, key.hashCode))
                            assertEquals(map.size, sameMap.size)
                            assertEquals(key.obj, sameMap[key])

                            map.remove(key)
                        }
                    }
                }
            }
            assertTrue(map.isEmpty())
        }
    }

    @Test
    fun randomOperationsTests() {
        repeat(times = 1) {

            val random = Random()
            val mutableMaps = List(10) { mutableMapOf<ObjectWrapper<Int>?, Int?>() }
            val immutableMaps = MutableList(10) { persistentHashMapOf<ObjectWrapper<Int>?, Int?>() }

            val operationCount = 2000000
            val hashCodes = List(operationCount / 3) { random.nextInt() }
            repeat(times = operationCount) {
                val index = random.nextInt(mutableMaps.size)
                val mutableMap = mutableMaps[index]
                val immutableMap = immutableMaps[index]

                val key = if (random.nextDouble() < 0.001) {
                    null
                } else {
                    val hashCodeIndex = random.nextInt(hashCodes.size)
                    ObjectWrapper(random.nextInt(), hashCodes[hashCodeIndex])
                }

                val operationType = random.nextDouble()

                val shouldRemove = operationType < 0.2
                val shouldRemoveEntry = !shouldRemove && operationType < 0.4
                val shouldPutNull = !shouldRemove && !shouldRemoveEntry && operationType < 0.45

                val newImmutableMap = when {
                    shouldRemove -> {
                        mutableMap.remove(key)
                        immutableMap.remove(key)
                    }
                    shouldRemoveEntry -> {
                        val value = random.nextInt(5)
                        mutableMap.remove(key, value)
                        immutableMap.remove(key, value)
                    }
                    shouldPutNull -> {
                        mutableMap[key] = null
                        immutableMap.put(key, null)
                    }
                    else -> {
                        val value = random.nextInt(5)
                        mutableMap[key] = value
                        immutableMap.put(key, value)
                    }
                }

                assertEquals(mutableMap.size, newImmutableMap.size)
                assertEquals(mutableMap[key], newImmutableMap[key])
                assertEquals(mutableMap.containsKey(key), newImmutableMap.containsKey(key))
//                assertEquals(mutableMap.containsValue(key), newImmutableMap.containsValue(key))
//                assertEquals(mutableMap.keys.sorted(), newImmutableMap.keys.sorted())
//                assertEquals(mutableMap.values.sorted(), newImmutableMap.values.sorted())

                immutableMaps[index] = newImmutableMap
            }

            println(mutableMaps.maxBy { it.size }?.size)
            println(immutableMaps.maxBy { it.size }?.size)

            mutableMaps.forEachIndexed { index, mutableMap ->
                var immutableMap = immutableMaps[index]

                for (key in mutableMap.keys.toMutableSet()) {
                    mutableMap.remove(key)
                    immutableMap = immutableMap.remove(key)

                    assertEquals(mutableMap.size, immutableMap.size)
                    assertEquals(mutableMap[key], immutableMap[key])
                    assertEquals(mutableMap.containsKey(key), immutableMap.containsKey(key))
//                    assertEquals(mutableMap.containsValue(key), immutableMap.containsValue(key))
//                    assertEquals(mutableMap.keys.sorted(), immutableMap.keys.sorted())
//                    assertEquals(mutableMap.values.sorted(), immutableMap.values.sorted())
                }
            }
        }
    }
}