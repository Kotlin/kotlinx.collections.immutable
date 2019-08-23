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