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

interface MapImplementation {
    val packageName: String

    fun type(): String
    fun empty(): String

    fun keysOperation(map: String): String
    fun valuesOperation(map: String): String

    fun getOperation(map: String, key: String): String
    fun putOperation(map: String, key: String, value: String): String
    fun removeOperation(map: String, key: String): String
}

const val mapKeyType = "IntWrapper"
const val mapValueType = "String"