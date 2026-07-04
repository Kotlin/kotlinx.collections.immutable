/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.implementations.map

import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.implementations.immutableMap.TrieNode
import tests.IntWrapper
import kotlin.test.*

/**
 * A node holding a single entry never appears as a sub-node through the public API:
 * all map operations eagerly promote such a node into an entry of its parent.
 * The hand-built tries below exercise the defensive canonicalization in the remove
 * operations, which drops a child node whose content collapses to nothing.
 */
class HashMapTrieNodeExtraTest {

    private val innerKey = IntWrapper(1, 0) // level-0 and level-1 hash segments are 0
    private val topKey = IntWrapper(2, 1)   // level-0 hash segment is 1: stays an entry in the root

    private fun singleEntryChild() = TrieNode<IntWrapper, Int>(1 shl 0, 0, arrayOf<Any?>(innerKey, 100))

    @Test
    fun `removing the only entry of a sub-node drops the whole node`() {
        val root = TrieNode<IntWrapper, Int>(1 shl 1, 1 shl 0, arrayOf<Any?>(topKey, 200, singleEntryChild()))
        val map = PersistentHashMap(root, 2)
        assertEquals(100, map[innerKey])
        assertEquals(200, map[topKey])

        val removed = map.removing(innerKey)
        assertEquals(1, removed.size)
        assertNull(removed[innerKey])
        assertEquals(200, removed[topKey])

        // a root whose only content is such a sub-node collapses to the empty map
        val chainRoot = TrieNode<IntWrapper, Int>(0, 1 shl 0, arrayOf<Any?>(singleEntryChild()))
        val emptied = PersistentHashMap(chainRoot, 1).removing(innerKey)
        assertSame(PersistentHashMap.emptyOf<IntWrapper, Int>(), emptied)
    }

    @Test
    fun `builder removing the only entry of a sub-node drops the whole node`() {
        // the builder does not own the root: the node is removed by copying the root buffer
        val builder1 = PersistentHashMap(TrieNode<IntWrapper, Int>(1 shl 1, 1 shl 0, arrayOf<Any?>(topKey, 200, singleEntryChild())), 2).builder()
        assertEquals(100, builder1.remove(innerKey))
        assertEquals(1, builder1.size)
        assertNull(builder1[innerKey])
        assertEquals(200, builder1[topKey])

        // the builder owns the root after the first modification: the node is removed in place
        val builder2 = PersistentHashMap(TrieNode<IntWrapper, Int>(1 shl 1, 1 shl 0, arrayOf<Any?>(topKey, 200, singleEntryChild())), 2).builder()
        builder2[topKey] = 999
        assertEquals(100, builder2.remove(innerKey))
        assertEquals(1, builder2.size)
        assertNull(builder2[innerKey])
        assertEquals(999, builder2[topKey])
        assertEquals<Map<IntWrapper, Int>>(mapOf(topKey to 999), builder2.build())

        // a root whose only content is such a sub-node collapses to an empty builder
        val builder3 = PersistentHashMap(TrieNode<IntWrapper, Int>(0, 1 shl 0, arrayOf<Any?>(singleEntryChild())), 1).builder()
        assertEquals(100, builder3.remove(innerKey))
        assertTrue(builder3.isEmpty())
        assertEquals(0, builder3.build().size)
    }
}
