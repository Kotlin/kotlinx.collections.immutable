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

package benchmarks.immutableSet.clojure.builder

import benchmarks.immutableSize

fun <E> persistentSetBuilderAdd(
        elements: List<E>,
        immutablePercentage: Double
): clojure.lang.ATransientSet {
    val immutableSize = immutableSize(elements.size, immutablePercentage)

    var set = clojure.lang.PersistentHashSet.EMPTY
    for (index in 0 until immutableSize) {
        set = set.cons(elements[index]) as clojure.lang.PersistentHashSet
    }

    val builder = set.asTransient() as clojure.lang.ATransientSet
    for (index in immutableSize until elements.size) {
        builder.conj(elements[index])
    }

    return builder
}
