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

package benchmarks.immutableMap.builder

import benchmarks.*
import benchmarks.immutableMap.emptyPersistentMap
import kotlinx.collections.immutable.PersistentMap


fun persistentMapBuilderPut(
        implementation: String,
        keys: List<IntWrapper>,
        immutablePercentage: Double
): PersistentMap.Builder<IntWrapper, String> {
    val immutableSize = immutableSize(keys.size, immutablePercentage)

    var map = emptyPersistentMap<IntWrapper, String>(implementation)
    for (index in 0 until immutableSize) {
        map = map.put(keys[index], "some element")
    }

    val builder = map.builder()
    for (index in immutableSize until keys.size) {
        builder[keys[index]] = "some element"
    }

    return builder
}
