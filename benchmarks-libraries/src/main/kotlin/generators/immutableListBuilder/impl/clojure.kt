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

class ListBuilderClojureBenchmark:
        ListBuilderAddBenchmark,
        ListBuilderGetBenchmark,
        ListBuilderRemoveBenchmark,
        ListBuilderSetBenchmark,
        ListBuilderBenchmarkUtils
{
    override val packageName: String = "clojure.builder"

    override fun listBuilderType(T: String): String = "clojure.lang.ITransientVector"

    override fun emptyOf(T: String): String = "clojure.lang.PersistentVector.EMPTY.asTransient() as clojure.lang.ITransientVector"
    override fun immutableOf(T: String): String = "clojure.lang.PersistentVector.EMPTY"

    override val addOperation: String = "conj"
    override val immutableAddOperation: String = "cons"

    override fun removeAtOperation(builder: String): String = "pop()"

    override val getOperation: String = "valAt"

    override val setOperation: String = "assocN"

    override fun builderOperation(list: String): String = "$list.asTransient() as clojure.lang.ITransientVector"
}