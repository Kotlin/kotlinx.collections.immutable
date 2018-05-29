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

//@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
//inline fun <T> @kotlin.internal.Exact ImmutableCollection<T>.mutate(mutator: (MutableCollection<T>) -> Unit): ImmutableCollection<T> = builder().apply(mutator).build()
// it or this?
inline fun <T, C : ImmutableSet<T>> C.mutate(mutator: (MutableSet<T>) -> Unit): C = builder().apply(mutator).build() as C
inline fun <T, C : ImmutableList<T>> C.mutate(mutator: (MutableList<T>) -> Unit): C = builder().apply(mutator).build() as C

inline fun <K, V, M : ImmutableMap<out K, V>> M.mutate(mutator: (MutableMap<K, V>) -> Unit): M = (this as ImmutableMap<K, V>).builder().apply(mutator).build() as M


operator fun <E, C : ImmutableCollection<E>> C.plus(element: E): C = builder().apply { add(element) }.build() as C
operator fun <E, C : ImmutableCollection<E>> C.minus(element: E): C = builder().apply { remove(element) }.build() as C


operator fun <E, C : ImmutableCollection<E>> C.plus(elements: Iterable<E>): C
        = builder().also { it.addAll(elements) }.build() as C
operator fun <E, C : ImmutableCollection<E>> C.plus(elements: Array<out E>): C
        = builder().also { it.addAll(elements) }.build() as C
operator fun <E, C : ImmutableCollection<E>> C.plus(elements: Sequence<E>): C
        = builder().also { it.addAll(elements) }.build() as C


operator fun <E, C : ImmutableCollection<E>> C.minus(elements: Iterable<E>): C
        = builder().also { it.removeAll(elements) }.build() as C
operator fun <E, C : ImmutableCollection<E>> C.minus(elements: Array<out E>): C
        = builder().also { it.removeAll(elements) }.build() as C
operator fun <E, C : ImmutableCollection<E>> C.minus(elements: Sequence<E>): C
        =  builder().also { it.removeAll(elements) }.build() as C


//inline operator fun <E> ImmutableList<E>.plus(element: E): ImmutableList<E> = add(element)
//inline operator fun <E> ImmutableList<E>.minus(element: E): ImmutableList<E> = remove(element)


//operator fun <E> ImmutableList<E>.plus(elements: Iterable<E>): ImmutableList<E>
//        = if (elements is Collection) addAll(elements) else mutate { it.addAll(elements) }
//operator fun <E> ImmutableList<E>.plus(elements: Array<out E>): ImmutableList<E>
//        = mutate { it.addAll(elements) }
//operator fun <E> ImmutableList<E>.plus(elements: Sequence<E>): ImmutableList<E>
//        = mutate { it.addAll(elements) }


//operator fun <E> ImmutableList<E>.minus(elements: Iterable<E>): ImmutableList<E>
//        = if (elements is Collection) removeAll(elements) else mutate { it.removeAll(elements) }
//operator fun <E> ImmutableList<E>.minus(elements: Array<out E>): ImmutableList<E>
//        = mutate { it.removeAll(elements) }
//operator fun <E> ImmutableList<E>.minus(elements: Sequence<E>): ImmutableList<E>
//        = mutate { it.removeAll(elements) }


//inline operator fun <E> ImmutableSet<E>.plus(element: E): ImmutableSet<E> = add(element)
//inline operator fun <E> ImmutableSet<E>.minus(element: E): ImmutableSet<E> = remove(element)
//
//operator fun <E> ImmutableSet<E>.plus(elements: Iterable<E>): ImmutableSet<E>
//        = if (elements is Collection) addAll(elements) else mutate { it.addAll(elements) }
//operator fun <E> ImmutableSet<E>.plus(elements: Array<out E>): ImmutableSet<E>
//        = mutate { it.addAll(elements) }
//operator fun <E> ImmutableSet<E>.plus(elements: Sequence<E>): ImmutableSet<E>
//        = mutate { it.addAll(elements) }


//operator fun <E> ImmutableSet<E>.minus(elements: Iterable<E>): ImmutableSet<E>
//        = if (elements is Collection) removeAll(elements) else mutate { it.removeAll(elements) }
//operator fun <E> ImmutableSet<E>.minus(elements: Array<out E>): ImmutableSet<E>
//        = mutate { it.removeAll(elements) }
//operator fun <E> ImmutableSet<E>.minus(elements: Sequence<E>): ImmutableSet<E>
//        = mutate { it.removeAll(elements) }


operator fun <K, V, M : ImmutableMap<out K, V>> M.plus(pair: Pair<K, V>): M
        = mutate { it.put(pair.first, pair.second) }

operator fun <K, V, M : ImmutableMap<out K, V>> M.plus(pairs: Iterable<Pair<K, V>>): M = putAll(pairs)
operator fun <K, V, M : ImmutableMap<out K, V>> M.plus(pairs: Array<out Pair<K, V>>): M = putAll(pairs)
operator fun <K, V, M : ImmutableMap<out K, V>> M.plus(pairs: Sequence<Pair<K, V>>): M = putAll(pairs)
operator fun <K, V, M : ImmutableMap<out K, V>> M.plus(map: Map<out K, V>): M = mutate { it.putAll(map) }

public fun <K, V, M : ImmutableMap<out K, V>> M.putAll(pairs: Iterable<Pair<K, V>>): M
        = mutate { it.putAll(pairs) }

public fun <K, V, M : ImmutableMap<out K, V>> M.putAll(pairs: Array<out Pair<K, V>>): M
        = mutate { it.putAll(pairs) }

public fun <K, V, M : ImmutableMap<out K, V>> M.putAll(pairs: Sequence<Pair<K, V>>): M
        = mutate { it.putAll(pairs) }


public operator fun <K, V, M : ImmutableMap<out K, V>> M.minus(key: K): M
        = mutate { it.remove(key) }

public operator fun <K, V, M : ImmutableMap<out K, V>> M.minus(keys: Iterable<K>): M
        = mutate { it.minusAssign(keys) }

public operator fun <K, V, M : ImmutableMap<out K, V>> M.minus(keys: Array<out K>): M
        = mutate { it.minusAssign(keys) }

public operator fun <K, V, M : ImmutableMap<out K, V>> M.minus(keys: Sequence<K>): M
        = mutate { it.minusAssign(keys) }


fun <E> immutableListOf(vararg elements: E): ImmutableList<E> = ImmutableVectorList.emptyOf<E>().addAll(elements.asList())
fun <E> immutableListOf(): ImmutableList<E> = ImmutableVectorList.emptyOf<E>()

fun <E> immutableSetOf(vararg elements: E): ImmutableSet<E> = ImmutableOrderedSet.emptyOf<E>().addAll(elements.asList())
fun <E> immutableSetOf(): ImmutableSet<E> = ImmutableOrderedSet.emptyOf<E>()

fun <E> immutableHashSetOf(vararg elements: E): ImmutableSet<E> = ImmutableHashSet.emptyOf<E>().addAll(elements.asList())

fun <K, V> immutableMapOf(vararg pairs: Pair<K, V>): ImmutableMap<K, V> = ImmutableOrderedMap.emptyOf<K,V>().mutate { it += pairs }
fun <K, V> immutableHashMapOf(vararg pairs: Pair<K, V>): ImmutableMap<K, V> = ImmutableHashMap.emptyOf<K,V>().mutate { it += pairs }

fun <T> Iterable<T>.toImmutableList(): ImmutableList<T> =
        this as? ImmutableList
        ?: (this as? ImmutableList.Builder)?.build()
        ?: immutableListOf<T>() + this

fun <T> Iterable<T>.toPersistentList(): PersistentList<T> =
        this as? PersistentList
        ?: (this as? PersistentList.Builder)?.build()
        ?: ImmutableVectorList.emptyOf<T>() + this


// fun <T> Array<T>.toImmutableList(): ImmutableList<T> = immutableListOf<T>() + this.asList()

fun CharSequence.toImmutableList(): ImmutableList<Char> =
        immutableListOf<Char>().mutate { this.toCollection(it) }

fun <T> Iterable<T>.toImmutableSet(): ImmutableSet<T> =
        this as? ImmutableSet<T>
        ?: (this as? ImmutableSet.Builder)?.build()
        ?: immutableSetOf<T>() + this

fun <T> Set<T>.toImmutableHashSet(): ImmutableSet<T>
    = this as? ImmutableHashSet
        ?: (this as? ImmutableHashSet.Builder)?.build()
        ?: ImmutableHashSet.emptyOf<T>() + this


fun <K, V> Map<K, V>.toImmutableMap(): ImmutableMap<K, V>
    = this as? ImmutableMap
        ?: (this as? ImmutableMap.Builder)?.build()
        ?: ImmutableOrderedMap.emptyOf<K, V>().putAll(this)

fun <K, V> Map<K, V>.toImmutableHashMap(): ImmutableMap<K, V>
    = this as? ImmutableMap
        ?: (this as? ImmutableHashMap.Builder)?.build()
        ?: ImmutableHashMap.emptyOf<K, V>().putAll(this)

fun <K, V> Map<K, V>.toPersistentMap(): PersistentMap<K, V>
    = this as? PersistentMap<K, V>
        ?: (this as? PersistentMap.Builder<K, V>)?.build()
        ?: ImmutableOrderedMap.emptyOf<K, V>().putAll(this)