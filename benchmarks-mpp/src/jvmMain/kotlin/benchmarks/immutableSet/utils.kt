/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableSet

import benchmarks.*
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.persistentSetOf


fun <E> emptyPersistentSet(implementation: String): PersistentSet<E> = when (implementation) {
    HASH_IMPL -> persistentHashSetOf()
    ORDERED_IMPL -> persistentSetOf()
    else -> throw AssertionError("Unknown PersistentSet implementation: $implementation")
}

fun <E> persistentSetAdd(implementation: String, elements: List<E>): PersistentSet<E> {
    var set = emptyPersistentSet<E>(implementation)
    for (element in elements) {
        set = set.add(element)
    }
    return set
}
