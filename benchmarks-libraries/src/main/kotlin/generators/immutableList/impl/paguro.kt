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

package generators.immutableList.impl

import generators.immutableList.*

class ListPaguroBenchmark:
        ListAddBenchmark,
        ListGetBenchmark,
        ListIterateBenchmark,
        ListRemoveBenchmark,
        ListSetBenchmark,
        ListBenchmarkUtils
{
    override val packageName: String = "paguro"

    override fun listType(T: String): String = "org.organicdesign.fp.collections.RrbTree.ImRrbt<$T>"

    override fun emptyOf(T: String): String = "org.organicdesign.fp.collections.RrbTree.empty<$T>()"

    override val addOperation: String = "append"

    override val removeAtOperation: String = "without"

    override val setOperation: String = "replace"
}