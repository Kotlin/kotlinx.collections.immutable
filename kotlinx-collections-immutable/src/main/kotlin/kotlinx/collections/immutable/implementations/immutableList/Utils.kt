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

import kotlinx.collections.immutable.PersistentList

internal const val MAX_BUFFER_SIZE = 32
internal const val LOG_MAX_BUFFER_SIZE = 5
internal const val MAX_BUFFER_SIZE_MINUS_ONE = MAX_BUFFER_SIZE - 1
internal const val MAX_BUFFER_SIZE_PlUS_ONE = MAX_BUFFER_SIZE + 1

internal class ObjectWrapper(var value: Any?)

fun <E> persistentVectorOf(): PersistentList<E> {
    return SmallPersistentVector.EMPTY
}