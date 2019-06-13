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

package generators.immutableListBuilder

import generators.BenchmarkUtilsGenerator
import java.io.PrintWriter

interface ListBuilderBenchmarkUtils {
    val packageName: String
    fun listBuilderType(T: String): String
    fun immutableOf(T: String): String
    val addOperation: String
    val immutableAddOperation: String
    fun builderOperation(list: String): String
}

class ListBuilderUtilsGenerator(private val impl: ListBuilderBenchmarkUtils): BenchmarkUtilsGenerator() {
    override fun getPackage(): String = super.getPackage() + ".immutableList." + impl.packageName

    override val outputFileName: String = "utils"

    override val imports: Set<String> = super.imports + "benchmarks.immutableSize"

    override fun generateBody(out: PrintWriter) {
        out.println("""
fun persistentListBuilderAdd(size: Int, immutablePercentage: Double): ${impl.listBuilderType("String")} {
    val immutableSize = immutableSize(size, immutablePercentage)

    var list = ${impl.immutableOf("String")}
    repeat(times = immutableSize) {
        list = list.${impl.immutableAddOperation}("some element")
    }

    val builder = ${impl.builderOperation("list")}
    repeat(times = size - immutableSize) {
        builder.${impl.addOperation}("some element")
    }

    return builder
}
        """.trimIndent()
        )
    }
}