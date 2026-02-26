/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.list

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import tests.NForAlgorithmComplexity
import tests.TestPlatform
import tests.contract.compare
import tests.contract.listIteratorProperties
import tests.distinctStringValues
import tests.stress.ExecutionTimeMeasuringTest
import tests.testOn
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.test.*

class PersistentListBuilderTest : ExecutionTimeMeasuringTest() {

    @Test
    fun isEmptyTests() {
        val builder = persistentListOf<String>().builder()

        assertTrue(builder.isEmpty())

        val elementsToAdd = NForAlgorithmComplexity.O_N

        val elements = distinctStringValues(elementsToAdd)
        repeat(times = elementsToAdd) { index ->
            builder.add(elements[index])
            check(!builder.isEmpty())
        }
        repeat(times = elementsToAdd - 1) {
            builder.removeAt(builder.size - 1)
            check(!builder.isEmpty())
        }
        builder.removeAt(builder.size - 1)
        assertTrue(builder.isEmpty())
    }

    @Test
    fun sizeTests() {
        val builder = persistentListOf<Int>().builder()

        assertTrue(builder.size == 0)

        val elementsToAdd = NForAlgorithmComplexity.O_N

        repeat(times = elementsToAdd) { index ->
            builder.add(index)
            check(index + 1 == builder.size)
        }
        repeat(times = elementsToAdd) { index ->
            builder.removeAt(builder.size - 1)
            check(elementsToAdd - index - 1 == builder.size)
        }
    }


    @Test
    fun firstTests() {
        val builder = persistentListOf<Int>().builder()

        assertNull(builder.firstOrNull())

        val elementsToAdd = NForAlgorithmComplexity.O_NN

        repeat(times = elementsToAdd) { index ->
            builder.add(0, index)
            check(index == builder.first())
        }
        repeat(times = elementsToAdd) { index ->
            check(elementsToAdd - index - 1 == builder.first())
            builder.removeAt(0)
        }
        assertNull(builder.firstOrNull())
    }

    @Test
    fun lastTests() {
        val builder = persistentListOf<Int>().builder()

        assertNull(builder.lastOrNull())

        val elementsToAdd = NForAlgorithmComplexity.O_N

        repeat(times = elementsToAdd) { index ->
            builder.add(index)
            check(index == builder.last())
        }
        repeat(times = elementsToAdd) { index ->
            check(elementsToAdd - index - 1 == builder.last())
            builder.removeAt(builder.size - 1)
        }
        assertNull(builder.lastOrNull())
    }

    @Test
    fun toListTest() {
        val builder = persistentListOf<Int>().builder()

        assertEquals(emptyList<Int>(), builder)

        val elementsToAdd = NForAlgorithmComplexity.O_NN

        val list = mutableListOf<Int>()
        repeat(times = elementsToAdd) { index ->
            list.add(index)
            builder.add(index)
            check(list == builder.toList())
        }
    }


    @Test
    fun addFirstTests() {
        val builder = persistentListOf<Int>().builder()

        assertNull(builder.firstOrNull())

        val elementsToAdd = NForAlgorithmComplexity.O_NN

        val allElements = List(elementsToAdd) { elementsToAdd - it - 1 }
        repeat(times = elementsToAdd) { index ->
            builder.add(0, index)

            check(index == builder.first())
            check(0 == builder.last())
            check(index + 1 == builder.size)

            val expectedContent = allElements.subList(elementsToAdd - builder.size, elementsToAdd)
            check(expectedContent == builder)
        }
    }

    @Test
    fun addLastTests() {
        val builder = persistentListOf<Int>().builder()

        val elementsToAdd = NForAlgorithmComplexity.O_NN

        val allElements = List(elementsToAdd) { it }
        repeat(times = elementsToAdd) { index ->
            builder.add(index)

            check(0 == builder[0])
            check(index == builder[index])
            check(index + 1 == builder.size)
            check(allElements.subList(0, builder.size) == builder)
        }
    }


    @Test
    fun removeFirstTests() {
        val builder = persistentListOf<Int>().builder()

        assertFailsWith<IndexOutOfBoundsException> { builder.removeAt(0) }

        val elementsToAdd = NForAlgorithmComplexity.O_NN

        val allElements = List(elementsToAdd) { it }
        repeat(times = elementsToAdd) { index ->
            builder.add(index)
        }
        repeat(times = elementsToAdd) { index ->
            check(elementsToAdd - 1 == builder.last())
            check(index == builder.first())
            check(elementsToAdd - index == builder.size)
            check(allElements.subList(index, elementsToAdd) == builder)

            builder.removeAt(0)
        }
    }

    @Test
    fun removeLastTests() {
        val builder = persistentListOf<Int>().builder()

        assertFailsWith<IndexOutOfBoundsException> {
            builder.removeAt(builder.size - 1)
        }

        val elementsToAdd = NForAlgorithmComplexity.O_NN

        val allElements = List(elementsToAdd) { elementsToAdd - it - 1 }
        repeat(times = elementsToAdd) { index ->
            builder.add(0, index)
        }
        repeat(times = elementsToAdd) { index ->
            check(index == builder.last())
            check(elementsToAdd - 1 == builder.first())
            check(elementsToAdd - index == builder.size)
            check(allElements.subList(0, builder.size) == builder)

            builder.removeAt(builder.size - 1)
        }

        val linear = NForAlgorithmComplexity.O_N

        repeat(times = linear) { index ->
            builder.add(index)
        }
        repeat(times = linear) { index ->
            check(linear - 1 - index == builder.last())
            check(0 == builder.first())
            check(linear - index == builder.size)

            builder.removeAt(builder.size - 1)
        }
    }

    @Test
    fun getTests() {
        val builder = persistentListOf<Int>().builder()

        assertFailsWith<IndexOutOfBoundsException> {
            builder[0]
        }

        val elementsToAdd = NForAlgorithmComplexity.O_NNlogN

        repeat(times = elementsToAdd) { index ->
            builder.add(index)

            for (i in 0..index) {
                check(i == builder[i])
            }
        }
        repeat(times = elementsToAdd) { index ->
            for (i in index until elementsToAdd) {
                check(i == builder[i - index])
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

        val elementsToAdd = NForAlgorithmComplexity.O_NNlogN

        repeat(times = elementsToAdd) { index ->
            builder.add(index * 2)

            for (i in 0..index) {
                check(i + index == builder[i])
                builder[i] = i + index + 1
                check(i + index + 1 == builder[i])
            }
        }
        repeat(times = elementsToAdd) { index ->
            for (i in 0 until elementsToAdd - index) {
                val expected = elementsToAdd + i

                check(expected == builder[i])
                builder[i] = expected - 1
                check(expected - 1 == builder[i])
            }

            builder.removeAt(0)
        }
    }

    @Test
    fun subListTests() {
        val builder = persistentListOf<Int>().builder()

        val elementsToAdd = NForAlgorithmComplexity.O_N
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
        testOn(TestPlatform.JVM) {
            assertFailsWith<ConcurrentModificationException> {
                subList.add(0)
            }
        }
    }

    @Suppress("TestFunctionName")
    private fun <E> PersistentList(size: Int, producer: (Int) -> E): PersistentList<E> {
        var list = persistentListOf<E>()
        repeat(times = size) { index ->
            list = list.add(producer(index))
        }
        return list
    }

    private fun <E> iterateWith(expectedIterator: MutableListIterator<E>,
                                actualIterator: MutableListIterator<E>,
                                maxIterationCount: Int,
                                afterIteration: () -> Unit) {
        val towardStart = Random.nextBoolean()
        val iterationCount = Random.nextInt(0..maxIterationCount)

        if (towardStart) {
            repeat(iterationCount) {
                if (!expectedIterator.hasPrevious()) return
                check(expectedIterator.previous() == actualIterator.previous())
                afterIteration()
                compare(expectedIterator, actualIterator) { listIteratorProperties() }
            }
        } else {
            repeat(iterationCount) {
                if (!expectedIterator.hasNext()) return
                check(expectedIterator.next() == actualIterator.next())
                afterIteration()
                compare(expectedIterator, actualIterator) { listIteratorProperties() }
            }
        }
    }

    @Test
    fun iterationTests() {
        val elementsToAdd = NForAlgorithmComplexity.O_NlogN

        val list = PersistentList(elementsToAdd) { it }
        val builder = list.builder()
        val expected = list.toMutableList()

        var builderIterator = builder.listIterator()
        var expectedIterator = expected.listIterator()
        compare(expectedIterator, builderIterator) { listIteratorProperties() }

        repeat(times = 100) {
            val createNew = Random.nextDouble() < 0.2
            if (createNew) {
                val index = Random.nextInt(expected.size)
                builderIterator = builder.listIterator(index)
                expectedIterator = expected.listIterator(index)
                compare(expectedIterator, builderIterator) { listIteratorProperties() }
            }

            val maxIterationCount = expected.size / 3
            iterateWith(expectedIterator, builderIterator, maxIterationCount) { /* Do nothing after iteration */ }
        }
    }

    @Test
    fun iteratorSetTests() {
        val elementsToAdd = NForAlgorithmComplexity.O_NlogN

        val list = PersistentList(elementsToAdd) { it }
        val builder = list.builder()
        val expected = list.toMutableList()

        var builderIterator = builder.listIterator()
        var expectedIterator = expected.listIterator()
        compare(expectedIterator, builderIterator) { listIteratorProperties() }

        repeat(times = 100) {
            val createNew = Random.nextDouble() < 0.1
            if (createNew) {
                val index = Random.nextInt(expected.size)
                builderIterator = builder.listIterator(index)
                expectedIterator = expected.listIterator(index)
                compare(expectedIterator, builderIterator) { listIteratorProperties() }
            }

            val maxIterationCount = expected.size / 3
            iterateWith(expectedIterator, builderIterator, maxIterationCount) {
                val shouldSet = Random.nextBoolean()
                if (shouldSet) {
                    val elementToSet = Random.nextInt()
                    expectedIterator.set(elementToSet)
                    builderIterator.set(elementToSet)
                }
            }
        }
    }

    @Test
    fun iteratorAddTests() {
        val elementsToAdd = NForAlgorithmComplexity.O_NNlogN

        val list = PersistentList(elementsToAdd) { it }
        val builder = list.builder()
        val expected = list.toMutableList()

        var builderIterator = builder.listIterator(builder.size)
        var expectedIterator = expected.listIterator(builder.size)
        compare(expectedIterator, builderIterator) { listIteratorProperties() }

        repeat(times = 100) {
            val createNew = Random.nextDouble() < 0.1
            if (createNew) {
                val index = Random.nextInt(expected.size)
                builderIterator = builder.listIterator(index)
                expectedIterator = expected.listIterator(index)
                compare(expectedIterator, builderIterator) { listIteratorProperties() }
            }

            val shouldAdd = Random.nextBoolean()
            if (shouldAdd) {
                val addCount = Random.nextInt(1000)
                repeat(addCount) {
                    val elementToAdd = Random.nextInt()
                    expectedIterator.add(elementToAdd)
                    builderIterator.add(elementToAdd)
                    compare(expectedIterator, builderIterator) { listIteratorProperties() }
                }
            } else {
                val maxIterationCount = expected.size / 3
                iterateWith(expectedIterator, builderIterator, maxIterationCount) { /* Do nothing after iteration */ }
            }
        }
    }

    @Test
    fun iteratorRemoveTests() {
        val elementsToAdd = NForAlgorithmComplexity.O_NNlogN

        val list = PersistentList(elementsToAdd) { it }
        val builder = list.builder()
        val expected = list.toMutableList()

        var builderIterator = builder.listIterator()
        var expectedIterator = expected.listIterator()
        compare(expectedIterator, builderIterator) { listIteratorProperties() }

        repeat(times = 100) {
            val createNew = Random.nextDouble() < 0.1
            if (createNew) {
                val index = Random.nextInt(0..expected.size)
                builderIterator = builder.listIterator(index)
                expectedIterator = expected.listIterator(index)
                compare(expectedIterator, builderIterator) { listIteratorProperties() }
            }

            val shouldAddOrRemove = Random.nextBoolean()
            if (shouldAddOrRemove) {
                val actionCount = Random.nextInt(1000)
                val shouldAdd = Random.nextBoolean()

                if (shouldAdd) {
                    repeat(actionCount) {
                        val elementToAdd = Random.nextInt()
                        expectedIterator.add(elementToAdd)
                        builderIterator.add(elementToAdd)
                        compare(expectedIterator, builderIterator) { listIteratorProperties() }
                    }
                } else {
                    iterateWith(expectedIterator, builderIterator, actionCount) {
                        expectedIterator.remove()
                        builderIterator.remove()
                        compare(expectedIterator, builderIterator) { listIteratorProperties() }
                    }
                }
            } else {
                val maxIterationCount = expected.size / 3 + 1
                iterateWith(expectedIterator, builderIterator, maxIterationCount) { /* Do nothing after iteration */ }
            }
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
                    val builder = list.builder().also { it.addAll(index, elementsToAdd) }

                    val expected = initialElements.toMutableList().also { it.addAll(index, elementsToAdd) }
                    check(expected == builder)
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
                removeElements.add(List(initialSize) { Random.nextInt(initialSize) }) // ~all elements
            }
            if (initialSize > maxBufferSize) {
                val rootSize = (initialSize - 1) and (maxBufferSize - 1).inv()
                val tailSize = initialSize - rootSize
                removeElements.add(List(maxBufferSize) { it }) // first leaf
                removeElements.add(List(tailSize) { rootSize + it }) // tail
                removeElements.add(List(maxBufferSize) { rootSize - it }) // last leaf
            }

            for (elements in removeElements) {
                val expected = initialElements.toMutableList().also { it.removeAll(elements) }

                val builder = list.builder().also { it.removeAll(elements) }

                val builderPredicate = list.builder().also {
                    val hashSet = elements.toHashSet()
                    it.removeAll { e -> hashSet.contains(e) }
                }

                check(expected == builder)
                check(expected == builderPredicate)
            }
        }
    }

    @Test
    fun randomOperationsTests() {
        val vectorGen = mutableListOf(List(20) { persistentListOf<Int>() })
        val expected = mutableListOf(List(20) { listOf<Int>() })

        repeat(times = 5) {

            val builders = vectorGen.last().map { it.builder() }
            val lists = builders.map { it.toMutableList() }

            val operationCount = NForAlgorithmComplexity.O_NlogN

            repeat(times = operationCount) {
                val index = Random.nextInt(lists.size)
                val list = lists[index]
                val builder = builders[index]

                val operationType = Random.nextDouble()
                val operationIndex = if (list.size > 1) Random.nextInt(list.size) else 0

                val shouldRemove = operationType < 0.15
                val shouldSet = operationType > 0.15 && operationType < 0.3

                if (list.isNotEmpty() && shouldRemove) {
                    check(
                            list.removeAt(operationIndex) ==
                            builder.removeAt(operationIndex)
                    )
                } else if (list.isNotEmpty() && shouldSet) {
                    val value = Random.nextInt()
                    check(
                            list.set(operationIndex, value) ==
                            builder.set(operationIndex, value)
                    )

                } else {
                    val value = Random.nextInt()
                    list.add(operationIndex, value)
                    builder.add(operationIndex, value)
                }

                testAfterOperation(list, builder, operationIndex)
            }

            assertEquals(lists, builders)

            vectorGen.add( builders.map { it.build() } )
            expected.add(lists)

            val maxSize = builders.maxOf { it.size }
            println("Largest persistent list builder size: $maxSize")
        }

        vectorGen.forEachIndexed { genIndex, vectors ->
            // assert that builders didn't modify vectors they were created from.
            vectors.forEachIndexed { vectorIndex, vector ->
                val expectedList = expected[genIndex][vectorIndex]
                assertEquals(
                        expectedList,
                        vector,
                        message = "The persistent list of $genIndex generation was modified.\nExpected: $expectedList\nActual: $vector"
                )
            }
        }
    }

    private fun testAfterOperation(list1: List<Int>, list2: List<Int>, operationIndex: Int) {
        check(list1.firstOrNull() == list2.firstOrNull())
        check(list1.lastOrNull() == list2.lastOrNull())
        check(list1.size == list2.size)
        if (operationIndex < list1.size) {
            check(list1[operationIndex] == list2[operationIndex])
        }
        if (operationIndex > 0) {
            check(list1[operationIndex - 1] == list2[operationIndex - 1])
        }
        if (operationIndex + 1 < list1.size) {
            check(list1[operationIndex + 1] == list2[operationIndex + 1])
        }

//        check(list1 == list2)
    }
}