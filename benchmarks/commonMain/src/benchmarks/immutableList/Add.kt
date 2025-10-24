/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableList

import benchmarks.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.benchmark.*

@State(Scope.Benchmark)
open class Add {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000, BM_10000000)
    var size: Int = 0

    @Benchmark
    fun addLast(): ImmutableList<String> {
        return persistentListAdd(size)
    }

    @Benchmark
    fun addLastAndIterate(bh: Blackhole) {
        val list = persistentListAdd(size)
        for (e in list) {
            bh.consume(e)
        }
    }

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
        return persistentListAdd(size - 1).adding(0, "another element")
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
        return persistentListAdd(size - 1).adding(size / 2, "another element")
    }
}
