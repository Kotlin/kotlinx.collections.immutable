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

package benchmarks.immutableSet.clojure.builder

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

    @Param("0.0", "50.0")
    var immutablePercentage: Double = 0.0

    private var elements = listOf<IntWrapper>()

    @Setup(Level.Trial)
    fun prepare() {
        elements = generateElements(hashCodeType, size)
    }

    @Benchmark
    fun add(): clojure.lang.ATransientSet {
        return persistentSetBuilderAdd(elements, immutablePercentage)
    }

    @Benchmark
    fun addAndContains(bh: Blackhole) {
        val builder = persistentSetBuilderAdd(elements, immutablePercentage)
        repeat(times = size) { index ->
            bh.consume(builder.contains(elements[index]))
        }
    }


}


@State(Scope.Thread)
open class Contains {
    @Param("10000", "100000")
    var size: Int = 0

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE, NON_EXISTING_HASH_CODE)
    var hashCodeType = ""

    @Param("0.0", "50.0")
    var immutablePercentage: Double = 0.0

    private var elements = listOf<IntWrapper>()
    private var builder = clojure.lang.PersistentHashSet.EMPTY.asTransient() as clojure.lang.ATransientSet

    @Setup(Level.Trial)
    fun prepare() {
        elements = generateElements(hashCodeType, size)
        builder = persistentSetBuilderAdd(elements, immutablePercentage)

        if (hashCodeType == NON_EXISTING_HASH_CODE)
            elements = generateElements(hashCodeType, size)
    }

    @Benchmark
    fun contains(bh: Blackhole) {
        repeat(times = size) { index ->
            bh.consume(builder.contains(elements[index]))
        }
    }
}





@State(Scope.Thread)
open class Remove {
    @Param("10000", "100000")
    var size: Int = 0

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE, NON_EXISTING_HASH_CODE)
    var hashCodeType = ""

    @Param("0.0", "50.0")
    var immutablePercentage: Double = 0.0

    private var elements = listOf<IntWrapper>()
    private var elementsToRemove = listOf<IntWrapper>()

    @Setup(Level.Trial)
    fun prepare() {
        elements = generateElements(hashCodeType, size)

        elementsToRemove = if (hashCodeType == NON_EXISTING_HASH_CODE) {
            generateElements(hashCodeType, size)
        } else {
            elements
        }
    }

    @Benchmark
    fun addAndRemove(): clojure.lang.ATransientSet {
        val builder = persistentSetBuilderAdd(elements, immutablePercentage)
        repeat(times = size) { index ->
            builder.disjoin(elementsToRemove[index])
        }
        return builder
    }
}


private fun persistentSetBuilderAdd(
        elements: List<IntWrapper>,
        immutablePercentage: Double
): clojure.lang.ATransientSet {
    val immutableSize = immutableSize(elements.size, immutablePercentage)

    var set = clojure.lang.PersistentHashSet.EMPTY
    for (index in 0 until immutableSize) {
        set = set.cons(elements[index]) as clojure.lang.PersistentHashSet
    }

    val builder = set.asTransient() as clojure.lang.ATransientSet
    for (index in immutableSize until elements.size) {
        builder.conj(elements[index])
    }

    return builder
}
