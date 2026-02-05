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

package generators.immutableListBuilder.impl

object KotlinListBuilderImplementation: ListBuilderImplementation {
    override val packageName: String
            = "kotlin.builder"

    override fun type(): String
            = "kotlinx.collections.immutable.PersistentList.Builder<$listBuilderElementType>"
    override fun empty(): String
            = "kotlinx.collections.immutable.persistentListOf<$listBuilderElementType>().builder()"

    override fun getOperation(builder: String, index: String): String
            = "$builder.get($index)"
    override fun setOperation(builder: String, index: String, newValue: String): String
            = "$builder.set($index, $newValue)"
    override fun addOperation(builder: String, element: String): String
            = "$builder.add($element)"
    override fun removeLastOperation(builder: String): String
            = "$builder.removeAt($builder.size - 1)"

    override val isIterable: Boolean
            = true

    override fun builderOperation(immutable: String): String
            = "$immutable.builder()"

    override fun immutableEmpty(): String
            = "kotlinx.collections.immutable.persistentListOf<$listBuilderElementType>()"
    override fun immutableAddOperation(immutable: String, element: String): String
            = "$immutable.add($element)"
}