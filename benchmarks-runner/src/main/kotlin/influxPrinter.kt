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

import java.io.File
import java.io.FileWriter


fun printInfluxResults(benchmarkResults: BenchmarkResults, outputPath: String) {
    File(outputPath).parentFile?.mkdirs()
    val fileWriter = FileWriter(outputPath)

    val measurement = "kotlinx_collections_immutable"
    val timestamp = System.currentTimeMillis()

    benchmarkResults.runResults.forEach { res ->
        val line = buildString {
            append(measurement)
            append(',')

            append(benchmarkMethod)
            append('=')
            append(res.benchmark)

            res.params.forEach { param ->
                append(',')
                append(param.key)
                append('=')
                append(param.value)
            }
            append(' ')

            append(benchmarkScore)
            append('=')
            append(res.score.formatted())
            append(',')

            append(benchmarkScoreError)
            append('=')
            append(res.scoreError.formatted())
            append(',')

            append(benchmarkAllocRate)
            append('=')
            append(res.allocRate.formatted())
            append(' ')

            append(timestamp)
        }

        fileWriter.appendln(line)
    }

    fileWriter.flush()
    fileWriter.close()
}