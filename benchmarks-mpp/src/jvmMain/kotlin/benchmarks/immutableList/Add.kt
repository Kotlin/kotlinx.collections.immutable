/*
 * Copyright 2016-2019 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package benchmarks.immutableList

import benchmarks.*
import kotlinx.collections.immutable.ImmutableList
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

@State(Scope.Thread)
open class Add {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000, BM_10000000)
    var size: Int = 0

    /**
     * Adds [size] elements to an empty persistent list.
     *
     * Expected time: nearly constant.
     * Expected memory: for size in 1..32 - O(size), nearly constant otherwise.
     */
    @Benchmark
    fun addLast(): ImmutableList<String> {
        return persistentListAdd(size)
    }

    /**
     * Adds [size] elements to an empty persistent list and then iterates all elements from first to last.
     *
     * Expected time: [addLast] + [Iterate.firstToLast]
     * Expected memory: [addLast] + [Iterate.firstToLast]
     */
    @Benchmark
    fun addLastAndIterate(bh: Blackhole) {
        val list = persistentListAdd(size)
        for (e in list) {
            bh.consume(e)
        }
    }

    /**
     * Adds [size] elements to an empty persistent list and then gets all elements by index from first to last.
     *
     * Expected time: [addLast] + [Get.getByIndex]
     * Expected memory: [addLast] + [Get.getByIndex]
     */
    @Benchmark
    fun addLastAndGet(bh: Blackhole) {
        val list = persistentListAdd(size)
        for (i in 0 until list.size) {
            bh.consume(list[i])
        }
    }
}