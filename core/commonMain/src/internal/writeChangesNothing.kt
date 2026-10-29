/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.internal

internal fun writeChangesNothing(stored: Any?, written: Any?): Boolean =
    if (stored is Number) {
        // A Number subclass, like BigInteger, is an object. Numbers never take ===: on Kotlin/JS it is value equality, and 0.0 === -0.0.
        if (stored is Int || stored is Double || stored is Long || stored is Float || stored is Short || stored is Byte) stored == written else stored === written
    } else {
        // On Kotlin/JS a null held as undefined, as arrayOfNulls gives it, is not === null.
        stored === written || stored == null && written == null ||
                (stored is Char || stored is Boolean || stored is UInt || stored is ULong || stored is UByte || stored is UShort) && stored == written
    }
