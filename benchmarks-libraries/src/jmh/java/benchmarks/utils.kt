/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Auto-generated file. DO NOT EDIT!

package benchmarks


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
    val random = java.util.Random(40)
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


fun immutableSize(size: Int, immutablePercentage: Double): Int {
    return kotlin.math.floor(size * immutablePercentage / 100.0).toInt()
}
