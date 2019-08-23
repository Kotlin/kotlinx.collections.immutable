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

import kotlinx.collections.immutable.internal.assert
import kotlin.math.pow
import kotlin.test.*

class TrieIteratorTest {
    @Test
    fun emptyIteratorTest() {
        val emptyIterator = TrieIterator<Int>(emptyArray(), 0, 0, 1)

        assertFalse(emptyIterator.hasNext())
        assertFailsWith<NoSuchElementException> {
            emptyIterator.next()
        }
    }

    private fun makeRoot(height: Int, leafCount: Int): Array<Any?> {
        var leaves = arrayListOf<Any?>()
        repeat(leafCount * MAX_BUFFER_SIZE) { it ->
            leaves.add(it)
        }

        repeat(height) {
            val newLeaves = arrayListOf<Any?>()
            for (i in 0 until leaves.size step MAX_BUFFER_SIZE) {
                val buffer = arrayOfNulls<Any?>(MAX_BUFFER_SIZE)
                for (j in i until minOf(leaves.size, i + MAX_BUFFER_SIZE)) {
                    buffer[j - i] = leaves[j]
                }
                newLeaves.add(buffer)
            }
            leaves = newLeaves
        }

        assert(leaves.size == 1)
        return leaves[0] as Array<Any?>
    }

    @Test
    fun simpleTest() {
        var lastStop = 1
        for (height in 1..7) {
            for (leafCount in lastStop..(1 shl 12) step height) {
                if (MAX_BUFFER_SIZE.toDouble().pow(height - 1) < leafCount) {
                    lastStop = leafCount
                    break
                }

                val root = makeRoot(height, leafCount)
                val size = leafCount * MAX_BUFFER_SIZE

                val iterator = TrieIterator<Int>(root, 0, size, height)
                repeat(size) { it ->
                    assertTrue(iterator.hasNext())
                    assertEquals(it, iterator.next())
                }

                assertFalse(iterator.hasNext())
                assertFailsWith<NoSuchElementException> {
                    iterator.next()
                }
            }
        }
    }
}