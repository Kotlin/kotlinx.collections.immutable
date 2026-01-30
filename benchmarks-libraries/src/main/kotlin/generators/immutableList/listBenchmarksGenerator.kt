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

package generators.immutableList

import generators.BenchmarkSourceGenerator
import generators.immutableList.impl.ListImplementation
import generators.immutableList.impl.listElement
import generators.immutableList.impl.listNewElement
import java.io.PrintWriter

class ListBenchmarksGenerator(private val impl: ListImplementation) : BenchmarkSourceGenerator() {
    override val outputFileName: String get() = "List"

    override fun getPackage(): String {
        return super.getPackage() + ".immutableList." + impl.packageName
    }

    override fun generateBenchmark(out: PrintWriter, header: String) {
        out.println("""
$header
open class Add {
    @Param("10000", "100000")
    var size: Int = 0

    @Benchmark
    fun addLast(): ${impl.type()} {
        return persistentListAdd(size)
    }

    @Benchmark
    fun addLastAndIterate(bh: Blackhole) {
        val list = persistentListAdd(size)
        for (e in list) {
            bh.consume(e)
        }
    }

    @Benchmark
    fun addLastAndGet(bh: Blackhole) {
        val list = persistentListAdd(size)
        for (i in 0 until size) {
            bh.consume(${impl.getOperation("list", "i")})
        }
    }
}


$header
open class Get {
    @Param("10000", "100000")
    var size: Int = 0

    private var persistentList = ${impl.empty()}

    @Setup(Level.Trial)
    fun prepare() {
        persistentList = persistentListAdd(size)
    }

    @Benchmark
    fun getByIndex(bh: Blackhole) {
        for (i in 0 until size) {
            bh.consume(${impl.getOperation("persistentList", "i")})
        }
    }
}


$header
open class Iterate {
    @Param("10000", "100000")
    var size: Int = 0

    private var persistentList = ${impl.empty()}

    @Setup(Level.Trial)
    fun prepare() {
        persistentList = persistentListAdd(size)
    }

    @Benchmark
    fun firstToLast(bh: Blackhole) {
        for (e in persistentList) {
            bh.consume(e)
        }
    }
${impl.iterateLastToFirst("persistentList", "size")}
}


$header
open class Remove {
    @Param("10000", "100000")
    var size: Int = 0

    private var persistentList = ${impl.empty()}

    @Setup(Level.Trial)
    fun prepare() {
        persistentList = persistentListAdd(size)
    }

    @Benchmark
    fun removeLast(): ${impl.type()} {
        var list = persistentList
        repeat(times = size) {
            list = ${impl.removeLastOperation("list")}
        }
        return list
    }
}


$header
open class Set {
    @Param("10000", "100000")
    var size: Int = 0

    private var persistentList = ${impl.empty()}
    private var randomIndices = listOf<Int>()

    @Setup(Level.Trial)
    fun prepare() {
        persistentList = persistentListAdd(size)
        randomIndices = List(size) { it }.shuffled()
    }

    @Benchmark
    fun setByIndex(): ${impl.type()} {
        repeat(times = size) { index ->
            persistentList = ${impl.setOperation("persistentList", "index", listNewElement)}
        }
        return persistentList
    }

    @Benchmark
    fun setByRandomIndex(): ${impl.type()} {
        repeat(times = size) { index ->
            persistentList = ${impl.setOperation("persistentList", "randomIndices[index]", listNewElement)}
        }
        return persistentList
    }
}


private fun persistentListAdd(size: Int): ${impl.type()} {
    var list = ${impl.empty()}
    repeat(times = size) {
        list = ${impl.addOperation("list", listElement)}
    }
    return list
}
        """.trimIndent()
        )
    }
}