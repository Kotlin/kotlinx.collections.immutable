/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableList

import benchmarks.*
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.benchmark.*
import kotlin.random.Random

@State(Scope.Benchmark)
open class RemoveAll {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000, BM_10000000)
    var size: Int = 0

    private var persistentList: PersistentList<Int> = persistentListOf()

    @Setup
    fun prepare() {
        persistentList = persistentListOf<Int>().addingAll(List(size) { it })
    }

    // Results of the following benchmarks do not indicate memory or time spent per operation,
    // however regressions there do indicate changes.
    //
    // The benchmarks measure (time and memory spent on `removeAll` operation) / size
    //
    // Expected time: nearly constant
    // Expected memory: nearly constant

    /**
     * Removes all elements using `removeAll(elements)` operation.
     */
    @Benchmark
    fun removeAll_All(): PersistentList<Int> {
        val list = persistentList
        val elementsToRemove = List(size) { it }
        return list.removeAll(elementsToRemove)
    }

    /**
     * Removes half of the elements using `removeAll(elements)` operation.
     */
    @Benchmark
    fun removeAll_RandomHalf(): PersistentList<Int> {
        val list = persistentList
        val elementsToRemove = randomIndexes(size / 2)
        return list.removeAll(elementsToRemove)
    }

    /**
     * Removes 10 random elements using `removeAll(elements)` operation.
     */
    @Benchmark
    fun removeAll_RandomTen(): PersistentList<Int> {
        val list = persistentList
        val elementsToRemove = randomIndexes(10)
        return list.removeAll(elementsToRemove)
    }

    /**
     * Removes last [tailSize] elements using `removeAll(elements)` operation.
     */
    @Benchmark
    fun removeAll_Tail(): PersistentList<Int> {
        val list = persistentList
        val elementsToRemove = List(tailSize()) { size - 1 - it }
        return list.removeAll(elementsToRemove)
    }

    /**
     * Removes 10 non-existing elements using `removeAll(elements)` operation.
     */
    @Benchmark
    fun removeAll_NonExisting(): PersistentList<Int> {
        val list = persistentList
        val elementsToRemove = randomIndexes(10).map { size + it }
        return list.removeAll(elementsToRemove)
    }

    private fun randomIndexes(count: Int): List<Int> {
        return List(count) { Random.nextInt(size) }
    }

    private fun tailSize(): Int {
        val bufferSize = 32
        return (size and (bufferSize - 1)).let { if (it == 0) bufferSize else it }
    }
}
