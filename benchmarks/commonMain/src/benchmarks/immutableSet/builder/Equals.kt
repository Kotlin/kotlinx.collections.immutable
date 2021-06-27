/*
 * Copyright 2016-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableSet.builder

import benchmarks.*
import kotlinx.benchmark.*
import kotlinx.collections.immutable.persistentSetOf

@State(Scope.Benchmark)
open class Equals {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000)
    var size: Int = 0

    @Param(HASH_IMPL, ORDERED_IMPL)
    var implementation = ""

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE, NON_EXISTING_HASH_CODE)
    var hashCodeType = ""

    private var persistentSet = persistentSetOf<IntWrapper>().builder()
    private var sameSet = persistentSetOf<IntWrapper>().builder()
    private var slightlyDifferentSet = persistentSetOf<IntWrapper>().builder()
    private var veryDifferentSet = persistentSetOf<IntWrapper>().builder()

    @Setup
    fun prepare() {
        val keys = generateKeys(hashCodeType, size * 2)
        persistentSet = persistentSetBuilderAdd(implementation, keys.take(size), 0.0)
        sameSet = persistentSetBuilderAdd(implementation, keys.take(size), 0.0)
        slightlyDifferentSet = sameSet.build().builder()
        slightlyDifferentSet.add(keys[size])
        slightlyDifferentSet.remove(keys[0])
        veryDifferentSet = persistentSetBuilderAdd(implementation, keys.drop(size), 0.0)
    }

    @Benchmark
    fun equalsTrue() = persistentSet == sameSet
    @Benchmark
    fun nearlyEquals() = persistentSet == slightlyDifferentSet
    @Benchmark
    fun notEquals() = persistentSet == veryDifferentSet
}