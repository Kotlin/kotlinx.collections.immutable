/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks
import kotlin.js.JsName

class ObjectWrapper<K: Comparable<K>>(
        val obj: K,
        @JsName("_hashCode") val hashCode: Int
) : Comparable<ObjectWrapper<K>> {
    override fun hashCode(): Int {
        return hashCode
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ObjectWrapper<*>) {
            return false
        }
        return obj == other.obj
    }

    override fun compareTo(other: ObjectWrapper<K>): Int {
        return obj.compareTo(other.obj)
    }
}


typealias IntWrapper = ObjectWrapper<Int>