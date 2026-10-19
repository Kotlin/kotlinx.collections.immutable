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

private const val COLLISION = LAST_LEVEL + 1

/** Each fixture and the level it takes when they are all stored together. */
private val fixtureLevels = listOf(
    collidingKey1 to COLLISION, collidingKey2 to COLLISION, collidingKey3 to COLLISION, lastLevelSibling to LAST_LEVEL,
    levelTwoSibling to 2, levelOneSibling to 1, otherLevelOneSibling to 1, rootSibling to 0,
)

/** The level each key takes in the trie of all of them, [COLLISION] in a collision node; a key not found there fails. */
private fun levelsOf(keys: List<Any?>): List<Int> {
    val map = persistentHashMapOf(*keys.map { it to "" }.toTypedArray()) as PersistentHashMap<*, *>
    return keys.map { key ->
        var node: TrieNode<*, *> = map.node
        var shift = 0
        while (shift <= MAX_SHIFT) {
            val positionMask = 1 shl indexSegment(key.hashCode(), shift)
            if (node.hasEntryAt(positionMask)) {
                assertEquals(key, node.keyAtIndex(node.entryKeyIndex(positionMask)))
                return@map shift / LOG_MAX_BRANCHING_FACTOR
            }
            assertTrue(node.hasNodeAt(positionMask), "$key is not in the trie")
            node = node.nodeAtIndex(node.nodeIndex(positionMask))
            shift += LOG_MAX_BRANCHING_FACTOR
        }
        assertTrue(node.buffer.any { it == key }, "$key is not in the collision node")
        COLLISION
    }
}

/** The fixtures sit at the levels their names give, and each shape forms the trie it is named after. */
class TrieKeysTest {

    @Test
    fun `the fixtures stored together sit at the levels of their names`() {
        val (fixtures, levels) = fixtureLevels.unzip()
        assertEquals(levels, levelsOf(fixtures))
    }

    @Test
    fun `a sibling keeps the path of its key above its level`() {
        assertEquals(listOf(2, 2), levelsOf(listOf(levelOneSibling, levelOneSibling.siblingAt(level = 2, id = 10))))
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
            "a null key in a collision node and a NaN key" to listOf(COLLISION, COLLISION, 3),
        )
        assertEquals(levels.keys, trieShapes.keys)
        for ((shape, keys) in trieShapes) assertEquals(levels.getValue(shape), levelsOf(keys), shape)
        assertTrue(trieShapes.getValue("a null key in a collision node and a NaN key").containsAll(listOf(null, Double.NaN)))
    }

    @Test
    fun `no two keys of the fixtures or of a shape are equal`() {
        for (keys in trieShapes.values + listOf(fixtureLevels.map { it.first })) assertEquals(keys, keys.distinct(), "$keys")
    }
}
