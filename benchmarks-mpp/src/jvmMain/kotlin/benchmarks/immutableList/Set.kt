/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableList

import benchmarks.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.openjdk.jmh.annotations.*

@State(Scope.Benchmark)
open class Set {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000, BM_10000000)
    var size: Int = 0

    private var persistentList: PersistentList<String> = persistentListOf()
    private var randomIndices = listOf<Int>()

    @Setup(Level.Trial)
    fun prepare() {
        persistentList = persistentListAdd(size)
        randomIndices = List(size) { it }.shuffled()
    }

    /**
     * Updates each element by index starting from first to last.
     *
     * Measures mean time and memory spent per `set` operation.
     *
     * Expected time: logarithmic
     * Expected memory: logarithmic
     */
    @Benchmark
    fun setByIndex(): ImmutableList<String> {
        repeat(times = size) { index ->
            persistentList = persistentList.set(index, "another element")
        }
        return persistentList
    }

    /**
     * Updates each element by index randomly.
     *
     * Measures mean time and memory spent per `set` operation.
     *
     * Expected time: logarithmic
     * Expected memory: logarithmic
     */
    @Benchmark
    fun setByRandomIndex(): ImmutableList<String> {
        repeat(times = size) { index ->
            persistentList = persistentList.set(randomIndices[index], "another element")
        }
        return persistentList
    }
}