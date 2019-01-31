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

package kotlinx.collections.immutable.stressTests

import java.util.*

class WrapperGenerator<K: Comparable<K>>(private val hashCodeUpperBound: Int) {
    private val elementMap = hashMapOf<K, ObjectWrapper<K>>()
    private val hashCodeMap = hashMapOf<Int, MutableList<ObjectWrapper<K>>>()
    private val random = Random()

    fun wrapper(element: K): ObjectWrapper<K> {
        val existing = elementMap[element]
        if (existing != null) {
            return existing
        }
        val hashCode = random.nextInt(hashCodeUpperBound)
        val wrapper = ObjectWrapper(element, hashCode)
        elementMap[element] = wrapper

        val wrappers = hashCodeMap[hashCode] ?: mutableListOf()
        wrappers.add(wrapper)
        hashCodeMap[hashCode] = wrappers

        return wrapper
    }

    fun wrappersByHashCode(hashCode: Int): List<ObjectWrapper<K>> {
        return hashCodeMap[hashCode] ?: mutableListOf()
    }
}