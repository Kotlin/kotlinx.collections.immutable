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

package benchmarks.immutableMap.cyclopsOrdered

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.infra.Blackhole
import benchmarks.*

@Fork(1)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
open class Iterate {
    @Param("10000", "100000")
    var size: Int = 0

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE)
    var hashCodeType = ""

    private var persistentMap = cyclops.data.LinkedMap.empty<IntWrapper, String>()

    @Setup(Level.Trial)
    fun prepare() {
        persistentMap = persistentMapPut(generateKeys(hashCodeType, size))
    }

    @Benchmark
    fun iterateKeys(bh: Blackhole) {
        for (k in persistentMap.keys) {
            bh.consume(k)
        }
    }

    @Benchmark
    fun iterateValues(bh: Blackhole) {
        for (v in persistentMap.values) {
            bh.consume(v)
        }
    }

    @Benchmark
    fun iterateEntries(bh: Blackhole) {
        for (e in persistentMap) {
            bh.consume(e)
        }
    }
}
