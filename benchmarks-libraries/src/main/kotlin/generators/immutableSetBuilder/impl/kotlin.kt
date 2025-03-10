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

package generators.immutableSetBuilder.impl

object KotlinSetBuilderImplementation: SetBuilderImplementation {
    override val packageName: String
            = "kotlin.builder"

    override fun type(): String
            = "kotlinx.collections.immutable.PersistentSet.Builder<$setBuilderElementType>"
    override fun empty(): String
            = "kotlinx.collections.immutable.persistentHashSetOf<$setBuilderElementType>().builder()"

    override fun addOperation(builder: String, element: String): String
            = "$builder.add($element)"
    override fun removeOperation(builder: String, element: String): String
            = "$builder.remove($element)"

    override val isIterable: Boolean
            = true

    override fun builderOperation(immutable: String): String
            = "$immutable.builder()"

    override fun immutableEmpty(): String
            = "kotlinx.collections.immutable.persistentHashSetOf<$setBuilderElementType>()"
    override fun immutableAddOperation(immutable: String, element: String): String
            = "$immutable.add($element)"
}