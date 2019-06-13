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

class ListBuilderKotlinBenchmark:
        ListBuilderAddBenchmark,
        ListBuilderGetBenchmark,
        ListBuilderIterateBenchmark,
        ListBuilderRemoveBenchmark,
        ListBuilderSetBenchmark,
        ListBuilderBenchmarkUtils
{
    override val packageName: String = "kotlin.builder"

    override fun listBuilderType(T: String): String = "kotlinx.collections.immutable.PersistentList.Builder<$T>"

    override fun emptyOf(T: String): String = "kotlinx.collections.immutable.persistentListOf<$T>().builder()"
    override fun immutableOf(T: String): String = "kotlinx.collections.immutable.persistentListOf<$T>()"

    override val addOperation: String = "add"
    override val immutableAddOperation: String = "add"

    override fun removeAtOperation(builder: String): String = "removeAt($builder.size - 1)"

    override val getOperation: String = "get"

    override val setOperation: String = "set"

    override fun builderOperation(list: String): String = "$list.builder()"
}