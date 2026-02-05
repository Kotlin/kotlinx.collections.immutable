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

package generators.immutableList.impl

object ClojureListImplementation: ListImplementation {
    override val packageName: String
            = "clojure"

    override fun type(): String
            = "clojure.lang.PersistentVector"
    override fun empty(): String
            = "clojure.lang.PersistentVector.EMPTY"

    override fun getOperation(list: String, index: String): String
            = "$list.get($index)"
    override fun setOperation(list: String, index: String, newValue: String): String
            = "$list.assocN($index, $newValue)"
    override fun addOperation(list: String, element: String): String
            = "$list.cons($element)"
    override fun removeLastOperation(list: String): String
            = "$list.pop()"

    override fun iterateLastToFirst(list: String, size: String): String {
        return """
    @Benchmark
    fun lastToFirst(bh: Blackhole) {
        val iterator = $list.listIterator($size)

        while (iterator.hasPrevious()) {
            bh.consume(iterator.previous())
        }
    }"""
    }
}