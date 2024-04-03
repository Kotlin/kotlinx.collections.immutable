/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableMap

import benchmarks.*
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.benchmark.*
import kotlinx.collections.immutable.emptyPersistentMap

@State(Scope.Benchmark)
open class Iterate {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000)
    var size: Int = 0

    @Param(HASH_IMPL, ORDERED_IMPL)
    var implementation = ""

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE)
    var hashCodeType = ""

    private var persistentMap = emptyPersistentMap<IntWrapper, String>()

    @Setup
    fun prepare() {
        persistentMap = persistentMapPut(implementation, generateKeys(hashCodeType, size))
    }

    @Benchmark
    fun iterateKeys(bh: Blackhole) {
        for (k in persistentMap.keys) {
            bh.consume(k)
        }
    }

    @Benchmark
    fun iterateValues(bh: Blackhole) {
        for (v in persistentMap.values) {
            bh.consume(v)
        }
    }

    @Benchmark
    fun iterateEntries(bh: Blackhole) {
        for (e in persistentMap) {
            bh.consume(e)
        }
    }
}