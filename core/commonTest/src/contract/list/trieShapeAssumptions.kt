/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.list

import kotlinx.collections.immutable.implementations.immutableList.MAX_BUFFER_SIZE

/**
 * Checks the assumption behind the hand-picked sizes in the list contract tests.
 *
 * The persistent list is a 32-way trie plus a tail buffer, so it changes shape at fixed sizes.
 * With [MAX_BUFFER_SIZE] = 32 the boundaries used across these tests are:
 * - 32: a full tail and no trie, the largest small vector;
 * - 33: the first leaf is pushed into the root;
 * - 64, 96: the tail is full again, with one and two leaves in the root;
 * - 65, 97: the second and the third leaf are pushed into the root;
 * - 1056: the root and the tail are both full;
 * - 1057: the root grows a level, producing a two-level trie;
 * - 1089: the first leaf is pushed into the root after it grew;
 * - 1091 (0..1090): a two-level root with two top-level children;
 * - 2146 (0..2145): a two-level root with three top-level children, so trimming the trie
 *   down to 1057 elements has a stale top-level child to nullify.
 *
 * Sizes between the boundaries (40, 100, 1100, ...) only need to stay inside the shape
 * named by their test, their exact values carry no meaning. The exception is an inserted
 * collection whose size is a multiple of 32: it preserves the tail size on purpose.
 *
 * If [MAX_BUFFER_SIZE] changes, this check fails: re-pick every size from the rules above
 * and verify with a coverage run (koverHtmlReport) that all branches of the immutableList
 * implementation are still exercised.
 */
internal fun checkTrieShapeAssumptions() {
    check(MAX_BUFFER_SIZE == 32) {
        """
        The sizes in these tests are hand-picked trie shape boundaries for the buffer size of 32.
        If MAX_BUFFER_SIZE changes, revisit each size manually
        and verify that all branches of the code under test are still covered.
        """.trimIndent()
    }
}
