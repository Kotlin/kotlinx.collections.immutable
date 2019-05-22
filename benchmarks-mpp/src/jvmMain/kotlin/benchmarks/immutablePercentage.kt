/*
 * Copyright 2016-2019 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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