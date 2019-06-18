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

import org.openjdk.jmh.runner.options.TimeValue


const val hashMapReferenceFilePath = "referenceBenchmarkResults/hashMap.cvs"
const val hashMapBuilderReferenceFilePath = "referenceBenchmarkResults/hashMapBuilder.cvs"
const val orderedMapReferenceFilePath = "referenceBenchmarkResults/orderedMap.cvs"
const val orderedMapBuilderReferenceFilePath = "referenceBenchmarkResults/orderedMapBuilder.cvs"

const val hashMapOutputFilePath = "benchmarkResults/hashMap.cvs"
const val hashMapBuilderOutputFilePath = "benchmarkResults/hashMapBuilder.cvs"
const val orderedMapOutputFilePath = "benchmarkResults/orderedMap.cvs"
const val orderedMapBuilderOutputFilePath = "benchmarkResults/orderedMapBuilder.cvs"

const val hashSetReferenceFilePath = "referenceBenchmarkResults/hashSet.cvs"
const val hashSetBuilderReferenceFilePath = "referenceBenchmarkResults/hashSetBuilder.cvs"
const val orderedSetReferenceFilePath = "referenceBenchmarkResults/orderedSet.cvs"
const val orderedSetBuilderReferenceFilePath = "referenceBenchmarkResults/orderedSetBuilder.cvs"

const val hashSetOutputFilePath = "benchmarkResults/hashSet.cvs"
const val hashSetBuilderOutputFilePath = "benchmarkResults/hashSetBuilder.cvs"
const val orderedSetOutputFilePath = "benchmarkResults/orderedSet.cvs"
const val orderedSetBuilderOutputFilePath = "benchmarkResults/orderedSetBuilder.cvs"

const val listReferenceFilePath = "referenceBenchmarkResults/list.cvs"
const val listBuilderReferenceFilePath = "referenceBenchmarkResults/listBuilder.cvs"

const val listOutputFilePath = "benchmarkResults/list.cvs"
const val listBuilderOutputFilePath = "benchmarkResults/listBuilder.cvs"


const val benchmarkMethod = "Benchmark"
const val benchmarkScore = "Score(ns/op)"
const val benchmarkScoreError = "ScoreError(ns/op)"
const val benchmarkAllocRate = "AllocRate(B/op)"

const val sizeParam = "size"
const val hashCodeTypeParam = "hashCodeType"
const val implementationParam = "implementation"
const val immutablePercentageParam = "immutablePercentage"


val jvmArgs = arrayOf("-Xms2048m", "-Xmx2048m")

const val forks = 1
const val warmupIterations = 20
const val measurementIterations = 20
val warmupTime = TimeValue.milliseconds(1000)!!
val measurementTime = TimeValue.milliseconds(1000)!!

val sizeParamValues = arrayOf("1", "10", "100", "1000", "10000", "100000", "1000000")
val hashCodeTypeParamValues = arrayOf("ascending", "random", "collision", "nonExisting")
val immutablePercentageParamValues = arrayOf("0.0", "20.0", "50.0", "90.0")
