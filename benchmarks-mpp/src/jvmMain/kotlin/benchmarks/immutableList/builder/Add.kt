/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableList.builder

import benchmarks.*
import kotlinx.collections.immutable.PersistentList
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

@State(Scope.Thread)
open class Add {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000, BM_10000000)
    var size: Int = 0

    @Param(IP_100, IP_99_09, IP_95, IP_70, IP_50, IP_30, IP_0)
    var immutablePercentage: Double = 0.0

    /**
     * Adds [size] elements to an empty persistent list builder.
     *
     * Measures mean time and memory spent per `add` operation.
     *
     * Expected time: nearly constant.
     * Expected memory: nearly constant.
     */
    @Benchmark
    fun addLast(): PersistentList.Builder<String> {
        return persistentListBuilderAdd(size, immutablePercentage)
    }

    /**
     * Adds [size] elements to an empty persistent list builder and then iterates all elements from first to last.
     *
     * Measures mean time and memory spent per `add` and `next` operations.
     *
     * Expected time: [addLast] + [Iterate.firstToLast]
     * Expected memory: [addLast] + [Iterate.firstToLast]
     */
    @Benchmark
    fun addLastAndIterate(bh: Blackhole) {
        val builder = persistentListBuilderAdd(size, immutablePercentage)
        for (e in builder) {
            bh.consume(e)
        }
    }

    /**
     * Adds [size] elements to an empty persistent list builder and then gets all elements by index from first to last.
     *
     * Measures mean time and memory spent per `add` and `get` operations.
     *
     * Expected time: [addLast] + [Get.getByIndex]
     * Expected memory: [addLast] + [Get.getByIndex]
     */
    @Benchmark
    fun addLastAndGet(bh: Blackhole) {
        val builder = persistentListBuilderAdd(size, immutablePercentage)
        for (i in 0 until builder.size) {
            bh.consume(builder[i])
        }
    }

    /**
     * Adds [size] - 1 elements to an empty persistent list builder
     * and then inserts one element at the beginning.
     *
     * Measures mean time and memory spent per `add` operation.
     *
     * Expected time: nearly constant.
     * Expected memory: nearly constant.
     */
    @Benchmark
    fun addFirst(): PersistentList.Builder<String> {
        val builder = persistentListBuilderAdd(size - 1, immutablePercentage)
        builder.add(0, "another element")
        return builder
    }

    /**
     * Adds [size] - 1 elements to an empty persistent list builder
     * and then inserts one element at the middle.
     *
     * Measures mean time and memory spent per `add` operation.
     *
     * Expected time: nearly constant.
     * Expected memory: nearly constant.
     */
    @Benchmark
    fun addMiddle(): PersistentList.Builder<String> {
        val builder = persistentListBuilderAdd(size - 1, immutablePercentage)
        builder.add(size / 2, "another element")
        return builder
    }
}