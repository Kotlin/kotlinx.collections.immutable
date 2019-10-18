/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableMap

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

    private var persistentMap = persistentMapOf<IntWrapper, String>()

    @Setup(Level.Trial)
    fun prepare() {
        persistentMap = persistentMapPut(implementation, generateKeys(hashCodeType, size))
    }

    /**
     * Iterates all keys.
     *
     * Measures mean time and memory spent per `next` operation.
     *
     * Expected time: nearly constant (logarithmic for ordered persistent map)
     * Expected memory: none once iterator is created.
     */
    @Benchmark
    fun iterateKeys(bh: Blackhole) {
        for (k in persistentMap.keys) {
            bh.consume(k)
        }
    }

    /**
     * Iterates all values.
     *
     * Measures mean time and memory spent per `next` operation.
     *
     * Expected time: nearly constant (logarithmic for ordered persistent map)
     * Expected memory: none once iterator is created.
     */
    @Benchmark
    fun iterateValues(bh: Blackhole) {
        for (v in persistentMap.values) {
            bh.consume(v)
        }
    }

    /**
     * Iterates all entries.
     *
     * Measures mean time and memory spent per `next` operation.
     *
     * Expected time: nearly constant (logarithmic for ordered persistent map)
     * Expected memory: constant.
     */
    @Benchmark
    fun iterateEntries(bh: Blackhole) {
        for (e in persistentMap) {
            bh.consume(e)
        }
    }
}