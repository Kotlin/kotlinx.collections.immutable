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

package kotlinx.collections.immutable

import org.pcollections.HashTreePMap
import org.pcollections.PMap
import java.util.ConcurrentModificationException


public class ImmutableOrderedMap<K, out V> private constructor(private val impl: PMap<K, LinkedEntry<K, V>>) : AbstractMap<K, V>(), PersistentMap<K, V> {
    // TODO: Keep reference to first/last entry

    class LinkedEntry<out K, out V>(val key: K, val value: @UnsafeVariance V, val prevKey: Any?, val nextKey: Any?) {
        // has referential equality/hashCode

        fun copy(value: @UnsafeVariance V = this.value, prevKey: Any? = this.prevKey, nextKey: Any? = this.nextKey) =
            LinkedEntry(key, value, prevKey, nextKey)

        // had to separate map entry implementations because of conflicting equality/hashCode
        inner class MapEntry : AbstractImmutableMap.AbstractImmutableEntry<K, V>() {
            override val key: K   get() = this@LinkedEntry.key
            override val value: V get() = this@LinkedEntry.value
        }
        val mapEntry = MapEntry()
    }

    override val size: Int get() = impl.size
    override fun isEmpty(): Boolean = impl.isEmpty()
    override fun containsKey(key: K): Boolean = impl.containsKey(key)
    override fun containsValue(value: @UnsafeVariance V): Boolean = impl.values.any { it.value == value }

    override fun get(key: K): V? = impl[key]?.value


    // should it be immutable set/collection or just read-only?
    private var _keys: ImmutableSet<K>? = null
    final override val keys: ImmutableSet<K> get() = _keys ?: createKeys().apply { _keys = this }
    private fun createKeys(): ImmutableSet<K> = OrderedKeySet()

    private var _values: ImmutableCollection<V>? = null
    final override val values: ImmutableCollection<V> get() = _values ?: createValues().apply { _values = this }
    private fun createValues(): ImmutableCollection<V> = OrderedValueCollection()

    private var _entries: ImmutableSet<Map.Entry<K, V>>? = null
    final override val entries: ImmutableSet<Map.Entry<K, V>> get() = _entries ?: createEntries().apply { _entries = this }
    private fun createEntries(): ImmutableSet<Map.Entry<K, V>> = OrderedEntrySet()

    // TODO: compiler bug: this bridge should be generated automatically
    @PublishedApi
    internal fun getEntries(): Set<Map.Entry<K, V>> = _entries ?: createEntries().apply { _entries = this }

    override fun put(key: K, value: @UnsafeVariance V): PersistentMap<K, V> = wrap(impl.putEntry(impl[key], key, value))
    override fun putAll(m: Map<out K, @UnsafeVariance V>): PersistentMap<K, V> {
        var newImpl = impl
        for ((k, v) in m)
            newImpl = newImpl.putEntry(newImpl[k], k, v)

        return wrap(newImpl)
    }
    override fun remove(key: K): PersistentMap<K, V> = wrap(impl.removeLinked(key))

    override fun remove(key: K, value: @UnsafeVariance V): PersistentMap<K, V>
            = if (!impl.contains(key, value)) this else remove(key)

    override fun clear(): PersistentMap<K, V> = emptyOf()

    override fun builder(): PersistentMap.Builder<K, @UnsafeVariance V> = Builder(this, impl)

    protected fun wrap(impl: PMap<K, LinkedEntry<K, @UnsafeVariance V>>): ImmutableOrderedMap<K, V> {
        return if (impl === this.impl) this else ImmutableOrderedMap(impl)
    }


    class Builder<K, V>(protected var value: ImmutableOrderedMap<K, V>, protected var impl: PMap<K, LinkedEntry<K, V>>) : PersistentMap.Builder<K, V>, AbstractMutableMap<K, V>() {
        override fun build(): PersistentMap<K, V> = value.wrap(impl).apply { value = this }

        override val size: Int get() = impl.size
        override fun isEmpty(): Boolean = impl.isEmpty()
        override fun containsKey(key: K): Boolean = impl.containsKey(key)
        override fun containsValue(value: @UnsafeVariance V): Boolean = impl.values.any { it.value == value }

        override fun get(key: K): V? = impl.get(key)?.value

        private var entrySet: MutableSet<MutableMap.MutableEntry<K, V>>? = null
        override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
            get() = entrySet ?: object : AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {
                override fun add(element: MutableMap.MutableEntry<K, V>): Boolean = throw UnsupportedOperationException()

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
                    private var nextEntry: LinkedEntry<K, V>? = impl.firstEntry()
                    private var currentEntry: LinkedEntry<K, V>? = null

                    override fun hasNext(): Boolean = nextEntry != null
                    override fun next(): MutableMap.MutableEntry<K, V> {
                        checkForComodification()
                        val entry = this.nextEntry ?: throw NoSuchElementException()
                        this.nextEntry = snapshot[entry.nextKey]
                        this.currentEntry = entry
                        return object : MutableMap.MutableEntry<K, V>, Map.Entry<K, V> by entry.mapEntry {
                            override fun setValue(newValue: V): V {
                                checkForComodification()
                                val oldValue = put(entry.key, newValue) as V
                                snapshot = impl
                                return oldValue
                            }
                        }
                    }

                    override fun remove() {
                        val currentEntry = checkNotNull(currentEntry)
                        checkForComodification()
                        this@Builder.remove(currentEntry.key)
                        this.currentEntry = null
                        snapshot = impl
                    }

                    protected fun checkForComodification() {
                        if (snapshot !== impl) throw ConcurrentModificationException()
                    }
                }
            }.apply { entrySet = this }


        override fun clear() {
            mutate(HashTreePMap.empty())
        }

        override fun put(key: K, value: V): V?  {
            val entry = impl[key]

            mutate(impl.putEntry(entry, key, value))

            return entry?.value
        }

        override fun putAll(from: Map<out K, V>) {
            for ((k, v) in from) put(k, v)
        }


        override fun remove(key: K): V? {
            val entry = impl[key] ?: return null
            mutate(impl.removeEntry(entry))
            return entry.value
        }

        protected inline fun mutate(newValue: PMap<K, LinkedEntry<K, V>>): Boolean {
            if (newValue !== impl) {
                impl = newValue
                return true
            }
            return false
        }

    }

    companion object {
        private val EMPTY = ImmutableOrderedMap(HashTreePMap.empty<Any?, LinkedEntry<Any?, Nothing>>())

        private val TERMINATOR = Any()

        private fun <K, V> PMap<K, LinkedEntry<K, V>>.contains(key: K, value: @UnsafeVariance V): Boolean
                = this[key]?.let { entry -> entry.value == value } ?: false

        private fun <K, V> PMap<K, LinkedEntry<K, V>>.removeLinked(key: K): PMap<K, LinkedEntry<K, V>> {
            val entry = this[key] ?: return this
            return removeEntry(entry)
        }

        private fun <K, V> PMap<K, LinkedEntry<K, V>>.removeEntry(entry: LinkedEntry<K, V>): PMap<K, LinkedEntry<K, V>> {
            val prevKey = entry.prevKey
            val nextKey = entry.nextKey
            var new = this.minus(entry.key)

            if (prevKey !== TERMINATOR) {
                @Suppress("UNCHECKED_CAST")
                val prevEntry = this[prevKey as K]!!
                val newPrevEntry = prevEntry.copy(nextKey = nextKey)
                new = new.plus(newPrevEntry.key, newPrevEntry)
            }
            if (nextKey !== TERMINATOR) {
                @Suppress("UNCHECKED_CAST")
                val nextEntry = this[nextKey as K]!!
                val newNextEntry = nextEntry.copy(prevKey = prevKey)
                new = new.plus(newNextEntry.key, newNextEntry)
            }
            return new
        }

        private fun <K, V> PMap<K, LinkedEntry<K, V>>.putEntry(entry: LinkedEntry<K, V>?, key: K, value: V): PMap<K, LinkedEntry<K, V>> {
            if (entry != null) {
                return if (entry.value == value) this else this.plus(key, entry.copy(value = value))
            }
            val lastEntry = this.lastEntry()
            if (lastEntry == null) {
                val newEntry = LinkedEntry(key, value, TERMINATOR, TERMINATOR)
                return this.plus(key, newEntry)
            } else {
                val newEntry = LinkedEntry(key, value, lastEntry.key, TERMINATOR)
                val newLastEntry = lastEntry.copy(nextKey = key)
                return this.plus(lastEntry.key, newLastEntry).plus(key, newEntry)
            }
        }

        private fun <K, V> PMap<K, LinkedEntry<K, V>>.firstEntry() = this.values.firstOrNull { it.prevKey === TERMINATOR }
        private fun <K, V> PMap<K, LinkedEntry<K, V>>.lastEntry() = this.values.firstOrNull { it.nextKey === TERMINATOR }


        @Suppress("UNCHECKED_CAST")
        fun <K, V> emptyOf(): ImmutableOrderedMap<K, V> = EMPTY as ImmutableOrderedMap<K, V>
    }

    private val entrySequence = generateSequence(impl.firstEntry()) { e -> impl[e.nextKey] }

    private inner class OrderedEntrySet : AbstractSet<Map.Entry<K, V>>(), ImmutableSet<Map.Entry<K, V>> {
        override val size: Int get() = impl.size
        override fun contains(element: Map.Entry<K, @UnsafeVariance V>): Boolean = impl.contains(element.key, element.value)
        override fun containsAll(elements: Collection<Map.Entry<K, @UnsafeVariance V>>): Boolean = elements.all { (k, v) -> impl.contains(k, v) }
        override fun isEmpty(): Boolean = impl.isEmpty()
        private val mapped = entrySequence.map { it.mapEntry }
        override fun iterator(): Iterator<Map.Entry<K, V>> = mapped.iterator()
    }

    private inner class OrderedKeySet : AbstractSet<K>(), ImmutableSet<K> {
        override val size: Int get() = impl.size
        override fun contains(element: K): Boolean = impl.containsKey(element)
        override fun containsAll(elements: Collection<K>): Boolean = impl.keys.containsAll(elements)

        override fun isEmpty(): Boolean = impl.isEmpty()

        private val mapped = entrySequence.map { it.key }
        override fun iterator(): Iterator<K> = mapped.iterator()
    }

    private inner class OrderedValueCollection : AbstractCollection<V>(), ImmutableCollection<V> {
        override val size: Int get() = impl.size
        override fun contains(element: @UnsafeVariance V): Boolean = containsValue(element)
        override fun containsAll(elements: Collection<@UnsafeVariance V>): Boolean =  elements.all { v -> containsValue(v) }
        override fun isEmpty(): Boolean = impl.isEmpty()

        private val mapped = entrySequence.map { it.value }
        override fun iterator(): Iterator<V> = mapped.iterator()

    }
}


