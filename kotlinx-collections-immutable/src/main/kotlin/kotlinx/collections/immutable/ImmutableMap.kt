package kotlinx.collections.immutable


public interface ImmutableMap<K, out V>: Map<K, V> {

    override val keys: Set<K>

    override val values: Collection<V>

    override val entries: Set<Map.Entry<K, V>>

    fun put(key: K, value: @UnsafeVariance V): ImmutableMap<K, V>

    fun remove(key: K): ImmutableMap<K, V>

    fun remove(key: K, value: @UnsafeVariance V): ImmutableMap<K, V>

    fun putAll(m: Map<out K, @UnsafeVariance V>): ImmutableMap<K, V>  // m: Iterable<Map.Entry<K, V>> or Map<out K,V> or Iterable<Pair<K, V>>

    fun clear(): ImmutableMap<K, V>

    interface Builder<K, V>: MutableMap<K, V> {
        fun build(): ImmutableMap<K, V>
    }

    fun builder(): Builder<K, @UnsafeVariance V>
}



