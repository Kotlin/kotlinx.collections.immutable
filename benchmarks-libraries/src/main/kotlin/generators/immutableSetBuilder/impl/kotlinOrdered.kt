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

class SetBuilderKotlinOrderedBenchmark:
        SetBuilderContainsBenchmark,
        SetBuilderIterateBenchmark,
        SetBuilderAddBenchmark,
        SetBuilderRemoveBenchmark,
        SetBuilderBenchmarkUtils
{
    override val packageName: String = "kotlinOrdered.builder"

    override fun setBuilderType(E: String): String = "kotlinx.collections.immutable.PersistentSet.Builder<$E>"

    override fun emptyOf(E: String): String = "kotlinx.collections.immutable.persistentSetOf<$E>().builder()"
    override fun immutableOf(E: String): String = "kotlinx.collections.immutable.persistentSetOf<$E>()"

    override val addOperation: String = "add"
    override val immutableAddOperation: String = "add"

    override val removeOperation: String = "remove"

    override val builderOperation: String = "builder"
}