/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.property

import tests.IntWrapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Two things have to hold for the snapshot property to mean anything: the mutation run must really
 * diverge between snapshots, and the check must notice a handed-out map that changed afterwards.
 */
class SnapshotCasesTest {

    private val a = IntWrapper(1, 1)
    private val b = IntWrapper(2, 2)
    private val c = IntWrapper(3, 33)
    private val d = IntWrapper(4, 65)

    private val model = mapOf<IntWrapper?, Value?>(a to Value(1), b to Value(2))

    @Test
    fun theRunDivergesBetweenSnapshots() {
        val snapshots = snapshotRun({ buildHashMap(it).builder() }, model, listOf(c, d)) { Value(it.payload) }

        assertTrue(snapshots.size >= 4, "only ${snapshots.size} snapshots")
        assertEquals(
            snapshots.size, snapshots.map { it.contentWhenTaken }.distinct().size,
            "the run repeated a state, so some snapshots prove nothing"
        )
    }

    @Test
    fun aCleanRunReportsNothing() {
        val snapshots = snapshotRun({ buildHashMap(it).builder() }, model, listOf(c, d)) { Value(it.payload) }

        assertEquals(emptyList(), verifySnapshots("hash", snapshots) { emptyList() })
    }

    /**
     * Hands out a map that is still writable, and lets [leak] write into it.
     *
     * A builder mutates in place wherever a node carries its own ownership token, and `build()`
     * rotates that token. A second builder adopts the first one's root and hands it out while the
     * first is still alive and still owns it, so what was handed out never stopped being writable.
     */
    private fun leakedSnapshot(leak: (MutableMap<IntWrapper?, Value?>) -> Unit): Snapshot {
        val base = buildHashMap(model)
        val live = base.builder()
        live[c] = Value(3)

        val other = base.builder()
        other.node = live.node
        other.size = live.size
        val handedOut = other.build()
        val contentWhenTaken = LinkedHashMap<IntWrapper?, Value?>(handedOut)

        leak(live)
        return Snapshot(handedOut, contentWhenTaken)
    }

    @Test
    fun anEntryAppearingInAMapAlreadyHandedOutIsCaught() {
        // Whether content comparison notices this one depends on the platform: `size` lives outside
        // the trie and stays stale, and the JVM's `AbstractMap.equals` checks size first and then
        // iterates the smaller side, so the extra entry is never looked at, while the Kotlin/JS
        // implementation does find it. Counting the trie is the check that holds everywhere, which
        // is why the runner counts on every operation rather than only at the end.
        val snapshot = leakedSnapshot { it[d] = Value(4) }

        val failures = verifySnapshots("leak", listOf(snapshot)) {
            it.asHashForTest().shape().violations(expectedSize = it.size)
        }

        assertTrue(failures.any { it.property == "snapshot.structure" }, "$failures")
    }

    @Test
    fun aValueChangingInAMapAlreadyHandedOutIsCaught() {
        val snapshot = leakedSnapshot { it[a] = Value(99) }

        val failures = verifySnapshots("leak", listOf(snapshot)) { emptyList() }

        assertTrue(failures.any { it.property == "snapshot.content" }, "$failures")
    }
}
