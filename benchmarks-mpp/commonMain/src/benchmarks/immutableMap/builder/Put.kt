/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableMap.builder

import benchmarks.*
import kotlinx.collections.immutable.PersistentMap
import kotlinx.benchmark.*

@State(Scope.Benchmark)
open class Put {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000)
    var size: Int = 0

    @Param(HASH_IMPL, ORDERED_IMPL)
    var implementation = ""

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE)
    var hashCodeType = ""

    @Param(IP_100, IP_99_09, IP_95, IP_70, IP_50, IP_30, IP_0)
    var immutablePercentage: Double = 0.0

    private var keys = listOf<IntWrapper>()

    @Setup
    fun prepare() {
        keys = generateKeys(hashCodeType, size)
    }

    @Benchmark
    fun put(): PersistentMap.Builder<IntWrapper, String> {
        return persistentMapBuilderPut(implementation, keys, immutablePercentage)
    }

    @Benchmark
    fun putAndGet(bh: Blackhole) {
        val builder = persistentMapBuilderPut(implementation, keys, immutablePercentage)
        repeat(times = size) { index ->
            bh.consume(builder[keys[index]])
        }
    }

    @Benchmark
    fun putAndIterateKeys(bh: Blackhole) {
        val builder = persistentMapBuilderPut(implementation, keys, immutablePercentage)
        for (key in builder.keys) {
            bh.consume(key)
        }
    }
}