@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.collections.immutable

//@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
//inline fun <T> @kotlin.internal.Exact ImmutableCollection<T>.mutate(mutator: (MutableCollection<T>) -> Unit): ImmutableCollection<T> = builder().apply(mutator).build()
// it or this?
inline fun <T> ImmutableSet<T>.mutate(mutator: (MutableSet<T>) -> Unit): ImmutableSet<T> = builder().apply(mutator).build()
inline fun <T> ImmutableList<T>.mutate(mutator: (MutableList<T>) -> Unit): ImmutableList<T> = builder().apply(mutator).build()

inline fun <K, V> ImmutableMap<K, V>.mutate(mutator: (MutableMap<K, V>) -> Unit): ImmutableMap<K, V> = builder().apply(mutator).build()


inline operator fun <E> ImmutableCollection<E>.plus(element: E): ImmutableCollection<E> = added(element)
inline operator fun <E> ImmutableCollection<E>.minus(element: E): ImmutableCollection<E> = removed(element)

inline operator fun <E> ImmutableList<E>.plus(element: E): ImmutableList<E> = added(element)
inline operator fun <E> ImmutableList<E>.minus(element: E): ImmutableList<E> = removed(element)


operator fun <E> ImmutableList<E>.plus(elements: Iterable<E>): ImmutableList<E>
        = if (elements is Collection) addedAll(elements) else mutate { it.addAll(elements) }

operator fun <E> ImmutableList<E>.minus(elements: Iterable<E>): ImmutableList<E>
        = if (elements is Collection) removedAll(elements) else mutate { it.removeAll(elements) }


inline operator fun <E> ImmutableSet<E>.plus(element: E): ImmutableSet<E> = added(element)
inline operator fun <E> ImmutableSet<E>.minus(element: E): ImmutableSet<E> = removed(element)


operator fun <E> ImmutableSet<E>.plus(elements: Iterable<E>): ImmutableSet<E>
        = if (elements is Collection) addedAll(elements) else mutate { it.addAll(elements) }

operator fun <E> ImmutableSet<E>.minus(elements: Iterable<E>): ImmutableSet<E>
        = if (elements is Collection) removedAll(elements) else mutate { it.removeAll(elements) }


inline operator fun <K, V> ImmutableMap<K, V>.plus(pair: Pair<K, V>): ImmutableMap<K, V> = added(pair.first, pair.second)

operator fun <K, V> ImmutableMap<K, V>.plus(pairs: Iterable<Pair<K, V>>): ImmutableMap<K, V>
        = mutate { it.putAll(pairs) }

// ImmutableMap.minus ?


fun <E> immutableListOf(vararg elements: E): ImmutableList<E> = ImmutableVectorList.emptyOf<E>().addedAll(elements.asList())
fun <E> immutableListOf(): ImmutableList<E> = ImmutableVectorList.emptyOf<E>()

fun <E> immutableSetOf(vararg elements: E): ImmutableSet<E> = ImmutableOrderedSet.emptyOf<E>().addedAll(elements.asList())
fun <E> immutableSetOf(): ImmutableSet<E> = ImmutableOrderedSet.emptyOf<E>()

fun <E> immutableHashSetOf(vararg elements: E): ImmutableSet<E> = ImmutableHashSet.emptyOf<E>().addedAll(elements.asList())

fun <K, V> immutableHashMapOf(vararg pairs: Pair<K, V>): ImmutableMap<K, V> = ImmutableHashMap.emptyOf<K,V>().mutate { it += pairs }