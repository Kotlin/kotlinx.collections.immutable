/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableSet

import benchmarks.*
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.benchmark.*
import kotlinx.collections.immutable.emptyPersistentSet

@State(Scope.Benchmark)
open class Iterate {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000)
    var size: Int = 0

    @Param(HASH_IMPL, ORDERED_IMPL)
    var implementation = ""

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE)
    var hashCodeType = ""

    private var persistentSet = emptyPersistentSet<IntWrapper>()

    @Setup
    fun prepare() {
        persistentSet = persistentSetAdd(implementation, generateElements(hashCodeType, size))
    }

    @Benchmark
    fun iterate(bh: Blackhole) {
        for (e in persistentSet) {
            bh.consume(e)
        }
    }
}