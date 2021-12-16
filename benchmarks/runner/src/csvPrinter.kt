/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

import java.io.File
import java.io.FileReader
import java.io.FileWriter


fun printCsvResults(benchmarkResults: BenchmarkResults, outputPath: String) {
    val paramsNamesString = benchmarkResults.paramsNames.joinToString(",")
    val csvHeader = "$benchmarkMethod,$paramsNamesString,$benchmarkScore,$benchmarkScoreError,$benchmarkAllocRate"

    File(outputPath).parentFile?.mkdirs()
    val fileWriter = FileWriter(outputPath)

    fileWriter.appendLine(csvHeader)
    benchmarkResults.runResults.forEach { res ->
        val paramsValuesString = benchmarkResults.paramsNames.joinToString(",") { res.paramValue(it) }
        val csvRow = "${res.benchmark},$paramsValuesString,${res.score.formatted()},${res.scoreError.formatted()},${res.allocRate.formatted()}"
        fileWriter.appendLine(csvRow)
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