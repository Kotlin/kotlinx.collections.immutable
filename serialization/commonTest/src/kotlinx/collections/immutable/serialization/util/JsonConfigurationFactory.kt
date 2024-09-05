/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.serialization.util

import kotlinx.serialization.json.Json

internal object JsonConfigurationFactory {
    fun createJsonConfiguration() = Json {
        isLenient = false
        prettyPrint = true
        ignoreUnknownKeys = false
    }
}