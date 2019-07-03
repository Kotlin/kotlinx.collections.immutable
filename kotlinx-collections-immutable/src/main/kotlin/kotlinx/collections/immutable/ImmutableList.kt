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

import kotlinx.collections.immutable.internal.ListImplementation

public interface ImmutableList<out E> : List<E>, ImmutableCollection<E> {

    override fun subList(fromIndex: Int, toIndex: Int): ImmutableList<E> = SubList(this, fromIndex, toIndex)

    public class SubList<E>(private val source: ImmutableList<E>, private val fromIndex: Int, private val toIndex: Int) : ImmutableList<E>, AbstractList<E>() {
        private var _size: Int = 0

        init {
            ListImplementation.checkRangeIndexes(fromIndex, toIndex, source.size)
            this._size = toIndex - fromIndex
        }

        override fun get(index: Int): E {
            ListImplementation.checkElementIndex(index, _size)

            return source[fromIndex + index]
        }

        override val size: Int get() = _size

        override fun subList(fromIndex: Int, toIndex: Int): ImmutableList<E> {
            ListImplementation.checkRangeIndexes(fromIndex, toIndex, this._size)
            return SubList(source, this.fromIndex + fromIndex, this.fromIndex + toIndex)
        }

    }
}

public interface PersistentList<out E> : ImmutableList<E>, PersistentCollection<E> {
    override fun add(element: @UnsafeVariance E): PersistentList<E>

    override fun addAll(elements: Collection<@UnsafeVariance E>): PersistentList<E> // = super<ImmutableCollection>.addAll(elements) as ImmutableList

    override fun remove(element: @UnsafeVariance E): PersistentList<E>

    override fun removeAll(elements: Collection<@UnsafeVariance E>): PersistentList<E>

    override fun removeAll(predicate: (E) -> Boolean): PersistentList<E>

    override fun clear(): PersistentList<E>


    fun addAll(index: Int, c: Collection<@UnsafeVariance E>): PersistentList<E> // = builder().apply { addAll(index, c.toList()) }.build()

    fun set(index: Int, element: @UnsafeVariance E): PersistentList<E>

    /**
     * Inserts an element into the list at the specified [index].
     */
    fun add(index: Int, element: @UnsafeVariance E): PersistentList<E>

    fun removeAt(index: Int): PersistentList<E>

    interface Builder<E>: MutableList<E>, PersistentCollection.Builder<E> {
        override fun build(): PersistentList<E>
    }

    override fun builder(): Builder<@UnsafeVariance E>
}