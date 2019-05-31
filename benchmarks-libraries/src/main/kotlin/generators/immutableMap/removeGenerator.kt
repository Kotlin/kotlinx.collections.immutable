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
import java.io.PrintWriter

interface MapRemoveBenchmark {
    val packageName: String
    fun emptyOf(K: String, V: String): String
    fun mapType(K: String, V: String): String
    val removeOperation: String
}

class MapRemoveBenchmarkGenerator(private val impl: MapRemoveBenchmark) : BenchmarkSourceGenerator() {
    override val benchmarkName: String = "Remove"

    override fun getPackage(): String {
        return super.getPackage() + ".immutableMap." + impl.packageName
    }

    override val imports: Set<String> = super.imports + "benchmarks.*"
    override fun generateBody(out: PrintWriter) {
        out.println("""
open class Remove {
    @Param("10000", "100000")
    var size: Int = 0

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE, NON_EXISTING_HASH_CODE)
    var hashCodeType = ""

    private var keys = listOf<IntWrapper>()
    private var persistentMap = ${impl.emptyOf("IntWrapper", "String")}

    @Setup(Level.Trial)
    fun prepare() {
        keys = generateKeys(hashCodeType, size)
        persistentMap = persistentMapPut(keys)

        if (hashCodeType == NON_EXISTING_HASH_CODE)
            keys = generateKeys(hashCodeType, size)
    }

    @Benchmark
    fun remove(): ${impl.mapType("IntWrapper", "String")} {
        var map = persistentMap
        repeat(times = size) { index ->
            map = map.${impl.removeOperation}(keys[index])
        }
        return map
    }
}
        """.trimIndent()
        )
    }
}