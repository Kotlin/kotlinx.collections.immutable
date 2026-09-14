/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.trie

import kotlinx.collections.immutable.implementations.immutableMap.LOG_MAX_BRANCHING_FACTOR
import kotlinx.collections.immutable.implementations.immutableMap.MAX_SHIFT
import tests.IntWrapper

// Keys for the hash trie tests. A key with hash `c shl (n * LOG_MAX_BRANCHING_FACTOR)` shares the path of the
// colliding keys for n levels and sits in cell c of the level n node, next to them.
// The `*Sibling` keys branch off the path of the colliding keys at the named level.
val collidingKey1 = IntWrapper(1, 0)
val collidingKey2 = IntWrapper(2, 0)
val collidingKey3 = IntWrapper(4, 0)
val lastLevelSibling = IntWrapper(3, 1 shl MAX_SHIFT)
val rootSibling = IntWrapper(8, 1)
val levelOneSibling = IntWrapper(5, 1 shl LOG_MAX_BRANCHING_FACTOR)
val levelTwoSibling = IntWrapper(6, 1 shl (2 * LOG_MAX_BRANCHING_FACTOR))
val otherLevelOneSibling = IntWrapper(7, 2 shl LOG_MAX_BRANCHING_FACTOR)
