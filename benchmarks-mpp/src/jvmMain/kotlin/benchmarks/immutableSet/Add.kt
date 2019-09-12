/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableSet

import benchmarks.*
import kotlinx.collections.immutable.ImmutableSet
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

@State(Scope.Thread)
open class Add {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000)
    var size: Int = 0

    @Param(HASH_IMPL, ORDERED_IMPL)
    var implementation = ""

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE)
    var hashCodeType = ""

    private var elements = listOf<IntWrapper>()

    @Setup(Level.Trial)
    fun prepare() {
        elements = generateElements(hashCodeType, size)
    }

    @Benchmark
    fun add(): ImmutableSet<IntWrapper> {
        return persistentSetAdd(implementation, elements)
    }

    @Benchmark
    fun addAndContains(bh: Blackhole) {
        val set = persistentSetAdd(implementation, elements)
        repeat(times = size) { index ->
            bh.consume(set.contains(elements[index]))
        }
    }

    @Benchmark
    fun addAndIterate(bh: Blackhole) {
        val set = persistentSetAdd(implementation, elements)
        for (element in set) {
            bh.consume(element)
        }
    }
}