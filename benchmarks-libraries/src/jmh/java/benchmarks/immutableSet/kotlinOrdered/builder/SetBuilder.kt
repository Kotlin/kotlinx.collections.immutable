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

package benchmarks.immutableSet.kotlinOrdered.builder

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
    fun add(): kotlinx.collections.immutable.PersistentSet.Builder<IntWrapper> {
        return persistentSetBuilderAdd(elements, immutablePercentage)
    }

    @Benchmark
    fun addAndContains(bh: Blackhole) {
        val builder = persistentSetBuilderAdd(elements, immutablePercentage)
        repeat(times = size) { index ->
            bh.consume(builder.contains(elements[index]))
        }
    }


    @Benchmark
    fun addAndIterate(bh: Blackhole) {
        val set = persistentSetBuilderAdd(elements, immutablePercentage)
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

    @Param("0.0", "50.0")
    var immutablePercentage: Double = 0.0

    private var elements = listOf<IntWrapper>()
    private var builder = kotlinx.collections.immutable.persistentSetOf<IntWrapper>().builder()

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
open class Iterate {
    @Param("10000", "100000")
    var size: Int = 0

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE)
    var hashCodeType = ""

    @Param("0.0", "50.0")
    var immutablePercentage: Double = 0.0

    private var builder = kotlinx.collections.immutable.persistentSetOf<IntWrapper>().builder()

    @Setup(Level.Trial)
    fun prepare() {
        val elements = generateElements(hashCodeType, size)
        builder = persistentSetBuilderAdd(elements, immutablePercentage)
    }

    @Benchmark
    fun iterate(bh: Blackhole) {
        for (e in builder) {
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
    fun addAndRemove(): kotlinx.collections.immutable.PersistentSet.Builder<IntWrapper> {
        val builder = persistentSetBuilderAdd(elements, immutablePercentage)
        repeat(times = size) { index ->
            builder.remove(elementsToRemove[index])
        }
        return builder
    }
}


private fun persistentSetBuilderAdd(
        elements: List<IntWrapper>,
        immutablePercentage: Double
): kotlinx.collections.immutable.PersistentSet.Builder<IntWrapper> {
    val immutableSize = immutableSize(elements.size, immutablePercentage)

    var set = kotlinx.collections.immutable.persistentSetOf<IntWrapper>()
    for (index in 0 until immutableSize) {
        set = set.add(elements[index])
    }

    val builder = set.builder()
    for (index in immutableSize until elements.size) {
        builder.add(elements[index])
    }

    return builder
}
