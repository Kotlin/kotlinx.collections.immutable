package kotlinx.collections.immutable

import org.pcollections.HashTreePMap
import org.pcollections.PMap

internal class ImmutableHashMap<K, out V> private constructor(impl: PMap<K, V>) : AbstractImmutableMap<K, V>(impl) {
    override fun wrap(impl: PMap<K, @UnsafeVariance V>): ImmutableHashMap<K, V>
            = if (this.impl === impl) this else ImmutableHashMap(impl)

    override fun clear(): ImmutableMap<K, V> = emptyOf()

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