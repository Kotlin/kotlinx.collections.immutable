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


fun calculateRegression(benchmarkResults: BenchmarkResults, referencePath: String): BenchmarkResults? {
    val referenceFile = File(referencePath)

    if (!referenceFile.exists()) {
        println("No reference file exists to calculate regression against")
        println("Writing the target benchmark results to the reference file: $referencePath")
        printCsvResults(benchmarkResults, referencePath)
        return null
    }

    val referenceResults = readCsvResults(referenceFile)

    println("Calculating regression...\n")
    return calculateRegression(referenceResults, benchmarkResults)
}

private fun calculateRegression(referenceResults: BenchmarkResults, targetResults: BenchmarkResults): BenchmarkResults {
    val paramsNames = targetResults.paramsNames

    check(referenceResults.paramsNames == paramsNames)

    val regression = targetResults.runResults.map { target ->
        val reference = referenceResults.runResults.firstOrNull { ref ->
            target.benchmark == ref.benchmark
                    && paramsNames.all { target.paramValue(it) == ref.paramValue(it) }
        }


        var regressScore = Double.NaN
        var regressScorePercent = Double.NaN
        var regressScoreError = Double.NaN
        var regressAllocRate = Double.NaN
        var regressAllocRatePercent = Double.NaN
        if (reference != null) {
            regressScore = target.score - reference.score
            regressScorePercent = 100 * regressScore / reference.score
            regressScoreError = target.scoreError + reference.scoreError
            regressAllocRate = target.allocRate - reference.allocRate
            regressAllocRatePercent = if (regressAllocRate > 0.1) 100 * regressAllocRate / reference.allocRate else 0.0
        }
        return@map BenchmarkRunResult(
                benchmark = target.benchmark,
                params = target.params + mapOf(
                        benchmarkScoreRegressPercent to regressScorePercent.formatted(),
                        benchmarkAllocRateRegressPercent to regressAllocRatePercent.formatted()
                ),
                score = regressScore,
                scoreError = regressScoreError,
                allocRate = regressAllocRate
        )
    }

    return BenchmarkResults(
            paramsNames = paramsNames + listOf(benchmarkScoreRegressPercent, benchmarkAllocRateRegressPercent),
            runResults = regression
    )
}
