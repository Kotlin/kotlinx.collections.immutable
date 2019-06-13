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

package benchmarks.immutableMap.clojure.builder

import benchmarks.*

fun persistentMapBuilderPut(
        keys: List<IntWrapper>,
        immutablePercentage: Double
): clojure.lang.ATransientMap {
    val immutableSize = immutableSize(keys.size, immutablePercentage)

    var map = clojure.lang.PersistentHashMap.EMPTY as clojure.lang.IPersistentMap
    for (index in 0 until immutableSize) {
        map = map.assoc(keys[index], "some element")
    }

    val builder = (map as clojure.lang.PersistentHashMap).asTransient() as clojure.lang.ATransientMap
    for (index in immutableSize until keys.size) {
        builder.assoc(keys[index], "some element")
    }

    return builder
}
