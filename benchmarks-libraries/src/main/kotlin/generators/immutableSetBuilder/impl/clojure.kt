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

class SetBuilderClojureBenchmark:
        SetBuilderContainsBenchmark,
        SetBuilderAddBenchmark,
        SetBuilderRemoveBenchmark,
        SetBuilderBenchmarkUtils
{
    override val packageName: String = "clojure.builder"

    override fun setBuilderType(E: String): String = "clojure.lang.ATransientSet"

    override fun emptyOf(E: String): String = "clojure.lang.PersistentHashSet.EMPTY.asTransient() as clojure.lang.ATransientSet"
    override fun immutableOf(E: String): String = "clojure.lang.PersistentHashSet.EMPTY"

    override val addOperation: String = "conj"
    override fun immutableAddOperation(set: String, element: String): String = "$set.cons($element) as clojure.lang.PersistentHashSet"

    override val removeOperation: String = "disjoin"

    override fun builderOperation(set: String): String = "$set.asTransient() as clojure.lang.ATransientSet"
}