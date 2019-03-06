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

package kotlinx.collections.immutable.implementations.immutableMap


internal class TrieNodeMutableEntriesIterator<K, V>(private val builder: PersistentHashMapBuilder<K, V>)
    : TrieNodeBaseIterator<K, V, MutableMap.MutableEntry<K, V>>() {

    override fun next(): MutableMap.MutableEntry<K, V> {
        assert(hasNextKey())
        index += 2
        return MutableMapEntry(builder, buffer[index - 2] as K, buffer[index - 1] as V)
    }
}

private class MutableMapEntry<K, V>(private val builder: PersistentHashMapBuilder<K, V>,
                                    key: K,
                                    override var value: V) : MapEntry<K, V>(key, value), MutableMap.MutableEntry<K, V> {
    override fun setValue(newValue: V): V {
        val result = value
        value = newValue
        builder[key] = newValue
        return result
    }
}


internal abstract class PersistentHashMapBuilderBaseIterator<K, V, T>(private val builder: PersistentHashMapBuilder<K, V>,
                                                                      path: Array<TrieNodeBaseIterator<K, V, T>>)
    : MutableIterator<T>, PersistentHashMapBaseIterator<K, V, T>(builder.node, path) {

    private var lastKey: K? = null
    private var nextWasInvoked = false

    override fun next(): T {
        lastKey = currentKey()
        nextWasInvoked = true
        return super.next()
    }

    override fun remove() {
        if (!nextWasInvoked) {
            throw NoSuchElementException()
        }
        if (hasNext()) {
            val currentKey = currentKey()

            builder.remove(lastKey)
            resetPath(currentKey?.hashCode() ?: NULL_HASH_CODE, builder.node, currentKey, 0)
        } else {
            builder.remove(lastKey)
        }

        lastKey = null
        nextWasInvoked = false
    }

    private fun resetPath(keyHash: Int, node: TrieNode<*, *>, key: K, pathIndex: Int) {
        val shift = pathIndex * LOG_MAX_BRANCHING_FACTOR
        val keyPosition = 1 shl ((keyHash shr shift) and MAX_BRANCHING_FACTOR_MINUS_ONE)

        if (node.hasDataAt(keyPosition)) { // key is directly in buffer
            val keyIndex = node.keyDataIndex(keyPosition)

            assert(node.keyAtIndex(keyIndex) == key)

            path[pathIndex].reset(node.buffer, 2 * Integer.bitCount(node.dataMap), keyIndex)
            return
        }

        assert(node.hasNodeAt(keyPosition)) // key is in node

        val nodeIndex = node.keyNodeIndex(keyPosition)
        val targetNode = node.nodeAtIndex(nodeIndex)
        if (shift == MAX_SHIFT) {   // collision
            path[pathIndex].reset(node.buffer, node.buffer.size, 0)
            while (path[pathIndex].currentKey() != key) {
                path[pathIndex].moveToNextKey()
            }
        } else {
            path[pathIndex].reset(node.buffer, 2 * Integer.bitCount(node.dataMap), nodeIndex)
            resetPath(keyHash, targetNode, key, pathIndex + 1)
        }
    }
}

internal class PersistentHashMapBuilderEntriesIterator<K, V>(builder: PersistentHashMapBuilder<K, V>)
    : PersistentHashMapBuilderBaseIterator<K, V, MutableMap.MutableEntry<K, V>>(builder, Array(TRIE_MAX_HEIGHT + 1) { TrieNodeMutableEntriesIterator<K, V>(builder) })

internal class PersistentHashMapBuilderKeysIterator<K, V>(builder: PersistentHashMapBuilder<K, V>)
    : PersistentHashMapBuilderBaseIterator<K, V, K>(builder, Array(TRIE_MAX_HEIGHT + 1) { TrieNodeKeysIterator<K, V>() })

internal class PersistentHashMapBuilderValuesIterator<K, V>(builder: PersistentHashMapBuilder<K, V>)
    : PersistentHashMapBuilderBaseIterator<K, V, V>(builder, Array(TRIE_MAX_HEIGHT + 1) { TrieNodeValuesIterator<K, V>() })