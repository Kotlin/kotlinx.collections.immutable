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

class MapKotlinBenchmark:
        MapGetBenchmark,
        MapIterateBenchmark,
        MapPutBenchmark,
        MapRemoveBenchmark,
        MapBenchmarkUtils
{
    override val packageName: String = "kotlin"

    override fun mapType(K: String, V: String): String = "kotlinx.collections.immutable.PersistentMap<$K, $V>"
    override fun emptyOf(K: String, V: String): String = "kotlinx.collections.immutable.persistentHashMapOf<$K, $V>()"

    override val getOperation: String = "get"
    override fun putOperation(map: String, key: String, value: String): String = "$map.put($key, $value)"
    override fun removeOperation(map: String, key: String): String = "$map.remove($key)"

    override val keys: String = "keys"
    override val values: String = "values"
}