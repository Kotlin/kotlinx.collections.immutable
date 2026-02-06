/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableList

import benchmarks.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.benchmark.*

@State(Scope.Benchmark)
open class Remove {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000, BM_10000000)
    var size: Int = 0

    private var persistentList: PersistentList<String> = persistentListOf()

    @Setup
    fun prepare() {
        persistentList = persistentListAdd(size)
    }

    @Benchmark
    fun removeLast(): ImmutableList<String> {
        var list = persistentList
        repeat(times = size) {
            list = list.removingAt(list.size - 1)
        }
        return list
    }

    /**
     * Removes one element from the beginning.
     *
     * Measures (time and memory spent on `removeAt` operation) / size.
     *
     * Expected time: nearly constant.
     * Expected memory: nearly constant.
     */
    @Benchmark
    fun removeFirst(): ImmutableList<String> {
        val list = persistentList
        return list.removingAt(0)
    }

    /**
     * Removes one element from the middle.
     *
     * Measures (time and memory spent on `removeAt` operation) / size.
     *
     * Expected time: nearly constant.
     * Expected memory: nearly constant.
     */
    @Benchmark
    fun removeMiddle(): ImmutableList<String> {
        val list = persistentList
        return list.removingAt(size / 2)
    }
}
