/*
 * Copyright 2016-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.internal

import kotlin.jvm.JvmName

/**
 * This should not be needed after KT-30016 and KT-45673 are fixed
 */
internal object MapImplementation {
    internal fun <K, V, K1, V1> containsEntry(map: Map<K, V>, element: Map.Entry<K1, V1>): Boolean {
        @Suppress("USELESS_CAST")
        if ((element as Any?) !is Map.Entry<*, *>) return false
        return map[element.key]?.let { candidate -> candidate == element.value }
            ?: (element.value == null && map.containsKey(element.key))
    }

    @JvmName("containsMutableEntry")
    internal fun <K, V, K1, V1> containsEntry(map: MutableMap<K, V>, element: MutableMap.MutableEntry<K1, V1>): Boolean {
        @Suppress("USELESS_CAST")
        if ((element as Any?) !is MutableMap.MutableEntry<*, *>) return false
        return map[element.key]?.let { candidate -> candidate == element.value }
            ?: (element.value == null && map.containsKey(element.key))
    }

    internal fun <K, V> hashCode(map: Map<K, V>) = map.entries.hashCode()
}

