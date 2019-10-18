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
open class Get {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000, BM_10000000)
    var size: Int = 0

    private var persistentList: PersistentList<String> = persistentListOf()

    @Setup(Level.Trial)
    fun prepare() {
        persistentList = persistentListAdd(size)
    }

    /**
     * Gets every element by index starting from first to last.
     *
     * Measures mean time and memory spent per `get` operation.
     *
     * Expected time: logarithmic
     * Expected memory: none
     */
    @Benchmark
    fun getByIndex(bh: Blackhole) {
        for (i in 0 until persistentList.size) {
            bh.consume(persistentList[i])
        }
    }
}