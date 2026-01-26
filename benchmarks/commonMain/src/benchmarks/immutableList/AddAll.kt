/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableList

import benchmarks.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.benchmark.*

@State(Scope.Benchmark)
open class AddAll {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000, BM_10000000)
    var size: Int = 0

    private var listToAdd = emptyList<String>()

    @Setup
    fun prepare() {
        listToAdd = List(size) { "another element" }
    }

    // Results of the following benchmarks do not indicate memory or time spent per operation,
    // however regressions there do indicate changes.
    //
    // the benchmarks measure mean time and memory spent per added element.
    //
    // Expected time: nearly constant.
    // Expected memory: nearly constant.

    /**
     * Adds [size] elements to an empty persistent list using `addAll` operation.
     */
    @Benchmark
    fun addAllLast(): ImmutableList<String> {
        return persistentListOf<String>().addingAll(listToAdd)
    }

    /**
     * Adds `size / 2` elements to an empty persistent list
     * and then adds `size - size / 2` elements using `addAll` operation.
     */
    @Benchmark
    fun addAllLast_Half(): ImmutableList<String> {
        val initialSize = size / 2
        val subListToAdd = listToAdd.subList(0, size - initialSize) // assuming subList creation is neglectable
        return persistentListAdd(initialSize).addingAll(subListToAdd)
    }

    /**
     * Adds `size - size / 3` elements to an empty persistent list
     * and then adds `size / 3` elements using `addAll` operation.
     */
    @Benchmark
    fun addAllLast_OneThird(): ImmutableList<String> {
        val initialSize = size - size / 3
        val subListToAdd = listToAdd.subList(0, size - initialSize)
        return persistentListAdd(initialSize).addingAll(subListToAdd)
    }

    /**
     * Adds `size / 2` elements to an empty persistent list
     * and then inserts `size - size / 2` elements at the beginning using `addAll` operation.
     */
    @Benchmark
    fun addAllFirst_Half(): ImmutableList<String> {
        val initialSize = size / 2
        val subListToAdd = listToAdd.subList(0, size - initialSize)
        return persistentListAdd(initialSize).insertingAllAt(0, subListToAdd)
    }

    /**
     * Adds `size - size / 3` elements to an empty persistent list
     * and then inserts `size / 3` elements at the beginning using `addAll` operation.
     */
    @Benchmark
    fun addAllFirst_OneThird(): ImmutableList<String> {
        val initialSize = size - size / 3
        val subListToAdd = listToAdd.subList(0, size - initialSize)
        return persistentListAdd(initialSize).insertingAllAt(0, subListToAdd)
    }

    /**
     * Adds `size / 2` elements to an empty persistent list
     * and then inserts `size - size / 2` elements at the middle using `addAll` operation.
     */
    @Benchmark
    fun addAllMiddle_Half(): ImmutableList<String> {
        val initialSize = size / 2
        val index = initialSize / 2
        val subListToAdd = listToAdd.subList(0, size - initialSize)
        return persistentListAdd(initialSize).insertingAllAt(index, subListToAdd)
    }

    /**
     * Adds `size - size / 3` elements to an empty persistent list builder
     * and then inserts `size / 3` elements at the middle using `addAll` operation.
     */
    @Benchmark
    fun addAllMiddle_OneThird(): ImmutableList<String> {
        val initialSize = size - size / 3
        val index = initialSize / 2
        val subListToAdd = listToAdd.subList(0, size - initialSize)
        return persistentListAdd(initialSize).insertingAllAt(index, subListToAdd)
    }
}
