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

package generators.immutableSet

import generators.BenchmarkSourceGenerator
import java.io.PrintWriter

class SetRemoveBenchmarkGenerator(private val impl: SetImplementation) : BenchmarkSourceGenerator() {
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

    private var elements = listOf<$setElementType>()
    private var persistentSet = ${impl.empty()}

    @Setup(Level.Trial)
    fun prepare() {
        elements = generateElements(hashCodeType, size)
        persistentSet = persistentSetAdd(elements)

        if (hashCodeType == NON_EXISTING_HASH_CODE)
            elements = generateElements(hashCodeType, size)
    }

    @Benchmark
    fun remove(): ${impl.type()} {
        var set = persistentSet
        repeat(times = size) { index ->
            set = ${impl.removeOperation("set", "elements[index]")}
        }
        return set
    }
}
        """.trimIndent()
        )
    }
}