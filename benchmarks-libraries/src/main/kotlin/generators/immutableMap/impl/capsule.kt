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

package generators.immutableMap.impl

import generators.immutableMap.*

class MapCapsuleBenchmark:
        MapGetBenchmark,
        MapIterateBenchmark,
        MapPutBenchmark,
        MapRemoveBenchmark,
        MapBenchmarkUtils
{
    override val packageName: String = "capsule"

    override fun mapType(K: String, V: String): String = "io.usethesource.capsule.Map.Immutable<$K, $V>"

    override fun emptyOf(K: String, V: String): String = "io.usethesource.capsule.core.PersistentTrieMap.of<$K, $V>()"

    override val putOperation: String = "__put"

    override val removeOperation: String = "__remove"
}