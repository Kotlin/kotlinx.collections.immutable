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

import org.openjdk.jmh.results.RunResult
import java.io.File
import java.io.FileWriter


fun printCsvResults(runResults: Collection<RunResult>, outputPath: String) {
    val paramsKeys = runResults.first().params.paramsKeys

    val csvHeader = "$benchmarkMethod,${paramsKeys.joinToString(",")},$benchmarkScore,$benchmarkScoreError,$benchmarkAllocRate"

    File(outputPath).parentFile?.mkdirs()
    val fileWriter = FileWriter(outputPath)

    fileWriter.appendln(csvHeader)
    runResults.forEach { fileWriter.appendln(csvRowFrom(it, paramsKeys)) }

    fileWriter.flush()
    fileWriter.close()
}

private fun csvRowFrom(result: RunResult, paramsKeys: Collection<String>): String {
    if (paramsKeys != result.params.paramsKeys)
        throw AssertionError("Benchmark runs should have same parameters")

    val nanosInMicros = 1000
    val benchmark = result.params.benchmark
    val paramValues = paramsKeys.map { result.params.getParam(it) }
    val size = result.params.getParam(sizeParam).toInt()
    val score = result.primaryResult.getScore() * nanosInMicros / size
    val scoreError = result.primaryResult.getScoreError() * nanosInMicros / size
    val allocationRate = result.secondaryResults["·gc.alloc.rate.norm"]!!.getScore() / size

    return "$benchmark,${paramValues.joinToString(",")},%.3f,%.3f,%.3f".format(score, scoreError, allocationRate)
}