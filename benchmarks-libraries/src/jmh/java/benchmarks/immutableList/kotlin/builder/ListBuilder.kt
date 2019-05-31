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

// Auto-generated file. DO NOT EDIT!

package benchmarks.immutableList.kotlin.builder

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.infra.Blackhole
import benchmarks.*

@State(Scope.Thread)
open class Add {
    @Param("10000", "100000")
    var size: Int = 0

    @Param("0.0", "50.0")
    var immutablePercentage: Double = 0.0

    @Benchmark
    fun addLast(): kotlinx.collections.immutable.PersistentList.Builder<String> {
        return persistentListBuilderAdd(size, immutablePercentage)
    }

    @Benchmark
    fun addLastAndGet(bh: Blackhole) {
        val builder = persistentListBuilderAdd(size, immutablePercentage)
        for (i in 0 until size) {
            bh.consume(builder.get(i))
        }
    }


    @Benchmark
    fun addLastAndIterate(bh: Blackhole) {
        val builder = persistentListBuilderAdd(size, immutablePercentage)
        for (e in builder) {
            bh.consume(e)
        }
    }
}


@State(Scope.Thread)
open class Get {
    @Param("10000", "100000")
    var size: Int = 0

    @Param("0.0", "50.0")
    var immutablePercentage: Double = 0.0

    private var builder: kotlinx.collections.immutable.PersistentList.Builder<String> = kotlinx.collections.immutable.persistentListOf<String>().builder()

    @Setup(Level.Trial)
    fun prepare() {
        builder = persistentListBuilderAdd(size, immutablePercentage)
    }

    @Benchmark
    fun getByIndex(bh: Blackhole) {
        for (i in 0 until size) {
            bh.consume(builder.get(i))
        }
    }
}



@State(Scope.Thread)
open class Iterate {
    @Param("10000", "100000")
    var size: Int = 0

    @Param("0.0", "50.0")
    var immutablePercentage: Double = 0.0

    private var builder = kotlinx.collections.immutable.persistentListOf<String>().builder()

    @Setup(Level.Trial)
    fun prepare() {
        builder = persistentListBuilderAdd(size, immutablePercentage)
    }

    @Benchmark
    fun firstToLast(bh: Blackhole) {
        for (e in builder) {
            bh.consume(e)
        }
    }

    @Benchmark
    fun lastToFirst(bh: Blackhole) {
        val iterator = builder.listIterator(size)

        while (iterator.hasPrevious()) {
            bh.consume(iterator.previous())
        }
    }
}


@State(Scope.Thread)
open class Remove {
    @Param("10000", "100000")
    var size: Int = 0

    @Param("0.0", "50.0")
    var immutablePercentage: Double = 0.0

    @Benchmark
    fun addAndRemoveLast(): kotlinx.collections.immutable.PersistentList.Builder<String> {
        val builder = persistentListBuilderAdd(size, immutablePercentage)
        for (i in 0 until size) {
            builder.removeAt(builder.size - 1)
        }
        return builder
    }
}


@State(Scope.Thread)
open class Set {
    @Param("10000", "100000")
    var size: Int = 0

    @Param("0.0", "50.0")
    var immutablePercentage: Double = 0.0

    private var builder = kotlinx.collections.immutable.persistentListOf<String>().builder()
    private var randomIndices = listOf<Int>()

    @Setup(Level.Trial)
    fun prepare() {
        builder = persistentListBuilderAdd(size, immutablePercentage)
        randomIndices = List(size) { it }.shuffled()
    }

    @Benchmark
    fun setByIndex(): kotlinx.collections.immutable.PersistentList.Builder<String> {
        for (i in 0 until size) {
            builder.set(i, "another element")
        }
        return builder
    }

    @Benchmark
    fun setByRandomIndex(): kotlinx.collections.immutable.PersistentList.Builder<String> {
        for (i in 0 until size) {
            builder.set(randomIndices[i], "another element")
        }
        return builder
    }
}


private fun persistentListBuilderAdd(size: Int, immutablePercentage: Double): kotlinx.collections.immutable.PersistentList.Builder<String> {
    val immutableSize = immutableSize(size, immutablePercentage)

    var list = kotlinx.collections.immutable.persistentListOf<String>()
    repeat(times = immutableSize) {
        list = list.add("another element")
    }

    val builder = list.builder()
    repeat(times = size - immutableSize) {
        builder.add("some element")
    }

    return builder
}
