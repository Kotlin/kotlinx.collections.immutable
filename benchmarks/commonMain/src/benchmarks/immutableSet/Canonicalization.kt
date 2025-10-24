/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableSet

import benchmarks.*
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.benchmark.*


/**
 * Benchmarks below measure how the trie canonicalization affects performance of the persistent set.
 *
 * Benchmark methods firstly remove some elements of the [persistentSet].
 * The expected height of the resulting set is half of the [persistentSet]'s expected height.
 * Then another operation is applied on the resulting set.
 *
 * If the [persistentSet] does not maintain its canonical form,
 * after removing some elements its actual average height will become bigger then its expected height.
 * Thus, many operations on the resulting set will be slower.
 */
@State(Scope.Benchmark)
open class Canonicalization {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000)
    var size: Int = 0

    @Param(HASH_IMPL, ORDERED_IMPL)
    var implementation = ""

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE)
    var hashCodeType = ""

    private var elements = listOf<IntWrapper>()
    private var elementsToRemove = listOf<IntWrapper>()
    private var persistentSet = persistentSetOf<IntWrapper>()

    /**
     * Expected height of this persistent set is equal to the [persistentSet]'s expected height divided by 2.
     * Obtained by removing some elements of the [persistentSet].
     */
    private var halfHeightPersistentSet = persistentSetOf<IntWrapper>()

    @Setup
    fun prepare() {
        elements = generateElements(hashCodeType, size)
        persistentSet = persistentSetAdd(implementation, elements)

        val elementsToLeave = sizeForHalfHeight(persistentSet)
        elementsToRemove = elements.shuffled().subList(fromIndex = elementsToLeave, toIndex = size)

        halfHeightPersistentSet = persistentSetRemove(persistentSet, elementsToRemove)
    }

    /**
     * Removes `elementsToRemove.size` elements of the [persistentSet] and
     * then puts `elementsToRemove.size` new elements.
     *
     * Measures mean time and memory spent per (roughly one) `remove` and `add` operations.
     *
     * Expected time: [Remove.remove] + [addAfterRemove]
     * Expected memory: [Remove.remove] + [addAfterRemove]
     */
    @Benchmark
    fun removeAndAdd(): PersistentSet<IntWrapper> {
        var set = persistentSetRemove(persistentSet, elementsToRemove)

        for (element in elementsToRemove) {
            set = set.adding(element)
        }

        return set
    }

    /**
     * Removes `elementsToRemove.size` elements of the [persistentSet] and
     * then iterates elements of the resulting set several times until iterating [size] elements.
     *
     * Measures mean time and memory spent per (roughly one) `remove` and `next` operations.
     *
     * Expected time: [Remove.remove] + [iterateAfterRemove]
     * Expected memory: [Remove.remove] + [iterateAfterRemove]
     */
    @Benchmark
    fun removeAndIterate(bh: Blackhole) {
        val set = persistentSetRemove(persistentSet, elementsToRemove)

        var count = 0
        while (count < size) {
            for (e in set) {
                bh.consume(e)

                if (++count == size)
                    break
            }
        }
    }

    /**
     * Adds `size - halfHeightPersistentSet.size` new elements to the [Canonicalization.halfHeightPersistentSet].
     *
     * Measures mean time and memory spent per (roughly one) `add` operation.
     *
     * Expected time: [Add.add]
     * Expected memory: [Add.add]
     */
    @Benchmark
    fun addAfterRemove(): PersistentSet<IntWrapper> {
        var set = halfHeightPersistentSet

        for (element in elementsToRemove) {
            set = set.adding(element)
        }

        return set
    }

    /**
     * Iterates elements of the [Remove.halfHeightPersistentSet] several times until iterating [size] elements.
     *
     * Measures mean time and memory spent per `iterate` operation.
     *
     * Expected time: [Iterate.iterate] with [Iterate.size] = `halfHeightPersistentSet.size`
     * Expected memory: [Iterate.iterate] with [Iterate.size] = `halfHeightPersistentSet.size`
     */
    @Benchmark
    fun iterateAfterRemove(bh: Blackhole) {
        var count = 0
        while (count < size) {
            for (e in halfHeightPersistentSet) {
                bh.consume(e)

                if (++count == size)
                    break
            }
        }
    }
}
