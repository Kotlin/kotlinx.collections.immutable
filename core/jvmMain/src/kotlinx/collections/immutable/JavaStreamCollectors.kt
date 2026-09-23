/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.kotlinx.collections.immutable

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import java.util.function.BiConsumer
import java.util.function.BinaryOperator
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Collector

public fun <T> collectToPersistentList(): Collector<T, *, PersistentList<T>> {
   return object : Collector<T, PersistentList.Builder<T>, PersistentList<T>> {
        override fun supplier(): Supplier<PersistentList.Builder<T>> = Supplier { persistentListOf<T>().builder() }

        override fun accumulator(): BiConsumer<PersistentList.Builder<T>, T> = BiConsumer { builder, value ->
            builder.add(value)
        }

        override fun combiner(): BinaryOperator<PersistentList.Builder<T>> {
            return BinaryOperator { b1, b2 -> b1.addAll(b2); b1 }
        }

        override fun finisher(): Function<PersistentList.Builder<T>, PersistentList<T>> {
            return Function { it.build() }
        }

        override fun characteristics(): Set<Collector.Characteristics> {
            return persistentHashSetOf()
        }
    }
}

public fun <T> collectToPersistentSet(): Collector<T, *, PersistentSet<T>> {
    return object : Collector<T, PersistentSet.Builder<T>, PersistentSet<T>> {
        override fun supplier(): Supplier<PersistentSet.Builder<T>> = Supplier { persistentSetOf<T>().builder() }

        override fun accumulator(): BiConsumer<PersistentSet.Builder<T>, T> = BiConsumer { builder, value ->
            builder.add(value)
        }

        override fun combiner(): BinaryOperator<PersistentSet.Builder<T>> {
            return BinaryOperator { b1, b2 -> b1.addAll(b2); b1 }
        }

        override fun finisher(): Function<PersistentSet.Builder<T>, PersistentSet<T>> {
            return Function { it.build() }
        }

        override fun characteristics(): Set<Collector.Characteristics> {
            return persistentHashSetOf()
        }
    }
}

public fun <T> collectToPersistentHashSet(): Collector<T, *, PersistentSet<T>> {
    return object : Collector<T, PersistentSet.Builder<T>, PersistentSet<T>> {
        override fun supplier(): Supplier<PersistentSet.Builder<T>> = Supplier { persistentHashSetOf<T>().builder() }

        override fun accumulator(): BiConsumer<PersistentSet.Builder<T>, T> = BiConsumer { builder, value ->
            builder.add(value)
        }

        override fun combiner(): BinaryOperator<PersistentSet.Builder<T>> {
            return BinaryOperator { b1, b2 -> b1.addAll(b2); b1 }
        }

        override fun finisher(): Function<PersistentSet.Builder<T>, PersistentSet<T>> {
            return Function { it.build() }
        }

        override fun characteristics(): Set<Collector.Characteristics> {
            return persistentHashSetOf(Collector.Characteristics.UNORDERED)
        }
    }
}

public fun <T, K> collectToPersistentHashMap(keyMapper: (T) -> K): Collector<T, *, PersistentMap<K, T>> {
    return collectToPersistentHashMap(keyMapper, { it })
}

public fun <T, K, U> collectToPersistentHashMap(keyMapper: (T) -> K, valueMapper: (T) -> U): Collector<T, *, PersistentMap<K, U>> {
    return object : Collector<T, PersistentMap.Builder<K, U>, PersistentMap<K, U>> {
        override fun supplier(): Supplier<PersistentMap.Builder<K, U>> = Supplier { persistentHashMapOf<K, U>().builder() }

        override fun accumulator(): BiConsumer<PersistentMap.Builder<K, U>, T> = BiConsumer { builder, value ->
            builder[keyMapper(value)] = valueMapper(value)
        }

        override fun combiner(): BinaryOperator<PersistentMap.Builder<K, U>> {
            return BinaryOperator { b1, b2 -> b1.putAll(b2); b1 }
        }

        override fun finisher(): Function<PersistentMap.Builder<K, U>, PersistentMap<K, U>> {
            return Function { it.build() }
        }

        override fun characteristics(): Set<Collector.Characteristics> {
            return persistentHashSetOf(Collector.Characteristics.UNORDERED)
        }
    }
}

public fun <T, K> collectToPersistentMap(keyMapper: (T) -> K): Collector<T, *, PersistentMap<K, T>> {
    return collectToPersistentMap(keyMapper, { it })
}

public fun <T, K, U> collectToPersistentMap(keyMapper: (T) -> K, valueMapper: (T) -> U): Collector<T, *, PersistentMap<K, U>> {
    return object : Collector<T, PersistentMap.Builder<K, U>, PersistentMap<K, U>> {
        override fun supplier(): Supplier<PersistentMap.Builder<K, U>> = Supplier { persistentMapOf<K, U>().builder() }

        override fun accumulator(): BiConsumer<PersistentMap.Builder<K, U>, T> = BiConsumer { builder, value ->
            builder[keyMapper(value)] = valueMapper(value)
        }

        override fun combiner(): BinaryOperator<PersistentMap.Builder<K, U>> {
            return BinaryOperator { b1, b2 -> b1.putAll(b2); b1 }
        }

        override fun finisher(): Function<PersistentMap.Builder<K, U>, PersistentMap<K, U>> {
            return Function { it.build() }
        }

        override fun characteristics(): Set<Collector.Characteristics> {
            return persistentHashSetOf()
        }
    }
}

