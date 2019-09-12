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

    @Benchmark
    fun addLast(): PersistentList.Builder<String> {
        return persistentListBuilderAdd(size, immutablePercentage)
    }

    @Benchmark
    fun addLastAndIterate(bh: Blackhole) {
        val builder = persistentListBuilderAdd(size, immutablePercentage)
        for (e in builder) {
            bh.consume(e)
        }
    }

    @Benchmark
    fun addLastAndGet(bh: Blackhole) {
        val builder = persistentListBuilderAdd(size, immutablePercentage)
        for (i in 0 until builder.size) {
            bh.consume(builder[i])
        }
    }
}