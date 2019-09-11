/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableMap.builder

import benchmarks.*
import kotlinx.collections.immutable.PersistentMap
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

    private var keys = listOf<IntWrapper>()
    private var keysToRemove = listOf<IntWrapper>()

    @Setup
    fun prepare() {
        keys = generateKeys(hashCodeType, size)

        keysToRemove = if (hashCodeType == NON_EXISTING_HASH_CODE) {
            generateKeys(hashCodeType, size)
        } else {
            keys
        }
    }

    // Q: Why not to benchmark pure remove method?
    // A: Each invocation of remove benchmark method would clear the builder and creating new one would be needed each time.
    // Setting `@Setup(Level.Invocation)` may cause bad benchmark accuracy amid frequent `prepare` calls, especially for small `size`.
    @Benchmark
    fun putAndRemove(): PersistentMap.Builder<IntWrapper, String> {
        val builder = persistentMapBuilderPut(implementation, keys, immutablePercentage)
        repeat(times = size) { index ->
            builder.remove(keysToRemove[index])
        }
        return builder
    }
}