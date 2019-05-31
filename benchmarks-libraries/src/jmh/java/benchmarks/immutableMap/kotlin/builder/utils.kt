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

// Auto-generated file. DO NOT EDIT!

package benchmarks.immutableMap.kotlin.builder

import benchmarks.*

fun persistentMapBuilderPut(
        keys: List<IntWrapper>,
        immutablePercentage: Double
): kotlinx.collections.immutable.PersistentMap.Builder<IntWrapper, String> {
    val immutableSize = immutableSize(keys.size, immutablePercentage)

    var map = kotlinx.collections.immutable.persistentHashMapOf<IntWrapper, String>()
    for (index in 0 until immutableSize) {
        map = map.put(keys[index], "some element")
    }

    val builder = map.builder()
    for (index in immutableSize until keys.size) {
        builder.put(keys[index], "some element")
    }

    return builder
}
