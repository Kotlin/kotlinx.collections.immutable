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


class BenchmarkResults(
    val paramsNames: List<String>,
    val runResults: List<BenchmarkRunResult>
)

fun Collection<RunResult>.toBenchmarkResults(): BenchmarkResults {
    val paramsNames = first().params.paramsKeys

    check(all { it.params.paramsKeys == paramsNames })

    return BenchmarkResults(paramsNames.toList(), map(RunResult::toBenchmarkRunResult))
}


class BenchmarkRunResult(
        val benchmark: String,
        val params: Map<String, String>,
        val score: Double,
        val scoreError: Double,
        val allocRate: Double
) {
    fun paramValue(paramName: String): String = params.getValue(paramName)
}

private fun RunResult.toBenchmarkRunResult(): BenchmarkRunResult {
    val allocRateLabel = "·gc.alloc.rate.norm"
    val allocRate = secondaryResults[allocRateLabel]!!

    check(primaryResult.getScoreUnit() == "us/op")
    check(allocRate.getScoreUnit() == "B/op")

    val nanosInMicros = 1000
    val size = params.getParam(sizeParam).toInt()

    return BenchmarkRunResult(
            benchmark = params.benchmark,
            params = params.paramsKeys.associateWith { params.getParam(it) },
            score = primaryResult.getScore() * nanosInMicros / size,
            scoreError = primaryResult.getScoreError() * nanosInMicros / size,
            allocRate = allocRate.getScore() / size
    )
}