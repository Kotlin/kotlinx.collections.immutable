/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableList

import benchmarks.*
import kotlinx.collections.immutable.ImmutableList
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

@State(Scope.Thread)
open class Add {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000, BM_10000000)
    var size: Int = 0

    /**
     * Adds [size] elements to an empty persistent list.
     *
     * Measures mean time and memory spent per `add` operation.
     *
     * Expected time: nearly constant.
     * Expected memory: for size in 1..32 - O(size), nearly constant otherwise.
     */
    @Benchmark
    fun addLast(): ImmutableList<String> {
        return persistentListAdd(size)
    }

    /**
     * Adds [size] elements to an empty persistent list and then iterates all elements from first to last.
     *
     * Measures mean time and memory spent per `add` and `next` operations.
     *
     * Expected time: [addLast] + [Iterate.firstToLast]
     * Expected memory: [addLast] + [Iterate.firstToLast]
     */
    @Benchmark
    fun addLastAndIterate(bh: Blackhole) {
        val list = persistentListAdd(size)
        for (e in list) {
            bh.consume(e)
        }
    }

    /**
     * Adds [size] elements to an empty persistent list and then gets all elements by index from first to last.
     *
     * Measures mean time and memory spent per `add` and `get` operations.
     *
     * Expected time: [addLast] + [Get.getByIndex]
     * Expected memory: [addLast] + [Get.getByIndex]
     */
    @Benchmark
    fun addLastAndGet(bh: Blackhole) {
        val list = persistentListAdd(size)
        for (i in 0 until list.size) {
            bh.consume(list[i])
        }
    }

    /**
     * Adds [size] - 1 elements to an empty persistent list
     * and then inserts one element at the beginning.
     *
     * Measures mean time and memory spent per `add` operation.
     *
     * Expected time: nearly constant.
     * Expected memory: nearly constant.
     */
    @Benchmark
    fun addFirst(): ImmutableList<String> {
        return persistentListAdd(size - 1).add(0, "another element")
    }

    /**
     * Adds [size] - 1 elements to an empty persistent list
     * and then inserts one element at the middle.
     *
     * Measures mean time and memory spent per `add` operation.
     *
     * Expected time: nearly constant.
     * Expected memory: nearly constant.
     */
    @Benchmark
    fun addMiddle(): ImmutableList<String> {
        return persistentListAdd(size - 1).add(size / 2, "another element")
    }
}