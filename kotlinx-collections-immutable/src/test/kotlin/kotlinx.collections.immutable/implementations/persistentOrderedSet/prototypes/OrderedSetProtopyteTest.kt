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

package kotlinx.collections.immutable.implementations.persistentOrderedSet.prototypes

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.implementations.persistentOrderedSet.prototypes.nullifying.NullifyingOrderedSet
import kotlinx.collections.immutable.implementations.persistentOrderedSet.prototypes.uniqueId.UniqueIdOrderedSet
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NullifyingOrderedSetTest : BaseOrderedSetTest() {
    override fun <E> emptyOf(): PersistentSet<E> = NullifyingOrderedSet.emptyOf()
}

class UniqueIdOrderedSetTest: BaseOrderedSetTest() {
    override fun <E> emptyOf(): PersistentSet<E> = UniqueIdOrderedSet.emptyOf()
}

abstract class BaseOrderedSetTest {

    abstract fun <E> emptyOf(): PersistentSet<E>

    @Test
    fun add() {
        var set = emptyOf<Int>()

        assertEquals(0, set.size)
        assertTrue(set.isEmpty())

        set = set.add(1)
        assertTrue(set.contains(1))
        assertFalse(set.contains(2))

        set = set.add(2)
        assertTrue(set.contains(1))
        assertTrue(set.contains(2))

        assertEquals(2, set.size)
        assertFalse(set.isEmpty())

        val elementsToAdd = 10000
        repeat(elementsToAdd - 2) { index ->
            set = set.add(index + 3)
        }

        val iterator = set.iterator()
        repeat(elementsToAdd) { index ->
            assertTrue(iterator.hasNext())
            assertEquals(index + 1, iterator.next())
        }
    }

    @Test
    fun remove() {
        var set = emptyOf<Int>()
        val mutableSet = mutableSetOf<Int>()

        set = set.add(1)
        mutableSet.add(1)

        assertEquals<Set<Int>>(set, mutableSet)

        set = set.remove(1)
        mutableSet.remove(1)

        assertEquals<Set<Int>>(set, mutableSet)

        set = set.remove(1)
        mutableSet.remove(1)

        assertEquals<Set<Int>>(set, mutableSet)

        val elementsToAdd = 10000

        repeat(elementsToAdd) {
            set = set.add(it)
            mutableSet.add(it)
            assertEquals<Set<Int>>(set, mutableSet)
        }
        repeat(elementsToAdd) {
            set = set.remove(it)
            mutableSet.remove(it)
            assertEquals<Set<Int>>(set, mutableSet)
        }

        repeat(elementsToAdd) {
            set = set.add(it)
            mutableSet.add(it)
            assertEquals<Set<Int>>(set, mutableSet)
        }
        repeat(elementsToAdd) {
            set = set.remove(elementsToAdd - it - 1)
            mutableSet.remove(elementsToAdd - it - 1)
            assertEquals<Set<Int>>(set, mutableSet)
        }

        val random = Random()
        repeat(elementsToAdd) {
            set = set.add(it)
            mutableSet.add(it)
            assertEquals<Set<Int>>(set, mutableSet)

            val shouldRemove = random.nextDouble() < 0.2
            if (shouldRemove) {
                val elementToRemove = random.nextInt(it + 1)
                set = set.remove(elementToRemove)
                mutableSet.remove(elementToRemove)
                assertEquals<Set<Int>>(set, mutableSet)
            }
        }
        val shuffledElements = mutableSet.shuffled()
        for (element in shuffledElements) {
            set = set.remove(element)
            mutableSet.remove(element)
            assertEquals<Set<Int>>(set, mutableSet)
        }
    }
}