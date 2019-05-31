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

package benchmarks.immutableMap.capsule.builder

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.infra.Blackhole
import benchmarks.*

@State(Scope.Thread)
open class Get {
    @Param("10000", "100000")
    var size: Int = 0

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE, NON_EXISTING_HASH_CODE)
    var hashCodeType = ""

    @Param("0.0", "50.0")
    var immutablePercentage: Double = 0.0

    private var keys = listOf<IntWrapper>()
    private var builder = io.usethesource.capsule.core.PersistentTrieMap.of<IntWrapper, String>().asTransient()

    @Setup(Level.Trial)
    fun prepare() {
        keys = generateKeys(hashCodeType, size)
        builder = persistentMapBuilderPut(keys, immutablePercentage)

        if (hashCodeType == NON_EXISTING_HASH_CODE)
            keys = generateKeys(hashCodeType, size)
    }

    @Benchmark
    fun get(bh: Blackhole) {
        repeat(times = size) { index ->
            bh.consume(builder.get(keys[index]))
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

    private var builder = io.usethesource.capsule.core.PersistentTrieMap.of<IntWrapper, String>().asTransient()

    @Setup(Level.Trial)
    fun prepare() {
        val keys = generateKeys(hashCodeType, size)
        builder = persistentMapBuilderPut(keys, immutablePercentage)
    }

    @Benchmark
    fun iterateKeys(bh: Blackhole) {
        for (k in builder.keys) {
            bh.consume(k)
        }
    }

    @Benchmark
    fun iterateValues(bh: Blackhole) {
        for (v in builder.values) {
            bh.consume(v)
        }
    }

    @Benchmark
    fun iterateEntries(bh: Blackhole) {
        for (e in builder) {
            bh.consume(e)
        }
    }
}


@State(Scope.Thread)
open class Put {
    @Param("10000", "100000")
    var size: Int = 0

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE)
    var hashCodeType = ""

    @Param("0.0", "50.0")
    var immutablePercentage: Double = 0.0

    private var keys = listOf<IntWrapper>()

    @Setup(Level.Trial)
    fun prepare() {
        keys = generateKeys(hashCodeType, size)
    }

    @Benchmark
    fun put(): io.usethesource.capsule.Map.Transient<IntWrapper, String> {
        return persistentMapBuilderPut(keys, immutablePercentage)
    }

    @Benchmark
    fun putAndGet(bh: Blackhole) {
        val builder = persistentMapBuilderPut(keys, immutablePercentage)
        repeat(times = size) { index ->
            bh.consume(builder.get(keys[index]))
        }
    }


    @Benchmark
    fun putAndIterateKeys(bh: Blackhole) {
        val builder = persistentMapBuilderPut(keys, immutablePercentage)
        for (key in builder.keys) {
            bh.consume(key)
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

    private var keys = listOf<IntWrapper>()
    private var keysToRemove = listOf<IntWrapper>()

    @Setup(Level.Trial)
    fun prepare() {
        keys = generateKeys(hashCodeType, size)

        keysToRemove = if (hashCodeType == NON_EXISTING_HASH_CODE) {
            generateKeys(hashCodeType, size)
        } else {
            keys
        }
    }

    @Benchmark
    fun putAndRemove(): io.usethesource.capsule.Map.Transient<IntWrapper, String> {
        val builder = persistentMapBuilderPut(keys, immutablePercentage)
        repeat(times = size) { index ->
            builder.remove(keysToRemove[index])
        }
        return builder
    }
}


private fun persistentMapBuilderPut(
        keys: List<IntWrapper>,
        immutablePercentage: Double
): io.usethesource.capsule.Map.Transient<IntWrapper, String> {
    val immutableSize = immutableSize(keys.size, immutablePercentage)

    var map = io.usethesource.capsule.core.PersistentTrieMap.of<IntWrapper, String>()
    for (index in 0 until immutableSize) {
        map = map.__put(keys[index], "some element")
    }

    val builder = map.asTransient()
    for (index in immutableSize until keys.size) {
        builder.put(keys[index], "some element")
    }

    return builder
}
