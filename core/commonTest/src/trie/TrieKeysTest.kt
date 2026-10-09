/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.trie

import kotlinx.collections.immutable.implementations.immutableMap.LOG_MAX_BRANCHING_FACTOR
import kotlinx.collections.immutable.implementations.immutableMap.MAX_SHIFT
import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.implementations.immutableMap.TrieNode
import kotlinx.collections.immutable.implementations.immutableMap.indexSegment
import kotlinx.collections.immutable.persistentHashMapOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The level reported for a key held in a collision node, under the last level. */
private const val COLLISION = LAST_LEVEL + 1

/** Walks the trie of [map] by the hash of [key] and returns the level of the node holding it. */
private fun levelOf(map: PersistentHashMap<*, *>, key: Any): Int {
    var node: TrieNode<*, *> = map.node
    var shift = 0
    while (shift <= MAX_SHIFT) {
        val positionMask = 1 shl indexSegment(key.hashCode(), shift)
        if (node.hasEntryAt(positionMask)) {
            assertEquals(key, node.keyAtIndex(node.entryKeyIndex(positionMask)))
            return shift / LOG_MAX_BRANCHING_FACTOR
        }
        assertTrue(node.hasNodeAt(positionMask), "$key is not in the trie")
        node = node.nodeAtIndex(node.nodeIndex(positionMask))
        shift += LOG_MAX_BRANCHING_FACTOR
    }
    assertTrue(node.buffer.any { it == key }, "$key is not in the collision node")
    return COLLISION
}

@Suppress("UNCHECKED_CAST")
private fun trieOf(keys: List<Any>): PersistentHashMap<Any, String> =
    persistentHashMapOf(*keys.map { it to "" }.toTypedArray()) as PersistentHashMap<Any, String>

class TrieKeysTest {

    @Test
    fun `the fixtures form the drawn trie`() {
        val levels = mapOf(
            collidingKey1 to COLLISION, collidingKey2 to COLLISION, collidingKey3 to COLLISION,
            lastLevelSibling to LAST_LEVEL, levelTwoSibling to 2,
            levelOneSibling to 1, otherLevelOneSibling to 1,
            rootSibling to 0,
        )
        val map = trieOf(levels.keys.toList())

        assertEquals(levels, levels.keys.associateWith { levelOf(map, it) })
    }

    @Test
    fun `the shapes form the tries they are named after`() {
        val levels = mapOf(
            "int keys" to listOf(0, 0, 0),
            "colliding keys" to listOf(COLLISION, COLLISION, COLLISION),
            "a collision node next to a last-level sibling" to listOf(COLLISION, COLLISION, LAST_LEVEL),
            "a collision node next to a root entry" to listOf(0, COLLISION, COLLISION),
            "a level-1 node next to root entries" to listOf(0, 0, 0, 1, 1),
            "a level-2 node under a level-1 node" to listOf(2, 2, 1, 0),
            "two collision nodes under a level-1 node" to listOf(COLLISION, COLLISION, COLLISION, COLLISION),
        )
        assertEquals(levels.keys, trieShapes.keys)

        for ((shape, keys) in trieShapes) {
            assertEquals(levels.getValue(shape), keys.map { levelOf(trieOf(keys), it) }, shape)
        }
    }
}
