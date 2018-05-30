/*
 * Copyright 2016-2018 JetBrains s.r.o.
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

package kotlinx.collections.immutable.implementations.immutableSet

import kotlinx.collections.immutable.ImmutableSet

class Marker

internal class PersistentHashSetBuilder<E>(var node: TrieNode<E>,
                                           override var size: Int) : AbstractMutableSet<E>(), ImmutableSet.Builder<E> {
    internal var marker = Marker()

    override fun build(): ImmutableSet<E> {
        marker = Marker()
        return PersistentHashSet(node, size)
    }

    override fun contains(element: E): Boolean {
        val hashCode = element?.hashCode() ?: NULL_HASH_CODE
        return node.contains(hashCode, element, 0)
    }

    override fun containsAll(elements: Collection<E>): Boolean {
        return elements.all { contains(it) }
    }

    override fun isEmpty(): Boolean {
        return size == 0
    }

    override fun clear() {
        node = TrieNode.EMPTY as TrieNode<E>
        size = 0
    }

    override fun add(element: E): Boolean {
        val hashCode = element?.hashCode() ?: NULL_HASH_CODE
        node = node.makeMutableFor(this)
        return node.mutableAdd(hashCode, element, 0, this)
    }

    override fun addAll(elements: Collection<E>): Boolean {
        var isModified = false
        for (element in elements) {
            isModified = isModified || add(element)
        }
        return isModified
    }

    override fun remove(element: E): Boolean {
        val hashCode = element?.hashCode() ?: NULL_HASH_CODE
        node = node.makeMutableFor(this)
        return node.mutableRemove(hashCode, element, 0, this)
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        var isModified = false
        for (element in elements) {
            isModified = isModified || remove(element)
        }
        return isModified
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        var isModified = false
        val iterator = iterator()
        while (iterator.hasNext()) {
            if (elements.contains(iterator.next())) {
                iterator.remove()
                isModified = true
            }
        }
        return isModified
    }

    override fun iterator(): MutableIterator<E> {
        return PersistentHashSetMutableIterator(this)
    }
}