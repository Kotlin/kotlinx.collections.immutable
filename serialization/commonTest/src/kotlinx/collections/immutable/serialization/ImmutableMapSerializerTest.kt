/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.serialization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.serialization.util.JsonConfigurationFactory
import kotlinx.collections.immutable.serialization.util.JsonExt.encodeAndDecode
import kotlinx.serialization.Serializable

class ImmutableMapSerializerTest {

    @Serializable
    private class ImmutableMapHolder<K, V>(
        @Serializable(with = ImmutableMapSerializer::class)
        val immutableMap: ImmutableMap<K, V>
    )

    @Test
    fun testImmutableList() {
        val json = JsonConfigurationFactory.createJsonConfiguration()
        persistentMapOf(1 to 1, 2 to 2, 3 to 3)
            .let(::ImmutableMapHolder)
            .let { expectedList -> assertEquals(expectedList.immutableMap, json.encodeAndDecode(expectedList).immutableMap) }
    }

    @Serializable
    private class PersistentMapHolder<K, V>(
        @Serializable(with = PersistentMapSerializer::class)
        val persistentMap: PersistentMap<K, V>
    )

    @Test
    fun testPersistentMap() {
        val json = JsonConfigurationFactory.createJsonConfiguration()
        persistentMapOf(1 to 1, 2 to 2, 3 to 3)
            .let(::PersistentMapHolder)
            .let { expectedList -> assertEquals(expectedList.persistentMap, json.encodeAndDecode(expectedList).persistentMap) }
    }

    @Serializable
    private class PersistentHashMapHolder<K, V>(
        @Serializable(with = PersistentHashMapSerializer::class)
        val hashMap: PersistentMap<K, V>
    )

    @Test
    fun testPersistentHashMap() {
        val json = JsonConfigurationFactory.createJsonConfiguration()
        persistentMapOf(1 to 1, 2 to 2, 3 to 3)
            .let(::PersistentHashMapHolder)
            .let { expectedList -> assertEquals(expectedList.hashMap, json.encodeAndDecode(expectedList).hashMap) }
    }


}