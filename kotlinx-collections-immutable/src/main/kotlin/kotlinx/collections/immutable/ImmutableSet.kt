package kotlinx.collections.immutable

public interface ImmutableSet<out E>: Set<E>, ImmutableCollection<E> {
    override fun add(element: @UnsafeVariance E): ImmutableSet<E>

    override fun addAll(elements: Collection<@UnsafeVariance E>): ImmutableSet<E>

    override fun remove(element: @UnsafeVariance E): ImmutableSet<E>

    override fun removeAll(elements: Collection<@UnsafeVariance E>): ImmutableSet<E>

    override fun removeAll(predicate: (E) -> Boolean): ImmutableSet<E>

    override fun clear(): ImmutableSet<E>

    interface Builder<E>: MutableSet<E>, ImmutableCollection.Builder<E> {
        override fun build(): ImmutableSet<E>
    }

    override fun builder(): Builder<@UnsafeVariance E>
}