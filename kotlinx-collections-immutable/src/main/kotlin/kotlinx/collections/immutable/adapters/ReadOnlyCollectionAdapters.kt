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

package kotlinx.collections.immutable.adapters

import kotlinx.collections.immutable.*


/*
 These classes allow to expose read-only collection as immutable, if it's actually immutable one
 Use with caution: wrapping mutable collection as immutable is a contract violation of the latter.
 */

public open class ImmutableCollectionAdapter<E>(private val impl: Collection<E>) : ImmutableCollection<E>, Collection<E> by impl {
    override fun equals(other: Any?): Boolean = impl.equals(other)
    override fun hashCode(): Int = impl.hashCode()
    override fun toString(): String = impl.toString()
}


public class ImmutableListAdapter<E>(private val impl: List<E>) : ImmutableList<E>, List<E> by impl {

    override fun subList(fromIndex: Int, toIndex: Int): ImmutableList<E> = ImmutableListAdapter(impl.subList(fromIndex, toIndex))

    override fun equals(other: Any?): Boolean = impl.equals(other)
    override fun hashCode(): Int = impl.hashCode()
    override fun toString(): String = impl.toString()
}


public class ImmutableSetAdapter<E>(impl: Set<E>) : ImmutableSet<E>, ImmutableCollectionAdapter<E>(impl)


public class ImmutableMapAdapter<K, out V>(private val impl: Map<K, V>) : ImmutableMap<K, V>, Map<K, V> by impl {
    // TODO: Lazy initialize these properties?
    override val keys: ImmutableSet<K> = ImmutableSetAdapter(impl.keys)
    override val values: ImmutableCollection<V> = ImmutableCollectionAdapter(impl.values)
    override val entries: ImmutableSet<Map.Entry<K, V>> = ImmutableSetAdapter(impl.entries)

    override fun equals(other: Any?): Boolean = impl.equals(other)
    override fun hashCode(): Int = impl.hashCode()
    override fun toString(): String = impl.toString()
}