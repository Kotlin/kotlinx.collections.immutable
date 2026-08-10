/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.property

import tests.IntWrapper
import kotlin.random.Random

/**
 * Hash codes are generated from a *shape* rather than uniformly.
 *
 * A hash is exactly seven trie segments: six five-bit segments at shifts 0..25, which consume bits
 * 0..29, and a two-bit field at shift 30. So the shape of a trie is a function of how the generated
 * hashes agree and disagree segment by segment, and generating hashes uniformly over a small range —
 * which is what the existing stress suite does — pins every high segment to zero and makes the
 * bottom of the trie unreachable.
 */
internal enum class HashProfile {
    /** Everything hashes to zero: one collision group at maximum depth. */
    OneBucket,

    /**
     * Keys agree on bits 0..29 and differ only in bits 30..31. This is the only way to put a branch
     * node at shift 30, and therefore the only way to produce a collision node sitting beside a lone
     * element — the shape behind issues #294 and #300.
     */
    TopBits,

    /** Keys share a prefix and diverge at a chosen depth, producing single-child chains. */
    DeepPrefix,

    /** Uniform over the whole Int range: wide, shallow tries, collisions essentially never. */
    FullRange,

    /** Sign boundaries, where an arithmetic shift and a logical one disagree. */
    Extremes,
}

private val EXTREME_HASHES = intArrayOf(0, -1, 1, -2, Int.MIN_VALUE, Int.MAX_VALUE)

/** The four cells a shift-30 branch node can have: `indexSegment(h, 30)` only ever yields these. */
internal val MAX_SHIFT_SEGMENTS = intArrayOf(0, 1, 30, 31)

/** A batch of keys generated together, so that their hashes stand in a known relation. */
internal class Cluster(val profile: HashProfile, val keys: List<IntWrapper>) {
    override fun toString(): String = "$profile${keys.map { it.hashCode() }}"
}

/**
 * The keys a single generated case may use, together with the structure the generator knows it put
 * there. Operand pairs are *constructed* from [collisionGroups] rather than sampled, because
 * independent draws essentially never produce the interesting shapes.
 */
internal class KeyUniverse(val clusters: List<Cluster>) {

    val keys: List<IntWrapper> = clusters.flatMap { it.keys }

    /** Keys sharing a full 32-bit hash — exactly the groups that become collision nodes. */
    val collisionGroups: List<List<IntWrapper>> =
        keys.groupBy { it.hashCode() }.values.filter { it.size >= 2 }.map { it.toList() }

    /**
     * A key that agrees with [group] on bits 0..29 but lands in a different cell at shift 30.
     *
     * Holding one is what pushes a group's key down to shift 30 without joining its collision node,
     * which is how a tree ends up with a lone element in the very cell where another tree holds a
     * collision node.
     */
    fun pusherFor(group: List<IntWrapper>): IntWrapper? {
        val groupHash = group.first().hashCode()
        return keys.firstOrNull { it.hashCode() != groupHash && it.hashCode() and LOW_BITS == groupHash and LOW_BITS }
    }

    override fun toString(): String = clusters.joinToString("; ")

    internal companion object {
        /** Bits 0..29 — everything the trie consumes above shift 30. */
        const val LOW_BITS = (1 shl 30) - 1
    }
}

/** How the two operands of a two-tree operation relate to each other. */
internal enum class OperandRelation {
    Disjoint,
    Overlapping,
    Subset,
    Identical,

    /**
     * One side holds a full collision group, the other holds a single key of that group plus the
     * key that pushes it down to shift 30. At shift 30 one tree then has a collision node exactly
     * where the other has a lone element — the shape that bulk operations mis-dispatched in #294.
     */
    CollisionVsLone,

    /** Both sides hold a collision group for the same hash, with at least one key in common. */
    CollisionVsCollision,
}

internal fun hashesFor(random: Random, profile: HashProfile, count: Int): List<Int> = when (profile) {
    HashProfile.OneBucket -> List(count) { 0 }

    HashProfile.TopBits -> {
        val low = random.nextInt(1 shl 10)
        List(count) { low or (random.nextInt(4) shl 30) }
    }

    HashProfile.DeepPrefix -> {
        val divergeAt = random.nextInt(1, 6)
        val shift = divergeAt * 5
        val prefix = random.nextInt(1 shl shift)
        List(count) { prefix or (random.nextInt(1 shl 5) shl shift) }
    }

    HashProfile.FullRange -> List(count) { random.nextInt() }

    HashProfile.Extremes -> List(count) { EXTREME_HASHES[random.nextInt(EXTREME_HASHES.size)] }
}

/**
 * Builds a universe out of several clusters. Keys carry distinct payloads throughout, because
 * [IntWrapper] compares by payload and asserts that equal payloads carry equal hashes.
 */
internal fun generateUniverse(
    random: Random,
    profiles: List<HashProfile> = HashProfile.entries,
    keysPerCluster: IntRange = 2..5
): KeyUniverse {
    var nextPayload = 0
    val clusters = profiles.map { profile ->
        val count = random.nextInt(keysPerCluster.first, keysPerCluster.last + 1)
        val keys = hashesFor(random, profile, count).map { hash -> IntWrapper(nextPayload++, hash) }
        Cluster(profile, keys)
    }
    return KeyUniverse(clusters)
}

/**
 * The universe used by the exhaustive bottom-level sweep: one low-bit value, all four shift-30
 * cells, two full collision groups and one lone key. Only four cells exist down there, so the sweep
 * enumerates rather than samples, and randomness is spent on the levels above.
 */
internal fun maxShiftUniverse(low: Int = 0b1): KeyUniverse {
    var payload = 0
    fun key(hash: Int) = IntWrapper(payload++, hash)
    val keys = listOf(
        key(low),                       // collision group A, cell 0
        key(low),
        key(low),
        key(low or (1 shl 30)),         // collision group B, cell 1
        key(low or (1 shl 30)),
        key(low or (2 shl 30)),         // lone key, cell 30
        key(low or (3 shl 30)),         // lone key, cell 31
        key(low or (1 shl 11)),         // separate subtree above shift 30
    )
    return KeyUniverse(listOf(Cluster(HashProfile.TopBits, keys)))
}

/**
 * Builds the two operand key sets for [relation], or returns `null` when this universe cannot
 * express it — a caller must skip rather than silently test a weaker shape.
 */
internal fun KeyUniverse.operandPair(
    random: Random,
    relation: OperandRelation
): Pair<List<IntWrapper>, List<IntWrapper>>? {
    if (keys.size < 2) return null
    return when (relation) {
        OperandRelation.Disjoint -> {
            val split = 1 + random.nextInt(keys.size - 1)
            keys.take(split) to keys.drop(split)
        }

        OperandRelation.Overlapping -> {
            val split = 1 + random.nextInt(keys.size - 1)
            val overlap = random.nextInt(1, keys.size - split + 1)
            keys.take(split + overlap) to keys.drop(split)
        }

        OperandRelation.Subset -> keys to keys.take(1 + random.nextInt(keys.size))

        OperandRelation.Identical -> keys to keys

        OperandRelation.CollisionVsLone -> {
            val group = collisionGroups.firstOrNull() ?: return null
            val pusher = pusherFor(group) ?: return null
            group to listOf(group.first(), pusher)
        }

        OperandRelation.CollisionVsCollision -> {
            val group = collisionGroups.firstOrNull { it.size >= 3 } ?: return null
            group.dropLast(1) to group.drop(1)
        }
    }
}
