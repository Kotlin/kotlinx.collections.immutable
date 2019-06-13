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

package generators.immutableListBuilder.impl

import generators.immutableListBuilder.*

class ListBuilderPaguroBenchmark:
        ListBuilderAddBenchmark,
        ListBuilderGetBenchmark,
        ListBuilderIterateBenchmark,
        ListBuilderRemoveBenchmark,
        ListBuilderSetBenchmark,
        ListBuilderBenchmarkUtils
{
    override val packageName: String = "paguro.builder"

    override fun listBuilderType(T: String): String = "org.organicdesign.fp.collections.RrbTree.MutableRrbt<$T>"

    override fun emptyOf(T: String): String = "org.organicdesign.fp.collections.RrbTree.emptyMutable<$T>()"
    override fun immutableOf(T: String): String = "org.organicdesign.fp.collections.RrbTree.empty<$T>()"

    override val addOperation: String = "append"
    override val immutableAddOperation: String = "append"

    override fun removeAtOperation(builder: String): String = "without($builder.size - 1)"

    override val getOperation: String = "get"

    override val setOperation: String = "replace"

    override fun builderOperation(list: String): String = "$list.mutable()"
}