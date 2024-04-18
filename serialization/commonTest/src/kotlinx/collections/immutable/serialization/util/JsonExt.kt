/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.serialization.util

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object JsonExt {
    inline fun <reified T> Json.encodeAndDecode(value: T): T {
        val string = encodeToString(value)
        return decodeFromString(string)
    }
}