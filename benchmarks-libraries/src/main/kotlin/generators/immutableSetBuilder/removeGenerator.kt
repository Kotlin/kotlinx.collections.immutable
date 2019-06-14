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

import generators.BenchmarkSourceGenerator
import generators.immutableSetBuilder.impl.SetBuilderImplementation
import generators.immutableSetBuilder.impl.setBuilderElementType
import java.io.PrintWriter

class SetBuilderRemoveBenchmarkGenerator(private val impl: SetBuilderImplementation) : BenchmarkSourceGenerator() {
    override val outputFileName: String = "Remove"

    override fun getPackage(): String {
        return super.getPackage() + ".immutableSet." + impl.packageName
    }

    override val imports: Set<String> = super.imports + "benchmarks.*"

    override fun generateBenchmark(out: PrintWriter) {
        out.println("""
open class Remove {
    @Param("10000", "100000")
    var size: Int = 0

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE, NON_EXISTING_HASH_CODE)
    var hashCodeType = ""

    @Param("0.0", "50.0")
    var immutablePercentage: Double = 0.0

    private var elements = listOf<$setBuilderElementType>()
    private var elementsToRemove = listOf<$setBuilderElementType>()

    @Setup(Level.Trial)
    fun prepare() {
        elements = generateElements(hashCodeType, size)

        elementsToRemove = if (hashCodeType == NON_EXISTING_HASH_CODE) {
            generateElements(hashCodeType, size)
        } else {
            elements
        }
    }

    @Benchmark
    fun addAndRemove(): ${impl.type()} {
        val builder = persistentSetBuilderAdd(elements, immutablePercentage)
        repeat(times = size) { index ->
            ${impl.removeOperation("builder", "elementsToRemove[index]")}
        }
        return builder
    }
}
        """.trimIndent()
        )
    }
}