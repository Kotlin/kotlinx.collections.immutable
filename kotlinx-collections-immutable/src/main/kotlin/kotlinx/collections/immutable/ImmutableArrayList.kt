package kotlinx.collections.immutable

import kotlinx.collections.immutable.internal.ListImplementation
import java.util.*
import kotlin.collections.AbstractList

public class ImmutableArrayList<out E> internal constructor(private val impl: Array<Any?>): PersistentList<E>, AbstractList<E>() {

    public constructor(source: Collection<E>) : this(source.toTypedArray<Any?>())

    override val size: Int get() = impl.size
    override fun isEmpty(): Boolean = impl.isEmpty()
    override fun contains(element: @UnsafeVariance E): Boolean = impl.contains(element)
    override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean = elements.all { impl.contains(it) }
    override fun get(index: Int): E = impl[index.also { ListImplementation.checkElementIndex(it, impl.size) }] as E
    override fun indexOf(element: @UnsafeVariance E): Int = impl.indexOf(element)
    override fun lastIndexOf(element: @UnsafeVariance E): Int = impl.lastIndexOf(element)


    override fun iterator(): Iterator<E> = impl.iterator() as Iterator<E>

    override fun subList(fromIndex: Int, toIndex: Int): ImmutableList<E> = super<PersistentList>.subList(fromIndex, toIndex)

    override fun add(element: @UnsafeVariance E): PersistentList<E> = ImmutableArrayList(impl + element)

    override fun addAll(elements: Collection<@UnsafeVariance E>): PersistentList<E> = ImmutableArrayList(impl.plus(elements = elements))

    override fun remove(element: @UnsafeVariance  E): PersistentList<E> {
        val index = indexOf(element).takeUnless { it < 0 } ?: return this
        return removeAt(index)
    }

    override fun removeAll(elements: Collection<@UnsafeVariance E>): PersistentList<E> {
        val values = (impl as Array<E>).filterNot { it in elements }
        return if (values.size < impl.size) values.toImmutableArrayList() else this
    }

    override fun removeAll(predicate: (E) -> Boolean): PersistentList<E> {
        val values = (impl as Array<E>).filterNot(predicate)
        return if (values.size < impl.size) values.toImmutableArrayList() else this
    }

    override fun clear(): PersistentList<E> = if (impl.isEmpty()) this else EMPTY

    override fun addAll(index: Int, c: Collection<@UnsafeVariance E>): PersistentList<E> = mutate { it.addAll(index, c) }

    override fun set(index: Int, element: @UnsafeVariance E): PersistentList<E> = ImmutableArrayList(impl.copyOf().apply { set(index, element) })

    override fun add(index: Int, element: @UnsafeVariance E): PersistentList<E> = mutate { it.add(index, element) }

    override fun removeAt(index: Int): PersistentList<E> = mutate { it.removeAt(index) }

    interface Builder<E> : PersistentList.Builder<E> {
        override fun build(): ImmutableArrayList<E>
    }

    override fun builder(): Builder<@UnsafeVariance E> = object : ArrayList<E>(impl.asList() as List<E>), Builder<E> {
        private var lastResult = this@ImmutableArrayList
        private var lastModCount = 0
        private var setCount = 0
        override fun build(): ImmutableArrayList<E> =
                if (this.lastModCount == modCount && setCount == 0 || (lastResult.isEmpty() && this.isEmpty()))
                    lastResult
                else
                    ImmutableArrayList<E>(this.toArray()).also { lastResult = it; lastModCount = modCount; setCount = 0 }

        override fun set(index: Int, element: @UnsafeVariance E): E {
            setCount++
            return super.set(index, element)
        }


    }

    companion object {
        public val EMPTY: ImmutableArrayList<Nothing> = ImmutableArrayList(emptyArray())
    }
}

public fun <E> immutableArrayListOf(): ImmutableArrayList<E> = ImmutableArrayList.EMPTY
public fun <E> immutableArrayListOf(vararg elements: E): ImmutableArrayList<E> =
        if (elements.isNotEmpty()) ImmutableArrayList<E>(elements as Array<Any?>) else ImmutableArrayList.EMPTY

fun <E> Collection<E>.toImmutableArrayList(): ImmutableArrayList<E> =
    this as? ImmutableArrayList
            ?: (this as? ImmutableArrayList.Builder)?.build()
            ?: ImmutableArrayList(this)

fun <E> Iterable<E>.toImmutableArrayList(): ImmutableArrayList<E> =
    ((this as? Collection) ?: this.toList()).toImmutableArrayList()

fun <E> Array<E>.toImmutableArrayList(): ImmutableArrayList<E> =
    ImmutableArrayList(this.asList())