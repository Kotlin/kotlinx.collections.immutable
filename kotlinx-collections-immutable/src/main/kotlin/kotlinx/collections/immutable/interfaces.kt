package kotlinx.collections.immutable


public interface ImmutableCollection<out E>: Collection<E> {
    fun added(element: @UnsafeVariance E): ImmutableCollection<E>

    fun addedAll(elements: Collection<@UnsafeVariance E>): ImmutableCollection<E>

    fun removed(element: @UnsafeVariance E): ImmutableCollection<E>

    fun removedAll(elements: Collection<@UnsafeVariance E>): ImmutableCollection<E>

    fun removedAll(predicate: (E) -> Boolean): ImmutableCollection<E>

//    fun retainAll(c: Collection<@UnsafeVariance E>): ImmutableCollection<E>
//
    fun cleared(): ImmutableCollection<E>

    interface Builder<E>: MutableCollection<E> {
        fun build(): ImmutableCollection<E>
    }

    fun builder(): Builder<@UnsafeVariance E>


}




public interface ImmutableSet<out E>: Set<E>, ImmutableCollection<E> {
    override fun added(element: @UnsafeVariance E): ImmutableSet<E>

    override fun addedAll(elements: Collection<@UnsafeVariance E>): ImmutableSet<E>

    override fun removed(element: @UnsafeVariance E): ImmutableSet<E>

    override fun removedAll(elements: Collection<@UnsafeVariance E>): ImmutableSet<E>

    override fun removedAll(predicate: (E) -> Boolean): ImmutableSet<E>

    //    override fun retainAll(c: Collection<@UnsafeVariance E>): ImmutableCollection<E>
    //
    override fun cleared(): ImmutableSet<E>

    interface Builder<E>: MutableSet<E>, ImmutableCollection.Builder<E> {
        override fun build(): ImmutableSet<E>
    }

    override fun builder(): Builder<@UnsafeVariance E>
}



public interface ImmutableList<out E>: List<E>, ImmutableCollection<E> {
    override fun added(element: @UnsafeVariance E): ImmutableList<E>

    override fun addedAll(elements: Collection<@UnsafeVariance E>): ImmutableList<E> // = super<ImmutableCollection>.addAll(elements) as ImmutableList

    override fun removed(element: @UnsafeVariance E): ImmutableList<E>

    override fun removedAll(elements: Collection<@UnsafeVariance E>): ImmutableList<E>

    override fun removedAll(predicate: (E) -> Boolean): ImmutableList<E>

//    override fun retainAll(c: Collection<@UnsafeVariance E>): ImmutableList<E>
//
    override fun cleared(): ImmutableList<E>


    fun addedAll(index: Int, c: Collection<@UnsafeVariance E>): ImmutableList<E> // = builder().apply { addAll(index, c.toList()) }.build()

    fun setAt(index: Int, element: @UnsafeVariance E): ImmutableList<E>

    /**
     * Inserts an element into the list at the specified [index].
     */
    public fun added(index: Int, element: @UnsafeVariance E): ImmutableList<E>

    public fun removedAt(index: Int): ImmutableList<E>


    override fun subList(fromIndex: Int, toIndex: Int): ImmutableList<E>

    interface Builder<E>: MutableList<E>, ImmutableCollection.Builder<E> {
        override fun build(): ImmutableList<E>
    }

    override fun builder(): Builder<@UnsafeVariance E>
}



public interface ImmutableMap<K, out V>: Map<K, V> {

    override val keys: ImmutableSet<K>

    override val values: ImmutableCollection<V>

    override val entries: ImmutableSet<Map.Entry<K, V>>

    fun added(key: K, value: @UnsafeVariance V): ImmutableMap<K, V>

    public fun removed(key: K): ImmutableMap<K, V>

    public fun addedAll(m: Map<out K, @UnsafeVariance V>): ImmutableMap<K, V>  // m: Iterable<Map.Entry<K, V>> or Map<out K,V> or Iterable<Pair<K, V>>

    public fun cleared(): ImmutableMap<K, V>

    interface Builder<K, V>: MutableMap<K, V> {
        fun build(): ImmutableMap<K, V>
    }

    fun builder(): Builder<K, @UnsafeVariance V>
}


