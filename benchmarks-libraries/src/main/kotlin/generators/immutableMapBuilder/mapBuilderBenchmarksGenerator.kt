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

package generators.immutableMapBuilder

import generators.BenchmarkSourceGenerator
import generators.immutableMapBuilder.impl.MapBuilderImplementation
import generators.immutableMapBuilder.impl.mapBuilderKeyType
import java.io.PrintWriter

class MapBuilderBenchmarksGenerator(private val impl: MapBuilderImplementation) : BenchmarkSourceGenerator() {
    override val outputFileName: String = "MapBuilder"

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

    @Param("0.0", "50.0")
    var immutablePercentage: Double = 0.0

    private var keys = listOf<$mapBuilderKeyType>()
    private var builder = ${impl.empty()}

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
            bh.consume(${impl.getOperation("builder", "keys[index]")})
        }
    }
}


${
if (impl.isIterable) """
$header
open class Iterate {
    @Param("10000", "100000")
    var size: Int = 0

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE)
    var hashCodeType = ""

    @Param("0.0", "50.0")
    var immutablePercentage: Double = 0.0

    private var builder = ${impl.empty()}

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
}"""
else ""
}


$header
open class Put {
    @Param("10000", "100000")
    var size: Int = 0

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE)
    var hashCodeType = ""

    @Param("0.0", "50.0")
    var immutablePercentage: Double = 0.0

    private var keys = listOf<$mapBuilderKeyType>()

    @Setup(Level.Trial)
    fun prepare() {
        keys = generateKeys(hashCodeType, size)
    }

    @Benchmark
    fun put(): ${impl.type()} {
        return persistentMapBuilderPut(keys, immutablePercentage)
    }

    @Benchmark
    fun putAndGet(bh: Blackhole) {
        val builder = persistentMapBuilderPut(keys, immutablePercentage)
        repeat(times = size) { index ->
            bh.consume(${impl.getOperation("builder", "keys[index]")})
        }
    }

${
if (impl.isIterable) """
    @Benchmark
    fun putAndIterateKeys(bh: Blackhole) {
        val builder = persistentMapBuilderPut(keys, immutablePercentage)
        for (key in builder.keys) {
            bh.consume(key)
        }
    }"""
else ""
}
}


$header
open class Remove {
    @Param("10000", "100000")
    var size: Int = 0

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE, NON_EXISTING_HASH_CODE)
    var hashCodeType = ""

    @Param("0.0", "50.0")
    var immutablePercentage: Double = 0.0

    private var keys = listOf<$mapBuilderKeyType>()
    private var keysToRemove = listOf<$mapBuilderKeyType>()

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
    fun putAndRemove(): ${impl.type()} {
        val builder = persistentMapBuilderPut(keys, immutablePercentage)
        repeat(times = size) { index ->
            ${impl.removeOperation("builder", "keysToRemove[index]")}
        }
        return builder
    }
}


private fun persistentMapBuilderPut(
        keys: List<$mapBuilderKeyType>,
        immutablePercentage: Double
): ${impl.type()} {
    val immutableSize = immutableSize(keys.size, immutablePercentage)

    var map = ${impl.immutableEmpty()}
    for (index in 0 until immutableSize) {
        map = ${impl.immutablePutOperation("map", "keys[index]", "\"some element\"")}
    }

    val builder = ${impl.builderOperation("map")}
    for (index in immutableSize until keys.size) {
        ${impl.putOperation("builder", "keys[index]", "\"some element\"")}
    }

    return builder
}
        """.trimIndent()
        )
    }
}