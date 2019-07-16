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

import java.io.PrintStream


private const val COLUMN_PAD = 2

fun printReport(regression: BenchmarkResults, out: PrintStream, descendingScoreRegress: Boolean) {
    val runResults = if (descendingScoreRegress) {
        regression.runResults.sortedByDescending { it.paramValue(benchmarkScoreRegressPercent).toDouble() }
    } else {
        regression.runResults
    }

    // determine columns lengths
    var nameLen = benchmarkMethod.length
    var scoreLen = benchmarkScore.length
    var scoreErrLen = benchmarkScoreError.length
    var allocRateLen = benchmarkAllocRate.length

    val paramsNames = regression.paramsNames
    val paramsLengths = paramsNames.associate { it to it.length + COLUMN_PAD }.toMutableMap()

    for (res in runResults) {
        nameLen = Math.max(nameLen, res.benchmark.length)
        scoreLen = Math.max(scoreLen, res.score.formatted().length)
        scoreErrLen = Math.max(scoreErrLen, res.scoreError.formatted().length)
        allocRateLen = Math.max(allocRateLen, res.allocRate.formatted().length)

        for (p in paramsNames) {
            paramsLengths[p] = Math.max(paramsLengths[p]!!, res.paramValue(p).length + COLUMN_PAD)
        }
    }
    scoreLen += COLUMN_PAD
    scoreErrLen += COLUMN_PAD - 1 // digest a single character for +- separator
    allocRateLen += COLUMN_PAD

    // print header
    out.printf("%-" + nameLen + "s", benchmarkMethod)
    for (p in paramsNames) {
        out.printf("%" + paramsLengths[p] + "s", p)
    }

    out.printf("%" + scoreLen + "s", benchmarkScore)
    out.print("  ")
    out.printf("%" + scoreErrLen + "s", benchmarkScoreError)
    out.printf("%" + allocRateLen + "s", benchmarkAllocRate)
    out.println()

    // print benchmark results
    for (res in runResults) {
        out.printf("%-" + nameLen + "s", res.benchmark)

        for (p in res.params) {
            out.printf("%" + paramsLengths[p.key] + "s", p.value)
        }

        out.printf("%" + scoreLen + "s", res.score.formatted())

        out.print(" \u00B1")
        out.printf("%" + scoreErrLen + "s", res.scoreError.formatted())

        out.printf("%" + allocRateLen + "s", res.allocRate.formatted())
        out.println()
    }
}