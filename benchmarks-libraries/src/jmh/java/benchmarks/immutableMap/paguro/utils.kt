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

package benchmarks.immutableMap.paguro

import benchmarks.IntWrapper

fun persistentMapPut(keys: List<IntWrapper>): org.organicdesign.fp.collections.PersistentHashMap<IntWrapper, String> {
    var map = org.organicdesign.fp.collections.PersistentHashMap.empty<IntWrapper, String>()
    for (key in keys) {
        map = map.assoc(key, "some element")
    }
    return map
}
