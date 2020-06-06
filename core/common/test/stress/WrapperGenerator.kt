/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress

import kotlin.random.Random


class WrapperGenerator<K: Comparable<K>>(private val hashCodeUpperBound: Int) {
    private val elementMap = hashMapOf<K, ObjectWrapper<K>>()
    private val hashCodeMap = hashMapOf<Int, MutableList<ObjectWrapper<K>>>()

    fun wrapper(element: K): ObjectWrapper<K> {
        val existing = elementMap[element]
        if (existing != null) {
            return existing
        }
        val hashCode = Random.nextInt(hashCodeUpperBound)
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