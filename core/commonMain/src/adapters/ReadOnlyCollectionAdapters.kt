/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.adapters

import kotlinx.collections.immutable.*


/**
 * Exposes the underlying collection as an [ImmutableCollection] without copying it.
 *
 * All operations, including `equals`, `hashCode` and `toString`, are delegated to the underlying collection.
 *
 * The caller is responsible for ensuring the underlying collection never changes:
 * wrapping a collection that changes violates the [ImmutableCollection] contract.
 */
public open class ImmutableCollectionAdapter<E>(private val impl: Collection<E>) : ImmutableCollection<E>, Collection<E> by impl {
    override fun equals(other: Any?): Boolean = impl.equals(other)
    override fun hashCode(): Int = impl.hashCode()
    override fun toString(): String = impl.toString()
}


/**
 * Exposes the underlying list as an [ImmutableList] without copying it.
 *
 * All operations, including `equals`, `hashCode` and `toString`, are delegated to the underlying list.
 * [subList] returns an adapted view of the underlying list's sublist.
 *
 * The caller is responsible for ensuring the underlying list never changes:
 * wrapping a list that changes violates the [ImmutableList] contract.
 */
public class ImmutableListAdapter<E>(private val impl: List<E>) : ImmutableList<E>, List<E> by impl {

    override fun subList(fromIndex: Int, toIndex: Int): ImmutableList<E> = ImmutableListAdapter(impl.subList(fromIndex, toIndex))

    override fun equals(other: Any?): Boolean = impl.equals(other)
    override fun hashCode(): Int = impl.hashCode()
    override fun toString(): String = impl.toString()
}


/**
 * Exposes the underlying set as an [ImmutableSet] without copying it.
 *
 * All operations are delegated to the underlying set, as in [ImmutableCollectionAdapter].
 *
 * The caller is responsible for ensuring the underlying set never changes:
 * wrapping a set that changes violates the [ImmutableSet] contract.
 */
public class ImmutableSetAdapter<E>(impl: Set<E>) : ImmutableSet<E>, ImmutableCollectionAdapter<E>(impl)


/**
 * Exposes the underlying map as an [ImmutableMap] without copying it.
 *
 * All operations, including `equals`, `hashCode` and `toString`, are delegated to the underlying map.
 * The [keys], [values] and [entries] views are adapted as well.
 *
 * The caller is responsible for ensuring the underlying map never changes:
 * wrapping a map that changes violates the [ImmutableMap] contract.
 */
public class ImmutableMapAdapter<K, out V>(private val impl: Map<K, V>) : ImmutableMap<K, V>, Map<K, V> by impl {
    // TODO: Lazy initialize these properties?
    override val keys: ImmutableSet<K> = ImmutableSetAdapter(impl.keys)
    override val values: ImmutableCollection<V> = ImmutableCollectionAdapter(impl.values)
    override val entries: ImmutableSet<Map.Entry<K, V>> = ImmutableSetAdapter(impl.entries)

    override fun equals(other: Any?): Boolean = impl.equals(other)
    override fun hashCode(): Int = impl.hashCode()
    override fun toString(): String = impl.toString()
}