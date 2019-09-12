/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks

import kotlin.math.floor

const val IP_100 = "100.0"
const val IP_99_09 = "99.09"
const val IP_95 = "95.0"
const val IP_70 = "70.0"
const val IP_50 = "50.0"
const val IP_30 = "30.0"
const val IP_0 = "0.0"


fun immutableSize(size: Int, immutablePercentage: Double): Int {
    return floor(size * immutablePercentage / 100.0).toInt()
}