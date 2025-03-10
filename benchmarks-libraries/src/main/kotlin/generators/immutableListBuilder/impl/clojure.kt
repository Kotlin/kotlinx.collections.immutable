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

object ClojureListBuilderImplementation: ListBuilderImplementation {
    override val packageName: String
            = "clojure.builder"

    override fun type(): String
            = "clojure.lang.ITransientVector"
    override fun empty(): String
            = "clojure.lang.PersistentVector.EMPTY.asTransient() as clojure.lang.ITransientVector"

    override fun getOperation(builder: String, index: String): String
            = "$builder.valAt($index)"
    override fun setOperation(builder: String, index: String, newValue: String): String
            = "$builder.assocN($index, $newValue)"
    override fun addOperation(builder: String, element: String): String
            = "$builder.conj($element)"
    override fun removeLastOperation(builder: String): String
            = "$builder.pop()"

    override val isIterable: Boolean
            = false

    override fun builderOperation(immutable: String): String
            = "$immutable.asTransient() as clojure.lang.ITransientVector"

    override fun immutableEmpty(): String
            = "clojure.lang.PersistentVector.EMPTY"
    override fun immutableAddOperation(immutable: String, element: String): String
            = "$immutable.cons($element)"
}