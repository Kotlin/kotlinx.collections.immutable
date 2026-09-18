/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable

/**
 * A persistent set whose element iteration order is unspecified.
 *
 * This interface expresses that insertion order is not required. Modification operations return
 * unordered sets and may change the relative iteration order of existing elements. Iteration order
 * remains the same for any given immutable set instance.
 *
 * Equality and hash codes follow the [Set] contract and do not depend on iteration order.
 *
 * @param E the type of elements contained in the set. The set is covariant on its element type.
 */
public interface PersistentUnorderedSet<out E> : PersistentSet<E> {
    override fun adding(element: @UnsafeVariance E): PersistentUnorderedSet<E>

    override fun addingAll(elements: Collection<@UnsafeVariance E>): PersistentUnorderedSet<E>

    override fun removing(element: @UnsafeVariance E): PersistentUnorderedSet<E>

    override fun removingAll(elements: Collection<@UnsafeVariance E>): PersistentUnorderedSet<E>

    override fun removingAll(predicate: (E) -> Boolean): PersistentUnorderedSet<E>

    override fun retainingAll(elements: Collection<@UnsafeVariance E>): PersistentUnorderedSet<E>

    override fun cleared(): PersistentUnorderedSet<E>

    override fun builder(): Builder<@UnsafeVariance E>

    /**
     * A reusable builder of an unordered persistent set.
     *
     * Iteration order is unspecified. Modifications do not affect previously built sets.
     */
    public interface Builder<E> : PersistentSet.Builder<E> {
        override fun build(): PersistentUnorderedSet<E>
    }
}
