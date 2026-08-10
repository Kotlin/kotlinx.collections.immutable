/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.property

import kotlinx.collections.immutable.implementations.immutableMap.ENTRY_SIZE
import kotlinx.collections.immutable.implementations.immutableMap.LOG_MAX_BRANCHING_FACTOR
import kotlinx.collections.immutable.implementations.immutableMap.MAX_BRANCHING_FACTOR_MINUS_ONE
import kotlinx.collections.immutable.implementations.immutableMap.MAX_SHIFT
import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMapBuilder
import kotlinx.collections.immutable.implementations.immutableMap.TrieNode

/**
 * A flattened, pre-order view of a persistent hash map's trie.
 *
 * [TrieNode.accept] is the only way a test can see the shape: `dataMap` and `nodeMap` are private
 * fields and the visitor is what hands them out. The visitor also hands out the node itself, whose
 * `buffer` is internal, so the walk can read keys and values directly.
 *
 * A pre-order sequence of nodes plus each node's child count is a faithful encoding of the tree,
 * which is what makes [digest] a sound structural comparison.
 */
internal class MapTrieShape(val nodes: List<Node>) {

    internal class Node(
        val shift: Int,
        val hashPrefix: Int,
        val dataMap: Int,
        val nodeMap: Int,
        val buffer: Array<Any?>
    ) {
        /**
         * Collision nodes are classified by depth, not by shape: they are created only when a
         * descent overruns [MAX_SHIFT], so a node reached below that level is one by construction.
         * That `dataMap` and `nodeMap` are both zero is then an invariant to check, not the test —
         * checking the shape instead would make the validator believe exactly what it must catch.
         */
        val isCollision: Boolean get() = shift > MAX_SHIFT

        val entryCount: Int get() = if (isCollision) buffer.size / ENTRY_SIZE else dataMap.countOneBits()

        val childCount: Int get() = nodeMap.countOneBits()

        fun keyAt(entryIndex: Int): Any? = buffer[entryIndex * ENTRY_SIZE]

        fun valueAt(entryIndex: Int): Any? = buffer[entryIndex * ENTRY_SIZE + 1]

        override fun toString(): String =
            "Node(shift=$shift, prefix=${hashPrefix.toHexString()}, " +
                "dataMap=${dataMap.toBinaryString()}, nodeMap=${nodeMap.toBinaryString()}, size=${buffer.size})"
    }

    /** Number of entries stored anywhere in the trie, counted independently of the map's `size` field. */
    val entryCount: Int get() = nodes.sumOf { it.entryCount }

    val depth: Int get() = nodes.maxOfOrNull { it.shift / LOG_MAX_BRANCHING_FACTOR + 1 } ?: 0

    val collisionNodes: List<Node> get() = nodes.filter { it.isCollision }

    /**
     * A value that is equal for two tries if and only if they have the same shape and the same
     * contents. Entries inside a collision node are sorted, because their order is history
     * dependent — `makeNode` writes stored-then-new, single put prepends, and a two-tree merge
     * appends the argument's residue — and equality treats them as a multiset.
     */
    fun digest(): String = buildString {
        for (node in nodes) {
            append(node.shift).append('/')
            append(node.dataMap).append('/')
            append(node.nodeMap).append('[')
            val entries = List(node.entryCount) { renderEntry(node, it) }
            append((if (node.isCollision) entries.sorted() else entries).joinToString(","))
            append(']')
        }
    }

    private fun renderEntry(node: Node, entryIndex: Int): String {
        val key = node.keyAt(entryIndex)
        return "${key.hashCode()}:$key=${node.valueAt(entryIndex)}"
    }

    override fun toString(): String = nodes.joinToString("\n") {
        " ".repeat(it.shift / LOG_MAX_BRANCHING_FACTOR) + it
    }
}

internal fun <K, V> TrieNode<K, V>.shape(): MapTrieShape {
    val nodes = mutableListOf<MapTrieShape.Node>()
    accept { node, shift, hash, dataMap, nodeMap ->
        nodes += MapTrieShape.Node(shift, hash, dataMap, nodeMap, node.buffer)
    }
    return MapTrieShape(nodes)
}

internal fun <K, V> PersistentHashMap<K, V>.shape(): MapTrieShape = node.shape()

internal fun <K, V> PersistentHashMapBuilder<K, V>.shape(): MapTrieShape = node.shape()

/**
 * Checks every structural invariant of the map trie that is meant to hold on any value the library
 * hands out, and returns one message per violation. An empty list means the trie is well formed.
 *
 * Deliberately not checked, because all three are legitimately history dependent: the order of
 * entries inside a collision node, which of two equal-but-distinct key objects survives, and how
 * much structure two tries share.
 */
internal fun MapTrieShape.violations(expectedSize: Int = -1): List<String> {
    val problems = mutableListOf<String>()

    fun report(node: MapTrieShape.Node, message: String) {
        problems += "$message — at $node"
    }

    for (node in nodes) {
        if (node.isCollision) {
            if (node.dataMap != 0 || node.nodeMap != 0) {
                report(node, "collision node must have both bitmaps clear")
            }
            if (node.buffer.size % ENTRY_SIZE != 0) {
                report(node, "collision buffer must hold whole entries")
            }
            if (node.entryCount < 2) {
                report(node, "collision node must hold at least two entries, has ${node.entryCount}")
            }
            val keys = List(node.entryCount) { node.keyAt(it) }
            if (keys.any { it.hashCode() != node.hashPrefix }) {
                report(node, "every key in a collision node must hash to the node's prefix")
            }
            if (keys.distinct().size != keys.size) {
                report(node, "collision node must not hold duplicate keys")
            }
            continue
        }

        if (node.dataMap and node.nodeMap != 0) {
            report(node, "dataMap and nodeMap must be disjoint")
        }
        val expectedBufferSize = node.entryCount * ENTRY_SIZE + node.childCount
        if (node.buffer.size != expectedBufferSize) {
            report(node, "buffer size ${node.buffer.size} does not match the bitmaps, expected $expectedBufferSize")
        }
        if (node.shift > 0) {
            if (node.buffer.isEmpty()) {
                report(node, "only the root may be empty")
            }
            // `hasSingleEntry`: a non-root node holding one entry and no children must have been
            // promoted into its parent. This is the canonical-form rule that makes the structural
            // `equalsWith` sound, and the rule PR #218 restored on the mutable path.
            if (node.buffer.size == ENTRY_SIZE && node.nodeMap == 0) {
                report(node, "a non-root node with a single entry must be promoted into its parent")
            }
        }

        // The bitmaps are what the walk trusts, so a buffer that disagrees with them would make
        // every read below out of bounds. Report that and stop looking at this node.
        if (node.buffer.size < node.entryCount * ENTRY_SIZE) continue

        val prefixMask = if (node.shift == 0) 0 else (1 shl node.shift) - 1
        var positions = node.dataMap
        var entryIndex = 0
        while (positions != 0) {
            val mask = positions.takeLowestOneBit()
            val key = node.keyAt(entryIndex)
            val keyHash = key.hashCode()
            if (keyHash and prefixMask != node.hashPrefix) {
                report(node, "key $key does not share the node's hash prefix")
            }
            val segment = (keyHash shr node.shift) and MAX_BRANCHING_FACTOR_MINUS_ONE
            if (segment != mask.countTrailingZeroBits()) {
                report(node, "key $key sits in cell ${mask.countTrailingZeroBits()} but hashes to segment $segment")
            }
            positions -= mask
            entryIndex++
        }

        for (i in node.entryCount * ENTRY_SIZE..<node.buffer.size) {
            if (node.buffer[i] !is TrieNode<*, *>) {
                report(node, "cell $i is in the node region but does not hold a node")
            }
        }
    }

    if (expectedSize >= 0 && entryCount != expectedSize) {
        problems += "trie holds $entryCount entries but size reports $expectedSize"
    }
    return problems
}

private fun Int.toBinaryString(): String = toUInt().toString(2)

private fun Int.toHexString(): String = "0x" + toUInt().toString(16).padStart(8, '0')
