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

package tests.stress

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.time.*

@UseExperimental(ExperimentalTime::class)
abstract class ExecutionTimeMeasuringTest {
    private var clockMark: ClockMark? = null

    private fun markExecutionStart() {
        clockMark = MonoClock.markNow()
    }

    private fun printExecutionTime() {
        val nonNullClockMark = clockMark ?: throw IllegalStateException("markExecutionStart() must be called first")
        val elapsed = nonNullClockMark.elapsedNow()

        if (elapsed > 3.seconds) {
            print("#".repeat(20) + " ")
        }
        println("Execution time: ${elapsed.toString(DurationUnit.MILLISECONDS)}")

        clockMark = null
    }

    @BeforeTest
    fun before() {
        markExecutionStart()
    }

    @AfterTest
    fun after() {
        printExecutionTime()
    }
}