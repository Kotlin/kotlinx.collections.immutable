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

package kotlinx.collections.immutable.implementations.persistentOrderedMap

import kotlinx.collections.immutable.ImmutableCollection
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.mutate

internal class Links<out K, out V>(val value: V, val previous: K?, val next: K?)

internal class PersistentOrderedMap<K, V>(internal val firstKey: K?,
                                          internal val lastKey: K?,
                                          internal val map: PersistentHashMap<K, Links<K, V>>): AbstractMap<K, V>(), PersistentMap<K, V> {

    override val size: Int
        get() = map.size

    override val keys: ImmutableSet<K>
        get() {
            return PersistentOrderedMapKeys(this)
        }

    override val values: ImmutableCollection<V>
        get() {
            return PersistentOrderedMapValues(this)
        }

    override val entries: ImmutableSet<Map.Entry<K, V>>
        get() {
            return createEntries()
        }

    private fun createEntries(): ImmutableSet<Map.Entry<K, V>> {
        return PersistentOrderedMapEntries(this)
    }

    // TODO: compiler bug: this bridge should be generated automatically
    @PublishedApi
    internal fun getEntries(): Set<Map.Entry<K, V>> {
        return createEntries()
    }

    override fun containsKey(key: K): Boolean {
        return map.containsKey(key)
    }

    override fun get(key: K): V? {
        return map[key]?.value
    }

    override fun put(key: K, value: @UnsafeVariance V): PersistentOrderedMap<K, V> {
        val links = map[key]
        if (links != null) {
            if (links.value == value) {
                return this
            }
            val newMap = map.put(key, Links(value, links.previous, links.next))
            return PersistentOrderedMap(firstKey, lastKey, newMap)
        }
        if (isEmpty()) {
            val newMap = map.put(key, Links(value, null, null))
            return PersistentOrderedMap(key, key, newMap)
        }
        val oldLinks = map[lastKey]!!
        assert(oldLinks.next == null)
        val newLinks = Links(oldLinks.value, oldLinks.previous, key)
        val newMap = map.put(lastKey as K, newLinks).put(key, Links(value, lastKey, null))
        return PersistentOrderedMap(firstKey, key, newMap)
    }

    override fun remove(key: K): PersistentOrderedMap<K, V> {
        val links = map[key] ?: return this

        var newMap = map.remove(key)
        if (key != firstKey) {
            val previousLinks = newMap[links.previous]!!
            assert(previousLinks.next == key)
            newMap = newMap.put(links.previous as K, Links(previousLinks.value, previousLinks.previous, links.next))
        }
        if (key != lastKey) {
            val nextLinks = newMap[links.next]!!
            assert(nextLinks.previous == key)
            newMap = newMap.put(links.next as K, Links(nextLinks.value, links.previous, nextLinks.next))
        }
        val newFirstKey = if (key == firstKey) links.next else firstKey
        val newLastKey = if (key == lastKey) links.previous else lastKey
        return PersistentOrderedMap(newFirstKey, newLastKey, newMap)
    }

    override fun remove(key: K, value: @UnsafeVariance V): PersistentOrderedMap<K, V> {
        val links = map[key] ?: return this
        return if (links.value == value) this.remove(key) else this
    }

    override fun putAll(m: Map<out K, @UnsafeVariance V>): PersistentMap<K, V> {
        return this.mutate { it.putAll(m) }
    }

    override fun clear(): PersistentMap<K, V> {
        return emptyOf()
    }

    override fun builder(): PersistentMap.Builder<K, V> {
        return PersistentOrderedMapBuilder(this)
    }

    internal companion object {
        private val EMPTY = PersistentOrderedMap<Nothing, Nothing>(null, null, PersistentHashMap.emptyOf())
        internal fun <K, V> emptyOf(): PersistentOrderedMap<K, V> = EMPTY as PersistentOrderedMap<K, V>
    }
}