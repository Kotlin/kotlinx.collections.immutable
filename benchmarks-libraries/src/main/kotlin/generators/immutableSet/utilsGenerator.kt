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

import generators.BenchmarkUtilsGenerator
import java.io.PrintWriter

interface SetBenchmarkUtils {
    val packageName: String
    fun emptyOf(E: String): String
    fun setType(E: String): String
    val addOperation: String
}

class SetUtilsGenerator(private val impl: SetBenchmarkUtils): BenchmarkUtilsGenerator() {
    override fun getPackage(): String = super.getPackage() + ".immutableSet." + impl.packageName

    override val outputFileName: String = "utils"

    override fun generateBody(out: PrintWriter) {
        out.println("""
fun <E> persistentSetAdd(elements: List<E>): ${impl.setType("E")} {
    var set = ${impl.emptyOf("E")}
    for (element in elements) {
        set = set.${impl.addOperation}(element)
    }
    return set
}
        """.trimIndent()
        )
    }
}