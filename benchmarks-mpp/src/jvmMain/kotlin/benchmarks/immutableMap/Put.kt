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

package benchmarks.immutableMap

import benchmarks.*
import kotlinx.collections.immutable.PersistentMap
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

@State(Scope.Thread)
open class Put {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000)
    var size: Int = 0

    @Param(HASH_IMPL, ORDERED_IMPL)
    var implementation = ""

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE)
    var hashCodeType = ""

    private var keys = listOf<IntWrapper>()

    @Setup(Level.Trial)
    fun prepare() {
        keys = generateKeys(hashCodeType, size)
    }

    /**
     * Adds [size] entries to an empty persistent map.
     *
     * Expected time: logarithmic
     * Expected memory: logarithmic
     */
    @Benchmark
    fun put(): PersistentMap<IntWrapper, String> {
        return persistentMapPut(implementation, keys)
    }

    /**
     * Adds [size] entries to an empty persistent map and then gets every value by key.
     *
     * Expected time: [put] + [Get.get]
     * Expected memory: [put] + [Get.get]
     */
    @Benchmark
    fun putAndGet(bh: Blackhole) {
        val map = persistentMapPut(implementation, keys)
        repeat(times = size) { index ->
            bh.consume(map[keys[index]])
        }
    }

    /**
     * Adds [size] entries to an empty persistent map and then iterates all keys.
     *
     * Expected time: [put] + [Iterate.iterateKeys]
     * Expected memory: [put] + [Iterate.iterateKeys]
     */
    @Benchmark
    fun putAndIterateKeys(bh: Blackhole) {
        val map = persistentMapPut(implementation, keys)
        for (key in map.keys) {
            bh.consume(key)
        }
    }
}