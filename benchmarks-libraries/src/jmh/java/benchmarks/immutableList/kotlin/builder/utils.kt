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

package benchmarks.immutableList.kotlin.builder

import benchmarks.immutableSize

fun persistentListBuilderAdd(size: Int, immutablePercentage: Double): kotlinx.collections.immutable.PersistentList.Builder<String> {
    val immutableSize = immutableSize(size, immutablePercentage)

    var list = kotlinx.collections.immutable.persistentListOf<String>()
    repeat(times = immutableSize) {
        list = list.add("another element")
    }

    val builder = list.builder()
    repeat(times = size - immutableSize) {
        builder.add("some element")
    }

    return builder
}
