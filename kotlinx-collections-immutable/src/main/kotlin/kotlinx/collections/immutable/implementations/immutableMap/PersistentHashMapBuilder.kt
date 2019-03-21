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

internal class PersistentHashMapBuilder<K, V>(private var node: TrieNode<K, V>,
                                              override var size: Int) : PersistentMap.Builder<K, V>, AbstractMutableMap<K, V>() {
    internal var marker = Marker()

    override fun build(): PersistentMap<K, V> {
        marker = Marker()
        return PersistentHashMap(node, size)
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() {
            val iterator = PersistentHashMapIterator(node)
            val entries = mutableSetOf<MutableMap.MutableEntry<K, V>>()
            while (iterator.hasNext()) {
                val entry = iterator.nextEntry().toMutable()
                entries.add(entry)
            }
            return entries
        }

    override val keys: MutableSet<K>
        get() {
            val iterator = PersistentHashMapIterator(node)
            val keys = mutableSetOf<K>()
            while (iterator.hasNext()) {
                keys.add(iterator.nextKey())
            }
            return keys
        }

    override val values: MutableCollection<V>
        get() {
            val iterator = PersistentHashMapIterator(node)
            val values = mutableListOf<V>()
            while (iterator.hasNext()) {
                values.add(iterator.nextValue())
            }
            return values
        }

    override fun containsKey(key: K): Boolean {
        val keyHash = key?.hashCode() ?: NULL_HASH_CODE
        return node.containsKey(keyHash, key, 0)
    }

    override fun get(key: K): V? {
        val keyHash = key?.hashCode() ?: NULL_HASH_CODE
        return node.get(keyHash, key, 0)
    }

    override fun put(key: K, value: @UnsafeVariance V): V? {
        val keyHash = key?.hashCode() ?: NULL_HASH_CODE
        node = node.makeMutableFor(this)
        return node.mutablePut(keyHash, key, value, 0, this)
    }

    override fun remove(key: K): V? {
        val keyHash = key?.hashCode() ?: NULL_HASH_CODE
        node = node.makeMutableFor(this)
        return node.mutableRemove(keyHash, key, 0, this)
    }

    override fun clear() {
        node = TrieNode.EMPTY as TrieNode<K, V>
        size = 0
    }
}