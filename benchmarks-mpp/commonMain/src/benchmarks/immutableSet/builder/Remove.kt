/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableSet.builder

import benchmarks.*
import kotlinx.collections.immutable.PersistentSet
import kotlinx.benchmark.*

@State(Scope.Benchmark)
open class Remove {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000)
    var size: Int = 0

    @Param(HASH_IMPL, ORDERED_IMPL)
    var implementation = ""

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE, NON_EXISTING_HASH_CODE)
    var hashCodeType = ""

    @Param(IP_100, IP_99_09, IP_95, IP_70, IP_50, IP_30, IP_0)
    var immutablePercentage: Double = 0.0

    private var elements = listOf<IntWrapper>()
    private var elementsToRemove = listOf<IntWrapper>()

    @Setup
    fun prepare() {
        elements = generateElements(hashCodeType, size)

        elementsToRemove = if (hashCodeType == NON_EXISTING_HASH_CODE) {
            generateElements(hashCodeType, size)
        } else {
            elements
        }
    }

    @Benchmark
    fun addAndRemove(): PersistentSet.Builder<IntWrapper> {
        val builder = persistentSetBuilderAdd(implementation, elements, immutablePercentage)
        repeat(times = size) { index ->
            builder.remove(elementsToRemove[index])
        }
        return builder
    }
}