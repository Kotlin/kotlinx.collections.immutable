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

package generators.immutableSet.impl

object VavrSortedSetImplementation: SetImplementation {
    override val packageName: String
            = "vavrSorted"

    override fun type(): String
            = "io.vavr.collection.TreeSet<$setElementType>"
    override fun empty(): String
            = "io.vavr.collection.TreeSet.empty<$setElementType>()"

    override fun addOperation(set: String, element: String): String
            = "$set.add($element)"
    override fun removeOperation(set: String, element: String): String
            = "$set.remove($element)"
}