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

public class ImmutableHashMap<K, out V> private constructor(impl: PMap<K, V>) : AbstractImmutableMap<K, V>(impl) {
    override fun wrap(impl: PMap<K, @UnsafeVariance V>): ImmutableHashMap<K, V>
            = if (this.impl === impl) this else ImmutableHashMap(impl)

    override fun clear(): PersistentMap<K, V> = emptyOf()

    override fun builder(): Builder<K, @UnsafeVariance V> = Builder(this, impl)

    class Builder<K, V> internal constructor(value: ImmutableHashMap<K, V>, impl: PMap<K, V>) : AbstractImmutableMap.Builder<K, V>(value, impl) {
        override fun clear() { mutate { HashTreePMap.empty() }}
    }

    companion object {
        private val EMPTY = ImmutableHashMap(HashTreePMap.empty<Any?, Nothing>())

        @Suppress("CAST_NEVER_SUCCEEDS")
        fun <K, V> emptyOf(): ImmutableHashMap<K, V> = EMPTY as ImmutableHashMap<K, V>
    }

}