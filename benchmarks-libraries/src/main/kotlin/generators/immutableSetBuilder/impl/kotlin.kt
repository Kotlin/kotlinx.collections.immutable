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

package generators.immutableSetBuilder.impl

import generators.immutableSetBuilder.*

class SetBuilderKotlinBenchmark:
        SetBuilderContainsBenchmark,
        SetBuilderIterateBenchmark,
        SetBuilderAddBenchmark,
        SetBuilderRemoveBenchmark,
        SetBuilderBenchmarkUtils
{
    override val packageName: String = "kotlin.builder"

    override fun setBuilderType(E: String): String = "kotlinx.collections.immutable.PersistentSet.Builder<$E>"

    override fun emptyOf(E: String): String = "kotlinx.collections.immutable.persistentHashSetOf<$E>().builder()"
    override fun immutableOf(E: String): String = "kotlinx.collections.immutable.persistentHashSetOf<$E>()"

    override val addOperation: String = "add"
    override fun immutableAddOperation(set: String, element: String): String = "$set.add($element)"

    override val removeOperation: String = "remove"

    override fun builderOperation(set: String): String = "$set.builder()"
}