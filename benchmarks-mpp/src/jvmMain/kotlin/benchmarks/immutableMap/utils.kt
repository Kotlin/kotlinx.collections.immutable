/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableMap

import benchmarks.*
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentMapOf


fun <K, V> emptyPersistentMap(implementation: String): PersistentMap<K, V> = when (implementation) {
    HASH_IMPL -> persistentHashMapOf()
    ORDERED_IMPL -> persistentMapOf()
    else -> throw AssertionError("Unknown PersistentMap implementation: $implementation")
}

fun <K> persistentMapPut(implementation: String, keys: List<K>): PersistentMap<K, String> {
    var map = emptyPersistentMap<K, String>(implementation)
    for (key in keys) {
        map = map.put(key, "some element")
    }
    return map
}
