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

package generators

import generators.immutableList.*
import generators.immutableList.impl.*
import generators.immutableListBuilder.*
import generators.immutableListBuilder.impl.*
import generators.immutableMap.*
import generators.immutableMap.impl.*
import generators.immutableMapBuilder.*
import generators.immutableMapBuilder.impl.*
import generators.immutableSet.*
import generators.immutableSet.impl.*
import generators.immutableSetBuilder.*
import generators.immutableSetBuilder.impl.*
import org.xml.sax.InputSource
import java.io.File
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.xpath.XPathFactory


private const val BENCHMARKS_ROOT = "src/jmh/java/"


abstract class SourceGenerator {
    abstract val outputFileName: String
    open fun getPackage(): String = "benchmarks"

    fun generate(out: PrintWriter) {
        out.println(readCopyrightNoticeFromProfile(File(".idea/copyright/apache_2_0.xml")))
        out.println("// Auto-generated file. DO NOT EDIT!")
        out.println()
        out.println("package ${getPackage()}")
        out.println()
        imports.forEach {
            out.println("import $it")
        }
        out.println()

        generateBody(out)
    }

    protected abstract val imports: Set<String>
    protected abstract fun generateBody(out: PrintWriter): Unit
}

fun readCopyrightNoticeFromProfile(copyrightProfile: File): String {
    val template = copyrightProfile.reader().use { reader ->
        XPathFactory.newInstance().newXPath().evaluate("/component/copyright/option[@name='notice']/@value", InputSource(reader))
    }
    val yearTemplate = "&#36;today.year"
    val year = java.time.LocalDate.now().year.toString()
    assert(yearTemplate in template)

    return template.replace(yearTemplate, year).lines().joinToString("", prefix = "/*\n", postfix = " */\n") { " * $it\n" }
}


abstract class BenchmarkSourceGenerator: SourceGenerator() {
    override val imports: Set<String> = setOf(
            "org.openjdk.jmh.annotations.*",
            "java.util.concurrent.TimeUnit",
            "org.openjdk.jmh.infra.Blackhole",
            "benchmarks.*"
    )

    override fun generateBody(out: PrintWriter) {
        generateBenchmark(out, "@State(Scope.Thread)")
    }

    protected abstract fun generateBenchmark(out: PrintWriter, header: String)
}


private val listImpls = listOf(
        KotlinListImplementation,
        PaguroListImplementation,
        CyclopsListImplementation,
        ClojureListImplementation,
        ScalaListImplementation,
        VavrListImplementation
)
private val listBuilderImpls = listOf(
        KotlinListBuilderImplementation,
        PaguroListBuilderImplementation,
        ClojureListBuilderImplementation
)

private val mapImpls = listOf(
        KotlinMapImplementation,
        KotlinOrderedMapImplementation,
        CapsuleMapImplementation,
        PaguroMapImplementation,
        PaguroSortedMapImplementation,
        CyclopsMapImplementation,
        CyclopsOrderedMapImplementation,
        CyclopsTrieMapImplementation,
        ClojureMapImplementation,
        ClojureSortedMapImplementation,
        ScalaMapImplementation,
        ScalaSortedMapImplementation,
        VavrMapImplementation,
        VavrSortedMapImplementation,
        VavrOrderedMapImplementation
)
private val mapBuilderImpls = listOf(
        KotlinMapBuilderImplementation,
        KotlinOrderedMapBuilderImplementation,
        CapsuleMapBuilderImplementation,
        PaguroMapBuilderImplementation,
        ClojureMapBuilderImplementation
)

private val setImpls = listOf(
        KotlinSetImplementation,
        KotlinOrderedSetImplementation,
        CapsuleSetImplementation,
        CyclopsSetImplementation,
        CyclopsTrieSetImplementation,
        CyclopsSortedSetImplementation,
        ClojureSetImplementation,
        ClojureSortedSetImplementation,
        ScalaSetImplementation,
        ScalaSortedSetImplementation,
        VavrSetImplementation,
        VavrSortedSetImplementation,
        VavrOrderedSetImplementation
)
private val setBuilderImpls = listOf(
        KotlinSetBuilderImplementation,
        KotlinOrderedSetBuilderImplementation,
        CapsuleSetBuilderImplementation,
        ClojureSetBuilderImplementation
)

fun generateBenchmarks() {
    val listBenchmarks = listImpls.map {
        ListBenchmarksGenerator(it)
    }
    val listBuilderBenchmarks = listBuilderImpls.map {
        ListBuilderBenchmarksGenerator(it)
    }

    val mapBenchmarks = mapImpls.map {
        MapBenchmarksGenerator(it)
    }
    val mapBuilderBenchmarks = mapBuilderImpls.map {
        MapBuilderBenchmarksGenerator(it)
    }

    val setBenchmarks = setImpls.map {
        SetBenchmarksGenerator(it)
    }
    val setBuilderBenchmarks = setBuilderImpls.map {
        SetBuilderBenchmarksGenerator(it)
    }

    val commonUtils = listOf(
            IntWrapperGenerator(),
            CommonUtilsGenerator()
    )


    val allGenerators = listOf(
            commonUtils,
            listBenchmarks,
            listBuilderBenchmarks,
            mapBenchmarks,
            mapBuilderBenchmarks,
            setBenchmarks,
            setBuilderBenchmarks
    ).flatten()

    allGenerators.forEach { generator ->
        val path = generator.getPackage().replace('.', '/') + "/" + generator.outputFileName + ".kt"
        val file = File(BENCHMARKS_ROOT + path)
        file.parentFile?.mkdirs()
        val out = PrintWriter(file)
        generator.generate(out)
        out.flush()
    }
}


fun main() {
    Files.walk(Paths.get(BENCHMARKS_ROOT))
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach { it.delete() }

    generateBenchmarks()
}