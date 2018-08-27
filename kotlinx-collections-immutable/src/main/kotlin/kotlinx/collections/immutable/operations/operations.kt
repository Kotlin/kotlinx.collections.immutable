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

@file:JvmName("ImmutableCollectionsKt")

package kotlinx.collections.immutable.operations

import kotlinx.collections.immutable.*
import kotlinx.collections.immutable.adapters.ImmutableListAdapter

// Collections.kt

/**
 * Returns an empty read-only list.  The returned list is serializable (JVM).
 * @sample samples.collections.Collections.Lists.emptyReadOnlyList
 */
public fun <T> emptyList(): List<T> = ImmutableArrayList.EMPTY

public fun <T> listOf(element: T): List<T> = ImmutableArrayList(arrayOf<Any?>(element))

/**
 * Returns a new read-only list of given elements.  The returned list is serializable (JVM).
 * @sample samples.collections.Collections.Lists.readOnlyList
 */
public fun <T> listOf(vararg elements: T): List<T> = if (elements.size > 0) ImmutableArrayList(elements as Array<Any?>) else ImmutableArrayList.EMPTY


/**
 * Returns an empty read-only list.  The returned list is serializable (JVM).
 * @sample samples.collections.Collections.Lists.emptyReadOnlyList
 */
//@kotlin.internal.InlineOnly
public fun <T> listOf(): List<T> = ImmutableArrayList.EMPTY


/**
 * Returns a new read-only list either of single given element, if it is not null, or empty list if the element is null. The returned list is serializable (JVM).
 * @sample samples.collections.Collections.Lists.listOfNotNull
 */
public fun <T : Any> listOfNotNull(element: T?): List<T> = if (element != null) listOf(element) else emptyList()

/**
 * Returns a new read-only list only of those given elements, that are not null.  The returned list is serializable (JVM).
 * @sample samples.collections.Collections.Lists.listOfNotNull
 */
public fun <T : Any> listOfNotNull(vararg elements: T?): List<T> = elements.filterNotNull()

/**
 * Creates a new read-only list with the specified [size], where each element is calculated by calling the specified
 * [init] function. The [init] function returns a list element given its index.
 * @sample samples.collections.Collections.Lists.readOnlyListFromInitializer
 */
@SinceKotlin("1.1")
public fun <T> List(size: Int, init: (index: Int) -> T): List<T> = ImmutableArrayList(Array<Any?>(size, init))






/**
 * Returns a reversed immutable view of the original List.
 * @sample samples.collections.ReversedViews.asReversedList
 */
public fun <T> ImmutableList<T>.asReversed(): ImmutableList<T> = ImmutableListAdapter((this as List<T>).asReversed())


internal fun <T> List<T>.optimizeReadOnlyList() = when (size) {
    0 -> emptyList<T>()
//    1 -> kotlin.collections.listOf(this[0])
    else -> toImmutableList()
}


/**
 * Returns the size of this iterable if it is known, or the specified [default] value otherwise.
 */
@PublishedApi
internal fun <T> Iterable<T>.collectionSizeOrDefault(default: Int): Int = if (this is Collection<*>) this.size else default

/** Converts this collection to a set, when it's worth so and it doesn't change contains method behavior. */
internal fun <T> Iterable<T>.convertToSetForSetOperationWith(source: Iterable<T>): Collection<T> =
        when(this) {
            is Set -> this
            is Collection ->
                when {
                    source is Collection && source.size < 2 -> this
                    else -> if (this.safeToConvertToSet()) toHashSet() else this
                }
            else -> toHashSet()
        }
/** Returns true when it's safe to convert this collection to a set without changing contains method behavior. */
private fun <T> Collection<T>.safeToConvertToSet() = size > 2 && this is ArrayList



@PublishedApi
internal fun <T> builderFor(source: Iterable<T>, estimateSizeDelta: Int): MutableList<T> =
        when (source) {
            is PersistentList -> source.builder()
            is Collection -> ArrayList<T>(source.size + estimateSizeDelta).apply { addAll(source) }
            else -> source.toMutableList()
        }

@PublishedApi
internal fun <T> emptyBuilderFor(source: Iterable<T>): MutableList<T> =
        if (source is PersistentList) source.clear().builder() else ArrayList()

@PublishedApi
internal fun <T> emptyBuilderOfType(source: Iterable<*>): MutableList<T> =
        // assumption: builder of empty list can be switched to any other element type
        if (source is PersistentList) source.clear().builder() as MutableList<T> else ArrayList()


@PublishedApi
internal fun <T> List<T>.toImmutableList() =
    this as? ImmutableList
            ?: (this as? PersistentList.Builder)?.build()
            ?: ImmutableArrayList(this)


// generated _Collections.kt

/**
 * Returns a list containing all elements except first [n] elements.
 *
 * @sample samples.collections.Collections.Transformations.drop
 */
public fun <T> Iterable<T>.drop(n: Int): List<T> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    if (n == 0) return toList()
    val list: ArrayList<T>
    if (this is Collection<*>) {
        val resultSize = size - n
        if (resultSize <= 0)
            return emptyList()
        if (resultSize == 1)
            return listOf(last())
        list = ArrayList<T>(resultSize)
        if (this is List<T>) {
            if (this is RandomAccess) {
                for (index in n until size)
                    list.add(this[index])
            } else {
                for (item in listIterator(n))
                    list.add(item)
            }
            return list
        }
    }
    else {
        list = ArrayList<T>()
    }
    var count = 0
    for (item in this) {
        if (count++ >= n) list.add(item)
    }
    return list.optimizeReadOnlyList()
}

/**
 * Returns a list containing all elements except last [n] elements.
 *
 * @sample samples.collections.Collections.Transformations.drop
 */
public fun <T> List<T>.dropLast(n: Int): List<T> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    return take((size - n).coerceAtLeast(0))
}

/**
 * Returns a list containing all elements except last elements that satisfy the given [predicate].
 *
 * @sample samples.collections.Collections.Transformations.drop
 */
public inline fun <T> List<T>.dropLastWhile(predicate: (T) -> Boolean): List<T> {
    if (!isEmpty()) {
        val iterator = listIterator(size)
        while (iterator.hasPrevious()) {
            if (!predicate(iterator.previous())) {
                return take(iterator.nextIndex() + 1)
            }
        }
    }
    return emptyList()
}

/**
 * Returns a list containing all elements except first elements that satisfy the given [predicate].
 *
 * @sample samples.collections.Collections.Transformations.drop
 */
public inline fun <T> Iterable<T>.dropWhile(predicate: (T) -> Boolean): List<T> {
    var yielding = false
    val list = emptyBuilderFor(this)
    for (item in this)
        if (yielding)
            list.add(item)
        else if (!predicate(item)) {
            list.add(item)
            yielding = true
        }
    return list.toImmutableList()
}

/**
 * Returns a list containing only elements matching the given [predicate].
 */
public inline fun <T> Iterable<T>.filter(predicate: (T) -> Boolean): List<T> {
    return filterTo(emptyBuilderFor(this), predicate).toImmutableList()
}


/**
 * Returns a list containing all elements not matching the given [predicate].
 */
public fun <T> PersistentList<T>.filter(predicate: (T) -> Boolean): PersistentList<T> = mutate { it.retainAll(predicate) }


/**
 * Returns a list containing only elements matching the given [predicate].
 * @param [predicate] function that takes the index of an element and the element itself
 * and returns the result of predicate evaluation on the element.
 */
public inline fun <T> Iterable<T>.filterIndexed(predicate: (index: Int, T) -> Boolean): List<T> {
    return filterIndexedTo(emptyBuilderFor(this), predicate).toImmutableList()
}

///**
// * Returns a list containing all elements that are instances of specified type parameter R.
// */
//public inline fun <reified R> Iterable<*>.filterIsInstance(): List<@kotlin.internal.NoInfer R> {
//    return filterIsInstanceTo(ArrayList<R>())
//}


/**
 * Returns a list containing all elements not matching the given [predicate].
 */
public inline fun <T> Iterable<T>.filterNot(predicate: (T) -> Boolean): List<T> {
    return filterNotTo(emptyBuilderFor(this), predicate).toImmutableList()
}

/**
 * Returns a list containing all elements not matching the given [predicate].
 */
public fun <T> PersistentList<T>.filterNot(predicate: (T) -> Boolean): PersistentList<T> = removeAll(predicate)

/**
 * Returns a list containing all elements that are not `null`.
 */
public fun <T : Any> Iterable<T?>.filterNotNull(): List<T> {
    return filterNotNullTo(emptyBuilderOfType<T>(this)).toImmutableList()
}

/**
 * Returns a list containing elements at indices in the specified [indices] range.
 */
public fun <T> List<T>.slice(indices: IntRange): List<T> {
    if (indices.isEmpty()) return listOf()
    return emptyBuilderFor(this).also { it += this.subList(indices.start, indices.endInclusive + 1) }.toImmutableList()
}

/**
 * Returns a list containing elements at specified [indices].
 */
public fun <T> List<T>.slice(indices: Iterable<Int>): List<T> {
    val size = indices.collectionSizeOrDefault(10)
    if (size == 0) return emptyList()
    val list = emptyBuilderFor(this) // ArrayList<T>(size)
    for (index in indices) {
        list.add(get(index))
    }
    return list.toImmutableList()
}

/**
 * Returns a list containing first [n] elements.
 *
 * @sample samples.collections.Collections.Transformations.take
 */
public fun <T> Iterable<T>.take(n: Int): List<T> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    if (n == 0) return emptyList()
    if (this is Collection<T>) {
        if (n >= size) return toList()
        if (n == 1) return listOf(first())
    }
    var count = 0
    val list = ArrayList<T>(n)
    for (item in this) {
        if (count++ == n)
            break
        list.add(item)
    }
    return list.optimizeReadOnlyList()
}

/**
 * Returns a list containing last [n] elements.
 *
 * @sample samples.collections.Collections.Transformations.take
 */
public fun <T> List<T>.takeLast(n: Int): List<T> {
    require(n >= 0) { "Requested element count $n is less than zero." }
    if (n == 0) return emptyList()
    val size = size
    if (n >= size) return toList()
    if (n == 1) return listOf(last())
    val list = ArrayList<T>(n)
    if (this is RandomAccess) {
        for (index in size - n until size)
            list.add(this[index])
    } else {
        for (item in listIterator(size - n))
            list.add(item)
    }
    return list.optimizeReadOnlyList()
}

/**
 * Returns a list containing last elements satisfying the given [predicate].
 *
 * @sample samples.collections.Collections.Transformations.take
 */
public inline fun <T> List<T>.takeLastWhile(predicate: (T) -> Boolean): List<T> {
    if (isEmpty())
        return emptyList()
    val iterator = listIterator(size)
    while (iterator.hasPrevious()) {
        if (!predicate(iterator.previous())) {
            iterator.next()
            val expectedSize = size - iterator.nextIndex()
            if (expectedSize == 0) return emptyList()
            return ArrayList<T>(expectedSize).apply {
                while (iterator.hasNext())
                    add(iterator.next())
            }
        }
    }
    return toImmutableList()
}

/**
 * Returns a list containing first elements satisfying the given [predicate].
 *
 * @sample samples.collections.Collections.Transformations.take
 */
public inline fun <T> Iterable<T>.takeWhile(predicate: (T) -> Boolean): List<T> {
    val list = ArrayList<T>()
    for (item in this) {
        if (!predicate(item))
            break
        list.add(item)
    }
    return list
}


/**
 * Returns a list with elements in reversed order.
 */
public fun <T> Iterable<T>.reversed(): List<T> {
    if (this is Collection && size <= 1) return toList()
    val list = toMutableList()
    list.reverse()
    return list
}

/**
 * Sorts elements in the list in-place according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <T, R : Comparable<R>> MutableList<T>.sortBy(crossinline selector: (T) -> R?): Unit {
    if (size > 1) sortWith(compareBy(selector))
}

/**
 * Sorts elements in the list in-place descending according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <T, R : Comparable<R>> MutableList<T>.sortByDescending(crossinline selector: (T) -> R?): Unit {
    if (size > 1) sortWith(compareByDescending(selector))
}

/**
 * Sorts elements in the list in-place descending according to their natural sort order.
 */
public fun <T : Comparable<T>> MutableList<T>.sortDescending(): Unit {
    sortWith(reverseOrder())
}

/**
 * Returns a list of all elements sorted according to their natural sort order.
 */
public fun <T : Comparable<T>> Iterable<T>.sorted(): List<T> {
    if (this is Collection) {
        if (size <= 1) return this.toList()
        @Suppress("UNCHECKED_CAST")
        return (toTypedArray<Comparable<T>>() as Array<T>).apply { sort() }.asList()
    }
    return toMutableList().apply { sort() }
}

/**
 * Returns a list of all elements sorted according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <T, R : Comparable<R>> Iterable<T>.sortedBy(crossinline selector: (T) -> R?): List<T> {
    return sortedWith(compareBy(selector))
}

/**
 * Returns a list of all elements sorted descending according to natural sort order of the value returned by specified [selector] function.
 */
public inline fun <T, R : Comparable<R>> Iterable<T>.sortedByDescending(crossinline selector: (T) -> R?): List<T> {
    return sortedWith(compareByDescending(selector))
}

/**
 * Returns a list of all elements sorted descending according to their natural sort order.
 */
public fun <T : Comparable<T>> Iterable<T>.sortedDescending(): List<T> {
    return sortedWith(reverseOrder())
}

/**
 * Returns a list of all elements sorted according to the specified [comparator].
 */
public fun <T> Iterable<T>.sortedWith(comparator: Comparator<in T>): List<T> {
    if (this is Collection) {
        if (size <= 1) return this.toList()
        @Suppress("UNCHECKED_CAST")
        return (toTypedArray<Any?>() as Array<T>).apply { sortWith(comparator) }.asList()
    }
    return toMutableList().apply { sortWith(comparator) }
}

/*
/**
 * Returns a [Map] containing key-value pairs provided by [transform] function
 * applied to elements of the given collection.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original collection.
 */
public inline fun <T, K, V> Iterable<T>.associate(transform: (T) -> Pair<K, V>): Map<K, V> {
    val capacity = mapCapacity(collectionSizeOrDefault(10)).coerceAtLeast(16)
    return associateTo(LinkedHashMap<K, V>(capacity), transform)
}

/**
 * Returns a [Map] containing the elements from the given collection indexed by the key
 * returned from [keySelector] function applied to each element.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original collection.
 */
public inline fun <T, K> Iterable<T>.associateBy(keySelector: (T) -> K): Map<K, T> {
    val capacity = mapCapacity(collectionSizeOrDefault(10)).coerceAtLeast(16)
    return associateByTo(LinkedHashMap<K, T>(capacity), keySelector)
}

/**
 * Returns a [Map] containing the values provided by [valueTransform] and indexed by [keySelector] functions applied to elements of the given collection.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 *
 * The returned map preserves the entry iteration order of the original collection.
 */
public inline fun <T, K, V> Iterable<T>.associateBy(keySelector: (T) -> K, valueTransform: (T) -> V): Map<K, V> {
    val capacity = mapCapacity(collectionSizeOrDefault(10)).coerceAtLeast(16)
    return associateByTo(LinkedHashMap<K, V>(capacity), keySelector, valueTransform)
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs,
 * where key is provided by the [keySelector] function applied to each element of the given collection
 * and value is the element itself.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 */
public inline fun <T, K, M : MutableMap<in K, in T>> Iterable<T>.associateByTo(destination: M, keySelector: (T) -> K): M {
    for (element in this) {
        destination.put(keySelector(element), element)
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs,
 * where key is provided by the [keySelector] function and
 * and value is provided by the [valueTransform] function applied to elements of the given collection.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 */
public inline fun <T, K, V, M : MutableMap<in K, in V>> Iterable<T>.associateByTo(destination: M, keySelector: (T) -> K, valueTransform: (T) -> V): M {
    for (element in this) {
        destination.put(keySelector(element), valueTransform(element))
    }
    return destination
}

/**
 * Populates and returns the [destination] mutable map with key-value pairs
 * provided by [transform] function applied to each element of the given collection.
 *
 * If any of two pairs would have the same key the last one gets added to the map.
 */
public inline fun <T, K, V, M : MutableMap<in K, in V>> Iterable<T>.associateTo(destination: M, transform: (T) -> Pair<K, V>): M {
    for (element in this) {
        destination += transform(element)
    }
    return destination
}
*/


/**
 * Returns a [List] containing all elements.
 */
public fun <T> Iterable<T>.toList(): List<T> {
    if (this is ImmutableList)
        return this
    if (this is Collection) {
        return when (size) {
            0 -> emptyList()
//            1 -> listOf(if (this is List) get(0) else iterator().next())
            else -> ImmutableArrayList(this)
        }
    }
    return this.toMutableList().optimizeReadOnlyList()
}
//
///**
// * Returns a [Set] of all elements.
// *
// * The returned set preserves the element iteration order of the original collection.
// */
//public fun <T> Iterable<T>.toSet(): Set<T> {
//    if (this is Collection) {
//        return when (size) {
//            0 -> emptySet()
//            1 -> setOf(if (this is List) this[0] else iterator().next())
//            else -> toCollection(LinkedHashSet<T>(mapCapacity(size)))
//        }
//    }
//    return toCollection(LinkedHashSet<T>()).optimizeReadOnlySet()
//}

/**
 * Returns a single list of all elements yielded from results of [transform] function being invoked on each element of original collection.
 */
public inline fun <T, R> Iterable<T>.flatMap(transform: (T) -> Iterable<R>): List<R> {
    return flatMapTo(emptyBuilderOfType<R>(this), transform).toImmutableList()
}

// TODO: Apply mapValuesInPlace to transform lists to immutable?
///**
// * Groups elements of the original collection by the key returned by the given [keySelector] function
// * applied to each element and returns a map where each group key is associated with a list of corresponding elements.
// *
// * The returned map preserves the entry iteration order of the keys produced from the original collection.
// *
// * @sample samples.collections.Collections.Transformations.groupBy
// */
//public inline fun <T, K> Iterable<T>.groupBy(keySelector: (T) -> K): Map<K, List<T>> {
//    return groupByTo(LinkedHashMap<K, MutableList<T>>(), keySelector)
//}

///**
// * Groups values returned by the [valueTransform] function applied to each element of the original collection
// * by the key returned by the given [keySelector] function applied to the element
// * and returns a map where each group key is associated with a list of corresponding values.
// *
// * The returned map preserves the entry iteration order of the keys produced from the original collection.
// *
// * @sample samples.collections.Collections.Transformations.groupByKeysAndValues
// */
//public inline fun <T, K, V> Iterable<T>.groupBy(keySelector: (T) -> K, valueTransform: (T) -> V): Map<K, List<V>> {
//    return groupByTo(LinkedHashMap<K, MutableList<V>>(), keySelector, valueTransform)
//}



/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element in the original collection.
 */
public inline fun <T, R> Iterable<T>.map(transform: (T) -> R): List<R> {
    return mapTo(emptyBuilderOfType<R>(this) /* ArrayList<R>(collectionSizeOrDefault(10)) */, transform).toImmutableList()
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element and its index in the original collection.
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 */
public inline fun <T, R> Iterable<T>.mapIndexed(transform: (index: Int, T) -> R): List<R> {
    return mapIndexedTo(emptyBuilderOfType<R>(this) /* ArrayList<R>(collectionSizeOrDefault(10)) */, transform).toImmutableList()
}

/**
 * Returns a list containing only the non-null results of applying the given [transform] function
 * to each element and its index in the original collection.
 * @param [transform] function that takes the index of an element and the element itself
 * and returns the result of the transform applied to the element.
 */
public inline fun <T, R : Any> Iterable<T>.mapIndexedNotNull(transform: (index: Int, T) -> R?): List<R> {
    return mapIndexedNotNullTo(emptyBuilderOfType<R>(this), transform).toImmutableList()
}


/**
 * Returns a list containing only the non-null results of applying the given [transform] function
 * to each element in the original collection.
 */
public inline fun <T, R : Any> Iterable<T>.mapNotNull(transform: (T) -> R?): List<R> {
    return mapNotNullTo(emptyBuilderOfType<R>(this), transform).toImmutableList()
}

/**
 * Returns a list containing only distinct elements from the given collection.
 *
 * The elements in the resulting list are in the same order as they were in the source collection.
 */
public fun <T> Iterable<T>.distinct(): List<T> {
    return this.toMutableSet().toImmutableList()
}

/**
 * Returns a list containing only elements from the given collection
 * having distinct keys returned by the given [selector] function.
 *
 * The elements in the resulting list are in the same order as they were in the source collection.
 */
public inline fun <T, K> Iterable<T>.distinctBy(selector: (T) -> K): List<T> {
    val set = HashSet<K>()
    val list = emptyBuilderFor(this)
    for (e in this) {
        val key = selector(e)
        if (set.add(key))
            list.add(e)
    }
    return list.toImmutableList()
}

/**
 * Returns a set containing all elements that are contained by both this set and the specified collection.
 *
 * The returned set preserves the element iteration order of the original collection.
 */
public infix fun <T> Iterable<T>.intersect(other: Iterable<T>): Set<T> {
    val set = this.toMutableSet()
    set.retainAll(other)
    return set // TODO
}

/**
 * Returns a set containing all elements that are contained by this collection and not contained by the specified collection.
 *
 * The returned set preserves the element iteration order of the original collection.
 */
public infix fun <T> Iterable<T>.subtract(other: Iterable<T>): Set<T> {
    val set = this.toMutableSet()
    set.removeAll(other)
    return set // TODO
}


/**
 * Returns a set containing all distinct elements from both collections.
 *
 * The returned set preserves the element iteration order of the original collection.
 * Those elements of the [other] collection that are unique are iterated in the end
 * in the order of the [other] collection.
 */
public infix fun <T> Iterable<T>.union(other: Iterable<T>): Set<T> {
    val set = this.toMutableSet()
    set.addAll(other)
    return set // TODO
}



/**
 * Returns a list containing all elements of the original collection without the first occurrence of the given [element].
 */
public operator fun <T> Iterable<T>.minus(element: T): List<T> {
    if (this is PersistentList) return this.remove(element)
    val result = ArrayList<T>(collectionSizeOrDefault(10))
    var removed = false
    this.filterTo(result) { if (!removed && it == element) { removed = true; false } else true }
    if (!removed && this is ImmutableList) return this
    return result.toImmutableList()
}

/**
 * Returns a list containing all elements of the original collection except the elements contained in the given [elements] array.
 */
public operator fun <T> Iterable<T>.minus(elements: Array<out T>): List<T> {
    if (elements.isEmpty()) return this.toList()
    val other = elements.toHashSet()
    if (this is PersistentList) return this.removeAll(other)
    return this.filterNot { it in other }
}

/**
 * Returns a list containing all elements of the original collection except the elements contained in the given [elements] collection.
 */
public operator fun <T> Iterable<T>.minus(elements: Iterable<T>): List<T> {
    val other = elements.convertToSetForSetOperationWith(this)
    if (other.isEmpty())
        return this.toList()
    if (this is PersistentList) return this.removeAll(other)
    return this.filterNot { it in other }
}

/**
 * Returns a list containing all elements of the original collection except the elements contained in the given [elements] sequence.
 */
public operator fun <T> Iterable<T>.minus(elements: Sequence<T>): List<T> {
    val other = elements.toHashSet()
    return when {
        other.isEmpty() -> this.toList()
        this is PersistentList -> this.removeAll(other)
        else -> this.filterNot { it in other }
    }
}

/**
 * Returns a list containing all elements of the original collection without the first occurrence of the given [element].
 */
//@kotlin.internal.InlineOnly
public inline fun <T> Iterable<T>.minusElement(element: T): List<T> {
    return minus(element)
}

/**
 * Splits the original collection into pair of lists,
 * where *first* list contains elements for which [predicate] yielded `true`,
 * while *second* list contains elements for which [predicate] yielded `false`.
 */
public inline fun <T> Iterable<T>.partition(predicate: (T) -> Boolean): Pair<List<T>, List<T>> {
    val first = emptyBuilderFor(this)
    val second = emptyBuilderFor(this)
    for (element in this) {
        if (predicate(element)) {
            first.add(element)
        } else {
            second.add(element)
        }
    }
    return Pair(first.toImmutableList(), second.toImmutableList())
}

/**
 * Returns a list containing all elements of the original collection and then the given [element].
 */
public operator fun <T> Iterable<T>.plus(element: T): List<T> {
    if (this is Collection) return this.plus(element)
    val result = ArrayList<T>()
    result.addAll(this)
    result.add(element)
    return result.toImmutableList()
}

/**
 * Returns a list containing all elements of the original collection and then the given [element].
 */
public operator fun <T> Collection<T>.plus(element: T): List<T> {
    if (this is PersistentList) return this.add(element)
    val result = ArrayList<T>(size + 1) // TODO
    result.addAll(this)
    result.add(element)
    return result.toImmutableList()
}

/**
 * Returns a list containing all elements of the original collection and then all elements of the given [elements] array.
 */
public operator fun <T> Iterable<T>.plus(elements: Array<out T>): List<T> {
    if (this is Collection) return this.plus(elements)
    val result = ArrayList<T>()
    result.addAll(this)
    result.addAll(elements)
    return result.toImmutableList()
}

/**
 * Returns a list containing all elements of the original collection and then all elements of the given [elements] array.
 */
public operator fun <T> Collection<T>.plus(elements: Array<out T>): List<T> {
    if (this is PersistentList) return this.addAll(elements.asList())
    if (elements.isEmpty()) return this.toList()
    val result = ArrayList<T>(this.size + elements.size)
    result.addAll(this)
    result.addAll(elements)
    return result.toImmutableList()
}

/**
 * Returns a list containing all elements of the original collection and then all elements of the given [elements] collection.
 */
public operator fun <T> Iterable<T>.plus(elements: Iterable<T>): List<T> {
    if (this is Collection) return this.plus(elements)
    val result = ArrayList<T>()
    result.addAll(this)
    if (result.isEmpty()) return elements.toList()
    result.addAll(elements)
    return result.toImmutableList()
}

/**
 * Returns a list containing all elements of the original collection and then all elements of the given [elements] collection.
 */
public operator fun <T> Collection<T>.plus(elements: Iterable<T>): List<T> {
    if (elements is Collection) {
        if (this is PersistentList) return this.addAll(elements)
        if (elements.isEmpty()) return this.toList()

        val result = builderFor(this, estimateSizeDelta = elements.size)
        result.addAll(elements)
        return result.toImmutableList()
    } else {
        val iterator = elements.iterator()
        if (!iterator.hasNext()) return this.toList()

        val result = builderFor(this, estimateSizeDelta = 10)
        do { result.add(iterator.next()) } while (iterator.hasNext())
        return result.toImmutableList()
    }
}

/**
 * Returns a list containing all elements of the original collection and then all elements of the given [elements] sequence.
 */
public operator fun <T> Iterable<T>.plus(elements: Sequence<T>): List<T> {
    val iterator = elements.iterator()
    if (!iterator.hasNext()) return this.toList()

    val result = builderFor(this, estimateSizeDelta = 10)
    do { result.add(iterator.next()) } while (iterator.hasNext())
    return result.toImmutableList()
}

/**
 * Returns a list containing all elements of the original collection and then all elements of the given [elements] sequence.
 */
public operator fun <T> Collection<T>.plus(elements: Sequence<T>): List<T> {
    return (this as Iterable<T>).plus(elements) // no specialization required anymore
}

/**
 * Returns a list containing all elements of the original collection and then the given [element].
 */
//@kotlin.internal.InlineOnly
public inline fun <T> Iterable<T>.plusElement(element: T): List<T> {
    return plus(element)
}

/**
 * Returns a list containing all elements of the original collection and then the given [element].
 */
//@kotlin.internal.InlineOnly
public inline fun <T> Collection<T>.plusElement(element: T): List<T> {
    return plus(element)
}

/*
/**
 * Returns a list of snapshots of the window of the given [size]
 * sliding along this collection with the given [step], where each
 * snapshot is a list.
 *
 * Several last lists may have less elements than the given [size].
 *
 * Both [size] and [step] must be positive and can be greater than the number of elements in this collection.
 * @param size the number of elements to take in each window
 * @param step the number of elements to move the window forward by on an each step, by default 1
 * @param partialWindows controls whether or not to keep partial windows in the end if any,
 * by default `false` which means partial windows won't be preserved
 *
 * @sample samples.collections.Sequences.Transformations.takeWindows
 */
@SinceKotlin("1.2")
public fun <T> Iterable<T>.windowed(size: Int, step: Int = 1, partialWindows: Boolean = false): List<List<T>> {
    checkWindowSizeStep(size, step)
    if (this is RandomAccess && this is List) {
        val thisSize = this.size
        val result = ArrayList<List<T>>((thisSize + step - 1) / step)
        var index = 0
        while (index < thisSize) {
            val windowSize = size.coerceAtMost(thisSize - index)
            if (windowSize < size && !partialWindows) break
            result.add(List(windowSize) { this[it + index] })
            index += step
        }
        return result
    }
    val result = ArrayList<List<T>>()
    windowedIterator(iterator(), size, step, partialWindows, reuseBuffer = false).forEach {
        result.add(it)
    }
    return result
}

/**
 * Returns a list of results of applying the given [transform] function to
 * an each list representing a view over the window of the given [size]
 * sliding along this collection with the given [step].
 *
 * Note that the list passed to the [transform] function is ephemeral and is valid only inside that function.
 * You should not store it or allow it to escape in some way, unless you made a snapshot of it.
 * Several last lists may have less elements than the given [size].
 *
 * Both [size] and [step] must be positive and can be greater than the number of elements in this collection.
 * @param size the number of elements to take in each window
 * @param step the number of elements to move the window forward by on an each step, by default 1
 * @param partialWindows controls whether or not to keep partial windows in the end if any,
 * by default `false` which means partial windows won't be preserved
 *
 * @sample samples.collections.Sequences.Transformations.averageWindows
 */
@SinceKotlin("1.2")
public fun <T, R> Iterable<T>.windowed(size: Int, step: Int = 1, partialWindows: Boolean = false, transform: (List<T>) -> R): List<R> {
    checkWindowSizeStep(size, step)
    if (this is RandomAccess && this is List) {
        val thisSize = this.size
        val result = ArrayList<R>((thisSize + step - 1) / step)
        val window = MovingSubList(this)
        var index = 0
        while (index < thisSize) {
            window.move(index, (index + size).coerceAtMost(thisSize))
            if (!partialWindows && window.size < size) break
            result.add(transform(window))
            index += step
        }
        return result
    }
    val result = ArrayList<R>()
    windowedIterator(iterator(), size, step, partialWindows, reuseBuffer = true).forEach {
        result.add(transform(it))
    }
    return result
}
*/

/**
 * Returns a list of pairs built from the elements of `this` collection and the [other] array with the same index.
 * The returned list has length of the shortest collection.
 *
 * @sample samples.collections.Iterables.Operations.zipIterable
 */
public infix fun <T, R> Iterable<T>.zip(other: Array<out R>): List<Pair<T, R>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of values built from the elements of `this` collection and the [other] array with the same index
 * using the provided [transform] function applied to each pair of elements.
 * The returned list has length of the shortest collection.
 *
 * @sample samples.collections.Iterables.Operations.zipIterableWithTransform
 */
public inline fun <T, R, V> Iterable<T>.zip(other: Array<out R>, transform: (a: T, b: R) -> V): List<V> {
    val arraySize = other.size
    val list = ArrayList<V>(minOf(collectionSizeOrDefault(10), arraySize))
    var i = 0
    for (element in this) {
        if (i >= arraySize) break
        list.add(transform(element, other[i++]))
    }
    return list.toImmutableList()
}

/**
 * Returns a list of pairs built from the elements of `this` collection and [other] collection with the same index.
 * The returned list has length of the shortest collection.
 *
 * @sample samples.collections.Iterables.Operations.zipIterable
 */
public infix fun <T, R> Iterable<T>.zip(other: Iterable<R>): List<Pair<T, R>> {
    return zip(other) { t1, t2 -> t1 to t2 }
}

/**
 * Returns a list of values built from the elements of `this` collection and the [other] collection with the same index
 * using the provided [transform] function applied to each pair of elements.
 * The returned list has length of the shortest collection.
 *
 * @sample samples.collections.Iterables.Operations.zipIterableWithTransform
 */
public inline fun <T, R, V> Iterable<T>.zip(other: Iterable<R>, transform: (a: T, b: R) -> V): List<V> {
    val first = iterator()
    val second = other.iterator()
    val list = ArrayList<V>(minOf(collectionSizeOrDefault(10), other.collectionSizeOrDefault(10)))
    while (first.hasNext() && second.hasNext()) {
        list.add(transform(first.next(), second.next()))
    }
    return list.toImmutableList()
}

/**
 * Returns a list of pairs of each two adjacent elements in this collection.
 *
 * The returned list is empty if this collection contains less than two elements.
 *
 * @sample samples.collections.Collections.Transformations.zipWithNext
 */
//@SinceKotlin("1.2")
public fun <T> Iterable<T>.zipWithNext(): List<Pair<T, T>> {
    return zipWithNext { a, b -> a to b }
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to an each pair of two adjacent elements in this collection.
 *
 * The returned list is empty if this collection contains less than two elements.
 *
 * @sample samples.collections.Collections.Transformations.zipWithNextToFindDeltas
 */
//@SinceKotlin("1.2")
public inline fun <T, R> Iterable<T>.zipWithNext(transform: (a: T, b: T) -> R): List<R> {
    val iterator = iterator()
    if (!iterator.hasNext()) return emptyList()
    val result = mutableListOf<R>()
    var current = iterator.next()
    while (iterator.hasNext()) {
        val next = iterator.next()
        result.add(transform(current, next))
        current = next
    }
    return result.toImmutableList()
}

