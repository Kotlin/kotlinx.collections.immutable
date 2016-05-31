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


public interface ImmutableList<out E>: List<E>, ImmutableCollection<E> {
    override fun add(element: @UnsafeVariance E): ImmutableList<E>

    override fun addAll(elements: Collection<@UnsafeVariance E>): ImmutableList<E> // = super<ImmutableCollection>.addAll(elements) as ImmutableList

    override fun remove(element: @UnsafeVariance E): ImmutableList<E>

    override fun removeAll(elements: Collection<@UnsafeVariance E>): ImmutableList<E>

    override fun removeAll(predicate: (E) -> Boolean): ImmutableList<E>

    override fun clear(): ImmutableList<E>


    fun addAll(index: Int, c: Collection<@UnsafeVariance E>): ImmutableList<E> // = builder().apply { addAll(index, c.toList()) }.build()

    fun set(index: Int, element: @UnsafeVariance E): ImmutableList<E>

    /**
     * Inserts an element into the list at the specified [index].
     */
    fun add(index: Int, element: @UnsafeVariance E): ImmutableList<E>

    fun removeAt(index: Int): ImmutableList<E>


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

    fun add(key: K, value: @UnsafeVariance V): ImmutableMap<K, V>

    fun remove(key: K): ImmutableMap<K, V>

    fun remove(key: K, value: @UnsafeVariance V): ImmutableMap<K, V>

    fun addAll(m: Map<out K, @UnsafeVariance V>): ImmutableMap<K, V>  // m: Iterable<Map.Entry<K, V>> or Map<out K,V> or Iterable<Pair<K, V>>

    fun clear(): ImmutableMap<K, V>

    interface Builder<K, V>: MutableMap<K, V> {
        fun build(): ImmutableMap<K, V>
    }

    fun builder(): Builder<K, @UnsafeVariance V>
}



