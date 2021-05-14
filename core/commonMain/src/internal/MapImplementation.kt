/*
 * Copyright 2016-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.internal

internal object MapImplementation {
    internal fun <K, V> containsEntry(map: Map<K, V>, element: Map.Entry<K, V>): Boolean {
        @Suppress("USELESS_CAST")
        if ((element as Any?) !is Map.Entry<*, *>) return false
        return map[element.key]?.let { candidate -> candidate == element.value }
                ?: (element.value == null && map.containsKey(element.key))
    }
}
