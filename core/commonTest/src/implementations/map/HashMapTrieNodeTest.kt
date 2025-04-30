/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.implementations.map

import kotlinx.collections.immutable.implementations.immutableMap.LOG_MAX_BRANCHING_FACTOR
import kotlinx.collections.immutable.implementations.immutableMap.MAX_SHIFT
import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.implementations.immutableMap.TrieNode
import tests.IntWrapper
import kotlin.test.*

class HashMapTrieNodeTest {
    private fun testEmptyMap(map: PersistentHashMap<IntWrapper, Int>) {
        map.node.accept { node: TrieNode<IntWrapper, Int>, shift: Int, hash: Int, dataMap: Int, nodeMap: Int ->
            assertEquals(0, shift)
            assertEquals(0, hash)
            assertEquals(0, dataMap)
            assertEquals(0, nodeMap)
            assertTrue(node.buffer.isEmpty())
        }
    }

    @Test
    fun addSingle() {
        var map = PersistentHashMap.emptyOf<IntWrapper, Int>()

        testEmptyMap(map)

        val wrapper1 = IntWrapper(1, 0b100_01101)
        map = map.put(wrapper1, 1)

        map.node.accept { node: TrieNode<IntWrapper, Int>, shift: Int, hash: Int, dataMap: Int, nodeMap: Int ->
            assertEquals(0, shift)
            assertEquals(0, hash)
            assertEquals(1 shl 0b01101, dataMap)
            assertEquals(0b0, nodeMap)
            assertTrue(arrayOf<Any?>(wrapper1, 1) contentEquals node.buffer)
        }

        map = map.remove(wrapper1)

        testEmptyMap(map)
    }

    //                      Remove (1, 1)
    //
    //         ---                          --------
    //        | * |                        | 33, 33 |
    //         -|-                          --------
    //          |                 ->
    //    ---------------
    //   | 1, 1 | 33, 33 |
    //    ---------------
    //
    @Test
    fun canonicalization() {
        val wrapper1 = IntWrapper(1, 0b1)
        val wrapper33 = IntWrapper(33, 0b1_00001)
        val map = PersistentHashMap.emptyOf<IntWrapper, Int>().put(wrapper1, 1).put(wrapper33, 33)

        map.node.accept { node: TrieNode<IntWrapper, Int>, shift: Int, hash: Int, dataMap: Int, nodeMap: Int ->
            if  (shift == 0) {
                assertEquals(0b0, dataMap)
                assertEquals(0b10, nodeMap)
            } else {
                assertEquals(1, hash)
                assertEquals(LOG_MAX_BRANCHING_FACTOR, shift)
                assertEquals(0b11, dataMap)
                assertEquals(0b0, nodeMap)
                assertTrue(arrayOf<Any?>(wrapper1, 1, wrapper33, 33) contentEquals node.buffer)
            }
        }

        map.remove(wrapper1).node.accept { node: TrieNode<IntWrapper, Int>, shift: Int, hash: Int, dataMap: Int, nodeMap: Int ->
            assertEquals(0, shift)
            assertEquals(0, hash)
            assertEquals(0b10, dataMap)
            assertEquals(0b0, nodeMap)
            assertTrue(arrayOf<Any?>(wrapper33, 33) contentEquals node.buffer)
        }

        testEmptyMap(map.remove(wrapper1).remove(wrapper33))
    }

    //                    Remove (1057, 1057)
    //
    //         ---                                  ---
    //        | * |                                | * |
    //         -|-                                  -|-
    //          |                ->                  |
    //     ----------                         ---------------
    //    | 1, 1 | * |                       | 1, 1 | 33, 33 |
    //     --------|-                         ---------------
    //             |
    //   ---------------------
    //  | 33, 33 | 1057, 1057 |
    //   ---------------------
    //
    @Test
    fun canonicalization1() {
        val wrapper1 = IntWrapper(1, 0b1)
        val wrapper33 = IntWrapper(33, 0b1_00001)
        val wrapper1057 = IntWrapper(1057, 0b1_00001_00001)
        val map = PersistentHashMap.emptyOf<IntWrapper, Int>().put(wrapper1, 1).put(wrapper33, 33).put(wrapper1057, 1057)

        map.node.accept { node: TrieNode<IntWrapper, Int>, shift: Int, hash: Int, dataMap: Int, nodeMap: Int ->
            when (shift) {
                0 -> {
                    assertEquals(0b0, dataMap)
                    assertEquals(0b10, nodeMap)
                }
                LOG_MAX_BRANCHING_FACTOR -> {
                    assertEquals(1, hash)
                    assertEquals(0b1, dataMap)
                    assertEquals(0b10, nodeMap)
                    assertTrue(arrayOf<Any?>(wrapper1, 1) contentEquals node.buffer.copyOf(2))
                }
                else -> {
                    assertEquals(33, hash)
                    assertEquals(2 * LOG_MAX_BRANCHING_FACTOR, shift)
                    assertEquals(0b11, dataMap)
                    assertEquals(0b0, nodeMap)
                    assertTrue(arrayOf<Any?>(wrapper33, 33, wrapper1057, 1057) contentEquals node.buffer)
                }
            }
        }

        map.remove(wrapper1057).node.accept { node: TrieNode<IntWrapper, Int>, shift: Int, hash: Int, dataMap: Int, nodeMap: Int ->
            if (shift == 0) {
                assertEquals(0b0, dataMap)
                assertEquals(0b10, nodeMap)
            } else {
                assertEquals(1, hash)
                assertEquals(LOG_MAX_BRANCHING_FACTOR, shift)
                assertEquals(0b11, dataMap)
                assertEquals(0b0, nodeMap)
                assertTrue(arrayOf<Any?>(wrapper1, 1, wrapper33, 33) contentEquals node.buffer)
            }
        }
    }

    //                     Remove (1, 1)                  Remove (1057, 1057)
    //
    //         ---                              ---                              --------
    //        | * |                            | * |                            | 33, 33 |
    //         -|-                              -|-                              --------
    //          |                ->              |                  ->
    //     ----------                           ---
    //    | 1, 1 | * |                         | * |
    //     --------|-                           -|-
    //             |                             |
    //   ---------------------          ---------------------
    //  | 33, 33 | 1057, 1057 |        | 33, 33 | 1057, 1057 |
    //   ---------------------          ---------------------
    //
    @Test
    fun canonicalization2() {
        val wrapper1 = IntWrapper(1, 0b1)
        val wrapper33 = IntWrapper(33, 0b1_00001)
        val wrapper1057 = IntWrapper(1057, 0b1_00001_00001)
        val map = PersistentHashMap.emptyOf<IntWrapper, Int>().put(wrapper1, 1).put(wrapper33, 33).put(wrapper1057, 1057)

        map.remove(wrapper1).node.accept { node: TrieNode<IntWrapper, Int>, shift: Int, hash: Int, dataMap: Int, nodeMap: Int ->
            when (shift) {
                0 -> {
                    assertEquals(0b0, dataMap)
                    assertEquals(0b10, nodeMap)
                }
                LOG_MAX_BRANCHING_FACTOR -> {
                    assertEquals(1, hash)
                    assertEquals(0b0, dataMap)
                    assertEquals(0b10, nodeMap)
                }
                else -> {
                    assertEquals(33, hash)
                    assertEquals(2 * LOG_MAX_BRANCHING_FACTOR, shift)
                    assertEquals(0b11, dataMap)
                    assertEquals(0b0, nodeMap)
                    assertTrue(arrayOf<Any?>(wrapper33, 33, wrapper1057, 1057) contentEquals node.buffer)
                }
            }
        }

        map.remove(wrapper1).remove(wrapper1057).node.accept { node: TrieNode<IntWrapper, Int>, shift: Int, hash: Int, dataMap: Int, nodeMap: Int ->
            assertEquals(0, shift)
            assertEquals(0, hash)
            assertEquals(0b10, dataMap)
            assertEquals(0b0, nodeMap)
            assertTrue(arrayOf<Any?>(wrapper33, 33) contentEquals node.buffer)
        }

        testEmptyMap(map.remove(wrapper1).remove(wrapper1057).remove(wrapper33))
    }

    // Nodes drawn using square braces are collision nodes.
    //
    //                    Remove (1, 1)
    //
    //         ---                        ------
    //        | * |                      | 2, 2 |
    //         -|-                        ------
    //          |              ->
    //          *
    //          *
    //          *
    //          |
    //    -------------
    //   [ 2, 2 | 1, 1 ]
    //    -------------
    //
    @Test
    fun collision() {
        val wrapper1 = IntWrapper(1, 0b1)
        val wrapper2 = IntWrapper(2, 0b1)
        val map = PersistentHashMap.emptyOf<IntWrapper, Int>().put(wrapper1, 1).put(wrapper2, 2)

        map.node.accept { node: TrieNode<IntWrapper, Int>, shift: Int, hash: Int, dataMap: Int, nodeMap: Int ->
            if (shift > MAX_SHIFT) {
                assertEquals(1, hash)
                assertEquals(0b0, dataMap)
                assertEquals(0b0, nodeMap)
                assertEquals(arrayOf<Any?>(wrapper1, 1, wrapper2, 2).map { it }, node.buffer.map { it })
            } else {
                assertEquals(0b0, dataMap)
                val mask = if (shift == 0) 0b10 else 0b1
                assertEquals(mask, nodeMap)
            }
        }

        map.remove(wrapper1).node.accept { node: TrieNode<IntWrapper, Int>, shift: Int, hash: Int, dataMap: Int, nodeMap: Int ->
            assertEquals(0, shift)
            assertEquals(0, hash)
            assertEquals(0b10, dataMap)
            assertEquals(0b0, nodeMap)
            assertTrue(arrayOf(wrapper2, 2) contentEquals node.buffer)
        }

        testEmptyMap(map.remove(wrapper1).remove(wrapper2))
    }

    // Nodes drawn using square braces are collision nodes.
    //
    //                    Remove (1, 1)
    //
    //        ---                              ---
    //       | * |                            | * |
    //        -|-                              -|-
    //         |                ->              |
    //     ----------                     -------------
    //    | 3, 3 | * |                   | 2, 2 | 3, 3 |
    //     --------|-                     -------------
    //             |
    //             *
    //             *
    //             *
    //             |
    //       -------------
    //      [ 2, 2 | 1, 1 ]
    //       -------------
    //
    @Test
    fun collision1() {
        val wrapper1 = IntWrapper(1, 0b1)
        val wrapper2 = IntWrapper(2, 0b1)
        val wrapper3 = IntWrapper(3, 0b1_00001)
        val map = PersistentHashMap.emptyOf<IntWrapper, Int>().put(wrapper1, 1).put(wrapper2, 2).put(wrapper3, 3)

        map.node.accept { node: TrieNode<IntWrapper, Int>, shift: Int, hash: Int, dataMap: Int, nodeMap: Int ->
            when (shift) {
                0 -> {
                    assertEquals(0, hash)
                    assertEquals(0b0, dataMap)
                    assertEquals(0b10, nodeMap)
                }
                LOG_MAX_BRANCHING_FACTOR -> {
                    assertEquals(1, hash)
                    assertEquals(0b10, dataMap)
                    assertEquals(0b1, nodeMap)
                    assertTrue(arrayOf<Any?>(wrapper3, 3) contentEquals node.buffer.copyOf(2))
                }
                in LOG_MAX_BRANCHING_FACTOR + 1..MAX_SHIFT -> {
                    assertEquals(1, hash)
                    assertEquals(0b0, dataMap)
                    assertEquals(0b1, nodeMap)
                }
                else -> {
                    assertEquals(1, hash)
                    assertEquals(0b0, dataMap)
                    assertEquals(0b0, nodeMap)
                    assertEquals(arrayOf<Any?>(wrapper1, 1, wrapper2, 2).map { it }, node.buffer.map { it })
                }
            }
        }

        map.remove(wrapper1).node.accept { node: TrieNode<IntWrapper, Int>, shift: Int, hash: Int, dataMap: Int, nodeMap: Int ->
            if (shift == 0) {
                assertEquals(0b0, dataMap)
                assertEquals(0b10, nodeMap)
            } else {
                assertEquals(LOG_MAX_BRANCHING_FACTOR, shift)
                assertEquals(1, hash)
                assertEquals(0b11, dataMap)
                assertEquals(0b0, nodeMap)
                assertTrue(arrayOf(wrapper2, 2, wrapper3, 3) contentEquals node.buffer)
            }
        }
    }

    // Nodes drawn using square braces are collision nodes.
    //
    //                    Remove (3, 3)                    Remove (1, 1)
    //
    //        ---                               ---                         ------
    //       | * |                             | * |                       | 2, 2 |
    //        -|-                               -|-                         ------
    //         |               ->                |              ->
    //     ----------                           ---
    //    | 3, 3 | * |                         | * |
    //     --------|-                           -|-
    //             |                             |
    //             *                             *
    //             *                             *
    //             *                             *
    //             |                             |
    //       -------------                 -------------
    //      [ 2, 2 | 1, 1 ]               [ 2, 2 | 1, 1 ]
    //       -------------                 -------------
    //
    @Test
    fun collision2() {
        val wrapper1 = IntWrapper(1, 0b1)
        val wrapper2 = IntWrapper(2, 0b1)
        val wrapper3 = IntWrapper(3, 0b1_00001)
        val map = PersistentHashMap.emptyOf<IntWrapper, Int>().put(wrapper1, 1).put(wrapper2, 2).put(wrapper3, 3)

        map.remove(wrapper3).node.accept { node: TrieNode<IntWrapper, Int>, shift: Int, hash: Int, dataMap: Int, nodeMap: Int ->
            when (shift) {
                in 0..MAX_SHIFT -> {
                    val code = if (shift == 0) 0 else 1
                    assertEquals(code, hash)
                    assertEquals(0b0, dataMap)
                    val mask = if (shift == 0) 0b10 else 0b1
                    assertEquals(mask, nodeMap)
                }
                else -> {
                    assertEquals(1, hash)
                    assertEquals(0b0, dataMap)
                    assertEquals(0b0, nodeMap)
                    assertTrue(arrayOf<Any?>(wrapper1, 1, wrapper2, 2) contentEquals node.buffer)
                }
            }
        }

        map.remove(wrapper3).remove(wrapper1).node.accept { node: TrieNode<IntWrapper, Int>, shift: Int, hash: Int, dataMap: Int, nodeMap: Int ->
            assertEquals(0, shift)
            assertEquals(0, hash)
            assertEquals(0b10, dataMap)
            assertEquals(0b0, nodeMap)
            assertTrue(arrayOf<Any?>(wrapper2, 2) contentEquals node.buffer)
        }
    }

    // TODO: Investigate performance impact of converting single-entry collision root node to compact node. See the following commented test:
/*
    // Nodes drawn using square braces are collision nodes.
    //
    //                    Remove (1, 1)
    //
    //    -------------                   ------
    //   [ 2, 2 | 1, 1 ]        ->       | 2, 2 |
    //    -------------                   ------
    //
    @Test
    fun collision() {
        val wrapper1 = IntWrapper(1, 0b1)
        val wrapper2 = IntWrapper(2, 0b1)
        val map = PersistentHashMap.emptyOf<IntWrapper, Int>().put(wrapper1, 1).put(wrapper2, 2)

        map.node.accept { node: TrieNode<IntWrapper, Int>, shift: Int, hash: Int, dataMap: Int, nodeMap: Int ->
            assertTrue(node.isCollision())
            assertTrue(arrayOf(wrapper2, 2, wrapper1, 1) contentEquals node.buffer)
        }

        map.remove(wrapper1).node.accept { node: TrieNode<IntWrapper, Int>, shift: Int, hash: Int, dataMap: Int, nodeMap: Int ->
            assertEquals(0, shift)
            assertFalse(node.isCollision())
            assertEquals(0b10, dataMap)
            assertEquals(0b0, nodeMap)
            assertTrue(arrayOf(wrapper2, 2) contentEquals node.buffer)
        }

        testEmptyMap(map.remove(wrapper1).remove(wrapper2))
    }
*/
    // TODO: Investigate performance impact of upping collision nodes that have empty parents. See the following commented test:
/*
    // Nodes drawn using square braces are collision nodes.
    //
    //                    Remove (3, 3)
    //
    //        ---
    //       | * |                       -------------
    //        -|-                       [ 2, 2 | 1, 1 ]
    //         |               ->        -------------
    //     ----------
    //    | 3, 3 | * |
    //     --------|-
    //             |
    //       -------------
    //      [ 2, 2 | 1, 1 ]
    //       -------------
    //
    @Test
    fun collision2() {
        val wrapper1 = IntWrapper(1, 0b1)
        val wrapper2 = IntWrapper(2, 0b1)
        val wrapper3 = IntWrapper(3, 0b1_00001)
        val map = PersistentHashMap.emptyOf<IntWrapper, Int>().put(wrapper1, 1).put(wrapper2, 2).put(wrapper3, 3)

        map.node.accept { node: TrieNode<IntWrapper, Int>, shift: Int, hash: Int, dataMap: Int, nodeMap: Int ->
            when (shift) {
                0 -> {
                    assertEquals(0b0, dataMap)
                    assertEquals(0b10, nodeMap)
                }
                LOG_MAX_BRANCHING_FACTOR -> {
                    assertEquals(1, hash)
                    assertEquals(0b10, dataMap)
                    assertEquals(0b1, nodeMap)
                    assertTrue(arrayOf<Any?>(wrapper3, 3) contentEquals node.buffer.copyOf(2))
                }
                else -> {
                    assertEquals(1, hash)
                    assertEquals(2 * LOG_MAX_BRANCHING_FACTOR, shift)
                    assertTrue(node.isCollision())
                    assertTrue(arrayOf<Any?>(wrapper2, 2, wrapper1, 1) contentEquals node.buffer)
                }
            }
        }

        map.remove(wrapper3).node.accept { node: TrieNode<IntWrapper, Int>, shift: Int, hash: Int, dataMap: Int, nodeMap: Int ->
            assertEquals(0, shift)
            assertTrue(node.isCollision())
            assertTrue(arrayOf(wrapper2, 2, wrapper1, 1) contentEquals node.buffer)
        }
    }
*/
}