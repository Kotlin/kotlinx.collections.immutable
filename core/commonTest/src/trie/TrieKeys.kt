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

/** The level of the deepest node, the root being level 0. */
const val LAST_LEVEL = MAX_SHIFT / LOG_MAX_BRANCHING_FACTOR

fun IntWrapper.colliding(id: Int): IntWrapper = IntWrapper(id, hashCode)

/**
 * A key that follows this key's path down to the node at [level], takes [cell] there, and takes cell 0 below. At the
 * last level only cells 0 and 1 are offered, since the trie reads cells 2 and 3 there as 30 and 31.
 */
fun IntWrapper.siblingAt(level: Int, id: Int, cell: Int = 1): IntWrapper {
    val shift = level * LOG_MAX_BRANCHING_FACTOR
    require(level in 0..LAST_LEVEL && cell in 0..<(if (level == LAST_LEVEL) 2 else MAX_BRANCHING_FACTOR)) { "no cell $cell at level $level" }
    require(cell != indexSegment(hashCode, shift)) { "cell $cell is this key's own cell at level $level" }
    return IntWrapper(id, (hashCode and ((1 shl shift) - 1)) or (cell shl shift))
}

/*
 * Each sibling parts from the path of collidingKey1 at the level in its name, so it sits at that level only when it
 * is stored next to collidingKey1.
 */
val collidingKey1 = IntWrapper(1, 0)
val collidingKey2 = collidingKey1.colliding(2)
val collidingKey3 = collidingKey1.colliding(4)
val lastLevelSibling = collidingKey1.siblingAt(level = LAST_LEVEL, id = 3)
val rootSibling = collidingKey1.siblingAt(level = 0, id = 8)
val levelOneSibling = collidingKey1.siblingAt(level = 1, id = 5)
val levelTwoSibling = collidingKey1.siblingAt(level = 2, id = 6)
val otherLevelOneSibling = collidingKey1.siblingAt(level = 1, id = 7, cell = 2)

val trieShapes: Map<String, List<Any>> = mapOf(
    "int keys" to listOf(1, 2, 3),
    "colliding keys" to listOf(collidingKey1, collidingKey2, collidingKey3),
    "a collision node next to a last-level sibling" to listOf(collidingKey1, collidingKey2, lastLevelSibling),
    "a collision node next to a root entry" to listOf(rootSibling, collidingKey1, collidingKey2),
    "a level-1 node next to root entries" to listOf(1, 2, 3, 0, MAX_BRANCHING_FACTOR),
    "a level-2 node under a level-1 node" to listOf(collidingKey1, levelTwoSibling, levelOneSibling, rootSibling),
    "two collision nodes under a level-1 node" to listOf(collidingKey1, collidingKey2, levelOneSibling, levelOneSibling.colliding(9)),
)

/**
 * A key the test changes after it is stored. A new [hash] moves it to another slot while it stays the same key. A new
 * [id] makes it equal to the key of that id and gives it that key's hash.
 */
class MutableKey(id: Int, var hash: Int = id) {
    var id: Int = id
        set(value) {
            field = value
            hash = value
        }

    override fun hashCode(): Int = hash

    override fun equals(other: Any?): Boolean = other is MutableKey && other.id == id

    override fun toString(): String = if (hash == id) "MutableKey($id)" else "MutableKey($id, hash $hash)"
}
