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

object CapsuleSetBuilderImplementation: SetBuilderImplementation {
    override val packageName: String
            = "capsule.builder"

    override fun type(): String
            = "io.usethesource.capsule.Set.Transient<$setBuilderElementType>"
    override fun empty(): String
            = "io.usethesource.capsule.core.PersistentTrieSet.of<$setBuilderElementType>().asTransient()"

    override fun addOperation(builder: String, element: String): String
            = "$builder.add($element)"
    override fun removeOperation(builder: String, element: String): String
            = "$builder.__remove($element)"

    override val isIterable: Boolean
            = true

    override fun builderOperation(immutable: String): String
            = "$immutable.asTransient()"

    override fun immutableEmpty(): String
            = "io.usethesource.capsule.core.PersistentTrieSet.of<$setBuilderElementType>()"
    override fun immutableAddOperation(immutable: String, element: String): String
            = "$immutable.__insert($element)"
}