package kotlinx.collections.immutable

import org.pcollections.PSet
import java.util.*

internal abstract class AbstractImmutableSet<out E> protected constructor(protected val impl: PSet<@UnsafeVariance E>) : ImmutableSet<E> {

    override val size: Int get() = impl.size
    override fun isEmpty(): Boolean = impl.isEmpty()
    override fun contains(element: @UnsafeVariance E): Boolean = impl.contains(element)
    override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean = impl.containsAll(elements)
    override fun iterator(): Iterator<E> = impl.iterator()

    override fun equals(other: Any?): Boolean = impl.equals(other)
    override fun hashCode(): Int = impl.hashCode()
    override fun toString(): String = impl.toString()

    override fun add(element: @UnsafeVariance E): ImmutableSet<E> = wrap(impl.plus(element))

    override fun addAll(elements: Collection<@UnsafeVariance E>): ImmutableSet<E> = wrap(impl.plusAll(elements))

    override fun remove(element: @UnsafeVariance E): ImmutableSet<E> = wrap(impl.minus(element))

    override fun removeAll(elements: Collection<@UnsafeVariance E>): ImmutableSet<E> = mutate { it.removeAll(elements) }

    override fun removeAll(predicate: (E) -> Boolean): ImmutableSet<E> = mutate { it.removeAll(predicate) }

    override abstract fun clear(): AbstractImmutableSet<E>

    override abstract fun builder(): Builder<@UnsafeVariance E>

    protected abstract fun wrap(impl: PSet<@UnsafeVariance E>): AbstractImmutableSet<E>

    abstract class Builder<E> internal constructor(protected var value: AbstractImmutableSet<E>, protected var impl: PSet<E>) : AbstractSet<E>(), ImmutableSet.Builder<E> {
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


