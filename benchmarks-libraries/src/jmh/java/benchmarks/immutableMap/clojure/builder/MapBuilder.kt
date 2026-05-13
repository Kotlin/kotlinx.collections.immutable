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

package benchmarks.immutableMap.clojure.builder

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
    private var builder = clojure.lang.PersistentHashMap.EMPTY.asTransient() as clojure.lang.ATransientMap

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
            bh.consume(builder.valAt(keys[index]))
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
    fun put(): clojure.lang.ATransientMap {
        return persistentMapBuilderPut(keys, immutablePercentage)
    }

    @Benchmark
    fun putAndGet(bh: Blackhole) {
        val builder = persistentMapBuilderPut(keys, immutablePercentage)
        repeat(times = size) { index ->
            bh.consume(builder.valAt(keys[index]))
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
    fun putAndRemove(): clojure.lang.ATransientMap {
        val builder = persistentMapBuilderPut(keys, immutablePercentage)
        repeat(times = size) { index ->
            builder.without(keysToRemove[index])
        }
        return builder
    }
}


private fun persistentMapBuilderPut(
        keys: List<IntWrapper>,
        immutablePercentage: Double
): clojure.lang.ATransientMap {
    val immutableSize = immutableSize(keys.size, immutablePercentage)

    var map = clojure.lang.PersistentHashMap.EMPTY as clojure.lang.IPersistentMap
    for (index in 0 until immutableSize) {
        map = map.assoc(keys[index], "some element")
    }

    val builder = (map as clojure.lang.PersistentHashMap).asTransient() as clojure.lang.ATransientMap
    for (index in immutableSize until keys.size) {
        builder.assoc(keys[index], "some element")
    }

    return builder
}
