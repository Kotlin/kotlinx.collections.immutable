/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableList.builder

import benchmarks.*
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.benchmark.*
import kotlinx.collections.immutable.emptyPersistentList

@State(Scope.Benchmark)
open class Set {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000, BM_10000000)
    var size: Int = 0

    @Param(IP_100, IP_99_09, IP_95, IP_70, IP_50, IP_30, IP_0)
    var immutablePercentage: Double = 0.0

    private var builder = emptyPersistentList<String>().builder()
    private var randomIndices = listOf<Int>()

    @Setup
    fun prepare() {
        builder = persistentListBuilderAdd(size, immutablePercentage)
        randomIndices = List(size) { it }.shuffled()
    }

    @Benchmark
    fun setByIndex(): PersistentList.Builder<String> {
        for (i in 0 until size) {
            builder[i] = "another element"
        }
        return builder
    }

    @Benchmark
    fun setByRandomIndex(): PersistentList.Builder<String> {
        for (i in 0 until size) {
            builder[randomIndices[i]] = "another element"
        }
        return builder
    }
}