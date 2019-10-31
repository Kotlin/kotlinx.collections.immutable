/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

import org.openjdk.jmh.runner.options.TimeValue


const val localReferenceBenchmarkResultsDirectory = "localReferenceBenchmarkResults"
const val remoteReferenceBenchmarkResultsDirectory = "remoteReferenceBenchmarkResults"
const val benchmarkResultsDirectory = "benchmarkResults"

const val hashMapOutputFileName = "hashMap"
const val hashMapBuilderOutputFileName = "hashMapBuilder"
const val orderedMapOutputFileName = "orderedMap"
const val orderedMapBuilderOutputFileName = "orderedMapBuilder"

const val hashSetOutputFileName = "hashSet"
const val hashSetBuilderOutputFileName = "hashSetBuilder"
const val orderedSetOutputFileName = "orderedSet"
const val orderedSetBuilderOutputFileName = "orderedSetBuilder"

const val listOutputFileName = "list"
const val listBuilderOutputFileName = "listBuilder"


const val benchmarkMethod = "Benchmark"
const val benchmarkScore = "Score(ns/op)"
const val benchmarkScoreError = "ScoreError(ns/op)"
const val benchmarkAllocRate = "AllocRate(B/op)"

const val benchmarkScoreRegressPercent = "Score(%)"
const val benchmarkAllocRateRegressPercent = "AllocRate(%)"


const val sizeParam = "size"
const val hashCodeTypeParam = "hashCodeType"
const val implementationParam = "implementation"
const val immutablePercentageParam = "immutablePercentage"


val jvmArgs = arrayOf("-Xms2048m", "-Xmx2048m")

const val forks = 1
const val warmupIterations = 10
const val measurementIterations = 20
val warmupTime = TimeValue.milliseconds(500)!!
val measurementTime = TimeValue.milliseconds(1000)!!

val sizeParamValues = arrayOf("1", "10", "100", "1000", "10000", "100000", "1000000")
val hashCodeTypeParamValues = arrayOf("ascending", "random", "collision", "nonExisting")
val immutablePercentageParamValues = arrayOf("0.0", "20.0", "50.0", "90.0")
