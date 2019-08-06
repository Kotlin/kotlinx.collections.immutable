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
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.openjdk.jmh.annotations.*

@State(Scope.Thread)
open class RemoveAll {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000, BM_10000000)
    var size: Int = 0

    private var persistentList: PersistentList<Int> = persistentListOf()

    @Setup(Level.Trial)
    fun prepare() {
        persistentList = persistentListOf<Int>().addAll(List(size) { it })
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
        val random = java.util.Random()
        return List(count) { random.nextInt(size) }
    }

    private fun tailSize(): Int {
        val bufferSize = 32
        return (size and (bufferSize - 1)).let { if (it == 0) bufferSize else it }
    }
}