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
import generators.immutableMap.impl.MapImplementation
import generators.immutableMap.impl.mapKeyType
import java.io.PrintWriter

class MapUtilsGenerator(private val impl: MapImplementation): UtilsSourceGenerator() {
    override val outputFileName: String = "utils"

    override fun getPackage(): String {
        return super.getPackage() + ".immutableMap." + impl.packageName
    }

    override val imports: Set<String> = super.imports + "benchmarks.IntWrapper"

    override fun generateBody(out: PrintWriter) {
        out.println("""
fun persistentMapPut(keys: List<$mapKeyType>): ${impl.type()} {
    var map = ${impl.empty()}
    for (key in keys) {
        map = ${impl.putOperation("map", "key", "\"some element\"")}
    }
    return map
}
        """.trimIndent()
        )
    }
}