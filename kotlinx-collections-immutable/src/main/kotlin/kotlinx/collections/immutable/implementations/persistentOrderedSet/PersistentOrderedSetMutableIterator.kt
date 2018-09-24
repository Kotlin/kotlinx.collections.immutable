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

package kotlinx.collections.immutable.implementations.persistentOrderedSet

internal class PersistentOrderedSetMutableIterator<E>(private val builder: PersistentOrderedSetBuilder<E>)
    : PersistentOrderedSetIterator<E>(builder.firstElement, builder.mapBuilder), MutableIterator<E> {

    var lastReturned: E? = null
    var nextWasInvoked = false

    override fun next(): E {
        val next = super.next()
        lastReturned = next
        nextWasInvoked = true
        return next
    }

    override fun remove() {
        if (!nextWasInvoked) {
            throw NoSuchElementException()
        }
        builder.remove(lastReturned)
        index--
        lastReturned = null
        nextWasInvoked = false
    }
}
