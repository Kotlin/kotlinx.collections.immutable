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

// Auto-generated file. DO NOT EDIT!

package benchmarks.immutableList.clojure.builder

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.infra.Blackhole
import benchmarks.*

@State(Scope.Thread)
open class Add {
    @Param("10000", "100000")
    var size: Int = 0

    @Param("0.0", "50.0")
    var immutablePercentage: Double = 0.0

    @Benchmark
    fun addLast(): clojure.lang.ITransientVector {
        return persistentListBuilderAdd(size, immutablePercentage)
    }

    @Benchmark
    fun addLastAndGet(bh: Blackhole) {
        val builder = persistentListBuilderAdd(size, immutablePercentage)
        for (i in 0 until size) {
            bh.consume(builder.valAt(i))
        }
    }


}


@State(Scope.Thread)
open class Get {
    @Param("10000", "100000")
    var size: Int = 0

    @Param("0.0", "50.0")
    var immutablePercentage: Double = 0.0

    private var builder: clojure.lang.ITransientVector = clojure.lang.PersistentVector.EMPTY.asTransient() as clojure.lang.ITransientVector

    @Setup(Level.Trial)
    fun prepare() {
        builder = persistentListBuilderAdd(size, immutablePercentage)
    }

    @Benchmark
    fun getByIndex(bh: Blackhole) {
        for (i in 0 until size) {
            bh.consume(builder.valAt(i))
        }
    }
}





@State(Scope.Thread)
open class Remove {
    @Param("10000", "100000")
    var size: Int = 0

    @Param("0.0", "50.0")
    var immutablePercentage: Double = 0.0

    @Benchmark
    fun addAndRemoveLast(): clojure.lang.ITransientVector {
        val builder = persistentListBuilderAdd(size, immutablePercentage)
        for (i in 0 until size) {
            builder.pop()
        }
        return builder
    }
}


@State(Scope.Thread)
open class Set {
    @Param("10000", "100000")
    var size: Int = 0

    @Param("0.0", "50.0")
    var immutablePercentage: Double = 0.0

    private var builder = clojure.lang.PersistentVector.EMPTY.asTransient() as clojure.lang.ITransientVector
    private var randomIndices = listOf<Int>()

    @Setup(Level.Trial)
    fun prepare() {
        builder = persistentListBuilderAdd(size, immutablePercentage)
        randomIndices = List(size) { it }.shuffled()
    }

    @Benchmark
    fun setByIndex(): clojure.lang.ITransientVector {
        for (i in 0 until size) {
            builder.assocN(i, "another element")
        }
        return builder
    }

    @Benchmark
    fun setByRandomIndex(): clojure.lang.ITransientVector {
        for (i in 0 until size) {
            builder.assocN(randomIndices[i], "another element")
        }
        return builder
    }
}


private fun persistentListBuilderAdd(size: Int, immutablePercentage: Double): clojure.lang.ITransientVector {
    val immutableSize = immutableSize(size, immutablePercentage)

    var list = clojure.lang.PersistentVector.EMPTY
    repeat(times = immutableSize) {
        list = list.cons("another element")
    }

    val builder = list.asTransient() as clojure.lang.ITransientVector
    repeat(times = size - immutableSize) {
        builder.conj("some element")
    }

    return builder
}
