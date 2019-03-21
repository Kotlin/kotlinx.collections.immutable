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

package kotlinx.collections.immutable.implementations.immutableList

import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import org.junit.Assert.*
import java.util.*
import kotlin.test.assertFailsWith

class PersistentListBuilderTest {

    @Test
    fun isEmptyTests() {
        val builder = persistentListOf<String>().builder()

        assertTrue(builder.isEmpty())

        val elementsToAdd = 100000
        repeat(times = elementsToAdd) { index ->
            builder.add(index.toString())
            assertFalse(builder.isEmpty())
        }
        repeat(times = elementsToAdd - 1) {
            builder.removeAt(builder.size - 1)
            assertFalse(builder.isEmpty())
        }
        builder.removeAt(builder.size - 1)
        assertTrue(builder.isEmpty())
    }

    @Test
    fun sizeTests() {
        val builder = persistentListOf<Int>().builder()

        assertTrue(builder.size == 0)

        val elementsToAdd = 100000
        repeat(times = elementsToAdd) { index ->
            builder.add(index)
            assertEquals(index + 1, builder.size)
        }
        repeat(times = elementsToAdd) { index ->
            builder.removeAt(builder.size - 1)
            assertEquals(elementsToAdd - index - 1, builder.size)
        }
    }


    @Test
    fun firstTests() {
        val builder = persistentListOf<Int>().builder()

        assertNull(builder.firstOrNull())

        val elementsToAdd = 10000
        repeat(times = elementsToAdd) { index ->
            builder.add(0, index)
            assertEquals(index, builder.first())
        }
        repeat(times = elementsToAdd) { index ->
            assertEquals(elementsToAdd - index - 1, builder.first())
            builder.removeAt(0)
        }
        assertNull(builder.firstOrNull())
    }

    @Test
    fun lastTests() {
        val builder = persistentListOf<Int>().builder()

        assertNull(builder.lastOrNull())

        val elementsToAdd = 100000
        repeat(times = elementsToAdd) { index ->
            builder.add(index)
            assertEquals(index, builder.last())
        }
        repeat(times = elementsToAdd) { index ->
            assertEquals(elementsToAdd - index - 1, builder.last())
            builder.removeAt(builder.size - 1)
        }
        assertNull(builder.lastOrNull())
    }

    @Test
    fun toListTest() {
        val builder = persistentListOf<Int>().builder()

        assertEquals(emptyList<Int>(), builder)

        val elementsToAdd = 1000
        val list = LinkedList<Int>()
        repeat(times = elementsToAdd) { index ->
            list.add(index)
            builder.add(index)
            assertEquals(list, builder)
        }
    }


    @Test
    fun addFirstTests() {
        val builder = persistentListOf<Int>().builder()

        assertNull(builder.firstOrNull())

        val elementsToAdd = 10000
        repeat(times = elementsToAdd) { index ->
            builder.add(0, index)

            assertEquals(index, builder.first())
            assertEquals(0, builder.last())
            assertEquals(index + 1, builder.size)
            assertEquals(List(index + 1) { index - it }, builder)
        }
    }

    @Test
    fun addLastTests() {
        val builder = persistentListOf<Int>().builder()

        val elementsToAdd = 10000
        repeat(times = elementsToAdd) { index ->
            builder.add(index)

            assertEquals(0, builder[0])
            assertEquals(index, builder[index])
            assertEquals(index + 1, builder.size)
            assertEquals(List(index + 1) { it }, builder)
        }
    }


    @Test
    fun removeFirstTests() {
        val builder = persistentListOf<Int>().builder()

        assertFailsWith<IndexOutOfBoundsException> { builder.removeAt(0) }

        val elementsToAdd = 10000
        repeat(times = elementsToAdd) { index ->
            builder.add(index)
        }
        repeat(times = elementsToAdd) { index ->
            assertEquals(elementsToAdd - 1, builder.last())
            assertEquals(index, builder.first())
            assertEquals(elementsToAdd - index, builder.size)
            assertEquals(List(elementsToAdd - index) { it + index }, builder.toList())
            builder.removeAt(0)
        }
    }

    @Test
    fun removeLastTests() {
        val builder = persistentListOf<Int>().builder()

        assertFailsWith<IndexOutOfBoundsException> {
            builder.removeAt(builder.size - 1)
        }

        val elementsToAdd = 10000
        repeat(times = elementsToAdd) { index ->
            builder.add(0, index)
        }
        repeat(times = elementsToAdd) { index ->
            assertEquals(index, builder.last())
            assertEquals(elementsToAdd - 1, builder.first())
            assertEquals(elementsToAdd - index, builder.size)
            assertEquals(List(elementsToAdd - index) { elementsToAdd - 1 - it }, builder)

            builder.removeAt(builder.size - 1)
        }


        repeat(times = 1000000) { index ->
            builder.add(index)
        }
        repeat(times = 1000000) { index ->
            assertEquals(1000000 - 1 - index, builder.last())
            assertEquals(0, builder.first())
            assertEquals(1000000 - index, builder.size)

            builder.removeAt(builder.size - 1)
        }
    }

    @Test
    fun getTests() {
        val builder = persistentListOf<Int>().builder()

        assertFailsWith<IndexOutOfBoundsException> {
            builder[0]
        }

        val elementsToAdd = 10000
        repeat(times = elementsToAdd) { index ->
            builder.add(index)

            for (i in 0..index) {
                assertEquals(i, builder[i])
            }
        }
        repeat(times = elementsToAdd) { index ->
            for (i in index until elementsToAdd) {
                assertEquals(i, builder[i - index])
            }

            builder.removeAt(0)
        }
    }

    @Test
    fun setTests() {
        val builder = persistentListOf<Int>().builder()

        assertFailsWith<IndexOutOfBoundsException> {
            builder[0] = 0
        }

        val elementsToAdd = 5000
        repeat(times = elementsToAdd) { index ->
            builder.add(index * 2)

            for (i in 0..index) {
                assertEquals(i + index, builder[i])
                builder[i] = i + index + 1
                assertEquals(i + index + 1, builder[i])
            }
        }
        repeat(times = elementsToAdd) { index ->
            for (i in 0..(elementsToAdd - index - 1)) {
                val expected = elementsToAdd + i

                assertEquals(expected, builder[i])
                builder[i] = expected - 1
                assertEquals(expected - 1, builder[i])
            }

            builder.removeAt(0)
        }
    }

    @Test
    fun subListTests() {
        val builder = persistentListOf<Int>().builder()

        val elementsToAdd = 10000
        repeat(times = elementsToAdd) { index ->
            builder.add(index)
        }

        val beginIndex = 1234
        val endIndex = 4321
        val subList = builder.subList(beginIndex, endIndex)

        builder[beginIndex] = 0
        assertEquals(endIndex - beginIndex, subList.size)
        assertEquals(subList[0], 0)
        assertEquals(elementsToAdd, builder.size)
        assertEquals(builder[beginIndex], 0)

        subList.add(beginIndex, 0)
        assertEquals(endIndex - beginIndex + 1, subList.size)
        assertEquals(0, subList[beginIndex])
        assertEquals(elementsToAdd + 1, builder.size)
        assertEquals(0, builder[beginIndex * 2])

        builder.add(0)
        assertFailsWith<ConcurrentModificationException> {
            subList.add(0)
        }
    }

    @Test
    fun randomOperationsTests() {
        val vectorGen = mutableListOf(List(20) { persistentListOf<Int>() })
        val actual = mutableListOf(List(20) { listOf<Int>() })

        repeat(times = 10) {

            val random = Random()
            val builders = vectorGen.last().map { it.builder() }
            val lists = builders.map { it.toMutableList() }

            repeat(times = 100000) {
                val index = random.nextInt(lists.size)
                val list = lists[index]
                val builder = builders[index]

                val operationType = random.nextDouble()
                val operationIndex = if (list.size > 1) random.nextInt(list.size) else 0

                val shouldRemove = operationType < 0.15
                val shouldSet = operationType > 0.15 && operationType < 0.3

                if (!list.isEmpty() && shouldRemove) {
                    assertEquals(list.removeAt(operationIndex),
                            builder.removeAt(operationIndex))
                } else if (!list.isEmpty() && shouldSet) {
                    val value = random.nextInt()
                    assertEquals(list.set(operationIndex, value),
                            builder.set(operationIndex, value))

                } else {
                    val value = random.nextInt()
                    list.add(operationIndex, value)
                    builder.add(operationIndex, value)
                }

                testAfterOperation(list, builder, operationIndex)
//                assertEquals(list, builder)
            }

            vectorGen.add( builders.map { it.build() } )
            actual.add( vectorGen.last().map { it.toMutableList() } )

            println(lists.maxBy { it.size }?.size)
        }

        vectorGen.forEachIndexed { index, vectors ->
            assertEquals(vectors.map { it.toList() }, actual[index].map { it.toList() })
        }
    }

    private fun testAfterOperation(list1: List<Int>, list2: List<Int>, operationIndex: Int) {
        assertEquals(list1.firstOrNull(), list2.firstOrNull())
        assertEquals(list1.lastOrNull(), list2.lastOrNull())
        assertEquals(list1.size, list2.size)
        if (operationIndex < list1.size) {
            assertEquals(list1[operationIndex], list2[operationIndex])
        }
        if (operationIndex > 0) {
            assertEquals(list1[operationIndex - 1], list2[operationIndex - 1])
        }
        if (operationIndex + 1 < list1.size) {
            assertEquals(list1[operationIndex + 1], list2[operationIndex + 1])
        }
    }
}