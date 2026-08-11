/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.property

import kotlinx.collections.immutable.PersistentMap
import tests.IntWrapper

/**
 * Takes snapshots out of a builder while it keeps being mutated, and checks all of them at the end.
 *
 * A builder mutates its trie in place wherever a node carries its own [kotlinx.collections.immutable.internal.MutabilityOwnership]
 * token, and `build()` rotates that token so the nodes handed to the snapshot stop being writable.
 * If a single node keeps the old token, a later mutation writes straight into a map that was already
 * handed out, and nothing complains: the snapshot answers every question about itself correctly
 * right up to the moment it silently holds someone else's data. The library guards this with
 * `assert` calls, which compile to nothing on JS and Wasm, so on those targets this is the only
 * check there is.
 *
 * Verifying each snapshot right after taking it would prove nothing. They are all kept and verified
 * after the mutation run is over.
 */
internal fun snapshotFailures(
    label: String,
    builderOf: (Map<IntWrapper?, Value?>) -> PersistentMap.Builder<IntWrapper?, Value?>,
    validate: (PersistentMap<IntWrapper?, Value?>) -> List<String>,
    model: Map<IntWrapper?, Value?>,
    extraKeys: List<IntWrapper?>,
    valueFor: (IntWrapper?) -> Value?
): List<PropertyFailure> =
    verifySnapshots(label, snapshotRun(builderOf, model, extraKeys, valueFor), validate)

/** One snapshot and the content it held at the moment it was taken. */
internal class Snapshot(val map: PersistentMap<IntWrapper?, Value?>, val contentWhenTaken: Map<IntWrapper?, Value?>)

/**
 * The mutation run itself, separated from the checking so that a test can assert the run is not
 * trivial: a sequence whose snapshots never diverge would pass every check and prove nothing.
 */
internal fun snapshotRun(
    builderOf: (Map<IntWrapper?, Value?>) -> PersistentMap.Builder<IntWrapper?, Value?>,
    model: Map<IntWrapper?, Value?>,
    extraKeys: List<IntWrapper?>,
    valueFor: (IntWrapper?) -> Value?
): List<Snapshot> {
    val snapshots = mutableListOf<Snapshot>()
    if (model.isEmpty()) return snapshots

    val builder = builderOf(model)
    val running = LinkedHashMap(model)

    fun snapshot() {
        snapshots += Snapshot(builder.build(), LinkedHashMap(running))
    }

    snapshot()
    for (key in extraKeys) {
        builder[key] = valueFor(key)
        running[key] = valueFor(key)
        snapshot()
    }
    for (key in model.keys.toList()) {
        builder.remove(key)
        running.remove(key)
        snapshot()
    }
    // One more round after the last snapshot, so that the final one is mutated past as well.
    for (key in extraKeys) {
        builder[key] = valueFor(key)
        builder.remove(key)
    }

    return snapshots
}

internal fun verifySnapshots(
    label: String,
    snapshots: List<Snapshot>,
    validate: (PersistentMap<IntWrapper?, Value?>) -> List<String>
): List<PropertyFailure> {
    val failures = mutableListOf<PropertyFailure>()
    for (i in snapshots.indices) {
        val snapshot = snapshots[i].map
        val expected = snapshots[i].contentWhenTaken
        if (snapshot != expected || expected != snapshot) {
            failures += PropertyFailure(
                "snapshot.content", "$label snapshot $i", "holds $snapshot, held $expected when taken"
            )
        }
        if (snapshot.size != expected.size) {
            failures += PropertyFailure(
                "snapshot.size", "$label snapshot $i", "size ${snapshot.size}, was ${expected.size}"
            )
        }
        val problems = validate(snapshot)
        if (problems.isNotEmpty()) {
            failures += PropertyFailure("snapshot.structure", "$label snapshot $i", problems.joinToString("; "))
        }
    }
    return failures
}
