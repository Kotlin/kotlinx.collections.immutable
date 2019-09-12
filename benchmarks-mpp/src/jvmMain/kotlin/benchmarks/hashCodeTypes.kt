/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks

import java.util.*

const val ASCENDING_HASH_CODE = "ascending"
const val RANDOM_HASH_CODE = "random"
const val COLLISION_HASH_CODE = "collision"
const val NON_EXISTING_HASH_CODE = "nonExisting"

private inline fun intWrappers(size: Int, hashCodeGenerator: (index: Int) -> Int): List<IntWrapper> {
    val keys = mutableListOf<IntWrapper>()
    repeat(size) {
        keys.add(IntWrapper(it, hashCodeGenerator(it)))
    }
    return keys
}

private fun generateIntWrappers(hashCodeType: String, size: Int): List<IntWrapper> {
    val random = Random(40)
    return when(hashCodeType) {
        ASCENDING_HASH_CODE -> intWrappers(size) { it }
        RANDOM_HASH_CODE,
        NON_EXISTING_HASH_CODE -> intWrappers(size) { random.nextInt() }
        COLLISION_HASH_CODE -> intWrappers(size) { random.nextInt((size + 1) / 2) }
        else -> throw AssertionError("Unknown hashCodeType: $hashCodeType")
    }
}

fun generateKeys(hashCodeType: String, size: Int) = generateIntWrappers(hashCodeType, size)
fun generateElements(hashCodeType: String, size: Int) = generateIntWrappers(hashCodeType, size)


const val HASH_IMPL = "hash"
const val ORDERED_IMPL = "ordered"