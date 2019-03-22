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

import kotlinx.collections.immutable.PersistentMap

internal class Marker

internal class PersistentHashMapBuilder<K, V>(private var map: PersistentHashMap<K, V>) : PersistentMap.Builder<K, V>, AbstractMutableMap<K, V>() {
    internal var marker = Marker()
    internal var node = map.node
    internal var operationResult: V? = null
    override var size = map.size

    override fun build(): PersistentHashMap<K, V> {
        map = if (node === map.node) {
            map
        } else {
            marker = Marker()
            PersistentHashMap(node, size)
        }
        return map
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() {
            return PersistentHashMapBuilderEntries(this)
        }

    override val keys: MutableSet<K>
        get() {
            return PersistentHashMapBuilderKeys(this)
        }

    override val values: MutableCollection<V>
        get() {
            return PersistentHashMapBuilderValues(this)
        }

    override fun containsKey(key: K): Boolean {
        return node.containsKey(key.hashCode(), key, 0)
    }

    override fun get(key: K): V? {
        return node.get(key.hashCode(), key, 0)
    }

    override fun put(key: K, value: @UnsafeVariance V): V? {
        operationResult = null
        node = node.mutablePut(key.hashCode(), key, value, 0, this)
        return operationResult
    }

    override fun remove(key: K): V? {
        operationResult = null
        node = node.mutableRemove(key.hashCode(), key, 0, this) ?: TrieNode.EMPTY as TrieNode<K, V>
        return operationResult
    }

    fun remove(key: K, value: V): Boolean {
        val oldSize = size
        node = node.mutableRemove(key.hashCode(), key, value, 0, this) ?: TrieNode.EMPTY as TrieNode<K, V>
        return oldSize != size
    }

    override fun clear() {
        node = TrieNode.EMPTY as TrieNode<K, V>
        size = 0
    }
}