/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableMap.builder

import benchmarks.*
import kotlinx.collections.immutable.persistentMapOf
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

@State(Scope.Thread)
open class Iterate {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000)
    var size: Int = 0

    @Param(HASH_IMPL, ORDERED_IMPL)
    var implementation = ""

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE)
    var hashCodeType = ""

    @Param(IP_100, IP_99_09, IP_95, IP_70, IP_50, IP_30, IP_0)
    var immutablePercentage: Double = 0.0

    private var builder = persistentMapOf<IntWrapper, String>().builder()

    @Setup(Level.Trial)
    fun prepare() {
        val keys = generateKeys(hashCodeType, size)
        builder = persistentMapBuilderPut(implementation, keys, immutablePercentage)
    }

    @Benchmark
    fun iterateKeys(bh: Blackhole) {
        for (k in builder.keys) {
            bh.consume(k)
        }
    }

    @Benchmark
    fun iterateValues(bh: Blackhole) {
        for (v in builder.values) {
            bh.consume(v)
        }
    }

    @Benchmark
    fun iterateEntries(bh: Blackhole) {
        for (e in builder) {
            bh.consume(e)
        }
    }
}