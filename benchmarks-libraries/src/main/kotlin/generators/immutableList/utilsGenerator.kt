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

import generators.UtilsSourceGenerator
import generators.immutableList.impl.ListImplementation
import generators.immutableList.impl.listElement
import java.io.PrintWriter

class ListUtilsGenerator(private val impl: ListImplementation): UtilsSourceGenerator() {
    override val outputFileName: String get() = "utils"

    override fun getPackage(): String {
        return super.getPackage() + ".immutableList." + impl.packageName
    }

    override fun generateBody(out: PrintWriter) {
        out.println("""
fun persistentListAdd(size: Int): ${impl.type()} {
    var list = ${impl.empty()}
    repeat(times = size) {
        list = ${impl.addOperation("list", listElement)}
    }
    return list
}
        """.trimIndent()
        )
    }
}