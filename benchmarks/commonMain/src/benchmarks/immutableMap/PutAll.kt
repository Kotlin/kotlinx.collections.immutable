/*
 * Copyright 2016-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableMap

import benchmarks.*
import kotlinx.collections.immutable.PersistentMap
import kotlinx.benchmark.*
import kotlinx.collections.immutable.persistentMapOf

@State(Scope.Benchmark)
open class PutAll {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000)
    var size: Int = 0

    @Param(HASH_IMPL, ORDERED_IMPL)
    var implementation = ""

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE)
    var hashCodeType = ""

    private var lhs = persistentMapOf<IntWrapper, String>()
    private var lhsSmall = persistentMapOf<IntWrapper, String>()
    private var rhs = persistentMapOf<IntWrapper, String>()
    private var rhsSmall = persistentMapOf<IntWrapper, String>()

    @Setup
    fun prepare() {
        val keys = generateKeys(hashCodeType, 2 * size)
        lhs = persistentMapPut(implementation, keys.take(size))
        lhsSmall = persistentMapPut(implementation, keys.take((size / 1000) + 1))
        rhs = persistentMapPut(implementation, keys.takeLast(size))
        rhsSmall = persistentMapPut(implementation, keys.takeLast((size / 1000) + 1))
    }

    @Benchmark
    fun putAllEqualSize(): PersistentMap<IntWrapper, String> {
        return lhs.putAll(rhs)
    }

    @Benchmark
    fun putAllSmallIntoLarge(): PersistentMap<IntWrapper, String> {
        return lhs.putAll(rhsSmall)
    }

    @Benchmark
    fun putAllLargeIntoSmall(): PersistentMap<IntWrapper, String> {
        return lhsSmall.putAll(rhs)
    }
}