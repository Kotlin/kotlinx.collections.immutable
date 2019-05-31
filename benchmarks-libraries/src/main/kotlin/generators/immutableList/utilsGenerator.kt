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

import generators.BenchmarkUtilsGenerator
import java.io.PrintWriter

interface ListBenchmarkUtils {
    val packageName: String
    fun emptyOf(T: String): String
    fun listType(T: String): String
    val addOperation: String
}

class ListUtilsGenerator(private val impl: ListBenchmarkUtils): BenchmarkUtilsGenerator() {
    override fun getPackage(): String = super.getPackage() + ".immutableList." + impl.packageName

    override val outputFileName: String = "utils"

    override fun generateBody(out: PrintWriter) {
        out.println("""
fun persistentListAdd(size: Int): ${impl.listType("String")} {
    var list = ${impl.emptyOf("String")}
    repeat(times = size) {
        list = list.${impl.addOperation}("some element")
    }
    return list
}
        """.trimIndent()
        )
    }
}