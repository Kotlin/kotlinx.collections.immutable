/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.collections.immutable

import kotlinx.collections.immutable.implementations.immutableSet.PersistentHashSet

/**
 * Returns a new persistent set with the provided modifications applied,
 * or this instance if no modifications were made in the result of this operation.
 *
 * The mutable set passed to the [mutator] closure has the same contents as this persistent set.
 */
public inline fun <T> PersistentUnorderedSet<T>.mutate(mutator: (MutableSet<T>) -> Unit): PersistentUnorderedSet<T> =
    builder().apply(mutator).build()

/**
 * Returns a new persistent set with the specified [element] added,
 * or this instance if it already contains the element.
 */
public inline operator fun <E> PersistentUnorderedSet<E>.plus(element: E): PersistentUnorderedSet<E> = adding(element)

/**
 * Returns a new persistent set with the specified [element] removed,
 * or this instance if there is no such element in this set.
 */
public inline operator fun <E> PersistentUnorderedSet<E>.minus(element: E): PersistentUnorderedSet<E> = removing(element)

/**
 * Returns a new persistent set with elements of the specified [elements] collection added,
 * or this instance if it already contains every element of the specified collection.
 */
public operator fun <E> PersistentUnorderedSet<E>.plus(elements: Iterable<E>): PersistentUnorderedSet<E> =
    if (elements is Collection) addingAll(elements) else mutate { it.addAll(elements) }

/**
 * Returns a new persistent set with elements of the specified [elements] array added,
 * or this instance if it already contains every element of the specified array.
 */
public operator fun <E> PersistentUnorderedSet<E>.plus(elements: Array<out E>): PersistentUnorderedSet<E> = mutate { it.addAll(elements) }

/**
 * Returns a new persistent set with elements of the specified [elements] sequence added,
 * or this instance if it already contains every element of the specified sequence.
 */
public operator fun <E> PersistentUnorderedSet<E>.plus(elements: Sequence<E>): PersistentUnorderedSet<E> = mutate { it.addAll(elements) }

/**
 * Returns a new persistent set containing all elements of this set
 * except the elements contained in the specified [elements] collection,
 * or this instance if there are no elements to remove.
 */
public operator fun <E> PersistentUnorderedSet<E>.minus(elements: Iterable<E>): PersistentUnorderedSet<E> =
    if (elements is Collection) removingAll(elements) else mutate { it.removeAll(elements) }

/**
 * Returns a new persistent set containing all elements of this set
 * except the elements contained in the specified [elements] array,
 * or this instance if there are no elements to remove.
 */
public operator fun <E> PersistentUnorderedSet<E>.minus(elements: Array<out E>): PersistentUnorderedSet<E> =
    mutate { it.removeAll(elements) }

/**
 * Returns a new persistent set containing all elements of this set
 * except the elements contained in the specified [elements] sequence,
 * or this instance if there are no elements to remove.
 */
public operator fun <E> PersistentUnorderedSet<E>.minus(elements: Sequence<E>): PersistentUnorderedSet<E> =
    mutate { it.removeAll(elements) }

/**
 * Returns a new persistent set with elements in this set that are also
 * contained in the specified [elements] collection,
 * or this instance if no modifications were made in the result of this operation.
 */
public infix fun <E> PersistentUnorderedSet<E>.intersect(elements: Iterable<E>): PersistentUnorderedSet<E> =
    if (elements is Collection) retainingAll(elements) else mutate { it.retainAll(elements) }

/**
 * Returns a new persistent set with the given elements.
 *
 * Order of the elements in the returned set is unspecified.
 */
public fun <E> persistentUnorderedSetOf(vararg elements: E): PersistentUnorderedSet<E> =
    PersistentHashSet.emptyOf<E>().addingAll(elements.asList())

/**
 * Returns an empty persistent set.
 */
public fun <E> persistentUnorderedSetOf(): PersistentUnorderedSet<E> = PersistentHashSet.emptyOf()

/**
 * Returns a persistent set containing all elements from this iterable.
 *
 * If the receiver is already a persistent unordered set, returns it as is.
 * If the receiver is a persistent unordered set builder, calls `build` on it and returns the result.
 *
 * Order of the elements in the returned set is unspecified.
 */
public fun <T> Iterable<T>.toPersistentUnorderedSet(): PersistentUnorderedSet<T> =
    this as? PersistentUnorderedSet<T>
        ?: (this as? PersistentUnorderedSet.Builder<T>)?.build()
        ?: (PersistentHashSet.emptyOf<T>() + this)

/**
 * Returns a persistent set of all elements of this array.
 *
 * Order of the elements in the returned set is unspecified.
 */
public fun <T> Array<out T>.toPersistentUnorderedSet(): PersistentUnorderedSet<T> = persistentUnorderedSetOf<T>() + this

/**
 * Returns a persistent set of all elements of this sequence.
 *
 * Order of the elements in the returned set is unspecified.
 */
public fun <T> Sequence<T>.toPersistentUnorderedSet(): PersistentUnorderedSet<T> = persistentUnorderedSetOf<T>() + this

/**
 * Returns a persistent set of all characters.
 *
 * Order of the elements in the returned set is unspecified.
 */
public fun CharSequence.toPersistentUnorderedSet(): PersistentUnorderedSet<Char> =
    persistentUnorderedSetOf<Char>().mutate { this.toCollection(it) }
