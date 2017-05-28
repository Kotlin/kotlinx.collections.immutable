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

import org.pcollections.PMap
import java.util.ConcurrentModificationException

internal abstract class AbstractImmutableMap<K, out V> protected constructor(protected val impl: PMap<K, @UnsafeVariance V>) : ImmutableMap<K, V> {

    abstract class AbstractImmutableEntry<out K, out V> : Map.Entry<K, V> {
        override fun equals(other: Any?): Boolean = other is Map.Entry<*,*> && other.key == key && other.value == value
        override fun hashCode(): Int = (key?.hashCode() ?: 0) xor (value?.hashCode() ?: 0)
        override fun toString(): String = "$key=$value"
    }

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
    protected open fun createKeys(): Set<K> = impl.keys

    private var _values: Collection<V>? = null
    final override val values: Collection<V> get() = _values ?: createValues().apply { _values = this }
    protected open fun createValues(): Collection<V> = impl.values

    private var _entries: Set<Map.Entry<K, V>>? = null
    final override val entries: Set<Map.Entry<K, V>> get() = _entries ?: createEntries().apply { _entries = this }
    protected open fun createEntries(): Set<Map.Entry<K, V>> = impl.entries

    override fun put(key: K, value: @UnsafeVariance V): ImmutableMap<K, V> = wrap(impl.plus(key, value))
    override fun putAll(m: Map<out K, @UnsafeVariance V>): ImmutableMap<K, V> = wrap(impl.plusAll(m))
    override fun remove(key: K): ImmutableMap<K, V> = wrap(impl.minus(key))
    override fun remove(key: K, value: @UnsafeVariance V): ImmutableMap<K, V>
            = if (!impl.contains(key, value)) this else wrap(impl.minus(key))

    override abstract fun builder(): Builder<K, @UnsafeVariance V>

    protected abstract fun wrap(impl: PMap<K, @UnsafeVariance V>): AbstractImmutableMap<K, V>


    abstract class Builder<K, V> protected constructor(protected var value: AbstractImmutableMap<K, V>, protected var impl: PMap<K, V>) : ImmutableMap.Builder<K, V>, AbstractMutableMap<K, V>() {
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
                    @JvmField
                    protected var snapshot = impl
                    private val iterator = impl.entries.iterator()
                    private var entry: MutableMap.MutableEntry<K,V>? = null

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

internal fun <K, V> PMap<K, V>.contains(key: K, value: @UnsafeVariance V): Boolean
    = this[key]?.let { candidate -> candidate == value } ?: (value == null && containsKey(key))


/*
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

    override fun add(element: E): ImmutableCollection<E> = builder().apply { add(element) }.build()
    override fun addAll(elements: Collection<E>): ImmutableCollection<E> = builder().apply { addAll(elements) }.build()
    override fun remove(element: E): ImmutableCollection<E> = builder().apply { remove(element) }.build()
    override fun removeAll(elements: Collection<E>): ImmutableCollection<E> = builder().apply { removeAll(elements) }.build()
    override fun removeAll(predicate: (E) -> Boolean): ImmutableCollection<E> = builder().apply { removeAll(predicate) }.build()
    override fun clear(): ImmutableCollection<E> = immutableListOf()
}

private class ImmutableSetWrapper<E>(impl: Set<E>) : ImmutableSet<E>, ImmutableCollectionWrapper<E>(impl) {

    override fun builder(): ImmutableSet.Builder<E> = ImmutableOrderedSet.emptyOf<E>().builder().apply { addAll(impl) }

    override fun add(element: E): ImmutableSet<E> = super.add(element) as ImmutableSet
    override fun addAll(elements: Collection<E>): ImmutableSet<E> = super.addAll(elements) as ImmutableSet
    override fun remove(element: E): ImmutableSet<E> = super.remove(element) as ImmutableSet
    override fun removeAll(elements: Collection<E>): ImmutableSet<E> = super.removeAll(elements) as ImmutableSet
    override fun removeAll(predicate: (E) -> Boolean): ImmutableSet<E> = super.removeAll(predicate) as ImmutableSet
    override fun clear(): ImmutableSet<E> = immutableSetOf()
}
*/
