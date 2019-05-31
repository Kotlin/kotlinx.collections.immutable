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

package benchmarks.immutableSet.clojureSorted

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.infra.Blackhole
import benchmarks.*

@State(Scope.Thread)
open class Add {
    @Param("10000", "100000")
    var size: Int = 0

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE)
    var hashCodeType = ""

    private var elements = listOf<IntWrapper>()

    @Setup(Level.Trial)
    fun prepare() {
        elements = generateElements(hashCodeType, size)
    }

    @Benchmark
    fun add(): clojure.lang.PersistentTreeSet {
        return persistentSetAdd(elements)
    }

    @Benchmark
    fun addAndContains(bh: Blackhole) {
        val set = persistentSetAdd(elements)
        repeat(times = size) { index ->
            bh.consume(set.contains(elements[index]))
        }
    }

    @Benchmark
    fun addAndIterate(bh: Blackhole) {
        val set = persistentSetAdd(elements)
        for (element in set) {
            bh.consume(element)
        }
    }
}


@State(Scope.Thread)
open class Contains {
    @Param("10000", "100000")
    var size: Int = 0

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE, NON_EXISTING_HASH_CODE)
    var hashCodeType = ""

    private var elements = listOf<IntWrapper>()
    private var persistentSet = clojure.lang.PersistentTreeSet.EMPTY

    @Setup(Level.Trial)
    fun prepare() {
        elements = generateElements(hashCodeType, size)
        persistentSet = persistentSetAdd(elements)

        if (hashCodeType == NON_EXISTING_HASH_CODE)
            elements = generateElements(hashCodeType, size)
    }

    @Benchmark
    fun contains(bh: Blackhole) {
        repeat(times = size) { index ->
            bh.consume(persistentSet.contains(elements[index]))
        }
    }
}


@State(Scope.Thread)
open class Iterate {
    @Param("10000", "100000")
    var size: Int = 0

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE)
    var hashCodeType = ""

    private var persistentSet = clojure.lang.PersistentTreeSet.EMPTY

    @Setup(Level.Trial)
    fun prepare() {
        persistentSet = persistentSetAdd(generateElements(hashCodeType, size))
    }

    @Benchmark
    fun iterate(bh: Blackhole) {
        for (e in persistentSet) {
            bh.consume(e)
        }
    }
}


@State(Scope.Thread)
open class Remove {
    @Param("10000", "100000")
    var size: Int = 0

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE, NON_EXISTING_HASH_CODE)
    var hashCodeType = ""

    private var elements = listOf<IntWrapper>()
    private var persistentSet = clojure.lang.PersistentTreeSet.EMPTY

    @Setup(Level.Trial)
    fun prepare() {
        elements = generateElements(hashCodeType, size)
        persistentSet = persistentSetAdd(elements)

        if (hashCodeType == NON_EXISTING_HASH_CODE)
            elements = generateElements(hashCodeType, size)
    }

    @Benchmark
    fun remove(): clojure.lang.PersistentTreeSet {
        var set = persistentSet
        repeat(times = size) { index ->
            set = set.disjoin(elements[index]) as clojure.lang.PersistentTreeSet
        }
        return set
    }
}


private fun persistentSetAdd(elements: List<IntWrapper>): clojure.lang.PersistentTreeSet {
    var set = clojure.lang.PersistentTreeSet.EMPTY
    for (element in elements) {
        set = set.cons(element) as clojure.lang.PersistentTreeSet
    }
    return set
}
