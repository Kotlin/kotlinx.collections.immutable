/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableMap.builder

import benchmarks.*
import benchmarks.immutableMap.emptyPersistentMap
import kotlinx.collections.immutable.PersistentMap


fun persistentMapBuilderPut(
        implementation: String,
        keys: List<IntWrapper>,
        immutablePercentage: Double
): PersistentMap.Builder<IntWrapper, String> {
    val immutableSize = immutableSize(keys.size, immutablePercentage)

    var map = emptyPersistentMap<IntWrapper, String>(implementation)
    for (index in 0 until immutableSize) {
        map = map.put(keys[index], "some value")
    }

    val builder = map.builder()
    for (index in immutableSize until keys.size) {
        builder[keys[index]] = "some value"
    }

    return builder
}
