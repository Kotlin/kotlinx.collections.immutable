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