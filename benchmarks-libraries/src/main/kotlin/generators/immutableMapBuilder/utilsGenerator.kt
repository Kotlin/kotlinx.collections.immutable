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

import generators.BenchmarkUtilsGenerator
import java.io.PrintWriter

interface MapBuilderBenchmarkUtils {
    val packageName: String
    fun mapBuilderType(K: String, V: String): String
    fun immutableOf(K: String, V: String): String
    val putOperation: String
    val immutablePutOperation: String
    val builderOperation: String
}

class MapBuilderUtilsGenerator(private val impl: MapBuilderBenchmarkUtils): BenchmarkUtilsGenerator() {
    override fun getPackage(): String = super.getPackage() + ".immutableMap." + impl.packageName

    override val outputFileName: String = "utils"

    override val imports: Set<String> = super.imports + "benchmarks.*"

    override fun generateBody(out: PrintWriter) {
        out.println("""
fun persistentMapBuilderPut(
        keys: List<IntWrapper>,
        immutablePercentage: Double
): ${impl.mapBuilderType("IntWrapper", "String")} {
    val immutableSize = immutableSize(keys.size, immutablePercentage)

    var map = ${impl.immutableOf("IntWrapper", "String")}
    for (index in 0 until immutableSize) {
        map = map.${impl.immutablePutOperation}(keys[index], "some element")
    }

    val builder = map.${impl.builderOperation}()
    for (index in immutableSize until keys.size) {
        builder.${impl.putOperation}(keys[index], "some element")
    }

    return builder
}
        """.trimIndent()
        )
    }
}