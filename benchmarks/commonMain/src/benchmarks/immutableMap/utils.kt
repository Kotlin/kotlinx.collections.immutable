/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableMap

import benchmarks.*
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentMapOf
import kotlin.math.ceil
import kotlin.math.log


fun <K, V> emptyPersistentMap(implementation: String): PersistentMap<K, V> = when (implementation) {
    HASH_IMPL -> persistentHashMapOf()
    ORDERED_IMPL -> persistentMapOf()
    else -> throw AssertionError("Unknown PersistentMap implementation: $implementation")
}

fun <K> persistentMapPut(implementation: String, keys: List<K>): PersistentMap<K, String> {
    var map = emptyPersistentMap<K, String>(implementation)
    for (key in keys) {
        map = map.putting(key, "some value")
    }
    return map
}

fun <K> persistentMapRemove(persistentMap: PersistentMap<K, String>, keys: List<K>): PersistentMap<K, String> {
    var map = persistentMap
    for (key in keys) {
        map = map.remove(key)
    }
    return map
}


private const val branchingFactor = 32
private const val logBranchingFactor = 5

private fun expectedHeightOfPersistentMapWithSize(size: Int): Int {
    return ceil(log(size.toDouble(), branchingFactor.toDouble())).toInt()
}

/**
 * Returns the size of a persistent map whose expected height is
 * half of the specified [persistentMap]'s expected height.
 */
fun sizeForHalfHeight(persistentMap: PersistentMap<*, *>): Int {
    val expectedHeight = expectedHeightOfPersistentMapWithSize(persistentMap.size)
    return 1 shl ((expectedHeight / 2) * logBranchingFactor)
}
