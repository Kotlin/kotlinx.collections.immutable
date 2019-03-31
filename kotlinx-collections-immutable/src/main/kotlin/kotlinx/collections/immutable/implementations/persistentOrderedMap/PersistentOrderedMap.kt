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

internal object EndOfLink

internal class LinkedValue<V>(val value: V, val previous: Any?, val next: Any?) {
    constructor(value: V) : this(value, EndOfLink, EndOfLink)

    fun withValue(newValue: V) = LinkedValue(newValue, previous, next)
    fun withPrevious(newPrevious: Any?) = LinkedValue(value, newPrevious, next)
    fun withNext(newNext: Any?) = LinkedValue(value, previous, newNext)

    fun putNextLink(value: V, previous: Any?): LinkedValue<V> {
//        assert(next === EndOfLink)
        return LinkedValue(value, previous, EndOfLink)
    }
}

internal class PersistentOrderedMap<K, V>(internal val firstKey: K?,
                                          internal val lastKey: K?,
                                          internal val map: PersistentHashMap<K, LinkedValue<V>>): AbstractMap<K, V>(), PersistentMap<K, V> {

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
            val newMap = map.put(key, links.withValue(value))
            return PersistentOrderedMap(firstKey, lastKey, newMap)
        }
        if (isEmpty()) {
            val newMap = map.put(key, LinkedValue(value))
            return PersistentOrderedMap(key, key, newMap)
        }
        val lastLink = map[lastKey]!!
//        assert(lastLink.next === EndOfLink)
        val newMap = map
                .put(lastKey as K, lastLink.withNext(key))
                .put(key, lastLink.putNextLink(value, lastKey))
        return PersistentOrderedMap(firstKey, key, newMap)
    }

    override fun remove(key: K): PersistentOrderedMap<K, V> {
        val links = map[key] ?: return this

        var newMap = map.remove(key)
        if (links.previous !== EndOfLink) {
            val previousLinks = newMap[links.previous]!!
//            assert(previousLinks.next == key)
            newMap = newMap.put(links.previous as K, previousLinks.withNext(links.next))
        }
        if (links.next !== EndOfLink) {
            val nextLinks = newMap[links.next]!!
//            assert(nextLinks.previous == key)
            newMap = newMap.put(links.next as K, nextLinks.withPrevious(links.previous))
        }
        val newFirstKey = if (links.previous === EndOfLink) links.next as? K else firstKey
        val newLastKey = if (links.next === EndOfLink) links.previous as? K else lastKey
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