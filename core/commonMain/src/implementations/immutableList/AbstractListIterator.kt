/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.implementations.immutableList

internal abstract class AbstractListIterator<out E>(var index: Int, var size: Int) : ListIterator<E> {
    override fun hasNext(): Boolean {
        return index < size
    }

    override fun hasPrevious(): Boolean {
        return index > 0
    }

    override fun nextIndex(): Int {
        return index
    }

    override fun previousIndex(): Int {
        return index - 1
    }
}