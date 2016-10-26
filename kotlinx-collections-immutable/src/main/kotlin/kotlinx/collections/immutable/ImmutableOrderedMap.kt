package kotlinx.collections.immutable

import org.pcollections.HashTreePMap
import org.pcollections.PMap
import org.pcollections.PVector
import org.pcollections.TreePVector
import java.util.*


internal class ImmutableOrderedMap<K, out V> private constructor(private val impl: PMap<K, V>, private val order: PVector<Map.Entry<K ,V>>) : ImmutableMap<K, V> {

    override val size: Int get() = impl.size
    override fun isEmpty(): Boolean = impl.isEmpty()
    override fun containsKey(key: K): Boolean = impl.containsKey(key)
    override fun containsValue(value: @UnsafeVariance V): Boolean = impl.containsValue(value)

    override fun get(key: K): V? = impl.get(key)

    override fun equals(other: Any?): Boolean = impl.equals(other)
    override fun hashCode(): Int = impl.hashCode()
    override fun toString(): String = impl.toString()


    // should it be immutable set/collection or just read-only?
    private var _keys: Set<K>? = null
    final override val keys: Set<K> get() = _keys ?: createKeys().apply { _keys = this }
    private fun createKeys(): Set<K> = OrderedKeySet()

    private var _values: Collection<V>? = null
    final override val values: Collection<V> get() = _values ?: createValues().apply { _values = this }
    private fun createValues(): Collection<V> = OrderedValueCollection()

    private var _entries: Set<Map.Entry<K, V>>? = null
    final override val entries: Set<Map.Entry<K, V>> get() = _entries ?: createEntries().apply { _entries = this }
    private fun createEntries(): Set<Map.Entry<K, V>> = OrderedEntrySet()

    override fun put(key: K, value: @UnsafeVariance V): ImmutableMap<K, V> = wrap(impl.plus(key, value)) { order.addOrReplace(key, value)  }
    override fun putAll(m: Map<out K, @UnsafeVariance V>): ImmutableMap<K, V> {
        var newImpl = impl
        var newOrder = order
        for ((k, v) in m) {
            newImpl.plus(k, v).let { if (it != newImpl) {
                newImpl = it
                newOrder = newOrder.addOrReplace(k, v)
            }}
        }
        return wrap(newImpl) { newOrder }
    }
    override fun remove(key: K): ImmutableMap<K, V> = wrap(impl.minus(key), { order.minus(order.indexOfFirst { it.key == key })})
    override fun remove(key: K, value: @UnsafeVariance V): ImmutableMap<K, V>
            = if (!impl.contains(key, value)) this else remove(key)

    override fun clear(): ImmutableMap<K, V> = emptyOf()

    override fun builder(): ImmutableMap.Builder<K, @UnsafeVariance V> = Builder(this, impl, order)

    private fun entry(key: K, value: @UnsafeVariance V): Map.Entry<K, V> = AbstractMap.SimpleEntry(key, value)

    protected fun wrap(impl: PMap<K, @UnsafeVariance V>, order: (PVector<Map.Entry<K, @UnsafeVariance V>>) -> PVector<Map.Entry<K, @UnsafeVariance V>>): ImmutableOrderedMap<K, V> {
        return if (impl === this.impl) this else ImmutableOrderedMap(impl, order(this.order))
    }


    protected class Builder<K, V>(protected var value: ImmutableOrderedMap<K, V>, protected var impl: PMap<K, V>, protected var order: PVector<Map.Entry<K, V>>) : ImmutableMap.Builder<K, V>, AbstractMap<K, V>() {
        override fun build(): ImmutableMap<K, V> = value.wrap(impl, { order }).apply { value = this }

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
                override fun contains(element: MutableMap.MutableEntry<K, V>): Boolean
                        = impl.contains(element.key, element.value)

                override fun remove(element: MutableMap.MutableEntry<K, V>): Boolean {
                    if (contains(element)) {
                        this@Builder.remove(element.key)
                        return true
                    } else {
                        return false
                    }
                }

                override fun iterator() = object : MutableIterator<MutableMap.MutableEntry<K, V>> {
                    private var snapshot = impl
                    private val iterator = order.iterator()
                    private var entry: Map.Entry<K,V>? = null

                    override fun hasNext(): Boolean = iterator.hasNext()
                    override fun next(): MutableMap.MutableEntry<K, V> {
                        checkForComodification()
                        val entry = iterator.next()
                        this.entry = entry
                        return object : MutableMap.MutableEntry<K, V>, Map.Entry<K, V> by entry {
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

        override fun clear() {
            mutate(HashTreePMap.empty(), { TreePVector.empty() })
        }

        override fun put(key: K, value: V): V? =
                get(key).apply {
                    mutate(impl.plus(key, value), { order.addOrReplace(key, value) })
                }

        override fun putAll(from: Map<out K, V>) {
            for ((k, v) in from) put(k, v)
        }


        override fun remove(key: K): V?
                = get(key).apply { mutate(impl.minus(key), { it.minus(it.indexOfFirst { it.key == key })}) }

        protected inline fun mutate(newValue: PMap<K, V>, orderOperation: (PVector<Map.Entry<K, V>>) -> (PVector<Map.Entry<K, V>>) ): Boolean {
            if (newValue !== impl) {
                order = orderOperation(order)
                impl = newValue
                return true
            }
            return false
        }

    }

    companion object {
        private val EMPTY = ImmutableOrderedMap(HashTreePMap.empty<Any?, Nothing>(), TreePVector.empty())

        @Suppress("UNCHECKED_CAST")
        fun <K, V> emptyOf(): ImmutableOrderedMap<K, V> = EMPTY as ImmutableOrderedMap<K, V>
    }


    private inner class OrderedEntrySet : Set<Map.Entry<K, V>> {
        override val size: Int get() = impl.size
        override fun contains(element: Map.Entry<K, @UnsafeVariance V>): Boolean = impl.entries.contains(element)
        override fun containsAll(elements: Collection<Map.Entry<K, @UnsafeVariance V>>): Boolean = impl.entries.containsAll(elements)
        override fun isEmpty(): Boolean = impl.isEmpty()
        override fun iterator(): Iterator<Map.Entry<K, V>> = order.iterator()
    }

    private inner class OrderedKeySet : Set<K> {
        override val size: Int get() = impl.size
        override fun contains(element: K): Boolean = impl.containsKey(element)
        override fun containsAll(elements: Collection<K>): Boolean = impl.keys.containsAll(elements)

        override fun isEmpty(): Boolean = impl.isEmpty()

        private val mapped = order.asSequence().map { it.key }
        override fun iterator(): Iterator<K> = mapped.iterator()
    }

    private inner class OrderedValueCollection : Collection<V> {
        override val size: Int get() = impl.size
        override fun contains(element: @UnsafeVariance V): Boolean = impl.containsValue(element)
        override fun containsAll(elements: Collection<@UnsafeVariance V>): Boolean = impl.values.containsAll(elements)
        override fun isEmpty(): Boolean = impl.isEmpty()

        private val mapped = order.asSequence().map { it.value }
        override fun iterator(): Iterator<V> = mapped.iterator()
    }
}

private fun <K, V> PVector<Map.Entry<K, V>>.addOrReplace(key: K, value: V): PVector<Map.Entry<K, V>> {
    val index = indexOfFirst { it.key == key }
    val entry = AbstractMap.SimpleEntry(key, value)
    return if (index >= 0) with(index, entry) else plus(entry)
}

