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

import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder
import org.openjdk.jmh.runner.options.OptionsBuilder
import org.openjdk.jmh.runner.options.TimeValue
import java.util.concurrent.TimeUnit


private fun <T> systemProperty(name: String, transform: String.() -> T): T?
        = System.getProperty(name)?.transform()

private fun timeSystemProperty(name: String): TimeValue?
        = systemProperty(name) { toLong().let { TimeValue.milliseconds(it) } }

private fun intSystemProperty(name: String): Int?
        = systemProperty(name) { toInt() }

private fun arraySystemProperty(name: String): Array<String>?
        = systemProperty(name) { split(",").toTypedArray() }

private val isRemoteSystemProperty: Boolean
        = systemProperty("remote") { toBoolean() } ?: false

val referenceBenchmarkResultsDirectory: String = if (isRemoteSystemProperty)
    remoteReferenceBenchmarkResultsDirectory
else
    localReferenceBenchmarkResultsDirectory

fun ChainedOptionsBuilder.defaultOptions(): ChainedOptionsBuilder = this
        .jvmArgs(*jvmArgs)
        .addProfiler("gc")
        .param(sizeParam, *(arraySystemProperty(sizeParam) ?: sizeParamValues))
        .param(hashCodeTypeParam, *(arraySystemProperty(hashCodeTypeParam) ?: hashCodeTypeParamValues))
        .param(immutablePercentageParam, *(arraySystemProperty(immutablePercentageParam) ?: immutablePercentageParamValues))
        .forks(intSystemProperty("forks") ?: forks)
        .warmupIterations(intSystemProperty("warmupIterations") ?: warmupIterations)
        .measurementIterations(intSystemProperty("measurementIterations") ?: measurementIterations)
        .warmupTime(timeSystemProperty("warmupTime") ?: warmupTime)
        .measurementTime(timeSystemProperty("measurementTime") ?: measurementTime)
        .mode(Mode.AverageTime)
        .timeUnit(TimeUnit.MICROSECONDS)

inline fun runBenchmarks(outputFileName: String, configure: ChainedOptionsBuilder.() -> ChainedOptionsBuilder) {
    val options = OptionsBuilder()
            .defaultOptions()
            .configure()
            .build()

    val outputPath = "$benchmarkResultsDirectory/$outputFileName"
    val regressionReferencePath = "$referenceBenchmarkResultsDirectory/$outputFileName"

    Runner(options).run().toBenchmarkResults()
            .also { printCsvResults(it, "$outputPath.csv") }
            .let { calculateRegression(it, "$regressionReferencePath.csv") }
            ?.also { printReport(it, System.out, descendingScoreRegress = true) }
            ?.also { printCsvResults(it, "$outputPath-regression.csv") }
}
