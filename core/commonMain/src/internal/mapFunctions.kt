/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.internal

/** Checks if this map contains the specified [element] entry. */
internal fun <K, V> Map<K, V>.containsEntry(element: Map.Entry<K, V>): Boolean =
    this[element.key]?.let { candidate -> candidate == element.value }
        ?: (element.value == null && containsKey(element.key))
