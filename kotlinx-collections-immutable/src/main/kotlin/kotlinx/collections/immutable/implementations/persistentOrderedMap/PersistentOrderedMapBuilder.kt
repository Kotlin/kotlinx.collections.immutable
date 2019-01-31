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

import kotlinx.collections.immutable.PersistentMap

internal class PersistentOrderedMapBuilder<K, V>(private var map: PersistentOrderedMap<K, V>) : AbstractMutableMap<K, V>(), PersistentMap.Builder<K, V> {
    internal var firstKey = map.firstKey
    private var lastKey = map.lastKey
    internal val mapBuilder = map.map.builder()

    override val size: Int
        get() = mapBuilder.size

    override fun build(): PersistentMap<K, V> {
        val newMap = mapBuilder.build()
        map = if (newMap === map.map) {
            assert(firstKey === map.firstKey)
            assert(lastKey === map.lastKey)
            map
        } else {
            PersistentOrderedMap(firstKey, lastKey, newMap)
        }
        return map
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() {
            return PersistentOrderedMapBuilderEntries(this)
        }

    override val keys: MutableSet<K>
        get() {
            return PersistentOrderedMapBuilderKeys(this)
        }

    override val values: MutableCollection<V>
        get() {
            return PersistentOrderedMapBuilderValues(this)
        }

    override fun containsKey(key: K): Boolean {
        return mapBuilder.containsKey(key)
    }

    override fun get(key: K): V? {
        return mapBuilder[key]?.value
    }

    override fun put(key: K, value: @UnsafeVariance V): V? {
        val links = mapBuilder[key]
        if (links != null) {
            if (links.value == value) {
                return value
            }
            mapBuilder[key] = Links(value, links.previous, links.next)
            return links.value
        }

        if (isEmpty()) {  //  isEmpty
            firstKey = key
            lastKey = key
            mapBuilder[key] = Links<K, V>(value, null, null)
            return null
        }
        val oldLinks = mapBuilder[lastKey]!!
        assert(oldLinks.next == null)
        val newLinks = Links(oldLinks.value, oldLinks.previous, key)

        mapBuilder[lastKey as K] = newLinks
        mapBuilder[key] = Links(value, lastKey, null)
        lastKey = key
        return null
    }

    override fun remove(key: K): V? {
        val links = mapBuilder.remove(key) ?: return null

        if (links.previous != null) {
            val previousLinks = mapBuilder[links.previous]!!
            assert(previousLinks.next == key)
            mapBuilder[links.previous] = Links(previousLinks.value, previousLinks.previous, links.next)
        }
        if (links.next != null) {
            val nextLinks = mapBuilder[links.next]!!
            assert(nextLinks.previous == key)
            mapBuilder[links.next] = Links(nextLinks.value, links.previous, nextLinks.next)
        }
        firstKey = if (key == firstKey) links.next else firstKey
        lastKey = if (key == lastKey) links.previous else lastKey
        return links.value
    }

    fun remove(key: K, value: V): Boolean {
        val links = mapBuilder[key] ?: return false

        return if (links.value != value) {
            false
        } else {
            remove(key)
            true
        }
    }

    override fun clear() {
        mapBuilder.clear()
        firstKey = null
        lastKey = null
    }
}