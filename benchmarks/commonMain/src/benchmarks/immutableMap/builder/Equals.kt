/*
 * Copyright 2016-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableMap.builder

import benchmarks.*
import kotlinx.benchmark.*
import kotlinx.collections.immutable.persistentMapOf

@State(Scope.Benchmark)
open class Equals {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000)
    var size: Int = 0

    @Param(HASH_IMPL, ORDERED_IMPL)
    var implementation = ""

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE, NON_EXISTING_HASH_CODE)
    var hashCodeType = ""

    private var persistentMap = persistentMapOf<IntWrapper, String>().builder()
    private var sameMap = persistentMapOf<IntWrapper, String>().builder()
    private var slightlyDifferentMap = persistentMapOf<IntWrapper, String>().builder()
    private var veryDifferentMap = persistentMapOf<IntWrapper, String>().builder()

    @Setup
    fun prepare() {
        val keys = generateKeys(hashCodeType, size * 2)
        persistentMap = persistentMapBuilderPut(implementation, keys.take(size), 0.0)
        sameMap = persistentMapBuilderPut(implementation, keys.take(size), 0.0)
        slightlyDifferentMap = sameMap.build().builder()
        slightlyDifferentMap.put(keys[size], "different value")
        slightlyDifferentMap.remove(keys[0])
        veryDifferentMap = persistentMapBuilderPut(implementation, keys.drop(size), 0.0)
    }

    @Benchmark
    fun equalsTrue() = persistentMap == sameMap
    @Benchmark
    fun nearlyEquals() = persistentMap == slightlyDifferentMap
    @Benchmark
    fun notEquals() = persistentMap == veryDifferentMap

}