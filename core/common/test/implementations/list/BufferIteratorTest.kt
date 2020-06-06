/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.implementations.list

import kotlinx.collections.immutable.implementations.immutableList.*
import kotlin.test.*

class BufferIteratorTest {
    @Test
    fun emptyBufferIteratorTest() {
        val emptyIterator = BufferIterator<Int>(emptyArray(), 0, 0)

        assertFalse(emptyIterator.hasNext())
        assertFailsWith<NoSuchElementException> { emptyIterator.next() }
    }

    @Test
    fun simpleTest() {
        val bufferIterator = BufferIterator(arrayOf(1, 2, 3, 4, 5), 0, 5)

        repeat(times = 5) { it ->
            assertTrue(bufferIterator.hasNext())
            assertEquals(it + 1, bufferIterator.next())
        }

        assertFalse(bufferIterator.hasNext())
        assertFailsWith<NoSuchElementException> { bufferIterator.next() }
    }

    @Test
    fun biggerThanSizeBufferTest() {
        val bufferIterator = BufferIterator(arrayOf(1, 2, 3, 4, 5), 0, 3)

        repeat(times = 3) { it ->
            assertTrue(bufferIterator.hasNext())
            assertEquals(it + 1, bufferIterator.next())
        }

        assertFalse(bufferIterator.hasNext())
        assertFailsWith<NoSuchElementException> { bufferIterator.next() }
    }
}