/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable

/**
 * A persistent set that iterates its elements in the insertion order.
 *
 * The set maintains a predictable iteration order: elements are iterated in the order they were
 * inserted into the set, from oldest to newest. The insertion order is not affected if an element is
 * re-inserted, i.e. when [adding] is called with an element equal to one the set already contains.
 *
 * Modification operations return ordered sets. Equality and hash codes follow the [Set] contract
 * and do not depend on iteration order.
 *
 * @param E the type of elements contained in the set. The set is covariant on its element type.
 */
public interface PersistentOrderedSet<out E> : PersistentSet<E> {
    override fun adding(element: @UnsafeVariance E): PersistentOrderedSet<E>

    override fun addingAll(elements: Collection<@UnsafeVariance E>): PersistentOrderedSet<E>

    override fun removing(element: @UnsafeVariance E): PersistentOrderedSet<E>

    override fun removingAll(elements: Collection<@UnsafeVariance E>): PersistentOrderedSet<E>

    override fun removingAll(predicate: (E) -> Boolean): PersistentOrderedSet<E>

    override fun retainingAll(elements: Collection<@UnsafeVariance E>): PersistentOrderedSet<E>

    override fun cleared(): PersistentOrderedSet<E>

    override fun builder(): Builder<@UnsafeVariance E>

    /**
     * A builder of a [PersistentOrderedSet].
     *
     * [PersistentOrderedSet.Builder] extends the [PersistentSet.Builder] contract with the iteration order
     * guarantee of [PersistentOrderedSet]: the builder's elements are iterated in the insertion order, and
     * [build] returns a set with the same iteration order.
     */
    public interface Builder<E> : PersistentSet.Builder<E> {
        override fun build(): PersistentOrderedSet<E>
    }
}
