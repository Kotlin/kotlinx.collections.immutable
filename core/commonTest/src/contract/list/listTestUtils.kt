/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.list

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.assertTrue

internal fun ownedBuilderOf(size: Int): PersistentList.Builder<Int> {
    val builder = persistentListOf<Int>().builder()
    for (element in 0..<size) {
        assertTrue(builder.add(element))
    }
    return builder
}
