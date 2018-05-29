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

public interface ImmutableSet<out E>: Set<E>, ImmutableCollection<E> {


    interface Builder<E>: MutableSet<E>, ImmutableCollection.Builder<E> {
        override fun build(): ImmutableSet<E>
    }

    override fun builder(): Builder<@UnsafeVariance E>
}

public interface PersistentSet<out E> : ImmutableSet<E>, PersistentCollection<E> {
    override fun add(element: @UnsafeVariance E): PersistentSet<E>

    override fun addAll(elements: Collection<@UnsafeVariance E>): PersistentSet<E>

    override fun remove(element: @UnsafeVariance E): PersistentSet<E>

    override fun removeAll(elements: Collection<@UnsafeVariance E>): PersistentSet<E>

    override fun removeAll(predicate: (E) -> Boolean): PersistentSet<E>

    override fun clear(): PersistentSet<E>


    interface Builder<E>: ImmutableSet.Builder<E>, PersistentCollection.Builder<E> {
        override fun build(): PersistentSet<E>
    }

    override fun builder(): Builder<@UnsafeVariance E>
}