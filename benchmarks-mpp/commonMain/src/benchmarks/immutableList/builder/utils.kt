/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableList.builder

import kotlinx.collections.immutable.PersistentList
import benchmarks.immutableList.persistentListAdd
import benchmarks.immutableSize

fun persistentListBuilderAdd(size: Int, immutablePercentage: Double): PersistentList.Builder<String> {
    val immutableSize = immutableSize(size, immutablePercentage)
    val builder = persistentListAdd(immutableSize).builder()
    repeat(times = size - immutableSize) {
        builder.add("some element")
    }
    return builder
}