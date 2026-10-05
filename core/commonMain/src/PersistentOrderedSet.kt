/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable

/**
 * A persistent set that iterates its elements in the insertion order.
 *
 * Adding a new element appends it to the iteration order. Adding an element already in the set does not
 * change its position. Removing an element preserves the relative order of the remaining elements;
 * removing and then adding it again places it at the end. Bulk additions process elements in the
 * iteration order of the source collection.
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
