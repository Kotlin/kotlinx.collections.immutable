/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.list

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import tests.NForAlgorithmComplexity
import tests.distinctStringValues
import tests.stress.ExecutionTimeMeasuringTest
import kotlin.random.Random
import kotlin.test.*


class PersistentListTest : ExecutionTimeMeasuringTest() {
    @Test
    fun isEmptyTests() {
        var vector = persistentListOf<String>()

        assertTrue(vector.isEmpty())
        assertFalse(vector.add("last").isEmpty())

        val elementsToAdd = NForAlgorithmComplexity.O_N

        val elements = distinctStringValues(elementsToAdd)
        repeat(times = elementsToAdd) { index ->
            vector = vector.add(elements[index])
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

        val elementsToAdd = NForAlgorithmComplexity.O_N

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

        val elementsToAdd = NForAlgorithmComplexity.O_NN

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

        val elementsToAdd = NForAlgorithmComplexity.O_N

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

        val elementsToAdd = NForAlgorithmComplexity.O_NN

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

        val elementsToAdd = NForAlgorithmComplexity.O_NN

        val allElements = List(elementsToAdd) { elementsToAdd - it - 1 }
        repeat(times = elementsToAdd) { index ->
            vector = vector.add(0, index)

            assertEquals(index, vector.first())
            assertEquals(0, vector.last())
            assertEquals(index + 1, vector.size)

            val expectedContent = allElements.subList(elementsToAdd - vector.size, elementsToAdd)
            assertEquals(expectedContent, vector.toList())
        }
    }

    @Test
    fun addLastTests() {
        var vector = persistentListOf<Int>()

        assertEquals(1, vector.add(1)[0])

        val elementsToAdd = NForAlgorithmComplexity.O_NN

        val allElements = List(elementsToAdd) { it }
        repeat(times = elementsToAdd) { index ->
            vector = vector.add(index)

            assertEquals(0, vector[0])
            assertEquals(index, vector[index])
            assertEquals(index + 1, vector.size)

            assertEquals(allElements.subList(0, vector.size), vector)
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

        val elementsToAdd = NForAlgorithmComplexity.O_NN

        val allElements = List(elementsToAdd) { it }
        repeat(times = elementsToAdd) { index ->
            vector = vector.add(index)
        }
        repeat(times = elementsToAdd) { index ->
            assertEquals(elementsToAdd - 1, vector.last())
            assertEquals(index, vector.first())
            assertEquals(elementsToAdd - index, vector.size)
            assertEquals(allElements.subList(index, elementsToAdd), vector)

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

        val elementsToAdd = NForAlgorithmComplexity.O_NN

        val allElements = List(elementsToAdd) { elementsToAdd - it - 1 }
        repeat(times = elementsToAdd) { index ->
            vector = vector.add(0, index)
        }
        repeat(times = elementsToAdd) { index ->
            assertEquals(index, vector.last())
            assertEquals(elementsToAdd - 1, vector.first())
            assertEquals(elementsToAdd - index, vector.size)
            assertEquals(allElements.subList(0, vector.size), vector)

            vector = vector.removeAt(vector.size - 1)
        }

        val linear = NForAlgorithmComplexity.O_N

        repeat(times = linear) { index ->
            vector = vector.add(index)
        }
        repeat(times = linear) { index ->
            assertEquals(linear - 1 - index, vector.last())
            assertEquals(0, vector.first())
            assertEquals(linear - index, vector.size)

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

        val elementsToAdd = NForAlgorithmComplexity.O_NNlogN

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

        val elementsToAdd = NForAlgorithmComplexity.O_NNlogN

        repeat(times = elementsToAdd) { index ->
            vector = vector.add(index * 2)

            for (i in 0..index) {
                assertEquals(i + index, vector[i])
                vector = vector.set(i, i + index + 1)
                assertEquals(i + index + 1, vector[i])
            }
        }
        repeat(times = elementsToAdd) { index ->
            for (i in 0 until elementsToAdd - index) {
                val expected = elementsToAdd + i

                assertEquals(expected, vector[i])
                vector = vector.set(i, expected - 1)
                assertEquals(expected - 1, vector[i])
            }

            vector = vector.removeAt(0)
        }
    }

    @Test
    fun addAllAtIndexTests() {
        val maxBufferSize = 32

        val listSizes = listOf(0, 1, 10, 31, 32, 33, 64, 65, 100, 1024, 1056, 1057, 10000, 100000).filter {
            it <= NForAlgorithmComplexity.O_NN
        }

        for (initialSize in listSizes) {

            val initialElements = List(initialSize) { it }
            val list = initialElements.fold(persistentListOf<Int>()) { list, element -> list.add(element) }

            val addIndex = mutableListOf(
                    initialSize // append
            )
            if (initialSize > 0) {
                addIndex.add(0) // prepend
                addIndex.add(Random.nextInt(initialSize)) // at random index
            }
            if (initialSize > maxBufferSize) {
                val rootSize = (initialSize - 1) and (maxBufferSize - 1).inv()
                val tailSize = initialSize - rootSize
                addIndex.add(Random.nextInt(maxBufferSize)) // first leaf
                addIndex.add(rootSize + Random.nextInt(tailSize)) // tail
                addIndex.add(rootSize - Random.nextInt(maxBufferSize)) // last leaf
                addIndex.add(rootSize) // after the last leaf
            }

            val addSize = Random.nextInt(maxBufferSize * 2)

            for (index in addIndex) {
                for (size in addSize..(addSize + maxBufferSize)) {

                    val elementsToAdd = List(size) { initialSize + it }
                    val result = list.addAll(index, elementsToAdd)

                    val expected = initialElements.toMutableList().also { it.addAll(index, elementsToAdd) }
                    assertEquals<List<*>>(expected, result)
                }
            }
        }
    }

    @Test
    fun removeAllTests() {
        val maxBufferSize = 32

        val listSizes = listOf(0, 1, 10, 31, 32, 33, 64, 65, 100, 1024, 1056, 1057, 10000, 33000).filter {
            it <= NForAlgorithmComplexity.O_NN
        }

        for (initialSize in listSizes) {

            val initialElements = List(initialSize) { it }
            val list = initialElements.fold(persistentListOf<Int>()) { list, element -> list.add(element) }

            val removeElements = mutableListOf(
                    initialElements // all
            )
            if (initialSize > 0) {
                removeElements.add(emptyList()) // none
                removeElements.add(List(1) { Random.nextInt(initialSize) }) // a random element
                removeElements.add(List(maxBufferSize) { Random.nextInt(initialSize) }) // random elements
                removeElements.add(List(initialSize / 2) { Random.nextInt(initialSize) }) // ~half elements
                removeElements.add(List(initialSize) { Random.nextInt(initialSize) }) // ~more than half elements
                removeElements.add(List(initialSize / 2) { it }) // ~first half
                removeElements.add(List(initialSize / 2) { initialSize - it }) // ~last half
            }
            if (initialSize > maxBufferSize) {
                val rootSize = (initialSize - 1) and (maxBufferSize - 1).inv()
                val tailSize = initialSize - rootSize
                removeElements.add(List(maxBufferSize) { it }) // first leaf
                removeElements.add(List(tailSize) { rootSize + it }) // tail
                removeElements.add(List(maxBufferSize) { rootSize - it }) // last leaf
                for (shift in 5 until 30 step 5) {
                    val branches = 1 shl shift
                    if (branches > rootSize) break
                    removeElements.add(initialElements.shuffled().take(initialSize - rootSize / branches)) // decrease root height
                }
            }

            for (elements in removeElements) {
                val expected = initialElements.toMutableList().also { it.removeAll(elements) }

//                println("${initialElements.size} -> ${expected.size} : ${initialElements.size.toDouble() / expected.size}")

                val result = list.removeAll(elements)

                val resultPredicate = list.let {
                    val hashSet = elements.toHashSet()
                    it.removeAll { e -> hashSet.contains(e) }
                }

                assertEquals<List<*>>(expected, result)
                assertEquals<List<*>>(expected, resultPredicate)
            }
        }
    }

    @Test
    fun randomOperationsTests() {
        repeat(times = 1) {

            val lists = List(20) { mutableListOf<Int>() }
            val vectors = MutableList(20) { persistentListOf<Int>() }

            val operationCount = NForAlgorithmComplexity.O_NlogN

            repeat(times = operationCount) {
                val index = Random.nextInt(lists.size)
                val list = lists[index]
                val vector = vectors[index]

                val operationType = Random.nextDouble()
                val operationIndex = if (list.size > 1) Random.nextInt(list.size) else 0

                val shouldRemove = operationType < 0.15
                val shouldSet = operationType > 0.15 && operationType < 0.3

                val newVector = if (list.isNotEmpty() && shouldRemove) {
                    list.removeAt(operationIndex)
                    vector.removeAt(operationIndex)
                } else if (list.isNotEmpty() && shouldSet) {
                    val value = Random.nextInt()
                    list[operationIndex] = value
                    vector.set(operationIndex, value)
                } else {
                    val value = Random.nextInt()
                    list.add(operationIndex, value)
                    vector.add(operationIndex, value)
                }

                testAfterOperation(list, newVector, operationIndex)

                vectors[index] = newVector
            }

            assertEquals<List<*>>(lists, vectors)

            val maxSize = vectors.maxBy { it.size }?.size
            println("Largest persistent list size: $maxSize")

            lists.forEachIndexed { index, list ->
                var vector = vectors[index]

                while (list.isNotEmpty()) {
                    val removeIndex = Random.nextInt(list.size)
                    list.removeAt(removeIndex)
                    vector = vector.removeAt(removeIndex)

                    testAfterOperation(list, vector, removeIndex)
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

//        assertEquals(list, vector)
    }
}