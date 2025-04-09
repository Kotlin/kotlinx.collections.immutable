/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableList.builder

import benchmarks.*
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.benchmark.*
import kotlinx.collections.immutable.emptyPersistentList

@State(Scope.Benchmark)
open class AddAll {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000, BM_10000000)
    var size: Int = 0

    @Param(IP_100, IP_99_09, IP_95, IP_70, IP_50, IP_30, IP_0)
    var immutablePercentage: Double = 0.0

    private var listToAdd = emptyList<String>()

    @Setup
    fun prepare() {
        listToAdd = List(size) { "another element" }
    }

    // Results of the following benchmarks do not indicate memory or time spent per operation,
    // however regressions there do indicate changes.
    //
    // The benchmarks measure mean time and memory spent per added element.
    //
    // Expected time: nearly constant.
    // Expected memory: nearly constant.

    /**
     * Adds [size] elements to an empty persistent list builder using `addAll` operation.
     */
    @Benchmark
    fun addAllLast(): PersistentList.Builder<String> {
        val builder = emptyPersistentList<String>().builder()
        builder.addAll(listToAdd)
        return builder
    }

    /**
     * Adds `size / 2` elements to an empty persistent list builder
     * and then adds `size - size / 2` elements using `addAll` operation.
     */
    @Benchmark
    fun addAllLast_Half(): PersistentList.Builder<String> {
        val initialSize = size / 2
        val subListToAdd = listToAdd.subList(0, size - initialSize) // assuming subList creation is neglectable

        val builder = persistentListBuilderAdd(initialSize, immutablePercentage)
        builder.addAll(subListToAdd)
        return builder
    }

    /**
     * Adds `size - size / 3` elements to an empty persistent list builder
     * and then adds `size / 3` elements using `addAll` operation.
     */
    @Benchmark
    fun addAllLast_OneThird(): PersistentList.Builder<String> {
        val initialSize = size - size / 3
        val subListToAdd = listToAdd.subList(0, size - initialSize)

        val builder = persistentListBuilderAdd(initialSize, immutablePercentage)
        builder.addAll(subListToAdd)
        return builder
    }

    /**
     * Adds `size / 2` elements to an empty persistent list builder
     * and then inserts `size - size / 2` elements at the beginning using `addAll` operation.
     */
    @Benchmark
    fun addAllFirst_Half(): PersistentList.Builder<String> {
        val initialSize = size / 2
        val subListToAdd = listToAdd.subList(0, size - initialSize)

        val builder = persistentListBuilderAdd(initialSize, immutablePercentage)
        builder.addAll(0, subListToAdd)
        return builder
    }

    /**
     * Adds `size - size / 3` elements to an empty persistent list builder
     * and then inserts `size / 3` elements at the beginning using `addAll` operation.
     */
    @Benchmark
    fun addAllFirst_OneThird(): PersistentList.Builder<String> {
        val initialSize = size - size / 3
        val subListToAdd = listToAdd.subList(0, size - initialSize)

        val builder = persistentListBuilderAdd(initialSize, immutablePercentage)
        builder.addAll(0, subListToAdd)
        return builder
    }

    /**
     * Adds `size / 2` elements to an empty persistent list builder
     * and then inserts `size - size / 2` elements at the middle using `addAll` operation.
     */
    @Benchmark
    fun addAllMiddle_Half(): PersistentList.Builder<String> {
        val initialSize = size / 2
        val index = initialSize / 2
        val subListToAdd = listToAdd.subList(0, size - initialSize)

        val builder = persistentListBuilderAdd(initialSize, immutablePercentage)
        builder.addAll(index, subListToAdd)
        return builder
    }

    /**
     * Adds `size - size / 3` elements to an empty persistent list builder
     * and then inserts `size / 3` elements at the middle using `addAll` operation.
     */
    @Benchmark
    fun addAllMiddle_OneThird(): PersistentList.Builder<String> {
        val initialSize = size - size / 3
        val index = initialSize / 2
        val subListToAdd = listToAdd.subList(0, size - initialSize)

        val builder = persistentListBuilderAdd(initialSize, immutablePercentage)
        builder.addAll(index, subListToAdd)
        return builder
    }
}