/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableSet

import benchmarks.*
import kotlinx.collections.immutable.persistentSetOf
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

    private var persistentSet = persistentSetOf<IntWrapper>()

    @Setup(Level.Trial)
    fun prepare() {
        persistentSet = persistentSetAdd(implementation, generateElements(hashCodeType, size))
    }

    /**
     * Iterates all elements.
     *
     * Measures mean time and memory spent per `next` operation.
     *
     * Expected time: nearly constant (logarithmic for ordered persistent map)
     * Expected memory: none once iterator is created.
     */
    @Benchmark
    fun iterate(bh: Blackhole) {
        for (e in persistentSet) {
            bh.consume(e)
        }
    }
}