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
import generators.immutableMap.MapIterateBenchmark
import java.io.PrintWriter

interface MapBuilderPutBenchmark {
    val packageName: String
    fun mapBuilderType(K: String, V: String): String
    val getOperation: String
}

class MapBuilderPutBenchmarkGenerator(private val impl: MapBuilderPutBenchmark) : BenchmarkSourceGenerator() {
    override val outputFileName: String = "Put"

    override fun getPackage(): String {
        return super.getPackage() + ".immutableMap." + impl.packageName
    }

    override val imports: Set<String> = super.imports + "org.openjdk.jmh.infra.Blackhole" + "benchmarks.*"

    override fun generateBody(out: PrintWriter) {
        out.println("""
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
    fun put(): ${impl.mapBuilderType("IntWrapper", "String")} {
        return persistentMapBuilderPut(keys, immutablePercentage)
    }

    @Benchmark
    fun putAndGet(bh: Blackhole) {
        val builder = persistentMapBuilderPut(keys, immutablePercentage)
        repeat(times = size) { index ->
            bh.consume(builder.${impl.getOperation}(keys[index]))
        }
    }
        """.trimIndent()
        )
        if (impl is MapBuilderIterateBenchmark) {
            out.println("""
    @Benchmark
    fun putAndIterateKeys(bh: Blackhole) {
        val builder = persistentMapBuilderPut(keys, immutablePercentage)
        for (key in builder.keys) {
            bh.consume(key)
        }
    }
            """
            )
        }

        out.println("}")
    }
}