/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableSet

import benchmarks.*
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.persistentSetOf
import kotlin.math.ceil
import kotlin.math.log


fun <E> emptyPersistentSet(implementation: String): PersistentSet<E> = when (implementation) {
    HASH_IMPL -> persistentHashSetOf()
    ORDERED_IMPL -> persistentSetOf()
    else -> throw AssertionError("Unknown PersistentSet implementation: $implementation")
}

fun <E> persistentSetAdd(implementation: String, elements: List<E>): PersistentSet<E> {
    var set = emptyPersistentSet<E>(implementation)
    for (element in elements) {
        set = set.adding(element)
    }
    return set
}

fun <E> persistentSetRemove(persistentSet: PersistentSet<E>, elements: List<E>): PersistentSet<E> {
    var set = persistentSet
    for (element in elements) {
        set = set.removing(element)
    }
    return set
}

private const val branchingFactor = 32
private const val logBranchingFactor = 5

private fun expectedHeightOfPersistentSetWithSize(size: Int): Int {
    return ceil(log(size.toDouble(), branchingFactor.toDouble())).toInt()
}

/**
 * Returns the size of a persistent set whose expected height is
 * half of the specified [persistentSet]'s expected height.
 */
fun sizeForHalfHeight(persistentSet: PersistentSet<*>): Int {
    val expectedHeight = expectedHeightOfPersistentSetWithSize(persistentSet.size)
    return 1 shl ((expectedHeight / 2) * logBranchingFactor)
}
