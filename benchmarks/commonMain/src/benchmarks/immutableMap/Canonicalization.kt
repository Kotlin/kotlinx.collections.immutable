/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableMap

import benchmarks.*
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.benchmark.*


/**
 * Benchmarks below measure how the trie canonicalization affects performance of the persistent map.
 *
 * Benchmark methods firstly remove some entries of the [persistentMap].
 * The expected height of the resulting map is half of the [persistentMap]'s expected height.
 * Then another operation is applied on the resulting map.
 *
 * If the [persistentMap] does not maintain its canonical form,
 * after removing some entries its actual average height will become bigger then its expected height.
 * Thus, many operations on the resulting map will be slower.
 */
@State(Scope.Benchmark)
open class Canonicalization {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000)
    var size: Int = 0

    @Param(HASH_IMPL, ORDERED_IMPL)
    var implementation = ""

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE)
    var hashCodeType = ""

    private var keys = listOf<IntWrapper>()
    private var keysToRemove = listOf<IntWrapper>()
    private var persistentMap = persistentMapOf<IntWrapper, String>()

    /**
     * Expected height of this persistent map is equal to the [persistentMap]'s expected height divided by 2.
     * Obtained by removing some entries of the [persistentMap].
     */
    private var halfHeightPersistentMap = persistentMapOf<IntWrapper, String>()

    @Setup
    fun prepare() {
        keys = generateKeys(hashCodeType, size)
        persistentMap = persistentMapPut(implementation, keys)

        val entriesToLeave = sizeForHalfHeight(persistentMap)
        keysToRemove = keys.shuffled().subList(fromIndex = entriesToLeave, toIndex = size)

        halfHeightPersistentMap = persistentMapRemove(persistentMap, keysToRemove)
    }

    /**
     * Removes `keysToRemove.size` entries of the [persistentMap] and
     * then puts `keysToRemove.size` new entries.
     *
     * Measures mean time and memory spent per (roughly one) `remove` and `put` operations.
     *
     * Expected time: [Remove.remove] + [putAfterRemove]
     * Expected memory: [Remove.remove] + [putAfterRemove]
     */
    @Benchmark
    fun removeAndPut(): PersistentMap<IntWrapper, String> {
        var map = persistentMapRemove(persistentMap, keysToRemove)

        for (key in keysToRemove) {
            map = map.copyingPut(key, "new value")
        }

        return map
    }

    /**
     * Removes `keysToRemove.size` entries of the [persistentMap] and
     * then iterates keys of the resulting map several times until iterating [size] elements.
     *
     * Measures mean time and memory spent per (roughly one) `remove` and `next` operations.
     *
     * Expected time: [Remove.remove] + [iterateKeysAfterRemove]
     * Expected memory: [Remove.remove] + [iterateKeysAfterRemove]
     */
    @Benchmark
    fun removeAndIterateKeys(bh: Blackhole) {
        val map = persistentMapRemove(persistentMap, keysToRemove)

        var count = 0
        while (count < size) {
            for (e in map) {
                bh.consume(e)

                if (++count == size)
                    break
            }
        }
    }

    /**
     * Puts `size - halfHeightPersistentMap.size` new entries to the [Canonicalization.halfHeightPersistentMap].
     *
     * Measures mean time and memory spent per (roughly one) `put` operation.
     *
     * Expected time: [Put.put]
     * Expected memory: [Put.put]
     */
    @Benchmark
    fun putAfterRemove(): PersistentMap<IntWrapper, String> {
        var map = halfHeightPersistentMap

        repeat(size - halfHeightPersistentMap.size) { index ->
            map = map.copyingPut(keys[index], "new value")
        }

        return map
    }

    /**
     * Iterates keys of the [Canonicalization.halfHeightPersistentMap] several times until iterating [size] elements.
     *
     * Measures mean time and memory spent per `iterate` operation.
     *
     * Expected time: [Iterate.iterateKeys] with [Iterate.size] = `halfHeightPersistentMap.size`
     * Expected memory: [Iterate.iterateKeys] with [Iterate.size] = `halfHeightPersistentMap.size`
     */
    @Benchmark
    fun iterateKeysAfterRemove(bh: Blackhole) {
        var count = 0
        while (count < size) {
            for (e in halfHeightPersistentMap) {
                bh.consume(e)

                if (++count == size)
                    break
            }
        }
    }
}
