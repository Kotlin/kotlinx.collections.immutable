/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.implementations.set

import kotlinx.collections.immutable.implementations.immutableSet.PersistentHashSet
import kotlinx.collections.immutable.implementations.immutableSet.TrieNode
import tests.IntWrapper
import kotlin.test.*

class SetContentIteratorsTest {

    @Test
    fun `hash set iterator skips an empty child node`() {
        // An empty child node cannot be produced by set operations; a hand-built trie
        // exercises the iterator's defensive skip over child nodes that contain no data.
        // Mirrors the hash-map test of the same name: PersistentHashSetIterator has the
        // identical recovery in ensureNextElementIsReady.
        val element = IntWrapper(1, 1)
        val root = TrieNode<IntWrapper>(0b11, arrayOf<Any?>(TrieNode.EMPTY, element))
        val set = PersistentHashSet(root, 1)

        assertTrue(set.contains(element))
        assertEquals(listOf(element), set.toList())
    }
}
