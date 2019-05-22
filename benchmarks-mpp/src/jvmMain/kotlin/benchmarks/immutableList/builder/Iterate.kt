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

package benchmarks.immutableList.builder

import benchmarks.*
import kotlinx.collections.immutable.persistentListOf
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole

@State(Scope.Benchmark)
open class Iterate {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000, BM_10000000)
    var size: Int = 0

    @Param(IP_100, IP_99_09, IP_95, IP_70, IP_50, IP_30, IP_0)
    var immutablePercentage: Double = 0.0

    private var builder = persistentListOf<String>().builder()

    @Setup(Level.Trial)
    fun prepare() {
        builder = persistentListBuilderAdd(size, immutablePercentage)
    }

    @Benchmark
    fun firstToLast(bh: Blackhole) {
        for (e in builder) {
            bh.consume(e)
        }
    }

    @Benchmark
    fun lastToFirst(bh: Blackhole) {
        val iterator = builder.listIterator(size)

        while (iterator.hasPrevious()) {
            bh.consume(iterator.previous())
        }
    }
}
