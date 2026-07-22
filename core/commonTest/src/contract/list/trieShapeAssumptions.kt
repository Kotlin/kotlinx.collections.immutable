/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.list

import kotlinx.collections.immutable.implementations.immutableList.MAX_BUFFER_SIZE

internal fun checkTrieShapeAssumptions() {
    check(MAX_BUFFER_SIZE == 32) {
        """
        The sizes in this class are hand-picked trie shape boundaries for the buffer size of 32.
        If MAX_BUFFER_SIZE changes, revisit each size manually
        and verify that all branches of the code under test are still covered.
        """.trimIndent()
    }
}
