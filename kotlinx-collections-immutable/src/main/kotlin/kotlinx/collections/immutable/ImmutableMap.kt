/*
 * Copyright 2016-2017 JetBrains s.r.o.
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

package kotlinx.collections.immutable


public interface ImmutableMap<K, out V>: Map<K, V> {

    override val keys: ImmutableSet<K>

    override val values: ImmutableCollection<V>

    override val entries: ImmutableSet<Map.Entry<K, V>>
}



public interface PersistentMap<K, out V> : ImmutableMap<K, V> {
    fun put(key: K, value: @UnsafeVariance V): PersistentMap<K, V>

    fun remove(key: K): PersistentMap<K, V>

    fun remove(key: K, value: @UnsafeVariance V): PersistentMap<K, V>

    fun putAll(m: Map<out K, @UnsafeVariance V>): PersistentMap<K, V>  // m: Iterable<Map.Entry<K, V>> or Map<out K,V> or Iterable<Pair<K, V>>

    fun clear(): PersistentMap<K, V>

    interface Builder<K, V>: MutableMap<K, V> {
        fun build(): PersistentMap<K, V>
    }

    fun builder(): Builder<K, @UnsafeVariance V>
}
