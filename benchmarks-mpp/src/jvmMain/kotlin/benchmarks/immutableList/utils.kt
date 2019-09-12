/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableList

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

fun persistentListAdd(size: Int): PersistentList<String> {
    var list = persistentListOf<String>()
    repeat(times = size) {
        list = list.add("some element")
    }
    return list
}