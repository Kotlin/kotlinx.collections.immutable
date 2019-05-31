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

package generators.immutableListBuilder

import generators.BenchmarkSourceGenerator
import generators.immutableListBuilder.impl.ListBuilderImplementation
import generators.immutableListBuilder.impl.listBuilderElement
import generators.immutableListBuilder.impl.listBuilderNewElement
import java.io.PrintWriter

class ListBuilderBenchmarksGenerator(private val impl: ListBuilderImplementation) : BenchmarkSourceGenerator() {
    override val outputFileName: String = "ListBuilder"

    override fun getPackage(): String {
        return super.getPackage() + ".immutableList." + impl.packageName
    }

    override fun generateBenchmark(out: PrintWriter, header: String) {
        out.println("""
$header
open class Add {
    @Param("10000", "100000")
    var size: Int = 0

    @Param("0.0", "50.0")
    var immutablePercentage: Double = 0.0

    @Benchmark
    fun addLast(): ${impl.type()} {
        return persistentListBuilderAdd(size, immutablePercentage)
    }

    @Benchmark
    fun addLastAndGet(bh: Blackhole) {
        val builder = persistentListBuilderAdd(size, immutablePercentage)
        for (i in 0 until size) {
            bh.consume(${impl.getOperation("builder", "i")})
        }
    }

${
if (impl.isIterable) """
    @Benchmark
    fun addLastAndIterate(bh: Blackhole) {
        val builder = persistentListBuilderAdd(size, immutablePercentage)
        for (e in builder) {
            bh.consume(e)
        }
    }"""
else ""
}
}


$header
open class Get {
    @Param("10000", "100000")
    var size: Int = 0

    @Param("0.0", "50.0")
    var immutablePercentage: Double = 0.0

    private var builder: ${impl.type()} = ${impl.empty()}

    @Setup(Level.Trial)
    fun prepare() {
        builder = persistentListBuilderAdd(size, immutablePercentage)
    }

    @Benchmark
    fun getByIndex(bh: Blackhole) {
        for (i in 0 until size) {
            bh.consume(${impl.getOperation("builder", "i")})
        }
    }
}


${
if (impl.isIterable) """
$header
open class Iterate {
    @Param("10000", "100000")
    var size: Int = 0

    @Param("0.0", "50.0")
    var immutablePercentage: Double = 0.0

    private var builder = ${impl.empty()}

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
}"""
else ""
}


$header
open class Remove {
    @Param("10000", "100000")
    var size: Int = 0

    @Param("0.0", "50.0")
    var immutablePercentage: Double = 0.0

    @Benchmark
    fun addAndRemoveLast(): ${impl.type()} {
        val builder = persistentListBuilderAdd(size, immutablePercentage)
        for (i in 0 until size) {
            ${impl.removeLastOperation("builder")}
        }
        return builder
    }
}


$header
open class Set {
    @Param("10000", "100000")
    var size: Int = 0

    @Param("0.0", "50.0")
    var immutablePercentage: Double = 0.0

    private var builder = ${impl.empty()}
    private var randomIndices = listOf<Int>()

    @Setup(Level.Trial)
    fun prepare() {
        builder = persistentListBuilderAdd(size, immutablePercentage)
        randomIndices = List(size) { it }.shuffled()
    }

    @Benchmark
    fun setByIndex(): ${impl.type()} {
        for (i in 0 until size) {
            ${impl.setOperation("builder", "i", listBuilderNewElement)}
        }
        return builder
    }

    @Benchmark
    fun setByRandomIndex(): ${impl.type()} {
        for (i in 0 until size) {
            ${impl.setOperation("builder", "randomIndices[i]", listBuilderNewElement)}
        }
        return builder
    }
}


private fun persistentListBuilderAdd(size: Int, immutablePercentage: Double): ${impl.type()} {
    val immutableSize = immutableSize(size, immutablePercentage)

    var list = ${impl.immutableEmpty()}
    repeat(times = immutableSize) {
        list = ${impl.immutableAddOperation("list", listBuilderNewElement)}
    }

    val builder = ${impl.builderOperation("list")}
    repeat(times = size - immutableSize) {
        ${impl.addOperation("builder", listBuilderElement)}
    }

    return builder
}
        """.trimIndent()
        )
    }
}