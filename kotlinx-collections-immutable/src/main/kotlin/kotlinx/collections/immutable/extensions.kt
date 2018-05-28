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

@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.collections.immutable

import kotlinx.collections.immutable.implementations.immutableList.persistentVectorOf
import kotlinx.collections.immutable.implementations.immutableMap.persistentHashMapOf

//@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
//inline fun <T> @kotlin.internal.Exact ImmutableCollection<T>.mutate(mutator: (MutableCollection<T>) -> Unit): ImmutableCollection<T> = builder().apply(mutator).build()
// it or this?
inline fun <T> PersistentSet<T>.mutate(mutator: (MutableSet<T>) -> Unit): PersistentSet<T> = builder().apply(mutator).build()
inline fun <T> PersistentList<T>.mutate(mutator: (MutableList<T>) -> Unit): PersistentList<T> = builder().apply(mutator).build()

inline fun <K, V> PersistentMap<out K, V>.mutate(mutator: (MutableMap<K, V>) -> Unit): PersistentMap<K, V> =
        (this as PersistentMap<K, V>).builder().apply(mutator).build()


inline operator fun <E> PersistentCollection<E>.plus(element: E): PersistentCollection<E> = add(element)
inline operator fun <E> PersistentCollection<E>.minus(element: E): PersistentCollection<E> = remove(element)


operator fun <E> PersistentCollection<E>.plus(elements: Iterable<E>): PersistentCollection<E>
        = if (elements is Collection) addAll(elements) else builder().also { it.addAll(elements) }.build()
operator fun <E> PersistentCollection<E>.plus(elements: Array<out E>): PersistentCollection<E>
        = builder().also { it.addAll(elements) }.build()
operator fun <E> PersistentCollection<E>.plus(elements: Sequence<E>): PersistentCollection<E>
        = builder().also { it.addAll(elements) }.build()


operator fun <E> PersistentCollection<E>.minus(elements: Iterable<E>): PersistentCollection<E>
        = if (elements is Collection) removeAll(elements) else builder().also { it.removeAll(elements) }.build()
operator fun <E> PersistentCollection<E>.minus(elements: Array<out E>): PersistentCollection<E>
        = builder().also { it.removeAll(elements) }.build()
operator fun <E> PersistentCollection<E>.minus(elements: Sequence<E>): PersistentCollection<E>
        =  builder().also { it.removeAll(elements) }.build()


inline operator fun <E> PersistentList<E>.plus(element: E): PersistentList<E> = add(element)
inline operator fun <E> PersistentList<E>.minus(element: E): PersistentList<E> = remove(element)


operator fun <E> PersistentList<E>.plus(elements: Iterable<E>): PersistentList<E>
        = if (elements is Collection) addAll(elements) else mutate { it.addAll(elements) }
operator fun <E> PersistentList<E>.plus(elements: Array<out E>): PersistentList<E>
        = mutate { it.addAll(elements) }
operator fun <E> PersistentList<E>.plus(elements: Sequence<E>): PersistentList<E>
        = mutate { it.addAll(elements) }


operator fun <E> PersistentList<E>.minus(elements: Iterable<E>): PersistentList<E>
        = if (elements is Collection) removeAll(elements) else mutate { it.removeAll(elements) }
operator fun <E> PersistentList<E>.minus(elements: Array<out E>): PersistentList<E>
        = mutate { it.removeAll(elements) }
operator fun <E> PersistentList<E>.minus(elements: Sequence<E>): PersistentList<E>
        = mutate { it.removeAll(elements) }


inline operator fun <E> PersistentSet<E>.plus(element: E): PersistentSet<E> = add(element)
inline operator fun <E> PersistentSet<E>.minus(element: E): PersistentSet<E> = remove(element)

operator fun <E> PersistentSet<E>.plus(elements: Iterable<E>): PersistentSet<E>
        = if (elements is Collection) addAll(elements) else mutate { it.addAll(elements) }
operator fun <E> PersistentSet<E>.plus(elements: Array<out E>): PersistentSet<E>
        = mutate { it.addAll(elements) }
operator fun <E> PersistentSet<E>.plus(elements: Sequence<E>): PersistentSet<E>
        = mutate { it.addAll(elements) }


operator fun <E> PersistentSet<E>.minus(elements: Iterable<E>): PersistentSet<E>
        = if (elements is Collection) removeAll(elements) else mutate { it.removeAll(elements) }
operator fun <E> PersistentSet<E>.minus(elements: Array<out E>): PersistentSet<E>
        = mutate { it.removeAll(elements) }
operator fun <E> PersistentSet<E>.minus(elements: Sequence<E>): PersistentSet<E>
        = mutate { it.removeAll(elements) }


inline operator fun <K, V> PersistentMap<out K, V>.plus(pair: Pair<K, V>): PersistentMap<K, V>
        = (this as PersistentMap<K, V>).put(pair.first, pair.second)
inline operator fun <K, V> PersistentMap<out K, V>.plus(pairs: Iterable<Pair<K, V>>): PersistentMap<K, V> = putAll(pairs)
inline operator fun <K, V> PersistentMap<out K, V>.plus(pairs: Array<out Pair<K, V>>): PersistentMap<K, V> = putAll(pairs)
inline operator fun <K, V> PersistentMap<out K, V>.plus(pairs: Sequence<Pair<K, V>>): PersistentMap<K, V> = putAll(pairs)
inline operator fun <K, V> PersistentMap<out K, V>.plus(map: Map<out K, V>): PersistentMap<K, V> = putAll(map)

public fun <K, V> PersistentMap<out K, V>.putAll(map: Map<out K, V>): PersistentMap<K, V> =
        (this as PersistentMap<K, V>).putAll(map)

public fun <K, V> PersistentMap<out K, V>.putAll(pairs: Iterable<Pair<K, V>>): PersistentMap<K, V>
        = mutate { it.putAll(pairs) }

public fun <K, V> PersistentMap<out K, V>.putAll(pairs: Array<out Pair<K, V>>): PersistentMap<K, V>
        = mutate { it.putAll(pairs) }

public fun <K, V> PersistentMap<out K, V>.putAll(pairs: Sequence<Pair<K, V>>): PersistentMap<K, V>
        = mutate { it.putAll(pairs) }


public operator fun <K, V> PersistentMap<out K, V>.minus(key: K): PersistentMap<K, V>
        = (this as PersistentMap<K, V>).remove(key)

public operator fun <K, V> PersistentMap<out K, V>.minus(keys: Iterable<K>): PersistentMap<K, V>
        = mutate { it.minusAssign(keys) }

public operator fun <K, V> PersistentMap<out K, V>.minus(keys: Array<out K>): PersistentMap<K, V>
        = mutate { it.minusAssign(keys) }

public operator fun <K, V> PersistentMap<out K, V>.minus(keys: Sequence<K>): PersistentMap<K, V>
        = mutate { it.minusAssign(keys) }


fun <E> persistentListOf(vararg elements: E): PersistentList<E> = ImmutableVectorList.emptyOf<E>().addAll(elements.asList())
fun <E> persistentListOf(): PersistentList<E> = ImmutableVectorList.emptyOf<E>()

fun <E> persistentSetOf(vararg elements: E): PersistentSet<E> = ImmutableOrderedSet.emptyOf<E>().addAll(elements.asList())
fun <E> persistentSetOf(): PersistentSet<E> = ImmutableOrderedSet.emptyOf<E>()

fun <E> persistentHashSetOf(vararg elements: E): PersistentSet<E> = ImmutableHashSet.emptyOf<E>().addAll(elements.asList())

fun <K, V> persistentMapOf(vararg pairs: Pair<K, V>): PersistentMap<K, V> = ImmutableOrderedMap.emptyOf<K,V>().mutate { it += pairs }
fun <K, V> persistentHashMapOf(vararg pairs: Pair<K, V>): PersistentMap<K, V> = ImmutableHashMap.emptyOf<K,V>().mutate { it += pairs }

@Deprecated("Use persistentListOf instead.", ReplaceWith("persistentListOf(*elements)"))
fun <E> immutableListOf(vararg elements: E): PersistentList<E> = persistentListOf(*elements)
@Deprecated("Use persistentListOf instead.", ReplaceWith("persistentListOf()"))
fun <E> immutableListOf(): PersistentList<E> = persistentListOf()

@Deprecated("Use persistentSetOf instead.", ReplaceWith("persistentSetOf(*elements)"))
fun <E> immutableSetOf(vararg elements: E): PersistentSet<E> = persistentSetOf(*elements)
@Deprecated("Use persistentSetOf instead.", ReplaceWith("persistentSetOf()"))
fun <E> immutableSetOf(): PersistentSet<E> = persistentSetOf()

@Deprecated("Use persistentHashSetOf instead.", ReplaceWith("persistentHashSetOf(*elements)"))
fun <E> immutableHashSetOf(vararg elements: E): PersistentSet<E> = persistentHashSetOf(*elements)

@Deprecated("Use persistentMapOf instead.", ReplaceWith("persistentMapOf(*pairs)"))
fun <K, V> immutableMapOf(vararg pairs: Pair<K, V>): PersistentMap<K, V> = persistentMapOf(*pairs)
@Deprecated("Use persistentHashMapOf instead.", ReplaceWith("persistentHashMapOf(*pairs)"))
fun <K, V> immutableHashMapOf(vararg pairs: Pair<K, V>): PersistentMap<K, V> = persistentHashMapOf(*pairs)



fun <T> Iterable<T>.toImmutableList(): ImmutableList<T> =
        this as? ImmutableList
        ?: (this as? PersistentList.Builder)?.build()
        ?: persistentListOf<T>() + this

fun <T> Iterable<T>.toPersistentList(): PersistentList<T> =
        this as? PersistentList
        ?: (this as? PersistentList.Builder)?.build()
        ?: ImmutableVectorList.emptyOf<T>() + this


// fun <T> Array<T>.toImmutableList(): ImmutableList<T> = immutableListOf<T>() + this.asList()

fun CharSequence.toImmutableList(): ImmutableList<Char> = toPersistentList()

fun CharSequence.toPersistentList(): PersistentList<Char> =
    persistentListOf<Char>().mutate { this.toCollection(it) }

fun CharSequence.toPersistentSet(): PersistentSet<Char> =
    persistentSetOf<Char>().mutate { this.toCollection(it) }


fun <T> Iterable<T>.toImmutableSet(): ImmutableSet<T> =
        this as? ImmutableSet<T>
        ?: (this as? PersistentSet.Builder)?.build()
        ?: persistentSetOf<T>() + this

fun <T> Iterable<T>.toPersistentSet(): PersistentSet<T> =
        this as? PersistentSet<T>
        ?: (this as? PersistentSet.Builder)?.build()
        ?: persistentSetOf<T>() + this

fun <T> Set<T>.toPersistentHashSet(): PersistentSet<T>
    = this as? ImmutableHashSet
        ?: (this as? ImmutableHashSet.Builder)?.build()
        ?: ImmutableHashSet.emptyOf<T>() + this


fun <K, V> Map<K, V>.toImmutableMap(): ImmutableMap<K, V>
    = this as? ImmutableMap
        ?: (this as? PersistentMap.Builder)?.build()
        ?: ImmutableOrderedMap.emptyOf<K, V>().putAll(this)


fun <K, V> Map<K, V>.toPersistentMap(): PersistentMap<K, V>
    = this as? PersistentMap<K, V>
        ?: (this as? PersistentMap.Builder<K, V>)?.build()
        ?: ImmutableOrderedMap.emptyOf<K, V>().putAll(this)

fun <K, V> Map<K, V>.toPersistentHashMap(): PersistentMap<K, V>
        = this as? PersistentMap
        ?: (this as? ImmutableHashMap.Builder)?.build()
        ?: ImmutableHashMap.emptyOf<K, V>().putAll(this)