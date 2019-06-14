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

import generators.UtilsSourceGenerator
import java.io.PrintWriter

class SetBuilderUtilsGenerator(private val impl: SetBuilderImplementation): UtilsSourceGenerator() {
    override fun getPackage(): String = super.getPackage() + ".immutableSet." + impl.packageName

    override val outputFileName: String = "utils"

    override val imports: Set<String> = super.imports + "benchmarks.immutableSize" + "benchmarks.IntWrapper"

    override fun generateBody(out: PrintWriter) {
        out.println("""
fun persistentSetBuilderAdd(
        elements: List<$setBuilderElementType>,
        immutablePercentage: Double
): ${impl.type()} {
    val immutableSize = immutableSize(elements.size, immutablePercentage)

    var set = ${impl.immutableEmpty()}
    for (index in 0 until immutableSize) {
        set = ${impl.immutableAddOperation("set", "elements[index]")}
    }

    val builder = ${impl.builderOperation("set")}
    for (index in immutableSize until elements.size) {
        ${impl.addOperation("builder", "elements[index]")}
    }

    return builder
}
        """.trimIndent()
        )
    }
}