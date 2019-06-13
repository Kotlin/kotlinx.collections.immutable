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

package generators.immutableSetBuilder

import generators.BenchmarkUtilsGenerator
import java.io.PrintWriter

interface SetBuilderBenchmarkUtils {
    val packageName: String
    fun emptyOf(E: String): String
    fun immutableOf(E: String): String
    fun setBuilderType(E: String): String
    val addOperation: String
    fun immutableAddOperation(set: String, element: String): String
    fun builderOperation(set: String): String
}

class SetBuilderUtilsGenerator(private val impl: SetBuilderBenchmarkUtils): BenchmarkUtilsGenerator() {
    override fun getPackage(): String = super.getPackage() + ".immutableSet." + impl.packageName

    override val outputFileName: String = "utils"

    override val imports: Set<String> = super.imports + "benchmarks.immutableSize"

    override fun generateBody(out: PrintWriter) {
        out.println("""
fun <E> persistentSetBuilderAdd(
        elements: List<E>,
        immutablePercentage: Double
): ${impl.setBuilderType("E")} {
    val immutableSize = immutableSize(elements.size, immutablePercentage)

    var set = ${impl.immutableOf("E")}
    for (index in 0 until immutableSize) {
        set = ${impl.immutableAddOperation("set", "elements[index]")}
    }

    val builder = ${impl.builderOperation("set")}
    for (index in immutableSize until elements.size) {
        builder.${impl.addOperation}(elements[index])
    }

    return builder
}
        """.trimIndent()
        )
    }
}