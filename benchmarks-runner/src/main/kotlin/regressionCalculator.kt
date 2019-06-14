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

import java.io.*
import java.nio.file.Files


fun calculateRegression(referencePath: String, targetPath: String) {
    val referenceFile = File(referencePath)
    val targetFile = File(targetPath)

    val targetResults = readResults(targetFile)
    if (targetResults.isEmpty()) {
        throw IllegalArgumentException("Target file is empty: $targetPath")
    }

    println("\n")

    if (!referenceFile.exists()) {
        println("No reference file exists to calculate regression against")
        println("Copying the target benchmark results to the reference file: $referencePath")
        referenceFile.parentFile?.mkdirs()
        Files.copy(targetFile.toPath(), referenceFile.toPath())
        return
    }

    val referenceResults = readResults(referenceFile)
    if (referenceResults.isEmpty()) {
        throw IllegalArgumentException("Reference file is empty: $referencePath")
    }

    println("Calculating regression...\n")
    val regressionResults = calculateRegression(referenceResults, targetResults)

    printReport(regressionResults, System.out, descendingScore = true)

    val regressionFile = targetFile.let {
        File(it.parent, "${it.nameWithoutExtension}-regression.${it.extension}")
    }
    writeResults(regressionResults, regressionFile)

    println("\nRegression results are saved to ${regressionFile.absolutePath}\n")
}

private fun calculateRegression(referenceResults: List<List<String>>, targetResults: List<List<String>>): List<List<String>> {
    require(referenceResults.isNotEmpty() && targetResults.isNotEmpty())

    if (referenceResults.first() != targetResults.first()) {
        throw IllegalArgumentException("Benchmark results have different headers")
    }

    val header = referenceResults.first()

    val scoreColumn = header.indexOf(benchmarkScore)
    val scoreErrorColumn = header.indexOf(benchmarkScoreError)
    val allocRateColumn = header.indexOf(benchmarkAllocRate)

    val regression = targetResults.drop(1).map { targetRow ->
        val reference = referenceResults.firstOrNull { referenceRow ->
            targetRow.withIndex().all {
                it.value == referenceRow[it.index] || it.index in listOf(scoreColumn, scoreErrorColumn, allocRateColumn)
            }
        }

        val resultRow = targetRow.toMutableList()
        if (reference == null) {
            resultRow[scoreColumn] = "NaN"
            resultRow[scoreErrorColumn] = "NaN"
            resultRow[allocRateColumn] = "NaN"
        } else {
            resultRow[scoreColumn] = "%.3f".format(targetRow[scoreColumn].toDouble() - reference[scoreColumn].toDouble())
            resultRow[scoreErrorColumn] = "%.3f".format(targetRow[scoreErrorColumn].toDouble() + reference[scoreErrorColumn].toDouble())
            resultRow[allocRateColumn] = "%.3f".format(targetRow[allocRateColumn].toDouble() - reference[allocRateColumn].toDouble())
        }
        return@map resultRow
    }

    return listOf(header) + regression
}

private fun readResults(file: File): List<List<String>> {
    val fileReader = FileReader(file)

    val results = mutableListOf<List<String>>()
    fileReader.forEachLine {
        if (it.isNotBlank()) {
            results.add(it.split(','))
        }
    }

    fileReader.close()

    return results
}

private fun writeResults(regression: List<List<String>>, regressionFile: File) {
    val fileWriter = FileWriter(regressionFile)

    regression.forEach {
        fileWriter.appendln(it.joinToString(","))
    }

    fileWriter.flush()
    fileWriter.close()
}