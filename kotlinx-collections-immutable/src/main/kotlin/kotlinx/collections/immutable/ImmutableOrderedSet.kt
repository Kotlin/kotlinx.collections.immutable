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