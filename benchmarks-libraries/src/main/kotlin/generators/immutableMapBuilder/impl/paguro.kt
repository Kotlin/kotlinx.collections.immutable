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

object PaguroMapBuilderImplementation: MapBuilderImplementation {
    override val packageName: String
            = "paguro.builder"

    override fun type(): String
            = "org.organicdesign.fp.collections.PersistentHashMap.MutableHashMap<$mapBuilderKeyType, $mapBuilderValueType>"
    override fun empty(): String
            = "org.organicdesign.fp.collections.PersistentHashMap.emptyMutable<$mapBuilderKeyType, $mapBuilderValueType>()"

    override fun getOperation(builder: String, key: String): String
            = "$builder.get($key)"
    override fun putOperation(builder: String, key: String, value: String): String
            = "$builder.assoc($key, $value)"
    override fun removeOperation(builder: String, key: String): String
            = "$builder.without($key)"

    override val isIterable: Boolean
            = true

    override fun builderOperation(immutable: String): String =
            "$immutable.mutable()"

    override fun immutableEmpty(): String
            = "org.organicdesign.fp.collections.PersistentHashMap.empty<$mapBuilderKeyType, $mapBuilderValueType>()"
    override fun immutablePutOperation(immutable: String, key: String, value: String): String
            = "$immutable.assoc($key, $value)"
}