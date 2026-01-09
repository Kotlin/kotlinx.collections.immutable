/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableMap

import benchmarks.*
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.benchmark.*

@State(Scope.Benchmark)
open class Remove {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000)
    var size: Int = 0

    @Param(HASH_IMPL, ORDERED_IMPL)
    var implementation = ""

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE, NON_EXISTING_HASH_CODE)
    var hashCodeType = ""

    private var keys = listOf<IntWrapper>()
    private var persistentMap = persistentMapOf<IntWrapper, String>()

    @Setup
    fun prepare() {
        keys = generateKeys(hashCodeType, size)
        persistentMap = persistentMapPut(implementation, keys)

        if (hashCodeType == NON_EXISTING_HASH_CODE)
            keys = generateKeys(hashCodeType, size)
    }

    @Benchmark
    fun remove(): PersistentMap<IntWrapper, String> {
        var map = persistentMap
        repeat(times = size) { index ->
            map = map.copyingRemove(keys[index])
        }
        return map
    }
}
