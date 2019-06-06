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

fun printReport(regression: List<List<String>>, out: PrintStream, descendingScore: Boolean) {
    val header = regression.first()

    val scoreColumn = header.indexOf(benchmarkScore)
    val runResults = regression.drop(1).let { res ->
        if (descendingScore) res.sortedByDescending { it[scoreColumn].toDouble() } else res
    }

    // determine name column length
    val nameColumn = header.indexOf(benchmarkMethod)
    var nameLen = benchmarkMethod.length
    for (prefix in regression.map { it[nameColumn] }) {
        nameLen = Math.max(nameLen, prefix.length)
    }

    // determine param lengths
    val params = header.withIndex().filter {
        it.value !in listOf(benchmarkMethod, benchmarkScore, benchmarkScoreError, benchmarkAllocRate)
    }
    val paramLengths = params.associate { it.value to "(${it.value})".length + COLUMN_PAD }.toMutableMap()

    for (res in runResults) {
        for (p in params) {
            paramLengths[p.value] = Math.max(paramLengths[p.value]!!, res[p.index].length + COLUMN_PAD)
        }
    }

    // determine column lengths for other columns
    var scoreLen = benchmarkScore.length
    val scoreErrColumn = header.indexOf(benchmarkScoreError)
    var scoreErrLen = benchmarkScoreError.length
    val allocRateColumn = header.indexOf(benchmarkAllocRate)
    var allocRateLen = benchmarkAllocRate.length

    for (res in runResults) {
        scoreLen = Math.max(scoreLen, res[scoreColumn].length)
        scoreErrLen = Math.max(scoreErrLen, res[scoreErrColumn].length)
        allocRateLen = Math.max(allocRateLen, res[allocRateColumn].length)
    }
    scoreLen += COLUMN_PAD
    scoreErrLen += COLUMN_PAD - 1 // digest a single character for +- separator
    allocRateLen += COLUMN_PAD

    // print header
    out.printf("%-" + nameLen + "s", benchmarkMethod)
    for (p in params) {
        out.printf("%" + paramLengths[p.value] + "s", "(${p.value})")
    }

    out.printf("%" + scoreLen + "s", benchmarkScore)
    out.print("  ")
    out.printf("%" + scoreErrLen + "s", benchmarkScoreError)
    out.printf("%" + allocRateLen + "s", benchmarkAllocRate)
    out.println()

    // print benchmark results
    for (res in runResults) {
        out.printf("%-" + nameLen + "s", res[nameColumn])

        for (p in params) {
            out.printf("%" + paramLengths[p.value] + "s", res[p.index])
        }

        out.printf("%" + scoreLen + "s", res[scoreColumn])

        out.print(" \u00B1")
        out.printf("%" + scoreErrLen + "s", res[scoreErrColumn])

        out.printf("%" + allocRateLen + "s", res[allocRateColumn])
        out.println()
    }
}