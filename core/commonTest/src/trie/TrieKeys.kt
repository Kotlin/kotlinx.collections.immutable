/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.trie

import kotlinx.collections.immutable.implementations.immutableMap.LOG_MAX_BRANCHING_FACTOR
import kotlinx.collections.immutable.implementations.immutableMap.MAX_BRANCHING_FACTOR
import kotlinx.collections.immutable.implementations.immutableMap.MAX_SHIFT
import kotlinx.collections.immutable.implementations.immutableMap.indexSegment
import tests.IntWrapper
import kotlin.random.Random

// Keys for the hash trie tests, declared by their relation to another key. The node at level n reads the bits
// [n * LOG_MAX_BRANCHING_FACTOR, (n + 1) * LOG_MAX_BRANCHING_FACTOR) of the hash, the root being level 0, so a
// key's hash is its path; keys with equal hashes share a collision node under the last level.

/** The level of the deepest node. Keys that still agree there go to a collision node. */
const val LAST_LEVEL = MAX_SHIFT / LOG_MAX_BRANCHING_FACTOR

/** A key with this key's hash: the two share a collision node under the last level. */
fun IntWrapper.colliding(id: Int): IntWrapper = IntWrapper(id, hashCode)

/**
 * A key that follows this key's path down to the node at [level] and takes [cell] there, beside this key's path;
 * below that node its path is cell 0 all the way. At the last level only the cells 0 and 1 are offered, so the hash stays
 * non-negative (the trie reads a negative hash's last cell as 30 or 31).
 */
fun IntWrapper.siblingAt(level: Int, id: Int, cell: Int = 1): IntWrapper {
    val shift = level * LOG_MAX_BRANCHING_FACTOR
    require(level in 0..LAST_LEVEL && cell in 0..<(if (level == LAST_LEVEL) 2 else MAX_BRANCHING_FACTOR)) { "no cell $cell at level $level" }
    require(cell != indexSegment(hashCode, shift)) { "cell $cell is this key's own cell at level $level" }
    return IntWrapper(id, (hashCode and ((1 shl shift) - 1)) or (cell shl shift))
}

/*
 * The trie the fixtures form when all of them are stored. A test storing a subset gets the trie the library
 * builds for it: a key sits where its path parts from the others', so collidingKey1 alone is a root entry.
 *
 *   root
 *   ├── cell 0: level-1 node
 *   │   ├── cell 0: level-2 node
 *   │   │   ├── cell 0: level-3 node ... level-6 node
 *   │   │   │   ├── cell 0: collision node { collidingKey1, collidingKey2, collidingKey3 }
 *   │   │   │   └── cell 1: lastLevelSibling
 *   │   │   └── cell 1: levelTwoSibling
 *   │   ├── cell 1: levelOneSibling
 *   │   └── cell 2: otherLevelOneSibling
 *   └── cell 1: rootSibling
 */
val collidingKey1 = IntWrapper(1, 0)
val collidingKey2 = collidingKey1.colliding(2)
val collidingKey3 = collidingKey1.colliding(4)
val lastLevelSibling = collidingKey1.siblingAt(level = LAST_LEVEL, id = 3)
val rootSibling = collidingKey1.siblingAt(level = 0, id = 8)
val levelOneSibling = collidingKey1.siblingAt(level = 1, id = 5)
val levelTwoSibling = collidingKey1.siblingAt(level = 2, id = 6)
val otherLevelOneSibling = collidingKey1.siblingAt(level = 1, id = 7, cell = 2)

/** Tries of a few keys, by the shape they form; ints where the shape is about plain ints (32 shares root cell 0 with 0). */
val trieShapes: Map<String, List<Any>> = mapOf(
    "int keys" to listOf(1, 2, 3),
    "colliding keys" to listOf(collidingKey1, collidingKey2, collidingKey3),
    "a collision node next to a last-level sibling" to listOf(collidingKey1, collidingKey2, lastLevelSibling),
    "a collision node next to a root entry" to listOf(rootSibling, collidingKey1, collidingKey2),
    "a level-1 node next to root entries" to listOf(1, 2, 3, 0, 32),
    "a level-2 node under a level-1 node" to listOf(collidingKey1, levelTwoSibling, levelOneSibling, rootSibling),
    "two collision nodes under a level-1 node" to listOf(collidingKey1, collidingKey2, levelOneSibling, levelOneSibling.colliding(9)),
)

/**
 * Seven hashes reaching every kind of node: root entries, a level-1 node, a level-2 node, the last level and a
 * collision node. The ids of the keys are irrelevant: [randomTrieKeys] hands the hashes out by id, so the ids
 * id and id + 7 collide.
 */
private val trieLevelHashes: List<Int> = listOf(
    collidingKey1, rootSibling,
    levelOneSibling, rootSibling.siblingAt(level = 1, id = 9),
    levelTwoSibling,
    lastLevelSibling, levelOneSibling.siblingAt(level = LAST_LEVEL, id = 10),
).map { it.hashCode }

/** A random subset of [ids] as keys placed all over the trie, by id. */
fun randomTrieKeys(random: Random, ids: IntRange): Map<Int, IntWrapper> =
    ids.filter { random.nextBoolean() }.associateWith { IntWrapper(it, trieLevelHashes[it % trieLevelHashes.size]) }

/** The instance the map stores for [key]. */
fun <K> Map<K, *>.storedKey(key: K): K = keys.single { it == key }

/** The instance the set stores for [element]. */
fun <E> Set<E>.storedElement(element: E): E = single { it == element }
