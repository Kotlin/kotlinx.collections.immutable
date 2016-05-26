package kotlinx.collections.immutable

import org.pcollections.PMap
import java.util.*

public abstract class AbstractImmutableMap<K, out V> protected constructor(protected val impl: PMap<K, @UnsafeVariance V>) : ImmutableMap<K, V> {

    override val size: Int get() = impl.size
    override fun isEmpty(): Boolean = impl.isEmpty()
    override fun containsKey(key: K): Boolean = impl.containsKey(key)
    override fun containsValue(value: @UnsafeVariance V): Boolean = impl.containsValue(value)

    override fun get(key: K): V? = impl.get(key)

    override fun equals(other: Any?): Boolean = impl.equals(other)
    override fun hashCode(): Int = impl.hashCode()
    override fun toString(): String = impl.toString()


    private var keysWrapped: ImmutableSet<K>? = null
    override val keys: ImmutableSet<K> get() = keysWrapped ?: ImmutableSetWrapper(impl.keys).apply { keysWrapped = this }

    private var valuesWrapped: ImmutableCollection<V>? = null
    override val values: ImmutableCollection<V> get() = valuesWrapped ?: ImmutableCollectionWrapper(impl.values).apply { valuesWrapped = this }

    private var entriesWrapped: ImmutableSet<Map.Entry<K, V>>? = null
    override val entries: ImmutableSet<Map.Entry<K, V>> get() = entriesWrapped ?: ImmutableSetWrapper(impl.entries).apply { entriesWrapped = this }

    override fun with(key: K, value: @UnsafeVariance V): ImmutableMap<K, V> = wrap(impl.plus(key, value))
    override fun withAll(m: Map<out K, @UnsafeVariance V>): ImmutableMap<K, V> = wrap(impl.plusAll(m))
    override fun without(key: K): ImmutableMap<K, V> = wrap(impl.minus(key))

    override abstract fun builder(): Builder<K, @UnsafeVariance V>

    protected abstract fun wrap(impl: PMap<K, @UnsafeVariance V>): AbstractImmutableMap<K, V>


    abstract class Builder<K, V> protected constructor(protected var value: AbstractImmutableMap<K, V>, protected var impl: PMap<K, V>) : ImmutableMap.Builder<K, V>, AbstractMap<K, V>() {
        override fun build(): ImmutableMap<K, V> = value.wrap(impl).apply { value = this }

        override val size: Int get() = impl.size
        override fun isEmpty(): Boolean = impl.isEmpty()
        override fun containsKey(key: K): Boolean = impl.containsKey(key)
        override fun containsValue(value: @UnsafeVariance V): Boolean = impl.containsValue(value)

        override fun get(key: K): V? = impl.get(key)

        override fun equals(other: Any?): Boolean = impl.equals(other)
        override fun hashCode(): Int = impl.hashCode()
        override fun toString(): String = impl.toString()

        private var entrySet: MutableSet<MutableMap.MutableEntry<K, V>>? = null
        override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
            get() = entrySet ?: object : MutableSet<MutableMap.MutableEntry<K, V>>, AbstractSet<MutableMap.MutableEntry<K, V>>() {
                override val size: Int get() = impl.size
                override fun isEmpty(): Boolean = impl.isEmpty()
                override fun clear() = this@Builder.clear()
                override fun contains(element: MutableMap.MutableEntry<K, V>): Boolean {
                    val (key, value) = element
                    val candidate = impl.get(key)
                    return if (candidate != null)
                        candidate == value
                    else
                        impl.contains(key) && value == null
                }

                override fun remove(element: MutableMap.MutableEntry<K, V>): Boolean {
                    if (contains(element)) {
                        this@Builder.remove(element.key)
                        return true
                    } else {
                        return false
                    }
                }

                override fun iterator() = object : MutableIterator<MutableMap.MutableEntry<K, V>> {
                    var snapshot = impl
                    val iterator = impl.entries.iterator()
                    var entry: MutableMap.MutableEntry<K,V>? = null

                    override fun hasNext(): Boolean = iterator.hasNext()
                    override fun next(): MutableMap.MutableEntry<K, V> {
                        checkForComodification()
                        val entry = iterator.next()
                        this.entry = entry
                        return object : MutableMap.MutableEntry<K, V> by entry {
                            override fun setValue(newValue: V): V {
                                checkForComodification()
                                val oldValue = put(entry.key, newValue) as V
                                snapshot = impl
                                return oldValue
                            }
                        }
                    }

                    override fun remove() {
                        checkNotNull(entry)
                        checkForComodification()
                        this@Builder.remove(entry!!.key)
                        entry = null
                        snapshot = impl
                    }

                    protected fun checkForComodification() {
                        if (snapshot !== impl) throw ConcurrentModificationException()
                    }
                }
            }.apply { entrySet = this }

        // by AbstractMap
//        override val keys: MutableSet<K>
//        override val values: MutableCollection<V>

        override val values: MutableCollection<V>
            get() = super.values
        override val keys: MutableSet<K>
            get() = super.keys

        override abstract fun clear()

        override fun put(key: K, value: V): V?
                = get(key).apply { mutate { it.plus(key, value) } }

        override fun putAll(from: Map<out K, V>) {
            mutate { it.plusAll(from) }
        }


        override fun remove(key: K): V?
                = get(key).apply { mutate { it.minus(key) } }

        protected inline fun mutate(operation: (PMap<K, V>) -> PMap<K, V>): Boolean {
            val newValue = operation(impl)
            if (newValue !== impl) {
                impl = newValue
                return true
            }
            return false
        }

    }
}



private open class ImmutableCollectionWrapper<E>(protected val impl: Collection<E>) : ImmutableCollection<E> {
    override val size: Int get() = impl.size
    override fun isEmpty(): Boolean = impl.isEmpty()
    override fun contains(element: @UnsafeVariance E): Boolean = impl.contains(element)
    override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean = impl.containsAll(elements)
    override fun iterator(): Iterator<E> = impl.iterator()

    override fun equals(other: Any?): Boolean = impl.equals(other)
    override fun hashCode(): Int = impl.hashCode()
    override fun toString(): String = impl.toString()

    override fun builder(): ImmutableCollection.Builder<E> = ImmutableVectorList.emptyOf<E>().builder().apply { addAll(impl) }

    override fun with(element: E): ImmutableCollection<E> = builder().apply { add(element) }.build()
    override fun withAll(elements: Collection<E>): ImmutableCollection<E> = builder().apply { addAll(elements) }.build()
    override fun without(element: E): ImmutableCollection<E> = builder().apply { remove(element) }.build()
    override fun withoutAll(elements: Collection<E>): ImmutableCollection<E> = builder().apply { removeAll(elements) }.build()
    override fun withoutAll(predicate: (E) -> Boolean): ImmutableCollection<E> = builder().apply { removeAll(predicate) }.build()

}

private class ImmutableSetWrapper<E>(impl: Set<E>) : ImmutableSet<E>, ImmutableCollectionWrapper<E>(impl) {

    override fun builder(): ImmutableSet.Builder<E> = ImmutableOrderedSet.emptyOf<E>().builder().apply { addAll(impl) }

    override fun with(element: E): ImmutableSet<E> = super.with(element) as ImmutableSet
    override fun withAll(elements: Collection<E>): ImmutableSet<E> = super.withAll(elements) as ImmutableSet
    override fun without(element: E): ImmutableSet<E> = super.without(element) as ImmutableSet
    override fun withoutAll(elements: Collection<E>): ImmutableSet<E> = super.withoutAll(elements) as ImmutableSet
    override fun withoutAll(predicate: (E) -> Boolean): ImmutableSet<E> = super.withoutAll(predicate) as ImmutableSet
}
