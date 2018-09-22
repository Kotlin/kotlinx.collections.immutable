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

import kotlinx.collections.immutable.PersistentSet

class Marker

internal class PersistentHashSetBuilder<E>(private var set: PersistentHashSet<E>) : AbstractMutableSet<E>(), PersistentSet.Builder<E> {
    internal var marker = Marker()
    internal var node = set.node
    override var size = set.size

    override fun build(): PersistentSet<E> {
        set = if (node === set.node) {
            set
        } else {
            marker = Marker()
            PersistentHashSet(node, size)
        }
        return set
    }

    override fun contains(element: E): Boolean {
        val hashCode = element?.hashCode() ?: NULL_HASH_CODE
        return node.contains(hashCode, element, 0)
    }

    override fun add(element: E): Boolean {
        val size = this.size
        val hashCode = element?.hashCode() ?: NULL_HASH_CODE
        node = node.mutableAdd(hashCode, element, 0, this)
        return size != this.size
    }

    override fun remove(element: E): Boolean {
        val size = this.size
        val hashCode = element?.hashCode() ?: NULL_HASH_CODE
        node = node.mutableRemove(hashCode, element, 0, this) ?: TrieNode.EMPTY as TrieNode<E>
        return size != this.size
    }

    override fun clear() {
        node = TrieNode.EMPTY as TrieNode<E>
        size = 0
    }

    override fun iterator(): MutableIterator<E> {
        return PersistentHashSetMutableIterator(this)
    }
}