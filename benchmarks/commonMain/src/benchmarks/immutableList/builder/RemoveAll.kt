/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableList.builder

import benchmarks.*
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.benchmark.*
import kotlin.random.Random

@State(Scope.Benchmark)
open class RemoveAll {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000, BM_10000000)
    var size: Int = 0

    @Param(IP_100, IP_99_09, IP_95, IP_70, IP_50, IP_30, IP_0)
    var immutablePercentage: Double = 0.0

    // Results of the following benchmarks do not indicate memory or time spent per operation,
    // however regressions there do indicate changes.
    //
    // The benchmarks measure (mean time and memory spent per `add` operation) + (time and memory spent on `removeAll` operation) / size.
    //
    // Expected time: [Add.addLast] + nearly constant.
    // Expected memory: [Add.addLast] + nearly constant.

    /**
     * Adds [size] elements to an empty persistent list builder
     * and then removes all of them using `removeAll(elements)` operation.
     */
    @Benchmark
    fun addAndRemoveAll_All(): Boolean {
        val builder = persistentListBuilderAddIndexes()
        val elementsToRemove = List(size) { it }
        return builder.removeAll(elementsToRemove)
    }

    /**
     * Adds [size] elements to an empty persistent list builder
     * and then removes half of them using `removeAll(elements)` operation.
     */
    @Benchmark
    fun addAndRemoveAll_RandomHalf(): Boolean {
        val builder = persistentListBuilderAddIndexes()
        val elementsToRemove = randomIndexes(size / 2)
        return builder.removeAll(elementsToRemove)
    }

    /**
     * Adds [size] elements to an empty persistent list builder
     * and then removes 10 of them using `removeAll(elements)` operation.
     */
    @Benchmark
    fun addAndRemoveAll_RandomTen(): Boolean {
        val builder = persistentListBuilderAddIndexes()
        val elementsToRemove = randomIndexes(10)
        return builder.removeAll(elementsToRemove)
    }

    /**
     * Adds [size] elements to an empty persistent list builder
     * and then removes last [tailSize] of them using `removeAll(elements)` operation.
     */
    @Benchmark
    fun addAndRemoveAll_Tail(): Boolean {
        val builder = persistentListBuilderAddIndexes()
        val elementsToRemove = List(tailSize()) { size - 1 - it }
        return builder.removeAll(elementsToRemove)
    }

    /**
     * Adds [size] elements to an empty persistent list builder
     * and then removes 10 non-existing elements using `removeAll(elements)` operation.
     */
    @Benchmark
    fun addAndRemoveAll_NonExisting(): Boolean {
        val builder = persistentListBuilderAddIndexes()
        val elementsToRemove = randomIndexes(10).map { size + it }
        return builder.removeAll(elementsToRemove)
    }

    private fun persistentListBuilderAddIndexes(): PersistentList.Builder<Int> {
        val immutableSize = immutableSize(size, immutablePercentage)
        var list = persistentListOf<Int>()
        for (i in 0 until immutableSize) {
            list = list.adding(i)
        }
        val builder = list.builder()
        for (i in immutableSize until size) {
            builder.add(i)
        }
        return builder
    }

    private fun randomIndexes(count: Int): List<Int> {
        return List(count) { Random.nextInt(size) }
    }

    private fun tailSize(): Int {
        val bufferSize = 32
        return (size and (bufferSize - 1)).let { if (it == 0) bufferSize else it }
    }
}
