/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.property

import tests.IntWrapper
import kotlin.random.Random

/**
 * One step of a generated sequence.
 *
 * Every index is taken modulo the register or the universe, so deleting any step leaves all the
 * others meaningful. That totality is what lets the shrinker be plain deletion, with no repair pass
 * and no chance of a shrunk case that no longer runs.
 */
internal sealed interface MapOp {
    val target: Int

    class Put(override val target: Int, val key: Int, val salt: Int) : MapOp
    class Remove(override val target: Int, val key: Int) : MapOp
    class RemoveEntry(override val target: Int, val key: Int, val salt: Int) : MapOp
    class PutAll(override val target: Int, val source: Int) : MapOp
    class MinusKeys(override val target: Int, val source: Int) : MapOp
    class ThroughBuilder(override val target: Int, val key: Int, val salt: Int) : MapOp
    class Clear(override val target: Int) : MapOp
}

/**
 * A run of operations over a small register of maps.
 *
 * The fixed battery elsewhere builds two maps and asks a lot of questions about them. This asks
 * fewer questions of a much longer history, which is where structure and ownership accumulate: a map
 * that has been through a dozen mixed operations shares nodes with several earlier versions, and
 * every one of those has to stay exactly as it was.
 */
internal class SequenceCase(
    val universe: List<IntWrapper?>,
    val slots: Int,
    val ops: List<MapOp>,
    val origin: String
) {
    fun keyAt(index: Int): IntWrapper? = universe[index.mod(universe.size)]

    fun slotOf(index: Int): Int = index.mod(slots)

    fun withOps(ops: List<MapOp>): SequenceCase = SequenceCase(universe, slots, ops, origin)

    fun withUniverse(universe: List<IntWrapper?>): SequenceCase = SequenceCase(universe, slots, ops, origin)

    fun withSlots(slots: Int): SequenceCase = SequenceCase(universe, slots, ops, origin)

    override fun toString(): String = render()

    /** Renders the run as a compilable test body, which is the only useful form of a random failure. */
    fun render(): String = buildString {
        appendLine("    @Test")
        appendLine("    fun `regression from $origin`() {")
        for (key in universe.distinct()) {
            if (key == null) continue
            appendLine("        val k${key.obj} = IntWrapper(${key.obj}, ${renderHash(key.hashCode())})")
        }
        appendLine()
        for (slot in 0..<slots) {
            appendLine("        var m$slot = persistentHashMapOf<IntWrapper?, Value?>()")
        }
        appendLine()
        for (op in ops) appendLine("        " + renderOp(op))
        appendLine("    }")
    }

    private fun renderOp(op: MapOp): String {
        val slot = "m${slotOf(op.target)}"
        return when (op) {
            is MapOp.Put -> "$slot = $slot.putting(${name(op.key)}, ${renderValue(op.key, op.salt)})"
            is MapOp.Remove -> "$slot = $slot.removing(${name(op.key)})"
            is MapOp.RemoveEntry -> "$slot = $slot.removing(${name(op.key)}, ${renderValue(op.key, op.salt)})"
            is MapOp.PutAll -> "$slot = $slot.puttingAll(m${slotOf(op.source)})"
            is MapOp.MinusKeys -> "$slot = $slot - m${slotOf(op.source)}.keys"
            is MapOp.ThroughBuilder ->
                "$slot = $slot.builder().apply { put(${name(op.key)}, ${renderValue(op.key, op.salt)}) }.build()"
            is MapOp.Clear -> "$slot = $slot.cleared()"
        }
    }

    private fun name(keyIndex: Int): String {
        val key = keyAt(keyIndex)
        return if (key == null) "null" else "k${key.obj}"
    }

    private fun renderValue(keyIndex: Int, salt: Int): String {
        val value = valueOf(keyAt(keyIndex), salt)
        return if (value == null) "null" else "Value(${value.id})"
    }

    private fun renderHash(hash: Int): String = when {
        hash == Int.MIN_VALUE -> "Int.MIN_VALUE"
        hash == Int.MAX_VALUE -> "Int.MAX_VALUE"
        hash and KeyUniverse.LOW_BITS == 0 && hash != 0 -> "${hash ushr 30} shl 30"
        hash ushr 30 == 0 -> "$hash"
        else -> "${hash and KeyUniverse.LOW_BITS} or (${hash ushr 30} shl 30)"
    }
}

/** Values are derived from the key so that dropping one during shrinking cannot leave a case torn. */
internal fun valueOf(key: IntWrapper?, salt: Int): Value? =
    if (key.payload % 5 == 0) null else Value(key.payload + salt)

internal fun generateSequenceCase(
    random: Random,
    universe: KeyUniverse,
    slots: Int = 3,
    length: Int = 30,
    origin: String = "sequence"
): SequenceCase {
    val ops = List(length) {
        val target = random.nextInt(slots)
        val key = random.nextInt(universe.keys.size)
        val salt = if (random.nextBoolean()) 0 else 1000
        when (random.nextInt(10)) {
            0, 1, 2 -> MapOp.Put(target, key, salt)
            3 -> MapOp.Remove(target, key)
            4 -> MapOp.RemoveEntry(target, key, salt)
            5, 6 -> MapOp.PutAll(target, random.nextInt(slots))
            7 -> MapOp.MinusKeys(target, random.nextInt(slots))
            8 -> MapOp.ThroughBuilder(target, key, salt)
            else -> MapOp.Clear(target)
        }
    }
    return SequenceCase(universe.keys, slots, ops, origin)
}

/** Shrinks by deletion, keeping a candidate only when it still fails the same property. */
internal fun shrinkSequence(
    original: SequenceCase,
    maxSteps: Int = 400,
    run: (SequenceCase) -> List<PropertyFailure>
): Pair<SequenceCase, List<PropertyFailure>> {
    val originalFailures = run(original)
    if (originalFailures.isEmpty()) return original to originalFailures

    val target = originalFailures.first().signature
    var best = original
    var bestFailures = originalFailures
    var steps = 0

    fun accept(candidate: SequenceCase): Boolean {
        if (steps++ >= maxSteps) return false
        val failures = run(candidate)
        if (failures.none { it.signature == target }) return false
        best = candidate
        bestFailures = failures
        return true
    }

    var progress = true
    while (progress && steps < maxSteps) {
        progress = false

        for (i in best.ops.indices) {
            if (accept(best.withOps(best.ops.filterIndexed { j, _ -> j != i }))) {
                progress = true
                break
            }
        }
        if (progress) continue

        // Dropping a key remaps every index, so it is a big jump - but a rejected jump costs one run
        // and an accepted one usually removes several keys from the report at once.
        for (i in best.universe.indices) {
            if (best.universe.size <= 1) break
            if (accept(best.withUniverse(best.universe.filterIndexed { j, _ -> j != i }))) {
                progress = true
                break
            }
        }
        if (progress) continue

        if (best.slots > 1 && accept(best.withSlots(best.slots - 1))) progress = true
    }
    return best to bestFailures
}
