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
import java.io.FileReader
import java.io.FileWriter


fun printCsvResults(benchmarkResults: BenchmarkResults, outputPath: String) {
    val paramsNamesString = benchmarkResults.paramsNames.joinToString(",")
    val csvHeader = "$benchmarkMethod,$paramsNamesString,$benchmarkScore,$benchmarkScoreError,$benchmarkAllocRate"

    File(outputPath).parentFile?.mkdirs()
    val fileWriter = FileWriter(outputPath)

    fileWriter.appendln(csvHeader)
    benchmarkResults.runResults.forEach { res ->
        val paramsValuesString = benchmarkResults.paramsNames.joinToString(",") { res.paramValue(it) }
        val csvRow = "${res.benchmark},$paramsValuesString,${res.score.formatted()},${res.scoreError.formatted()},${res.allocRate.formatted()}"
        fileWriter.appendln(csvRow)
    }

    fileWriter.flush()
    fileWriter.close()
}


fun readCsvResults(file: File): BenchmarkResults {
    val fileReader = FileReader(file)
    val lines = fileReader.readLines().map { it.split(',') }
    fileReader.close()

    check(lines.isNotEmpty())
    check(lines.all { it.size == lines.first().size })

    val header = lines.first()

    val benchmarkColumn = header.indexOf(benchmarkMethod)
    val scoreColumn = header.indexOf(benchmarkScore)
    val scoreErrorColumn = header.indexOf(benchmarkScoreError)
    val allocRateColumn = header.indexOf(benchmarkAllocRate)

    val paramsColumns = header.indices.filter {
        it !in listOf(benchmarkColumn, scoreColumn, scoreErrorColumn, allocRateColumn)
    }

    val runResults = lines.drop(1).map { line ->
        BenchmarkRunResult(
                benchmark = line[benchmarkColumn],
                params = paramsColumns.associate { header[it] to line[it] },
                score = line[scoreColumn].toDouble(),
                scoreError = line[scoreErrorColumn].toDouble(),
                allocRate = line[allocRateColumn].toDouble()
        )
    }

    return BenchmarkResults(paramsColumns.map { header[it] }, runResults)
}


fun Double.formatted(): String = "%.3f".format(this)