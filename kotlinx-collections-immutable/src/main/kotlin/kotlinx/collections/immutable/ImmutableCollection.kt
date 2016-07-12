package kotlinx.collections.immutable

public interface ImmutableCollection<out E>: Collection<E> {
    fun add(element: @UnsafeVariance E): ImmutableCollection<E>

    fun addAll(elements: Collection<@UnsafeVariance E>): ImmutableCollection<E>

    fun remove(element: @UnsafeVariance E): ImmutableCollection<E>

    fun removeAll(elements: Collection<@UnsafeVariance E>): ImmutableCollection<E>

    fun removeAll(predicate: (E) -> Boolean): ImmutableCollection<E>

    fun clear(): ImmutableCollection<E>

    interface Builder<E>: MutableCollection<E> {
        fun build(): ImmutableCollection<E>
    }

    fun builder(): Builder<@UnsafeVariance E>
}