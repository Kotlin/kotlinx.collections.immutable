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

package generators.immutableMap

import generators.UtilsSourceGenerator
import java.io.PrintWriter

interface MapBenchmarkUtils {
    val packageName: String
    fun emptyOf(K: String, V: String): String
    fun mapType(K: String, V: String): String
    fun putOperation(map: String, key: String, value: String): String
}

class MapUtilsGenerator(private val impl: MapBenchmarkUtils): UtilsSourceGenerator() {
    override fun getPackage(): String = super.getPackage() + ".immutableMap." + impl.packageName

    override val outputFileName: String = "utils"

    override val imports: Set<String> = super.imports + "benchmarks.IntWrapper"

    override fun generateBody(out: PrintWriter) {
        out.println("""
fun persistentMapPut(keys: List<IntWrapper>): ${impl.mapType("IntWrapper", "String")} {
    var map = ${impl.emptyOf("IntWrapper", "String")}
    for (key in keys) {
        map = ${impl.putOperation("map", "key", "\"some element\"")}
    }
    return map
}
        """.trimIndent()
        )
    }
}