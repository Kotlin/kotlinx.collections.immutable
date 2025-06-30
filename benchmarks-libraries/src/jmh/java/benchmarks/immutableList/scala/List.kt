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

package benchmarks.immutableList.scala

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.infra.Blackhole
import benchmarks.*

@State(Scope.Thread)
open class Add {
    @Param("10000", "100000")
    var size: Int = 0

    @Benchmark
    fun addLast(): scala.collection.immutable.Vector<String> {
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
            bh.consume(list.apply(i))
        }
    }
}


@State(Scope.Thread)
open class Get {
    @Param("10000", "100000")
    var size: Int = 0

    private var persistentList = scala.collection.immutable.`Vector$`.`MODULE$`.empty<String>()

    @Setup(Level.Trial)
    fun prepare() {
        persistentList = persistentListAdd(size)
    }

    @Benchmark
    fun getByIndex(bh: Blackhole) {
        for (i in 0 until size) {
            bh.consume(persistentList.apply(i))
        }
    }
}


@State(Scope.Thread)
open class Iterate {
    @Param("10000", "100000")
    var size: Int = 0

    private var persistentList = scala.collection.immutable.`Vector$`.`MODULE$`.empty<String>()

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
        val iterator = persistentList.reverseIterator()

        while (iterator.hasNext()) {
            bh.consume(iterator.next())
        }
    }
}


@State(Scope.Thread)
open class Remove {
    @Param("10000", "100000")
    var size: Int = 0

    private var persistentList = scala.collection.immutable.`Vector$`.`MODULE$`.empty<String>()

    @Setup(Level.Trial)
    fun prepare() {
        persistentList = persistentListAdd(size)
    }

    @Benchmark
    fun removeLast(): scala.collection.immutable.Vector<String> {
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

    private var persistentList = scala.collection.immutable.`Vector$`.`MODULE$`.empty<String>()
    private var randomIndices = listOf<Int>()

    @Setup(Level.Trial)
    fun prepare() {
        persistentList = persistentListAdd(size)
        randomIndices = List(size) { it }.shuffled()
    }

    @Benchmark
    fun setByIndex(): scala.collection.immutable.Vector<String> {
        repeat(times = size) { index ->
            persistentList = persistentList.updateAt(index, "another element")
        }
        return persistentList
    }

    @Benchmark
    fun setByRandomIndex(): scala.collection.immutable.Vector<String> {
        repeat(times = size) { index ->
            persistentList = persistentList.updateAt(randomIndices[index], "another element")
        }
        return persistentList
    }
}


private fun persistentListAdd(size: Int): scala.collection.immutable.Vector<String> {
    var list = scala.collection.immutable.`Vector$`.`MODULE$`.empty<String>()
    repeat(times = size) {
        list = list.appended("some element") as scala.collection.immutable.Vector<String>
    }
    return list
}
