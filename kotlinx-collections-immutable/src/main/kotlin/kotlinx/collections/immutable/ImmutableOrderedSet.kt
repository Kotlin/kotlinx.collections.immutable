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

import org.pcollections.OrderedPSet
import org.pcollections.POrderedSet
import org.pcollections.PSet

public class ImmutableOrderedSet<out E> private constructor(impl: POrderedSet<E>) : AbstractImmutableSet<E>(impl) {
    override fun wrap(impl: PSet<@UnsafeVariance E>): ImmutableOrderedSet<E>
            = if (impl === this.impl) this else ImmutableOrderedSet(impl as POrderedSet)

    override fun clear(): AbstractImmutableSet<E> = EMPTY

    override fun builder(): Builder<@UnsafeVariance E> = Builder(this, impl)

    class Builder<E> internal constructor(value: AbstractImmutableSet<E>, impl: PSet<E>) : AbstractImmutableSet.Builder<E>(value, impl) {
        override fun clear(): Unit { mutate { OrderedPSet.empty() } }
    }

    companion object {
        private val EMPTY = ImmutableOrderedSet(OrderedPSet.empty<Nothing>())
        fun <E> emptyOf(): ImmutableOrderedSet<E> = EMPTY
    }
}