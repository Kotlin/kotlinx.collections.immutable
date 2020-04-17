/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.time.*

@UseExperimental(ExperimentalTime::class)
abstract class ExecutionTimeMeasuringTest {
    private var clockMark: TimeMark? = null

    private fun markExecutionStart() {
        clockMark = TimeSource.Monotonic.markNow()
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