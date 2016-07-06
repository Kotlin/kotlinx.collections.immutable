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