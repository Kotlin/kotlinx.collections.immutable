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

package generators.immutableList

import generators.BenchmarkSourceGenerator
import java.io.PrintWriter

interface ListSetBenchmark {
    val packageName: String
    fun emptyOf(T: String): String
    fun listType(T: String): String
    val setOperation: String
}

class ListSetBenchmarkGenerator(private val impl: ListSetBenchmark) : BenchmarkSourceGenerator() {
    override val benchmarkName: String = "Set"

    override fun getPackage(): String {
        return super.getPackage() + ".immutableList." + impl.packageName
    }

    override fun generateBody(out: PrintWriter) {
        out.println("""
open class Set {
    @Param("10000", "100000")
    var size: Int = 0

    private var persistentList = ${impl.emptyOf("String")}
    private var randomIndices = listOf<Int>()

    @Setup(Level.Trial)
    fun prepare() {
        persistentList = persistentListAdd(size)
        randomIndices = List(size) { it }.shuffled()
    }

    @Benchmark
    fun setByIndex(): ${impl.listType("String")} {
        repeat(times = size) { index ->
            persistentList = persistentList.${impl.setOperation}(index, "another element")
        }
        return persistentList
    }

    @Benchmark
    fun setByRandomIndex(): ${impl.listType("String")} {
        repeat(times = size) { index ->
            persistentList = persistentList.${impl.setOperation}(randomIndices[index], "another element")
        }
        return persistentList
    }
}
        """.trimIndent()
        )
    }
}