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

package kotlinx.collections.immutable.tests


internal fun Char.isUpperCase(): Boolean = this in 'A'..'Z'
internal fun Char.isDigit(): Boolean = this in '0'..'9'

internal fun <K, V> MutableMap<K, V>.remove(key: K, value: V): Boolean =
        if (key in this && this[key] == value) {
            remove(key)
            true
        } else {
            false
        }

public expect fun assertTypeEquals(expected: Any?, actual: Any?)
