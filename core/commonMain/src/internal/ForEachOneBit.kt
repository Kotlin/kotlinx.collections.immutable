/*
 * Copyright 2016-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.internal

// 'iterate' all the bits set to one in a given integer, in the form of one-bit masks
internal inline fun Int.forEachOneBit(body: (mask: Int) -> Unit) {
    var mask = this
    while (mask != 0) {
        val bit = mask.takeLowestOneBit()
        body(bit)
        mask = mask xor bit
    }
}
