/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableList

import benchmarks.*
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

@State(Scope.Benchmark)
open class Iterate {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000, BM_10000000)
    var size: Int = 0

    private var persistentList: PersistentList<String> = persistentListOf()

    @Setup(Level.Trial)
    fun prepare() {
        persistentList = persistentListAdd(size)
    }

    /**
     * Iterates every element starting from first to last.
     *
     * Measures mean time and memory spent per `next` operation.
     *
     * Expected time: nearly constant
     * Expected memory: none once iterator created
     */
    @Benchmark
    fun firstToLast(bh: Blackhole) {
        for (e in persistentList) {
            bh.consume(e)
        }
    }

    /**
     * Iterates every element starting from last to first.
     *
     * Measures mean time and memory spent per `previous` operation.
     *
     * Expected time: nearly constant
     * Expected memory: none once iterator created
     */
    @Benchmark
    fun lastToFirst(bh: Blackhole) {
        val iterator = persistentList.listIterator(size)

        while (iterator.hasPrevious()) {
            bh.consume(iterator.previous())
        }
    }
}
