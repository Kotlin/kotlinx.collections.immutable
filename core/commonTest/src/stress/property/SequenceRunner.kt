/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.property

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.minus
import tests.IntWrapper

/**
 * Replays a [SequenceCase] against a register of maps and a register of stdlib models.
 *
 * Each step is checked cheaply - content, size, and the entry count walked out of the trie - because
 * the point is the length of the history, not the depth of any one check. Everything that survived
 * the run is then checked in full, including every intermediate version: a map a dozen operations
 * old shares nodes with the current one, and has to be exactly what it was.
 */
internal fun runSequenceCase(case: SequenceCase): List<PropertyFailure> {
    val failures = mutableListOf<PropertyFailure>()

    fun fail(property: String, operation: String, detail: String) {
        failures += PropertyFailure(property, operation, detail)
    }

    val maps = MutableList<PersistentMap<IntWrapper?, Value?>>(case.slots) {
        PersistentHashMap.emptyOf()
    }
    val models = MutableList(case.slots) { LinkedHashMap<IntWrapper?, Value?>() }
    val history = mutableListOf<Snapshot>()

    fun checkSlot(step: Int, operation: String, slot: Int) {
        val map = maps[slot]
        val model = models[slot]
        if (map != model || model != map) {
            fail("sequence.model", "step $step $operation", "holds $map, expected $model")
        }
        if (map.size != model.size) {
            fail("sequence.size", "step $step $operation", "${map.size}, expected ${model.size}")
        }
        val counted = map.asHashForTest().shape().entryCount
        if (counted != map.size) {
            fail("sequence.count", "step $step $operation", "trie holds $counted, size says ${map.size}")
        }
    }

    for (step in case.ops.indices) {
        val op = case.ops[step]
        val slot = case.slotOf(op.target)
        val name: String
        when (op) {
            is MapOp.Put -> {
                val key = case.keyAt(op.key)
                val value = valueOf(key, op.salt)
                maps[slot] = maps[slot].putting(key, value)
                models[slot][key] = value
                name = "put"
            }

            is MapOp.Remove -> {
                val key = case.keyAt(op.key)
                maps[slot] = maps[slot].removing(key)
                models[slot].remove(key)
                name = "remove"
            }

            is MapOp.RemoveEntry -> {
                val key = case.keyAt(op.key)
                val value = valueOf(key, op.salt)
                maps[slot] = maps[slot].removing(key, value)
                if (models[slot].containsKey(key) && models[slot][key] == value) models[slot].remove(key)
                name = "remove entry"
            }

            is MapOp.PutAll -> {
                val source = case.slotOf(op.source)
                maps[slot] = maps[slot].puttingAll(maps[source])
                models[slot].putAll(models[source])
                name = "putAll from $source"
            }

            is MapOp.MinusKeys -> {
                val source = case.slotOf(op.source)
                val doomed = models[source].keys.toList()
                maps[slot] = maps[slot].minus(maps[source].keys)
                for (key in doomed) models[slot].remove(key)
                name = "minus keys of $source"
            }

            is MapOp.ThroughBuilder -> {
                val key = case.keyAt(op.key)
                val value = valueOf(key, op.salt)
                maps[slot] = maps[slot].builder().apply { put(key, value) }.build()
                models[slot][key] = value
                name = "builder put"
            }

            is MapOp.Clear -> {
                maps[slot] = maps[slot].cleared()
                models[slot].clear()
                name = "clear"
            }
        }
        checkSlot(step, name, slot)
        history += Snapshot(maps[slot], LinkedHashMap(models[slot]))
    }

    // Everything ever handed out must still be what it was. This is the property the whole run
    // exists for: a version left behind a dozen operations ago shares nodes with the current one.
    failures += verifySnapshots("sequence", history) {
        it.asHashForTest().shape().violations(expectedSize = it.size)
    }

    for (slot in 0..<case.slots) {
        val problems = maps[slot].asHashForTest().shape().violations(expectedSize = maps[slot].size)
        if (problems.isNotEmpty()) {
            fail("sequence.structure", "slot $slot at the end", problems.joinToString("; "))
        }
        val iterated = maps[slot].keys.toList()
        if (iterated.toSet() != models[slot].keys || iterated.size != models[slot].size) {
            fail("sequence.iteration", "slot $slot at the end", "iterated $iterated")
        }
    }
    return failures
}
