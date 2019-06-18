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

import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder
import org.openjdk.jmh.runner.options.OptionsBuilder
import org.openjdk.jmh.runner.options.TimeValue


private fun <T> env(name: String, transform: String.() -> T): T?
        = System.getenv(name)?.transform()

private fun timeEnv(name: String): TimeValue?
        = env(name) { toLong().let { TimeValue.milliseconds(it) } }

private fun intEnv(name: String): Int?
        = env(name) { toInt() }

private fun arrayEnv(name: String): Array<String>?
        = env(name) { split(",").toTypedArray() }

fun ChainedOptionsBuilder.defaultOptions(): ChainedOptionsBuilder = this
        .jvmArgs(*jvmArgs)
        .addProfiler("gc")
        .param(sizeParam, *(arrayEnv(sizeParam) ?: sizeParamValues))
        .param(hashCodeTypeParam, *(arrayEnv(hashCodeTypeParam) ?: hashCodeTypeParamValues))
        .param(immutablePercentageParam, *(arrayEnv(immutablePercentageParam) ?: immutablePercentageParamValues))
        .forks(intEnv("forks") ?: forks)
        .warmupIterations(intEnv("wi") ?: warmupIterations)
        .measurementIterations(intEnv("i") ?: measurementIterations)
        .warmupTime(timeEnv("w") ?: warmupTime)
        .measurementTime(timeEnv("r") ?: measurementTime)

inline fun runBenchmarks(outputPath: String, regressionReferencePath: String, configure: ChainedOptionsBuilder.() -> ChainedOptionsBuilder) {
    val options = OptionsBuilder()
            .defaultOptions()
            .configure()
            .build()

    Runner(options).run()
            .also { printCsvResults(it, outputPath) }
            .also { calculateRegression(regressionReferencePath, outputPath) }
}
