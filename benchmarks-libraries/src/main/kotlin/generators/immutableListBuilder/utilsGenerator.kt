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

import generators.UtilsSourceGenerator
import java.io.PrintWriter

class ListBuilderUtilsGenerator(private val impl: ListBuilderImplementation): UtilsSourceGenerator() {
    override fun getPackage(): String = super.getPackage() + ".immutableList." + impl.packageName

    override val outputFileName: String = "utils"

    override val imports: Set<String> = super.imports + "benchmarks.immutableSize"

    override fun generateBody(out: PrintWriter) {
        out.println("""
fun persistentListBuilderAdd(size: Int, immutablePercentage: Double): ${impl.type()} {
    val immutableSize = immutableSize(size, immutablePercentage)

    var list = ${impl.immutableEmpty()}
    repeat(times = immutableSize) {
        list = ${impl.immutableAddOperation("list", listBuilderNewElement)}
    }

    val builder = ${impl.builderOperation("list")}
    repeat(times = size - immutableSize) {
        ${impl.addOperation("builder", listBuilderElement)}
    }

    return builder
}
        """.trimIndent()
        )
    }
}