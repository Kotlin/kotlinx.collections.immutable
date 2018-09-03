/*
 * Copyright 2016-2018 JetBrains s.r.o.
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

package kotlinx.collections.immutable.implementations.immutableMap

class KeyWrapper<K: Comparable<K>>(val key: K, val hashCode: Int) : Comparable<KeyWrapper<K>> {
    override fun hashCode(): Int {
        return hashCode
    }

    override fun equals(other: Any?): Boolean {
        if (other !is KeyWrapper<*>) {
            return false
        }
        assert(key != other.key || hashCode == other.hashCode)  // if keys are equal hashCodes must be equal
        return key == other.key
    }

    override fun compareTo(other: KeyWrapper<K>): Int {
        return key.compareTo(other.key)
    }
}