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

package kotlinx.collections.immutable.implementations.immutableList

import java.util.NoSuchElementException

internal class BufferIterator<out T>(
        private val buffer: Array<T>,
        index: Int,
        size: Int
) : AbstractListIterator<T>(index, size) {
    override fun next(): T {
        if (!hasNext()) {
            throw NoSuchElementException()
        }
        return buffer[index++]
    }

    override fun previous(): T {
        if (!hasPrevious()) {
            throw NoSuchElementException()
        }
        return buffer[--index]
    }
}