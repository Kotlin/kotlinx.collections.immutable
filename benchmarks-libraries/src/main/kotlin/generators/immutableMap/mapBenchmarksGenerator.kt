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

package generators.immutableMap

import generators.BenchmarkSourceGenerator
import generators.immutableMap.impl.MapImplementation
import generators.immutableMap.impl.mapKeyType
import java.io.PrintWriter

class MapBenchmarksGenerator(private val impl: MapImplementation) : BenchmarkSourceGenerator() {
    override val outputFileName: String = "Map"

    override fun getPackage(): String {
        return super.getPackage() + ".immutableMap." + impl.packageName
    }

    override fun generateBenchmark(out: PrintWriter, header: String) {
        out.println("""
$header
open class Get {
    @Param("10000", "100000")
    var size: Int = 0

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE, NON_EXISTING_HASH_CODE)
    var hashCodeType = ""

    private var keys = listOf<$mapKeyType>()
    private var persistentMap = ${impl.empty()}

    @Setup(Level.Trial)
    fun prepare() {
        keys = generateKeys(hashCodeType, size)
        persistentMap = persistentMapPut(keys)

        if (hashCodeType == NON_EXISTING_HASH_CODE)
            keys = generateKeys(hashCodeType, size)

    }

    @Benchmark
    fun get(bh: Blackhole) {
        repeat(times = size) { index ->
            bh.consume(${impl.getOperation("persistentMap", "keys[index]")})
        }
    }
}


$header
open class Iterate {
    @Param("10000", "100000")
    var size: Int = 0

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE)
    var hashCodeType = ""

    private var persistentMap = ${impl.empty()}

    @Setup(Level.Trial)
    fun prepare() {
        persistentMap = persistentMapPut(generateKeys(hashCodeType, size))
    }

    @Benchmark
    fun iterateKeys(bh: Blackhole) {
        for (k in ${impl.keysOperation("persistentMap")}) {
            bh.consume(k)
        }
    }

    @Benchmark
    fun iterateValues(bh: Blackhole) {
        for (v in ${impl.valuesOperation("persistentMap")}) {
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


$header
open class Put {
    @Param("10000", "100000")
    var size: Int = 0

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE)
    var hashCodeType = ""

    private var keys = listOf<$mapKeyType>()

    @Setup(Level.Trial)
    fun prepare() {
        keys = generateKeys(hashCodeType, size)
    }

    @Benchmark
    fun put(): ${impl.type()} {
        return persistentMapPut(keys)
    }

    @Benchmark
    fun putAndGet(bh: Blackhole) {
        val map = persistentMapPut(keys)
        repeat(times = size) { index ->
            bh.consume(${impl.getOperation("map", "keys[index]")})
        }
    }

    @Benchmark
    fun putAndIterateKeys(bh: Blackhole) {
        val map = persistentMapPut(keys)
        for (key in ${impl.keysOperation("map")}) {
            bh.consume(key)
        }
    }
}


$header
open class Remove {
    @Param("10000", "100000")
    var size: Int = 0

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE, NON_EXISTING_HASH_CODE)
    var hashCodeType = ""

    private var keys = listOf<$mapKeyType>()
    private var persistentMap = ${impl.empty()}

    @Setup(Level.Trial)
    fun prepare() {
        keys = generateKeys(hashCodeType, size)
        persistentMap = persistentMapPut(keys)

        if (hashCodeType == NON_EXISTING_HASH_CODE)
            keys = generateKeys(hashCodeType, size)
    }

    @Benchmark
    fun remove(): ${impl.type()} {
        var map = persistentMap
        repeat(times = size) { index ->
            map = ${impl.removeOperation("map", "keys[index]")}
        }
        return map
    }
}


private fun persistentMapPut(keys: List<$mapKeyType>): ${impl.type()} {
    var map = ${impl.empty()}
    for (key in keys) {
        map = ${impl.putOperation("map", "key", "\"some element\"")}
    }
    return map
}
        """.trimIndent()
        )
    }
}