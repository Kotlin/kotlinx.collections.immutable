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

package generators.immutableMapBuilder.impl

import generators.immutableMapBuilder.*

class MapBuilderPaguroBenchmark:
        MapBuilderGetBenchmark,
        MapBuilderIterateBenchmark,
        MapBuilderPutBenchmark,
        MapBuilderRemoveBenchmark,
        MapBuilderBenchmarkUtils
{
    override val packageName: String = "paguro.builder"

    override fun mapBuilderType(K: String, V: String): String = "org.organicdesign.fp.collections.PersistentHashMap.MutableHashMap<$K, $V>"

    override fun emptyOf(K: String, V: String): String = "org.organicdesign.fp.collections.PersistentHashMap.emptyMutable<$K, $V>()"
    override fun immutableOf(K: String, V: String): String = "org.organicdesign.fp.collections.PersistentHashMap.empty<$K, $V>()"

    override val putOperation: String = "assoc"
    override val immutablePutOperation: String = "assoc"

    override val removeOperation: String = "without"

    override val builderOperation: String = "mutable"
}