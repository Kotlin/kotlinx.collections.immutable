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

package tests.stress.map

import kotlinx.collections.immutable.persistentHashMapOf
import tests.stress.ObjectWrapper
import tests.stress.WrapperGenerator
import tests.remove
import kotlin.random.Random
import kotlin.test.*

class PersistentHashMapBuilderTest {

    @Test
    fun isEmptyTests() {
        val builder = persistentHashMapOf<Int, String>().builder()

        assertTrue(builder.isEmpty())

        val elementsToAdd = 100000
        repeat(times = elementsToAdd) { index ->
            builder[index] = index.toString()
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

        val elementsToAdd = 100000
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
        val builder = persistentHashMapOf<Int, Int>().builder()
        assertTrue(builder.keys.isEmpty())
        assertTrue(builder.values.isEmpty())

        val set = mutableSetOf<Int>()

        val elementsToAdd = 2000
        repeat(times = elementsToAdd) {
            val key = Random.nextInt()
            set.add(key)
            builder[key] = key

            assertEquals(set.sorted(), builder.keys.sorted())
            assertEquals(set.sorted(), builder.values.sorted())

            builder.entries.forEach { entry ->
                assertEquals(entry.key, entry.value)
                assertTrue(set.contains(entry.key))
            }
        }

        set.toMutableSet().forEach { key ->
            set.remove(key)
            builder.remove(key)

            assertEquals(set.sorted(), builder.keys.sorted())
            assertEquals(set.sorted(), builder.values.sorted())

            builder.entries.forEach { entry ->
                assertEquals(entry.key, entry.value)
                assertTrue(set.contains(entry.key))
            }
        }
    }

    @Test
    fun removeTests() {
        val builder = persistentHashMapOf<Int, String>().builder()

        val elementsToAdd = 1000000
        repeat(times = elementsToAdd) { index ->
            builder[index] = index.toString()
        }
        repeat(times = elementsToAdd) { index ->
            assertEquals(elementsToAdd - index, builder.size)
            assertEquals(index.toString(), builder[index])
            assertEquals(index.toString(), builder.remove(index))
            assertNull(builder[index])
        }
    }

    @Test
    fun removeEntryTests() {
        val builder = persistentHashMapOf<Int, String>().builder()

        val elementsToAdd = 1000000
        repeat(times = elementsToAdd) { index ->
            builder[index] = index.toString()
        }
        repeat(times = elementsToAdd) { index ->
            assertEquals(elementsToAdd - index, builder.size)
            assertEquals(index.toString(), builder[index])
            assertFalse(builder.remove(index, (index + 1).toString()))
            assertEquals(index.toString(), builder[index])
            assertTrue(builder.remove(index, index.toString()))
            assertNull(builder[index])
        }
    }

    @Test
    fun getTests() {
        val builder = persistentHashMapOf<Int, String>().builder()

        val elementsToAdd = 10000
        repeat(times = elementsToAdd) { index ->
            builder[index] = index.toString()

            for (i in 0..index) {
                assertEquals(i.toString(), builder[i])
            }
        }
        repeat(times = elementsToAdd) { index ->
            for (i in elementsToAdd - 1 downTo index) {
                assertEquals(i.toString(), builder[i])
            }

            builder.remove(index)
        }
    }

    @Test
    fun putTests() {
        val builder = persistentHashMapOf<Int, String>().builder()

        val elementsToAdd = 5000
        repeat(times = elementsToAdd) { index ->
            assertNull(builder.put(index, (index * 2).toString()))

            for (i in 0..index) {
                assertEquals((i + index).toString(), builder[i])
                assertEquals((i + index).toString(), builder.put(i, (i + index + 1).toString()))
                assertEquals((i + index + 1).toString(), builder[i])
            }
        }
        repeat(times = elementsToAdd) { index ->
            for (i in index until elementsToAdd) {
                val expected = elementsToAdd - index + i

                assertEquals(expected.toString(), builder[i])
                assertEquals(expected.toString(), builder.put(i, (expected - 1).toString()))
                assertEquals((expected - 1).toString(), builder[i])
            }

            builder.remove(index)
        }
    }

    @Test
    fun collisionTests() {
        val builder = persistentHashMapOf<ObjectWrapper<Int>, Int>().builder()

        repeat(times = 2) { removeEntryPredicate ->
            val keyGen = WrapperGenerator<Int>(20000)
            fun key(key: Int): ObjectWrapper<Int> {
                return keyGen.wrapper(key)
            }

            val elementsToAdd = 100000   /// should be more than keyGen.hashCodeUpperBound
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
                            assertNull(builder.remove(ObjectWrapper(Int.MIN_VALUE, key.hashCode)))
                            assertEquals(key.obj, builder[key])
                            assertEquals(true, builder.remove(key, key.obj))
                            assertNull(builder[key])
                        } else {
                            assertNull(builder.remove(ObjectWrapper(Int.MIN_VALUE, key.hashCode)))
                            assertEquals(key.obj, builder[key])
                            assertEquals(key.obj, builder.remove(key))
                            assertNull(builder[key])
                        }
                    }
                }
            }
            assertTrue(builder.isEmpty())
        }
    }

    @Test
    fun randomOperationsTests() {
        val mapGen = mutableListOf(List(20) { persistentHashMapOf<ObjectWrapper<Int>, Int>() })
        val expected = mutableListOf(List(20) { mapOf<ObjectWrapper<Int>, Int>() })

        repeat(times = 10) {

            val builders = mapGen.last().map { it.builder() }
            val maps = builders.map { it.toMutableMap() }

            val operationCount = 200000
            val hashCodes = List(operationCount / 2) { Random.nextInt() }
            repeat(times = operationCount) {
                val index = Random.nextInt(maps.size)
                val map = maps[index]
                val builder = builders[index]

                val operationType = Random.nextDouble()
                val hashCodeIndex = Random.nextInt(hashCodes.size)
                val key = ObjectWrapper(Random.nextInt(), hashCodes[hashCodeIndex])

                val shouldRemove = operationType < 0.2
                val shouldRemoveEntry = !shouldRemove && operationType < 0.4
                when {
                    shouldRemove -> {
                        assertEquals(map.remove(key), builder.remove(key))
                    }
                    shouldRemoveEntry -> {
                        val value = Random.nextInt(5)
                        assertEquals(map.remove(key, value), builder.remove(key, value))
                    }
                    else -> {
                        val value = Random.nextInt(5)
                        assertEquals(map.put(key, value), builder.put(key, value))
                    }
                }

                assertEquals(map.size, builder.size)
                assertEquals(map[key], builder[key])
                assertEquals(map.containsKey(key), builder.containsKey(key))
//                assertEquals(mutableMap.containsValue(key), newImmutableMap.containsValue(key))
//                assertEquals(mutableMap.keys.sorted(), newImmutableMap.keys.sorted())
//                assertEquals(mutableMap.values.sorted(), newImmutableMap.values.sorted())
            }

            mapGen.add( builders.map { it.build() } )
            expected.add( mapGen.last().map { it.toMutableMap() } )

            println(maps.maxBy { it.size }?.size)
        }

        mapGen.forEachIndexed { index, maps ->
            assertEquals(expected[index], maps)
        }
    }
}