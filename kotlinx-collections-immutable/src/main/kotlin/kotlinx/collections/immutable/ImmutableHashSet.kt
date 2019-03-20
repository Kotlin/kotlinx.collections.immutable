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

import org.pcollections.HashTreePSet
import org.pcollections.PSet

internal class ImmutableHashSet<out E> private constructor(impl: PSet<E>) : AbstractImmutableSet<E>(impl) {
    override fun wrap(impl: PSet<@UnsafeVariance E>): ImmutableHashSet<E>
        = if (impl === this.impl) this else ImmutableHashSet(impl)

    override fun clear(): AbstractImmutableSet<E> = EMPTY

    override fun builder(): Builder<@UnsafeVariance E> = Builder(this, impl)

    class Builder<E> internal constructor(value: AbstractImmutableSet<E>, impl: PSet<E>) : AbstractImmutableSet.Builder<E>(value, impl) {
        override fun clear(): Unit { mutate { HashTreePSet.empty() } }
    }

    companion object {
        private val EMPTY = ImmutableHashSet(HashTreePSet.empty<Nothing>())
        fun <E> emptyOf(): ImmutableHashSet<E> = EMPTY
    }

}