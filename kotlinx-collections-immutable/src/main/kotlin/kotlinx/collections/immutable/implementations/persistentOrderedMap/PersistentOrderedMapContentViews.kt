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

internal class PersistentOrderedMapEntries<K, V>(private val map: PersistentOrderedMap<K, V>) : ImmutableSet<Map.Entry<K, V>>, AbstractSet<Map.Entry<K, V>>() {
    override val size: Int get() = map.size

    override fun contains(element: Map.Entry<K, V>): Boolean {
        return map[element.key]?.let { candidate -> candidate == element.value }
                ?: (element.value == null && map.containsKey(element.key))
    }

    override fun iterator(): Iterator<Map.Entry<K, V>> {
        return PersistentOrderedMapEntriesIterator(map)
    }
}

internal class PersistentOrderedMapKeys<K, V>(private val map: PersistentOrderedMap<K, V>) : ImmutableSet<K>, AbstractSet<K>() {
    override val size: Int
        get() = map.size

    override fun contains(element: K): Boolean {
        return map.containsKey(element)
    }

    override fun iterator(): Iterator<K> {
        return PersistentOrderedMapKeysIterator(map)
    }
}

internal class PersistentOrderedMapValues<K, V>(private val map: PersistentOrderedMap<K, V>) : ImmutableCollection<V>, AbstractCollection<V>() {
    override val size: Int
        get() = map.size

    override fun contains(element: V): Boolean {
        return map.containsValue(element)
    }

    override fun iterator(): Iterator<V> {
        return PersistentOrderedMapValuesIterator(map)
    }
}