/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableList.builder

import benchmarks.*
import kotlinx.collections.immutable.PersistentList
import org.openjdk.jmh.annotations.*

@State(Scope.Thread)
open class Remove {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000, BM_10000000)
    var size: Int = 0

    @Param(IP_100, IP_99_09, IP_95, IP_70, IP_50, IP_30, IP_0)
    var immutablePercentage: Double = 0.0

    /**
     * Adds [size] elements to an empty persistent list builder and then removes each element by index starting from last to first.
     *
     * Measures mean time and memory spent per `add` and `removeAt` operations.
     *
     * Expected time: [Add.addLast] + nearly constant.
     * Expected memory: [Add.addLast] + nearly constant.
     */
    @Benchmark
    fun addAndRemoveLast(): PersistentList.Builder<String> {
        val builder = persistentListBuilderAdd(size, immutablePercentage)
        for (i in 0 until size) {
            builder.removeAt(builder.size - 1)
        }
        return builder
    }

    /**
     * Adds [size] elements to an empty persistent list builder
     * and then removes one element from the beginning.
     *
     * Measures (mean time and memory spent per `add` operation) + (time and memory spent on `removeAt` operation) / size.
     *
     * Expected time: [Add.addLast] + nearly constant.
     * Expected memory: [Add.addLast] + nearly constant.
     */
    @Benchmark
    fun addAndRemoveFirst(): String {
        val builder = persistentListBuilderAdd(size, immutablePercentage)
        return builder.removeAt(0)
    }

    /**
     * Adds [size] elements to an empty persistent list builder
     * and then removes one element from the middle.
     *
     * Measures (mean time and memory spent per `add` operation) + (time and memory spent on `removeAt` operation) / size.
     *
     * Expected time: [Add.addLast] + nearly constant.
     * Expected memory: [Add.addLast] + nearly constant.
     */
    @Benchmark
    fun addAndRemoveMiddle(): String {
        val builder = persistentListBuilderAdd(size, immutablePercentage)
        return builder.removeAt(size / 2)
    }
}