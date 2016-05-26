@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.collections.immutable

//@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
//inline fun <T> @kotlin.internal.Exact ImmutableCollection<T>.mutate(mutator: (MutableCollection<T>) -> Unit): ImmutableCollection<T> = builder().apply(mutator).build()
// it or this?
inline fun <T> ImmutableSet<T>.mutate(mutator: (MutableSet<T>) -> Unit): ImmutableSet<T> = builder().apply(mutator).build()
inline fun <T> ImmutableList<T>.mutate(mutator: (MutableList<T>) -> Unit): ImmutableList<T> = builder().apply(mutator).build()

inline fun <K, V> ImmutableMap<K, V>.mutate(mutator: (MutableMap<K, V>) -> Unit): ImmutableMap<K, V> = builder().apply(mutator).build()


inline operator fun <E> ImmutableCollection<E>.plus(element: E): ImmutableCollection<E> = with(element)
inline operator fun <E> ImmutableCollection<E>.minus(element: E): ImmutableCollection<E> = without(element)

inline operator fun <E> ImmutableList<E>.plus(element: E): ImmutableList<E> = with(element)
inline operator fun <E> ImmutableList<E>.minus(element: E): ImmutableList<E> = without(element)



fun <E> immutableListOf(vararg elements: E): ImmutableList<E> = ImmutableVectorList.emptyOf<E>().withAll(elements.asList())
fun <E> immutableListOf(): ImmutableList<E> = ImmutableVectorList.emptyOf<E>()

fun <E> immutableSetOf(vararg elements: E): ImmutableSet<E> = ImmutableOrderedSet.emptyOf<E>().withAll(elements.asList())
fun <E> immutableSetOf(): ImmutableSet<E> = ImmutableOrderedSet.emptyOf<E>()

fun <E> immutableHashSetOf(vararg elements: E): ImmutableSet<E> = ImmutableHashSet.emptyOf<E>().withAll(elements.asList())

fun <K, V> immutableHashMapOf(vararg pairs: Pair<K, V>): ImmutableMap<K, V> = ImmutableHashMap.emptyOf<K,V>().mutate { it += pairs }