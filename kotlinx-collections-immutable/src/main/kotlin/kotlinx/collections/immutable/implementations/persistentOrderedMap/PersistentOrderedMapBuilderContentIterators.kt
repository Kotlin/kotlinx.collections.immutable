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

internal open class PersistentOrderedMapBuilderLinksIterator<K, V>(internal var nextKey: K?,
                                                                   internal val map: MutableMap<K, Links<K, V>>) : MutableIterator<Links<K, V>> {
    internal var lastProcessedKey: K? = null
    private var nextWasInvoked = false
    internal var index = 0

    override fun hasNext(): Boolean {
        return index < map.size
    }

    override fun next(): Links<K, V> {
        if (!hasNext()) {
            throw NoSuchElementException()
        }
        lastProcessedKey = nextKey
        nextWasInvoked = true
        index++
        val result = map[nextKey as K]!!
        nextKey = result.next
        return result
    }

    override fun remove() {
        if (!nextWasInvoked) {
            throw NoSuchElementException()
        }
        map.remove(lastProcessedKey)
        lastProcessedKey = null
        nextWasInvoked = false
        index--
    }
}

internal class PersistentOrderedMapBuilderEntriesIterator<K, V>(map: PersistentOrderedMapBuilder<K, V>): MutableIterator<MutableMap.MutableEntry<K, V>> {
    private val internal = PersistentOrderedMapBuilderLinksIterator(map.firstKey, map.mapBuilder)

    override fun hasNext(): Boolean {
        return internal.hasNext()
    }

    override fun next(): MutableMap.MutableEntry<K, V> {
        val links = internal.next()
        return MutableMapEntry(internal.map, internal.lastProcessedKey as K, links)
    }

    override fun remove() {
        internal.remove()
    }
}

private data class MutableMapEntry<K, V>(private val mutableMap: MutableMap<K, Links<K, V>>,
                                    override val key: K,
                                    private var links: Links<K, V>) : MutableMap.MutableEntry<K, V> {
    override val value: V
        get() = links.value

    override fun setValue(newValue: V): V {
        val result = links.value
        links = Links(newValue, links.previous, links.next)
        mutableMap[key] = links
        return result
    }

    override fun toString(): String {
        return key.toString() + "=" + value.toString()
    }
}

internal class PersistentOrderedMapBuilderKeysIterator<out K, out V>(map: PersistentOrderedMapBuilder<K, V>): MutableIterator<K> {
    private val internal = PersistentOrderedMapBuilderLinksIterator(map.firstKey, map.mapBuilder)

    override fun hasNext(): Boolean {
        return internal.hasNext()
    }

    override fun next(): K {
        internal.next()
        return internal.lastProcessedKey as K
    }

    override fun remove() {
        internal.remove()
    }
}

internal class PersistentOrderedMapBuilderValuesIterator<out K, out V>(map: PersistentOrderedMapBuilder<K, V>): MutableIterator<V> {
    private val internal = PersistentOrderedMapBuilderLinksIterator(map.firstKey, map.mapBuilder)

    override fun hasNext(): Boolean {
        return internal.hasNext()
    }

    override fun next(): V {
        return internal.next().value
    }

    override fun remove() {
        internal.remove()
    }
}