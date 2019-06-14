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

import generators.UtilsSourceGenerator
import java.io.PrintWriter

class MapBuilderUtilsGenerator(private val impl: MapBuilderImplementation): UtilsSourceGenerator() {
    override val outputFileName: String = "utils"

    override fun getPackage(): String = super.getPackage() + ".immutableMap." + impl.packageName

    override val imports: Set<String> = super.imports + "benchmarks.*"

    override fun generateBody(out: PrintWriter) {
        out.println("""
fun persistentMapBuilderPut(
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