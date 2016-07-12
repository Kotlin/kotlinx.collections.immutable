@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.collections.immutable

//@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
//inline fun <T> @kotlin.internal.Exact ImmutableCollection<T>.mutate(mutator: (MutableCollection<T>) -> Unit): ImmutableCollection<T> = builder().apply(mutator).build()
// it or this?
inline fun <T> ImmutableSet<T>.mutate(mutator: (MutableSet<T>) -> Unit): ImmutableSet<T> = builder().apply(mutator).build()
inline fun <T> ImmutableList<T>.mutate(mutator: (MutableList<T>) -> Unit): ImmutableList<T> = builder().apply(mutator).build()

inline fun <K, V> ImmutableMap<K, V>.mutate(mutator: (MutableMap<K, V>) -> Unit): ImmutableMap<K, V> = builder().apply(mutator).build()


inline operator fun <E> ImmutableCollection<E>.plus(element: E): ImmutableCollection<E> = add(element)
inline operator fun <E> ImmutableCollection<E>.minus(element: E): ImmutableCollection<E> = remove(element)

inline operator fun <E> ImmutableList<E>.plus(element: E): ImmutableList<E> = add(element)
inline operator fun <E> ImmutableList<E>.minus(element: E): ImmutableList<E> = remove(element)


operator fun <E> ImmutableList<E>.plus(elements: Iterable<E>): ImmutableList<E>
        = if (elements is Collection) addAll(elements) else mutate { it.addAll(elements) }

operator fun <E> ImmutableList<E>.minus(elements: Iterable<E>): ImmutableList<E>
        = if (elements is Collection) removeAll(elements) else mutate { it.removeAll(elements) }


inline operator fun <E> ImmutableSet<E>.plus(element: E): ImmutableSet<E> = add(element)
inline operator fun <E> ImmutableSet<E>.minus(element: E): ImmutableSet<E> = remove(element)


operator fun <E> ImmutableSet<E>.plus(elements: Iterable<E>): ImmutableSet<E>
        = if (elements is Collection) addAll(elements) else mutate { it.addAll(elements) }

operator fun <E> ImmutableSet<E>.minus(elements: Iterable<E>): ImmutableSet<E>
        = if (elements is Collection) removeAll(elements) else mutate { it.removeAll(elements) }


inline operator fun <K, V> ImmutableMap<out K, V>.plus(pair: Pair<K, V>): ImmutableMap<K, V>
        = (this as ImmutableMap<K, V>).put(pair.first, pair.second)
inline operator fun <K, V> ImmutableMap<out K, V>.plus(pairs: Iterable<Pair<K, V>>): ImmutableMap<K, V> = putAll(pairs)
inline operator fun <K, V> ImmutableMap<out K, V>.plus(pairs: Array<out Pair<K, V>>): ImmutableMap<K, V> = putAll(pairs)
inline operator fun <K, V> ImmutableMap<out K, V>.plus(pairs: Sequence<Pair<K, V>>): ImmutableMap<K, V> = putAll(pairs)
inline operator fun <K, V> ImmutableMap<out K, V>.plus(map: Map<out K, V>): ImmutableMap<K, V>
        = (this as ImmutableMap<K, V>).putAll(map)

public fun <K, V> ImmutableMap<out K, V>.putAll(pairs: Iterable<Pair<K, V>>): ImmutableMap<K, V>
        = (this as ImmutableMap<K, V>).mutate { it.putAll(pairs) }

public fun <K, V> ImmutableMap<out K, V>.putAll(pairs: Array<out Pair<K, V>>): ImmutableMap<K, V>
        = (this as ImmutableMap<K, V>).mutate { it.putAll(pairs) }

public fun <K, V> ImmutableMap<out K, V>.putAll(pairs: Sequence<Pair<K, V>>): ImmutableMap<K, V>
        = (this as ImmutableMap<K, V>).mutate { it.putAll(pairs) }


// ImmutableMap.minus ?


fun <E> immutableListOf(vararg elements: E): ImmutableList<E> = ImmutableVectorList.emptyOf<E>().addAll(elements.asList())
fun <E> immutableListOf(): ImmutableList<E> = ImmutableVectorList.emptyOf<E>()

fun <E> immutableSetOf(vararg elements: E): ImmutableSet<E> = ImmutableOrderedSet.emptyOf<E>().addAll(elements.asList())
fun <E> immutableSetOf(): ImmutableSet<E> = ImmutableOrderedSet.emptyOf<E>()

fun <E> immutableHashSetOf(vararg elements: E): ImmutableSet<E> = ImmutableHashSet.emptyOf<E>().addAll(elements.asList())

fun <K, V> immutableHashMapOf(vararg pairs: Pair<K, V>): ImmutableMap<K, V> = ImmutableHashMap.emptyOf<K,V>().mutate { it += pairs }

fun <T> Iterable<T>.toImmutableList(): ImmutableList<T> =
        this as? ImmutableList
        ?: (this as? ImmutableList.Builder)?.build()
        ?: immutableListOf<T>() + this


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
        ?: ImmutableHashMap.emptyOf<K, V>().putAll(this) // TODO: ImmutableOrderedMap.emptyOf

fun <K, V> Map<K, V>.toImmutableHashMap(): ImmutableMap<K, V>
    = this as? ImmutableMap
        ?: (this as? ImmutableHashMap.Builder)?.build()
        ?: ImmutableHashMap.emptyOf<K, V>().putAll(this)
