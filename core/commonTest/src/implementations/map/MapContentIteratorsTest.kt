/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.implementations.map

import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.implementations.immutableMap.TrieNode
import kotlinx.collections.immutable.implementations.immutableMap.TrieNodeKeysIterator
import kotlinx.collections.immutable.implementations.persistentOrderedMap.PersistentOrderedMap
import kotlinx.collections.immutable.implementations.persistentOrderedMap.PersistentOrderedMapBuilder
import kotlinx.collections.immutable.implementations.persistentOrderedMap.PersistentOrderedMapBuilderLinksIterator
import kotlinx.collections.immutable.implementations.persistentOrderedMap.PersistentOrderedMapLinksIterator
import kotlinx.collections.immutable.persistentMapOf
import tests.IntWrapper
import kotlin.test.*

class MapContentIteratorsTest {

    @Test
    fun `trie node keys iterator follows the Iterator contract`() {
        // Production code drives TrieNodeBaseIterator through hasNextKey/moveToNextKey directly;
        // its Iterator-interface hasNext implementation is reachable only by iterating it as is.
        val iterator = TrieNodeKeysIterator<String, Int>()
        iterator.reset(arrayOf<Any?>("a", 1, "b", 2), 4)
        val keys = mutableListOf<String>()
        while (iterator.hasNext()) {
            keys.add(iterator.next())
        }
        assertEquals(listOf("a", "b"), keys)
    }

    @Test
    fun `hash map iterator skips an empty child node`() {
        // An empty child node cannot be produced by map operations; a hand-built trie
        // exercises the iterator's defensive skip over child nodes that contain no data.
        val key = IntWrapper(1, 0)
        val realChild = TrieNode<IntWrapper, Int>(1 shl 0, 0, arrayOf<Any?>(key, 42))
        val root = TrieNode<IntWrapper, Int>(0, 0b11, arrayOf<Any?>(TrieNode.EMPTY, realChild))
        val map = PersistentHashMap(root, 1)

        assertEquals(42, map[key])
        assertEquals(listOf(key to 42), map.entries.map { it.key to it.value })
    }

    @Test
    fun `entries and links iterator of a non-empty ordered map iterate in insertion order`() {
        val map = persistentMapOf("a" to 1, "b" to 2, "c" to 3) as PersistentOrderedMap<String, Int>

        assertEquals(listOf("a" to 1, "b" to 2, "c" to 3), map.entries.map { it.key to it.value })

        val links = PersistentOrderedMapLinksIterator(map.firstKey, map.hashMap)
        assertEquals(0, links.index)
        val values = mutableListOf<Int>()
        while (links.hasNext()) {
            values.add(links.next().value)
        }
        assertEquals(listOf(1, 2, 3), values)
        assertEquals(3, links.index)
        assertFailsWith<NoSuchElementException> { links.next() }
    }

    @Test
    fun `ordered map builder links iterator iterates in order and detects concurrent modification`() {
        val builder = persistentMapOf("a" to 1, "b" to 2, "c" to 3).builder() as PersistentOrderedMapBuilder<String, Int>

        val links = PersistentOrderedMapBuilderLinksIterator(builder.firstKey, builder)
        assertEquals(0, links.index)
        val values = mutableListOf<Int>()
        while (links.hasNext()) {
            values.add(links.next().value)
        }
        assertEquals(listOf(1, 2, 3), values)
        assertEquals(3, links.index)
        assertFailsWith<NoSuchElementException> { links.next() }

        val freshLinks = PersistentOrderedMapBuilderLinksIterator(builder.firstKey, builder)
        builder["d"] = 4
        assertFailsWith<ConcurrentModificationException> { freshLinks.next() }
    }
}
