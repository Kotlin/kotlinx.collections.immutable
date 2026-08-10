/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.property

import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentMapOf
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Replacing a value through an entry is not a structural change, so it must not invalidate the
 * iterator that produced the entry. The ordered builder breaks this — see
 * [#307](https://github.com/Kotlin/kotlinx.collections.immutable/issues/307) — which is why
 * [iteratorFailures] takes `checkSetValueKeepsIteratorValid` and the ordered runner passes `false`.
 */
class OrderedIteratorSetValueTest {

    private fun shout(builder: MutableMap<Int, String>): Map<Int, String> {
        for (entry in builder.entries) entry.setValue(entry.value + "!")
        return builder
    }

    @Test
    fun theHashBuilderKeepsTheIteratorValid() {
        assertEquals(mapOf(1 to "a!", 2 to "b!"), shout(persistentHashMapOf(1 to "a", 2 to "b").builder()))
    }

    @Test
    fun theStdlibReferenceKeepsTheIteratorValid() {
        assertEquals(mapOf(1 to "a!", 2 to "b!"), shout(LinkedHashMap(mapOf(1 to "a", 2 to "b"))))
    }

    @Test
    @Ignore // https://github.com/Kotlin/kotlinx.collections.immutable/issues/307
    fun theOrderedBuilderKeepsTheIteratorValid() {
        assertEquals(mapOf(1 to "a!", 2 to "b!"), shout(persistentMapOf(1 to "a", 2 to "b").builder()))
    }
}
