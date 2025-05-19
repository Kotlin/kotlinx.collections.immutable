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

package benchmarks.immutableList.cyclops

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.infra.Blackhole
import benchmarks.*

@State(Scope.Thread)
open class Add {
    @Param("10000", "100000")
    var size: Int = 0

    @Benchmark
    fun addLast(): cyclops.data.Vector<String> {
        return persistentListAdd(size)
    }

    @Benchmark
    fun addLastAndIterate(bh: Blackhole) {
        val list = persistentListAdd(size)
        for (e in list) {
            bh.consume(e)
        }
    }

    @Benchmark
    fun addLastAndGet(bh: Blackhole) {
        val list = persistentListAdd(size)
        for (i in 0 until size) {
            bh.consume(list.get(i))
        }
    }
}


@State(Scope.Thread)
open class Get {
    @Param("10000", "100000")
    var size: Int = 0

    private var persistentList = cyclops.data.Vector.empty<String>()

    @Setup(Level.Trial)
    fun prepare() {
        persistentList = persistentListAdd(size)
    }

    @Benchmark
    fun getByIndex(bh: Blackhole) {
        for (i in 0 until size) {
            bh.consume(persistentList.get(i))
        }
    }
}


@State(Scope.Thread)
open class Iterate {
    @Param("10000", "100000")
    var size: Int = 0

    private var persistentList = cyclops.data.Vector.empty<String>()

    @Setup(Level.Trial)
    fun prepare() {
        persistentList = persistentListAdd(size)
    }

    @Benchmark
    fun firstToLast(bh: Blackhole) {
        for (e in persistentList) {
            bh.consume(e)
        }
    }

    @Benchmark
    fun lastToFirst(bh: Blackhole) {
        val iterator = persistentList.reverse().iterator()

        while (iterator.hasNext()) {
            bh.consume(iterator.next())
        }
    }
}


@State(Scope.Thread)
open class Remove {
    @Param("10000", "100000")
    var size: Int = 0

    private var persistentList = cyclops.data.Vector.empty<String>()

    @Setup(Level.Trial)
    fun prepare() {
        persistentList = persistentListAdd(size)
    }

    @Benchmark
    fun removeLast(): cyclops.data.Vector<String> {
        var list = persistentList
        repeat(times = size) {
            list = list.dropRight(1)
        }
        return list
    }
}


@State(Scope.Thread)
open class Set {
    @Param("10000", "100000")
    var size: Int = 0

    private var persistentList = cyclops.data.Vector.empty<String>()
    private var randomIndices = listOf<Int>()

    @Setup(Level.Trial)
    fun prepare() {
        persistentList = persistentListAdd(size)
        randomIndices = List(size) { it }.shuffled()
    }

    @Benchmark
    fun setByIndex(): cyclops.data.Vector<String> {
        repeat(times = size) { index ->
            persistentList = persistentList.updateAt(index, "another element")
        }
        return persistentList
    }

    @Benchmark
    fun setByRandomIndex(): cyclops.data.Vector<String> {
        repeat(times = size) { index ->
            persistentList = persistentList.updateAt(randomIndices[index], "another element")
        }
        return persistentList
    }
}


private fun persistentListAdd(size: Int): cyclops.data.Vector<String> {
    var list = cyclops.data.Vector.empty<String>()
    repeat(times = size) {
        list = list.plus("some element")
    }
    return list
}
