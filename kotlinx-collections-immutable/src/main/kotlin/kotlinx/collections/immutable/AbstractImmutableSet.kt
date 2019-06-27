/*
 * Copyright 2016-2017 JetBrains s.r.o.
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

package kotlinx.collections.immutable

import org.pcollections.PSet
import java.util.ConcurrentModificationException

public abstract class AbstractImmutableSet<out E> protected constructor(protected val impl: PSet<@UnsafeVariance E>) : PersistentSet<E> {

    override val size: Int get() = impl.size
    override fun isEmpty(): Boolean = impl.isEmpty()
    override fun contains(element: @UnsafeVariance E): Boolean = impl.contains(element)
    override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean = impl.containsAll(elements)
    override fun iterator(): Iterator<E> = impl.iterator()

    override fun equals(other: Any?): Boolean = impl.equals(other)
    override fun hashCode(): Int = impl.hashCode()
    override fun toString(): String = impl.toString()

    override fun add(element: @UnsafeVariance E): PersistentSet<E> = wrap(impl.plus(element))

    override fun addAll(elements: Collection<@UnsafeVariance E>): PersistentSet<E> = wrap(impl.plusAll(elements))

    override fun remove(element: @UnsafeVariance E): PersistentSet<E> = wrap(impl.minus(element))

    override fun removeAll(elements: Collection<@UnsafeVariance E>): PersistentSet<E> = mutate { it.removeAll(elements) }

    override fun removeAll(predicate: (E) -> Boolean): PersistentSet<E> = mutate { it.removeAll(predicate) }

    override abstract fun clear(): AbstractImmutableSet<E>

    override abstract fun builder(): Builder<@UnsafeVariance E>

    protected abstract fun wrap(impl: PSet<@UnsafeVariance E>): AbstractImmutableSet<E>

    abstract class Builder<E> internal constructor(protected var value: AbstractImmutableSet<E>, protected var impl: PSet<E>) : AbstractMutableSet<E>(), PersistentSet.Builder<E> {
        override fun build(): AbstractImmutableSet<E> = value.wrap(impl).apply { value = this }
        // delegating to impl
        override val size: Int get() = impl.size
        override fun isEmpty(): Boolean = impl.isEmpty()
        override fun contains(element: @UnsafeVariance E): Boolean = impl.contains(element)
        override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean = impl.containsAll(elements)

        override fun iterator(): MutableIterator<E> = object : MutableIterator<E> {
            private var snapshot = impl
            private val iterator = impl.iterator()
            private var nextCalled: Boolean = false
            private var current: E? = null

            override fun hasNext(): Boolean = iterator.hasNext()

            override fun next(): E {
                checkForComodification()
                val current = iterator.next()
                this.current = current
                nextCalled = true
                return current
            }

            override fun remove() {
                check(nextCalled)
                checkForComodification()
                remove(current as E)
                snapshot = impl
                current = null
                nextCalled = false
            }

            private fun checkForComodification() {
                if (snapshot !== impl) throw ConcurrentModificationException()
            }
        }

        override fun equals(other: Any?): Boolean = impl.equals(other)
        override fun hashCode(): Int = impl.hashCode()
        override fun toString(): String = impl.toString()



        protected inline fun mutate(operation: (PSet<E>) -> PSet<E>): Boolean {
            val newValue = operation(impl)
            if (newValue !== impl) {
                impl = newValue
                return true
            }
            return false
        }

        override fun add(element: E): Boolean = mutate { it.plus(element) }

        override fun addAll(elements: Collection<E>): Boolean = mutate { it.plusAll(elements) }

        override abstract fun clear() // { mutate { TreePVector.empty() } }

        override fun remove(element: E): Boolean = mutate { it.minus(element) }

        // by abstract set
        //override fun removeAll(elements: Collection<E>): Boolean = mutate { it.minusAll(elements) }

        //override fun retainAll(elements: Collection<E>): Boolean =

    }

}


