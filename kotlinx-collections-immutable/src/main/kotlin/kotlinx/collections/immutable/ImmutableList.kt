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

public interface ImmutableList<out E>: List<E>, ImmutableCollection<E> {
    override fun add(element: @UnsafeVariance E): ImmutableList<E>

    override fun addAll(elements: Collection<@UnsafeVariance E>): ImmutableList<E> // = super<ImmutableCollection>.addAll(elements) as ImmutableList

    override fun remove(element: @UnsafeVariance E): ImmutableList<E>

    override fun removeAll(elements: Collection<@UnsafeVariance E>): ImmutableList<E>

    override fun removeAll(predicate: (E) -> Boolean): ImmutableList<E>

    override fun clear(): ImmutableList<E>


    fun addAll(index: Int, c: Collection<@UnsafeVariance E>): ImmutableList<E> // = builder().apply { addAll(index, c.toList()) }.build()

    fun set(index: Int, element: @UnsafeVariance E): ImmutableList<E>

    /**
     * Inserts an element into the list at the specified [index].
     */
    fun add(index: Int, element: @UnsafeVariance E): ImmutableList<E>

    fun removeAt(index: Int): ImmutableList<E>


    override fun subList(fromIndex: Int, toIndex: Int): ImmutableList<E>

    interface Builder<E>: MutableList<E>, ImmutableCollection.Builder<E> {
        override fun build(): ImmutableList<E>
    }

    override fun builder(): Builder<@UnsafeVariance E>
}