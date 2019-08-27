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

package tests.stress.list

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlin.random.Random
import kotlin.test.*


class PersistentListTest {
    @Test
    fun isEmptyTests() {
        var vector = persistentListOf<String>()

        assertTrue(vector.isEmpty())
        assertFalse(vector.add("last").isEmpty())

        val elementsToAdd = 100000
        repeat(times = elementsToAdd) { index ->
            vector = vector.add(index.toString())
            assertFalse(vector.isEmpty())
        }
        repeat(times = elementsToAdd - 1) {
            vector = vector.removeAt(vector.size - 1)
            assertFalse(vector.isEmpty())
        }
        vector = vector.removeAt(vector.size - 1)
        assertTrue(vector.isEmpty())
    }

    @Test
    fun sizeTests() {
        var vector = persistentListOf<Int>()

        assertTrue(vector.size == 0)
        assertEquals(1, vector.add(1).size)

        val elementsToAdd = 100000
        repeat(times = elementsToAdd) { index ->
            vector = vector.add(index)
            assertEquals(index + 1, vector.size)
        }
        repeat(times = elementsToAdd) { index ->
            vector = vector.removeAt(vector.size - 1)
            assertEquals(elementsToAdd - index - 1, vector.size)
        }
    }


    @Test
    fun firstTests() {
        var vector = persistentListOf<Int>()

        assertNull(vector.firstOrNull())
        assertEquals(1, vector.add(0, 1).first())
        assertEquals(1, vector.add(1).first())

        val elementsToAdd = 10000
        repeat(times = elementsToAdd) { index ->
            vector = vector.add(0, index)
            assertEquals(index, vector.first())
        }
        repeat(times = elementsToAdd) { index ->
            assertEquals(elementsToAdd - index - 1, vector.first())
            vector = vector.removeAt(0)
        }
        assertNull(vector.firstOrNull())
    }

    @Test
    fun lastTests() {
        var vector = persistentListOf<Int>()

        assertNull(vector.lastOrNull())
        assertEquals(1, vector.add(0, 1).last())
        assertEquals(1, vector.add(1).last())

        val elementsToAdd = 100000
        repeat(times = elementsToAdd) { index ->
            vector = vector.add(index)
            assertEquals(index, vector.last())
        }
        repeat(times = elementsToAdd) { index ->
            assertEquals(elementsToAdd - index - 1, vector.last())
            vector = vector.removeAt(vector.size - 1)
        }
        assertNull(vector.lastOrNull())
    }

    @Test
    fun toListTest() {
        var vector = persistentListOf<Int>()

        assertEquals(emptyList<Int>(), vector.toList())
        assertEquals(listOf(1), vector.add(1).toList())

        assertEquals(
                listOf(1, 2, 3, 4, 5, 6),
                vector
                        .add(1).add(2).add(3).add(4).add(5)
                        .add(6)
                        .toList()
        )

        assertEquals(
                listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20),
                vector
                        .add(1).add(2).add(3).add(4).add(5)
                        .add(6).add(7).add(8).add(9).add(10)
                        .add(11).add(12).add(13).add(14).add(15)
                        .add(16).add(17).add(18).add(19).add(20)
                        .toList()
        )

        val elementsToAdd = 1000
        val list = mutableListOf<Int>()
        repeat(times = elementsToAdd) { index ->
            list.add(index)
            vector = vector.add(index)
            assertEquals(list, vector.toList())
        }
    }


    @Test
    fun addFirstTests() {
        var vector = persistentListOf<Int>()

        assertNull(vector.firstOrNull())
        assertEquals(1, vector.add(0, 1).first())
        assertEquals(1, vector.add(0, 1).last())

        val elementsToAdd = 10000
        repeat(times = elementsToAdd) { index ->
            vector = vector.add(0, index)

            assertEquals(index, vector.first())
            assertEquals(0, vector.last())
            assertEquals(index + 1, vector.size)
            assertEquals(List(index + 1) { index - it }, vector.toList())
        }
    }

    @Test
    fun addLastTests() {
        var vector = persistentListOf<Int>()

        assertEquals(1, vector.add(1)[0])

        val elementsToAdd = 10000
        repeat(times = elementsToAdd) { index ->
            vector = vector.add(index)

            assertEquals(0, vector[0])
            assertEquals(index, vector[index])
            assertEquals(index + 1, vector.size)
            assertEquals(List(index + 1) { it }, vector.toList())
        }
    }


    @Test
    fun removeFirstTests() {
        var vector = persistentListOf<Int>()

        assertFailsWith<IndexOutOfBoundsException> {
            vector.removeAt(0)
        }
        assertTrue(vector.add(1).removeAt(0).isEmpty())
        assertTrue(vector.add(0, 1).removeAt(0).isEmpty())

        val elementsToAdd = 10000
        repeat(times = elementsToAdd) { index ->
            vector = vector.add(index)
        }
        repeat(times = elementsToAdd) { index ->
            assertEquals(elementsToAdd - 1, vector.last())
            assertEquals(index, vector.first())
            assertEquals(elementsToAdd - index, vector.size)
            assertEquals(List(elementsToAdd - index) { it + index }, vector.toList())
            vector = vector.removeAt(0)
        }
    }

    @Test
    fun removeLastTests() {
        var vector = persistentListOf<Int>()

        assertFailsWith<IndexOutOfBoundsException> {
            vector.removeAt(vector.size - 1)
        }
        assertTrue(vector.add(1).removeAt(0).isEmpty())
        assertTrue(vector.add(0, 1).removeAt(0).isEmpty())

        val elementsToAdd = 10000
        repeat(times = elementsToAdd) { index ->
            vector = vector.add(0, index)
        }
        repeat(times = elementsToAdd) { index ->
            assertEquals(index, vector.last())
            assertEquals(elementsToAdd - 1, vector.first())
            assertEquals(elementsToAdd - index, vector.size)
            assertEquals(List(elementsToAdd - index) { elementsToAdd - 1 - it }, vector.toList())

            vector = vector.removeAt(vector.size - 1)
        }


        repeat(times = 1000000) { index ->
            vector = vector.add(index)
        }
        repeat(times = 1000000) { index ->
            assertEquals(1000000 - 1 - index, vector.last())
            assertEquals(0, vector.first())
            assertEquals(1000000 - index, vector.size)

            vector = vector.removeAt(vector.size - 1)
        }
    }

    @Test
    fun getTests() {
        var vector = persistentListOf<Int>()

        assertFailsWith<IndexOutOfBoundsException> {
            vector[0]
        }
        assertEquals(1, vector.add(1)[0])

        val elementsToAdd = 10000
        repeat(times = elementsToAdd) { index ->
            vector = vector.add(index)

            for (i in 0..index) {
                assertEquals(i, vector[i])
            }
        }
        repeat(times = elementsToAdd) { index ->
            for (i in index until elementsToAdd) {
                assertEquals(i, vector[i - index])
            }

            vector = vector.removeAt(0)
        }
    }

    @Test
    fun setTests() {
        var vector = persistentListOf<Int>()

        assertFailsWith<IndexOutOfBoundsException> {
            vector.set(0, 0)
        }
        assertEquals(2, vector.add(1).set(0, 2)[0])

        val elementsToAdd = 5000
        repeat(times = elementsToAdd) { index ->
            vector = vector.add(index * 2)

            for (i in 0..index) {
                assertEquals(i + index, vector[i])
                vector = vector.set(i, i + index + 1)
                assertEquals(i + index + 1, vector[i])
            }
        }
        repeat(times = elementsToAdd) { index ->
            for (i in 0..(elementsToAdd - index - 1)) {
                val expected = elementsToAdd + i

                assertEquals(expected, vector[i])
                vector = vector.set(i, expected - 1)
                assertEquals(expected - 1, vector[i])
            }

            vector = vector.removeAt(0)
        }
    }

    @Test
    fun randomOperationsTests() {
        repeat(times = 1) {

            val lists = List(20) { mutableListOf<Int>() }
            val vectors = MutableList(20) { persistentListOf<Int>() }

            repeat(times = 1000000) {
                val index = Random.nextInt(lists.size)
                val list = lists[index]
                val vector = vectors[index]

                val operationType = Random.nextDouble()
                val operationIndex = if (list.size > 1) Random.nextInt(list.size) else 0

                val shouldRemove = operationType < 0.15
                val shouldSet = operationType > 0.15 && operationType < 0.3

                val newVector = if (!list.isEmpty() && shouldRemove) {
                    list.removeAt(operationIndex)
                    vector.removeAt(operationIndex)
                } else if (!list.isEmpty() && shouldSet) {
                    val value = Random.nextInt()
                    list[operationIndex] = value
                    vector.set(operationIndex, value)
                } else {
                    val value = Random.nextInt()
                    list.add(operationIndex, value)
                    vector.add(operationIndex, value)
                }

                testAfterOperation(list, newVector, operationIndex)
//                assertEquals(list, newVector.toList())

                vectors[index] = newVector
            }

            println(lists.maxBy { it.size }?.size)

            lists.forEachIndexed { index, list ->
                var vector = vectors[index]

                while (!list.isEmpty()) {
                    val removeIndex = Random.nextInt(list.size)
                    list.removeAt(removeIndex)
                    vector = vector.removeAt(removeIndex)

                    testAfterOperation(list, vector, removeIndex)
//                    assertEquals(list, vector.toList())
                }
            }
        }
    }

    private fun testAfterOperation(list: List<Int>, vector: PersistentList<Int>, operationIndex: Int) {
        assertEquals(list.firstOrNull(), vector.firstOrNull())
        assertEquals(list.lastOrNull(), vector.lastOrNull())
        assertEquals(list.size, vector.size)
        if (operationIndex < list.size) {
            assertEquals(list[operationIndex], vector[operationIndex])
        }
        if (operationIndex > 0) {
            assertEquals(list[operationIndex - 1], vector[operationIndex - 1])
        }
        if (operationIndex + 1 < list.size) {
            assertEquals(list[operationIndex + 1], vector[operationIndex + 1])
        }
    }
}