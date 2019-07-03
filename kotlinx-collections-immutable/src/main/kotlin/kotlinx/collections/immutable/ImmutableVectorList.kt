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

import org.pcollections.*

internal class ImmutableVectorList<out E> private constructor(private val impl: PVector<E>) : PersistentList<E> {

    // delegating to impl
    override val size: Int get() = impl.size
    override fun contains(element: @UnsafeVariance E): Boolean = impl.contains(element)
    override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean = impl.containsAll(elements)
    override fun get(index: Int): E = impl.get(index)
    override fun indexOf(element: @UnsafeVariance E): Int = impl.indexOf(element)
    override fun isEmpty(): Boolean = impl.isEmpty()
    override fun iterator(): Iterator<E> = impl.iterator()
    override fun lastIndexOf(element: @UnsafeVariance E): Int = impl.lastIndexOf(element)
    override fun listIterator(): ListIterator<E> = impl.listIterator()
    override fun listIterator(index: Int): ListIterator<E> = impl.listIterator(index)
    override fun equals(other: Any?): Boolean = impl.equals(other)
    override fun hashCode(): Int = impl.hashCode()
    override fun toString(): String = impl.toString()



    override fun add(element: @UnsafeVariance E): PersistentList<E> = wrap(impl.plus(element))

    override fun add(index: Int, element: @UnsafeVariance E): PersistentList<E> = wrap(impl.plus(index, element))

    override fun addAll(elements: Collection<@UnsafeVariance E>): PersistentList<E> = wrap(impl.plusAll(elements))

    override fun addAll(index: Int, c: Collection<@UnsafeVariance E>): PersistentList<E> = wrap(impl.plusAll(index, c))

    override fun remove(element: @UnsafeVariance E): PersistentList<E> = wrap(impl.minus(element))

    // not minusAll
    override fun removeAll(elements: Collection<@UnsafeVariance E>): PersistentList<E> = mutate { it.removeAll(elements) }

    override fun removeAll(predicate: (E) -> Boolean): PersistentList<E> = mutate { it.removeAll(predicate) }

    //    override fun retainAll(c: Collection<@UnsafeVariance E>): PersistentList<E> = builder().apply { retainAll(c) }.build()

    override fun set(index: Int, element: @UnsafeVariance E): PersistentList<E> = wrap(impl.with(index, element))

    override fun removeAt(index: Int): PersistentList<E> = wrap(impl.minus(index))

    override fun clear(): PersistentList<E> = EMPTY

    override fun builder(): Builder<@UnsafeVariance E> = Builder(this, impl)


    protected fun wrap(impl: PVector<@UnsafeVariance E>): ImmutableVectorList<E>
            = if (impl === this.impl) this else ImmutableVectorList(impl)

    companion object {
        private val EMPTY = ImmutableVectorList(TreePVector.empty<Nothing>())

        public fun <T> emptyOf(): ImmutableVectorList<T> = EMPTY
    }

    class Builder<E> internal constructor(private var value: ImmutableVectorList<E>, private var impl: PVector<E>) : AbstractMutableList<E>(), PersistentList.Builder<E> {
        override fun build(): ImmutableVectorList<E> = value.wrap(impl).apply { value = this }
        // delegating to impl
        override val size: Int get() = impl.size
        override fun get(index: Int): E = impl.get(index)

        override fun contains(element: @UnsafeVariance E): Boolean = impl.contains(element)
        override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean = impl.containsAll(elements)
        override fun indexOf(element: @UnsafeVariance E): Int = impl.indexOf(element)
        override fun lastIndexOf(element: @UnsafeVariance E): Int = impl.lastIndexOf(element)

        override fun equals(other: Any?): Boolean = impl.equals(other)
        override fun hashCode(): Int = impl.hashCode()
        override fun toString(): String = impl.toString()

        private inline fun mutate(operation: (PVector<E>) -> PVector<E>): Boolean {
            val newValue = operation(impl)
            if (newValue !== impl) {
                if (newValue.size != impl.size)
                    modCount++
                impl = newValue
                return true
            }
            return false
        }

        override fun add(element: E): Boolean = mutate { it.plus(element) }

        override fun add(index: Int, element: E) { mutate { it.plus(index, element) } }

        override fun addAll(index: Int, elements: Collection<E>): Boolean = mutate { it.plusAll(index, elements) }

        override fun addAll(elements: Collection<E>): Boolean = mutate { it.plusAll(elements) }

        override fun clear() { mutate { TreePVector.empty() } }

        override fun remove(element: E): Boolean = mutate { it.minus(element) }

        override fun removeAt(index: Int): E = get(index).apply { mutate { it.minus(index) }}

        // by AbstractList
        //override fun removeAll(elements: Collection<E>): Boolean = mutate { it.minusAll(elements) }

        //override fun retainAll(elements: Collection<E>): Boolean

        override fun set(index: Int, element: E): E = get(index).apply { mutate { it.with(index, element) } }

        // to make subList a view of this builder, rather than builder of the view
        //override fun subList(fromIndex: Int, toIndex: Int): Builder<E> = Builder(value, impl.subList(fromIndex, toIndex))
    }

}





