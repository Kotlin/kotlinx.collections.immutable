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

internal object EndOfChain

internal class LinkedValue<V>(val value: V, val previous: Any?, val next: Any?) {
    constructor(value: V) : this(value, EndOfChain, EndOfChain)

    fun withValue(newValue: V) = LinkedValue(newValue, previous, next)
    fun withPrevious(newPrevious: Any?) = LinkedValue(value, newPrevious, next)
    fun withNext(newNext: Any?) = LinkedValue(value, previous, newNext)

    val hasNext get() = next !== EndOfChain
    val hasPrevious get() = previous !== EndOfChain

    // TODO: it doesn't use 'this', can be made static
    fun putNextLink(value: V, previous: Any?): LinkedValue<V> {
//        assert(next === EndOfChain)
        return LinkedValue(value, previous, EndOfChain)
    }
}

internal class PersistentOrderedMap<K, V>(
        internal val firstKey: Any?,
        internal val lastKey: Any?,
        internal val hashMap: PersistentHashMap<K, LinkedValue<V>>
) : AbstractMap<K, V>(), PersistentMap<K, V> {

    override val size: Int get() = hashMap.size

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

    override fun containsKey(key: K): Boolean = hashMap.containsKey(key)

    override fun get(key: K): V? = hashMap[key]?.value

    override fun put(key: K, value: @UnsafeVariance V): PersistentOrderedMap<K, V> {
        if (isEmpty()) {
            val newMap = hashMap.put(key, LinkedValue(value))
            return PersistentOrderedMap(key, key, newMap)
        }

        val links = hashMap[key]
        if (links != null) {
            if (links.value === value) {
                return this
            }
            val newMap = hashMap.put(key, links.withValue(value))
            return PersistentOrderedMap(firstKey, lastKey, newMap)
        }

        @Suppress("UNCHECKED_CAST")
        val lastKey = lastKey as K
        val lastLink = hashMap[lastKey]!!
//        assert(!lastLink.hasNext)
        val newMap = hashMap
                .put(lastKey, lastLink.withNext(key))
                .put(key, lastLink.putNextLink(value, lastKey))
        return PersistentOrderedMap(firstKey, key, newMap)
    }

    override fun remove(key: K): PersistentOrderedMap<K, V> {
        val links = hashMap[key] ?: return this

        var newMap = hashMap.remove(key)
        if (links.hasPrevious) {
            val previousLinks = newMap[links.previous]!!
//            assert(previousLinks.next == key)
            @Suppress("UNCHECKED_CAST")
            newMap = newMap.put(links.previous as K, previousLinks.withNext(links.next))
        }
        if (links.hasNext) {
            val nextLinks = newMap[links.next]!!
//            assert(nextLinks.previous == key)
            @Suppress("UNCHECKED_CAST")
            newMap = newMap.put(links.next as K, nextLinks.withPrevious(links.previous))
        }

        val newFirstKey = if (!links.hasPrevious) links.next else firstKey
        val newLastKey = if (!links.hasNext) links.previous else lastKey
        return PersistentOrderedMap(newFirstKey, newLastKey, newMap)
    }

    override fun remove(key: K, value: @UnsafeVariance V): PersistentOrderedMap<K, V> {
        val links = hashMap[key] ?: return this
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
        private val EMPTY = PersistentOrderedMap<Nothing, Nothing>(EndOfChain, EndOfChain, PersistentHashMap.emptyOf<Nothing, LinkedValue<Nothing>>())
        @Suppress("UNCHECKED_CAST")
        internal fun <K, V> emptyOf(): PersistentOrderedMap<K, V> = EMPTY as PersistentOrderedMap<K, V>
    }
}