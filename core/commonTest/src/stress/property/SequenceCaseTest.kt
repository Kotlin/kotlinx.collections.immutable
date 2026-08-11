/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.property

import tests.IntWrapper
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** What a generated run has to satisfy before any property it checks means anything. */
class SequenceCaseTest {

    private fun kindOf(op: MapOp): String = when (op) {
        is MapOp.Put -> "put"
        is MapOp.Remove -> "remove"
        is MapOp.RemoveEntry -> "removeEntry"
        is MapOp.PutAll -> "putAll"
        is MapOp.MinusKeys -> "minusKeys"
        is MapOp.ThroughBuilder -> "throughBuilder"
        is MapOp.Clear -> "clear"
    }

    @Test
    fun everyOpKindIsGenerated() {
        val kinds = mutableSetOf<String>()
        for (seed in 0..<20) {
            val case = generateSequenceCase(Random(seed), maxShiftUniverse())
            for (op in case.ops) kinds += kindOf(op)
        }

        assertEquals(7, kinds.size, "the generator never produced ${kinds.size} of the op kinds: $kinds")
    }

    @Test
    fun aGeneratedRunBuildsSomethingAndStaysClean() {
        val case = generateSequenceCase(Random(1), wideUniverse(), slots = 3, length = 60)

        assertEquals(emptyList(), runSequenceCase(case))

        // A run whose maps stayed empty, or never diverged, would pass every property and prove
        // nothing. Replay it here to see that it does real work.
        val states = replayStates(case)
        assertTrue(states.any { it.isNotEmpty() }, "every slot stayed empty for the whole run")
        assertTrue(states.distinct().size >= 10, "the run only reached ${states.distinct().size} states")
    }

    private fun replayStates(case: SequenceCase): List<Map<IntWrapper?, Value?>> {
        val models = MutableList(case.slots) { LinkedHashMap<IntWrapper?, Value?>() }
        val states = mutableListOf<Map<IntWrapper?, Value?>>()
        for (op in case.ops) {
            val slot = case.slotOf(op.target)
            when (op) {
                is MapOp.Put -> models[slot][case.keyAt(op.key)] = valueOf(case.keyAt(op.key), op.salt)
                is MapOp.Remove -> models[slot].remove(case.keyAt(op.key))
                is MapOp.RemoveEntry -> {
                    val key = case.keyAt(op.key)
                    if (models[slot][key] == valueOf(key, op.salt)) models[slot].remove(key)
                }
                is MapOp.PutAll -> models[slot].putAll(models[case.slotOf(op.source)])
                is MapOp.MinusKeys -> for (key in models[case.slotOf(op.source)].keys.toList()) {
                    models[slot].remove(key)
                }
                is MapOp.ThroughBuilder -> models[slot][case.keyAt(op.key)] = valueOf(case.keyAt(op.key), op.salt)
                is MapOp.Clear -> models[slot].clear()
            }
            states += LinkedHashMap(models[slot])
        }
        return states
    }

    @Test
    fun theShrinkerKeepsOnlyWhatTheFailureNeeds() {
        val case = generateSequenceCase(Random(2), maxShiftUniverse(), slots = 3, length = 40)
        val marker = PropertyFailure("planted", "clear", "a clear is present")

        // Fails only while a clear is in the run, so everything else must come out.
        val (shrunk, failures) = shrinkSequence(case) { candidate ->
            if (candidate.ops.any { it is MapOp.Clear }) listOf(marker) else emptyList()
        }

        assertTrue(failures.isNotEmpty())
        assertEquals(1, shrunk.ops.size, "shrunk to ${shrunk.ops.size} ops: ${shrunk.ops.map { kindOf(it) }}")
        assertTrue(shrunk.ops.single() is MapOp.Clear)
        assertEquals(1, shrunk.slots)
    }

    @Test
    fun theReportRendersOneLinePerOp() {
        val case = generateSequenceCase(Random(3), maxShiftUniverse(), slots = 2, length = 6)
        val rendered = case.render()

        assertEquals(6, rendered.lines().count { it.trimStart().startsWith("m") && " = " in it })
        assertTrue("var m0 = persistentHashMapOf" in rendered, rendered)
        assertTrue("var m1 = persistentHashMapOf" in rendered, rendered)
    }
}
